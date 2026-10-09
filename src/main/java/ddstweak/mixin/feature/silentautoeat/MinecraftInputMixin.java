package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 11904
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(Minecraft.class)
public abstract class MinecraftInputMixin {
    @Inject(method="startUseItem",at=@At("HEAD"))
    private void ddstweak$silentAutoEatUse(CallbackInfo ci){
        SilentAutoEat.onExplicitPlayerAction((Minecraft)(Object)this);
    }

    //#if MC >= 11904
    @Inject(method="startAttack",at=@At("HEAD"))
    private void ddstweak$silentAutoEatAttack(CallbackInfoReturnable<Boolean> cir){
        SilentAutoEat.onExplicitPlayerAction((Minecraft)(Object)this);
    }
    //#else
    //$$ @Inject(method="startAttack",at=@At("HEAD"))
    //$$ private void ddstweak$silentAutoEatAttack(CallbackInfo ci){
    //$$     SilentAutoEat.onExplicitPlayerAction((Minecraft)(Object)this);
    //$$ }
    //#endif

    @Redirect(
            method="handleKeybinds",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"),
            require=0
    )
    private void ddstweak$silentAutoEatKeepUsing(MultiPlayerGameMode gameMode,Player player){
        if(!(player instanceof LocalPlayer)||!SilentAutoEat.ownsUse((LocalPlayer)player))
            gameMode.releaseUsingItem(player);
    }
}
