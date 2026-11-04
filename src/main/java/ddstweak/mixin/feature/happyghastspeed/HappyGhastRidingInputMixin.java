package ddstweak.mixin.feature.happyghastspeed;

//#if MC >= 12106
import ddstweak.feature.happyghastspeed.HappyGhastRidingSpeedFeature;
import ddstweak.feature.happyghastspeed.HappyGhastRidingSpeedTarget;
//#if MC >= 12109
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
//#else
//$$ import net.minecraft.world.entity.animal.HappyGhast;
//#endif
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HappyGhast.class)
public abstract class HappyGhastRidingInputMixin implements HappyGhastRidingSpeedTarget {
    @Inject(method="getRiddenInput",at=@At("RETURN"),cancellable=true)
    private void ddstweak$scaleRiddenInput(Player controller,Vec3 selfInput,CallbackInfoReturnable<Vec3> cir){
        Vec3 input=cir.getReturnValue();
        if(input!=null)cir.setReturnValue(HappyGhastRidingSpeedFeature.scaleRiddenInput(controller,input));
    }
}
//#else
//$$ public final class HappyGhastRidingInputMixin {}
//#endif
