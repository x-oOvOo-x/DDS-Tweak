package ddstweak.integration.gca.playerinventory;

import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.integration.dds.playerinventory.*;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
//#if MC >= 12111
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import java.util.*;

/**
 * Dependency-free GCA recognition.
 * Prefer the translation key; fall back to the rendered title for GCA versions that send a resolved/literal title.
 */
public final class GcaPlayerInventoryAdapter{
    public static final String SOURCE="gca",TITLE_INVENTORY="gca.player.inventory",TITLE_CONTROLLER="gca.player.other_controller",TITLE_ENDER="gca.player.ender_chest",BEHAVIOR_ID="gca_slot_control_v1";
    private static final boolean TRACE=Boolean.getBoolean("ddstweak.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger TRACE_LOG=
            org.apache.logging.log4j.LogManager.getLogger("ddstweak.playerinventory");
    private GcaPlayerInventoryAdapter(){}

    /** Side-effect-free recognition for binding the pending clicked player to an opened menu. */
    public static boolean isGcaMenu(MenuType<?> type,Component title){
        return title!=null&&resolve(type,title)!=null;
    }

    public static PlayerInventoryContext tryContext(MenuType<?> type,int containerId,Component title){
        if(title==null)return null;
        Resolved resolved=resolve(type,title);
        if(resolved==null)return null;
        return context(containerId,resolved.target,resolved.kind,resolved.layout,title);
    }

    public static PlayerInventoryContext authoritativeContext(MenuType<?> type,PlayerInventoryContext wire){
        if(wire==null||!SOURCE.equals(wire.source()))return null;
        PlayerInventoryKind kind=wire.inventoryKind();
        String layout=wire.layoutId();
        if(kind==PlayerInventoryKind.INVENTORY&&!PlayerInventoryLayouts.GCA_INVENTORY_V1.equals(layout)
                ||kind==PlayerInventoryKind.CONTROLLER&&!PlayerInventoryLayouts.GCA_CONTROLLER_3_V1.equals(layout)
                ||kind==PlayerInventoryKind.ENDER_CHEST&&!PlayerInventoryLayouts.GCA_ENDER_3_V1.equals(layout))return null;
        //#if MC >= 12111
        Set<PlayerInventoryAction> actions=wire.targetState()!=PlayerInventoryTargetState.OFFLINE
                ?actions(kind,wire.targetType()):Collections.<PlayerInventoryAction>emptySet();
        //#else
        //$$ Set<PlayerInventoryAction> actions=wire.targetState()==PlayerInventoryTargetState.ONLINE
        //$$         ?actions(kind,wire.targetType()):Collections.<PlayerInventoryAction>emptySet();
        //#endif
        return new PlayerInventoryContext(wire.containerId(),wire.contextSchema(),SOURCE,wire.targetUuid(),wire.targetName(),
                wire.targetType(),wire.targetState(),kind,layout,wire.layoutSchema(),wire.behaviorId(),actions,
                Collections.singleton(PlayerInventoryInteractionCapability.CONTROL_SLOT_BINDING),wire.fallbackPolicy());
    }

    private static Resolved resolve(MenuType<?> type,Component title){
        String key=MinecraftCompat.translationKey(title);
        if(key!=null){
            String target=targetName(MinecraftCompat.translationArgs(title));
            if(TITLE_INVENTORY.equals(key)&&type==MenuType.GENERIC_9x6)return new Resolved(target,PlayerInventoryKind.INVENTORY,PlayerInventoryLayouts.GCA_INVENTORY_V1);
            if(TITLE_CONTROLLER.equals(key)&&type==MenuType.GENERIC_9x3)return new Resolved(target,PlayerInventoryKind.CONTROLLER,PlayerInventoryLayouts.GCA_CONTROLLER_3_V1);
            if(TITLE_ENDER.equals(key)&&type==MenuType.GENERIC_9x6)return new Resolved(target,PlayerInventoryKind.ENDER_CHEST,PlayerInventoryLayouts.GCA_ENDER_3_V1);
        }

        String visible=title.getString(),target=findTargetName(visible);
        if(target.trim().isEmpty())return null;
        if(type==MenuType.GENERIC_9x6&&isRenderedGcaEnder(visible,target))
            return new Resolved(target,PlayerInventoryKind.ENDER_CHEST,PlayerInventoryLayouts.GCA_ENDER_3_V1);
        if(type==MenuType.GENERIC_9x3&&isRenderedGcaController(visible,target))
            return new Resolved(target,PlayerInventoryKind.CONTROLLER,PlayerInventoryLayouts.GCA_CONTROLLER_3_V1);
        if(type==MenuType.GENERIC_9x6&&isRenderedGcaInventory(visible,target))
            return new Resolved(target,PlayerInventoryKind.INVENTORY,PlayerInventoryLayouts.GCA_INVENTORY_V1);
        return null;
    }

    private static PlayerInventoryContext context(int containerId,String target,PlayerInventoryKind kind,String layout,Component title){
        Identity identity;
        //#if MC >= 12111
        GcaPlayerInteractionIntent.Target clicked=GcaPlayerInteractionIntent.consume(containerId,title,kind);
        Identity fromTitle=identity(target,title);
        // If two independently known accounts disagree, this menu cannot safely
        // be associated with the clicked player. Do not send commands to either.
        if(clicked!=null&&!fromTitle.name.isEmpty()&&!fromTitle.name.equals(clicked.name)){
            if(TRACE)TRACE_LOG.warn("gca_target_conflict id={} clicked={} title={}",
                    containerId,clicked.name,fromTitle.name);
            identity=new Identity(null,"",PlayerInventoryTargetType.UNKNOWN);
        }else{
            identity=clicked==null?fromTitle
                    :new Identity(clicked.uuid,clicked.name,PlayerIdentityResolver.resolve(clicked.uuid));
            if(TRACE&&clicked!=null)
                TRACE_LOG.info("gca_target_source=interaction id={} kind={} target={}",
                        containerId,kind,clicked.name);
        }
        //#else
        //$$ identity=identity(target,title);
        //#endif
        if(TRACE&&identity.name.isEmpty())
            TRACE_LOG.info("gca_target_unresolved id={} kind={} title_key={} title={} args={}",
                    containerId,kind,MinecraftCompat.translationKey(title),title,
                    Arrays.toString(MinecraftCompat.translationArgs(title)));
        Set<PlayerInventoryAction> actions=actions(kind,identity.type);
        return new PlayerInventoryContext(containerId,1,SOURCE,identity.uuid,identity.name,identity.type,PlayerInventoryTargetState.ONLINE,
                kind,layout,1,BEHAVIOR_ID,actions,Collections.singleton(PlayerInventoryInteractionCapability.CONTROL_SLOT_BINDING),
                PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
    }

    private static Set<PlayerInventoryAction> actions(PlayerInventoryKind kind,PlayerInventoryTargetType type){
        //#if MC < 12111
        //$$ if(type==PlayerInventoryTargetType.REAL)return Collections.emptySet();
        //#endif
        EnumSet<PlayerInventoryAction> actions=EnumSet.noneOf(PlayerInventoryAction.class);
        actions.addAll(CarpetPlayerCommandBridge.supportedActions());
        //#if MC < 12111
        //$$ if(type==PlayerInventoryTargetType.FAKE){
        //$$     if(kind==PlayerInventoryKind.INVENTORY)actions.addAll(GcaPlayerInventoryActionBinding.nativeActions());
        //$$     actions.add(PlayerInventoryAction.STOP_ATTACK);
        //$$     actions.add(PlayerInventoryAction.STOP_USE);
        //$$ }else{
        //$$     actions.remove(PlayerInventoryAction.DROP_ALL);
        //$$     actions.remove(PlayerInventoryAction.DISCONNECT);
        //$$     actions.remove(PlayerInventoryAction.TOGGLE_SNEAK);
        //$$ }
        //#endif
        actions.removeIf(action->!action.allowedIn(kind));
        return Collections.unmodifiableSet(actions);
    }

    private static Identity identity(String fallback,Component title){
        Minecraft mc=Minecraft.getInstance();
        //#if MC >= 12111
        // A displayed player name, even with a known fake-player prefix, is not
        // proof of the account to send a /player command to.
        Set<String> online=new LinkedHashSet<>();
        Map<String,UUID> uuids=new LinkedHashMap<>();
        ClientPacketListener connection=mc.getConnection();
        if(connection!=null)for(PlayerInfo info:connection.getOnlinePlayers()){
            if(info==null||info.getProfile()==null)continue;
            String name=MinecraftCompat.gameProfileName(info.getProfile());
            if(name==null)continue;
            online.add(name);
            uuids.put(name,MinecraftCompat.gameProfileId(info.getProfile()));
        }
        Map<String,String> loadedDisplays=new LinkedHashMap<>();
        if(mc.level!=null)for(Player player:mc.level.players()){
            String name=player.getName().getString();
            loadedDisplays.put(name,player.getDisplayName().getString());
            uuids.putIfAbsent(name,player.getUUID());
        }
        String name=verifiedAccountName(fallback,online,loadedDisplays);
        if(name.isEmpty())return new Identity(null,"",PlayerInventoryTargetType.UNKNOWN);
        UUID uuid=uuids.get(name);
        return new Identity(uuid,name,PlayerIdentityResolver.resolve(uuid));
        //#else
        //$$ if(mc.level!=null)for(Player player:mc.level.players()){
        //$$     String name=player.getName().getString(),display=player.getDisplayName().getString();
        //$$     if(matches(fallback,name,display)){
        //$$         PlayerInventoryTargetType type=PlayerIdentityResolver.resolve(player.getUUID());
        //$$         return new Identity(player.getUUID(),name,type);
        //$$     }
        //$$ }
        //$$ return new Identity(null,fallback,PlayerInventoryTargetType.UNKNOWN);
        //#endif
    }

    //#if MC >= 12111
    /**
     * A displayed title can identify a target only when it agrees exactly with
     * one known account or one locally loaded player's full display name.
     * Conflicting matches are rejected, including Bot_Alex (an account) versus
     * Alex (a different player's display name). Never strip fake-name prefixes.
     */
    static String verifiedAccountName(String label,Collection<String> online,Map<String,String> displays){
        if(label==null||label.isEmpty())return "";
        Set<String> matches=new LinkedHashSet<>();
        if(isPlainPlayerName(label)&&online!=null&&online.contains(label))matches.add(label);
        if(displays!=null)for(Map.Entry<String,String> entry:displays.entrySet()){
            if(!isPlainPlayerName(entry.getKey()))continue;
            if(label.equals(entry.getKey())||label.equals(entry.getValue()))matches.add(entry.getKey());
        }
        return matches.size()==1?matches.iterator().next():"";
    }

    static boolean isPlainPlayerName(String name){
        if(name==null||name.isEmpty()||name.length()>64)return false;
        for(int i=0;i<name.length();){
            int cp=name.codePointAt(i);
            if(cp!='_'&&!Character.isLetterOrDigit(cp))return false;
            i+=Character.charCount(cp);
        }
        return true;
    }
    //#endif
    private static String findTargetName(String title){
        //#if MC >= 12111
        if(title==null)return "";
        Minecraft mc=Minecraft.getInstance();
        Set<String> candidates=new LinkedHashSet<>();
        if(mc.level!=null)for(Player player:mc.level.players())
            candidates.add(player.getName().getString());
        ClientPacketListener connection=mc.getConnection();
        if(connection!=null)for(PlayerInfo info:connection.getOnlinePlayers()){
            if(info==null||info.getProfile()==null)continue;
            String name=MinecraftCompat.gameProfileName(info.getProfile());
            if(name!=null)candidates.add(name);
        }
        return matchingRenderedTarget(title,candidates);
        //#else
        //$$ Minecraft mc=Minecraft.getInstance();if(mc.level==null||title==null)return "";
        //$$ Player best=null;int length=-1;
        //$$ for(Player player:mc.level.players()){
        //$$     String name=player.getName().getString(),display=player.getDisplayName().getString();
        //$$     if(title.contains(name)&&name.length()>length){best=player;length=name.length();}
        //$$     if(title.contains(display)&&display.length()>length){best=player;length=display.length();}
        //$$ }
        //$$ return best==null?"":best.getName().getString();
        //#endif
    }
    //#if MC >= 12111
    /** Accept only complete GCA titles, never a substring or a generic container title. */
    static String matchingRenderedTarget(String title,Iterable<String> candidates){
        String matched="";
        for(String name:candidates){
            if(name==null||name.trim().isEmpty())continue;
            if(!isRenderedGcaInventory(title,name)&&!isRenderedGcaEnder(title,name)
                    &&!isRenderedGcaController(title,name))continue;
            if(!matched.isEmpty()&&!matched.equals(name))return "";
            matched=name;
        }
        return matched;
    }
    //#endif
    private static boolean isRenderedGcaEnder(String visible,String target){return renderedTitleMatches(TITLE_ENDER,visible,target)||visible.equals(target+"'s Ender Chest")||visible.equals(target+"的末影箱");}
    private static boolean isRenderedGcaController(String visible,String target){return renderedTitleMatches(TITLE_CONTROLLER,visible,target)||visible.equals(target+"'s Other Controller")||visible.equals(target+"的额外控制面板");}
    private static boolean isRenderedGcaInventory(String visible,String target){return renderedTitleMatches(TITLE_INVENTORY,visible,target)||visible.equals(target+"'s Inventory")||visible.equals(target+"的背包");}
    private static boolean renderedTitleMatches(String key,String visible,String target){return visible.equals(MinecraftCompat.translatable(key,MinecraftCompat.literal(target)).getString());}
    private static boolean matches(String label,String name,String display){return label.equals(name)||label.equals(display)||!name.trim().isEmpty()&&label.endsWith(name);}
    private static String targetName(Object[] args){if(args.length==0||args[0]==null)return "";Object arg=args[0];return arg instanceof Component?((Component)arg).getString():String.valueOf(arg);}
    private static final class Resolved{
        private final String target,layout;private final PlayerInventoryKind kind;
        private Resolved(String target,PlayerInventoryKind kind,String layout){this.target=target;this.kind=kind;this.layout=layout;}
    }
    private static final class Identity{
        private final UUID uuid;private final String name;private final PlayerInventoryTargetType type;
        private Identity(UUID uuid,String name,PlayerInventoryTargetType type){this.uuid=uuid;this.name=name;this.type=type;}
    }
}
