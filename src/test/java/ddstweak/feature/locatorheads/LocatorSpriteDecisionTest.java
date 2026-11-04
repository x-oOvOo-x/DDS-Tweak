package ddstweak.feature.locatorheads;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies the production marker ownership decision without a Minecraft renderer. */
final class LocatorSpriteDecisionTest {
    @Test void disabledFeatureAlwaysUsesVanilla(){
        for(boolean filtered:new boolean[]{false,true})
            for(boolean drewHead:new boolean[]{false,true})
                assertEquals(LocatorSpriteDecision.Outcome.ORIGINAL,
                        LocatorSpriteDecision.decide(false,filtered,drewHead));
    }

    @Test void excludedPlayerNeverDrawsVanillaOrDt(){
        for(boolean drewHead:new boolean[]{false,true})
            assertEquals(LocatorSpriteDecision.Outcome.HIDDEN,
                    LocatorSpriteDecision.decide(true,true,drewHead));
    }

    @Test void availableSkinReplacesVanillaAndMissingSkinFallsBack(){
        assertEquals(LocatorSpriteDecision.Outcome.HEAD,
                LocatorSpriteDecision.decide(true,false,true));
        assertEquals(LocatorSpriteDecision.Outcome.ORIGINAL,
                LocatorSpriteDecision.decide(true,false,false));
    }
}
