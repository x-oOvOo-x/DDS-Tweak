package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRulePreferencesTest {
    @Test void favoritesAndGroupsRoundTripWithoutLosingManagerIdentity() {
        CarpetRulePreferences p = new CarpetRulePreferences();
        CarpetRuleCatalog.RuleKey a = new CarpetRuleCatalog.RuleKey("carpet", "commandPlayer");
        CarpetRuleCatalog.RuleKey b = new CarpetRuleCatalog.RuleKey("tis", "commandPlayer");
        assertTrue(p.toggleFavorite(a));
        assertTrue(p.createGroup("生电"));
        assertTrue(p.setGroupMembership("生电", b, true));
        CarpetRulePreferences q = CarpetRulePreferences.parse(p.serialize());
        assertTrue(q.isFavorite(a));
        assertFalse(q.isFavorite(b));
        assertTrue(q.inGroup("生电", b));
        assertFalse(q.inGroup("生电", a));
    }

    @Test void renamingGroupRetainsMembershipAndRejectsCollisions() {
        CarpetRulePreferences p=new CarpetRulePreferences();
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("tis","rule");
        assertTrue(p.createGroup("old"));
        assertTrue(p.createGroup("other"));
        assertTrue(p.setGroupMembership("old",key,true));
        assertFalse(p.renameGroup("old","other"));
        assertTrue(p.renameGroup("old","new"));
        assertTrue(p.inGroup("new",key));
        assertFalse(p.inGroup("old",key));
        assertTrue(CarpetRulePreferences.parse(p.serialize()).inGroup("new",key));
    }

    @Test void emptyGroupCanReceiveFirstRuleAndRemoveItWithoutServerWrite() {
        CarpetRulePreferences p=new CarpetRulePreferences();
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet","newRule");
        assertTrue(p.createGroup("new"));
        assertFalse(p.inGroup("new",key));
        assertTrue(p.setGroupMembership("new",key,true));
        assertTrue(CarpetRulePreferences.parse(p.serialize()).inGroup("new",key));
        assertTrue(p.setGroupMembership("new",key,false));
        assertFalse(CarpetRulePreferences.parse(p.serialize()).inGroup("new",key));
    }

    @Test void malformedAndUnsupportedContentDoesNotInventRules() {
        assertTrue(CarpetRulePreferences.parse("{").favorites().isEmpty());
        assertTrue(CarpetRulePreferences.parse("{\"version\":999,\"favorites\":[]}").favorites().isEmpty());
        String invalid = "{\"version\":1,\"favorites\":[{\"manager\":\"bad name\",\"rule\":\"x\"},{\"manager\":\"carpet\",\"rule\":\"foo\"}]}";
        CarpetRulePreferences parsed = CarpetRulePreferences.parse(invalid);
        assertEquals(1, parsed.favorites().size()); // keep valid data only
    }
}
