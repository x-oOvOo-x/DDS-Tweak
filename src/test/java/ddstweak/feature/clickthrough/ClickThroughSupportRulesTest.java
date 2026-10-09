package ddstweak.feature.clickthrough;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ClickThroughSupportRulesTest {
    @Test void jukeboxRequiresEntityAndRealUse(){
        assertFalse(ClickThroughSupportRules.jukebox(false,true,true));
        assertFalse(ClickThroughSupportRules.jukebox(true,false,false));
        assertTrue(ClickThroughSupportRules.jukebox(true,true,false));
        assertTrue(ClickThroughSupportRules.jukebox(true,false,true));
    }

    @Test void composterMatchesLevelAndInputSemantics(){
        assertTrue(ClickThroughSupportRules.composter(8,false));
        assertTrue(ClickThroughSupportRules.composter(0,true));
        assertFalse(ClickThroughSupportRules.composter(7,false));
        assertTrue(ClickThroughSupportRules.composter(8,true));
    }

    @Test void campfireIncludesCookingAndDousing(){
        assertFalse(ClickThroughSupportRules.campfire(false,false));
        assertTrue(ClickThroughSupportRules.campfire(true,false));
        assertTrue(ClickThroughSupportRules.campfire(false,true));
    }

    @Test void harvestAndAnchorRulesAreStateAware(){
        assertFalse(ClickThroughSupportRules.beehive(4,true));
        assertFalse(ClickThroughSupportRules.beehive(5,false));
        assertTrue(ClickThroughSupportRules.beehive(5,true));

        assertFalse(ClickThroughSupportRules.respawnAnchor(0,false));
        assertTrue(ClickThroughSupportRules.respawnAnchor(0,true));
        assertTrue(ClickThroughSupportRules.respawnAnchor(1,false));
        assertTrue(ClickThroughSupportRules.respawnAnchor(4,false));
    }

    @Test void cakeAndCandleCakeRequireAnActualAction(){
        assertFalse(ClickThroughSupportRules.cake(false,false));
        assertTrue(ClickThroughSupportRules.cake(true,false));
        assertTrue(ClickThroughSupportRules.cake(false,true));

        assertFalse(ClickThroughSupportRules.candleCake(false,false,false));
        assertTrue(ClickThroughSupportRules.candleCake(true,false,false));
        assertTrue(ClickThroughSupportRules.candleCake(false,true,false));
        assertTrue(ClickThroughSupportRules.candleCake(false,false,true));
    }

    @Test void enderChestAndLecternPreserveInertFrameClicks(){
        assertFalse(ClickThroughSupportRules.enderChest(false,false));
        assertFalse(ClickThroughSupportRules.enderChest(true,true));
        assertTrue(ClickThroughSupportRules.enderChest(true,false));

        assertFalse(ClickThroughSupportRules.lectern(false,false));
        assertTrue(ClickThroughSupportRules.lectern(true,false));
        assertTrue(ClickThroughSupportRules.lectern(false,true));
    }
}
