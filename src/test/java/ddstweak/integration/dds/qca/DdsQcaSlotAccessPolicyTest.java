package ddstweak.integration.dds.qca;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsQcaSlotAccessPolicyTest {
    private static Path root(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root not found");}
    private static String read(String path)throws IOException{return Files.readString(root().resolve(path));}
    @Test void unifiedInventoryExposesStorageNotEquipment()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");assertTrue(code.contains("case EQUIPMENT:return Role.TARGET_EQUIPMENT"));assertTrue(code.contains("if(slot==offhand)return Role.TARGET_OFFHAND"));assertTrue(code.contains("case MAIN:case HOTBAR:return Role.TARGET_STORAGE"));assertTrue(code.contains("case VIEWER_MAIN:case VIEWER_HOTBAR:return Role.VIEWER_INVENTORY"));}
    @Test void unifiedEnderExposesEnderAndViewerRegions()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");assertTrue(code.contains("case ENDER_STORAGE:return Role.ENDER_STORAGE"));assertTrue(code.contains("case VIEWER_MAIN:case VIEWER_HOTBAR:return Role.VIEWER_INVENTORY"));}
    @Test void explicitSlotRolesDenyControlsDisabledAndEquipment() {
        for (DdsQcaSlotRoleProvider.Role role : new DdsQcaSlotRoleProvider.Role[]{
                DdsQcaSlotRoleProvider.Role.TARGET_STORAGE,
                DdsQcaSlotRoleProvider.Role.TARGET_OFFHAND,
                DdsQcaSlotRoleProvider.Role.VIEWER_INVENTORY,
                DdsQcaSlotRoleProvider.Role.ENDER_STORAGE
        }) assertTrue(DdsQcaClientSlotAccessPolicy.permitsExplicitRole(role), role.name());
        for (DdsQcaSlotRoleProvider.Role role : new DdsQcaSlotRoleProvider.Role[]{
                DdsQcaSlotRoleProvider.Role.TARGET_EQUIPMENT,
                DdsQcaSlotRoleProvider.Role.CONTROL,
                DdsQcaSlotRoleProvider.Role.DISABLED,
                DdsQcaSlotRoleProvider.Role.UNKNOWN
        }) assertFalse(DdsQcaClientSlotAccessPolicy.permitsExplicitRole(role), role.name());
    }

    @Test void vanillaStorageFallbackRequiresExactMenuAndSafeMemberSlot() {
        Class<?>[] supported = {
                net.minecraft.world.inventory.ChestMenu.class,
                net.minecraft.world.inventory.HopperMenu.class,
                net.minecraft.world.inventory.DispenserMenu.class,
                net.minecraft.world.inventory.ShulkerBoxMenu.class
        };
        for (Class<?> menuClass : supported) {
            assertTrue(DdsQcaClientSlotAccessPolicy.permitsVanillaStorageSlot(menuClass, true, true),
                    menuClass.getSimpleName());
            assertFalse(DdsQcaClientSlotAccessPolicy.permitsVanillaStorageSlot(menuClass, false, true),
                    menuClass.getSimpleName() + " foreign slot");
            assertFalse(DdsQcaClientSlotAccessPolicy.permitsVanillaStorageSlot(menuClass, true, false),
                    menuClass.getSimpleName() + " unsafe slot");
        }
        for (Class<?> menuClass : new Class<?>[]{
                net.minecraft.world.inventory.CraftingMenu.class,
                net.minecraft.world.inventory.FurnaceMenu.class,
                net.minecraft.world.inventory.AbstractContainerMenu.class,
                Object.class
        }) assertFalse(DdsQcaClientSlotAccessPolicy.permitsVanillaStorageSlot(menuClass, true, true),
                menuClass.getSimpleName());
        assertFalse(DdsQcaClientSlotAccessPolicy.permitsVanillaStorageSlot(null, true, true));
    }
}
