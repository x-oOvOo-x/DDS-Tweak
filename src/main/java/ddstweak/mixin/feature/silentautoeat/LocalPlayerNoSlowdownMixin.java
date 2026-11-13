package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// Skips item-use slowdown only for Auto Eat-owned offhand use.
// 仅对自动进食持有的副手使用跳过减速。

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerNoSlowdownMixin {
    private boolean ddstweak$ignoreEatSlowdown(){
        return SilentAutoEat.bypassMovementSlowdown((LocalPlayer)(Object)this);
    }

    //#if MC >= 12111
    @Inject(method="itemUseSpeedMultiplier",at=@At("HEAD"),cancellable=true)
    private void ddstweak$normalEatMovement(CallbackInfoReturnable<Float> cir){
        if(ddstweak$ignoreEatSlowdown())cir.setReturnValue(1.0F);
    }

    @Inject(method="isSlowDueToUsingItem",at=@At("HEAD"),cancellable=true)
    private void ddstweak$allowAutoEatSprint(CallbackInfoReturnable<Boolean> cir){
        if(ddstweak$ignoreEatSlowdown())cir.setReturnValue(false);
    }
    //#elseif MC >= 12105
    //$$ @ModifyConstant(method="modifyInput",constant=@Constant(floatValue=0.2F))
    //$$ private float ddstweak$normalEatMovement(float vanillaFactor){
    //$$     return ddstweak$ignoreEatSlowdown()?1.0F:vanillaFactor;
    //$$ }
    //#else
    //$$ @ModifyConstant(method="aiStep",constant=@Constant(floatValue=0.2F,ordinal=0))
    //$$ private float ddstweak$normalEatSideways(float vanillaFactor){
    //$$     return ddstweak$ignoreEatSlowdown()?1.0F:vanillaFactor;
    //$$ }
    //$$ @ModifyConstant(method="aiStep",constant=@Constant(floatValue=0.2F,ordinal=1))
    //$$ private float ddstweak$normalEatForward(float vanillaFactor){
    //$$     return ddstweak$ignoreEatSlowdown()?1.0F:vanillaFactor;
    //$$ }
    //#endif
}
