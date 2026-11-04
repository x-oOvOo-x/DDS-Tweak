package ddstweak.config;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class FabricApiModIdContractTest {
    @Test void historicFabricApiIdChangesAtMinecraft1192() throws Exception {
        String gradle=Files.readString(Path.of("common.gradle"));
        String mod=Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        assertTrue(gradle.contains("mcVersion < 1_19_02 ? 'fabric' : 'fabric-api'"));
        assertTrue(gradle.contains("fabric_api_mod_id    : fabricApiModId"));
        assertTrue(mod.contains("\"${fabric_api_mod_id}\""));
        assertFalse(mod.contains("\"fabric-api\": \"${fabric_api_dependency}\""));
        assertTrue(gradle.contains("modMetadata.depends[fabricApiModId] != project.fabric_api_dependency"));
        assertTrue(gradle.contains("modMetadata.depends.containsKey(alternateId)"));
        String root=Files.readString(Path.of("build.gradle"));
        assertTrue(root.contains("modJson.contains('\"${fabric_api_mod_id}\": \"${fabric_api_dependency}\"')"));
        assertTrue(root.contains("target.mcVersion < 1_19_02 ? 'fabric' : 'fabric-api'"));
        assertTrue(root.contains("processedMod.depends?.get(fabricApiId)"));
        assertTrue(root.contains("processedMod.depends?.containsKey(alternateFabricApiId)"));
    }
}
