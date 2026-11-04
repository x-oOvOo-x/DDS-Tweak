package ddstweak.integration.provider;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ProviderFirstFrameUpgradeContractTest {
    private static final Path ROOT = root();

    @Test void trustedProviderUpgradeIsAttemptedAtOpenScreenBeforeTickFallback() throws IOException {
        String mixin = read("src/main/java/ddstweak/mixin/playerinventory/ClientPacketListenerMixin.java");
        String fallback = read("src/main/java/ddstweak/mixin/fakeplayer/AbstractContainerScreenMixin.java");

        assertTrue(mixin.contains("//#if MC >= 12111"));
        assertTrue(mixin.contains("method=\"handleOpenScreen\""));
        assertTrue(mixin.contains("at=@At(\"TAIL\")"));
        assertTrue(mixin.contains("ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent()"));
        assertTrue(fallback.contains("method=\"containerTick\""));
        assertTrue(fallback.contains("ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent()"));
    }

    @Test void earlyUpgradeRetainsExactProviderAndSessionGuards() throws IOException {
        String coordinator = read("src/main/java/ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");

        assertTrue(coordinator.contains("Configs.Tweaks.ENHANCED_INVENTORY_GUI.getBooleanValue()"));
        assertTrue(coordinator.contains("source.getMenu().containerId!=minecraft.player.containerMenu.containerId"));
        assertTrue(coordinator.contains("OrgPlayerInventoryOpenIntent.isOfflineMenu(type,containerId,title)"));
        assertTrue(coordinator.contains("OrgPlayerInventoryAdapter.tryContext(sourceMenu,title)"));
        assertTrue(coordinator.contains("tryStructuralReopen(sourceMenu,context)"));
        assertTrue(coordinator.contains("source.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu"));
        assertTrue(coordinator.contains("return false"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static Path root() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.exists(p.resolve("settings.json")) && Files.isDirectory(p.resolve("src/main")))
                return p;
        }
        throw new IllegalStateException("Repository root not found");
    }
}
