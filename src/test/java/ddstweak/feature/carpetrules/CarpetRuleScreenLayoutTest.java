package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleScreenLayoutTest {
    @Test void fiveTimesGuiWidthKeepsListAndDetailPanelsUsable(){
        int width=758,margin=14;
        int sidebar=Math.min(172,Math.max(112,width/5));
        int middleLeft=margin+sidebar+8;
        int available=width-middleLeft-margin-8;
        int middle=CarpetRuleScreenLayout.middleWidth(available);
        int detail=available-middle;

        assertTrue(CarpetRuleScreenLayout.wide(width));
        assertEquals(333,middle);
        assertEquals(230,detail);
        assertTrue(middle>=CarpetRuleScreenLayout.MIN_MIDDLE_WIDTH);
        assertTrue(detail>=CarpetRuleScreenLayout.MIN_DETAIL_WIDTH);
    }

    @Test void narrowNavigationStartsBeforeTheThreeColumnLayoutGetsCramped(){
        assertFalse(CarpetRuleScreenLayout.wide(719));
        assertTrue(CarpetRuleScreenLayout.wide(720));
    }

    @Test void detailPanelGrowsGraduallyOnLargerScreens(){
        int width=1024,margin=14,sidebar=172;
        int middleLeft=margin+sidebar+8;
        int available=width-middleLeft-margin-8;
        int middle=CarpetRuleScreenLayout.middleWidth(available);
        assertEquals(558,middle);
        assertEquals(250,available-middle);
    }
}
