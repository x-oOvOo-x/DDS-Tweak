package ddstweak.feature.nocommandconfirm;

import ddstweak.config.Configs;

public final class NoCommandConfirmFeature {
    public static boolean enabled(){return Configs.Tweaks.NO_COMMAND_CONFIRM.getBooleanValue();}
    private NoCommandConfirmFeature(){}
}
