package ddstweak.integration.provider;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class EnhancedStorageScreenContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath(),JAVA=ROOT.resolve("src/main/java");
    private static String read(String path)throws IOException{return Files.readString(JAVA.resolve(path));}

    @Test void replacementStorageScreenPreservesVanillaItemTooltipStage()throws IOException{
        String screen=read("ddstweak/integration/provider/EnhancedStorageScreen.java");
        assertTrue(screen.contains("super.render(g,mouseX,mouseY,partialTick);renderTooltip(g,mouseX,mouseY)"));
        assertTrue(screen.contains("super.render(mouseX,mouseY,partialTick);renderTooltip(mouseX,mouseY)"));
        assertTrue(screen.contains("super.extractRenderState(g,mouseX,mouseY,partialTick)"));
    }

    @Test void extractorScreensPreserveVanillaBackgroundDimming()throws IOException{
        String storage=read("ddstweak/integration/provider/EnhancedStorageScreen.java");
        String player=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        assertTrue(storage.contains("super.extractBackground(g,mouseX,mouseY,partialTick);renderStorageBackground(g)"));
        assertTrue(player.contains("super.extractBackground(g,mouseX,mouseY,partialTick);if(isEnder())"));
    }

    @Test void genericStorageReplacementRemainsExplicitlyBoundToEnhancedUi()throws IOException{
        String mixin=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        assertTrue(mixin.contains("Configs.Tweaks.ENHANCED_INVENTORY_GUI.getBooleanValue()"));
        assertTrue(mixin.contains("new EnhancedStorageScreen"));
        assertTrue(mixin.contains("MenuType.GENERIC_9x6"));
        assertTrue(mixin.contains("MenuType.GENERIC_9x3"));
        assertTrue(mixin.contains("MenuType.SHULKER_BOX"));
    }
}
