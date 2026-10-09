package ddstweak.integration.dds.playerinventory.screen;

import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** IPN placement marker only; real/offline inventory behavior and appearance are inherited unchanged. */
public final class UnifiedPlayerInventoryRealScreen extends UnifiedPlayerInventoryScreen {
    public UnifiedPlayerInventoryRealScreen(UnifiedPlayerInventoryMirrorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
