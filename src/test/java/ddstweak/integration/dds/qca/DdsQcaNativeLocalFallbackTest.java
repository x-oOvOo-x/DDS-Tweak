package ddstweak.integration.dds.qca;

import ddstweak.integration.dds.playerinventory.PlayerInventoryLayouts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class DdsQcaNativeLocalFallbackTest {
    @Test
    void knownProviderLayoutsDistinguishStorageFromControls() {
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("gca", PlayerInventoryLayouts.GCA_INVENTORY_V1, 90, 17));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("gca", PlayerInventoryLayouts.GCA_INVENTORY_V1, 90, 7));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("gca", PlayerInventoryLayouts.GCA_INVENTORY_V1, 90, 4));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("gca", PlayerInventoryLayouts.GCA_INVENTORY_V1, 90, 18));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("gca", PlayerInventoryLayouts.GCA_ENDER_3_V1, 90, 26));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("gca", PlayerInventoryLayouts.GCA_ENDER_3_V1, 90, 27));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", PlayerInventoryLayouts.ORG_INVENTORY_V1, 90, 40));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", PlayerInventoryLayouts.ORG_INVENTORY_V1, 90, 39));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", PlayerInventoryLayouts.ORG_INVENTORY_V1, 90, 54));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", PlayerInventoryLayouts.ORG_CONTROLS_V1, 90, 17));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", PlayerInventoryLayouts.ORG_CONTROLS_V1, 90, 7));
        assertTrue(DdsQcaNativeSlotLayout.storageSlot("org", PlayerInventoryLayouts.ORG_CONTROLS_V1, 90, 18));
    }

    @Test
    void rejectsUnknownOrDimensionMismatchedLayouts() {
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("gca", PlayerInventoryLayouts.ORG_INVENTORY_V1, 90, 18));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("org", "unknown", 90, 54));
        assertFalse(DdsQcaNativeSlotLayout.storageSlot("gca", PlayerInventoryLayouts.GCA_INVENTORY_V1, 89, 54));
    }

    @Test
    void nativeClassificationDoesNotRequireExtensionPacket() throws IOException {
        String source = Files.readString(root().resolve(
                "src/main/java/ddstweak/integration/dds/qca/DdsQcaNativeMenuSlots.java"));
        String entry = Files.readString(root().resolve(
                "src/main/java/ddstweak/mixin/fakeplayer/MenuScreensMixin.java"));

        assertTrue(source.contains("extension != null"));
        assertTrue(source.contains(": localContext"));
        assertTrue(source.contains("OrgPlayerInventoryOpenIntent.isOfflineMenu"));
        assertTrue(source.contains("localContext = null;"));
        assertTrue(entry.contains("DdsQcaNativeMenuSlots.observeLocal(type,containerId,title)"));
        assertTrue(entry.contains("DdsQcaNativeMenuSlots.rememberLocal(type,containerId,context)"));
    }

    private static Path root() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.exists(p.resolve("settings.json"))) return p;
        }
        throw new IllegalStateException("repository root not found");
    }
}
