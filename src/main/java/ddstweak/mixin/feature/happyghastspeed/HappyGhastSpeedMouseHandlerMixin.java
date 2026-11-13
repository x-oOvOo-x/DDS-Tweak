package ddstweak.mixin.feature.happyghastspeed;

//#if MC >= 12106
import ddstweak.feature.happyghastspeed.HappyGhastRidingSpeedFeature;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Consumes the wheel only while a controllable Happy Ghast uses speed adjustment.
 * 仅在操控快乐恶魂调整速度时拦截滚轮。
 */

@Mixin(MouseHandler.class)
public abstract class HappyGhastSpeedMouseHandlerMixin {
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void ddstweak$adjustHappyGhastSpeed(long window,double horizontal,double vertical,CallbackInfo ci){
        if(HappyGhastRidingSpeedFeature.handleScroll(vertical))ci.cancel();
    }
}
//#else
//$$ public final class HappyGhastSpeedMouseHandlerMixin {}
//#endif
