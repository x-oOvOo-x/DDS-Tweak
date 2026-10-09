package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class BetaHappyGhastExclusionTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static String read(String path) throws Exception {
        return Files.readString(ROOT.resolve(path));
    }

    @Test void speedModificationIsAbsentFromBetaSourcesAndRegistration() throws Exception {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/ddstweak/feature/happyghastspeed")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/ddstweak/mixin/feature/happyghastspeed")));
        assertFalse(read("src/main/java/ddstweak/config/Configs.java").contains("HAPPY_GHAST_RIDING_SPEED_MULTIPLIER"));
        assertFalse(read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java").contains("HAPPY_GHAST_RIDING_SPEED_MULTIPLIER"));
        assertFalse(read("src/main/resources/dds-tweak.mixins.json").contains("feature.happyghastspeed."));
        assertFalse(read("config/version-capability-boundaries.json").contains("happyGhastRidingSpeed"));
        for (String language : new String[]{"en_us", "zh_cn", "zh_tw"}) {
            String translations = read("src/main/resources/assets/dds-tweak/lang/" + language + ".json");
            assertFalse(translations.contains("happyGhastRidingSpeedMultiplier"));
            assertFalse(translations.contains("happyghastridingspeedmultiplier"));
            assertFalse(translations.contains("happy_ghast.riding_speed"));
        }
    }
}
