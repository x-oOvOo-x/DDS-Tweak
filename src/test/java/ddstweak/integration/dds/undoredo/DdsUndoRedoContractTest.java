package ddstweak.integration.dds.undoredo;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsUndoRedoContractTest {
    private static Path root(){
        for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())
            if(Files.isDirectory(p.resolve("src/main")))return p;
        throw new IllegalStateException("repo root not found");
    }
    private static String read(String path)throws IOException{return Files.readString(root().resolve(path));}

    @Test void packetIdsStayProtocolV1Stable(){
        assertEquals("undo_c2s",DdsUndoRedoPackets.UNDO_C2S);
        assertEquals("redo_c2s",DdsUndoRedoPackets.REDO_C2S);
    }

    @Test void shortcutRequiresBothAuthoritativeCapabilities()throws IOException{
        String code=read("src/main/java/ddstweak/integration/dds/undoredo/DdsUndoRedoClientNetwork.java");
        assertTrue(code.contains("canTakeOverInput(DdsUndoRedoPackets.UNDO_C2S)"));
        assertTrue(code.contains("canTakeOverInput(DdsUndoRedoPackets.REDO_C2S)"));
    }

    @Test void undoRedoConfigIsAvailableAcrossExistingTargets()throws IOException{
        // The removed KeyboardHandlerMixin supported MC < 1.21.9 as well:
        // introducing a new version gate would silently remove legacy DT shortcuts.
        String hotkeys=read("src/main/java/ddstweak/config/Hotkeys.java");
        String callbacks=read("src/main/java/ddstweak/config/HotkeyCallbacks.java");
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        assertFalse(hotkeys.contains("//#if MC >= 12109"),
                "Hotkey registration must not be gated to modern targets");
        assertFalse(callbacks.contains("//#if MC >= 12109\n        Hotkeys.DDS_UNDO"),
                "The Undo/Redo callbacks must also be registered on legacy targets");
        assertFalse(screen.contains("//#if MC >= 12109\n        c.add(Hotkeys.DDS_UNDO)"),
                "Undo/Redo must remain visible in the legacy Client tab");
        assertTrue(hotkeys.contains("DDS_UNDO,DDS_REDO"));
        assertTrue(callbacks.contains("Hotkeys.DDS_UNDO.getKeybind().setCallback"));
        assertTrue(callbacks.contains("Hotkeys.DDS_REDO.getKeybind().setCallback"));
        assertTrue(screen.contains("c.add(Hotkeys.DDS_UNDO)"));
        assertTrue(screen.contains("c.add(Hotkeys.DDS_REDO)"));
    }

    @Test void shortcutsAreMaLiLibConfigsAndNotAKeyboardMixin()throws IOException{
        String keys=read("src/main/java/ddstweak/config/Hotkeys.java");
        String callbacks=read("src/main/java/ddstweak/config/HotkeyCallbacks.java");
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        String mixins=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(keys.contains("hotkey(\"ddsUndo\",\"LEFT_CONTROL,Z\",KEY)"));
        assertTrue(keys.contains("hotkey(\"ddsRedo\",\"LEFT_CONTROL,LEFT_ALT,Z\",KEY)"));
        assertTrue(keys.contains("DDS_UNDO,DDS_REDO"));
        String client=screen.substring(screen.indexOf("private static List<IConfigBase> clientConfigs()"));
        assertTrue(client.contains("c.add(Hotkeys.DDS_UNDO)"));
        assertTrue(client.contains("c.add(Hotkeys.DDS_REDO)"));
        assertTrue(callbacks.contains("DdsLocalOwnershipResolver.current().tweakOwnsFeatureClient()"));
        assertTrue(callbacks.contains("mc.player.isCreative()"));
        assertTrue(callbacks.contains("mc.getConnection()!=null"));
        assertTrue(callbacks.contains("MinecraftCompat.screen(mc)==null"));
        assertTrue(callbacks.contains("DdsUndoRedoClientNetwork.sendUndo()"));
        assertTrue(callbacks.contains("DdsUndoRedoClientNetwork.sendRedo()"));
        assertTrue(callbacks.contains("DDS_UNDO.getStringValue().equals(Hotkeys.DDS_REDO.getStringValue())"));
        assertFalse(mixins.contains("integration.dds.undoredo.KeyboardHandlerMixin"));
        assertFalse(Files.exists(root().resolve("src/main/java/ddstweak/mixin/integration/dds/undoredo/KeyboardHandlerMixin.java")));
    }
}
