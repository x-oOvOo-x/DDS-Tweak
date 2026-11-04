package ddstweak.feature.cameraplayerselector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CameraCommandProfileTest {
    @Test void cdaDefaultsAndOrdinaryTeleportStayIndependent(){
        CameraCommandProfile profile=new CameraCommandProfile();
        assertEquals(CameraCommandProfile.Mode.TOGGLE,profile.mode);
        assertEquals("c",profile.render(CameraCommandProfile.Action.TOGGLE));
        assertEquals("c back",profile.render(CameraCommandProfile.Action.BACK));
        assertEquals("c tp Steve",profile.renderPlayer(CameraCommandProfile.Action.PLAYER_TP,"Steve"));
        assertEquals("c follow Alex",profile.renderPlayer(CameraCommandProfile.Action.PLAYER_FOLLOW,"Alex"));
        assertEquals("tp Steve",profile.renderPlayer(CameraCommandProfile.Action.PLAYER_DIRECT_TP,"Steve"));
    }

    @Test void invalidOrMissingVariablesCannotExecute(){
        CameraCommandProfile profile=new CameraCommandProfile();
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.PLAYER_TP,"/tp Steve"));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.PLAYER_TP,"/tp {unknown}"));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.PLAYER_TP,"/tp {dimension}"));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.TOGGLE,"/c\n/op"));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.TOGGLE,"/c;op"));
        profile.set(CameraCommandProfile.Action.PLAYER_TP,"/tp {unknown}");
        assertNull(profile.renderPlayer(CameraCommandProfile.Action.PLAYER_TP,"Steve"));
        profile.set(CameraCommandProfile.Action.PLAYER_TP,"");
        assertNull(profile.renderPlayer(CameraCommandProfile.Action.PLAYER_TP,"Steve"));
    }

    @Test void chatTriggeredTemplatesRenderAsMessagesWithoutSlash(){
        CameraCommandProfile profile=new CameraCommandProfile();
        profile.set(CameraCommandProfile.Action.PLAYER_TP,"!!tp {player}");
        profile.set(CameraCommandProfile.Action.PLAYER_FOLLOW,"!follow {player}");
        profile.set(CameraCommandProfile.Action.BACK,"!!back");
        assertNull(CameraCommandProfile.validate(CameraCommandProfile.Action.PLAYER_TP,profile.get(CameraCommandProfile.Action.PLAYER_TP)));
        assertEquals("!!tp Steve",profile.renderPlayer(CameraCommandProfile.Action.PLAYER_TP,"Steve"));
        assertEquals("!follow Alex",profile.renderPlayer(CameraCommandProfile.Action.PLAYER_FOLLOW,"Alex"));
        assertEquals("!!back",profile.render(CameraCommandProfile.Action.BACK));
        assertTrue(CameraCommandSyntax.isChat("!!tp Steve"));
        assertTrue(CameraCommandSyntax.isChat("#home"));
        assertFalse(CameraCommandSyntax.isChat("tp Steve"));
        assertFalse(CameraCommandSyntax.isChat("/tp Steve"));
        assertEquals("!!tp Steve",CameraCommandSyntax.display(profile.renderPlayer(CameraCommandProfile.Action.PLAYER_TP,"Steve")));
        assertEquals("/tp Steve",CameraCommandSyntax.display("tp Steve"));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.PLAYER_TP,"/!!tp {player}"));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.PLAYER_TP,"!!tp Steve"));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.PLAYER_TP,"!! {player}"));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.PLAYER_TP,"!!tp {player};/op"));
    }

    @Test void profileCopyAndCustomCameraModes(){
        CameraCommandProfile a=new CameraCommandProfile();
        CameraCommandProfile b=a.copy();
        b.mode=CameraCommandProfile.Mode.SEPARATE;
        b.set(CameraCommandProfile.Action.ENTER,"/gmsp");
        b.set(CameraCommandProfile.Action.EXIT,"/gmc");
        assertEquals("gmsp",b.render(CameraCommandProfile.Action.ENTER));
        assertEquals("gmc",b.render(CameraCommandProfile.Action.EXIT));
        assertEquals(CameraCommandProfile.Mode.TOGGLE,a.mode);
        assertEquals("",a.get(CameraCommandProfile.Action.ENTER));
    }

    @Test void destinationTemplatesCanOmitDimensionForVanillaServer(){
        CameraCommandProfile p=new CameraCommandProfile();
        p.set(CameraCommandProfile.Action.DESTINATION,"/tp {x} {y} {z}");
        assertNull(CameraCommandProfile.validate(CameraCommandProfile.Action.DESTINATION,p.get(CameraCommandProfile.Action.DESTINATION)));
        assertNotNull(CameraCommandProfile.validate(CameraCommandProfile.Action.DESTINATION,"/tp {x} {y}"));
        assertEquals("tp 1 2 3",p.render(CameraCommandProfile.Action.DESTINATION,null,null,"1","2","3"));
    }
}