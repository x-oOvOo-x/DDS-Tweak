package ddstweak.mixin.playerinventory;

import ddstweak.integration.org.playerinventory.OrgPlayerInventoryOpenIntent;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
//#if MC < 11900
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

/** Legacy pre-signed-chat command observer. Empty on 1.19+ where ClientPacketListener owns command sending. */
@Mixin(LocalPlayer.class)
public abstract class LegacyLocalPlayerCommandMixin {
    //#if MC < 11900
    //$$ @Inject(method="chat",at=@At("HEAD"))
    //$$ private void ddstweak$rememberLegacyOrgPlayerInventoryIntent(String message,CallbackInfo ci){
    //$$     if(message!=null&&message.startsWith("/")&&message.length()>1)OrgPlayerInventoryOpenIntent.observeCommand(message.substring(1));
    //$$ }
    //#endif
}
