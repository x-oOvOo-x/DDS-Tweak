package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRulePresentationTest {
    @Test void ruleCardHitIgnoresGapsAndRespectsScrollOffset(){
        CarpetRuleListScroll scroll=new CarpetRuleListScroll();
        assertEquals(0,scroll.hit(10,10,3,100));
        assertEquals(-1,scroll.hit(47,10,3,100));
        assertEquals(1,scroll.hit(52,10,3,100));
        assertEquals(-1,scroll.hit(110,10,3,100));
        scroll.scrollToFraction(1,10,100);
        int top=scroll.top(10,8);
        assertEquals(8,scroll.hit(top+12,10,10,100));
        assertEquals(-1,scroll.hit(top+38,10,10,100));
    }

    @Test void presetCardsUseTheSame36Plus6Geometry(){
        assertEquals(CarpetRuleListScroll.CARD_HEIGHT,CarpetRulePresetLayout.CARD_HEIGHT);
        assertEquals(CarpetRuleListScroll.STRIDE,CarpetRulePresetLayout.ROW);
        assertEquals(-1,CarpetRulePresetLayout.rowAt(46,10,100,0));
        assertEquals(1,CarpetRulePresetLayout.rowAt(52,10,100,0));
    }

    @Test void selectedAccentPaintsOnlyTheVisibleShapeDifference(){
        assertEquals(2,CarpetRuleUiStyle.SELECTION_BACKING_SHIFT);
        assertEquals(1,CarpetRuleUiStyle.TAB_BACKING_SHIFT);
        assertEquals(230,CarpetRuleUiStyle.SELECTED >>> 24);
        assertEquals(9,CarpetRuleUiStyle.CHECKBOX_SIZE);

        int left=20,top=10,right=120,bottom=46,radius=7;
        int cardShift=CarpetRuleUiStyle.SELECTION_BACKING_SHIFT;
        int back=CarpetRuleUiStyle.roundedCoverage(left-cardShift,top+17,
                left-cardShift,top,right-cardShift,bottom,radius);
        int front=CarpetRuleUiStyle.roundedCoverage(left-cardShift,top+17,
                left,top,right,bottom,radius);
        assertEquals(16,back-front);

        // Fully covered foreground pixels must never receive accent underneath.
        int x=left+radius,y=top+17;
        int hidden=Math.max(0,CarpetRuleUiStyle.roundedCoverage(x,y,
                left-cardShift,top,right-cardShift,bottom,radius)-
                CarpetRuleUiStyle.roundedCoverage(x,y,left,top,right,bottom,radius));
        assertEquals(0,hidden);
        assertEquals(0,CarpetRuleUiStyle.roundedCoverage(left-1,top+17,
                left,top,right,bottom,radius));
        // The shifted backing is visible inside the foreground's left bound
        // near the top corner. A scissor at x=left wrongly discards it.
        int cornerX=left+2,cornerY=top;
        int shifted=CarpetRuleUiStyle.roundedCoverage(cornerX,cornerY,
                left-cardShift,top,right-cardShift,bottom,radius);
        int foreground=CarpetRuleUiStyle.roundedCoverage(cornerX,cornerY,
                left,top,right,bottom,radius);
        assertTrue(shifted>foreground, "The outer accent must retain its round corner");

    }

    @Test void selectedAccentSupportsHalfPixelSubpixelCoverage(){
        int left=20,top=10,right=120,bottom=46;
        int back=CarpetRuleUiStyle.roundedCoverage(left+2,top,
                left-2,top,right-2,bottom,7.5f);
        int front=CarpetRuleUiStyle.roundedCoverage(left+2,top,
                left,top,right,bottom,7.5f);
        assertTrue(back>front, "7.5px corner must retain the left arc");
    }

    @Test void selfPaintedControlsKeepTheCalibratedVisualCenter(){
        int top=10;
        // Shift each self-painted label one pixel up from its previous calibrated offset.
        assertEquals(top+6,CarpetRuleUiStyle.visualTextY(top,20,9));
        assertEquals(top+7,CarpetRuleUiStyle.visualTextY(top,22,9));
        assertEquals(top+5,CarpetRuleUiStyle.visualTextY(top,18,9));

        // The icon was deliberately lowered 1px while the label baseline stayed put.
        int plusCanvasY=(20-12)/2;
        assertEquals(10,(plusCanvasY+2+plusCanvasY+10)/2);
    }

    @Test void sidebarExtraHalfPixelAppliesOnlyToChineseCharacters(){
        assertTrue(CarpetRuleSidebar.isChineseLabelCodepoint('中'));
        assertTrue(CarpetRuleSidebar.isChineseLabelCodepoint('附'));
        assertTrue(CarpetRuleSidebar.isChineseLabelCodepoint('（'));
        assertFalse(CarpetRuleSidebar.isChineseLabelCodepoint('C'));
        assertFalse(CarpetRuleSidebar.isChineseLabelCodepoint('/'));
        assertFalse(CarpetRuleSidebar.isChineseLabelCodepoint('7'));
    }

    @Test void vanillaValueShiftPreservesNormalRulePositions(){
        assertEquals(0,CarpetRuleListScroll.valueShift(false));
        assertEquals(24,CarpetRuleListScroll.valueShift(true));
        int cardRight=300;
        assertTrue(CarpetRuleListScroll.switchX(cardRight)+
                CarpetRuleListScroll.valueShift(true)+
                CarpetRuleListScroll.SWITCH_WIDTH<
                CarpetRuleListScroll.favoriteX(cardRight));
        assertTrue(CarpetRuleListScroll.choiceX(cardRight)+
                CarpetRuleListScroll.valueShift(true)+
                CarpetRuleListScroll.CHOICE_WIDTH<
                CarpetRuleListScroll.favoriteX(cardRight));
    }

    @Test void presetActionColumnAlignsWithMainCards(){
        int panelRight=360;
        int cardRight=panelRight-18;
        assertEquals(CarpetRuleListScroll.favoriteX(cardRight),
                CarpetRulePresetLayout.removeX(panelRight));
        assertEquals(18,CarpetRuleUiStyle.TARGET_ACTION_SIZE);
        assertEquals(4,CarpetRuleUiStyle.TARGET_ACTION_GAP);
        assertEquals(14,CarpetRuleUiStyle.TARGET_RIGHT_PADDING);
    }

    @Test void uppercaseEnumDefaultsAreOnlyNormalizedForDisplay(){
        assertEquals("vanilla",CarpetRuleUiStyle.displayDefinition("VANILLA"));
        assertEquals("default_mode",CarpetRuleUiStyle.displayDefinition("DEFAULT_MODE"));
        assertEquals("false",CarpetRuleUiStyle.displayDefinition("FALSE"));
        assertEquals("off",CarpetRuleUiStyle.displayDefinition("off"));
        assertEquals("inventory_with_shulker_box",
                CarpetRuleUiStyle.displayDefinition("inventory_with_shulker_box"));
        assertEquals("MixedCase",CarpetRuleUiStyle.displayDefinition("MixedCase"));
        assertEquals("-1",CarpetRuleUiStyle.displayDefinition("-1"));
        assertEquals("—",CarpetRuleUiStyle.displayDefinition("—"));
    }

    @Test void parentSectionFollowsSelectedChildKind(){
        assertEquals(CarpetRuleSidebar.Section.EXTENSIONS,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.OWNER));
        assertEquals(CarpetRuleSidebar.Section.EXTENSIONS,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.CARPET));
        assertEquals(CarpetRuleSidebar.Section.CATEGORIES,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.CATEGORY));
        assertEquals(CarpetRuleSidebar.Section.GROUPS,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.PLAN));
        assertEquals(CarpetRuleSidebar.Section.NONE,
                CarpetRuleSidebar.sectionFor(CarpetRuleSidebar.Kind.MODIFIED));
    }
}
