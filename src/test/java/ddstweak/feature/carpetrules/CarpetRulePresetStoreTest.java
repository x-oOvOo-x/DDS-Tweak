package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRulePresetStoreTest {
    @Test void preservesManagerIdentitiesAndVersionedJson(){
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        Map<CarpetRuleCatalog.RuleKey,String> targets=new LinkedHashMap<>();
        targets.put(new CarpetRuleCatalog.RuleKey("carpet","x"),"true");
        targets.put(new CarpetRuleCatalog.RuleKey("tis","x"),"false");
        assertTrue(store.put("生电","example-server",1234L,false,targets));
        CarpetRulePresetStore reloaded=CarpetRulePresetStore.parse(store.toJson());
        assertEquals(2,reloaded.get("生电").values().size());
        assertFalse(reloaded.get("生电").isSnapshot());
        assertEquals("example-server",reloaded.get("生电").source());
    }

    @Test void serverDifferencesAreCheckedWithoutCreatingServerRules(){
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        Map<CarpetRuleCatalog.RuleKey,String> targets=new LinkedHashMap<>();
        CarpetRuleCatalog.RuleKey a=new CarpetRuleCatalog.RuleKey("carpet","x");
        CarpetRuleCatalog.RuleKey b=new CarpetRuleCatalog.RuleKey("addon","y");
        targets.put(a,"true");
        targets.put(b,"false");
        store.put("test","",0L,false,targets);
        CarpetRuleCatalog catalog=new CarpetRuleCatalog();
        Object connection=new Object();
        catalog.connect(connection);
        assertTrue(catalog.replace(connection,Arrays.asList(new CarpetRuleCatalog.RemoteRule(a,"false",true))));
        assertEquals(CarpetRulePresetStore.DiffStatus.DIFFERENT,store.compare(catalog,"test").get(0).status());
        assertEquals(CarpetRulePresetStore.DiffStatus.MISSING,store.compare(catalog,"test").get(1).status());
        assertEquals(1,catalog.snapshot().size());
    }

    @Test void snapshotCannotBeEditedAndPresetValuesCanBeChangedAndRemoved() {
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet","commandPlayer");
        Map<CarpetRuleCatalog.RuleKey,String> entries=new LinkedHashMap<>();
        entries.put(key,"false");
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        assertTrue(store.put("snapshot","server",1L,true,entries));
        assertFalse(store.setTarget("snapshot",key,"true"));
        assertFalse(store.removeTarget("snapshot",key));
        assertFalse(store.put("snapshot","",2L,false,entries));
        assertTrue(store.put("preset","",3L,false,entries));
        assertTrue(store.setTarget("preset",key,"true"));
        assertEquals("true",store.get("preset").values().get(key));
        assertFalse(store.setTarget("preset",key,"true\n/kick @a"));
        assertTrue(store.removeTarget("preset",key));
        assertTrue(store.get("preset").values().isEmpty());
    }

    @Test void aNewServerRuleCanBeAddedToPresetAndRoundTrip() {
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        assertTrue(store.put("selected","server",42L,false,new LinkedHashMap<>()));
        CarpetRuleCatalog.RuleKey extra=new CarpetRuleCatalog.RuleKey("addon","newRule");
        assertTrue(store.setTarget("selected",extra,"true"));
        assertEquals("true",CarpetRulePresetStore.parse(store.toJson())
                .get("selected").values().get(extra));
        assertFalse(store.setTarget("selected",extra,"bad\ncommand"));
        assertTrue(store.removeTarget("selected",extra));
        assertTrue(store.get("selected").values().isEmpty());
    }

    @Test void malformedInputAndDuplicateKeysFailClosed(){
        String bad="{\"version\":1,\"presets\":[{\"name\":\"bad\",\"source\":\"\",\"createdAt\":0,\"snapshot\":false,\"rules\":[{\"manager\":\"carpet\",\"rule\":\"x\",\"value\":\"0\"},{\"manager\":\"carpet\",\"rule\":\"x\",\"value\":\"1\"}]}]}";
        assertNull(CarpetRulePresetStore.parse(bad).get("bad"));
        assertTrue(CarpetRulePresetStore.parse("{}").presets().isEmpty());
    }

    @Test void planApplyModeRenamingAndLegacyImportRemainCompatible(){
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        Map<CarpetRuleCatalog.RuleKey,String> selected=new LinkedHashMap<>();
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet","onlySelected");
        selected.put(key,"true");
        assertTrue(store.put("plan","",100,false,selected));
        assertTrue(store.setApplyMode("plan",CarpetRuleCommands.Mode.SET_DEFAULT));
        assertTrue(store.rename("plan","renamed"));
        assertNull(store.get("plan"));
        assertEquals("true",store.get("renamed").values().get(key));
        assertEquals(CarpetRuleCommands.Mode.SET_DEFAULT,CarpetRulePresetStore.parse(store.toJson())
                .get("renamed").applyMode());
        assertTrue(store.toJson().contains("\"version\":2"));

        String old="{\"version\":1,\"presets\":[{\"name\":\"legacy\",\"source\":\"\",\"createdAt\":0,"
                +"\"snapshot\":false,\"rules\":[{\"manager\":\"carpet\",\"rule\":\"x\",\"value\":\"true\"}]}]}";
        CarpetRulePresetStore loaded=CarpetRulePresetStore.parse(old);
        assertNotNull(loaded.get("legacy"));
        assertEquals(CarpetRuleCommands.Mode.TEMPORARY,loaded.get("legacy").applyMode());
        assertEquals(1,loaded.importJson(store.toJson()));
    }

    @Test void snapshotsCannotSwitchModeAndDuplicateNamesCannotBeRenamed(){
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        assertTrue(store.put("snapshot","",0,true,new LinkedHashMap<>()));
        assertTrue(store.put("plan","",0,false,new LinkedHashMap<>()));
        assertFalse(store.setApplyMode("snapshot",CarpetRuleCommands.Mode.SET_DEFAULT));
        assertFalse(store.setApplyMode("plan",CarpetRuleCommands.Mode.REMOVE_DEFAULT));
        assertFalse(store.rename("plan","snapshot"));
        assertEquals(2,store.presets().size());
    }

    @Test void retiredSnapshotUiDoesNotDeleteStoredLegacySnapshots(){
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        Map<CarpetRuleCatalog.RuleKey,String> targets=new LinkedHashMap<>();
        targets.put(new CarpetRuleCatalog.RuleKey("carpet","sampleRule"),"true");
        assertTrue(store.put("oldSnapshot","",1L,true,targets));
        assertTrue(store.put("activePlan","",2L,false,targets));
        assertEquals(1,store.editablePlans().size());
        assertEquals("activePlan",store.editablePlans().iterator().next().name());
        assertThrows(UnsupportedOperationException.class,()->store.editablePlans().clear());

        CarpetRulePresetStore reloaded=CarpetRulePresetStore.parse(store.toJson());
        assertTrue(reloaded.get("oldSnapshot").isSnapshot());
        assertEquals(1,reloaded.editablePlans().size());
        assertEquals("true",reloaded.get("oldSnapshot").values()
                .get(new CarpetRuleCatalog.RuleKey("carpet","sampleRule")));
        assertTrue(reloaded.delete("activePlan"));
        assertTrue(reloaded.editablePlans().isEmpty());
        assertNotNull(reloaded.get("oldSnapshot"));
    }

    @Test void comboDropdownTargetsRemainLocalUntilPlanIsExplicitlyApplied(){
        // Synthetic rule: do not assume these suggestions are the live rule's values.
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey(
                "test-manager","test-combination-rule");
        CarpetRuleCatalog catalog=new CarpetRuleCatalog();
        Object connection=new Object();
        catalog.connect(connection);
        assertTrue(catalog.replace(connection,java.util.Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(key,"none",true))));

        CarpetRuleMultiValue combination=new CarpetRuleMultiValue(
                Arrays.asList("none","all","passive","ambient","monster"),"none","all");
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        assertTrue(store.put("combination","",0L,false,new LinkedHashMap<>()));
        assertTrue(store.setTarget("combination",key,"none"));
        String target=combination.toggle(store.get("combination").values().get(key),"passive");
        assertEquals("passive",target);
        assertTrue(store.setTarget("combination",key,target));
        String partial=combination.toggle(target,"ambient");
        assertEquals("passive,ambient",partial);
        assertEquals("all",combination.toggle(partial,"monster"));
        assertEquals("passive,ambient",combination.toggle("all","monster"));
        assertNull(combination.toggle("unrecognized","ambient"));
        assertEquals("none",catalog.get(key).value());
        assertEquals("passive",store.get("combination").values().get(key));
        assertEquals("passive",CarpetRulePresetStore.parse(store.toJson())
                .get("combination").values().get(key));
    }


    @Test void selectedExportRejectsEmptyAndDoesNotLeakOtherPlansOrSnapshots(){
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        CarpetRuleCatalog.RuleKey a=new CarpetRuleCatalog.RuleKey("carpet","a");
        CarpetRuleCatalog.RuleKey b=new CarpetRuleCatalog.RuleKey("addon","b");
        assertTrue(store.put("one","",1L,false,java.util.Collections.singletonMap(a,"true")));
        assertTrue(store.put("other","",2L,false,java.util.Collections.singletonMap(b,"false")));
        assertTrue(store.put("empty","",3L,false,new LinkedHashMap<>()));
        assertTrue(store.put("snapshot","",4L,true,java.util.Collections.singletonMap(a,"true")));
        assertNull(store.exportJson("missing"));
        assertNull(store.exportJson("empty"));
        assertNull(store.exportJson("snapshot"));
        CarpetRulePresetStore selected=CarpetRulePresetStore.parse(store.exportJson("one"));
        assertNotNull(selected.get("one"));
        assertEquals("true",selected.get("one").values().get(a));
        assertNull(selected.get("other"));
        assertNull(selected.get("empty"));
        assertNull(selected.get("snapshot"));
        assertEquals(4,CarpetRulePresetStore.parse(store.toJson()).presets().size());
    }

}
