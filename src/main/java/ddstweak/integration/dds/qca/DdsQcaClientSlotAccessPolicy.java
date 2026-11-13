package ddstweak.integration.dds.qca;

import ddstweak.platform.minecraft.MinecraftCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.inventory.Slot;

/**
 * Fail-closed client slot eligibility for dedicated QCA storage interactions.
 * 未确认槽位可操作时默认拒绝 QCA 存储交互。
 */
public final class DdsQcaClientSlotAccessPolicy {
    private DdsQcaClientSlotAccessPolicy(){}
    public static boolean canDirectStorage(AbstractContainerMenu menu, Slot slot, Player player) {
        if (menu == null || slot == null || player == null) return false;
        DdsQcaSlotRoleProvider.Role role = role(menu, slot);
        // Explicit mirror roles and native Provider decisions outrank vanilla fallbacks.
        if (role != DdsQcaSlotRoleProvider.Role.UNKNOWN) return permitsExplicitRole(role);
        Boolean nativeRole = DdsQcaNativeMenuSlots.storageRole(menu, slot);
        if (nativeRole != null) return nativeRole;
        if (slot.container == MinecraftCompat.playerInventory(player)) return true;

        // Only exact vanilla storage menu classes: no subclass or custom-menu fallback.
        // Known Provider slot decisions above remain authoritative.
        if (isVanillaStorageMenu(menu.getClass()))
            return permitsVanillaStorageSlot(menu.getClass(), menu.slots.contains(slot),
                    safeSourceSlot(menu, slot, player));
        return isVanillaEnderScreen();
    }

    static boolean permitsExplicitRole(DdsQcaSlotRoleProvider.Role role) {
        return role == DdsQcaSlotRoleProvider.Role.TARGET_STORAGE
                || role == DdsQcaSlotRoleProvider.Role.TARGET_OFFHAND
                || role == DdsQcaSlotRoleProvider.Role.VIEWER_INVENTORY
                || role == DdsQcaSlotRoleProvider.Role.ENDER_STORAGE;
    }

    private static boolean isVanillaStorageMenu(Class<?> menuClass) {
        return menuClass == ChestMenu.class || menuClass == HopperMenu.class
                || menuClass == DispenserMenu.class || menuClass == ShulkerBoxMenu.class;
    }

    static boolean permitsVanillaStorageSlot(Class<?> menuClass, boolean member, boolean safeSource) {
        return isVanillaStorageMenu(menuClass) && member && safeSource;
    }
    public static boolean safeSourceSlot(AbstractContainerMenu menu,Slot slot,Player player){if(menu==null||slot==null||player==null||!slot.mayPickup(player))return false;DdsQcaSlotRoleProvider.Role role=role(menu,slot);if(role==DdsQcaSlotRoleProvider.Role.CONTROL||role==DdsQcaSlotRoleProvider.Role.DISABLED)return false;return slot.getItem().isEmpty()||slot.mayPlace(slot.getItem());}
    public static DdsQcaSlotRoleProvider.Role role(AbstractContainerMenu menu,Slot slot){
        if(!(menu instanceof DdsQcaSlotRoleProvider)||slot==null)return DdsQcaSlotRoleProvider.Role.UNKNOWN;
        DdsQcaSlotRoleProvider provider=(DdsQcaSlotRoleProvider)menu;
        int id=menu.slots.indexOf(slot);
        return id<0?DdsQcaSlotRoleProvider.Role.UNKNOWN:provider.ddsTweak$qcaRole(id);
    }
    private static boolean isVanillaEnderScreen(){Screen screen=MinecraftCompat.screen(Minecraft.getInstance());return screen!=null&&"container.enderchest".equals(MinecraftCompat.translationKey(screen.getTitle()));}
}
