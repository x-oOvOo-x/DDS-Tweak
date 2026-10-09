package ddstweak.mixin.feature.dropconfirmation;

import ddstweak.feature.dropconfirmation.DropConfirmationGuard;
import net.minecraft.client.player.LocalPlayer;
//#if MC >= 260300
//$$ import net.minecraft.client.multiplayer.MultiPlayerGameMode;
//#endif
//#if MC < 11500
//$$ import net.minecraft.world.entity.item.ItemEntity;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC >= 260300
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#else
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

//#if MC >= 260300
//$$ @Mixin(MultiPlayerGameMode.class)
//$$ public abstract class WorldDropConfirmationMixin {
//$$     @Inject(method="dropItem",at=@At("HEAD"),cancellable=true)
//$$     private void ddstweak$confirmWorldDrop(LocalPlayer player,boolean entireStack,CallbackInfo ci){
//$$         if(DropConfirmationGuard.shouldCancel(player,entireStack))ci.cancel();
//$$     }
//$$ }
//#else
@Mixin(LocalPlayer.class)
public abstract class WorldDropConfirmationMixin {
    @Inject(method="drop",at=@At("HEAD"),cancellable=true)
    private void ddstweak$confirmWorldDrop(
            boolean entireStack,
            //#if MC < 11500
            //$$ CallbackInfoReturnable<ItemEntity> cir
            //#else
            CallbackInfoReturnable<Boolean> cir
            //#endif
    ){
        if(DropConfirmationGuard.shouldCancel((LocalPlayer)(Object)this,entireStack)){
            //#if MC < 11500
            //$$ cir.setReturnValue(null);
            //#else
            cir.setReturnValue(false);
            //#endif
        }
    }
}
//#endif
