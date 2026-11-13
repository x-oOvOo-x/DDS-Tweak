package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRulePersistentChangeTest {
    private static final CarpetRuleCatalog.RuleKey KEY =
            new CarpetRuleCatalog.RuleKey("carpet", "commandPlayer");

    @Test void settingSameDefaultStillCreatesReviewedOperation() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        Object server = new Object();
        catalog.connect(server);
        catalog.replace(server, Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(KEY, "ops", true)));
        CarpetRulePersistentChange draft = new CarpetRulePersistentChange();
        assertTrue(draft.stage(catalog, KEY, CarpetRuleCommands.Mode.SET_DEFAULT, "ops"));
        assertEquals(CarpetRulePersistentChange.Status.READY, draft.validate(catalog));
        assertEquals("ops", draft.target());
        assertEquals(CarpetRuleCommands.Mode.SET_DEFAULT, draft.mode());
    }

    @Test void booleanFalseCanBeStagedAsPersistedTrueWithoutTemporaryWrite(){
        CarpetRuleCatalog catalog=new CarpetRuleCatalog();
        Object server=new Object();
        catalog.connect(server);
        catalog.replace(server,Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(KEY,"false",true)));
        CarpetRulePersistentChange draft=new CarpetRulePersistentChange();
        assertTrue(draft.stage(catalog,KEY,CarpetRuleCommands.Mode.SET_DEFAULT,"true"));
        assertEquals(CarpetRulePersistentChange.Status.READY,draft.validate(catalog));
        assertEquals("false",catalog.get(KEY).value());
        assertEquals("true",draft.target());
        assertEquals("carpet setDefault commandPlayer true",
                CarpetRuleCommands.build(KEY,draft.mode(),draft.target()));
    }

    @Test void booleanDefaultEditorKeepsModeAndDisplaysTheStagedTarget() throws Exception {
        // Normalize checkout line endings before asserting multi-line source contracts.
        String screen=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerScreen.java"))
                .replace("\r\n","\n");
        String renderer=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerRenderer.java"));
        int start=screen.indexOf("void chooseBoolean(boolean value)");
        int end=screen.indexOf("private void openValuePicker()",start);
        assertTrue(start>=0&&end>start);
        String handler=screen.substring(start,end);
        assertTrue(handler.contains("editMode==CarpetRuleCommands.Mode.SET_DEFAULT"));
        assertTrue(handler.contains("targetBox.setValue(target)"));
        assertTrue(handler.contains("stage();"));
        assertTrue(handler.contains("if(editMode!=CarpetRuleCommands.Mode.TEMPORARY)return;"));
        assertFalse(handler.contains("editMode=CarpetRuleCommands.Mode.TEMPORARY"));
        int resetStart=screen.indexOf("void restoreDefinitionDefault()");
        int resetEnd=screen.indexOf("boolean isNumberRule(",resetStart);
        assertTrue(resetStart>=0&&resetEnd>resetStart);
        String reset=screen.substring(resetStart,resetEnd);
        assertTrue(reset.contains("if(editMode==CarpetRuleCommands.Mode.SET_DEFAULT){\n            stage();"));
        assertTrue(renderer.contains("owner.targetBox==null?null:owner.targetBox.getValue()"));
        assertFalse(renderer.contains("owner.editMode==CarpetRuleCommands.Mode.SET_DEFAULT&&owner.targetBox!=null"));
        assertTrue(renderer.contains("String inFlight=owner.temporaryInFlight(entry.key())"));
        assertTrue(renderer.contains("inFlight==null?entry.value():inFlight"));
        // Changing the submission mode must not silently overwrite the target.
        int modeStart=screen.indexOf("void selectMode(CarpetRuleCommands.Mode mode)");
        int modeEnd=screen.indexOf("private String modeName()",modeStart);
        assertTrue(modeStart>=0&&modeEnd>modeStart);
        String mode=screen.substring(modeStart,modeEnd);
        assertFalse(mode.contains("targetBox.setValue("));
        int temporaryStart=handler.indexOf("if(editMode!=CarpetRuleCommands.Mode.TEMPORARY)return;");
        assertTrue(temporaryStart>=0);
        assertTrue(handler.substring(temporaryStart).contains("targetBox.setValue(target);"));
        assertTrue(handler.substring(temporaryStart).contains("submitTemporary(rule,target);"));
        assertTrue(screen.contains("if(rule.key().equals(selected)&&targetBox!=null)targetBox.setValue(target)"));
        assertTrue(screen.contains("if(editMode==CarpetRuleCommands.Mode.REMOVE_DEFAULT)"));
        int applyStart=screen.indexOf("private void apply()");
        int applyEnd=screen.indexOf("void refreshTarget()",applyStart);
        assertTrue(applyStart>=0&&applyEnd>applyStart);
        String apply=screen.substring(applyStart,applyEnd);
        assertFalse(apply.contains("persistentDraft.clear();\n                editMode=CarpetRuleCommands.Mode.TEMPORARY;"));
    }

    @Test void removalDoesNotRequireValueAndConflictsAreCaught() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        Object server = new Object();
        catalog.connect(server);
        catalog.replace(server, Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(KEY, "ops", true)));
        CarpetRulePersistentChange draft = new CarpetRulePersistentChange();
        assertTrue(draft.stage(catalog, KEY, CarpetRuleCommands.Mode.REMOVE_DEFAULT, null));
        assertNull(draft.target());
        catalog.update(server, new CarpetRuleCatalog.RemoteRule(KEY, "true", true));
        assertEquals(CarpetRulePersistentChange.Status.CHANGED, draft.validate(catalog));
        catalog.connect(new Object());
        assertEquals(CarpetRulePersistentChange.Status.STALE_CONNECTION, draft.validate(catalog));
    }

    @Test void legacyUnknownManagerAndInvalidTargetsNeverStage() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        Object server = new Object();
        catalog.connect(server);
        catalog.replace(server, Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(KEY, "ops", false)));
        CarpetRulePersistentChange draft = new CarpetRulePersistentChange();
        assertFalse(draft.stage(catalog, KEY, CarpetRuleCommands.Mode.SET_DEFAULT, "true"));
        assertEquals(CarpetRulePersistentChange.Status.NOT_STAGED, draft.validate(catalog));
        catalog.update(server, new CarpetRuleCatalog.RemoteRule(KEY, "ops", true));
        assertFalse(draft.stage(catalog, KEY, CarpetRuleCommands.Mode.SET_DEFAULT, "bad\ncommand"));
        assertFalse(draft.stage(catalog, KEY, CarpetRuleCommands.Mode.TEMPORARY, "true"));
        assertEquals(CarpetRulePersistentChange.Status.NOT_STAGED, draft.validate(catalog));
    }
}
