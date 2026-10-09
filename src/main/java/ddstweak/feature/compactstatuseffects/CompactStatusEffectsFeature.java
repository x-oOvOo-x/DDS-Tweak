package ddstweak.feature.compactstatuseffects;

//#if MC >= 12101
import ddstweak.config.Configs;
import java.util.Map;
import java.util.WeakHashMap;

public final class CompactStatusEffectsFeature {
    private static final Map<Object,CompactStatusEffectBounds> BOUNDS=new WeakHashMap<>();
    private CompactStatusEffectsFeature(){}

    public static void setBounds(Object screen,CompactStatusEffectBounds bounds){
        synchronized(BOUNDS){if(bounds==null)BOUNDS.remove(screen);else BOUNDS.put(screen,bounds);}
    }
    public static CompactStatusEffectBounds bounds(Object screen){synchronized(BOUNDS){return BOUNDS.get(screen);}}

    public static boolean enabled(){return Configs.Visual.COMPACT_STATUS_EFFECTS.getBooleanValue();}
    public static boolean requiresHoldingTab(){return Configs.Visual.COMPACT_STATUS_EFFECTS_HOLD_TAB.getBooleanValue();}

    public static boolean isLeftSide(){return Configs.Visual.COMPACT_STATUS_EFFECTS_ON_LEFT.getBooleanValue();}
}
//#else
//$$ public final class CompactStatusEffectsFeature {
//$$     private CompactStatusEffectsFeature(){}
//$$     public static void setBounds(Object screen,CompactStatusEffectBounds bounds){}
//$$     public static CompactStatusEffectBounds bounds(Object screen){return null;}
//$$     public static boolean enabled(){return false;}
//$$     public static boolean requiresHoldingTab(){return false;}
//$$     public static boolean isLeftSide(){return false;}
//$$ }
//#endif
