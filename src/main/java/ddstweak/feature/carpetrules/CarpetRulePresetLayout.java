package ddstweak.feature.carpetrules;

/** Geometry shared by preset-card painting, mouse hit-testing and the edit popup. */
final class CarpetRulePresetLayout {
    static final int ROW=CarpetRuleListScroll.STRIDE,CARD_HEIGHT=CarpetRuleListScroll.CARD_HEIGHT;
    static final int CREATE_HEIGHT=22; // Main Carpet sidebar parent tab height.
    static final int EXPANDED_HEIGHT=88,EXPANSION=EXPANDED_HEIGHT-CARD_HEIGHT;
    static final int GAP=ROW-CARD_HEIGHT;

    private CarpetRulePresetLayout(){}

    static int createHeight(boolean creating){
        return creating?EXPANDED_HEIGHT:CREATE_HEIGHT;
    }

    /** Inline editing increases the row height without covering subsequent cards. */
    static int rowY(int index,int offset,int listTop,int expandedIndex){
        return listTop+(index-offset)*ROW+
                (expandedIndex>=offset&&index>expandedIndex?EXPANSION:0);
    }

    static int rowAt(double y,int listTop,int listBottom,int offset){
        return rowAt(y,listTop,listBottom,offset,-1);
    }

    static int rowAt(double y,int listTop,int listBottom,int offset,int expandedIndex){
        if(y<listTop||y>=listBottom)return -1;
        int relative=(int)(y-listTop),expandedRow=expandedIndex-offset;
        if(expandedRow>=0&&relative>=expandedRow*ROW){
            if(relative<expandedRow*ROW+EXPANDED_HEIGHT)return expandedIndex;
            relative-=EXPANSION;
        }
        return relative%ROW>=CARD_HEIGHT?-1:offset+relative/ROW;
    }

    /** Right-aligned import/export/add actions use one anchor at the panel edge. */
    static int addActionX(int panelRight){return panelRight-32;}
    static int transferActionX(int panelRight){return addActionX(panelRight)-6-62-4-62;}

    /** Align the remove action with the main rule list's right-side icon column. */
    static int removeX(int middleRight){
        return CarpetRuleListScroll.favoriteX(middleRight-18);
    }
}
