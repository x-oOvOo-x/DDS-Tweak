package ddstweak.feature.windowplayerhead;

import ddstweak.config.Configs;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import java.util.UUID;

public final class WindowPlayerHeadFeature {
    private static boolean initialized;
    public static void initialize(){if(initialized)return;initialized=true;ClientLifecycleEvents.CLIENT_STARTED.register(client->{
        if(!Configs.Visual.PLAYER_HEAD_WINDOW_ICON.getBooleanValue())return;UUID id=MinecraftCompat.userProfileId(client);if(id==null)return;Path gameDir=FabricLoader.getInstance().getGameDir();
        PlayerSkinDownloader.resolve(gameDir,id).thenApply(path->path==null?null:PlayerHeadRenderer.renderIcons(path)).thenAccept(icons->{if(icons!=null)client.execute(()->{if(Configs.Visual.PLAYER_HEAD_WINDOW_ICON.getBooleanValue())WindowIcon.set(client,icons);});});
    });}
    private WindowPlayerHeadFeature(){}
}
