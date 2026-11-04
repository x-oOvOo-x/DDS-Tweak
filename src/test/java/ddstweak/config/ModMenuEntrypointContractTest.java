package ddstweak.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

final class ModMenuEntrypointContractTest {
    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    @Test void configFactoryIsOptionalAndOwnedByDt() throws Exception {
        JsonObject metadata = JsonParser.parseString(read("src/main/resources/fabric.mod.json")).getAsJsonObject();
        assertEquals("ddstweak.integration.modmenu.DDSTweakModMenuIntegration",
                metadata.getAsJsonObject("entrypoints").getAsJsonArray("modmenu").get(0).getAsString());
        assertFalse(metadata.getAsJsonObject("depends").has("modmenu"),
                "Mod Menu must remain optional at runtime");

        String adapter = read("src/main/java/ddstweak/integration/modmenu/DDSTweakModMenuIntegration.java");
        assertTrue(adapter.contains("implements ModMenuApi"));
        assertTrue(adapter.contains("getModConfigScreenFactory()"));
        assertTrue(adapter.contains("getConfigScreenFactory()"));
        assertTrue(adapter.contains("DDSTweakConfigScreen screen = new DDSTweakConfigScreen()"));
        assertTrue(adapter.contains("screen.setParent(parent)"));
        assertFalse(read("src/main/java/ddstweak/DDSTweak.java").contains("modmenu"),
                "Fabric Loader should only instantiate the optional adapter from its entrypoint");
    }

    @Test void compileOnlyApiCoversAllSupportedNodes() throws Exception {
        JsonObject settings = JsonParser.parseString(read("settings.json")).getAsJsonObject();
        String gradle = read("common.gradle");
        Matcher versions = Pattern.compile("'([^']+)': '[^']+'").matcher(
                gradle.substring(gradle.indexOf("final def modMenuVersions = ["),
                        gradle.indexOf("]", gradle.indexOf("final def modMenuVersions = ["))));
        Set<String> supported = new HashSet<>();
        while (versions.find()) supported.add(versions.group(1));
        supported.add("1.14.4");

        settings.getAsJsonArray("activeVersions").forEach(version ->
                assertTrue(supported.contains(version.getAsString()), "Missing Mod Menu API: " + version));
        assertTrue(gradle.contains("modMenuApi.transitive = false"));
        assertTrue(gradle.contains("'compileOnly' : 'modCompileOnly'"));
        assertTrue(gradle.contains("maven.modrinth:modmenu:leKKCpqK"));
    }
}
