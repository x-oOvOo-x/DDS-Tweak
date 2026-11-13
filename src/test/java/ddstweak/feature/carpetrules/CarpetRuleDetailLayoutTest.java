package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleDetailLayoutTest {
    @Test void bothScreensShareDescriptionAndValuePositions(){
        int top=50;
        int panelHeight=830;
        assertEquals(110,CarpetRuleDetailLayout.descriptionTop(top));
        assertEquals(144,CarpetRuleDetailLayout.descriptionHeight(panelHeight));
        assertEquals(320,CarpetRuleDetailLayout.valuesTop(top,panelHeight));
        assertEquals(393,CarpetRuleDetailLayout.targetTop(top,panelHeight));
        assertEquals(449,CarpetRuleDetailLayout.modeTop(top,panelHeight));
    }

    @Test void shortScreensReduceDescriptionHeightBeforeClippingActions(){
        assertEquals(70,CarpetRuleDetailLayout.descriptionHeight(450));
        assertEquals(120,CarpetRuleDetailLayout.descriptionHeight(500));
        assertEquals(144,CarpetRuleDetailLayout.descriptionHeight(525));
        assertEquals(144,CarpetRuleDetailLayout.descriptionHeight(635));
        assertEquals(36,CarpetRuleDetailLayout.descriptionHeight(400));
        assertEquals(246,CarpetRuleDetailLayout.valuesTop(50,450));
        assertEquals(319,CarpetRuleDetailLayout.targetTop(50,450));
        // Value rows move down as description room grows, then stop at the 12-line cap.
        assertTrue(CarpetRuleDetailLayout.valuesTop(50,500)<
                CarpetRuleDetailLayout.valuesTop(50,635));
        assertEquals(CarpetRuleDetailLayout.valuesTop(50,635),
                CarpetRuleDetailLayout.valuesTop(50,830));
    }

    @Test void fiveTimesViewportCompressesDetailGapsWithoutCoveringFooter(){
        int top=50,panelHeight=347;
        assertEquals(192,CarpetRuleDetailLayout.valuesTop(top,panelHeight));
        assertEquals(245,CarpetRuleDetailLayout.targetTop(top,panelHeight));
        assertEquals(301,CarpetRuleDetailLayout.modeTop(top,panelHeight));
        assertTrue(CarpetRuleDetailLayout.modeOptionsFit(panelHeight));
        assertTrue(CarpetRuleDetailLayout.compact(panelHeight));
        assertFalse(CarpetRuleDetailLayout.compact(400));

        int modeRowsBottom=CarpetRuleDetailLayout.modeTop(top,panelHeight)+50;
        int presetFooterTop=top+panelHeight-28;
        int managerFooterTop=top+panelHeight-29;
        int statusBottom=CarpetRuleDetailLayout.modeTop(top,panelHeight)+54+9;
        assertTrue(modeRowsBottom+17<=presetFooterTop);
        assertTrue(statusBottom+4<=managerFooterTop);
        assertFalse(CarpetRuleDetailLayout.modeOptionsFit(300));

        assertFalse(CarpetRuleDetailLayout.modeOptionsFit(306));
        assertFalse(CarpetRuleDetailLayout.modeOptionsFit(311));
        assertTrue(CarpetRuleDetailLayout.modeOptionsFit(312));
        int dividerBottom=CarpetRuleDetailLayout.targetTop(0,312)+30;
        int modeHeadingTop=CarpetRuleDetailLayout.modeTop(0,312)-12;
        assertEquals(6,modeHeadingTop-dividerBottom);
    }
}
