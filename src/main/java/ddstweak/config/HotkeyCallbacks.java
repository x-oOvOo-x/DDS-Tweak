package ddstweak.config;

import ddstweak.gui.DDSTweakConfigScreen;
import ddstweak.feature.carpetrules.CarpetRuleManagerScreen;
import ddstweak.feature.quickmute.QuickMute;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.integration.dds.undoredo.DdsUndoRedoClientNetwork;
import ddstweak.network.ownership.DdsLocalOwnershipResolver;
import net.minecraft.client.Minecraft;

/**
 * Binds registered hotkeys to their client actions.
 * 将已注册快捷键关联到客户端操作。
 */
public final class HotkeyCallbacks {
    private HotkeyCallbacks(){}

    public static void init(){
        Hotkeys.OPEN_CONFIG_SCREEN.getKeybind().setCallback((action,key)->{
            MinecraftCompat.setScreen(Minecraft.getInstance(),new DDSTweakConfigScreen());
            return true;
        });
        Hotkeys.OPEN_CARPET_RULE_MANAGER.getKeybind().setCallback((action,key)->{
            Minecraft mc=Minecraft.getInstance();
            if(mc==null||mc.player==null||mc.getConnection()==null
                    ||MinecraftCompat.screen(mc)!=null)return false;
            MinecraftCompat.setScreen(mc,new CarpetRuleManagerScreen(null));
            return true;
        });
        Hotkeys.DDS_UNDO.getKeybind().setCallback((action,key)->sendUndoRedo(true));
        Hotkeys.DDS_REDO.getKeybind().setCallback((action,key)->sendUndoRedo(false));
        QuickMute.initialize();
    }

    private static boolean sendUndoRedo(boolean undo){
        // Identical user-configured shortcuts cannot issue two opposing requests.
        if(Hotkeys.DDS_UNDO.getStringValue().equals(Hotkeys.DDS_REDO.getStringValue()))return false;
        Minecraft mc=Minecraft.getInstance();
        return DdsLocalOwnershipResolver.current().tweakOwnsFeatureClient()
                && mc!=null && mc.player!=null && mc.player.isCreative()
                && mc.getConnection()!=null && MinecraftCompat.screen(mc)==null
                && (undo?DdsUndoRedoClientNetwork.sendUndo():DdsUndoRedoClientNetwork.sendRedo());
    }
}
