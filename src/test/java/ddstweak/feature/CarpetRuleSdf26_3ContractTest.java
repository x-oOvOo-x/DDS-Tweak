package ddstweak.feature;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 26.x is the only generation with optimized GPU rounded-corner rendering. */
class CarpetRuleSdf26_3ContractTest {
    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    @Test void legacyVersionsUseCpuRoundedCoverageWithoutGpuSdf() throws Exception {
        String renderer = read("src/main/java/ddstweak/gui/SdfRoundedRectRenderer.java");
        String style = read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleUiStyle.java");
        assertTrue(renderer.contains("public static void fill("));
        assertTrue(renderer.contains("public static void outline("));
        assertTrue(renderer.contains("//#elseif MC >= 260100"));
        assertTrue(renderer.contains("LegacyGpuSdfRenderer.draw(gui,"));
        assertTrue(renderer.contains("cpuRoundedFill(gui, x1, y1, x2, y2, radius, argb)"));
        assertTrue(renderer.contains("private static void paintRoundedRows("));
        assertTrue(renderer.contains("cornerCoverageMask(r,steps)"));
        assertTrue(renderer.contains("CORNER_COVERAGE_CACHE"));
        assertTrue(renderer.contains("if(next==coverage)continue;"));
        assertTrue(renderer.contains("private static int cornerCoverage(float radius,"));
        assertTrue(renderer.contains("drawRectOutline(gui, x1, y1, x2, y2, argb)"));
        assertFalse(renderer.contains("SdfGpu1218Renderer"));
        assertFalse(renderer.contains("SdfTexture12111Renderer"));
        assertFalse(renderer.contains("drawSoftware"));
        assertTrue(style.contains("SdfRoundedRectRenderer.fill(g,x1,y1,x2,y2,r,color)"));
        assertFalse(style.contains("BATCH_12111"));
        assertFalse(style.contains("roundedListRow("));
        assertFalse(Files.exists(Path.of("src/main/java/ddstweak/gui/SdfCornerCoverage.java")));
    }

    @Test void gpuPathsStillKeep26xPipelinesAndClipping() throws Exception {
        String renderer = read("src/main/java/ddstweak/gui/SdfRoundedRectRenderer.java");
        String gpu = read("src/main/java/ddstweak/gui/LegacyGpuSdfRenderer.java");
        String accessor = read("src/main/java/ddstweak/mixin/gui/RenderPipelinesAccess.java");
        String main = read("src/main/java/ddstweak/DDSTweak.java");
        assertTrue(renderer.contains("//#if MC >= 260300"));
        assertTrue(renderer.contains("gui.guiRenderState.addGuiElement("));
        assertTrue(renderer.contains("gui.scissorStack.peek()"));
        assertTrue(renderer.contains("ColorTargetState(BlendFunction.TRANSLUCENT)"));
        assertTrue(gpu.contains("VertexFormat.builder(0)"));
        assertTrue(gpu.contains("withVertexBinding(0, FORMAT)"));
        assertTrue(gpu.contains("withVertexFormat(FORMAT, VertexFormat.Mode.QUADS)"));
        assertTrue(accessor.contains("//#if MC >= 260100"));
        assertTrue(main.contains("LegacyGpuSdfRenderer.initialize()"));
        assertFalse(main.contains("SdfGpu1218Renderer"));
    }

    @Test void pre26xGpuAdaptersAndResourcesAreExcluded() throws Exception {
        String gradle = read("common.gradle");
        String build = read("build.gradle");
        String mixins = read("src/main/resources/dds-tweak.mixins.json");
        assertFalse(mixins.contains("SdfGui1218StateMixin"));
        assertTrue(mixins.contains("SdfGuiRenderStateMixin"));
        assertTrue(mixins.contains("RenderPipelinesAccess"));
        assertTrue(gradle.contains("exclude 'ddstweak/mixin/gui/RenderPipelinesAccess.java'"));
        assertTrue(gradle.contains("unavailable.addAll(['gui.SdfGuiRenderStateMixin', 'gui.RenderPipelinesAccess'])"));
        assertTrue(build.contains("target.mcVersion < 26_01_02"));
        assertFalse(gradle.contains("generateSdf12111Sprites"));
        assertFalse(Files.exists(Path.of("src/main/java/ddstweak/gui/SdfGpu1218Renderer.java")));
        assertFalse(Files.exists(Path.of("src/main/java/ddstweak/gui/SdfTexture12111Renderer.java")));
        assertFalse(Files.exists(Path.of("src/main/java/ddstweak/mixin/gui/SdfGui1218StateMixin.java")));
        assertFalse(read("src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerScreen.java").contains("BATCH_12111"));
        assertFalse(read("src/main/java/ddstweak/feature/carpetrules/CarpetRulePresetScreen.java").contains("BATCH_12111"));
    }
}
