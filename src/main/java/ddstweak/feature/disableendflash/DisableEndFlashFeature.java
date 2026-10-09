package ddstweak.feature.disableendflash;

import ddstweak.config.Configs;

public final class DisableEndFlashFeature {
    public static boolean enabled(){return Configs.Visual.DISABLE_END_FLASH.getBooleanValue();}
    private DisableEndFlashFeature(){}
}
