package ddstweak.config;

import com.google.common.collect.ImmutableList;
import ddstweak.platform.malilib.MalilibCompat;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import java.util.List;

public final class Hotkeys {
    private static final String KEY="dds-tweak.config.hotkey";
    public static final ConfigHotkey OPEN_CONFIG_SCREEN=MalilibCompat.hotkey("openConfigScreen","LEFT_ALT,V",KEY);
    public static final ConfigHotkey CAMERA_PLAYER_SELECTOR=MalilibCompat.hotkey("cameraPlayerSelector","R",KEY);
    public static final List<ConfigHotkey> HOTKEY_LIST=ImmutableList.of(OPEN_CONFIG_SCREEN,CAMERA_PLAYER_SELECTOR);
    private Hotkeys(){}
}
