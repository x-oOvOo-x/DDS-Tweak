package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class ChunkCenterIndicatorContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}

    @Test void visualOptionIsDefaultOnAndFollowsChunkBorderRenderer() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String mixins=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(configs.contains("CHUNK_CENTER_INDICATOR=MalilibCompat.bool(\"chunkCenterIndicator\",true,VISUAL_KEY)"));
        assertTrue(mixins.contains("feature.chunkcenterindicator.ChunkBorderRendererMixin"));
    }

    @Test void geometryUsesExactChunkCenterIncludingNegativeCoordinates() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/chunkcenterindicator/ChunkCenterIndicatorFeature.java");
        assertTrue(feature.contains("Math.floor(coordinate/16.0D)*16.0D+8.0D"));
        assertTrue(feature.contains("ARM=0.25D"));
        assertTrue(feature.contains("VERTICAL=0.5D"));
        assertTrue(feature.contains("Y_OFFSET=0.02D"));
        assertTrue(feature.contains("COLOR_ARGB=0xE6FFC714"));
        assertTrue(feature.contains("COLOR_R=1.0F,COLOR_G=0.78F,COLOR_B=0.08F,COLOR_A=0.90F"));
    }

    @Test void rendererOnlyAddsMarkerToVanillaF3GPass() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/chunkcenterindicator/ChunkBorderRendererMixin.java");
        assertTrue(mixin.contains("@Mixin(ChunkBorderRenderer.class)"));
        assertTrue(mixin.contains("@Inject(method=\"emitGizmos\",at=@At(\"TAIL\"))"));
        assertTrue(mixin.contains("@Inject(method=\"render\",at=@At(\"TAIL\"))"));
        assertFalse(mixin.contains("Keybind"));
        assertFalse(mixin.contains("Keyboard"));
        assertFalse(mixin.contains("for("));
    }

    @Test void rendererHasModernBufferAndLegacyPaths() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/chunkcenterindicator/ChunkBorderRendererMixin.java");
        for(String token:new String[]{
                "Gizmos.line",
                "ChunkCenterIndicatorFeature.COLOR_ARGB",
                "ChunkCenterIndicatorFeature.COLOR_R",
                "RenderType.debugLineStrip(3.0D)",
                "VertexFormat.Mode.DEBUG_LINE_STRIP",
                "GlStateManager.lineWidth(3.0F)",
                "ChunkCenterIndicatorFeature.chunkCenter(entity.getX())",
                "ChunkCenterIndicatorFeature.chunkCenter(entity.getZ())"
        })assertTrue(mixin.contains(token),token);
    }
}
