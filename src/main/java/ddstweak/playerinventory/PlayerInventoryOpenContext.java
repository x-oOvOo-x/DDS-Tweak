package ddstweak.playerinventory;

import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.integration.provider.PlayerIdentityResolver;
import java.util.Collections;
import java.util.UUID;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;

/**
 * Short-lived client navigation context used when a provider fake-player inventory opens
 * a generic Ender Chest through QCA. It preserves the target identity while the server
 * remains authoritative for the new menu and its 3/6-row capacity.
 */
public final class PlayerInventoryOpenContext {
    private static final long TTL_NANOS=3_000_000_000L;
    private static PlayerInventoryContext pendingEnder,pendingInventory;
    private static long pendingAt,pendingInventoryAt;
    private static int pendingEnderId=-1;

    private PlayerInventoryOpenContext(){}

    public static synchronized void expectEnder(PlayerInventoryContext source){
        if(source==null||source.targetName().trim().isEmpty())return;
        pendingEnder=source;pendingAt=System.nanoTime();pendingEnderId=-1;
    }

    /** The server ties this owner to the exact Ender menu, including returns that missed the click hook. */
    public static synchronized void expectVerifiedEnder(int containerId,UUID ownerUuid,String ownerName,PlayerInventoryContext previous){
        if(containerId<0||ownerUuid==null||ownerName==null||ownerName.trim().isEmpty()||ownerName.length()>64)return;
        String source=previous==null?"dds_qca":previous.source();
        String behavior=previous==null?"dds_qca_ender_v1":previous.behaviorId();
        pendingEnder=new PlayerInventoryContext(containerId,1,source,ownerUuid,ownerName,
                PlayerInventoryTargetType.FAKE,PlayerInventoryTargetState.ONLINE,PlayerInventoryKind.ENDER_CHEST,
                PlayerInventoryLayouts.ENDER_3_V1,1,behavior,CarpetPlayerCommandBridge.supportedActions(),
                Collections.<PlayerInventoryInteractionCapability>emptySet(),PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
        pendingEnderId=containerId;pendingAt=System.nanoTime();
    }

    /** A verified QCA return reopens GCA's inventory with the target name as its title. */
    public static synchronized void expectInventory(PlayerInventoryContext source){
        if(source==null||!"gca".equals(source.source())||source.inventoryKind()!=PlayerInventoryKind.INVENTORY
                ||!PlayerInventoryLayouts.GCA_INVENTORY_V1.equals(source.layoutId())||source.targetName().trim().isEmpty())return;
        pendingInventory=source;pendingInventoryAt=System.nanoTime();
    }

    public static synchronized PlayerInventoryContext consumeInventory(MenuType<?> type,int containerId,Component title){
        if(pendingInventory==null)return null;
        if(System.nanoTime()-pendingInventoryAt>TTL_NANOS){clearInventory();return null;}
        PlayerInventoryContext source=pendingInventory;
        if(type!=MenuType.GENERIC_9x6||!matchesInventoryTitle(title,source))return null;
        clearInventory();
        return new PlayerInventoryContext(containerId,source.contextSchema(),source.source(),source.targetUuid(),source.targetName(),
                source.targetType(),source.targetState(),source.inventoryKind(),source.layoutId(),source.layoutSchema(),
                source.behaviorId(),source.supportedActions(),source.interactionCapabilities(),source.fallbackPolicy());
    }

    public static synchronized PlayerInventoryContext consumeEnder(MenuType<?> type,int containerId,Component title){
        if(pendingEnder==null)return null;
        if(System.nanoTime()-pendingAt>TTL_NANOS){clearEnder();return null;}
        int rows=type==MenuType.GENERIC_9x6?6:type==MenuType.GENERIC_9x3?3:0;
        PlayerInventoryContext source=pendingEnder;
        if(rows==0||pendingEnderId>=0&&pendingEnderId!=containerId||!matchesEnderTitle(title,source))return null;
        clearEnder();
        Set<PlayerInventoryAction> actions=enderActions(source);
        return new PlayerInventoryContext(containerId,source.contextSchema(),source.source(),source.targetUuid(),source.targetName(),
                source.targetType(),source.targetState(),PlayerInventoryKind.ENDER_CHEST,
                rows==6?PlayerInventoryLayouts.ENDER_6_V1:PlayerInventoryLayouts.ENDER_3_V1,1,
                source.behaviorId(),actions,source.interactionCapabilities(),source.fallbackPolicy());
    }

    private static Set<PlayerInventoryAction> enderActions(PlayerInventoryContext source){
        EnumSet<PlayerInventoryAction> actions=source.supportedActions().isEmpty()
                ?EnumSet.noneOf(PlayerInventoryAction.class):EnumSet.copyOf(source.supportedActions());
        if(PlayerIdentityResolver.isAuthoritativeFake(source)){
            if(CarpetPlayerCommandBridge.supports(PlayerInventoryAction.TOGGLE_SNEAK))
                actions.add(PlayerInventoryAction.TOGGLE_SNEAK);
            if(DdsQcaClientNetwork.canUseOwnerEnderAction()){
                actions.add(PlayerInventoryAction.ENDER_DROP_ALL);
                actions.add(PlayerInventoryAction.ENDER_STORE_ALL);
                actions.add(PlayerInventoryAction.ENDER_TAKE_ALL);
            }
        }
        return Collections.unmodifiableSet(actions);
    }

    public static synchronized void revokeStrictAuthority(UUID targetUuid){
        if(targetUuid==null)return;
        if(pendingEnder!=null&&targetUuid.equals(pendingEnder.targetUuid()))
            pendingEnder=PlayerIdentityResolver.withoutStrictSession(pendingEnder);
        if(pendingInventory!=null&&targetUuid.equals(pendingInventory.targetUuid()))
            pendingInventory=PlayerIdentityResolver.withoutStrictSession(pendingInventory);
    }

    public static synchronized void clear(){clearEnder();clearInventory();}
    private static void clearEnder(){pendingEnder=null;pendingAt=0L;pendingEnderId=-1;}
    private static void clearInventory(){pendingInventory=null;pendingInventoryAt=0L;}

    private static boolean matchesInventoryTitle(Component title,PlayerInventoryContext source){
        if(title==null||source==null)return false;
        String visible=title.getString().trim(),target=source.targetName().trim();
        if(visible.equals(target))return true;
        if("gca".equals(source.source())){
            String expected=MinecraftCompat.translatable("gca.player.inventory",MinecraftCompat.literal(target)).getString();
            if(visible.equals(expected.trim()))return true;
        }
        return matchesOfflineOrgTitle(visible,source,target);
    }

    private static boolean matchesEnderTitle(Component title,PlayerInventoryContext source){
        if(title==null||source==null)return false;
        if("container.enderchest".equals(MinecraftCompat.translationKey(title)))return true;
        String visible=title.getString().trim(),target=source.targetName().trim();
        if("Ender Chest".equals(visible)||"末影箱".equals(visible)||visible.equals(target))return true;
        return matchesOfflineOrgTitle(visible,source,target);
    }

    private static boolean matchesOfflineOrgTitle(String visible,PlayerInventoryContext source,String target){
        if(!"org".equals(source.source())||source.targetState()!=PlayerInventoryTargetState.OFFLINE)return false;
        String modern=MinecraftCompat.translatable("carpet-org-addition.operation.offline_player_name",
                MinecraftCompat.literal(target)).getString().trim();
        if(visible.equals(modern))return true;
        String legacy=MinecraftCompat.translatable("carpet.commands.player.inventory.offline.display_name",
                MinecraftCompat.literal(target)).getString().trim();
        return visible.equals(legacy);
    }
}
