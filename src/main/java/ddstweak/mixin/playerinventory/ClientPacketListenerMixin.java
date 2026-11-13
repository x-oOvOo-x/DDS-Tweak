package ddstweak.mixin.playerinventory;

import ddstweak.integration.org.playerinventory.OrgPlayerInventoryOpenIntent;
import ddstweak.integration.provider.ProviderPlayerInventoryScreenCoordinator;
import ddstweak.playerinventory.PlayerInventoryCommandStateObserver;
import net.minecraft.client.multiplayer.ClientPacketListener;
//#if MC >= 12111
//$$ import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
//#endif
import org.spongepowered.asm.mixin.Mixin;
//#if MC >= 11900
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

/**
 * Tracks Org player-inventory command intent. Connection state resets through DdsClientSessionHooks.
 * 跟踪 Org 背包导航意图，不授予菜单权限。
 */
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

    //#if MC >= 12111
    // Vanilla has already installed the Provider menu at this point, but no frame
    // has rendered yet. Use the exact menu/command/Extension context when available;
    // the containerTick fallback still handles metadata delivered after OpenScreen.
    //$$ @Inject(method="handleOpenScreen", at=@At("TAIL"))
    //$$ private void ddstweak$upgradeProviderOnOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
    //$$     ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent();
    //$$ }
    //#endif
}
