package ddstweak.feature.stackcountscale;

import ddstweak.config.Configs;

public final class StackCountScaleFeature {
    public static boolean enabled(){return Configs.Visual.STACK_COUNT_SCALE_ENABLED.getBooleanValue();}
    public static float scale(){return Configs.Visual.STACK_COUNT_SCALE.getIntegerValue()/100.0F;}
    private StackCountScaleFeature(){}
}
