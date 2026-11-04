package ddstweak.integration.org.playerinventory;

import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.integration.gca.playerinventory.GcaPlayerInteractionIntent;
import ddstweak.integration.dds.playerinventory.*;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Org client adapter. Command correlation covers versions such as 1.21.11 that expose no client marker;
 * provider markers remain preferred when newer Org versions expose richer control layouts.
 */
public final class OrgPlayerInventoryAdapter{
    public static final String SOURCE="org",BEHAVIOR_MENU="provider_menu_v1",BEHAVIOR_CONTROLS="provider_controls_v1";
    private static final boolean TRACE=Boolean.getBoolean("ddstweak.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger TRACE_LOG=
            org.apache.logging.log4j.LogManager.getLogger("ddstweak.playerinventory");
    private static final String OLD_MARKER="carpet_Org_Addition$isOldPlayerInventoryScreen";
    private static final String BUTTON_MARKER="carpet_Org_Addition$isWithButton";
    private static final String[] CLIENT_UTILS={"boat.carpetorgaddition.client.util.ClientUtils","org.carpetorgaddition.client.util.ClientUtils"};
    private OrgPlayerInventoryAdapter(){}

    /**
     * Bind the next exact Org menu. A trusted QCA return carries its full parent context and can
     * therefore be rendered synchronously; ordinary command correlation still waits for menu markers.
     */
    public static PlayerInventoryContext observeMenu(MenuType<?> type,int containerId,Component title){
        OrgPlayerInventoryOpenIntent.Session session=OrgPlayerInventoryOpenIntent.bind(type,containerId,title);
        if(OrgPlayerInventoryOpenIntent.isOfflineMenu(type,containerId,title))return null;
        //#if MC >= 12111
        // Org's openPlayerInventory rule opens a 3-row Ender by sneaking and
        // right-clicking a player. No /player command intent or client marker
        // exists for this path. Correlate the same clicked GameProfile, menu ID,
        // connection and styled player-title marker; never infer from plain text.
        if(session==null&&type==MenuType.GENERIC_9x3){
            GcaPlayerInteractionIntent.Target clicked=GcaPlayerInteractionIntent.consumeOrgEnder(containerId);
            if(clicked!=null&&isClickedEnderTitle(type,title,clicked.name())){
                UUID uuid=clicked.uuid();
                if(TRACE)TRACE_LOG.info("org_ender_target_source=interaction id={} target={} uuid={}",
                        containerId,clicked.name(),uuid);
                return context(containerId,new Identity(uuid,clicked.name(),fake(uuid),
                        PlayerInventoryTargetState.ONLINE),PlayerInventoryKind.ENDER_CHEST,
                        PlayerInventoryLayouts.ORG_ENDER_3_V1,false);
            }
        }
        //#endif
        PlayerInventoryContext trusted=trustedReturnContext(containerId,session);
        if(trusted!=null)return trusted;
        if(session!=null&&session.kind()==OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST)
            return context(containerId,identity(title,session),PlayerInventoryKind.ENDER_CHEST,
                    PlayerInventoryLayouts.ORG_ENDER_3_V1,false);
        return null;
    }

    public static PlayerInventoryContext authoritativeContext(MenuType<?> type,PlayerInventoryContext wire){
        if(wire==null||!SOURCE.equals(wire.source())
                ||wire.targetState()==PlayerInventoryTargetState.OFFLINE)return null;
        boolean controls=PlayerInventoryLayouts.ORG_CONTROLS_V1.equals(wire.layoutId());
        boolean inventory=PlayerInventoryLayouts.ORG_INVENTORY_V1.equals(wire.layoutId());
        boolean ender=PlayerInventoryLayouts.ORG_ENDER_3_V1.equals(wire.layoutId());
        if(wire.inventoryKind()==PlayerInventoryKind.INVENTORY?!(controls||inventory)
                :wire.inventoryKind()!=PlayerInventoryKind.ENDER_CHEST||!ender)return null;
        Identity identity=new Identity(wire.targetUuid(),wire.targetName(),wire.targetType(),wire.targetState());
        return context(wire.containerId(),identity,wire.inventoryKind(),wire.layoutId(),controls);
    }

    public static PlayerInventoryContext tryContext(AbstractContainerMenu menu,Component title){
        if(menu==null)return null;
        MenuType<?> type=menuType(menu);
        if(OrgPlayerInventoryOpenIntent.isOfflineMenu(type,menu.containerId,title))return null;
        OrgPlayerInventoryOpenIntent.Session session=type==null?null:OrgPlayerInventoryOpenIntent.session(menu.containerId,type,title);
        boolean old=marker(menu,OLD_MARKER),controls=marker(menu,BUTTON_MARKER);
        //#if MC >= 260100
        //$$ // Org sends a menu-id-scoped layout marker after the OpenScreen packet.
        //$$ // A command session establishes the target, not whether slots 0..17
        //$$ // are action buttons. Preserve the native menu until Org attests its layout.
        //$$ if(!old&&!controls)return null;
        //#else
        if(!old&&!controls&&session==null)return null;
        //#endif

        Identity identity=identity(title,session);
        if(identity.state==PlayerInventoryTargetState.OFFLINE)return null;
        if(session!=null&&session.kind()==OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST)
            return context(menu.containerId,identity,PlayerInventoryKind.ENDER_CHEST,PlayerInventoryLayouts.ORG_ENDER_3_V1,false);

        String layout=controls?PlayerInventoryLayouts.ORG_CONTROLS_V1:PlayerInventoryLayouts.ORG_INVENTORY_V1;
        return context(menu.containerId,identity,PlayerInventoryKind.INVENTORY,layout,controls);
    }

    private static PlayerInventoryContext context(int containerId,Identity identity,PlayerInventoryKind kind,String layout,boolean controls){
        Set<PlayerInventoryAction> actions=actions(identity,kind,controls);
        Set<PlayerInventoryInteractionCapability> capabilities=controls&&identity.state==PlayerInventoryTargetState.ONLINE?Collections.singleton(PlayerInventoryInteractionCapability.CONTROL_SLOT_BINDING):Collections.<PlayerInventoryInteractionCapability>emptySet();
        return new PlayerInventoryContext(containerId,1,SOURCE,identity.uuid,identity.name,identity.type,identity.state,
                kind,layout,1,controls?BEHAVIOR_CONTROLS:BEHAVIOR_MENU,actions,capabilities,PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
    }

    private static Set<PlayerInventoryAction> actions(Identity identity,PlayerInventoryKind kind,boolean controls){
        //#if MC >= 12111
        if(identity.state==PlayerInventoryTargetState.OFFLINE)return Collections.emptySet();
        //#else
        //$$ if(identity.state!=PlayerInventoryTargetState.ONLINE
        //$$         ||identity.type==PlayerInventoryTargetType.REAL)return Collections.emptySet();
        //#endif
        EnumSet<PlayerInventoryAction> actions=EnumSet.noneOf(PlayerInventoryAction.class);
        actions.addAll(CarpetPlayerCommandBridge.supportedActions());
        //#if MC < 12111
        //$$ if(identity.type==PlayerInventoryTargetType.FAKE){
        //$$     if(controls)actions.addAll(OrgPlayerInventoryActionBinding.nativeActions());
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

    private static PlayerInventoryContext trustedReturnContext(int containerId,OrgPlayerInventoryOpenIntent.Session session){
        if(session==null||session.kind()!=OrgPlayerInventoryOpenIntent.Kind.INVENTORY)return null;
        PlayerInventoryContext source=session.returnContext();
        if(source==null||!SOURCE.equals(source.source())||source.inventoryKind()!=PlayerInventoryKind.INVENTORY
                ||!PlayerInventoryLayouts.isOrgInventory(source)&&!PlayerInventoryLayouts.isOrgControls(source)
                ||!session.target().equals(source.targetName())
                ||source.targetState()!=PlayerInventoryTargetState.ONLINE)return null;
        return new PlayerInventoryContext(containerId,source.contextSchema(),source.source(),source.targetUuid(),source.targetName(),
                source.targetType(),source.targetState(),source.inventoryKind(),source.layoutId(),source.layoutSchema(),
                source.behaviorId(),source.supportedActions(),source.interactionCapabilities(),source.fallbackPolicy());
    }

    //#if MC >= 12111
    /** Org's native three-row player Ender carries an account insertion and
     * click/hover player identity metadata, unlike a plain chest title. */
    public static boolean isClickedEnderTitle(MenuType<?> type,Component title,String name){
        return type==MenuType.GENERIC_9x3&&hasOrgPlayerMarker(title,name);
    }
    static boolean hasOrgPlayerMarker(Component title,String name){
        if(title==null||!CarpetPlayerCommandBridge.validTargetName(name))return false;
        Deque<Component> queue=new ArrayDeque<>();
        queue.add(title);
        boolean found=false;
        while(!queue.isEmpty()){
            Component node=queue.removeFirst();
            String insertion=node.getStyle().getInsertion();
            if(insertion!=null){
                if(!insertion.equals(name))return false;
                if(node.getStyle().getClickEvent()!=null&&node.getStyle().getHoverEvent()!=null)
                    found=true;
            }
            queue.addAll(node.getSiblings());
        }
        return found;
    }
    //#endif

    private static MenuType<?> menuType(AbstractContainerMenu menu){
        try{return menu.getType();}catch(UnsupportedOperationException ignored){return null;}
    }

    private static Identity identity(Component title,OrgPlayerInventoryOpenIntent.Session session){
        String label=title==null?"":title.getString().trim(),target=session==null?label:session.target();
        Minecraft mc=Minecraft.getInstance();

        //#if MC >= 12111
        // Org's styled title includes the canonical account in Component.Style
        // insertion (e.g. visual "[Log head] 假的Log", insertion="Log").
        // Accept only a unique insertion that matches an actual online profile,
        // never the human-readable decorated title or guessed name prefixes.
        Map<String,UUID> online=new LinkedHashMap<>();
        if(mc.getConnection()!=null)for(PlayerInfo info:mc.getConnection().getOnlinePlayers()){
            if(info==null||info.getProfile()==null)continue;
            String name=MinecraftCompat.gameProfileName(info.getProfile());
            if(name!=null)online.put(name,MinecraftCompat.gameProfileId(info.getProfile()));
        }
        if(mc.level!=null)for(Player player:mc.level.players())
            online.putIfAbsent(player.getName().getString(),player.getUUID());
        // An explicit Org /player command or verified return context takes
        // precedence over decorative title metadata.
        String inserted=session==null?profileInsertion(title,online.keySet()):"";
        if(!inserted.isEmpty()){
            UUID uuid=online.get(inserted);
            if(TRACE)TRACE_LOG.info("org_target_source=title_insertion target={} uuid={}",inserted,uuid);
            return new Identity(uuid,inserted,fake(uuid),PlayerInventoryTargetState.ONLINE);
        }
        //#endif

        if(mc.level!=null)for(Player player:mc.level.players()){
            String name=player.getName().getString(),display=player.getDisplayName().getString();
            if(matches(target,label,name,display)){
                UUID uuid=player.getUUID();
                return new Identity(uuid,name,fake(uuid),PlayerInventoryTargetState.ONLINE);
            }
        }

        //#if MC >= 12111
        // A recognized Org menu may belong to a remote player that has no
        // loaded client entity. A matching tab-list profile confirms ONLINE only;
        // fake identity still comes exclusively from the trusted resolver.
        if(session==null&&mc.getConnection()!=null)for(PlayerInfo info:mc.getConnection().getOnlinePlayers()){
            if(info==null||info.getProfile()==null)continue;
            String name=MinecraftCompat.gameProfileName(info.getProfile());
            if(label.equals(name)){
                UUID uuid=MinecraftCompat.gameProfileId(info.getProfile());
                return new Identity(uuid,name,fake(uuid),PlayerInventoryTargetState.ONLINE);
            }
        }

        //#endif

        if(session!=null&&mc.getConnection()!=null){
            PlayerInfo info=mc.getConnection().getPlayerInfo(session.target());
            if(info!=null){
                UUID uuid=playerInfoUuid(mc.getConnection(),info);
                return new Identity(uuid,session.target(),fake(uuid),PlayerInventoryTargetState.ONLINE);
            }
            return new Identity(null,session.target(),PlayerInventoryTargetType.REAL,PlayerInventoryTargetState.OFFLINE);
        }
        return new Identity(null,target,PlayerInventoryTargetType.UNKNOWN,PlayerInventoryTargetState.UNKNOWN);
    }

    //#if MC >= 12111
    /**
     * Org's player label contains a styled account node with an insertion.
     * Only accept one account already present in the online GameProfile list.
     * Titles with multiple distinct account insertions are ambiguous.
     */
    static String profileInsertion(Component title,Collection<String> profileNames){
        if(title==null||profileNames==null||profileNames.isEmpty())return "";
        Set<String> known=new HashSet<>(profileNames);
        Deque<Component> queue=new ArrayDeque<>();
        queue.add(title);
        String matched="";
        while(!queue.isEmpty()){
            Component node=queue.removeFirst();
            String insertion=node.getStyle().getInsertion();
            if(insertion!=null&&known.contains(insertion)){
                if(!matched.isEmpty()&&!matched.equals(insertion))return "";
                matched=insertion;
            }
            queue.addAll(node.getSiblings());
        }
        return matched;
    }
    //#endif

    private static boolean matches(String target,String label,String name,String display){
        return target.equals(name)||target.equals(display)||label.equals(name)||label.equals(display);
    }
    private static UUID playerInfoUuid(ClientPacketListener connection,PlayerInfo info){
        if(connection==null||info==null)return null;
        for(PlayerInfo candidate:connection.getOnlinePlayers())if(candidate==info&&candidate.getProfile()!=null)return MinecraftCompat.gameProfileId(candidate.getProfile());
        return null;
    }
    private static PlayerInventoryTargetType fake(UUID uuid){
        if(uuid==null)return PlayerInventoryTargetType.UNKNOWN;
        PlayerInventoryTargetType synced=PlayerIdentityResolver.resolve(uuid);
        if(synced!=PlayerInventoryTargetType.UNKNOWN)return synced;
        return OrgFakePlayerIdentityClient.isFake(uuid)||isOrgFake(uuid)?PlayerInventoryTargetType.FAKE:PlayerInventoryTargetType.UNKNOWN;
    }
    private static boolean marker(Object target,String name){
        try{Method method=target.getClass().getMethod(name);Object value=method.invoke(target);return value instanceof Boolean&&((Boolean)value);}
        catch(ReflectiveOperationException|LinkageError|SecurityException ignored){return false;}
    }
    private static boolean orgRegistryAvailable(){for(String className:CLIENT_UTILS)try{Class.forName(className,false,OrgPlayerInventoryAdapter.class.getClassLoader());return true;}catch(ClassNotFoundException|LinkageError|SecurityException ignored){}return false;}
    private static boolean isOrgFake(UUID uuid){
        if(uuid==null)return false;
        for(String className:CLIENT_UTILS)try{
            Class<?> type=Class.forName(className,false,OrgPlayerInventoryAdapter.class.getClassLoader());
            Field field=type.getField("FAKE_PLAYERS");Object value=field.get(null);
            if(value instanceof Set<?> && ((Set<?>)value).contains(uuid))return true;
        }catch(ReflectiveOperationException|LinkageError|SecurityException ignored){}
        return false;
    }
    private static final class Identity{
        private final UUID uuid;private final String name;private final PlayerInventoryTargetType type;private final PlayerInventoryTargetState state;
        private Identity(UUID uuid,String name,PlayerInventoryTargetType type,PlayerInventoryTargetState state){this.uuid=uuid;this.name=name;this.type=type;this.state=state;}
    }
}
