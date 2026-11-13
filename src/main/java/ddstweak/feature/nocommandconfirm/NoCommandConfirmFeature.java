package ddstweak.feature.nocommandconfirm;

import ddstweak.config.Configs;

/**
 * Exposes the client command-confirmation suppression setting.
 * 提供客户端命令确认弹窗的禁用配置。
 */
public final class NoCommandConfirmFeature {
    public static boolean enabled(){return Configs.Tweaks.NO_COMMAND_CONFIRM.getBooleanValue();}
    private NoCommandConfirmFeature(){}
}
