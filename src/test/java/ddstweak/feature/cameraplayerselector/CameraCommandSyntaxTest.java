package ddstweak.feature.cameraplayerselector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CameraCommandSyntaxTest {
    @Test void dispatchClassificationPreservesCommandsAndChatTriggers(){
        for(String command:new String[]{"/tp Steve","tp Steve","/c tp Steve","c tp Steve"})
            assertTrue(CameraCommandSyntax.validRoot(command));
        for(String trigger:new String[]{"!!tp Steve","!back","#menu"})
            assertTrue(CameraCommandSyntax.validRoot(trigger));
        assertFalse(CameraCommandSyntax.isChat("/tp Steve"));
        assertFalse(CameraCommandSyntax.isChat("tp Steve"));
        assertTrue(CameraCommandSyntax.isChat("!!tp Steve"));
        assertTrue(CameraCommandSyntax.isChat("#menu"));
        assertEquals("/tp Steve",CameraCommandSyntax.display("tp Steve"));
        assertEquals("/tp Steve",CameraCommandSyntax.display("/tp Steve"));
        assertEquals("!!tp Steve",CameraCommandSyntax.display("!!tp Steve"));
        assertEquals("#menu",CameraCommandSyntax.display("#menu"));
    }

    @Test void rejectsAmbiguousOrEmptyRoots(){
        for(String invalid:new String[]{"/!!tp Steve","/!back","!!","!","#","/","//tp Steve","! tp Steve","!! tp Steve"})
            assertFalse(CameraCommandSyntax.validRoot(invalid),invalid);
    }
}
