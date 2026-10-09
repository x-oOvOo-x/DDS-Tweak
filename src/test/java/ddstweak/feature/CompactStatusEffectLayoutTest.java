package ddstweak.feature;

import ddstweak.feature.compactstatuseffects.CompactStatusEffectLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CompactStatusEffectLayoutTest {
    @Test void laysOutIndependentFramesInSixRowColumns(){
        assertEquals(24,CompactStatusEffectLayout.FRAME_SIZE);
        assertEquals(16,CompactStatusEffectLayout.ICON_SIZE);
        assertEquals(2,CompactStatusEffectLayout.GAP);
        assertEquals(6,CompactStatusEffectLayout.MAX_ROWS);
        assertEquals(32,CompactStatusEffectLayout.EXPANDED_HEIGHT);
        assertEquals(6,CompactStatusEffectLayout.rows(8));
        assertEquals(2,CompactStatusEffectLayout.columns(8));
        assertEquals(50,CompactStatusEffectLayout.width(8));
        assertEquals(154,CompactStatusEffectLayout.height(8));
    }

    @Test void hoverExpansionShiftsOnlyAffectedRowsAndFartherColumns(){
        assertEquals(8,CompactStatusEffectLayout.expandedExtraHeight());
        assertEquals(96,CompactStatusEffectLayout.hoverColumnShift(6,0,120,false));
        assertEquals(-96,CompactStatusEffectLayout.hoverColumnShift(6,0,120,true));
        assertEquals(8,CompactStatusEffectLayout.hoverRowShift(1,0));
        assertEquals(0,CompactStatusEffectLayout.hoverRowShift(6,0));
    }

    @Test void expandedRowsUseLeftCoordinatesFromTheirAnchor(){
        assertEquals(80,CompactStatusEffectLayout.expandedRowX(200,120,true));
        assertEquals(200,CompactStatusEffectLayout.expandedRowX(200,120,false));
    }

    @Test void expandedListUsesVanillaSpacingContract(){
        assertEquals(33,CompactStatusEffectLayout.expandedStep(5));
        assertEquals(26,CompactStatusEffectLayout.expandedStep(6));
        assertEquals(164,CompactStatusEffectLayout.expandedHeight(5));
        assertEquals(162,CompactStatusEffectLayout.expandedHeight(6));
    }

    @Test void leftSideKeepsFirstColumnNearestInventory(){
        assertEquals(26,CompactStatusEffectLayout.columnOffset(0,8,true));
        assertEquals(0,CompactStatusEffectLayout.columnOffset(6,8,true));
        assertEquals(0,CompactStatusEffectLayout.columnOffset(0,8,false));
        assertEquals(26,CompactStatusEffectLayout.columnOffset(6,8,false));
    }
}
