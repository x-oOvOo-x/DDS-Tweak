package ddstweak.integration.dds.qca;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DdsQcaNativeSlotLayoutTest {
    @Test void normalOrgInventoryExcludesEquipmentAndPlaceholders() {
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_inventory_v1", 90, 0));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_inventory_v1", 90, 35));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_inventory_v1", 90, 36));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_inventory_v1", 90, 40));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_inventory_v1", 90, 39));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_inventory_v1", 90, 53));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_inventory_v1", 90, 54));
    }

    @Test void providerControlButtonsNeverBecomeStorage() {
        for (String layout : new String[]{"gca_player_inventory_v1", "org_player_inventory_controls_v1"}) {
            String provider = layout.startsWith("gca_") ? "gca" : "org";
            for (int i = 0; i < 18; i++)
                if (i == 7) assertTrue(DdsQcaNativeSlotLayout.storageSlot(provider, layout, 90, i), layout + " offhand");
                else assertFalse(DdsQcaNativeSlotLayout.storageSlot(provider, layout, 90, i), layout + " control " + i);
            assertTrue(DdsQcaNativeSlotLayout.storageSlot(provider, layout, 90, 18));
            assertTrue(DdsQcaNativeSlotLayout.storageSlot(provider, layout, 90, 53));
            assertTrue(DdsQcaNativeSlotLayout.storageSlot(provider, layout, 90, 89));
        }
    }

    @Test void gcaEnderAndControllerHaveDistinctProtectedRegions() {
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("gca", "gca_player_ender_3_v1", 90, 26));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("gca", "gca_player_ender_3_v1", 90, 27));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("gca", "gca_player_ender_3_v1", 90, 53));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("gca", "gca_player_controller_3_v1", 63, 0));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("gca", "gca_player_controller_3_v1", 63, 27));
    }

    @Test void unknownOrMismatchedLayoutsFailClosed() {
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("unknown", "org_player_inventory_v1", 90, 20));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "gca_player_inventory_v1", 90, 20));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "unknown", 90, 20));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_inventory_v1", 63, 20));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_ender_3_v1", 90, 0));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_ender_3_v1", 63, 26));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "org_player_ender_3_v1", 63, 63));
    }
}
