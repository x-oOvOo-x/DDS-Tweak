package ddstweak.integration.dds.playerinventory;

import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import net.minecraft.world.entity.player.Player;
//#if MC >= 260102
//$$ import net.minecraft.world.inventory.ContainerInput;
//#else
import net.minecraft.world.inventory.ClickType;
//#endif
import java.util.*;
import java.util.function.Supplier;

/**
 * Late-bound action route chain. Preferred protocol routes are registered independently;
 * provider bindings stay provider-neutral and are always the final fallback.
 */
public final class PlayerInventoryActionBindingRouter implements PlayerInventoryActionBinding{
    private static final List<Supplier<PlayerInventoryActionBinding>> PREFERRED=new ArrayList<>();
    private final List<PlayerInventoryActionBinding> routes;

    private PlayerInventoryActionBindingRouter(List<PlayerInventoryActionBinding> routes){
        this.routes=Collections.unmodifiableList(routes);
    }

    public static synchronized void registerPreferred(Supplier<PlayerInventoryActionBinding> factory){
        PREFERRED.add(Objects.requireNonNull(factory,"factory"));
    }

    public static PlayerInventoryActionBinding compose(PlayerInventoryActionBinding fallback){
        if(fallback==null)return null;
        List<Supplier<PlayerInventoryActionBinding>> factories;
        synchronized(PlayerInventoryActionBindingRouter.class){factories=new ArrayList<>(PREFERRED);}
        if(factories.isEmpty())return fallback;

        List<PlayerInventoryActionBinding> routes=new ArrayList<>();
        for(Supplier<PlayerInventoryActionBinding> factory:factories){
            PlayerInventoryActionBinding route=factory.get();
            if(route!=null)routes.add(route);
        }
        routes.add(fallback);
        return routes.size()==1?fallback:new PlayerInventoryActionBindingRouter(routes);
    }

    @Override public void attach(UnifiedPlayerInventoryMirrorMenu menu){for(PlayerInventoryActionBinding route:routes)route.attach(menu);}
    @Override public boolean canSend(){for(PlayerInventoryActionBinding route:routes)if(route.canSend())return true;return false;}
    @Override public boolean supports(PlayerInventoryAction action){return route(action)!=null;}
    @Override public boolean send(PlayerInventoryAction action,int value){
        PlayerInventoryActionBinding route=route(action);
        return route!=null&&route.send(action,value);
    }
    @Override public int dataSlotCount(PlayerInventoryContext context){
        int count=0;for(PlayerInventoryActionBinding route:routes)count=Math.max(count,route.dataSlotCount(context));return count;
    }
    //#if MC >= 260102
    //$$ @Override public boolean handleControlClick(int slotId,int button,ContainerInput type,Player player){
    //$$     for(PlayerInventoryActionBinding route:routes)if(route.handleControlClick(slotId,button,type,player))return true;
    //$$     return false;
    //$$ }
    //#else
    @Override public boolean handleControlClick(int slotId,int button,ClickType type,Player player){
        for(PlayerInventoryActionBinding route:routes)if(route.handleControlClick(slotId,button,type,player))return true;
        return false;
    }
    //#endif

    @Override public int selectedTargetHotbarSlot(){PlayerInventoryActionBinding route=stateRoute(State.SELECTED);return route==null?-1:route.selectedTargetHotbarSlot();}
    @Override public int attackMode(){PlayerInventoryActionBinding route=stateRoute(State.ATTACK_MODE);return route==null?PlayerInventoryBehaviorV1.MODE_NONE:route.attackMode();}
    @Override public int attackInterval(){PlayerInventoryActionBinding route=stateRoute(State.ATTACK_INTERVAL);return route==null?PlayerInventoryBehaviorV1.DEFAULT_INTERVAL:route.attackInterval();}
    @Override public int useMode(){PlayerInventoryActionBinding route=stateRoute(State.USE_MODE);return route==null?PlayerInventoryBehaviorV1.MODE_NONE:route.useMode();}
    @Override public int useInterval(){PlayerInventoryActionBinding route=stateRoute(State.USE_INTERVAL);return route==null?PlayerInventoryBehaviorV1.DEFAULT_INTERVAL:route.useInterval();}
    @Override public boolean sneaking(){PlayerInventoryActionBinding route=stateRoute(State.SNEAKING);return route!=null&&route.sneaking();}

    @Override public PlayerInventoryStateConfidence selectedHotbarConfidence(){return confidence(State.SELECTED);}
    @Override public PlayerInventoryStateConfidence attackModeConfidence(){return confidence(State.ATTACK_MODE);}
    @Override public PlayerInventoryStateConfidence attackIntervalConfidence(){return confidence(State.ATTACK_INTERVAL);}
    @Override public PlayerInventoryStateConfidence useModeConfidence(){return confidence(State.USE_MODE);}
    @Override public PlayerInventoryStateConfidence useIntervalConfidence(){return confidence(State.USE_INTERVAL);}
    @Override public PlayerInventoryStateConfidence sneakingConfidence(){return confidence(State.SNEAKING);}

    private PlayerInventoryActionBinding route(PlayerInventoryAction action){
        if(action==null)return null;
        for(PlayerInventoryActionBinding route:routes)if(route.supports(action))return route;
        return null;
    }
    private PlayerInventoryActionBinding stateRoute(State state){
        PlayerInventoryActionBinding best=null;
        PlayerInventoryStateConfidence bestConfidence=PlayerInventoryStateConfidence.UNKNOWN;
        for(PlayerInventoryActionBinding route:routes){
            PlayerInventoryStateConfidence current=state.confidence(route);
            if(current.outranks(bestConfidence)){
                best=route;bestConfidence=current;
                if(current==PlayerInventoryStateConfidence.AUTHORITATIVE)break;
            }
        }
        return best;
    }
    private PlayerInventoryStateConfidence confidence(State state){
        PlayerInventoryActionBinding route=stateRoute(state);
        return route==null?PlayerInventoryStateConfidence.UNKNOWN:state.confidence(route);
    }

    private enum State{
        SELECTED{PlayerInventoryStateConfidence confidence(PlayerInventoryActionBinding b){return b.selectedHotbarConfidence();}},
        ATTACK_MODE{PlayerInventoryStateConfidence confidence(PlayerInventoryActionBinding b){return b.attackModeConfidence();}},
        ATTACK_INTERVAL{PlayerInventoryStateConfidence confidence(PlayerInventoryActionBinding b){return b.attackIntervalConfidence();}},
        USE_MODE{PlayerInventoryStateConfidence confidence(PlayerInventoryActionBinding b){return b.useModeConfidence();}},
        USE_INTERVAL{PlayerInventoryStateConfidence confidence(PlayerInventoryActionBinding b){return b.useIntervalConfidence();}},
        SNEAKING{PlayerInventoryStateConfidence confidence(PlayerInventoryActionBinding b){return b.sneakingConfidence();}};
        abstract PlayerInventoryStateConfidence confidence(PlayerInventoryActionBinding binding);
    }
}
