package ddstweak.feature.cameraplayerselector;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.platform.minecraft.MinecraftCompat;
//#if MC >= 260300
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.math.BigDecimal;
import java.util.*;

/**
 * Adapts player snapshots and command availability across versions.
 * 适配不同版本的玩家列表与指令可用性。
 */
final class CameraSelectorCompat {
    private CameraSelectorCompat(){}

    static List<CameraPlayerEntry> snapshotPlayers(Minecraft minecraft){
        ClientPacketListener connection=minecraft==null?null:minecraft.getConnection();
        if(connection==null)return Collections.emptyList();
        UUID self=MinecraftCompat.userProfileId(minecraft);
        List<CameraPlayerEntry> result=new ArrayList<>();
        for(PlayerInfo info:connection.getOnlinePlayers()){
            if(info==null)continue;
            GameProfile profile=info.getProfile();
            UUID uuid=MinecraftCompat.gameProfileId(profile);
            String name=MinecraftCompat.gameProfileName(profile);
            if(uuid==null||uuid.equals(self)||name==null||name.trim().isEmpty())continue;
            result.add(new CameraPlayerEntry(uuid,name,PlayerIdentityResolver.resolve(uuid)));
        }
        result.sort(Comparator.comparing(entry->entry.name.toLowerCase(Locale.ROOT)));
        return Collections.unmodifiableList(result);
    }

    /** A configured template can be submitted; only the server decides permission. */
    static boolean configured(CameraCommandProfile profile,CameraCommandProfile.Action action){
        return profile!=null&&CameraCommandProfile.validate(action,profile.get(action))==null
                &&profile.get(action)!=null&&!profile.get(action).isEmpty();
    }

    /** Command arguments use names, so reject stale name/UUID snapshots before dispatch. */
    static boolean matchesSnapshot(Minecraft minecraft,CameraPlayerEntry entry){
        if(entry==null)return false;
        PlayerInfo current=info(minecraft,entry.uuid);
        return current!=null&&entry.name.equals(MinecraftCompat.gameProfileName(current.getProfile()));
    }

    static boolean online(Minecraft minecraft,UUID uuid){return info(minecraft,uuid)!=null;}
    static PlayerInfo info(Minecraft minecraft,UUID uuid){
        ClientPacketListener connection=minecraft==null?null:minecraft.getConnection();
        return connection==null||uuid==null?null:connection.getPlayerInfo(uuid);
    }

    static boolean leftMouse(int button){
        //#if MC >= 260300
        //$$ return button==InputConstants.MOUSE_BUTTON_LEFT;
        //#else
        return button==0;
        //#endif
    }

    static boolean rightMouse(int button){
        //#if MC >= 260300
        //$$ return button==InputConstants.MOUSE_BUTTON_RIGHT;
        //#else
        return button==1;
        //#endif
    }

    static boolean shiftDown(Minecraft minecraft){
        return MinecraftCompat.shiftDown(minecraft);
    }

    static boolean sendAction(Minecraft minecraft,CameraCommandProfile profile,CameraCommandProfile.Action action){
        if(!configured(profile,action))return false;
        return CameraCommandSyntax.send(minecraft,profile.render(action));
    }
    static boolean sendPlayer(Minecraft minecraft,CameraCommandProfile profile,CameraCommandProfile.Action action,String name){
        if(!configured(profile,action))return false;
        return CameraCommandSyntax.send(minecraft,profile.renderPlayer(action,name));
    }
    static boolean sendDimension(Minecraft minecraft,CameraCommandProfile profile,String dimension){
        if(!configured(profile,CameraCommandProfile.Action.DIMENSION))return false;
        return CameraCommandSyntax.send(minecraft,profile.renderDimension(CameraCommandProfile.Action.DIMENSION,dimension));
    }
    static boolean sendDestination(Minecraft minecraft,CameraCommandProfile profile,CameraDestination destination){
        if(destination==null||!destination.isValid()||!configured(profile,CameraCommandProfile.Action.DESTINATION))return false;
        return CameraCommandSyntax.send(minecraft,profile.renderDestination(destination));
    }
    static String preview(CameraCommandProfile profile,CameraCommandProfile.Action action,String player){
        String command=profile.renderPlayer(action,player);
        return command==null?"":CameraCommandSyntax.display(command);
    }

    static String coordinateText(CameraDestination destination){
        return formatNumber(destination.x())+", "+formatNumber(destination.y())+", "+formatNumber(destination.z());
    }

    static String formatNumber(double value){
        if(value==0.0D)return "0";
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    static String formatCapturedCoordinate(double value){
        return Long.toString((long)Math.floor(value));
    }

    static double playerX(Minecraft minecraft){
        if(minecraft==null||minecraft.player==null)return 0.0D;
        //#if MC >= 11500
        return minecraft.player.getX();
        //#else
        //$$ return minecraft.player.x;
        //#endif
    }

    static double playerY(Minecraft minecraft){
        if(minecraft==null||minecraft.player==null)return 0.0D;
        //#if MC >= 11500
        return minecraft.player.getY();
        //#else
        //$$ return minecraft.player.y;
        //#endif
    }

    static double playerZ(Minecraft minecraft){
        if(minecraft==null||minecraft.player==null)return 0.0D;
        //#if MC >= 11500
        return minecraft.player.getZ();
        //#else
        //$$ return minecraft.player.z;
        //#endif
    }

    static String currentDimension(Minecraft minecraft){
        if(minecraft==null||minecraft.level==null)return "";
        //#if MC >= 11600
        //#if MC <= 12110
        //$$ return minecraft.level.dimension().location().toString();
        //#else
        return minecraft.level.dimension().identifier().toString();
        //#endif
        //#else
        //$$ return "";
        //#endif
    }

}
