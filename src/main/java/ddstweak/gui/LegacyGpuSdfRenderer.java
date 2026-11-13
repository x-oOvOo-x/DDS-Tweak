package ddstweak.gui;

//#if MC >= 260100
//#if MC < 260300

//$$ import com.mojang.blaze3d.pipeline.BlendFunction;
//$$ import com.mojang.blaze3d.pipeline.ColorTargetState;
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline;
//#if MC < 260200
//$$ import com.mojang.blaze3d.shaders.UniformType;
//#endif
//$$ import com.mojang.blaze3d.vertex.VertexConsumer;
//$$ import com.mojang.blaze3d.vertex.VertexFormat;
//#if MC >= 260200
//$$ import com.mojang.blaze3d.GpuFormat;
//$$ import com.mojang.blaze3d.PrimitiveTopology;
//$$ import net.minecraft.client.renderer.BindGroupLayouts;
//#else
//$$ import com.mojang.blaze3d.vertex.VertexFormatElement;
//#endif
//$$ import ddstweak.mixin.gui.RenderPipelinesAccess;
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//$$ import net.minecraft.client.gui.navigation.ScreenRectangle;
//$$ import net.minecraft.client.gui.render.TextureSetup;
//$$ import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
//$$ import net.minecraft.resources.Identifier;
//$$ import org.joml.Matrix3x2f;
//$$ import org.joml.Matrix3x2fc;

//$$ /** GPU SDF via 26.1/26.2 Blaze3D. No raw OpenGL calls; the 26.3 path is separate. */
//$$ public final class LegacyGpuSdfRenderer {
    //$$ private static final Identifier SHADER = Identifier.fromNamespaceAndPath("dds-tweak", "core/rounded_rect_legacy");
    //#if MC >= 260200
    //$$ private static final VertexFormat FORMAT = VertexFormat.builder(0)
            //$$ .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            //$$ .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            //$$ .addAttribute("UV0", GpuFormat.RG32_FLOAT)
            //$$ .addAttribute("UV1", GpuFormat.RG16_SINT)
            //$$ .addAttribute("UV2", GpuFormat.RG16_SINT)
            //$$ .build();
    //#else
    //$$ private static final VertexFormat FORMAT = VertexFormat.builder()
            //$$ .add("Position", VertexFormatElement.POSITION)
            //$$ .add("Color", VertexFormatElement.COLOR)
            //$$ .add("UV0", VertexFormatElement.UV0)
            //$$ .add("UV1", VertexFormatElement.UV1)
            //$$ .add("UV2", VertexFormatElement.UV2)
            //$$ .build();
    //#endif
    //$$ private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            //$$ .withLocation(Identifier.fromNamespaceAndPath("dds-tweak", "pipeline/rounded_rect_legacy"))
            //$$ .withVertexShader(SHADER)
            //$$ .withFragmentShader(SHADER)
            //#if MC >= 260200
            //$$ .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            //$$ .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            //$$ .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            //$$ .withVertexBinding(0, FORMAT)
            //$$ .withPrimitiveTopology(PrimitiveTopology.QUADS)
            //#else
            //$$ .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            //$$ .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            //$$ .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            //$$ .withVertexFormat(FORMAT, VertexFormat.Mode.QUADS)
            //#endif
            //$$ .withCull(false)
            //$$ .build();

    //$$ private LegacyGpuSdfRenderer() {}

    //$$ /** Must execute during client bootstrap before the first shader-resource reload. */
    //$$ public static void initialize() {
        //$$ RenderPipelinesAccess.ddsTweak$getPipelinesByLocation().put(PIPELINE.getLocation(), PIPELINE);
    //$$ }

    //$$ static void draw(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2,
                     //$$ float radius, int argb, boolean border) {
        //$$ drawMode(gui, x1, y1, x2, y2, radius, argb, border ? 1 : 0);
    //$$ }

    //$$ static void drawTriangle(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2,
    //$$         boolean right, int argb) {
        //$$ drawMode(gui, x1, y1, x2, y2, 0, argb, right ? 3 : 2);
    //$$ }

    //$$ static void drawAccent(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2,
    //$$         float radius, int shift, int argb) {
        //$$ drawMode(gui, x1, y1, x2, y2, radius, argb, 4 + shift);
    //$$ }

    //$$ private static void drawMode(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2,
    //$$         float radius, int argb, int mode) {
        //$$ if (x2 <= x1 || y2 <= y1 || (argb >>> 24) == 0) return;
        //$$ int r = Math.round(Math.max(0, Math.min(radius, Math.min(x2 - x1, y2 - y1) / 2.0f)) * 2);
        //$$ LegacyGpuSdfAccess access = (LegacyGpuSdfAccess) gui;
        //$$ access.ddsTweak$guiRenderState().addGuiElement(new SdfRect(
                //$$ new Matrix3x2f(gui.pose()), access.ddsTweak$scissorArea(),
                //$$ x1, y1, x2, y2, r, argb, mode));
    //$$ }

    //$$ private static final class SdfRect implements GuiElementRenderState {
        //$$ private final Matrix3x2fc pose;
        //$$ private final ScreenRectangle scissor, bounds;
        //$$ private final int x1, y1, x2, y2, radius, argb;
        //$$ private final int mode;

        //$$ SdfRect(Matrix3x2fc pose, ScreenRectangle scissor, int x1, int y1,
                //$$ int x2, int y2, int radius, int argb, int mode) {
            //$$ this.pose = pose;
            //$$ this.scissor = scissor;
            //$$ this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            //$$ this.radius = radius; this.argb = argb; this.mode = mode;
            //$$ ScreenRectangle area = new ScreenRectangle(x1, y1, x2-x1, y2-y1).transformMaxBounds(pose);
            //$$ this.bounds = scissor == null ? area : scissor.intersection(area);
        //$$ }

        //$$ @Override public RenderPipeline pipeline() { return PIPELINE; }
        //$$ @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        //$$ @Override public ScreenRectangle scissorArea() { return scissor; }
        //$$ @Override public ScreenRectangle bounds() { return bounds; }

        //$$ @Override public void buildVertices(VertexConsumer vertices) {
            //$$ float hx = (x2 - x1) * 0.5F, hy = (y2 - y1) * 0.5F;
            //$$ vertex(vertices, x1, y1, -hx, -hy);
            //$$ vertex(vertices, x1, y2, -hx, hy);
            //$$ vertex(vertices, x2, y2, hx, hy);
            //$$ vertex(vertices, x2, y1, hx, -hy);
        //$$ }

        //$$ private void vertex(VertexConsumer vertices, int x, int y, float localX, float localY) {
            //$$ vertices.addVertexWith2DPose(pose, x, y).setColor(argb)
                    //$$ .setUv(localX, localY).setUv1(radius, mode)
                    //$$ .setUv2(x2 - x1, y2 - y1);
        //$$ }
    //$$ }
//$$ }

//#endif
//#endif