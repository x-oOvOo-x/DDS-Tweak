package ddstweak.feature.gammaoverride;

import ddstweak.config.Configs;

/**
 * Runtime-only gamma override. Vanilla brightness state and options.txt are never mutated.
 * 仅覆盖运行时亮度，不写入原版设置。
 */
public final class GammaOverrideFeature {
    public static boolean enabled(){return Configs.Visual.GAMMA_OVERRIDE.getBooleanValue();}
    public static double value(){return Configs.Visual.GAMMA_OVERRIDE_VALUE.getDoubleValue();}
    public static double resolve(double vanillaGamma){return enabled()?value():vanillaGamma;}
    public static Object resolve(Object vanillaGamma){return enabled()?Double.valueOf(value()):vanillaGamma;}
    private GammaOverrideFeature(){}
}
