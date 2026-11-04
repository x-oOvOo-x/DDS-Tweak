package ddstweak.integration.carpet.playerinventory;

import ddstweak.integration.dds.playerinventory.PlayerInventoryAction;
import ddstweak.integration.dds.playerinventory.PlayerInventoryBehaviorV1;
import ddstweak.platform.minecraft.MinecraftCompat;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Provider-neutral fake-player controller backed by the standard Carpet /player command tree. */
public final class CarpetPlayerCommandBridge {
    private static final Set<PlayerInventoryAction> SUPPORTED=EnumSet.of(
            PlayerInventoryAction.SELECT_HOTBAR,
            PlayerInventoryAction.ATTACK_ONCE,PlayerInventoryAction.ATTACK_INTERVAL,PlayerInventoryAction.ATTACK_CONTINUOUS,PlayerInventoryAction.STOP_ATTACK,
            PlayerInventoryAction.USE_ONCE,PlayerInventoryAction.USE_INTERVAL,PlayerInventoryAction.USE_CONTINUOUS,PlayerInventoryAction.STOP_USE,
            PlayerInventoryAction.STOP_ALL,PlayerInventoryAction.DROP_ALL,PlayerInventoryAction.TOGGLE_SNEAK,PlayerInventoryAction.DISCONNECT);
    private CarpetPlayerCommandBridge(){}

    /** A filtered Brigadier tree cannot be used as a permission oracle.
     * Carpet server authorizes standard /player actions when the command arrives.
     * Only nonstandard exact-stop subcommands need positive capability discovery. */
    public static boolean available(){
        Minecraft mc=Minecraft.getInstance();
        return mc!=null&&mc.getConnection()!=null;
    }
    public static boolean supports(PlayerInventoryAction action){
        if(action==null||!SUPPORTED.contains(action))return false;
        if(action==PlayerInventoryAction.STOP_ATTACK)
            return path(targetNode(),"attack","stop");
        if(action==PlayerInventoryAction.STOP_USE)
            return path(targetNode(),"use","stop");
        return true;
    }
    public static Set<PlayerInventoryAction> supportedActions(){
        EnumSet<PlayerInventoryAction> result=EnumSet.noneOf(PlayerInventoryAction.class);
        for(PlayerInventoryAction action:SUPPORTED)if(supports(action))result.add(action);
        return Collections.unmodifiableSet(result);
    }

    /** A /player target must be a single command argument, never a decorated display name. */
    public static boolean validTargetName(String target){
        if(target==null||target.trim().isEmpty())return false;
        //#if MC >= 12111
        for(int i=0;i<target.length();i++)
            if(Character.isWhitespace(target.charAt(i))||Character.isISOControl(target.charAt(i)))return false;
        //#endif
        return true;
    }

    public static boolean send(String target,PlayerInventoryAction action,int value){
        ClientPacketListener connection=Minecraft.getInstance().getConnection();
        if(connection==null||target==null||target.trim().isEmpty()||!supports(action))return false;
        String command=commandFor(target,action,value);
        if(command==null)return false;
        return MinecraftCompat.sendServerCommandSilently(Minecraft.getInstance(),command);
    }
    /** Exact command text sent to Carpet, without the leading slash. */
    public static String commandFor(String target,PlayerInventoryAction action,int value){
        if(!validTargetName(target)||action==null)return null;
        switch(action){
            case SELECT_HOTBAR:return value>=1&&value<=9?"player "+target+" hotbar "+value:null;
            case ATTACK_ONCE:return "player "+target+" attack once";
            case ATTACK_INTERVAL:return "player "+target+" attack interval "+PlayerInventoryBehaviorV1.clampInterval(value);
            case ATTACK_CONTINUOUS:return "player "+target+" attack continuous";
            case STOP_ATTACK:return "player "+target+" attack stop";
            case USE_ONCE:return "player "+target+" use once";
            case USE_INTERVAL:return "player "+target+" use interval "+PlayerInventoryBehaviorV1.clampInterval(value);
            case USE_CONTINUOUS:return "player "+target+" use continuous";
            case STOP_USE:return "player "+target+" use stop";
            case STOP_ALL:return "player "+target+" stop";
            case DROP_ALL:{String argument=dropAllArgument(targetNode());return argument==null?null:"player "+target+" dropStack "+argument;}
            case TOGGLE_SNEAK:return "player "+target+(value!=0?" sneak":" unsneak");
            case DISCONNECT:return "player "+target+" kill";
            default:return null;
        }
    }
    private static CommandNode<?> targetNode(){
        ClientPacketListener connection=Minecraft.getInstance().getConnection();if(connection==null)return null;
        CommandNode<?> root=connection.getCommands().getRoot(),command=child(root,"player");
        return command==null?null:child(command,"player");
    }
    private static CommandNode<?> child(CommandNode<?> node,String name){return node==null?null:node.getChild(name);}
    private static String dropAllArgument(CommandNode<?> target){
        CommandNode<?> drop=child(target,"dropStack");
        return child(drop,"inventory")!=null?"inventory":"all";
    }
    private static boolean path(CommandNode<?> node,String first,String second){CommandNode<?> child=child(node,first);return child!=null&&child(child,second)!=null;}
}
