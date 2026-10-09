package ddstweak.feature.silentautoeat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SilentAutoEatRulesTest {
    @Test void triggerRequiresEverySafetyGate(){
        assertTrue(SilentAutoEatRules.shouldTrigger(true,true,false,false,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(false,true,false,false,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,false,false,false,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,true,true,false,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,true,false,true,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,true,false,false,false,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,true,false,false,true,15,14));
    }
}
