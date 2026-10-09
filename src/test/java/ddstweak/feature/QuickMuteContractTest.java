package ddstweak.feature;

import org.junit.jupiter.api.Test;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

final class QuickMuteContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();

    private static String read(String path) throws Exception {
        Path file=ROOT.resolve(path);
        return Files.exists(file)?Files.readString(file):"";
    }

    @Test void quickMuteIsDefaultOffWithCtrlM() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(configs.contains("QUICK_MUTE=MalilibCompat.boolHotkeyed(\"quickMute\",false,\"LEFT_CONTROL,M\",TWEAKS_KEY)"));
        assertTrue(configs.contains("CLIENT_STARTUP_SOUND,QUICK_MUTE,HELPFUL_HITBOXES"));
    }

    @Test void hotkeyTogglesTheBooleanStateAndInstallsCustomBehavior() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/quickmute/QuickMute.java");
        String callbacks=read("src/main/java/ddstweak/config/HotkeyCallbacks.java");

        assertTrue(feature.contains("QUICK_MUTE.getKeybind().setCallback"));
        assertTrue(feature.contains("QUICK_MUTE.toggleBooleanValue()"));
        assertTrue(feature.contains("QUICK_MUTE.setValueChangeCallback"));
        assertTrue(callbacks.contains("QuickMute.initialize()"));
        assertTrue(feature.contains("if(Configs.Tweaks.QUICK_MUTE.getBooleanValue())apply(true,false)"));
        assertTrue(feature.contains("if(muted==appliedMuted)return"));
        assertTrue(feature.contains("appliedMuted=true"));
        assertTrue(feature.contains("appliedMuted=false"));
    }

    @Test void mutingRemembersAndRestoresMasterVolume() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/quickmute/QuickMute.java");
        String compat=read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");

        assertTrue(feature.contains("restoreVolume=MinecraftCompat.masterVolume(minecraft)"));
        assertTrue(feature.contains("MinecraftCompat.setMasterVolume(minecraft,0.0F)"));
        assertTrue(feature.contains("MinecraftCompat.setMasterVolume(minecraft,restoreVolume)"));
        assertTrue(compat.contains("getSoundSourceVolume(SoundSource.MASTER)"));
        assertTrue(compat.contains("getSoundSourceOptionInstance(SoundSource.MASTER).set((double)value)"));
        assertTrue(compat.contains("//$$ minecraft.options.setSoundCategoryVolume(SoundSource.MASTER,value);"));
        assertFalse(feature.contains("options.save()"));
    }

    @Test void feedbackUsesActionBarAndRestoredPercentage() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/quickmute/QuickMute.java");
        String zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");

        assertTrue(feature.contains("dds-tweak.quick_mute.muted"));
        assertTrue(feature.contains("dds-tweak.quick_mute.restored"));
        assertTrue(feature.contains("MinecraftCompat.overlayMessage"));
        assertTrue(feature.contains("+\"%\""));
        assertTrue(zh.contains("已静音"));
        assertTrue(zh.contains("已恢复声音：%s"));
    }

    @Test void tooltipCommentsContainNoRawPercentSign() throws Exception {
        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            String lang=read("src/main/resources/assets/dds-tweak/lang/"+locale+".json");
            for(String line:lang.split("\\R")){
                if(line.contains("config.tweaks.comment.quickMute")||line.contains("config.tweaks.comment.quickmute")){
                    assertFalse(line.contains("%"),locale+" quick mute tooltip contains raw percent: "+line);
                }
            }
        }
    }

    @Test void volumeApiUsesTheLegacyModernSplit() throws Exception {
        String compat=read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        assertTrue(compat.contains("//#if MC >= 11904"));
        assertTrue(compat.contains("getSoundSourceOptionInstance(SoundSource.MASTER).set((double)value)"));
        assertTrue(compat.contains("//$$ minecraft.options.setSoundCategoryVolume(SoundSource.MASTER,value);"));
    }
}
