package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleChoiceMenuTest {
    @Test void dropdownRespectsVisibleRowsAndScrollOffset(){
        assertEquals(0,CarpetRuleChoiceMenu.rows(0,7));
        assertEquals(3,CarpetRuleChoiceMenu.rows(3,7));
        assertEquals(7,CarpetRuleChoiceMenu.rows(16,7));
        assertEquals(9,CarpetRuleChoiceMenu.rows(9,12)); // show all nine choices
        assertEquals(179,CarpetRuleChoiceMenu.height(9,12));
        assertEquals(141,CarpetRuleChoiceMenu.height(16,7));
        assertEquals(9,CarpetRuleChoiceMenu.maxOffset(16,7));
        assertEquals(9,CarpetRuleChoiceMenu.clampOffset(100,16,7));
        assertEquals(0,CarpetRuleChoiceMenu.clampOffset(-2,16,7));

        int top=150;
        assertEquals(-1,CarpetRuleChoiceMenu.hit(top+3,top,0,16,7));
        assertEquals(0,CarpetRuleChoiceMenu.hit(top+4,top,0,16,7));
        assertEquals(6,CarpetRuleChoiceMenu.hit(top+4+6*19,top,0,16,7));
        assertEquals(9,CarpetRuleChoiceMenu.hit(top+4,top,9,16,7));
        assertEquals(15,CarpetRuleChoiceMenu.hit(top+4+6*19,top,9,16,7));
        assertEquals(-1,CarpetRuleChoiceMenu.hit(top+4+7*19,top,9,16,7));
        assertEquals(-1,CarpetRuleChoiceMenu.hit(top+4+3*19,top,0,3,7));
        assertEquals(8,CarpetRuleChoiceMenu.hit(top+4+8*19,top,0,9,12));
    }

    @Test void stagedEnumValuesNeverMutateServerCatalog(){
        CarpetRuleCatalog catalog=new CarpetRuleCatalog();
        Object connection=new Object();
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet-dds-addition","autoRestock");
        catalog.connect(connection);
        assertTrue(catalog.replace(connection,java.util.Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(key,"off",true))));
        CarpetRuleChanges drafts=new CarpetRuleChanges();
        assertTrue(drafts.stage(catalog,key,"client"));
        assertEquals("client",drafts.desired(key));
        assertEquals("off",catalog.get(key).value());
        assertTrue(drafts.stage(catalog,key,"off"));
        assertNull(drafts.desired(key));
    }
}
