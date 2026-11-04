package ddstweak.build;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class VersionFeatureBoundaryContractTest {
    @Test void versionGatedSoundAndRestockUseSharedArtifactBoundaryContract() throws Exception {
        JsonArray features = JsonParser.parseString(
                Files.readString(Path.of("config/version-capability-boundaries.json"))
        ).getAsJsonObject().getAsJsonArray("features");
        check(features, "blockSoundTracker", "Visual", "blockSoundTracker",
                "ddstweak/feature/blocksoundtracker/", "ddstweak/mixin/feature/blocksoundtracker/");
        check(features, "shulkerRestock", "Tweaks", "shulkerRestockEnabled",
                "ddstweak/feature/shulkerrestock/", null);
        check(features, "tabFakePlayersLast", "Visual", "tabFakePlayersLast",
                "ddstweak/feature/tabgroups/", "ddstweak/mixin/feature/tabgroups/");
        String build = Files.readString(Path.of("common.gradle"));
        assertTrue(build.contains("unavailableJavaPrefixes.each"), "Exclude unavailable source classes");
        assertTrue(build.contains("feature.mixins"), "Exclude unavailable Mixin registrations");
        assertFalse(build.contains("if (mcVersion < 1_21_11) unavailable.add('feature.blocksoundtracker"),
                "Sound tracker should use the canonical boundary table, not a parallel condition");
    }


    @Test void restockControlsAreVisibleAcrossAllActiveMinecraftVersions() throws Exception {
        JsonObject capabilities = JsonParser.parseString(
                Files.readString(Path.of("config/version-capability-boundaries.json"))
        ).getAsJsonObject();
        JsonObject restock = null;
        for (var entry : capabilities.getAsJsonArray("features")) {
            JsonObject feature = entry.getAsJsonObject();
            if ("shulkerRestock".equals(feature.get("id").getAsString())) restock = feature;
        }
        assertNotNull(restock);
        int minimum = restock.get("minMcVersionCode").getAsInt();

        JsonArray active = JsonParser.parseString(
                Files.readString(Path.of("settings.json"))
        ).getAsJsonObject().getAsJsonArray("activeVersions");
        assertTrue(active.size() > 0);
        boolean includes12101 = false;
        for (var value : active) {
            String version = value.getAsString();
            String[] numbers = version.split("\\.");
            int code = Integer.parseInt(numbers[0]) * 10000 + Integer.parseInt(numbers[1]) * 100
                    + (numbers.length > 2 ? Integer.parseInt(numbers[2]) : 0);
            assertTrue(code >= minimum, "Unsupported active version: " + version);
            if (code == 12101) includes12101 = true;
        }
        assertTrue(includes12101, "The reported 1.21.1 regression must remain covered");

        String configs = Files.readString(Path.of("src/main/java/ddstweak/config/Configs.java"));
        String screen = Files.readString(Path.of("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java"));
        String[] options = {
                "SHULKER_RESTOCK_ENABLED", "SHULKER_RESTOCK_MODE", "SHULKER_RESTOCK_WHITELIST",
                "SHULKER_RESTOCK_BLACKLIST", "SHULKER_RESTOCK_THRESHOLD"
        };
        String featureRows = screen.substring(screen.indexOf("c.add(Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST);"),
                screen.indexOf("c.add(Configs.Tweaks.CONFIRM_ITEM_DROP);"));
        String parentRows = screen.substring(screen.indexOf("group(parents,Configs.Tweaks.AUTO_LOG_CLEANUP,"),
                screen.indexOf("group(parents,Configs.Tweaks.SHOW_ORIGINAL_CONFIG_NAME,"));
        for (String option : options) {
            assertTrue(configs.contains("public static final Config"), "Config definition exists");
            assertTrue(configs.contains("SHULKER_RESTOCK_ENABLED,SHULKER_RESTOCK_MODE,SHULKER_RESTOCK_WHITELIST,SHULKER_RESTOCK_BLACKLIST,SHULKER_RESTOCK_THRESHOLD,"),
                    "Every restock setting must be saved in Tweaks.OPTIONS");
            assertTrue(featureRows.contains("c.add(Configs.Tweaks." + option + ");"), option + " missing from Features tab");
            assertTrue(parentRows.contains("Configs.Tweaks." + option), option + " missing from disclosure group");
        }
        assertFalse(featureRows.contains("//#if"), "No version gate may hide supported restock settings");
        assertFalse(parentRows.contains("//#if"), "No version gate may remove restock disclosure grouping");

        String label = screen.substring(screen.indexOf("protected final void captureOriginalName("),
                screen.indexOf("if(!statusRow&&!ConfigRowGrouping.showOriginalName"));
        assertTrue(label.contains("boolean statusRow=config==Configs.Tweaks.SHULKER_RESTOCK_ENABLED;"));
        assertFalse(label.contains("//#if"), "Restock status row must work on legacy versions");
        String statusRender = screen.substring(screen.indexOf("String secondary=enabled?"),
                screen.indexOf("boolean show=!secondary.isEmpty();"));
        assertTrue(statusRender.contains("secondary=restockStatusText();"));
        assertFalse(statusRender.contains("//#if"), "Server-authority status must be visible on legacy versions");
        assertTrue(screen.contains("private static String restockStatusText()"));
        String init = Files.readString(Path.of("src/main/java/ddstweak/integration/dds/DdsIntegrationBootstrap.java"));
        assertTrue(init.contains("ShulkerRestockClientNetwork.initialize();"));
    }

    private static void check(JsonArray features, String id, String owner, String key,
                              String implementation, String mixinPackage) {
        JsonObject found = null;
        for (var feature : features) {
            JsonObject value = feature.getAsJsonObject();
            if (id.equals(value.get("id").getAsString())) {
                assertNull(found, "Duplicate feature ID " + id);
                found = value;
            }
        }
        assertNotNull(found, "Missing feature boundary " + id);
        int minCode = "shulkerRestock".equals(id) ? 11404 : 12111;
        String minVersion = "shulkerRestock".equals(id) ? "1.14.4" : "1.21.11";
        assertEquals(minCode, found.get("minMcVersionCode").getAsInt(), id);
        assertEquals(minVersion, found.get("minMinecraft").getAsString(), id);
        assertEquals(owner, found.get("configOwner").getAsString(), id);
        assertEquals(key, found.get("configKey").getAsString(), id);
        String javaPrefixes = found.getAsJsonArray("javaPrefixes").toString();
        assertTrue(javaPrefixes.contains(implementation), id);
        if (mixinPackage != null) assertTrue(javaPrefixes.contains(mixinPackage), id);
        if (id.equals("blockSoundTracker"))
            assertTrue(found.getAsJsonArray("mixins").toString()
                    .contains("feature.blocksoundtracker.ClientLevelEventMixin"), id);
    }
}
