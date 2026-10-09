package ddstweak.integration.carpet.playerinventory;

import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.playerinventory.PlayerInventoryClientActionState;

/**
 * Preferred route for standard Carpet /player actions on the new inventory nodes.
 * Provider native controls and DDS RPCs remain fallbacks when the command tree lacks an action.
 */
public final class CarpetPlayerCommandActionBinding implements PlayerInventoryActionBinding {
    private static boolean initialized;
    private UnifiedPlayerInventoryMirrorMenu menu;
    private PlayerInventoryClientActionState.State local;

    private CarpetPlayerCommandActionBinding(){}

    public static synchronized void initialize(){
        if(initialized)return;
        PlayerInventoryActionBindingRouter.registerPreferred(CarpetPlayerCommandActionBinding::new);
        initialized=true;
    }

    @Override public void attach(UnifiedPlayerInventoryMirrorMenu menu){
        this.menu=menu;
        this.local=PlayerInventoryClientActionState.forContext(menu.context());
    }

    @Override public boolean canSend(){
        return menu!=null&&menu.context().targetState()==PlayerInventoryTargetState.ONLINE
                &&menu.targetType()!=PlayerInventoryTargetType.REAL&&CarpetPlayerCommandBridge.available();
    }

    @Override public boolean supports(PlayerInventoryAction action){
        if(!canSend()||action==null||!action.allowedIn(menu.kind())||!CarpetPlayerCommandBridge.supports(action))return false;
        if(action==PlayerInventoryAction.ENDER_DROP_ALL||action==PlayerInventoryAction.ENDER_STORE_ALL||action==PlayerInventoryAction.ENDER_TAKE_ALL)return false;
        if(action==PlayerInventoryAction.DROP_ALL||action==PlayerInventoryAction.DISCONNECT||action==PlayerInventoryAction.TOGGLE_SNEAK)
            return PlayerIdentityResolver.isAuthoritativeFake(menu.context());
        return true;
    }

    @Override public boolean send(PlayerInventoryAction action,int value){
        if(!supports(action)||!CarpetPlayerCommandBridge.send(menu.context().targetName(),action,value))return false;
        state().record(action,value);
        return true;
    }

    @Override public int selectedTargetHotbarSlot(){return state().selectedKnown()?state().selected():-1;}
    @Override public int attackMode(){return state().attackKnown()?state().attackMode():PlayerInventoryBehaviorV1.MODE_NONE;}
    @Override public int attackInterval(){return state().attackInterval();}
    @Override public int useMode(){return state().useKnown()?state().useMode():PlayerInventoryBehaviorV1.MODE_NONE;}
    @Override public int useInterval(){return state().useInterval();}
    @Override public boolean sneaking(){return state().sneaking();}
    @Override public PlayerInventoryStateConfidence selectedHotbarConfidence(){return state().selectedKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence attackModeConfidence(){return state().attackKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence attackIntervalConfidence(){return state().attackKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence useModeConfidence(){return state().useKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence useIntervalConfidence(){return state().useKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence sneakingConfidence(){return state().sneakingKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}

    private PlayerInventoryClientActionState.State state(){
        return local!=null?local:PlayerInventoryClientActionState.forContext(menu.context());
    }
}
