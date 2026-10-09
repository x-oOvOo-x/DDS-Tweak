package ddstweak.integration.dds.playerinventory.screen;

import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Type marker for third-party inventory integrations; rendering and behavior stay in the unified base screen. */
public final class UnifiedPlayerEnderChestScreen extends UnifiedPlayerInventoryScreen {
    public UnifiedPlayerEnderChestScreen(UnifiedPlayerInventoryMirrorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
