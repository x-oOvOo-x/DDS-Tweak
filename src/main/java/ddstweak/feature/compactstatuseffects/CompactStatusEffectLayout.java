package ddstweak.feature.compactstatuseffects;

public final class CompactStatusEffectLayout {
    public static final int FRAME_SIZE=24;
    public static final int ICON_SIZE=16;
    public static final int ICON_OFFSET=(FRAME_SIZE-ICON_SIZE)/2;
    public static final int GAP=2;
    public static final int MAX_ROWS=6;
    public static final int EXPANDED_HEIGHT=32;

    private CompactStatusEffectLayout(){}

    public static int rows(int count){return count<=0?0:Math.min(count,MAX_ROWS);}
    public static int columns(int count){return count<=0?0:(count+MAX_ROWS-1)/MAX_ROWS;}
    public static int row(int index){return index%MAX_ROWS;}
    public static int column(int index){return index/MAX_ROWS;}
    public static int width(int count){int cols=columns(count);return cols==0?0:cols*FRAME_SIZE+(cols-1)*GAP;}
    public static int height(int count){int rows=rows(count);return rows==0?0:rows*FRAME_SIZE+(rows-1)*GAP;}
    public static int columnOffset(int index,int count,boolean leftSide){
        int col=column(index);
        if(leftSide)col=columns(count)-1-col;
        return col*(FRAME_SIZE+GAP);
    }

    public static int expandedExtraWidth(int expandedWidth){return Math.max(0,expandedWidth-FRAME_SIZE);}
    public static int expandedExtraHeight(){return EXPANDED_HEIGHT-FRAME_SIZE;}

    public static int hoverColumnShift(int index,int hoveredIndex,int expandedWidth,boolean leftSide){
        if(hoveredIndex<0||column(index)<=column(hoveredIndex))return 0;
        int shift=expandedExtraWidth(expandedWidth);
        return leftSide?-shift:shift;
    }

    public static int hoverRowShift(int index,int hoveredIndex){
        if(hoveredIndex<0||column(index)!=column(hoveredIndex)||row(index)<=row(hoveredIndex))return 0;
        return expandedExtraHeight();
    }

    public static int expandedRowX(int anchor,int width,boolean leftSide){
        return leftSide?anchor-width:anchor;
    }

    public static int expandedStep(int count){
        return count>5?132/Math.max(1,count-1):33;
    }

    public static int expandedHeight(int count){
        return count<=0?0:(count-1)*expandedStep(count)+EXPANDED_HEIGHT;
    }
}
