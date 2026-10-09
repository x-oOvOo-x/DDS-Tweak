package ddstweak.integration.dds.qca;

import ddstweak.platform.minecraft.MinecraftCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.inventory.Slot;

/** Fail-closed client slot eligibility for dedicated QCA storage interactions. */
public final class DdsQcaClientSlotAccessPolicy {
    private DdsQcaClientSlotAccessPolicy(){}
    public static boolean canDirectStorage(AbstractContainerMenu menu,Slot slot,Player player){if(menu==null||slot==null||player==null)return false;DdsQcaSlotRoleProvider.Role role=role(menu,slot);if(role!=DdsQcaSlotRoleProvider.Role.UNKNOWN)return role==DdsQcaSlotRoleProvider.Role.TARGET_STORAGE||role==DdsQcaSlotRoleProvider.Role.VIEWER_INVENTORY||role==DdsQcaSlotRoleProvider.Role.ENDER_STORAGE;if(slot.container==MinecraftCompat.playerInventory(player))return true;return isVanillaEnderScreen();}
    public static boolean safeSourceSlot(AbstractContainerMenu menu,Slot slot,Player player){if(menu==null||slot==null||player==null||!slot.mayPickup(player))return false;DdsQcaSlotRoleProvider.Role role=role(menu,slot);if(role==DdsQcaSlotRoleProvider.Role.CONTROL||role==DdsQcaSlotRoleProvider.Role.DISABLED)return false;return slot.getItem().isEmpty()||slot.mayPlace(slot.getItem());}
    public static DdsQcaSlotRoleProvider.Role role(AbstractContainerMenu menu,Slot slot){
        if(!(menu instanceof DdsQcaSlotRoleProvider)||slot==null)return DdsQcaSlotRoleProvider.Role.UNKNOWN;
        DdsQcaSlotRoleProvider provider=(DdsQcaSlotRoleProvider)menu;
        int id=menu.slots.indexOf(slot);
        return id<0?DdsQcaSlotRoleProvider.Role.UNKNOWN:provider.ddsTweak$qcaRole(id);
    }
    private static boolean isVanillaEnderScreen(){Screen screen=MinecraftCompat.screen(Minecraft.getInstance());return screen!=null&&"container.enderchest".equals(MinecraftCompat.translationKey(screen.getTitle()));}
}
