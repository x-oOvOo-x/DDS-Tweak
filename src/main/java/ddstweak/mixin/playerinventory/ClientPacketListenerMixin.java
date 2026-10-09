package ddstweak.mixin.playerinventory;

import ddstweak.integration.org.playerinventory.OrgPlayerInventoryOpenIntent;
import ddstweak.playerinventory.PlayerInventoryCommandStateObserver;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
//#if MC >= 11900
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

/** Tracks Org player-inventory command intent. Connection state resets through DdsClientSessionHooks. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    //#if MC >= 11900
    @Inject(method="sendCommand",at=@At("HEAD"))
    private void ddstweak$rememberOrgPlayerInventoryIntent(String command,CallbackInfo ci){
        OrgPlayerInventoryOpenIntent.observeCommand(command);
        //#if MC >= 12111
        PlayerInventoryCommandStateObserver.observeCommand(command);
        //#endif
    }
    //#endif
}
