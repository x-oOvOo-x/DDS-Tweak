package ddstweak.feature;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

final class BetterSnowCoverageContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();

    private static String read(String path) throws Exception {
        Path file=ROOT.resolve(path);
        return Files.exists(file)?Files.readString(file):"";
    }

    @Test void featureIsVisualDefaultOffAndUsesTwoModes() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String mode=read("src/main/java/ddstweak/feature/bettersnowcoverage/SnowCoverageMode.java");

        assertTrue(configs.contains("BETTER_SNOW_COVERAGE=MalilibCompat.bool(\"betterSnowCoverage\",false,VISUAL_KEY)"));
        assertTrue(configs.contains("SnowCoverageMode.PREFER_SNOW"));
        assertTrue(mode.contains("PREFER_SNOW(\"prefer_snow\")"));
        assertTrue(mode.contains("ALL_SIDES(\"all_sides\")"));
        assertFalse(mode.contains("PREFER_AIR"));
    }

    @Test void blacklistIsACompleteEarlyExclusionAndRealSnowIsNotDuplicated() throws Exception {
        String checker=read("src/main/java/ddstweak/feature/bettersnowcoverage/SnowCoverageChecker.java");
        String result=read("src/main/java/ddstweak/feature/bettersnowcoverage/SnowCoverageResult.java");

        int blacklist=checker.indexOf("if(matches(aboveState,context.blacklist))return remember(context,key,SnowCoverageResult.NONE)");
        int neighbor=checker.indexOf("hasSnowNeighbor(world,above,context)");
        assertTrue(blacklist>=0);
        assertTrue(neighbor>blacklist,"blacklist must reject before any neighbor scan");
        assertTrue(checker.contains("aboveState.is(BlockTags.SNOW)"));
        assertFalse(result.contains("WITHOUT_LAYER"));
    }

    @Test void listsSupportBlockIdsAndTagsWithEmptyWhitelistMeaningUnrestricted() throws Exception {
        String checker=read("src/main/java/ddstweak/feature/bettersnowcoverage/SnowCoverageChecker.java");

        assertTrue(checker.contains("boolean tag=value.charAt(0)=='#'"));
        assertTrue(checker.contains("TagKey.create(Registries.BLOCK,id)"));
        assertTrue(checker.contains("BuiltInRegistries.BLOCK.getKey(state.getBlock())"));
        assertTrue(checker.contains("context.whitelistEnabled&&!matches(aboveState,context.whitelist)"));
    }

    @Test void scanUsesSectionScopedThreadLocalCaches() throws Exception {
        String checker=read("src/main/java/ddstweak/feature/bettersnowcoverage/SnowCoverageChecker.java");
        String context=read("src/main/java/ddstweak/feature/bettersnowcoverage/SnowCoverageBuildContext.java");

        assertTrue(checker.contains("MAX_HORIZONTAL_DISTANCE=8"));
        assertTrue(checker.contains("MAX_VERTICAL_DISTANCE=2"));
        assertTrue(checker.contains("Long2ObjectOpenHashMap<SnowCoverageResult> results"));
        assertTrue(checker.contains("Long2ObjectOpenHashMap<Probe> probes"));
        assertTrue(context.contains("ThreadLocal<SectionCache>"));
        assertTrue(context.contains("GENERATION"));
        assertTrue(context.contains("sectionX"));
        assertTrue(context.contains("context=new SnowCoverageChecker.Context()"));
        assertTrue(context.contains("public static void invalidate()"));
    }

    @Test void rendererUsesFabricModelApiWithoutRendererSpecificMixins() throws Exception {
        String gradle=read("common.gradle");
        String loading=read("src/main/java/ddstweak/feature/bettersnowcoverage/BetterSnowModelLoading.java");
        String wrapper=read("src/main/java/ddstweak/feature/bettersnowcoverage/BetterSnowBlockStateModel.java");
        String plugin=read("src/main/java/ddstweak/mixin/DdsTweakMixinConfigPlugin.java");
        String mixins=read("src/main/resources/dds-tweak.mixins.json");

        assertTrue(gradle.contains("net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"));
        assertFalse(gradle.contains("fabricApi.module("));
        assertFalse(gradle.contains("include module"));
        assertFalse(gradle.contains("maven.modrinth:sodium"));
        assertTrue(loading.contains("ModelLoadingPlugin.register"));
        assertTrue(loading.contains("ModelModifier.WRAP_LAST_PHASE"));
        assertTrue(wrapper.contains("extends WrapperBlockStateModel"));
        assertTrue(wrapper.contains("FabricBlockStateModel"));
        assertTrue(wrapper.contains("emitQuads"));
        assertTrue(wrapper.contains("createGeometryKey"));
        assertFalse(plugin.contains("isModLoaded(\"sodium\")"));
        assertFalse(plugin.contains("SNOW_VANILLA_PREFIX"));
        assertFalse(plugin.contains("SNOW_SODIUM_PREFIX"));
        assertFalse(mixins.contains("feature.bettersnowcoverage."));
    }

    @Test void wrapperUsesBaseBlockAsTheSingleSnowCoverageAuthority() throws Exception {
        String wrapper=read("src/main/java/ddstweak/feature/bettersnowcoverage/BetterSnowBlockStateModel.java");

        assertTrue(wrapper.contains("SnowCoverageResult coverage=SnowCoverageBuildContext.evaluate(level,pos)"));
        assertTrue(wrapper.contains("SnowCoverageBuildContext.snowyState(state)"));
        assertTrue(wrapper.contains("BetterSnowModelLoading.model(snowy)"));
        assertTrue(wrapper.contains("BetterSnowModelLoading.model(SNOW)"));
        assertFalse(wrapper.contains("SnowCoverageBuildContext.evaluate(level,pos.below())"));
    }

    @Test void virtualSnowLayerIsTranslatedAboveTheBaseBlockWithoutNeighborCullLeakage() throws Exception {
        String wrapper=read("src/main/java/ddstweak/feature/bettersnowcoverage/BetterSnowBlockStateModel.java");

        assertTrue(wrapper.contains("BlockPos snowPos=pos.above()"));
        assertTrue(wrapper.contains("quad.y(vertex)+1.0F"));
        assertTrue(wrapper.contains("quad.cullFace(null)"));
        assertTrue(wrapper.contains("direction->false"));
        assertTrue(wrapper.contains("createGeometryKey(level,snowPos,SNOW,random)"));
    }

    @Test void blacklistAndWhitelistUseSharedCompactBlockPicker() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/dropconfirmation/DropConfirmStringListButtonMixin.java");
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(mixin.contains("\"betterSnowCoverageBlacklist\""));
        assertTrue(mixin.contains("\"betterSnowCoverageWhitelist\""));
        assertTrue(mixin.contains("DropConfirmationItemScreen.Kind.BLOCK"));
        assertTrue(picker.contains("item instanceof BlockItem"));
        assertTrue(picker.contains("value.startsWith(\"#\")"));
        assertTrue(picker.contains("key()==257&&addQueryEntry()"));
    }

    @Test void modelLoadingRegistersEarlyAndConfigChangesInvalidateCachesAndMeshes() throws Exception {
        String entry=read("src/main/java/ddstweak/DDSTweak.java");
        String feature=read("src/main/java/ddstweak/feature/bettersnowcoverage/BetterSnowCoverage.java");
        String init=read("src/main/java/ddstweak/DDSTweakInitHandler.java");

        assertTrue(entry.contains("BetterSnowCoverage.registerModelLoading()"));
        assertTrue(feature.contains("SnowCoverageBuildContext.invalidate()"));
        assertTrue(feature.contains("BETTER_SNOW_COVERAGE.setValueChangeCallback"));
        assertTrue(feature.contains("BETTER_SNOW_COVERAGE_MODE.setValueChangeCallback"));
        assertTrue(feature.contains("BETTER_SNOW_COVERAGE_BLACKLIST.setValueChangeCallback"));
        assertTrue(feature.contains("BETTER_SNOW_COVERAGE_WHITELIST.setValueChangeCallback"));
        assertTrue(feature.contains("//#if MC >= 260200"));
        assertTrue(feature.contains("levelExtractor.allChanged()"));
        assertFalse(feature.contains("levelRenderer.invalidateCompiledGeometry"));
        assertTrue(feature.contains("levelRenderer.allChanged()"));
        assertTrue(init.contains("BetterSnowCoverage.initialize()"));
    }
}
