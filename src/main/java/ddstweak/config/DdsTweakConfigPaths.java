package ddstweak.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ddstweak.platform.malilib.MalilibCompat;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Single home for DT client config; copy legacy files without destroying rollback data. */
public final class DdsTweakConfigPaths {
    private static final String DIRECTORY = "dds-tweak";

    private DdsTweakConfigPaths() {}

    public static Path settings() {
        return migrate(MalilibCompat.configDirectory(), "dds-tweak.json", "settings.json");
    }

    public static Path cameraCommandProfiles() {
        return migrate(MalilibCompat.configDirectory(),
                "dds-tweak-camera-command-profiles.json", "camera-command-profiles.json");
    }

    public static Path cameraDestinations() {
        return migrate(MalilibCompat.configDirectory(),
                "dds-tweak-camera-destinations.json", "camera-destinations.json");
    }

    public static Path cameraQuickCommands() {
        return migrate(MalilibCompat.configDirectory(),
                "dds-tweak-camera-quick-commands.json", "camera-quick-commands.json");
    }

    /** New files win; legacy originals remain untouched for rollback to older DT builds. */
    static synchronized Path migrate(Path configRoot, String oldName, String newName) {
        Path destination = configRoot.resolve(DIRECTORY).resolve(newName);
        Path legacy = configRoot.resolve(oldName);
        try {
            if (Files.exists(destination)) {
                requireJsonObject(destination);
                return destination;
            }
            if (!Files.exists(legacy)) return destination;

            requireJsonObject(legacy);
            Files.createDirectories(destination.getParent());
            copyIfAbsent(legacy, destination);
            requireJsonObject(destination);

            // Camera stores may already have a last-known-good backup.
            copyIfAbsent(configRoot.resolve(oldName + ".bak"),
                    destination.resolveSibling(newName + ".bak"));
            return destination;
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot migrate DT config from " + legacy
                    + " to " + destination + "; legacy file preserved", failure);
        }
    }

    private static void requireJsonObject(Path file) {
        if (!Files.isRegularFile(file) || !Files.isReadable(file))
            throw new IllegalStateException("Unreadable DT config, original preserved: " + file);
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement value = new JsonParser().parse(reader);
            if (value == null || !value.isJsonObject())
                throw new IllegalStateException("Invalid DT config JSON, original preserved: " + file);
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Invalid DT config, original preserved: " + file, failure);
        }
    }

    /** Copy into the destination directory first so a failed copy cannot leave a partial config. */
    private static void copyIfAbsent(Path source, Path destination) throws IOException {
        if (!Files.exists(source) || Files.exists(destination)) return;
        if (!Files.isRegularFile(source))
            throw new IOException("Not a regular config file: " + source);
        Path temporary = Files.createTempFile(destination.getParent(), ".dds-tweak-migrate-", ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, destination);
            } catch (FileAlreadyExistsException ignored) {
                // Another instance migrated the file first; never replace its result.
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
