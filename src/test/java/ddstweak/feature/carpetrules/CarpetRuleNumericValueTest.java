package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleNumericValueTest {
    @Test void acceptsSignedDecimalAndIntegerValues(){
        assertTrue(CarpetRuleNumericValue.valid("0"));
        assertTrue(CarpetRuleNumericValue.valid("-42"));
        assertTrue(CarpetRuleNumericValue.valid("0.125"));
        assertTrue(CarpetRuleNumericValue.valid(".5"));
        assertFalse(CarpetRuleNumericValue.valid(""));
        assertFalse(CarpetRuleNumericValue.valid("NaN"));
        assertFalse(CarpetRuleNumericValue.valid("1 2"));
        assertFalse(CarpetRuleNumericValue.valid("true"));
    }

    @Test void limitsInlineEditorsToNumericRulesWithoutEnums(){
        assertTrue(CarpetRuleNumericValue.editable("0","0",false));
        assertFalse(CarpetRuleNumericValue.editable("0","0",true));
        assertFalse(CarpetRuleNumericValue.editable("0","ops",false));
        assertFalse(CarpetRuleNumericValue.editable("ops",null,false));
    }
}
