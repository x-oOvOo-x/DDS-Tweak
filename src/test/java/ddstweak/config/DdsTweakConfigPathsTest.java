package ddstweak.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class DdsTweakConfigPathsTest {
    @TempDir Path root;

    @Test void migratesAllDtOwnedJsonFilesWithoutChangingContent() throws Exception {
        String[][] names = {
                {"dds-tweak.json", "settings.json"},
                {"dds-tweak-camera-command-profiles.json", "camera-command-profiles.json"},
                {"dds-tweak-camera-destinations.json", "camera-destinations.json"},
                {"dds-tweak-camera-quick-commands.json", "camera-quick-commands.json"},
                {"dds-tweak-carpet-rules.json", "carpet-rules.json"},
                {"dds-tweak-carpet-presets.json", "carpet-presets.json"}
        };
        for (int i = 0; i < names.length; i++) {
            String oldName = names[i][0];
            Path oldFile = root.resolve(oldName);
            String content = "{\"schema\":" + i + ",\"global\":{\"value\":\"保留设置\"}}";
            Files.write(oldFile, content.getBytes(StandardCharsets.UTF_8));
            Path target = DdsTweakConfigPaths.migrate(root, oldName, names[i][1]);
            assertEquals(root.resolve("dds-tweak").resolve(names[i][1]), target);
            assertEquals(content, new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
            assertEquals(content, new String(Files.readAllBytes(oldFile), StandardCharsets.UTF_8));
        }
    }

    @Test void newLocationWinsEvenIfLegacyChangesLater() throws Exception {
        Path legacy = root.resolve("dds-tweak.json");
        Files.write(legacy, "{\"value\":1}".getBytes(StandardCharsets.UTF_8));
        Path target = DdsTweakConfigPaths.migrate(root, "dds-tweak.json", "settings.json");
        Files.write(target, "{\"value\":2}".getBytes(StandardCharsets.UTF_8));
        Files.write(legacy, "{\"value\":3}".getBytes(StandardCharsets.UTF_8));
        assertEquals(target, DdsTweakConfigPaths.migrate(root, "dds-tweak.json", "settings.json"));
        assertEquals("{\"value\":2}", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test void preservesExistingLegacyBackup() throws Exception {
        Path legacy = root.resolve("dds-tweak-camera-destinations.json");
        Path backup = root.resolve("dds-tweak-camera-destinations.json.bak");
        Files.write(legacy, "{\"destinations\":[]}".getBytes(StandardCharsets.UTF_8));
        Files.write(backup, "{\"destinations\":[{\"id\":\"old\"}]}".getBytes(StandardCharsets.UTF_8));
        Path target = DdsTweakConfigPaths.migrate(root, legacy.getFileName().toString(),
                "camera-destinations.json");
        assertArrayEquals(Files.readAllBytes(backup),
                Files.readAllBytes(target.resolveSibling("camera-destinations.json.bak")));
        assertTrue(Files.exists(backup));
    }

    @Test void missingLegacyDoesNotCreateEmptyFile() {
        Path target = DdsTweakConfigPaths.migrate(root, "dds-tweak.json", "settings.json");
        assertFalse(Files.exists(target));
        assertFalse(Files.exists(target.getParent()));
    }

    @Test void rejectsBrokenLegacyWithoutOverwritingIt() throws Exception {
        Path legacy = root.resolve("dds-tweak.json");
        Files.write(legacy, "{broken".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class,
                () -> DdsTweakConfigPaths.migrate(root, "dds-tweak.json", "settings.json"));
        assertEquals("{broken", new String(Files.readAllBytes(legacy), StandardCharsets.UTF_8));
        assertFalse(Files.exists(root.resolve("dds-tweak/settings.json")));
    }

    @Test void brokenNewConfigCannotBeSilentlyReplacedByLegacy() throws Exception {
        Files.createDirectories(root.resolve("dds-tweak"));
        Path target = root.resolve("dds-tweak/settings.json");
        Files.write(target, "[]".getBytes(StandardCharsets.UTF_8));
        Files.write(root.resolve("dds-tweak.json"), "{\"value\":1}".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class,
                () -> DdsTweakConfigPaths.migrate(root, "dds-tweak.json", "settings.json"));
        assertEquals("[]", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
    }

    @Test void refusesDirectoryMasqueradingAsLegacyConfig() throws Exception {
        Files.createDirectory(root.resolve("dds-tweak.json"));
        assertThrows(IllegalStateException.class,
                () -> DdsTweakConfigPaths.migrate(root, "dds-tweak.json", "settings.json"));
        assertFalse(Files.exists(root.resolve("dds-tweak/settings.json")));
    }

    @Test void migratesOnlyDtJsonExportsAndPreservesLegacyAndOtherModFiles() throws Exception {
        Path oldExports = Files.createDirectories(root.resolve("dds-tweak-carpet-exports"));
        Path legacy = oldExports.resolve("plan.json");
        Files.write(legacy, "{\"version\":2}".getBytes(StandardCharsets.UTF_8));
        Files.write(oldExports.resolve("notes.txt"), "keep".getBytes(StandardCharsets.UTF_8));
        Path foreign = Files.createDirectories(root.resolve("carpet")).resolve("carpet.conf");
        Files.write(foreign, "rule true".getBytes(StandardCharsets.UTF_8));

        Path exports = DdsTweakConfigPaths.migrateExportDirectory(root);
        assertEquals(root.resolve("dds-tweak/carpet-exports"), exports);
        assertArrayEquals(Files.readAllBytes(legacy), Files.readAllBytes(exports.resolve("plan.json")));
        assertTrue(Files.isRegularFile(legacy));
        assertFalse(Files.exists(exports.resolve("notes.txt")));
        assertEquals("rule true", new String(Files.readAllBytes(foreign), StandardCharsets.UTF_8));

        Files.write(exports.resolve("plan.json"), "newer".getBytes(StandardCharsets.UTF_8));
        DdsTweakConfigPaths.migrateExportDirectory(root);
        assertEquals("newer", new String(Files.readAllBytes(exports.resolve("plan.json")),
                StandardCharsets.UTF_8));
        Files.delete(exports.resolve("plan.json"));
        DdsTweakConfigPaths.migrateExportDirectory(root);
        assertFalse(Files.exists(exports.resolve("plan.json")),
                "Old exports must not reappear after the user deletes the new copy");
    }

    @Test void doesNotCreateExportDirectoryWithoutLegacyData() {
        Path exports = DdsTweakConfigPaths.migrateExportDirectory(root);
        assertFalse(Files.exists(exports));
    }

    @Test void malformedCarpetLegacyFileCannotReplaceExistingSettings() throws Exception {
        Path legacy = root.resolve("dds-tweak-carpet-rules.json");
        Files.write(legacy, "broken".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class,
                () -> DdsTweakConfigPaths.migrate(root,
                        "dds-tweak-carpet-rules.json", "carpet-rules.json"));
        assertFalse(Files.exists(root.resolve("dds-tweak/carpet-rules.json")));
        assertEquals("broken", new String(Files.readAllBytes(legacy), StandardCharsets.UTF_8));
    }

    @Test void optionalExchangeCopiesOriginalBytesWithoutBlockingOnBadImportJson() throws Exception {
        Path oldFile = root.resolve("dds-tweak-carpet-presets-transfer.json");
        Files.write(oldFile, "invalid JSON".getBytes(StandardCharsets.UTF_8));
        Path target = DdsTweakConfigPaths.migrateOptionalExchange(root);
        assertEquals(root.resolve("dds-tweak/carpet-presets-transfer.json"), target);
        assertArrayEquals(Files.readAllBytes(oldFile), Files.readAllBytes(target));
        Files.write(target, "new transfer".getBytes(StandardCharsets.UTF_8));
        DdsTweakConfigPaths.migrateOptionalExchange(root);
        assertEquals("new transfer", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        assertEquals("invalid JSON", new String(Files.readAllBytes(oldFile), StandardCharsets.UTF_8));
    }

}
