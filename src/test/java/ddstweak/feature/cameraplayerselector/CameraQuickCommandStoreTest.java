package ddstweak.feature.cameraplayerselector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CameraQuickCommandStoreTest {
    @Test void acceptsSingleProxyAndOrdinaryCommands(){
        assertNull(CameraQuickCommandStore.validate("生存服","/server survival"));
        assertNull(CameraQuickCommandStore.validate("菜单","/menu"));
        assertNull(CameraQuickCommandStore.validate("家","home"));
        CameraQuickCommandStore.Entry entry=new CameraQuickCommandStore.Entry(null,"生存服","/server survival");
        assertFalse(entry.id.isEmpty());
        assertEquals("server survival",CameraQuickCommandStore.commandToSend(entry));
    }

    @Test void acceptsChatTriggersButNotSlashPrefixedTriggers(){
        assertNull(CameraQuickCommandStore.validate("传送","!!tp Steve"));
        assertNull(CameraQuickCommandStore.validate("返回","!!back"));
        assertNull(CameraQuickCommandStore.validate("菜单","#menu"));
        assertEquals("!!back",CameraQuickCommandStore.commandToSend(
                new CameraQuickCommandStore.Entry(null,"返回","!!back")));
        assertNotNull(CameraQuickCommandStore.validate("bad","/!!back"));
        assertNotNull(CameraQuickCommandStore.validate("bad","!!"));
        assertNotNull(CameraQuickCommandStore.validate("bad","!"));
        assertNotNull(CameraQuickCommandStore.validate("bad","!!tp Steve;/op"));
        assertNotNull(CameraQuickCommandStore.validate("bad","!!tp Steve\n/op"));
    }

    @Test void rejectsEmptyUnsafeOrMultiCommandTemplates(){
        assertNotNull(CameraQuickCommandStore.validate("","/server lobby"));
        assertNotNull(CameraQuickCommandStore.validate("test",""));
        assertNotNull(CameraQuickCommandStore.validate("test","/"));
        assertNotNull(CameraQuickCommandStore.validate("test","//server lobby"));
        assertNotNull(CameraQuickCommandStore.validate("test","/server lobby\n/op"));
        assertNotNull(CameraQuickCommandStore.validate("test","/server lobby;/op"));
        assertNotNull(CameraQuickCommandStore.validate("test","/server lobby\r/test"));
        assertNotNull(CameraQuickCommandStore.validate("test","/server lobby"+(char)0));
    }

    @Test void mergesGlobalAndLocalWithoutReplacingLegacyLists(){
        com.google.gson.JsonObject root=sample();
        java.util.List<CameraQuickCommandStore.Entry> entries=CameraQuickCommandStore.merge(root,"server:alpha");
        assertEquals(2,entries.size());
        assertEquals("home",entries.get(0).id);
        assertEquals(CameraQuickCommandStore.Scope.GLOBAL,entries.get(0).scope);
        assertEquals("warp",entries.get(1).id);
        assertEquals(CameraQuickCommandStore.Scope.LOCAL,entries.get(1).scope);
        java.util.List<CameraQuickCommandStore.Entry> beta=CameraQuickCommandStore.merge(root,"server:beta");
        assertEquals(2,beta.size());
        assertEquals("home",beta.get(0).id);
        assertEquals(CameraQuickCommandStore.Scope.GLOBAL,beta.get(0).scope);
        assertEquals("other",beta.get(1).id);
        assertEquals(CameraQuickCommandStore.Scope.LOCAL,beta.get(1).scope);
    }

    @Test void localMutationsWithoutServerIdentityMustFailRatherThanPretendToSave(){
        CameraQuickCommandStore.Entry local=new CameraQuickCommandStore.Entry(
                "local-1","本地","/home",CameraQuickCommandStore.Scope.LOCAL);
        assertThrows(IllegalStateException.class,
                ()->CameraQuickCommandStore.upsert(null,local,CameraQuickCommandStore.Scope.GLOBAL));
        assertThrows(IllegalStateException.class,
                ()->CameraQuickCommandStore.upsert(null,
                        new CameraQuickCommandStore.Entry("local-1","移动","/home",
                                CameraQuickCommandStore.Scope.GLOBAL),CameraQuickCommandStore.Scope.LOCAL));
        assertThrows(IllegalStateException.class,
                ()->CameraQuickCommandStore.delete(null,"local-1",CameraQuickCommandStore.Scope.LOCAL));
    }

    @Test void movingEntryBetweenScopesPreservesOtherServers(){
        com.google.gson.JsonObject root=sample();
        CameraQuickCommandStore.Entry moved=new CameraQuickCommandStore.Entry(
                "warp","公共传送","/warp all",CameraQuickCommandStore.Scope.GLOBAL);
        CameraQuickCommandStore.upsertIn(root,"server:alpha",moved,CameraQuickCommandStore.Scope.LOCAL);
        assertEquals(2,CameraQuickCommandStore.merge(root,"server:alpha").size());
        assertEquals(0,root.getAsJsonObject("servers").getAsJsonArray("server:alpha").size());
        assertEquals(1,root.getAsJsonObject("servers").getAsJsonArray("server:beta").size());
        assertEquals(2,root.getAsJsonArray("global").size());
    }

    @Test void deletionTargetsOneScopeOnly(){
        com.google.gson.JsonObject root=sample();
        assertTrue(CameraQuickCommandStore.deleteFrom(root,"server:alpha","warp",CameraQuickCommandStore.Scope.LOCAL));
        assertFalse(CameraQuickCommandStore.deleteFrom(root,"server:alpha","warp",CameraQuickCommandStore.Scope.LOCAL));
        assertEquals(1,CameraQuickCommandStore.merge(root,"server:alpha").size());
        assertEquals(1,root.getAsJsonObject("servers").getAsJsonArray("server:beta").size());
        assertEquals(1,root.getAsJsonArray("global").size());
    }

    @Test void seedPresetsOnlyOnceAndNeverRestoreDeletedItems(){
        com.google.gson.JsonObject root=sample();
        String[] names={"生存服","创造服","镜像服"};
        assertTrue(CameraQuickCommandStore.ensurePresets(root,names));
        assertEquals(4,root.getAsJsonArray("global").size());
        assertFalse(CameraQuickCommandStore.ensurePresets(root,names));
        assertTrue(CameraQuickCommandStore.deleteFrom(root,"","preset-survival",CameraQuickCommandStore.Scope.GLOBAL));
        assertFalse(CameraQuickCommandStore.ensurePresets(root,names));
        assertEquals(3,root.getAsJsonArray("global").size());
        assertEquals(1,root.getAsJsonObject("servers").getAsJsonArray("server:alpha").size());
    }

    @Test void invalidEntryCannotBeSilentlyDroppedOnEdit(){
        com.google.gson.JsonObject root=sample();
        root.getAsJsonArray("global").add(new com.google.gson.JsonPrimitive("malformed"));
        assertThrows(IllegalStateException.class,()->CameraQuickCommandStore.merge(root,"server:alpha"));
        assertEquals(2,root.getAsJsonArray("global").size());
    }

    @Test void malformedScopeTypesAreRejected(){
        com.google.gson.JsonObject root=sample();
        root.addProperty("global","not-an-array");
        assertThrows(IllegalStateException.class,()->CameraQuickCommandStore.merge(root,"server:alpha"));
    }

    private static com.google.gson.JsonObject sample(){
        com.google.gson.JsonObject root=new com.google.gson.JsonObject();
        com.google.gson.JsonArray global=new com.google.gson.JsonArray();
        global.add(item("home","回家","/home"));
        root.add("global",global);
        com.google.gson.JsonObject servers=new com.google.gson.JsonObject();
        com.google.gson.JsonArray alpha=new com.google.gson.JsonArray();
        alpha.add(item("warp","传送","/warp spawn"));
        servers.add("server:alpha",alpha);
        com.google.gson.JsonArray beta=new com.google.gson.JsonArray();
        beta.add(item("other","其他","/menu"));
        servers.add("server:beta",beta);
        root.add("servers",servers);
        return root;
    }

    private static com.google.gson.JsonObject item(String id,String name,String command){
        com.google.gson.JsonObject result=new com.google.gson.JsonObject();
        result.addProperty("id",id);
        result.addProperty("name",name);
        result.addProperty("command",command);
        return result;
    }

    @Test void editorUpdatesDoNotChangeEntryIdentity(){
        CameraQuickCommandStore.Entry original=new CameraQuickCommandStore.Entry(null,"大厅","/server lobby");
        CameraQuickCommandStore.Entry changed=new CameraQuickCommandStore.Entry(original.id,"生存","/server survival");
        assertEquals(original.id,changed.id);
        assertEquals("server survival",CameraQuickCommandStore.commandToSend(changed));
    }
}
