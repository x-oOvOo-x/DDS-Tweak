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
        assertTrue(feature.contains("resolve(Object vanillaGamma)"));
        assertFalse(feature.contains("Minecraft.getInstance"));
        assertFalse(feature.contains("setDoubleValue"));
    }

    @Test void lightmapExpressionIsComposablyOverriddenWithoutMutatingOptions() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/gammaoverride/GammaOverrideMixin.java");
        String feature=read("src/main/java/ddstweak/feature/gammaoverride/GammaOverrideFeature.java");
        assertTrue(mixin.contains("import com.llamalad7.mixinextras.injector.ModifyExpressionValue;"));
        assertTrue(mixin.contains("@Mixin(LightTexture.class)"));
        assertTrue(mixin.contains("@Mixin(LightmapRenderStateExtractor.class)"));
        assertTrue(mixin.contains("@ModifyExpressionValue"));
        assertTrue(mixin.contains("@Expression(\"?.gamma().get()\")"));
        assertTrue(mixin.contains("Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;"));
        assertTrue(mixin.contains("Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"));
        assertFalse(mixin.contains("@Redirect"));
        assertTrue(mixin.contains("private Object ddstweak$runtimeGamma(Object original){return GammaOverrideFeature.resolve(original);}"));
        assertTrue(mixin.contains("private double ddstweak$runtimeGamma(double original){return GammaOverrideFeature.resolve(original);}"));
        assertTrue(feature.contains("enabled()?value():vanillaGamma"));
        assertTrue(feature.contains("enabled()?Double.valueOf(value()):vanillaGamma"));
        assertFalse(mixin.contains("option.get()"));
        assertFalse(mixin.contains("options.gamma"));
        assertFalse(mixin.contains("setValue("));
        assertFalse(mixin.contains("setDoubleValue("));
        assertFalse(mixin.contains("GAMMA_VALUE_ORIGINAL"));
    }

    @Test void injectionSelectorsCoverEverySupportedRenderingEra() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/gammaoverride/GammaOverrideMixin.java");
        assertTrue(mixin.contains("MC >= 260102"));
        assertTrue(mixin.contains("method=\"extract\""));
        assertTrue(mixin.contains("MC >= 11904"));
        assertTrue(mixin.contains("MIXINEXTRAS:EXPRESSION"));
        assertFalse(mixin.contains("ordinal="));
        assertEquals(3, mixin.split("allow=1", -1).length - 1);
        String build=read("common.gradle");
        assertTrue(build.contains("tasks.register('verifyGammaBytecodeTarget')"));
        assertTrue(build.contains("tasks.named('build').configure { dependsOn tasks.named('verifyGammaBytecodeTarget') }"));
        assertTrue(build.contains("found != 1"));
        assertTrue(mixin.contains("value=\"FIELD\",target=\"Lnet/minecraft/client/Options;gamma:D\""));
    }

    @Test void loaderSuppliesMixinExtrasWithoutEmbedding() throws Exception {
        String build=read("common.gradle");
        assertTrue(build.contains("mixinextras-common:0.5.5"));
        assertTrue(build.contains("compileOnly 'io.github.llamalad7:mixinextras-common:0.5.5'"));
        assertTrue(build.contains("annotationProcessor 'io.github.llamalad7:mixinextras-common:0.5.5'"));
        assertFalse(build.contains("mixinextras-fabric"));
        assertFalse(build.contains("include(implementation("));
        String rootBuild=read("build.gradle");
        assertTrue(rootBuild.contains("name.toLowerCase().startsWith('meta-inf/jars/fabric-')"));
        assertTrue(rootBuild.contains("name.toLowerCase().startsWith('meta-inf/jars/mixinextras-')"));
        assertTrue(rootBuild.contains("Processed Fabric Loader dependency mismatch"));
        for(String version:new String[]{"1.14.4","1.15.2","1.16.5","1.17.1","1.18.2",
                "1.19.4","1.20.1","1.20.2","1.20.4","1.20.6","1.21.1",
                "1.21.3","1.21.4","1.21.5","1.21.8","1.21.9","1.21.10","1.21.11"}){
            assertTrue(read("versions/"+version+"/gradle.properties")
                    .contains("loader_dependency=>=0.17.0"), version);
        }
        for(String version:new String[]{"26.1.2","26.2","26.3"}){
            assertTrue(read("versions/"+version+"/gradle.properties")
                    .contains("loader_dependency=>=0.19.5"), version);
        }
    }

    @Test void mixinIsRegisteredAndNotVersionFilteredOut() throws Exception {
        String json=read("src/main/resources/dds-tweak.mixins.json");
        String build=read("common.gradle");
        assertTrue(json.contains("feature.gammaoverride.GammaOverrideMixin"));
        // @Expression requires a declared MixinExtras floor even when Loader provides the library.
        assertTrue(json.contains("\"mixinextras\""));
        assertTrue(json.contains("\"minVersion\": \"0.5.0\""));
        assertFalse(build.contains("unavailable.add('feature.gammaoverride.GammaOverrideMixin')"));
    }

    @Test void allLocalesDescribeRuntimeLightmapScopeConcisely() throws Exception {
        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            var lang=com.google.gson.JsonParser.parseString(
                    read("src/main/resources/assets/dds-tweak/lang/"+locale+".json")).getAsJsonObject();
            String expected=locale.equals("en_us")?"lightmap":locale.equals("zh_cn")?"光照贴图":"光照貼圖";
            for(String option:new String[]{"gammaOverride","gammaOverrideValue"}){
                String tip=lang.get("dds-tweak.config.visual.comment."+option).getAsString();
                assertTrue(tip.toLowerCase(java.util.Locale.ROOT).contains(expected),
                        locale+" must identify the lightmap scope: "+option);
                assertTrue(tip.codePointCount(0,tip.length())<=(locale.equals("en_us")?80:40),
                        locale+": "+option);
            }
        }
    }
}
