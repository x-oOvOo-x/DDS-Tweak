package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRulePresetTransferTest {
    @Test void distinctPlanNamesExportToSafeSeparateFiles() throws Exception {
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet","example");
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        assertTrue(store.put("计划/A","",1,false,Collections.singletonMap(key,"true")));
        assertTrue(store.put("计划\\A","",2,false,Collections.singletonMap(key,"false")));
        Path first=CarpetRulePresetTransfer.fileForPlan(directory,"计划/A");
        Path second=CarpetRulePresetTransfer.fileForPlan(directory,"计划\\A");
        assertEquals(first,CarpetRulePresetTransfer.fileForPlan(directory,"计划/A"));
        assertNotEquals(first,second);
        assertEquals(directory,first.getParent());
        assertFalse(first.getFileName().toString().contains("/"));
        assertFalse(first.getFileName().toString().contains("\\"));
        assertTrue(CarpetRulePresetTransfer.exportSelectedTo(first,store,"计划/A"));
        assertTrue(CarpetRulePresetTransfer.exportSelectedTo(second,store,"计划\\A"));
        assertTrue(Files.isRegularFile(first));
        assertTrue(Files.isRegularFile(second));
        CarpetRulePresetStore fromFirst=new CarpetRulePresetStore();
        CarpetRulePresetStore fromSecond=new CarpetRulePresetStore();
        assertEquals(1,CarpetRulePresetTransfer.importFrom(first,fromFirst));
        assertEquals(1,CarpetRulePresetTransfer.importFrom(second,fromSecond));
        assertNotNull(fromFirst.get("计划/A"));
        assertNull(fromFirst.get("计划\\A"));
        assertNotNull(fromSecond.get("计划\\A"));
        assertNull(fromSecond.get("计划/A"));
        assertThrows(IllegalArgumentException.class,
                ()->CarpetRulePresetTransfer.fileForPlan(directory," "));
    }


    @TempDir Path directory;

    @Test void exportsAndImportsPresetsWithoutLosingManagerIdentity(){
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("tis","sameName");
        CarpetRulePresetStore from=new CarpetRulePresetStore();
        assertTrue(from.put("template","server",123L,false,Collections.singletonMap(key,"true")));
        Path file=directory.resolve("export.json");
        assertTrue(CarpetRulePresetTransfer.exportTo(file,from));
        CarpetRulePresetStore into=new CarpetRulePresetStore();
        assertEquals(1,CarpetRulePresetTransfer.importFrom(file,into));
        assertEquals("true",into.get("template").values().get(key));
    }

    @Test void collisionAndDamagedRuleRejectEntireTransfer(){
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet","x");
        CarpetRulePresetStore source=new CarpetRulePresetStore();
        source.put("new","",1,false,Collections.singletonMap(key,"true"));
        CarpetRulePresetStore target=new CarpetRulePresetStore();
        target.put("new","",2,false,Collections.singletonMap(key,"false"));
        assertEquals(-1,target.importJson(source.toJson()));
        assertEquals("false",target.get("new").values().get(key));
        String valid=source.toJson();
        String damaged=valid.replace("\"manager\":\"carpet\"","\"manager\":\"bad name\"");
        CarpetRulePresetStore clean=new CarpetRulePresetStore();
        assertEquals(-1,clean.importJson(damaged));
        assertEquals(0,clean.presets().size());
        assertEquals(1,target.presets().size());
    }

    @Test void malformedInputCannotPartiallyOverwrite(){
        CarpetRulePresetStore target=new CarpetRulePresetStore();
        assertEquals(-1,target.importJson("{"));
        assertEquals(0,target.importJson("{\"version\":2,\"presets\":[]}"));
        assertEquals(0,target.importJson("{\"version\":1,\"presets\":[]}"));
        assertEquals(0,target.presets().size());
    }

    @Test void emptyPlanCannotOverwriteExistingExportAndOnlySelectedPlanIsTransferred(){
        CarpetRuleCatalog.RuleKey rule=new CarpetRuleCatalog.RuleKey("carpet","target");
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        assertTrue(store.put("empty","",1L,false,Collections.emptyMap()));
        assertTrue(store.put("selected","",2L,false,Collections.singletonMap(rule,"true")));
        assertTrue(store.put("other","",3L,false,Collections.singletonMap(rule,"false")));
        Path file=directory.resolve("selected.json");
        assertFalse(CarpetRulePresetTransfer.exportSelectedTo(file,store,"empty"));
        assertFalse(java.nio.file.Files.exists(file));
        assertTrue(CarpetRulePresetTransfer.exportSelectedTo(file,store,"selected"));
        CarpetRulePresetStore imported=new CarpetRulePresetStore();
        assertEquals(1,CarpetRulePresetTransfer.importFrom(file,imported));
        assertNotNull(imported.get("selected"));
        assertNull(imported.get("empty"));
        assertNull(imported.get("other"));
        assertFalse(CarpetRulePresetTransfer.exportSelectedTo(file,store,"empty"));
        CarpetRulePresetStore unchanged=new CarpetRulePresetStore();
        assertEquals(1,CarpetRulePresetTransfer.importFrom(file,unchanged));
    }

}
