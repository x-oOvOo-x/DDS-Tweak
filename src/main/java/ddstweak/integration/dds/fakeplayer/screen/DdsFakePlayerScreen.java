package ddstweak.integration.dds.fakeplayer.screen;

import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * DDS compatibility shell retaining the inventory-specific IPN opt-out while the UI is owned by the unified screen.
 * 保留 DDS 兼容界面，渲染仍由统一界面负责。
 */
@org.anti_ad.mc.ipn.api.IPNIgnore
public final class DdsFakePlayerScreen extends UnifiedPlayerInventoryScreen{
    public DdsFakePlayerScreen(UnifiedPlayerInventoryMirrorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
}
