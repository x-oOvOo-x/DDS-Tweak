package ddstweak.feature.locatorheads;

//#if MC >= 12108
/** One original waypoint sprite call has exactly one owner: vanilla, DT, or neither. */
public final class LocatorSpriteDecision {
    public enum Outcome { ORIGINAL, HEAD, HIDDEN }

    private LocatorSpriteDecision(){}

    /**
     * @param drewHead whether the DT face was actually submitted (never merely attempted)
     */
    public static Outcome decide(boolean enabled,boolean filtered,boolean drewHead){
        if(!enabled)return Outcome.ORIGINAL;
        if(filtered)return Outcome.HIDDEN;
        return drewHead?Outcome.HEAD:Outcome.ORIGINAL;
    }
}
//#else
//$$ public final class LocatorSpriteDecision { private LocatorSpriteDecision(){} }
//#endif
