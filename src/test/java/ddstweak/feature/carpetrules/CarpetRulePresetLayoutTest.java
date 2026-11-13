package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRulePresetLayoutTest {
    @Test void planCardsRejectGapClicksAndPreserveScrollOffset(){
        assertEquals(3,CarpetRulePresetLayout.rowAt(100,100,400,3));
        assertEquals(3,CarpetRulePresetLayout.rowAt(135,100,400,3));
        assertEquals(-1,CarpetRulePresetLayout.rowAt(136,100,400,3));
        assertEquals(4,CarpetRulePresetLayout.rowAt(142,100,400,3));
        assertEquals(-1,CarpetRulePresetLayout.rowAt(99,100,400,3));
        assertEquals(-1,CarpetRulePresetLayout.rowAt(400,100,400,3));
    }

    @Test void createAndEditExpandInPlaceWithoutCoveringFollowingCards(){
        assertEquals(22,CarpetRulePresetLayout.createHeight(false));
        assertEquals(22,CarpetRulePresetLayout.CREATE_HEIGHT);
        assertEquals(88,CarpetRulePresetLayout.createHeight(true));
        assertEquals(52,CarpetRulePresetLayout.EXPANSION);
        assertEquals(6,CarpetRulePresetLayout.GAP);

        int top=100,offset=2,selected=3,bottom=450;
        assertEquals(100,CarpetRulePresetLayout.rowY(2,offset,top,selected));
        assertEquals(142,CarpetRulePresetLayout.rowY(3,offset,top,selected));
        assertEquals(236,CarpetRulePresetLayout.rowY(4,offset,top,selected));
        assertEquals(3,CarpetRulePresetLayout.rowAt(220,top,bottom,offset,selected));
        assertEquals(3,CarpetRulePresetLayout.rowAt(229,top,bottom,offset,selected));
        assertEquals(-1,CarpetRulePresetLayout.rowAt(231,top,bottom,offset,selected));
        assertEquals(4,CarpetRulePresetLayout.rowAt(236,top,bottom,offset,selected));
        assertEquals(-1,CarpetRulePresetLayout.rowAt(272,top,bottom,offset,selected));
        assertEquals(5,CarpetRulePresetLayout.rowAt(278,top,bottom,offset,selected));
    }

    @Test void presetHeaderActionsRightAlignWithoutOverlapping(){
        int right=600;
        int addX=CarpetRulePresetLayout.addActionX(right);
        int importX=CarpetRulePresetLayout.transferActionX(right);
        int exportX=importX+66;
        assertEquals(right-12,addX+20);
        assertEquals(4,exportX-(importX+62));
        assertEquals(6,addX-(exportX+62));
    }

    @Test void removeIconAlignsWithMainRuleActionColumnAndFitsCard(){
        int panelRight=300;
        int cardRight=panelRight-18;
        int iconX=CarpetRulePresetLayout.removeX(panelRight);
        assertEquals(257,iconX);
        assertEquals(CarpetRuleListScroll.favoriteX(cardRight),iconX);
        assertTrue(iconX+CarpetRuleListScroll.STAR_WIDTH<=cardRight);
    }
    @Test void noPlansLeaveMiddleBlankWhileSidebarCanStillImport() throws Exception {
        String screen=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRulePresetScreen.java"))
                .replace("\r\n","\n");
        assertTrue(screen.contains("emptyImportButton=button("));
        assertTrue(screen.contains("this::importPresets"));
        assertTrue(screen.contains("emptyImportButton.visible=selected==null&&showSidebar()"));
        assertTrue(screen.contains("exportButton.visible=importButton.visible=selected!=null"));
        assertTrue(screen.contains(
                "if(showMiddle()&&(selected!=null||importing||naming||pickingRule)){"));
    }

}
