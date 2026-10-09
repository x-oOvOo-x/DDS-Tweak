package ddstweak.integration.dds.fakeplayer;

import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge;
import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.provider.PlayerIdentityResolver;
import ddstweak.playerinventory.PlayerInventoryClientActionState;

/** DDS RPC compatibility fallback for provider menus when the standard Carpet command is unavailable. */
public final class DdsFakePlayerExtensionActionBinding implements PlayerInventoryActionBinding{
    private UnifiedPlayerInventoryMirrorMenu menu;
    private PlayerInventoryClientActionState.State local;

    @Override public void attach(UnifiedPlayerInventoryMirrorMenu menu){
        this.menu=menu;
        this.local=PlayerInventoryClientActionState.forContext(menu.context());
    }
    @Override public boolean canSend(){
        return menu!=null&&PlayerIdentityResolver.isAuthoritativeFake(menu.context())
                &&(DdsFakePlayerClientNetwork.canUseProviderActions()
                ||extension()!=null&&DdsFakePlayerClientNetwork.canUseActions());
    }
    @Override public boolean supports(PlayerInventoryAction action){
        return canSend()&&action!=null&&!isEnderBatch(action)&&!standardCommandPreferred(action)
                &&(DdsFakePlayerClientNetwork.canSendExtensionAction(menu,action)
                ||DdsFakePlayerClientNetwork.canSendProviderAction(menu,action));
    }
    @Override public boolean send(PlayerInventoryAction action,int value){
        if(!canSend()||action==null||standardCommandPreferred(action)||!action.allowedIn(menu.kind())||!menu.supports(action))return false;
        if(DdsFakePlayerClientNetwork.sendExtensionAction(menu,action,value)
                ||DdsFakePlayerClientNetwork.sendProviderAction(menu,action,value)){
            state().record(action,value);
            return true;
        }
        return false;
    }
    @Override public int selectedTargetHotbarSlot(){DdsFakePlayerExtensionContext e=extension();return e!=null&&!preferLocalSelected(e)?e.selectedHotbar():state().selectedKnown()?state().selected():-1;}
    @Override public int attackMode(){DdsFakePlayerExtensionContext e=extension();return e!=null&&!preferLocalAttack(e)?e.attackMode():state().attackKnown()?state().attackMode():PlayerInventoryBehaviorV1.MODE_NONE;}
    @Override public int attackInterval(){DdsFakePlayerExtensionContext e=extension();return e!=null&&!preferLocalAttack(e)?e.attackInterval():state().attackInterval();}
    @Override public int useMode(){DdsFakePlayerExtensionContext e=extension();return e!=null&&!preferLocalUse(e)?e.useMode():state().useKnown()?state().useMode():PlayerInventoryBehaviorV1.MODE_NONE;}
    @Override public int useInterval(){DdsFakePlayerExtensionContext e=extension();return e!=null&&!preferLocalUse(e)?e.useInterval():state().useInterval();}
    @Override public boolean sneaking(){DdsFakePlayerExtensionContext e=extension();return e!=null&&!preferLocalSneaking(e)?e.sneaking():state().sneaking();}
    @Override public PlayerInventoryStateConfidence selectedHotbarConfidence(){DdsFakePlayerExtensionContext e=extension();return e!=null?(preferLocalSelected(e)?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.AUTHORITATIVE):state().selectedKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence attackModeConfidence(){DdsFakePlayerExtensionContext e=extension();return e!=null?(preferLocalAttack(e)?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.AUTHORITATIVE):state().attackKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence attackIntervalConfidence(){DdsFakePlayerExtensionContext e=extension();return e!=null?(preferLocalAttack(e)?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.AUTHORITATIVE):state().attackKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence useModeConfidence(){DdsFakePlayerExtensionContext e=extension();return e!=null?(preferLocalUse(e)?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.AUTHORITATIVE):state().useKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence useIntervalConfidence(){DdsFakePlayerExtensionContext e=extension();return e!=null?(preferLocalUse(e)?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.AUTHORITATIVE):state().useKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}
    @Override public PlayerInventoryStateConfidence sneakingConfidence(){DdsFakePlayerExtensionContext e=extension();return e!=null?(preferLocalSneaking(e)?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.AUTHORITATIVE):state().sneakingKnown()?PlayerInventoryStateConfidence.LOCAL:PlayerInventoryStateConfidence.UNKNOWN;}

    private static boolean standardCommandPreferred(PlayerInventoryAction action){
        //#if MC >= 12111
        return CarpetPlayerCommandBridge.supports(action);
        //#else
        //$$ return false;
        //#endif
    }
    private PlayerInventoryClientActionState.State state(){return local!=null?local:PlayerInventoryClientActionState.forContext(menu.context());}
    private boolean preferLocalSelected(DdsFakePlayerExtensionContext e){return e!=null&&state().preferLocalSelected(e.selectedHotbar());}
    private boolean preferLocalAttack(DdsFakePlayerExtensionContext e){return e!=null&&state().preferLocalAttack(e.attackMode(),e.attackInterval());}
    private boolean preferLocalUse(DdsFakePlayerExtensionContext e){return e!=null&&state().preferLocalUse(e.useMode(),e.useInterval());}
    private boolean preferLocalSneaking(DdsFakePlayerExtensionContext e){return e!=null&&state().preferLocalSneaking(e.sneaking());}
    private static boolean isEnderBatch(PlayerInventoryAction action){return action==PlayerInventoryAction.ENDER_DROP_ALL||action==PlayerInventoryAction.ENDER_STORE_ALL||action==PlayerInventoryAction.ENDER_TAKE_ALL;}
    private DdsFakePlayerExtensionContext extension(){return DdsFakePlayerClientNetwork.activeExtension(menu);}
}
