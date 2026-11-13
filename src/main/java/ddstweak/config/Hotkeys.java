package ddstweak.config;

import com.google.common.collect.ImmutableList;
import ddstweak.platform.malilib.MalilibCompat;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import java.util.List;

/**
 * Declares shared client hotkeys and their defaults.
 * 集中定义客户端快捷键及默认按键。
 */
public final class Hotkeys {
    private static final String KEY="dds-tweak.config.hotkey";
    public static final ConfigHotkey OPEN_CONFIG_SCREEN=MalilibCompat.hotkey("openConfigScreen","LEFT_ALT,V",KEY);
    public static final ConfigHotkey OPEN_CARPET_RULE_MANAGER=MalilibCompat.hotkey("openCarpetRuleManager","F7",KEY);
    public static final ConfigHotkey CAMERA_PLAYER_SELECTOR=MalilibCompat.hotkey("cameraPlayerSelector","R",KEY);
    public static final ConfigHotkey DDS_UNDO=MalilibCompat.hotkey("ddsUndo","LEFT_CONTROL,Z",KEY);
    public static final ConfigHotkey DDS_REDO=MalilibCompat.hotkey("ddsRedo","LEFT_CONTROL,LEFT_ALT,Z",KEY);
    public static final List<ConfigHotkey> HOTKEY_LIST=ImmutableList.of(OPEN_CONFIG_SCREEN,OPEN_CARPET_RULE_MANAGER,CAMERA_PLAYER_SELECTOR,DDS_UNDO,DDS_REDO);
    private Hotkeys(){}
}
