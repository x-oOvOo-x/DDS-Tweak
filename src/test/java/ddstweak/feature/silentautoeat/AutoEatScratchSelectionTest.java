package ddstweak.feature.silentautoeat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AutoEatScratchSelectionTest {
    @Test void preferEmptyScratchButNeverCurrentSelected(){
        assertEquals(2,AutoEatScratchSelection.choose(0,i->i==2||i==0,i->true));
        assertEquals(8,AutoEatScratchSelection.choose(3,i->i==8,i->false));
    }

    @Test void nonemptyDistinctSlotIsAllowedWhenEveryHotbarSlotIsOccupied(){
        assertEquals(1,AutoEatScratchSelection.choose(0,i->false,i->i==1||i==2));
        assertEquals(-1,AutoEatScratchSelection.choose(0,i->false,i->false));
    }

    @Test void experimentalMainhandStagingOwnsCurrentSelectedSlot(){
        assertEquals(4,AutoEatScratchSelection.selected(4,i->i==4));
        assertEquals(0,AutoEatScratchSelection.selected(0,i->true));
        assertEquals(8,AutoEatScratchSelection.selected(8,i->true));
        assertEquals(-1,AutoEatScratchSelection.selected(4,i->false));
        assertEquals(-1,AutoEatScratchSelection.selected(-1,i->true));
        assertEquals(-1,AutoEatScratchSelection.selected(9,i->true));
    }

    @Test void sourceAndScratchMustBelongToIndependentRegions(){
        assertTrue(AutoEatScratchSelection.isOwnedStageSource(9,0));
        assertTrue(AutoEatScratchSelection.isOwnedStageSource(35,8));
        assertFalse(AutoEatScratchSelection.isOwnedStageSource(8,1));
        assertFalse(AutoEatScratchSelection.isOwnedStageSource(36,1));
        assertFalse(AutoEatScratchSelection.isOwnedStageSource(9,9));
    }
}
