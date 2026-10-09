package ddstweak.integration.gca.playerinventory;

import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.integration.dds.playerinventory.*;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
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
    private GcaPlayerInventoryAdapter(){}

    public static PlayerInventoryContext tryContext(MenuType<?> type,int containerId,Component title){
        if(title==null)return null;
        Resolved resolved=resolve(type,title);
        if(resolved==null)return null;
        return context(containerId,resolved.target,resolved.kind,resolved.layout);
    }

    public static PlayerInventoryContext authoritativeContext(MenuType<?> type,PlayerInventoryContext wire){
        if(wire==null||!SOURCE.equals(wire.source()))return null;
        PlayerInventoryKind kind=wire.inventoryKind();
        String layout=wire.layoutId();
        if(kind==PlayerInventoryKind.INVENTORY&&!PlayerInventoryLayouts.GCA_INVENTORY_V1.equals(layout)
                ||kind==PlayerInventoryKind.CONTROLLER&&!PlayerInventoryLayouts.GCA_CONTROLLER_3_V1.equals(layout)
                ||kind==PlayerInventoryKind.ENDER_CHEST&&!PlayerInventoryLayouts.GCA_ENDER_3_V1.equals(layout))return null;
        Set<PlayerInventoryAction> actions=wire.targetState()==PlayerInventoryTargetState.ONLINE
                ?actions(kind,wire.targetType()):Collections.<PlayerInventoryAction>emptySet();
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

    private static PlayerInventoryContext context(int containerId,String target,PlayerInventoryKind kind,String layout){
        Identity identity=identity(target);
        Set<PlayerInventoryAction> actions=actions(kind,identity.type);
        return new PlayerInventoryContext(containerId,1,SOURCE,identity.uuid,identity.name,identity.type,PlayerInventoryTargetState.ONLINE,
                kind,layout,1,BEHAVIOR_ID,actions,Collections.singleton(PlayerInventoryInteractionCapability.CONTROL_SLOT_BINDING),
                PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
    }

    private static Set<PlayerInventoryAction> actions(PlayerInventoryKind kind,PlayerInventoryTargetType type){
        if(type==PlayerInventoryTargetType.REAL)return Collections.emptySet();
        EnumSet<PlayerInventoryAction> actions=EnumSet.noneOf(PlayerInventoryAction.class);
        actions.addAll(CarpetPlayerCommandBridge.supportedActions());
        if(type==PlayerInventoryTargetType.FAKE){
            if(kind==PlayerInventoryKind.INVENTORY)actions.addAll(GcaPlayerInventoryActionBinding.nativeActions());
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

    private static Identity identity(String fallback){
        Minecraft mc=Minecraft.getInstance();
        if(mc.level!=null)for(Player player:mc.level.players()){
            String name=player.getName().getString(),display=player.getDisplayName().getString();
            if(matches(fallback,name,display)){
                PlayerInventoryTargetType type=PlayerIdentityResolver.resolve(player.getUUID());
                return new Identity(player.getUUID(),name,type);
            }
        }
        return new Identity(null,fallback,PlayerInventoryTargetType.UNKNOWN);
    }

    private static String findTargetName(String title){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||title==null)return "";
        Player best=null;int length=-1;
        for(Player player:mc.level.players()){
            String name=player.getName().getString(),display=player.getDisplayName().getString();
            if(title.contains(name)&&name.length()>length){best=player;length=name.length();}
            if(title.contains(display)&&display.length()>length){best=player;length=display.length();}
        }
        return best==null?"":best.getName().getString();
    }
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
