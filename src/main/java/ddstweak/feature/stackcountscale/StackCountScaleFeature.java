package ddstweak.feature.stackcountscale;

import ddstweak.config.Configs;

/**
 * Exposes stack-count text scaling without changing item quantities.
 * 只调整堆叠数量文字比例，不改变实际数量。
 */
public final class StackCountScaleFeature {
    public static boolean enabled(){return Configs.Visual.STACK_COUNT_SCALE_ENABLED.getBooleanValue();}
    public static float scale(){return Configs.Visual.STACK_COUNT_SCALE.getIntegerValue()/100.0F;}
    private StackCountScaleFeature(){}
}
