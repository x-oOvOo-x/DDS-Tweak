package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleHotkeyContractTest {
    private static String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    @Test void registersF7ThroughExistingMalilibHotkeyAndConfigPersistence() throws Exception {
        String hotkeys=source("src/main/java/ddstweak/config/Hotkeys.java");
        String configs=source("src/main/java/ddstweak/config/Configs.java");
        assertTrue(hotkeys.contains("OPEN_CARPET_RULE_MANAGER=MalilibCompat.hotkey(\"openCarpetRuleManager\",\"F7\",KEY)"));
        assertTrue(hotkeys.contains("ImmutableList.of(OPEN_CONFIG_SCREEN,OPEN_CARPET_RULE_MANAGER,"));
        assertTrue(configs.contains("addAll(Hotkeys.HOTKEY_LIST)"));
        assertTrue(configs.contains("readConfigBase(r,\"Hotkeys\",Hotkeys.HOTKEY_LIST)"));
        assertTrue(configs.contains("writeConfigBase(r,\"Hotkeys\",Hotkeys.HOTKEY_LIST)"));
    }

    @Test void opensOnlyFromGameplayAndDoesNotRequireCda() throws Exception {
        String callbacks=source("src/main/java/ddstweak/config/HotkeyCallbacks.java");
        assertTrue(callbacks.contains("Hotkeys.OPEN_CARPET_RULE_MANAGER.getKeybind().setCallback"));
        assertTrue(callbacks.contains("mc.player==null||mc.getConnection()==null"));
        assertTrue(callbacks.contains("MinecraftCompat.screen(mc)!=null"));
        assertTrue(callbacks.contains("new CarpetRuleManagerScreen(null)"));
        assertFalse(callbacks.contains("isModLoaded(\"carpet-dds-addition\")"));
    }

    @Test void clientTabShowsBothGlobalScreenShortcuts() throws Exception {
        String screen=source("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        String features=screen.substring(screen.indexOf("private static List<IConfigBase> featureConfigs()"),
                screen.indexOf("private static List<IConfigBase> tweakConfigs()"));
        String client=screen.substring(screen.indexOf("private static List<IConfigBase> clientConfigs()"),
                screen.indexOf("/** Shared DT-only label renderer"));
        assertFalse(features.contains("Hotkeys.OPEN_CONFIG_SCREEN"));
        assertFalse(features.contains("Hotkeys.OPEN_CARPET_RULE_MANAGER"));
        assertTrue(client.contains("c.add(Hotkeys.OPEN_CONFIG_SCREEN);"));
        assertTrue(client.contains("c.add(Hotkeys.OPEN_CARPET_RULE_MANAGER);"));
    }

    @Test void bothCurrentAndLegacyMalilibTranslationKeysExist() throws Exception {
        for(String language:new String[]{"zh_cn","zh_tw","en_us"}){
            String json=source("src/main/resources/assets/dds-tweak/lang/"+language+".json");
            assertTrue(json.contains("\"dds-tweak.config.hotkey.name.openCarpetRuleManager\""),language);
            assertTrue(json.contains("\"dds-tweak.config.hotkey.prettyName.openCarpetRuleManager\""),language);
            assertTrue(json.contains("\"dds-tweak.config.hotkey.comment.openCarpetRuleManager\""),language);
            assertTrue(json.contains("\"dds-tweak.config.hotkey.prettyName.opencarpetrulemanager\""),language);
            assertTrue(json.contains("\"dds-tweak.config.hotkey.comment.opencarpetrulemanager\""),language);
        }
    }
}
