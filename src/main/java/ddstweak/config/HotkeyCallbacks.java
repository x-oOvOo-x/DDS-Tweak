package ddstweak.config;

import ddstweak.gui.DDSTweakConfigScreen;
import ddstweak.feature.quickmute.QuickMute;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;

public final class HotkeyCallbacks {
    private HotkeyCallbacks(){}
    public static void init(){Hotkeys.OPEN_CONFIG_SCREEN.getKeybind().setCallback((action,key)->{MinecraftCompat.setScreen(Minecraft.getInstance(),new DDSTweakConfigScreen());return true;});QuickMute.initialize();}
}
