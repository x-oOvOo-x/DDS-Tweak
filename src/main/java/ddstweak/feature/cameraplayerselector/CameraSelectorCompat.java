package ddstweak.feature.cameraplayerselector;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.platform.minecraft.MinecraftCompat;
//#if MC >= 260300
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

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

    static boolean supports(Minecraft minecraft,String... path){
        try{
            ClientPacketListener connection=minecraft==null?null:minecraft.getConnection();
            CommandNode<?> node=connection==null?null:connection.getCommands().getRoot().getChild("c");
            for(String child:path){if(node==null)return false;node=node.getChild(child);}
            return node!=null;
        }catch(RuntimeException ignored){return false;}
    }

    static boolean supportsDestinationTeleport(Minecraft minecraft){
        return supports(minecraft,"tp","dimension","dimension","pos");
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
        if(minecraft==null)return false;
        //#if MC >= 12109
        return minecraft.hasShiftDown();
        //#else
        //$$ return net.minecraft.client.gui.screens.Screen.hasShiftDown();
        //#endif
    }

    static boolean toggleCamera(Minecraft minecraft){return send(minecraft,"c");}
    static boolean backCamera(Minecraft minecraft){return send(minecraft,"c back");}
    static String tpCommand(String name){return "c tp "+StringArgumentType.escapeIfRequired(name);}
    static boolean tpPlayer(Minecraft minecraft,String name){return send(minecraft,tpCommand(name));}
    static String followCommand(String name){return "c follow "+StringArgumentType.escapeIfRequired(name);}
    static boolean followPlayer(Minecraft minecraft,String name){return send(minecraft,followCommand(name));}
    static boolean tpDimension(Minecraft minecraft,String dimension){return send(minecraft,"c tp dimension "+dimension);}

    static String destinationCommand(CameraDestination destination){
        return "c tp dimension "+destination.dimension()+" "
                +formatNumber(destination.x())+" "
                +formatNumber(destination.y())+" "
                +formatNumber(destination.z());
    }

    static boolean tpDestination(Minecraft minecraft,CameraDestination destination){
        return destination!=null&&destination.isValid()&&send(minecraft,destinationCommand(destination));
    }

    static String coordinateText(CameraDestination destination){
        return formatNumber(destination.x())+", "+formatNumber(destination.y())+", "+formatNumber(destination.z());
    }

    static String formatNumber(double value){
        if(value==0.0D)return "0";
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    static String formatCapturedCoordinate(double value){
        return BigDecimal.valueOf(value).setScale(2,RoundingMode.DOWN).toPlainString();
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

    private static boolean send(Minecraft minecraft,String command){return MinecraftCompat.sendServerCommandSilently(minecraft,command);}
}
