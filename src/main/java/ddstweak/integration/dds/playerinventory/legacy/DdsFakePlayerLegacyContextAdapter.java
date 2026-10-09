package ddstweak.integration.dds.playerinventory.legacy;

import ddstweak.integration.dds.fakeplayer.*;
import ddstweak.integration.dds.playerinventory.*;
import java.util.*;

/** Maps the existing Protocol-v1 fake_player_open_menu_s2c contract into the neutral GUI-v2 model without changing the wire contract. */
public final class DdsFakePlayerLegacyContextAdapter{
    public static final String LAYOUT_INVENTORY=PlayerInventoryLayouts.INVENTORY_V1,LAYOUT_ENDER_3=PlayerInventoryLayouts.ENDER_3_V1,LAYOUT_ENDER_6=PlayerInventoryLayouts.ENDER_6_V1;
    private static final Map<DdsFakePlayerAction,PlayerInventoryAction>ACTIONS=createActions();
    private DdsFakePlayerLegacyContextAdapter(){}

    private static Map<DdsFakePlayerAction,PlayerInventoryAction> createActions(){
        EnumMap<DdsFakePlayerAction,PlayerInventoryAction> m=new EnumMap<DdsFakePlayerAction,PlayerInventoryAction>(DdsFakePlayerAction.class);
        m.put(DdsFakePlayerAction.SELECT_HOTBAR,PlayerInventoryAction.SELECT_HOTBAR);
        m.put(DdsFakePlayerAction.ATTACK_INTERVAL,PlayerInventoryAction.ATTACK_INTERVAL);
        m.put(DdsFakePlayerAction.ATTACK_CONTINUOUS,PlayerInventoryAction.ATTACK_CONTINUOUS);
        m.put(DdsFakePlayerAction.STOP_ATTACK,PlayerInventoryAction.STOP_ATTACK);
        m.put(DdsFakePlayerAction.USE_INTERVAL,PlayerInventoryAction.USE_INTERVAL);
        m.put(DdsFakePlayerAction.USE_CONTINUOUS,PlayerInventoryAction.USE_CONTINUOUS);
        m.put(DdsFakePlayerAction.STOP_USE,PlayerInventoryAction.STOP_USE);
        m.put(DdsFakePlayerAction.STOP_ALL,PlayerInventoryAction.STOP_ALL);
        m.put(DdsFakePlayerAction.DROP_ALL,PlayerInventoryAction.DROP_ALL);
        m.put(DdsFakePlayerAction.ENDER_DROP_ALL,PlayerInventoryAction.ENDER_DROP_ALL);
        m.put(DdsFakePlayerAction.ENDER_STORE_ALL,PlayerInventoryAction.ENDER_STORE_ALL);
        m.put(DdsFakePlayerAction.ENDER_TAKE_ALL,PlayerInventoryAction.ENDER_TAKE_ALL);
        m.put(DdsFakePlayerAction.TOGGLE_SNEAK,PlayerInventoryAction.TOGGLE_SNEAK);
        m.put(DdsFakePlayerAction.DISCONNECT,PlayerInventoryAction.DISCONNECT);
        return Collections.unmodifiableMap(m);
    }

    public static PlayerInventoryAction action(DdsFakePlayerAction action){return action==null?null:ACTIONS.get(action);}
    public static DdsFakePlayerAction legacyAction(PlayerInventoryAction action){if(action==null)return null;for(Map.Entry<DdsFakePlayerAction,PlayerInventoryAction> entry:ACTIONS.entrySet())if(entry.getValue()==action)return entry.getKey();return null;}
    public static boolean supports(PlayerInventoryContext context,DdsFakePlayerAction action){PlayerInventoryAction mapped=action(action);return context!=null&&mapped!=null&&context.supports(mapped);}
    public static int enderRows(PlayerInventoryContext context){if(context==null||context.inventoryKind()!=PlayerInventoryKind.ENDER_CHEST)return 0;return LAYOUT_ENDER_3.equals(context.layoutId())?3:LAYOUT_ENDER_6.equals(context.layoutId())?6:0;}
    public static PlayerInventoryContext adapt(DdsFakePlayerOpenContext legacy){
        if(legacy==null||!legacy.mirrorCompatible())return null;
        EnumSet<PlayerInventoryAction>actions=EnumSet.noneOf(PlayerInventoryAction.class);
        for(DdsFakePlayerAction a:legacy.supportedActions()){PlayerInventoryAction mapped=ACTIONS.get(a);if(mapped!=null)actions.add(mapped);}
        PlayerInventoryKind kind=legacy.layout()==DdsFakePlayerPackets.MENU_INVENTORY?PlayerInventoryKind.INVENTORY:PlayerInventoryKind.ENDER_CHEST;
        String layout=kind==PlayerInventoryKind.INVENTORY?LAYOUT_INVENTORY:legacy.layout()==DdsFakePlayerPackets.MENU_ENDER_3?LAYOUT_ENDER_3:LAYOUT_ENDER_6;
        EnumSet<PlayerInventoryInteractionCapability>caps=EnumSet.noneOf(PlayerInventoryInteractionCapability.class);
        if(kind==PlayerInventoryKind.INVENTORY){caps.add(PlayerInventoryInteractionCapability.SORT_MAIN);caps.add(PlayerInventoryInteractionCapability.SORT_HOTBAR);caps.add(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP);}
        else{caps.add(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP);caps.add(PlayerInventoryInteractionCapability.QUICK_MOVE_PREDICTION);}
        return new PlayerInventoryContext(legacy.containerId(),1,"dds",legacy.targetUuid(),legacy.targetName(),PlayerInventoryTargetType.FAKE,PlayerInventoryTargetState.ONLINE,kind,layout,1,PlayerInventoryBehaviorV1.ID,actions,caps,PlayerInventoryFallbackPolicy.REJECT_INCOMPATIBLE_CONTAINER);
    }
}
