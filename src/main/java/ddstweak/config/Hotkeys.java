package ddstweak.config;

import com.google.common.collect.ImmutableList;
import ddstweak.platform.malilib.MalilibCompat;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import java.util.List;

public final class Hotkeys {
    private static final String KEY="dds-tweak.config.hotkey";
    public static final ConfigHotkey OPEN_CONFIG_SCREEN=MalilibCompat.hotkey("openConfigScreen","LEFT_ALT,V",KEY);
    public static final ConfigHotkey CAMERA_PLAYER_SELECTOR=MalilibCompat.hotkey("cameraPlayerSelector","R",KEY);
    public static final ConfigHotkey DDS_UNDO=MalilibCompat.hotkey("ddsUndo","LEFT_CONTROL,Z",KEY);
    public static final ConfigHotkey DDS_REDO=MalilibCompat.hotkey("ddsRedo","LEFT_CONTROL,LEFT_ALT,Z",KEY);
    public static final List<ConfigHotkey> HOTKEY_LIST=ImmutableList.of(OPEN_CONFIG_SCREEN,CAMERA_PLAYER_SELECTOR,DDS_UNDO,DDS_REDO);
    private Hotkeys(){}
}
