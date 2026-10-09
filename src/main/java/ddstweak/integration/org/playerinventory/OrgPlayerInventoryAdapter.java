package ddstweak.integration.org.playerinventory;

import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge;
import ddstweak.integration.provider.PlayerIdentityResolver;
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
        if(!old&&!controls&&session==null)return null;

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
        if(identity.state!=PlayerInventoryTargetState.ONLINE||identity.type==PlayerInventoryTargetType.REAL)return Collections.emptySet();
        EnumSet<PlayerInventoryAction> actions=EnumSet.noneOf(PlayerInventoryAction.class);
        actions.addAll(CarpetPlayerCommandBridge.supportedActions());
        if(identity.type==PlayerInventoryTargetType.FAKE){
            if(controls)actions.addAll(OrgPlayerInventoryActionBinding.nativeActions());
            actions.add(PlayerInventoryAction.STOP_ATTACK);
            actions.add(PlayerInventoryAction.STOP_USE);
        }else{
            actions.remove(PlayerInventoryAction.DROP_ALL);
            actions.remove(PlayerInventoryAction.DISCONNECT);
            actions.remove(PlayerInventoryAction.TOGGLE_SNEAK);
        }
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

    private static MenuType<?> menuType(AbstractContainerMenu menu){
        try{return menu.getType();}catch(UnsupportedOperationException ignored){return null;}
    }

    private static Identity identity(Component title,OrgPlayerInventoryOpenIntent.Session session){
        String label=title==null?"":title.getString().trim(),target=session==null?label:session.target();
        Minecraft mc=Minecraft.getInstance();

        if(mc.level!=null)for(Player player:mc.level.players()){
            String name=player.getName().getString(),display=player.getDisplayName().getString();
            if(matches(target,label,name,display)){
                UUID uuid=player.getUUID();
                return new Identity(uuid,name,fake(uuid),PlayerInventoryTargetState.ONLINE);
            }
        }

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
