package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class HideBlockOutlineContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {
        Path f=ROOT.resolve(p);
        return Files.exists(f)?Files.readString(f):"";
    }

    @Test void featureIsOptInVisualSetting() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(configs.contains("HIDE_BLOCK_OUTLINE=MalilibCompat.bool(\"hideBlockOutline\",false,VISUAL_KEY)"));
        assertTrue(configs.contains("CHUNK_CENTER_INDICATOR"));
        assertTrue(configs.contains("HIDE_BLOCK_OUTLINE"));
    }

    @Test void onlyCancelsOutlineRenderingAndSneakRestoresIt() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/hideblockoutline/BlockOutlineVisibilityMixin.java");
        String json=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(mixin.contains("@Mixin(LevelRenderer.class)"));
        assertTrue(mixin.contains("method=\"renderHitOutline\""));
        assertTrue(mixin.contains("method=\"renderBlockOutline\""));
        assertTrue(mixin.contains("method=\"submitBlockOutline\""));
        assertTrue(mixin.contains("Configs.Visual.HIDE_BLOCK_OUTLINE.getBooleanValue()"));
        assertTrue(mixin.contains("!minecraft.player.isShiftKeyDown()"));
        assertTrue(mixin.contains("!HelpfulHitboxesFeature.shouldRevealOutline()"));
        assertTrue(mixin.contains("ci.cancel()"));
        assertFalse(mixin.contains("VoxelShape"));
        assertFalse(mixin.contains("getShape("));
        assertTrue(json.contains("feature.hideblockoutline.BlockOutlineVisibilityMixin"));
    }
}
