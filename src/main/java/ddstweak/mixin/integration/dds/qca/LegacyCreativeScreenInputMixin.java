package ddstweak.mixin.integration.dds.qca;

import ddstweak.integration.dds.qca.DdsQcaMouseInputBridge;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
//#if MC < 11700
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

/**
 * Before 1.17, the creative inventory overrides mouse input and can bypass
 * AbstractContainerScreen hooks. Forward handled gestures to the same QCA state.
 * 将旧版创造界面输入交给统一 QCA 处理器。
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class LegacyCreativeScreenInputMixin {
    //#if MC < 11700
    //$$ @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$creativeClicked(double x,double y,int button,CallbackInfoReturnable<Boolean> cir) {
    //$$     if(((DdsQcaMouseInputBridge)(Object)this).ddstweak$qcaMouseClicked(x,y,button))cir.setReturnValue(true);
    //$$ }
    //$$ @Inject(method="mouseDragged",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$creativeDragged(double x,double y,int button,double dx,double dy,CallbackInfoReturnable<Boolean> cir) {
    //$$     if(((DdsQcaMouseInputBridge)(Object)this).ddstweak$qcaMouseDragged(x,y,button))cir.setReturnValue(true);
    //$$ }
    //$$ @Inject(method="mouseReleased",at=@At("HEAD"),cancellable=true)
    //$$ private void ddstweak$creativeReleased(double x,double y,int button,CallbackInfoReturnable<Boolean> cir) {
    //$$     if(((DdsQcaMouseInputBridge)(Object)this).ddstweak$qcaMouseReleased(button))cir.setReturnValue(true);
    //$$ }
    //#endif
}
