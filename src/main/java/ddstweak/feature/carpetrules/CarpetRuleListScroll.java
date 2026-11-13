package ddstweak.feature.carpetrules;

/** Stable coordinate model shared by painting, hover, and mouse hit-testing. */
public final class CarpetRuleListScroll {
    public static final int CARD_HEIGHT=36;
    public static final int CARD_GAP=6;
    public static final int STRIDE=CARD_HEIGHT+CARD_GAP;
    public static final int STAR_WIDTH=20,SWITCH_WIDTH=33,SWITCH_HEIGHT=14,CHOICE_WIDTH=68;
    public static int valueShift(boolean vanillaGameRules){return vanillaGameRules?24:0;}
    public static int favoriteX(int cardRight){return cardRight-25;}
    public static int saveX(int cardRight){return cardRight-49;}
    public static int switchX(int cardRight){return cardRight-93;}
    public static int choiceX(int cardRight){return switchX(cardRight)+SWITCH_WIDTH-CHOICE_WIDTH;}

    private double position,target;

    public void reset(){position=0;target=0;}
    public double position(){return position;}
    public int top(int viewportTop,int index){return viewportTop+(int)Math.round(index*STRIDE-position);}

    public void wheel(double amount,int count,int viewportHeight){
        target=clamp(target-amount*STRIDE*1.6,0,max(count,viewportHeight));
    }

    public void scrollToFraction(double fraction,int count,int viewportHeight){
        target=clamp(fraction,0,1)*max(count,viewportHeight);
        position=target;
    }

    public static int thumbHeight(int count,int viewportHeight,int trackHeight){
        int limit=max(count,viewportHeight);
        if(limit==0)return trackHeight;
        return Math.max(18,(int)Math.round(
                (double)trackHeight*viewportHeight/(viewportHeight+limit)));
    }

    public int thumbTop(int trackTop,int count,int viewportHeight,int trackHeight){
        int travel=Math.max(0,trackHeight-thumbHeight(count,viewportHeight,trackHeight));
        int limit=max(count,viewportHeight);
        return trackTop+(limit==0?0:(int)Math.round(position*travel/limit));
    }

    public void animate(int count,int viewportHeight){
        double limit=max(count,viewportHeight);
        target=clamp(target,0,limit);
        position=clamp(position,0,limit);
        position+=(target-position)*0.28;
        if(Math.abs(target-position)<0.25)position=target;
    }

    public int hit(double mouseY,int viewportTop,int count,int viewportHeight){
        if(mouseY<viewportTop||mouseY>=viewportTop+viewportHeight)return -1;
        double local=mouseY-viewportTop+position;
        int index=(int)Math.floor(local/STRIDE);
        if(index<0||index>=count)return -1;
        double inside=local-index*STRIDE;
        return inside>=0&&inside<CARD_HEIGHT?index:-1;
    }

    public static int max(int count,int viewportHeight){
        return Math.max(0,count*STRIDE-CARD_GAP-viewportHeight);
    }

    private static double clamp(double v,double min,double max){
        return Math.max(min,Math.min(max,v));
    }
}
