package ddstweak.feature;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

final class AutoLogCleanupContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();

    private static String read(String path) throws Exception {
        Path file=ROOT.resolve(path);
        return Files.exists(file)?Files.readString(file):"";
    }

    @Test void settingsMatchTheAgreedDefaultsAndRanges() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");

        assertTrue(configs.contains("AUTO_LOG_CLEANUP=MalilibCompat.bool(\"autoLogCleanup\",false,TWEAKS_KEY)"));
        assertTrue(configs.contains("AUTO_LOG_CLEANUP_MODE=MalilibCompat.optionList(\"autoLogCleanupMode\",AutoLogCleanupMode.DAYS,TWEAKS_KEY)"));
        assertTrue(configs.contains("AUTO_LOG_KEEP_DAYS=MalilibCompat.integer(\"autoLogKeepDays\",14,1,365,true,TWEAKS_KEY)"));
        assertTrue(configs.contains("AUTO_LOG_KEEP_LIMIT=MalilibCompat.integer(\"autoLogKeepLimit\",10,1,100,true,TWEAKS_KEY)"));
    }

    @Test void cleanupRunsOnceAfterClientStartup() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/autologcleanup/AutoLogCleanup.java");
        String init=read("src/main/java/ddstweak/DDSTweakInitHandler.java");

        assertTrue(feature.contains("ClientLifecycleEvents.CLIENT_STARTED.register(client -> cleanOnce())"));
        assertTrue(feature.contains("if(cleaned)return"));
        assertTrue(feature.contains("cleaned=true"));
        assertTrue(feature.contains("if(!Configs.Tweaks.AUTO_LOG_CLEANUP.getBooleanValue())return"));
        assertTrue(init.contains("AutoLogCleanup.initialize()"));
    }

    @Test void onlyVanillaStyleHistoricalLogsAreCandidates() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/autologcleanup/AutoLogCleanup.java");

        assertTrue(feature.contains("[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]+\\\\.log(?:\\\\.gz)?"));
        assertTrue(feature.contains("HISTORICAL_LOG.matcher(name.toString()).matches()"));
        assertFalse(feature.contains("endsWith(\".log\")"));
        assertFalse(feature.contains("endsWith(\".log.gz\")"));
    }

    @Test void daysAndLimitModesUseSafeDeletion() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/autologcleanup/AutoLogCleanup.java");
        String mode=read("src/main/java/ddstweak/feature/autologcleanup/AutoLogCleanupMode.java");

        assertTrue(mode.contains("DAYS(\"days\")"));
        assertTrue(mode.contains("LIMIT(\"limit\")"));
        assertTrue(feature.contains("cleanByDays(files,Configs.Tweaks.AUTO_LOG_KEEP_DAYS.getIntegerValue())"));
        assertTrue(feature.contains("cleanByLimit(files,Configs.Tweaks.AUTO_LOG_KEEP_LIMIT.getIntegerValue())"));
        assertTrue(feature.contains("Files.deleteIfExists(file)"));
        assertTrue(feature.contains("return Long.MAX_VALUE"));
    }

    @Test void cleanupUsesGameDirectoryLogsAndDoesNotRunOnSettingChanges() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/autologcleanup/AutoLogCleanup.java");

        assertTrue(feature.contains("FabricLoader.getInstance().getGameDir().resolve(\"logs\")"));
        assertFalse(feature.contains("setValueChangeCallback"));
    }
}
