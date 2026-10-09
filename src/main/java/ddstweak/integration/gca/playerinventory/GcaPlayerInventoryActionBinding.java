package ddstweak.integration.gca.playerinventory;

import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge;
import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import ddstweak.integration.dds.qca.DdsQcaItems;
import ddstweak.playerinventory.PlayerInventoryClientActionState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
//#if MC >= 260102
//$$ import net.minecraft.world.inventory.ContainerInput;
//#else
import net.minecraft.world.inventory.ClickType;
//#endif
import net.minecraft.world.item.Items;
import java.util.*;

/** Executes GCA fixed control slots or standard Carpet commands; local command state survives screen reopenings. */
public final class GcaPlayerInventoryActionBinding implements PlayerInventoryActionBinding{
    private static final Set<PlayerInventoryAction> NATIVE_ACTIONS=Collections.unmodifiableSet(EnumSet.of(
            PlayerInventoryAction.SELECT_HOTBAR,PlayerInventoryAction.ATTACK_CONTINUOUS,PlayerInventoryAction.STOP_ATTACK,
            PlayerInventoryAction.USE_CONTINUOUS,PlayerInventoryAction.STOP_USE,PlayerInventoryAction.STOP_ALL));
    private UnifiedPlayerInventoryMirrorMenu menu;
    private PlayerInventoryClientActionState.State local;

    static Set<PlayerInventoryAction> nativeActions(){return NATIVE_ACTIONS;}

    @Override public void attach(UnifiedPlayerInventoryMirrorMenu menu){
        this.menu=menu;
        this.local=PlayerInventoryClientActionState.forContext(menu.context());
    }

    @Override public boolean canSend(){
        Minecraft mc=Minecraft.getInstance();
        if(menu==null||menu.context().targetState()!=PlayerInventoryTargetState.ONLINE||mc.player==null||mc.gameMode==null)return false;
        PlayerInventoryTargetType type=menu.targetType();
        if(type==PlayerInventoryTargetType.REAL)return false;
        if(type==PlayerInventoryTargetType.UNKNOWN)return CarpetPlayerCommandBridge.available();
        return CarpetPlayerCommandBridge.available()||DdsQcaClientNetwork.canUseOwnerEnderAction()||nativeAvailable();
    }

    @Override public boolean supports(PlayerInventoryAction action){
        if(!canSend()||action==null||!action.allowedIn(menu.kind()))return false;
        PlayerInventoryTargetType type=menu.targetType();
        if(type==PlayerInventoryTargetType.UNKNOWN)return CarpetPlayerCommandBridge.supports(action);
        if(isEnderBatch(action))return DdsQcaClientNetwork.canUseOwnerEnderAction();
        return nativeSupports(action)||CarpetPlayerCommandBridge.supports(action);
    }

    @Override public boolean send(PlayerInventoryAction action,int value){
        if(!canSend()||action==null||!action.allowedIn(menu.kind())||!menu.supports(action))return false;
        if(isEnderBatch(action))return DdsQcaClientNetwork.sendOwnerEnderAction(menu,action);
        int slot=nativeSlot(action,value);
        if(slot>=0&&sendNative(slot)){record(action,value);return true;}
        if(CarpetPlayerCommandBridge.send(menu.context().targetName(),action,value)){record(action,value);return true;}
        return false;
    }

    //#if MC >= 260102
    //$$ @Override public boolean handleControlClick(int slotId,int button,ContainerInput type,Player player){
    //$$     return menu!=null&&menu.slotRole(slotId)==PlayerInventorySlotRole.CONTROL;
    //$$ }
    //#else
    @Override public boolean handleControlClick(int slotId,int button,ClickType type,Player player){
        return menu!=null&&menu.slotRole(slotId)==PlayerInventorySlotRole.CONTROL;
    }
    //#endif

    @Override public int selectedTargetHotbarSlot(){if(state().selectedKnown())return state().selected();if(isFixedInventory())for(int i=0;i<9;i++)if(on(9+i))return i;return -1;}
    @Override public int attackMode(){if(state().attackKnown())return state().attackMode();if(isFixedInventory()){if(on(6))return PlayerInventoryBehaviorV1.MODE_CONTINUOUS;if(on(5))return PlayerInventoryBehaviorV1.MODE_INTERVAL;}return PlayerInventoryBehaviorV1.MODE_NONE;}
    @Override public int attackInterval(){if(state().attackKnown())return state().attackInterval();return isFixedInventory()&&on(5)?12:state().attackInterval();}
    @Override public int useMode(){if(state().useKnown())return state().useMode();if(isFixedInventory()&&on(8))return PlayerInventoryBehaviorV1.MODE_CONTINUOUS;return PlayerInventoryBehaviorV1.MODE_NONE;}
    @Override public int useInterval(){return state().useInterval();}
    @Override public boolean sneaking(){return state().sneaking();}

    @Override public PlayerInventoryStateConfidence selectedHotbarConfidence(){return state().selectedKnown()?PlayerInventoryStateConfidence.LOCAL:isFixedInventory()&&controlsPresent(9,18)?PlayerInventoryStateConfidence.AUTHORITATIVE:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence attackModeConfidence(){return state().attackKnown()?PlayerInventoryStateConfidence.LOCAL:isFixedInventory()&&(on(5)||on(6))?PlayerInventoryStateConfidence.AUTHORITATIVE:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence attackIntervalConfidence(){return state().attackKnown()?PlayerInventoryStateConfidence.LOCAL:isFixedInventory()&&on(5)?PlayerInventoryStateConfidence.AUTHORITATIVE:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence useModeConfidence(){return state().useKnown()?PlayerInventoryStateConfidence.LOCAL:isFixedInventory()&&on(8)?PlayerInventoryStateConfidence.AUTHORITATIVE:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence useIntervalConfidence(){return state().useKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence sneakingConfidence(){return state().sneakingKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}

    private static boolean isEnderBatch(PlayerInventoryAction action){return action==PlayerInventoryAction.ENDER_DROP_ALL||action==PlayerInventoryAction.ENDER_STORE_ALL||action==PlayerInventoryAction.ENDER_TAKE_ALL;}

    private void record(PlayerInventoryAction action,int value){state().record(action,value);}
    private PlayerInventoryClientActionState.State state(){return local!=null?local:PlayerInventoryClientActionState.forContext(menu.context());}

    private int nativeSlot(PlayerInventoryAction action,int value){
        if(!nativeSupports(action))return -1;
        switch(action){
            case SELECT_HOTBAR:return value>=1&&value<=9?8+value:-1;
            case ATTACK_CONTINUOUS:return 6;
            case STOP_ATTACK:return on(5)?5:on(6)?6:-1;
            case USE_CONTINUOUS:return 8;
            case STOP_USE:return on(8)?8:-1;
            case STOP_ALL:return 0;
            default:return -1;
        }
    }
    private boolean nativeAvailable(){
        //#if MC >= 12111
        return false;
        //#else
        //$$ return menu!=null&&menu.targetType()==PlayerInventoryTargetType.FAKE
        //$$         &&PlayerInventoryLayouts.isGcaInventory(menu.context())
        //$$         &&menu.context().allows(PlayerInventoryInteractionCapability.CONTROL_SLOT_BINDING);
        //#endif
    }
    private boolean nativeSupports(PlayerInventoryAction action){
        if(!nativeAvailable()||!NATIVE_ACTIONS.contains(action))return false;
        if(action==PlayerInventoryAction.STOP_ATTACK)return on(5)||on(6);
        if(action==PlayerInventoryAction.STOP_USE)return on(8);
        return true;
    }
    private boolean sendNative(int slot){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.gameMode==null)return false;
        //#if MC >= 260102
        //$$ mc.gameMode.handleContainerInput(menu.containerId,slot,0,ContainerInput.PICKUP,mc.player);
        //#else
        mc.gameMode.handleInventoryMouseClick(menu.containerId,slot,0,ClickType.PICKUP,mc.player);
        //#endif
        return true;
    }
    private boolean isFixedInventory(){return menu!=null&&PlayerInventoryLayouts.isGcaInventory(menu.context());}
    private boolean controlsPresent(int from,int to){if(menu==null)return false;for(int i=from;i<to;i++)if(!controlPresent(i))return false;return true;}
    private boolean controlPresent(int slot){return menu!=null&&slot>=0&&slot<menu.slots.size()&&!menu.getSlot(slot).getItem().isEmpty();}
    private boolean on(int slot){return menu!=null&&slot>=0&&slot<menu.slots.size()&&DdsQcaItems.isItem(menu.getSlot(slot).getItem(),Items.BARRIER);}
}
