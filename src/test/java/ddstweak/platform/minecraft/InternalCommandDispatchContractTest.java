package ddstweak.platform.minecraft;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class InternalCommandDispatchContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String path)throws Exception{return Files.readString(ROOT.resolve(path));}

    @Test void tweakOwnedCommandsUseSilentServerDispatch()throws Exception{
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
        assertTrue(camera.contains("MinecraftCompat.sendServerCommandSilently"));
        assertFalse(carpet.contains("MinecraftCompat.sendCommand("));
        assertFalse(camera.contains("MinecraftCompat.sendCommand("));
    }

    @Test void manualCommandHelperRemainsSeparate()throws Exception{
        String compat=read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        assertTrue(compat.contains("public static boolean sendCommand"));
        assertTrue(compat.contains("connection.sendCommand(command)"));
    }
}
