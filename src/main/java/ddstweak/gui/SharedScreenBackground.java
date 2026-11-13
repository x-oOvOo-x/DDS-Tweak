package ddstweak.gui;

import ddstweak.platform.minecraft.ScreenRenderCompat;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

/**
 * A single blurred world background is drawn by the owning vanilla Screen first.
 * 复用各界面的导航背景与分隔线绘制。
 */
public final class SharedScreenBackground {
    public static final int NAV_HEIGHT=222;
    public static final int HEADER_RULE_Y=16,FOOTER_RULE_Y=192,FOOTER_ROW_Y=200;
    private static final int RULE_COLOR=0x257F858C;
    private SharedScreenBackground(){}
    public static int navigationTop(int height){return Math.max(2,(height-NAV_HEIGHT)/2);}

    /** Shared 360-GUI-pixel header/footer rules for Camera navigation and editors. */
    public static void rule(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int left,int y,int width){
        ScreenRenderCompat.fill(graphics,left,y,left+width,y+1,RULE_COLOR);
    }

    public static void drawNavigationRules(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int width,int top){
        int ruleWidth=Math.min(360,width-16),left=(width-ruleWidth)/2;
        ScreenRenderCompat.fill(graphics,left,top+HEADER_RULE_Y,left+ruleWidth,top+HEADER_RULE_Y+1,RULE_COLOR);
        ScreenRenderCompat.fill(graphics,left,top+FOOTER_RULE_Y,left+ruleWidth,top+FOOTER_RULE_Y+1,RULE_COLOR);
    }

    public static void draw(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor graphics,
            //#elseif MC >= 12000
            GuiGraphics graphics,
            //#elseif MC >= 11600
            //$$ PoseStack graphics,
            //#else
            //$$ Object graphics,
            //#endif
            int width,int height,int contentTop,int contentBottom,int middleColor) {
        int top=Math.max(0,Math.min(height,contentTop));
        int bottom=Math.max(top,Math.min(height,contentBottom));
        ScreenRenderCompat.fill(graphics,0,0,width,height,0x29000000);
        ScreenRenderCompat.fill(graphics,0,top,width,bottom,middleColor);
        if(top>0) ScreenRenderCompat.fill(graphics,0,top,width,top+1,0x66D8DEE3);
        if(bottom<height) ScreenRenderCompat.fill(graphics,0,bottom-1,width,bottom,0x66D8DEE3);
    }
}