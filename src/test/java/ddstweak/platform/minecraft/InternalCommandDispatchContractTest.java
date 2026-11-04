package ddstweak.platform.minecraft;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class InternalCommandDispatchContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String path)throws Exception{return Files.readString(ROOT.resolve(path));}

    @Test void internalCommandsPreserveSafeDispatchBoundaries()throws Exception{
        String compat=read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        String carpet=read("src/main/java/ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandBridge.java");
        String camera=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");

        assertTrue(compat.contains("sendServerCommandSilently"));
        assertTrue(compat.contains("MC >= 12103"));
        assertTrue(compat.contains("new ServerboundChatCommandPacket(command)"));
        assertTrue(compat.contains("connection.getConnection().send"));
        assertTrue(compat.contains("MC >= 11900"));
        assertTrue(compat.contains("connection.sendUnsignedCommand(command)"));
        assertTrue(carpet.contains("MinecraftCompat.sendServerCommandSilently"));
        assertFalse(carpet.contains("MinecraftCompat.sendCommand("));
        // Player-editable camera templates can contain signable arguments;
        // dispatch them through vanilla's command-signing path.
        String syntax=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandSyntax.java");
        assertTrue(camera.contains("CameraCommandSyntax.send(minecraft,"));
        assertTrue(syntax.contains("MinecraftCompat.sendCommandWithoutHistory(minecraft,value)"));
        assertTrue(syntax.contains("MinecraftCompat.sendChatWithoutHistory(minecraft,value)"));
        assertFalse(camera.contains("MinecraftCompat.sendServerCommandSilently"));
        assertFalse(syntax.contains("sendServerCommandSilently"));
    }

    @Test void cameraCommandsUseSignedDispatchWithoutAddingUpArrowHistory()throws Exception{
        String compat=read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        String camera=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        String quick=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandOverlay.java");

        assertTrue(compat.contains("connection.sendCommand(command)"));
        assertTrue(compat.contains("minecraft.player.chat(\"/\"+command)"));
        assertTrue(compat.contains("CommandHistoryGuard.run(command,()->sendCommand(minecraft,command))"));
        assertFalse(compat.contains("addRecentChat("));
        assertFalse(compat.contains("addToCommandHistory("));
        assertFalse(compat.contains("ChatScreen.handleChatInput"));
        String syntax=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandSyntax.java");
        assertTrue(camera.contains("CameraCommandSyntax.send(minecraft,"));
        assertTrue(quick.contains("CameraCommandSyntax.send(minecraft,command)"));
        assertTrue(syntax.contains("MinecraftCompat.sendCommandWithoutHistory(minecraft,value)"));
        assertTrue(syntax.contains("MinecraftCompat.sendChatWithoutHistory(minecraft,value)"));
        assertTrue(compat.contains("CommandHistoryGuard.runChat(message"));
        assertTrue(compat.contains("connection.sendChat(message)"));
        assertFalse(camera.contains("sendServerCommandSilently"));
        assertFalse(quick.contains("sendServerCommandSilently"));
    }

    @Test void manualCommandHelperRemainsSeparate()throws Exception{
        String compat=read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        assertTrue(compat.contains("public static boolean sendCommand"));
        assertTrue(compat.contains("connection.sendCommand(command)"));
    }
}
