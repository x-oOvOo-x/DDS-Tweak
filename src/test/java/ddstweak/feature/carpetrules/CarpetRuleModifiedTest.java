package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleModifiedTest {
    @Test void ignoresUnknownDefaultsAndRecognizesNumericEquivalence(){
        assertEquals(CarpetRuleModified.State.UNKNOWN,CarpetRuleModified.classify("true",null));
        assertEquals(CarpetRuleModified.State.UNKNOWN,CarpetRuleModified.classify(null,"true"));
        assertEquals(CarpetRuleModified.State.UNCHANGED,CarpetRuleModified.classify("TRUE","true"));
        assertEquals(CarpetRuleModified.State.MODIFIED,CarpetRuleModified.classify("false","true"));
        assertEquals(CarpetRuleModified.State.UNCHANGED,CarpetRuleModified.classify("-1.00","-1"));
        assertEquals(CarpetRuleModified.State.UNCHANGED,CarpetRuleModified.classify("+20","20.0"));
        assertEquals(CarpetRuleModified.State.MODIFIED,CarpetRuleModified.classify("40","20"));
        assertEquals(CarpetRuleModified.State.UNCHANGED,CarpetRuleModified.classify("client","client"));
        assertEquals(CarpetRuleModified.State.MODIFIED,CarpetRuleModified.classify("client","off"));
    }

    @Test void quickFilterCountsOnlyRulesWithTrustedDefinitionDefaults(){
        CarpetRuleCatalog.RemoteRule changed=new CarpetRuleCatalog.RemoteRule(
                new CarpetRuleCatalog.RuleKey("carpet","changed"),"true",true);
        CarpetRuleCatalog.RemoteRule unchanged=new CarpetRuleCatalog.RemoteRule(
                new CarpetRuleCatalog.RuleKey("carpet","unchanged"),"20.0",true);
        CarpetRuleCatalog.RemoteRule unknown=new CarpetRuleCatalog.RemoteRule(
                new CarpetRuleCatalog.RuleKey("carpet","unknown"),"true",true);
        List<CarpetRuleCatalog.RemoteRule> rules=Arrays.asList(changed,unchanged,unknown);
        Map<CarpetRuleCatalog.RuleKey,String> definitions=new HashMap<>();
        definitions.put(changed.key(),"false");
        definitions.put(unchanged.key(),"20");
        List<CarpetRuleSidebar.Entry> entries=CarpetRuleSidebar.entries(rules,
                new CarpetRuleMetadata(),new CarpetRulePreferences(),Collections.emptyMap(),
                Collections.emptySet(),false,definitions);
        CarpetRuleSidebar.Entry modified=entries.stream()
                .filter(e->e.kind()==CarpetRuleSidebar.Kind.MODIFIED)
                .findFirst().orElseThrow(()->new AssertionError("Modified entry missing"));
        assertEquals(1,modified.count());
        assertTrue(modified.selectable());

        CarpetRuleSidebar.Entry unavailable=CarpetRuleSidebar.entries(rules,
                new CarpetRuleMetadata(),new CarpetRulePreferences(),Collections.emptyMap(),
                Collections.emptySet(),false,Collections.emptyMap()).stream()
                .filter(e->e.kind()==CarpetRuleSidebar.Kind.MODIFIED)
                .findFirst().orElseThrow(()->new AssertionError("Modified entry missing"));
        assertEquals(-1,unavailable.count());
        assertFalse(unavailable.selectable());
    }
}
