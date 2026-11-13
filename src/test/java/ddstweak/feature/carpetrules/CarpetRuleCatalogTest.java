package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleCatalogTest {
    private static CarpetRuleCatalog.RemoteRule rule(String manager, String name, String value) {
        return new CarpetRuleCatalog.RemoteRule(new CarpetRuleCatalog.RuleKey(manager,name),value,true);
    }

    @Test void sameNameAcrossManagersRemainsIndependent() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        Object server = new Object();
        catalog.connect(server);
        assertTrue(catalog.replace(server, Arrays.asList(
                rule("carpet","same","false"),rule("tis","same","true"))));
        assertEquals(2,catalog.snapshot().size());
        assertEquals("false",catalog.get(new CarpetRuleCatalog.RuleKey("carpet","same")).value());
        assertEquals("true",catalog.get(new CarpetRuleCatalog.RuleKey("tis","same")).value());
    }

    @Test void reconnectCannotConsumeOldSnapshotOrIncrementalUpdate() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        Object old = new Object(), next = new Object();
        catalog.connect(old);
        assertFalse(catalog.update(old,rule("carpet","one","true")));
        assertTrue(catalog.replace(old,Collections.singletonList(rule("carpet","one","true"))));
        catalog.connect(next);
        assertFalse(catalog.isReady());
        assertFalse(catalog.replace(old,Collections.singletonList(rule("carpet","old","false"))));
        assertFalse(catalog.update(old,rule("carpet","old","false")));
        assertTrue(catalog.replace(next,Collections.singletonList(rule("new-manager","newRule","0"))));
        assertEquals(1,catalog.snapshot().size());
    }

    @Test void malformedSnapshotDoesNotPartiallyOverwriteExistingRules() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        Object server = new Object();
        catalog.connect(server);
        assertTrue(catalog.replace(server,Collections.singletonList(rule("carpet","one","false"))));
        assertFalse(catalog.replace(server,Arrays.asList(rule("tis","duplicate","true"),rule("tis","duplicate","false"))));
        assertEquals("false",catalog.get(new CarpetRuleCatalog.RuleKey("carpet","one")).value());
    }

    @Test void stagedChangesDetectConflictsAndStaleConnection() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        CarpetRuleChanges changes = new CarpetRuleChanges();
        Object old = new Object();
        CarpetRuleCatalog.RuleKey key = new CarpetRuleCatalog.RuleKey("carpet","one");
        catalog.connect(old);
        assertTrue(catalog.replace(old,Collections.singletonList(rule("carpet","one","false"))));
        assertTrue(changes.stage(catalog,key,"true"));
        assertEquals(CarpetRuleChanges.Status.READY,changes.preview(catalog).get(0).status());
        assertTrue(catalog.update(old,rule("carpet","one","other")));
        assertEquals(CarpetRuleChanges.Status.CONFLICT,changes.preview(catalog).get(0).status());
        catalog.connect(new Object());
        assertEquals(CarpetRuleChanges.Status.STALE_CONNECTION,changes.preview(catalog).get(0).status());
    }

    @Test void quickToggleDraftCanBeToggledBackWithoutAnyServerWrite(){
        CarpetRuleCatalog c=new CarpetRuleCatalog();
        Object connection=new Object();
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet","bool");
        c.connect(connection);
        c.replace(connection,java.util.Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(key,"false",true)));
        CarpetRuleChanges changes=new CarpetRuleChanges();
        assertTrue(changes.stage(c,key,"true"));
        assertEquals("true",changes.desired(key));
        assertEquals("false",c.get(key).value());
        assertTrue(changes.stage(c,key,"false"));
        assertNull(changes.desired(key));
        assertEquals("false",c.get(key).value());
    }

    @Test void submittingOneDraftKeepsOtherDrafts() {
        CarpetRuleCatalog catalog=new CarpetRuleCatalog();
        Object session=new Object();
        CarpetRuleCatalog.RuleKey a=new CarpetRuleCatalog.RuleKey("carpet","a");
        CarpetRuleCatalog.RuleKey b=new CarpetRuleCatalog.RuleKey("tis","b");
        catalog.connect(session);
        catalog.replace(session,Arrays.asList(rule("carpet","a","false"),rule("tis","b","false")));
        CarpetRuleChanges changes=new CarpetRuleChanges();
        assertTrue(changes.stage(catalog,a,"true"));
        assertTrue(changes.stage(catalog,b,"true"));
        assertTrue(changes.remove(a));
        assertEquals(1,changes.preview(catalog).size());
        assertEquals(b,changes.preview(catalog).get(0).key());
        assertFalse(changes.remove(a));
    }

    @Test void metadataCannotAddRemoteRules() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        CarpetRuleMetadata metadata = new CarpetRuleMetadata();
        CarpetRuleCatalog.RuleKey key = new CarpetRuleCatalog.RuleKey("client-only","extra");
        metadata.put(key,new CarpetRuleMetadata.Description("Extra","Local-only",
                Arrays.asList("CLIENT","COMMAND"),"local"));
        Object server = new Object();
        catalog.connect(server);
        assertTrue(catalog.replace(server,Collections.emptyList()));
        assertTrue(catalog.snapshot().isEmpty());
        assertEquals(2,metadata.find(key).categories().size());
    }

    @Test void commandsRejectControlCharactersAndDoNotInventRpc() {
        CarpetRuleCatalog.RuleKey key = new CarpetRuleCatalog.RuleKey("carpet","commandPlayer");
        assertEquals("carpet commandPlayer true",CarpetRuleCommands.build(key,CarpetRuleCommands.Mode.TEMPORARY,"true"));
        assertEquals("carpet setDefault commandPlayer ops",CarpetRuleCommands.build(key,CarpetRuleCommands.Mode.SET_DEFAULT,"ops"));
        assertEquals("carpet removeDefault commandPlayer",CarpetRuleCommands.build(key,CarpetRuleCommands.Mode.REMOVE_DEFAULT,null));
        assertThrows(IllegalArgumentException.class,()->CarpetRuleCommands.build(key,CarpetRuleCommands.Mode.TEMPORARY,"true\n/kill @a"));
        assertThrows(IllegalArgumentException.class,()->CarpetRuleCommands.build(new CarpetRuleCatalog.RuleKey("bad manager","x"),CarpetRuleCommands.Mode.TEMPORARY,"true"));
    }
}
