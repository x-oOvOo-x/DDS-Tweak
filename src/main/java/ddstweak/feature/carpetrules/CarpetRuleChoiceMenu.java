package ddstweak.feature.carpetrules;

/** Shared geometry for variable-height in-place value dropdowns. */
final class CarpetRuleChoiceMenu {
    static final int MAX_ROWS=12, ROW_HEIGHT=19, PADDING=4;

    private CarpetRuleChoiceMenu(){}

    static int rows(int count,int available){
        return Math.max(0,Math.min(count,Math.min(MAX_ROWS,available)));
    }
    static int height(int count,int visible){return rows(count,visible)*ROW_HEIGHT+2*PADDING;}
    static int maxOffset(int count,int visible){return Math.max(0,count-rows(count,visible));}
    static int clampOffset(int offset,int count,int visible){
        return Math.max(0,Math.min(maxOffset(count,visible),offset));
    }
    static int hit(int y,int top,int offset,int count,int visible){
        int shown=rows(count,visible);
        if(y<top+PADDING||y>=top+PADDING+shown*ROW_HEIGHT)return -1;
        int row=(y-top-PADDING)/ROW_HEIGHT;
        int index=clampOffset(offset,count,visible)+row;
        return index<count?index:-1;
    }
}
