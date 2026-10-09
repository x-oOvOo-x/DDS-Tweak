package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class GammaOverrideContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}

    @Test void configMatchesTweakerooRangeButIsRuntimeOnly() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String feature=read("src/main/java/ddstweak/feature/gammaoverride/GammaOverrideFeature.java");
        assertTrue(configs.contains("GAMMA_OVERRIDE=MalilibCompat.boolHotkeyed(\"gammaOverride\",false,\"\",VISUAL_KEY)"));
        assertTrue(configs.contains("GAMMA_OVERRIDE_VALUE=MalilibCompat.decimal(\"gammaOverrideValue\",16.0,0.0,32.0,true,VISUAL_KEY)"));
        String compat=read("src/main/java/ddstweak/platform/malilib/MalilibCompat.java");
        String provider=read("src/main/java/ddstweak/config/DDSTweakHotkeyProvider.java");
        assertTrue(compat.contains("ConfigBooleanHotkeyed"));
        assertTrue(compat.contains("boolHotkeyed(String name,boolean value,String hotkey,String translationPrefix)"));
        assertTrue(provider.contains("config instanceof IHotkey"));
        assertTrue(provider.contains("Configs.ALL"));
        String mixins=read("src/main/resources/dds-tweak.mixins.json");
        String build=read("common.gradle");
        assertTrue(mixins.contains("config.LegacyHotkeyedBooleanWidgetMixin"));
        assertTrue(mixins.contains("config.LegacyConfigBooleanHotkeyedMixin"));
        assertTrue(build.contains("mcVersion >= 1_18_00"));
        assertTrue(build.contains("LegacyHotkeyedBooleanWidgetMixin"));
        assertTrue(build.contains("LegacyConfigBooleanHotkeyedMixin"));
        assertTrue(feature.contains("getDoubleValue()"));
        assertTrue(feature.contains("resolve(double vanillaGamma)"));
        assertFalse(feature.contains("Minecraft.getInstance"));
        assertFalse(feature.contains("setDoubleValue"));
    }

    @Test void lightmapReadIsOverriddenInsteadOfGameOptions() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/gammaoverride/GammaOverrideMixin.java");
        assertTrue(mixin.contains("@Mixin(LightTexture.class)"));
        assertTrue(mixin.contains("@Mixin(LightmapRenderStateExtractor.class)"));
        assertTrue(mixin.contains("GammaOverrideFeature.enabled()?GammaOverrideFeature.value():option.get()"));
        assertTrue(mixin.contains("GammaOverrideFeature.resolve(options.gamma)"));
        assertFalse(mixin.contains("setValue("));
        assertFalse(mixin.contains("setDoubleValue("));
        assertFalse(mixin.contains("GAMMA_VALUE_ORIGINAL"));
    }

    @Test void injectionOrdinalsCoverEverySupportedRenderingEra() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/gammaoverride/GammaOverrideMixin.java");
        assertTrue(mixin.contains("MC >= 260102"));
        assertTrue(mixin.contains("method=\"extract\""));
        assertTrue(mixin.contains("MC >= 12109"));
        assertTrue(mixin.contains("MC >= 11904"));
        assertTrue(mixin.contains("ordinal=2"));
        assertTrue(mixin.contains("ordinal=1"));
        assertTrue(mixin.contains("value=\"FIELD\",target=\"Lnet/minecraft/client/Options;gamma:D\""));
    }

    @Test void mixinIsRegisteredAndNotVersionFilteredOut() throws Exception {
        String json=read("src/main/resources/dds-tweak.mixins.json");
        String build=read("common.gradle");
        assertTrue(json.contains("feature.gammaoverride.GammaOverrideMixin"));
        assertFalse(build.contains("unavailable.add('feature.gammaoverride.GammaOverrideMixin')"));
    }

    @Test void allLocalesExplainNonPollutingBehavior() throws Exception {
        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            String lang=read("src/main/resources/assets/dds-tweak/lang/"+locale+".json");
            assertTrue(lang.contains("gammaOverride"));
            assertTrue(lang.contains("gammaOverrideValue"));
            assertTrue(lang.contains("options.txt"));
        }
    }
}
