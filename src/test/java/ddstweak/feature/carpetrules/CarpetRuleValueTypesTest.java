package ddstweak.feature.carpetrules;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleValueTypesTest {
    @Test void booleanRulesRequireBooleanVocabularyOrDefinition(){
        assertTrue(CarpetRuleValueTypes.booleanEditor(
                "true","false",List.of("true","false")));
        assertTrue(CarpetRuleValueTypes.booleanEditor(
                "false","true",List.of()));
        assertFalse(CarpetRuleValueTypes.booleanEditor(
                "true","ops",List.of("true","false","ops")));
        assertFalse(CarpetRuleValueTypes.booleanEditor(
                "true","ops",List.of("true","false")));
        assertFalse(CarpetRuleValueTypes.booleanEditor(
                "true","ops",List.of()));
        assertFalse(CarpetRuleValueTypes.booleanEditor(
                "true",null,List.of()));
        assertFalse(CarpetRuleValueTypes.booleanEditor(
                "ops","ops",List.of("ops","true","false")));
    }
}
