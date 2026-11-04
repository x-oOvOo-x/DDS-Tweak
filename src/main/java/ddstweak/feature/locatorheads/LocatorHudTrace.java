package ddstweak.feature.locatorheads;

//#if MC >= 12108
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Invocation-level diagnostics; never interprets method calls as GPU draw counts. */
public final class LocatorHudTrace {
    private static final boolean ENABLED=Boolean.getBoolean("ddstweak.locatorHeads.hudTrace");
    private static final Logger LOGGER=LoggerFactory.getLogger(LocatorHudTrace.class);
    private static final Counters COUNTERS=new Counters();
    private static long ticks;
    enum Kind { XP_BG,LOC_BG,LOC_FG,XP_RETURN,OVERLAY,PRIORITY,FORCED,CAPTURE,ORIGINAL,HEAD,HIDDEN,ARROW,ARROW_HIDDEN }
    static final class Counters {
        private final long[] counts=new long[Kind.values().length];
        void add(Kind kind){counts[kind.ordinal()]++;}
        long[] drain(){long[] result=counts.clone();Arrays.fill(counts,0);return result;}
    }
    private LocatorHudTrace(){}
    private static void count(Kind kind){if(ENABLED)COUNTERS.add(kind);}
    public static void background(boolean xp){count(xp?Kind.XP_BG:Kind.LOC_BG);}
    public static void foreground(){count(Kind.LOC_FG);}
    public static void capture(){count(Kind.CAPTURE);}
    public static void outcome(LocatorSpriteDecision.Outcome result){
        if(!ENABLED)return;
        count(switch(result){case ORIGINAL->Kind.ORIGINAL;case HEAD->Kind.HEAD;case HIDDEN->Kind.HIDDEN;});
    }
    public static void arrow(boolean hidden){count(hidden?Kind.ARROW_HIDDEN:Kind.ARROW);}
    public static void xpReturn(boolean overlay){
        if(!ENABLED)return;
        count(Kind.XP_RETURN);
        if(overlay)count(Kind.OVERLAY);
    }
    public static void priority(boolean forced){
        if(!ENABLED)return;
        count(Kind.PRIORITY);
        if(forced)count(Kind.FORCED);
    }
    /** One aggregate per 200 END_CLIENT_TICK events while connected, disabled by default. */
    public static void tick(boolean connected){
        if(!ENABLED||++ticks%200!=0)return;
        long[] n=COUNTERS.drain();
        if(connected)LOGGER.info("LocatorHeads hud: xpBg={}, locBg={}, locFg={}, xpReturn={}, overlayRequested={}, priorityReturn={}, forcedXp={}, waypointCapture={}, original={}, head={}, hidden={}, arrowDrawn={}, arrowHidden={}",
                n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9],n[10],n[11],n[12]);
    }
}
//#else
//$$ public final class LocatorHudTrace { private LocatorHudTrace(){} }
//#endif
