package ddstweak.integration.dds.undoredo;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsUndoRedoContractTest {
    private static Path root(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root not found");}
    private static String read(String path)throws IOException{return Files.readString(root().resolve(path));}

    @Test void packetIdsStayProtocolV1Stable(){assertEquals("undo_c2s",DdsUndoRedoPackets.UNDO_C2S);assertEquals("redo_c2s",DdsUndoRedoPackets.REDO_C2S);}
    @Test void shortcutRequiresBothAuthoritativeCapabilities()throws IOException{
        String code=read("src/main/java/ddstweak/integration/dds/undoredo/DdsUndoRedoClientNetwork.java");
        assertTrue(code.contains("canTakeOverInput(DdsUndoRedoPackets.UNDO_C2S)"));
        assertTrue(code.contains("canTakeOverInput(DdsUndoRedoPackets.REDO_C2S)"));
    }
    @Test void keyboardSemanticsMatchLegacyClient()throws IOException{
        String code=read("src/main/java/ddstweak/mixin/integration/dds/undoredo/KeyboardHandlerMixin.java");
        for(String token:new String[]{"GLFW.GLFW_PRESS","GLFW.GLFW_KEY_Z","event.hasControlDown()","event.hasShiftDown()","mc.player.isCreative()","mc.screen!=null","event.hasAltDown()","if(sent)ci.cancel()"})assertTrue(code.contains(token),token);
    }
}
