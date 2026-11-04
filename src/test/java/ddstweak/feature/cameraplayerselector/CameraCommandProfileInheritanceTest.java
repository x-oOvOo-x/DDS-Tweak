package ddstweak.feature.cameraplayerselector;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CameraCommandProfileInheritanceTest {
    @Test void unmodifiedServerKeysFollowChangingGlobalDefaults(){
        JsonObject defaults=new JsonObject();
        defaults.addProperty("playerFollow","/c follow {player}");
        JsonObject local=new JsonObject();
        local.addProperty("playerTeleport","/tp {player}");

        CameraCommandProfile first=CameraCommandProfileStore.merge(defaults,local);
        assertEquals("/c follow {player}",first.get(CameraCommandProfile.Action.PLAYER_FOLLOW));
        assertFalse(first.overrides(CameraCommandProfile.Action.PLAYER_FOLLOW));

        defaults.addProperty("playerFollow","/follow {player}");
        CameraCommandProfile next=CameraCommandProfileStore.merge(defaults,
                CameraCommandProfileStore.serializeOverrides(first));
        assertEquals("/follow {player}",next.get(CameraCommandProfile.Action.PLAYER_FOLLOW));
        assertEquals("/tp {player}",next.get(CameraCommandProfile.Action.PLAYER_TP));
    }

    @Test void explicitEmptyStringDisablesInsteadOfInheriting(){
        JsonObject global=new JsonObject();
        global.addProperty("playerFollow","/c follow {player}");
        JsonObject local=new JsonObject();
        local.addProperty("playerFollow","");
        CameraCommandProfile merged=CameraCommandProfileStore.merge(global,local);
        assertTrue(merged.overrides(CameraCommandProfile.Action.PLAYER_FOLLOW));
        assertEquals("",merged.get(CameraCommandProfile.Action.PLAYER_FOLLOW));
        assertEquals("",CameraCommandProfileStore.serializeOverrides(merged).get("playerFollow").getAsString());
        merged.restore(CameraCommandProfile.Action.PLAYER_FOLLOW,
                CameraCommandProfileStore.merge(global,null));
        assertFalse(merged.overrides(CameraCommandProfile.Action.PLAYER_FOLLOW));
        assertFalse(CameraCommandProfileStore.serializeOverrides(merged).has("playerFollow"));
    }

    @Test void legacyFullSnapshotNeverInfersMissingOverridesFromEqualValues(){
        JsonObject global=new JsonObject();
        JsonObject oldServer=new JsonObject();
        CameraCommandProfile defaults=new CameraCommandProfile();
        global.addProperty("spectatorControlMode","TOGGLE");
        oldServer.addProperty("spectatorControlMode","TOGGLE");
        for(CameraCommandProfile.Action action:CameraCommandProfile.Action.values()){
            global.addProperty(action.key,defaults.get(action));
            oldServer.addProperty(action.key,defaults.get(action));
        }
        CameraCommandProfile merged=CameraCommandProfileStore.merge(global,oldServer);
        assertTrue(merged.overridesMode());
        for(CameraCommandProfile.Action action:CameraCommandProfile.Action.values())
            assertTrue(merged.overrides(action));
        assertEquals(oldServer,CameraCommandProfileStore.serializeOverrides(merged));
    }

    @Test void serverIdentityMustNotDisappearOrChangeBeforeSaving(){
        assertDoesNotThrow(()->CameraCommandProfileStore.requireServerIdentity("server:alpha","server:alpha"));
        assertThrows(IllegalStateException.class,
                ()->CameraCommandProfileStore.requireServerIdentity("server:alpha",""));
        assertThrows(IllegalStateException.class,
                ()->CameraCommandProfileStore.requireServerIdentity("server:alpha","server:beta"));
        assertThrows(IllegalStateException.class,
                ()->CameraCommandProfileStore.requireServerIdentity("","server:alpha"));
    }

    @Test void restoringModeDoesNotChangeUnrelatedFields(){
        CameraCommandProfile profile=new CameraCommandProfile();
        CameraCommandProfile global=new CameraCommandProfile();
        profile.set(CameraCommandProfile.Action.PLAYER_TP,"/tp {player}");
        profile.setMode(CameraCommandProfile.Mode.SEPARATE);
        profile.restoreMode(global);
        assertFalse(profile.overridesMode());
        assertTrue(profile.overrides(CameraCommandProfile.Action.PLAYER_TP));
    }
}
