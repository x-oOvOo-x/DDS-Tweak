package ddstweak.mixin.gui;

//#if MC >= 260100
//#if MC < 260300

//$$ import ddstweak.gui.LegacyGpuSdfAccess;
//$$ import java.util.ArrayDeque;
//$$ import java.util.Deque;
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//$$ import net.minecraft.client.gui.navigation.ScreenRectangle;
//$$ import net.minecraft.client.renderer.state.gui.GuiRenderState;
//$$ import org.joml.Matrix3x2fStack;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//$$ /** Mirrors the native clip stack without exposing the extractor's private nested class. */
//$$ @Mixin(GuiGraphicsExtractor.class)
//$$ public abstract class SdfGuiRenderStateMixin implements LegacyGpuSdfAccess {
    //$$ @Shadow private GuiRenderState guiRenderState;
    //$$ @Shadow private Matrix3x2fStack pose;
    //$$ @Unique private Deque<ScreenRectangle> ddsTweak$sdfClip;

    //$$ @Override public GuiRenderState ddsTweak$guiRenderState() { return guiRenderState; }
    //$$ @Override public ScreenRectangle ddsTweak$scissorArea() { return ddsTweak$sdfClip == null ? null : ddsTweak$sdfClip.peek(); }

    //$$ @Inject(method = "enableScissor", at = @At("TAIL"))
    //$$ private void ddsTweak$pushSdfClip(int x0, int y0, int x1, int y1, CallbackInfo ci) {
        //$$ if (ddsTweak$sdfClip == null) ddsTweak$sdfClip = new ArrayDeque<>();
        //$$ ScreenRectangle clip = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformAxisAligned(pose);
        //$$ ScreenRectangle parent = ddsTweak$sdfClip.peek();
        //$$ if (parent != null) {
            //$$ ScreenRectangle intersection = parent.intersection(clip);
            //$$ clip = intersection == null ? new ScreenRectangle(0, 0, 0, 0) : intersection;
        //$$ }
        //$$ ddsTweak$sdfClip.push(clip);
    //$$ }

    //$$ @Inject(method = "disableScissor", at = @At("TAIL"))
    //$$ private void ddsTweak$popSdfClip(CallbackInfo ci) {
        //$$ if (ddsTweak$sdfClip != null && !ddsTweak$sdfClip.isEmpty()) ddsTweak$sdfClip.pop();
    //$$ }
//$$ }

//#endif
//#endif