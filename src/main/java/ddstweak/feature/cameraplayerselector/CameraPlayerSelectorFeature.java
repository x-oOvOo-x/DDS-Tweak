package ddstweak.feature.cameraplayerselector;

import ddstweak.config.Configs;
import ddstweak.config.Hotkeys;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;

/** Press-to-open Camera navigation screen. The server command tree remains authoritative. */
public final class CameraPlayerSelectorFeature {
    private static boolean initialized;
    private CameraPlayerSelectorFeature(){}

    public static synchronized void initialize(){
        if(initialized)return;
        Hotkeys.CAMERA_PLAYER_SELECTOR.getKeybind().setCallback((action,key)->open());
        initialized=true;
    }

    static void finishAction(CameraPlayerSelectorScreen screen){
        Minecraft minecraft=Minecraft.getInstance();
        if(MinecraftCompat.screen(minecraft)==screen)MinecraftCompat.setScreen(minecraft,null);
    }

    private static boolean open(){
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft==null
                ||!Configs.Tweaks.CAMERA_PLAYER_SELECTOR.getBooleanValue()
                ||minecraft.player==null
                ||minecraft.getConnection()==null
                ||MinecraftCompat.screen(minecraft)!=null)return false;

        CameraPlayerSelectorScreen screen=new CameraPlayerSelectorScreen(
                CameraSelectorCompat.snapshotPlayers(minecraft),
                CameraSelectorCompat.supports(minecraft,"tp","player"),
                CameraSelectorCompat.supports(minecraft,"tp","dimension"),
                CameraSelectorCompat.supportsDestinationTeleport(minecraft),
                CameraSelectorCompat.supports(minecraft,"follow"),
                CameraSelectorCompat.supports(minecraft),
                CameraSelectorCompat.supports(minecraft,"back")
        );
        MinecraftCompat.setScreen(minecraft,screen);
        return true;
    }
}
