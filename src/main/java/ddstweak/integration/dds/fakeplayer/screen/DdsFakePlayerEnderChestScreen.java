package ddstweak.integration.dds.fakeplayer.screen;

import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** DDS compatibility shell; Ender Chest intentionally keeps its previous IPN behavior. */
public final class DdsFakePlayerEnderChestScreen extends UnifiedPlayerInventoryScreen{
    public DdsFakePlayerEnderChestScreen(UnifiedPlayerInventoryMirrorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
}
