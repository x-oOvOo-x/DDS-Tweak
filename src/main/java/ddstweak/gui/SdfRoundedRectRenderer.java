package ddstweak.gui;

import ddstweak.platform.minecraft.ScreenRenderCompat;
import java.util.HashMap;
import java.util.Map;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#elseif MC >= 12000
import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 11600
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

//#if MC >= 260300
//$$ import com.mojang.blaze3d.vertex.VertexConsumer;
//$$ import com.mojang.renderpearl.api.GpuFormat;
//$$ import com.mojang.renderpearl.api.pipeline.BlendFunction;
//$$ import com.mojang.renderpearl.api.pipeline.ColorTargetState;
//$$ import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
//$$ import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//$$ import com.mojang.renderpearl.api.vertex.VertexFormat;
//$$ import net.minecraft.client.gui.navigation.ScreenRectangle;
//$$ import net.minecraft.client.gui.render.TextureSetup;
//$$ import net.minecraft.client.renderer.BindGroupLayouts;
//$$ import net.minecraft.client.renderer.RenderPipelines;
//$$ import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
//$$ import net.minecraft.resources.Identifier;
//$$ import org.joml.Matrix3x2f;
//$$ import org.joml.Matrix3x2fc;
//#endif

/** GPU SDF on 26.x, CPU-covered rounded rectangles on earlier targets. */
public final class SdfRoundedRectRenderer {
    private SdfRoundedRectRenderer() {}

    public static void fill(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor gui,
            //#elseif MC >= 12000
            GuiGraphics gui,
            //#elseif MC >= 11600
            //$$ PoseStack gui,
            //#else
            //$$ Object gui,
            //#endif
            int x1, int y1, int x2, int y2, float radius, int argb) {
        //#if MC >= 260300
        //$$ draw(gui, x1, y1, x2, y2, radius, argb, 0);
        //#elseif MC >= 260100
        //$$ LegacyGpuSdfRenderer.draw(gui, x1, y1, x2, y2, radius, argb, false);
        //#else
        cpuRoundedFill(gui, x1, y1, x2, y2, radius, argb);
        //#endif
    }

    public static void outline(
            //#if MC >= 260100
            //$$ GuiGraphicsExtractor gui,
            //#elseif MC >= 12000
            GuiGraphics gui,
            //#elseif MC >= 11600
            //$$ PoseStack gui,
            //#else
            //$$ Object gui,
            //#endif
            int x1, int y1, int x2, int y2, int radius, int argb) {
        //#if MC >= 260300
        //$$ draw(gui, x1, y1, x2, y2, radius, argb, 1);
        //#elseif MC >= 260100
        //$$ LegacyGpuSdfRenderer.draw(gui, x1, y1, x2, y2, radius, argb, true);
        //#else
        drawRectOutline(gui, x1, y1, x2, y2, argb);
        //#endif
    }

    //#if MC >= 260100
    //$$ /** Analytic antialiased arrows; 2 = down and 3 = right. */
    //$$ public static void triangle(GuiGraphicsExtractor gui, int x1, int y1,
    //$$         int x2, int y2, boolean right, int argb) {
    //$$     if (x2 <= x1 || y2 <= y1 || (argb >>> 24) == 0) return;
        //#if MC >= 260300
        //$$ draw(gui, x1, y1, x2, y2, 0, argb, right ? 3 : 2);
        //#else
        //$$ LegacyGpuSdfRenderer.drawTriangle(gui, x1, y1, x2, y2, right, argb);
        //#endif
    //$$ }
    //#endif

    //#if MC >= 260100
    //$$ /** Complete GPU antialiased difference of shifted backing and foreground. */
    //$$ public static void leftAccent(GuiGraphicsExtractor gui, int left, int top,
    //$$         int right, int bottom, float radius, int shift, int argb) {
    //$$     if (shift <= 0 || right <= left || bottom <= top || (argb >>> 24) == 0) return;
        //#if MC >= 260300
        //$$ draw(gui, left - shift, top, right - shift, bottom, radius, argb, 4 + shift);
        //#else
        //$$ LegacyGpuSdfRenderer.drawAccent(gui, left - shift, top, right - shift,
        //$$         bottom, radius, shift, argb);
        //#endif
    //$$ }
    //#endif

    //#if MC < 260100
    /** Cached 4x4 corner masks; rendering groups equal-alpha pixels into horizontal spans. */
    private static final Map<Integer,int[]> CORNER_COVERAGE_CACHE=new HashMap<>();

    /** Draw rounded corners with a small, batched set of spans per scanline. */
    private static void cpuRoundedFill(
            //#if MC >= 12000
            GuiGraphics gui,
            //#elseif MC >= 11600
            //$$ PoseStack gui,
            //#else
            //$$ Object gui,
            //#endif
            int left,int top,int right,int bottom,float radius,int color){
        if(right<=left||bottom<=top||(color>>>24)==0)return;
        float r=Math.max(0,Math.min(radius,Math.min(right-left,bottom-top)/2.0f));
        if(r==0){ScreenRenderCompat.fill(gui,left,top,right,bottom,color);return;}
        int steps=(int)Math.ceil(r);
        int[] mask=cornerCoverageMask(r,steps);
        // Middle region is full width; curved rows above and below do not overlap it.
        if(top+steps<bottom-steps)
            ScreenRenderCompat.fill(gui,left,top+steps,right,bottom-steps,color);
        for(int y=0;y<steps;y++)
            paintRoundedRows(gui,left,top+y,bottom-1-y,right,steps,mask,y,color);
    }

    /** Build each radius mask once; the cache stays bounded for custom UI radii. */
    private static int[] cornerCoverageMask(float radius,int steps){
        int key=Float.floatToIntBits(radius);
        int[] cached=CORNER_COVERAGE_CACHE.get(key);
        if(cached!=null)return cached;
        int[] mask=new int[steps*steps];
        for(int y=0;y<steps;y++)
            for(int x=0;x<steps;x++)
                mask[y*steps+x]=cornerCoverage(radius,x,y);
        if(CORNER_COVERAGE_CACHE.size()>=16)CORNER_COVERAGE_CACHE.clear();
        CORNER_COVERAGE_CACHE.put(key,mask);
        return mask;
    }

    /**
     * Render mirrored top/bottom scanlines from the same cached mask. Adjacent
     * pixels with equal coverage become one fill call instead of one call each.
     */
    private static void paintRoundedRows(
            //#if MC >= 12000
            GuiGraphics gui,
            //#elseif MC >= 11600
            //$$ PoseStack gui,
            //#else
            //$$ Object gui,
            //#endif
            int left,int topY,int bottomY,int right,int steps,int[] mask,int cornerY,int color){
        int row=cornerY*steps;
        // Small radii may have no fully covered corner pixel; keep the central span.
        if(mask[row+steps-1]!=16)
            fillRows(gui,left+steps,topY,right-steps,topY+1,bottomY,color);
        int start=0,coverage=mask[row];
        for(int x=1;x<=steps;x++){
            int next=x==steps?-1:mask[row+x];
            if(next==coverage)continue;
            if(coverage>0){
                int alpha=((color>>>24)*coverage+8)/16;
                int rgba=(alpha<<24)|(color&0x00FFFFFF);
                if(coverage==16){
                    fillRows(gui,left+start,topY,right-start,topY+1,
                            bottomY,rgba);
                }else{
                    int leftStart=left+start,leftEnd=left+x;
                    int rightStart=right-x,rightEnd=right-start;
                    if(rightStart<leftEnd){
                        fillRows(gui,leftStart,topY,rightEnd,topY+1,bottomY,rgba);
                    }else{
                        fillRows(gui,leftStart,topY,leftEnd,topY+1,bottomY,rgba);
                        fillRows(gui,rightStart,topY,rightEnd,topY+1,bottomY,rgba);
                    }
                }
            }
            start=x;
            coverage=next;
        }
    }

    private static void fillRows(
            //#if MC >= 12000
            GuiGraphics gui,
            //#elseif MC >= 11600
            //$$ PoseStack gui,
            //#else
            //$$ Object gui,
            //#endif
            int left,int top,int right,int bottom,int bottomY,int color){
        if(right<=left)return;
        ScreenRenderCompat.fill(gui,left,top,right,bottom,color);
        if(bottomY!=top)
            ScreenRenderCompat.fill(gui,left,bottomY,right,bottomY+1,color);
    }

    private static int cornerCoverage(float radius,int pixelX,int pixelY){
        int inside=0;
        for(int j=0;j<4;j++)for(int i=0;i<4;i++){
            double dx=radius-pixelX-(i+0.5)/4.0;
            double dy=radius-pixelY-(j+0.5)/4.0;
            if(dx*dx+dy*dy<=radius*radius)inside++;
        }
        return inside;
    }
    //#endif

    /** Fast rectangular fallback on pre-26.x targets. */
    //#if MC < 260100
    private static void drawRectOutline(
            //#if MC >= 12000
            GuiGraphics gui,
            //#elseif MC >= 11600
            //$$ PoseStack gui,
            //#else
            //$$ Object gui,
            //#endif
            int x1, int y1, int x2, int y2, int argb) {
        if (x2 <= x1 || y2 <= y1 || (argb >>> 24) == 0) return;
        ScreenRenderCompat.fill(gui, x1, y1, x2, y1 + 1, argb);
        if (y2 - y1 > 1) ScreenRenderCompat.fill(gui, x1, y2 - 1, x2, y2, argb);
        if (y2 - y1 > 2) {
            ScreenRenderCompat.fill(gui, x1, y1 + 1, x1 + 1, y2 - 1, argb);
            if (x2 - x1 > 1)
                ScreenRenderCompat.fill(gui, x2 - 1, y1 + 1, x2, y2 - 1, argb);
        }
    }
    //#endif

    //#if MC >= 260300
    //$$ private static final Identifier SHADER = Identifier.fromNamespaceAndPath("dds-tweak", "core/rounded_rect");
    //$$ private static final VertexFormat FORMAT = VertexFormat.builder(0)
    //$$         .addAttribute("Position", GpuFormat.RGB32_FLOAT)
    //$$         .addAttribute("Color", GpuFormat.RGBA8_UNORM)
    //$$         .addAttribute("UV0", GpuFormat.RG32_FLOAT)
    //$$         .addAttribute("UV1", GpuFormat.RG16_SINT)
    //$$         .addAttribute("UV3", GpuFormat.RG32_FLOAT)
    //$$         .build();
    //$$ private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
    //$$         .withLocation(Identifier.fromNamespaceAndPath("dds-tweak", "pipeline/rounded_rect"))
    //$$         .withBindGroupLayout(BindGroupLayouts.GLOBALS)
    //$$         .withBindGroupLayout(BindGroupLayouts.PROJECTION)
    //$$         .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
    //$$         .withVertexShader(SHADER)
    //$$         .withFragmentShader(SHADER)
    //$$         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    //$$         .withVertexBinding(0, FORMAT)
    //$$         .withPrimitiveTopology(PrimitiveTopology.QUADS)
    //$$         .withCull(false)
    //$$         .build());
    //$$
    //$$ private static void draw(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2,
    //$$         float radius, int argb, int mode) {
    //$$     if (x2 <= x1 || y2 <= y1 || (argb >>> 24) == 0) return;
    //$$     int r = Math.round(Math.max(0, Math.min(radius, Math.min(x2 - x1, y2 - y1) / 2.0f)) * 2);
    //$$     gui.guiRenderState.addGuiElement(new SdfRect(
    //$$             new Matrix3x2f(gui.pose()), gui.scissorStack.peek(), x1, y1, x2, y2, r, argb, mode));
    //$$ }
    //$$
    //$$ private static final class SdfRect implements GuiElementRenderState {
    //$$     private final Matrix3x2fc pose;
    //$$     private final ScreenRectangle scissor;
    //$$     private final ScreenRectangle bounds;
    //$$     private final int x1, y1, x2, y2, radius, argb;
    //$$     private final int mode;
    //$$
    //$$     SdfRect(Matrix3x2fc pose, ScreenRectangle scissor,
    //$$             int x1, int y1, int x2, int y2, int radius, int argb, int mode) {
    //$$         this.pose = pose;
    //$$         this.scissor = scissor;
    //$$         this.x1 = x1;
    //$$         this.y1 = y1;
    //$$         this.x2 = x2;
    //$$         this.y2 = y2;
    //$$         this.radius = radius;
    //$$         this.argb = argb;
    //$$         this.mode = mode;
    //$$         ScreenRectangle area = new ScreenRectangle(x1, y1, x2 - x1, y2 - y1).transformMaxBounds(pose);
    //$$         this.bounds = scissor == null ? area : scissor.intersection(area);
    //$$     }
    //$$
    //$$     @Override public RenderPipeline pipeline() { return PIPELINE; }
    //$$     @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
    //$$     @Override public ScreenRectangle scissorArea() { return scissor; }
    //$$     @Override public ScreenRectangle bounds() { return bounds; }
    //$$
    //$$     @Override public void buildVertices(VertexConsumer vertices) {
    //$$         float hx = (x2 - x1) * 0.5F, hy = (y2 - y1) * 0.5F;
    //$$         vertex(vertices, x1, y1, -hx, -hy, hx, hy);
    //$$         vertex(vertices, x1, y2, -hx,  hy, hx, hy);
    //$$         vertex(vertices, x2, y2,  hx,  hy, hx, hy);
    //$$         vertex(vertices, x2, y1,  hx, -hy, hx, hy);
    //$$     }
    //$$
    //$$     private void vertex(VertexConsumer vertices, int x, int y,
    //$$             float localX, float localY, float hx, float hy) {
    //$$         vertices.addVertexWith2DPose(pose, x, y)
    //$$                 .setColor(argb).setUv(localX, localY)
    //$$                 .setUv1(radius, mode).setUv3(hx, hy);
    //$$     }
    //$$ }
    //#endif
}
