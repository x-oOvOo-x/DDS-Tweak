package ddstweak.feature.cameraplayerselector;

import ddstweak.platform.minecraft.ScreenRenderCompat;
import ddstweak.gui.SdfRoundedRectRenderer;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

/**
 * Exact CPU rounded slots before 26.x, shared SDF on 26.x; preserve 1px curved outline.
 * 26.x 使用共享 SDF；旧版本保留 CPU 圆角和 1px 曲线描边，不改变物品贴图及命中区域。
 */
final class CameraSlotRender {
    private CameraSlotRender(){}

    private static final int SAMPLES=16,MAX_RADIUS = 7;
    //#if MC < 260100
    private static final int[][][] COVERAGE=new int[2][MAX_RADIUS+1][];

    static {
        // Cache 16x16 subpixel coverage so per-frame rendering does not repeat circle math.
        for(int radius=2;radius<=MAX_RADIUS;radius++){
            for(int kind=0;kind<2;kind++){
                int[] mask=new int[radius*radius];
                for(int py=0;py<radius;py++)
                    for(int px=0;px<radius;px++)
                        mask[py*radius+px]=coverage(px,py,radius,kind==1);
                COVERAGE[kind][radius]=mask;
            }
        }
    }
    //#endif

    static void fill(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x,int y,int w,int h,int color){
        if(w<=0||h<=0)return;
        //#if MC >= 260100
        //$$ SdfRoundedRectRenderer.fill(graphics,x,y,x+w,y+h,
        //$$         Math.min(MAX_RADIUS, Math.min(w, h) / 2),color);
        //#else
        draw(graphics,x,y,w,h,color,false);
        //#endif
    }

    static void outline(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x,int y,int w,int h,int color){
        if(w<=0||h<=0)return;
        //#if MC >= 260100
        //$$ SdfRoundedRectRenderer.outline(graphics,x,y,x+w,y+h,
        //$$         Math.min(MAX_RADIUS, Math.min(w, h) / 2),color);
        //#else
        draw(graphics,x,y,w,h,color,true);
        //#endif
    }

    //#if MC < 260100
    /** Draw 16x16 sampled alpha edges without version-specific shaders or textures. */
    private static void draw(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int x,int y,int w,int h,int color,boolean border){
        if(w<=0||h<=0)return;
        int radius=Math.min(MAX_RADIUS,Math.min(w,h)/2);
        if(radius<2){
            if(border){
                ScreenRenderCompat.fill(graphics,x,y,x+w,y+1,color);
                ScreenRenderCompat.fill(graphics,x,y+h-1,x+w,y+h,color);
                ScreenRenderCompat.fill(graphics,x,y,x+1,y+h,color);
                ScreenRenderCompat.fill(graphics,x+w-1,y,x+w,y+h,color);
            }else ScreenRenderCompat.fill(graphics,x,y,x+w,y+h,color);
            return;
        }

        if(border){
            ScreenRenderCompat.fill(graphics,x+radius,y,x+w-radius,y+1,color);
            ScreenRenderCompat.fill(graphics,x+radius,y+h-1,x+w-radius,y+h,color);
            ScreenRenderCompat.fill(graphics,x,y+radius,x+1,y+h-radius,color);
            ScreenRenderCompat.fill(graphics,x+w-1,y+radius,x+w,y+h-radius,color);
        }else{
            ScreenRenderCompat.fill(graphics,x+radius,y,x+w-radius,y+radius,color);
            ScreenRenderCompat.fill(graphics,x,y+radius,x+w,y+h-radius,color);
            ScreenRenderCompat.fill(graphics,x+radius,y+h-radius,x+w-radius,y+h,color);
        }

        // Coalesce adjacent pixels of identical computed alpha into horizontal spans.
        // The premultiplied appearance and non-overlapping corner regions stay unchanged.
        for(int py=0;py<radius;py++){
            for(int px=0;px<radius;){
                int coverage=COVERAGE[border?1:0][radius][py*radius+px];
                if(coverage==0){px++;continue;}
                int shaded=(color&0x00FFFFFF)|((((color>>>24)*coverage+SAMPLES*SAMPLES*128)/(SAMPLES*SAMPLES*256))<<24);
                int start=px++;
                while(px<radius){
                    int next=COVERAGE[border?1:0][radius][py*radius+px];
                    int nextShade=(color&0x00FFFFFF)|((((color>>>24)*next+SAMPLES*SAMPLES*128)/(SAMPLES*SAMPLES*256))<<24);
                    if(next==0||nextShade!=shaded)break;
                    px++;
                }
                ScreenRenderCompat.fill(graphics,x+start,y+py,x+px,y+py+1,shaded);
                ScreenRenderCompat.fill(graphics,x+w-px,y+py,x+w-start,y+py+1,shaded);
                ScreenRenderCompat.fill(graphics,x+start,y+h-1-py,x+px,y+h-py,shaded);
                ScreenRenderCompat.fill(graphics,x+w-px,y+h-1-py,x+w-start,y+h-py,shaded);
            }
        }
    }

    /** 1 GUI px smooth alpha transition instead of a hard binary edge per sample. */
    private static int coverage(int x,int y,int radius,boolean border){
        double opacity=0;
        for(int sy=0;sy<SAMPLES;sy++){
            for(int sx=0;sx<SAMPLES;sx++){
                double dx=radius-x-(sx+0.5)/SAMPLES;
                double dy=radius-y-(sy+0.5)/SAMPLES;
                double distance=Math.sqrt(dx*dx+dy*dy);
                double outer=smooth(radius-distance);
                opacity+=border?outer-smooth(radius-1-distance):outer;
            }
        }
        return (int)Math.round(Math.max(0,opacity)*256);
    }

    private static double smooth(double distance){
        double t=Math.max(0,Math.min(1,distance+0.5));
        return t*t*(3-2*t);
    }
    //#endif
}
