package ddstweak.mixin.feature.disableendflash;

//#if MC >= 12109
import ddstweak.feature.disableendflash.DisableEndFlashFeature;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Suppresses End flash rendering only when its toggle is enabled.
 * 仅在配置开启时跳过末地闪烁渲染。
 */

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Inject(method="renderEndFlash",at=@At("HEAD"),cancellable=true)
    private void ddstweak$disableEndFlash(CallbackInfo ci){
        if(DisableEndFlashFeature.enabled())ci.cancel();
    }
}
//#else
//$$ public abstract class SkyRendererMixin {}
//#endif
