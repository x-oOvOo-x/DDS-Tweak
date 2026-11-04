package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class TabFakeGroupsContractTest {
    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    @Test void disabledByDefaultAndExcludedFromOlderVersions() throws Exception {
        String config = read("src/main/java/ddstweak/config/Configs.java");
        String versions = read("config/version-capability-boundaries.json");
        String mixins = read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(config.contains("TAB_FAKE_PLAYERS_LAST=MalilibCompat.bool(\"tabFakePlayersLast\",false,VISUAL_KEY)"));
        assertTrue(versions.contains("\"id\": \"tabFakePlayersLast\""));
        assertTrue(versions.contains("\"minMcVersionCode\": 12111"));
        assertTrue(mixins.contains("feature.tabgroups.PlayerTabOverlayMixin"));
        String gui = read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        String visual = gui.substring(gui.indexOf("private static List<IConfigBase> visualConfigs()"),
                gui.indexOf("private static List<IConfigBase> clientConfigs()"));
        assertTrue(visual.contains("c.add(Configs.Visual.TAB_FAKE_PLAYERS_LAST);"),
                "The option must be explicitly listed in DT's custom Visual tab");
    }

    @Test void newConfigLabelsAndTipsFollowTheCanonicalTranslationContract() throws Exception {
        String suffix = "dds-tweak.config.visual.";
        for (String locale : new String[]{"en_us", "zh_cn", "zh_tw"}) {
            com.google.gson.JsonObject lang = com.google.gson.JsonParser.parseString(
                    read("src/main/resources/assets/dds-tweak/lang/" + locale + ".json")
            ).getAsJsonObject();
            String name = lang.get(suffix + "name.tabFakePlayersLast").getAsString();
            String prettyName = lang.get(suffix + "prettyName.tabFakePlayersLast").getAsString();
            String tip = lang.get(suffix + "comment.tabFakePlayersLast").getAsString();
            assertFalse(name.isBlank(), locale + " name must be translated");
            assertEquals(name, prettyName, locale + " must keep the displayed name consistent");
            assertFalse(tip.isBlank(), locale + " needs a useful short tip");
            assertTrue(tip.codePointCount(0, tip.length()) <=
                    ("en_us".equals(locale) ? 80 : 40), locale + " tip is too long");
            assertFalse(lang.has(suffix + "comment.tabfakeplayerslast"),
                    locale + " must not duplicate casing variants");
        }
    }

    @Test void keepsListedMembershipAndVanillaComparator() throws Exception {
        String mixin = read("src/main/java/ddstweak/mixin/feature/tabgroups/PlayerTabOverlayMixin.java");
        assertTrue(mixin.contains("PlayerIdentityResolver.resolve("));
        assertTrue(mixin.contains("PlayerInventoryTargetType.FAKE"));
        assertTrue(mixin.contains("thenComparing(vanilla)"));
        assertTrue(mixin.contains("Stream;sorted(Ljava/util/Comparator;)"));
        assertFalse(mixin.contains("getLatency()"));
        assertFalse(mixin.contains("setListed("));
    }

    @Test void rowOffsetsIncludePingAndFooterOnBothRenderBackends() throws Exception {
        String mixin = read("src/main/java/ddstweak/mixin/feature/tabgroups/PlayerTabOverlayMixin.java");
        assertTrue(mixin.contains("GuiGraphicsExtractor;fill(IIIII)V"));
        assertTrue(mixin.contains("GuiGraphics;fill(IIIII)V"));
        assertTrue(mixin.contains("renderPingIcon("));
        assertTrue(mixin.contains("extractPingIcon("));
        assertTrue(mixin.contains("ddstweak$maxOffset"));
        assertTrue(mixin.contains("popMatrix()"));
        assertTrue(mixin.contains("@WrapMethod(method = \"render\")"));
        assertTrue(mixin.contains("@WrapMethod(method = \"extractRenderState\")"));
        assertTrue(mixin.contains("finally {"));
        assertTrue(mixin.contains("fitsVertically("));
    }
}
