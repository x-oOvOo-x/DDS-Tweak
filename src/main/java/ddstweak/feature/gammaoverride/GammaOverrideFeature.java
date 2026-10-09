package ddstweak.feature.gammaoverride;

import ddstweak.config.Configs;

/** Runtime-only gamma override. Vanilla brightness state and options.txt are never mutated. */
public final class GammaOverrideFeature {
    public static boolean enabled(){return Configs.Visual.GAMMA_OVERRIDE.getBooleanValue();}
    public static double value(){return Configs.Visual.GAMMA_OVERRIDE_VALUE.getDoubleValue();}
    public static double resolve(double vanillaGamma){return enabled()?value():vanillaGamma;}
    private GammaOverrideFeature(){}
}
