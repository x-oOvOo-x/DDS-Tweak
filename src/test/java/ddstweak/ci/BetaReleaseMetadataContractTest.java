package ddstweak.ci;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class BetaReleaseMetadataContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String path) throws Exception {
        return Files.readString(ROOT.resolve(path));
    }

    @Test void licenseNoticesAndSourceLinkArePreparedForPublishing() throws Exception {
        assertTrue(read("LICENSE").startsWith("GNU LESSER GENERAL PUBLIC LICENSE"));
        assertTrue(read("LICENSE").contains("GNU GENERAL PUBLIC LICENSE"));
        assertTrue(read("THIRD_PARTY_NOTICES.md").contains("Better Snow Coverage"));
        assertTrue(read("src/main/resources/resourcepacks/dds_fake_player_ui/NOTICE.txt").contains("CozyUI+"));
        String mod=read("src/main/resources/fabric.mod.json");
        assertFalse(mod.contains("DDS-Tweak-Dev"));
        assertTrue(mod.contains("\"license\": \"LGPL-3.0-only\""));
        String gradle=read("common.gradle");
        assertTrue(gradle.contains("rootProject.file('LICENSE')"));
        assertTrue(gradle.contains("rootProject.file('THIRD_PARTY_NOTICES.md')"));
    }
}
