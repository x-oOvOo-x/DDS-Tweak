package ddstweak.feature.disableendflash;

import ddstweak.config.Configs;

/**
 * Exposes the End-flash suppression setting to rendering hooks.
 * 向渲染 Hook 提供末地闪烁开关。
 */
public final class DisableEndFlashFeature {
    public static boolean enabled(){return Configs.Visual.DISABLE_END_FLASH.getBooleanValue();}
    private DisableEndFlashFeature(){}
}
