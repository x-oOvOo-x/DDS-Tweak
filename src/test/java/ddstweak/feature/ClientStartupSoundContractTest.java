package ddstweak.feature;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

final class ClientStartupSoundContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();

    private static String read(String path) throws Exception {
        Path file=ROOT.resolve(path);
        return Files.exists(file)?Files.readString(file):"";
    }

    @Test void featureWaitsForClientTickAndLoadedSounds() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/startupsound/ClientStartupSound.java");
        String init=read("src/main/java/ddstweak/DDSTweakInitHandler.java");

        assertTrue(feature.contains("ClientTickEvents.END_CLIENT_TICK.register(ClientStartupSound::onClientTick)"));
        assertTrue(feature.contains("minecraft.getSoundManager().getAvailableSounds().isEmpty()"));
        assertTrue(feature.contains("private static boolean pending=true"));
        assertTrue(feature.contains("pending=false"));
        assertTrue(init.contains("ClientStartupSound.initialize()"));
        assertFalse(feature.contains("ClientLifecycleEvents.CLIENT_STARTED"));
    }

    @Test void startupSoundUsesVanillaExperiencePickupSound() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/startupsound/ClientStartupSound.java");

        assertTrue(feature.contains("SoundEvents.EXPERIENCE_ORB_PICKUP"));
        assertTrue(feature.contains("SimpleSoundInstance.forUI"));
        assertTrue(feature.contains("minecraft.getSoundManager().play"));
    }

    @Test void startupSoundIsConfigurableAndEnabledByDefault() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String feature=read("src/main/java/ddstweak/feature/startupsound/ClientStartupSound.java");

        assertTrue(configs.contains("CLIENT_STARTUP_SOUND=MalilibCompat.bool(\"clientStartupSound\",true,TWEAKS_KEY)"));
        assertTrue(configs.contains("CLIENT_STARTUP_SOUND"));
        assertTrue(configs.contains("CLIENT_STARTUP_SOUND,QUICK_MUTE,HELPFUL_HITBOXES"));
        assertTrue(feature.contains("Configs.Tweaks.CLIENT_STARTUP_SOUND.getBooleanValue()"));
    }
}
