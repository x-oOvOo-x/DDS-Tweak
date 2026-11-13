package ddstweak.integration.dds.playerinventory.screen;

import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Type marker for third-party inventory integrations; behavior stays in the unified base screen.
 * 为可选集成保留玩家背包界面类型标记。
 */
public final class UnifiedPlayerInventoryMainScreen extends UnifiedPlayerInventoryScreen {
    public UnifiedPlayerInventoryMainScreen(UnifiedPlayerInventoryMirrorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
