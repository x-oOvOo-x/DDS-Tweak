package ddstweak.playerinventory;

import ddstweak.integration.dds.playerinventory.*;
import java.util.*;

/**
 * Client-session memory for fake-player controls.
 * It preserves the most recent local request per target across screen reopenings while
 * bounded pending windows prevent an older authoritative snapshot from flashing back.
 */
public final class PlayerInventoryClientActionState {
    private static final long PENDING_NANOS=2_000_000_000L;
    private static final Map<String,State> STATES=new HashMap<>(),BY_NAME=new HashMap<>();
    private static final Map<String,UUID> UUID_BY_NAME=new HashMap<>();
    private PlayerInventoryClientActionState(){}

    public static synchronized State forContext(PlayerInventoryContext context){
        Objects.requireNonNull(context);
        String name=normalizeName(context.targetName());
        UUID uuid=context.targetUuid();
        String key=uuid!=null?"uuid:"+uuid:"name:"+name;
        State state=STATES.get(key);
        if(state==null&&!name.isEmpty()){
            UUID bound=UUID_BY_NAME.get(name);
            if(uuid==null||bound==null||uuid.equals(bound))state=BY_NAME.get(name);
        }
        if(state==null)state=new State();
        STATES.put(key,state);
        if(!name.isEmpty()){
            BY_NAME.put(name,state);
            if(uuid!=null)UUID_BY_NAME.put(name,uuid);
        }
        return state;
    }

    /** Used only for commands sent by this client; it never claims server authority. */
    public static synchronized State forTargetName(String targetName){
        String name=normalizeName(targetName);
        if(name.isEmpty())return null;
        State state=BY_NAME.get(name);
        if(state==null)state=STATES.computeIfAbsent("name:"+name,ignored->new State());
        BY_NAME.put(name,state);
        return state;
    }

    public static synchronized void reset(){STATES.clear();BY_NAME.clear();UUID_BY_NAME.clear();}
    private static String normalizeName(String value){return value==null?"":value.trim().toLowerCase(Locale.ROOT);}

    public static final class State {
        private int selected=-1;
        private int attackMode=PlayerInventoryBehaviorV1.MODE_NONE,attackInterval=PlayerInventoryBehaviorV1.DEFAULT_INTERVAL;
        private int useMode=PlayerInventoryBehaviorV1.MODE_NONE,useInterval=PlayerInventoryBehaviorV1.DEFAULT_INTERVAL;
        private boolean selectedKnown,attackKnown,useKnown,sneakingKnown,sneaking;
        private long selectedPendingUntil,attackPendingUntil,usePendingUntil,sneakPendingUntil;

        public int selected(){return selected;}
        public int attackMode(){return attackMode;}
        public int attackInterval(){return attackInterval;}
        public int useMode(){return useMode;}
        public int useInterval(){return useInterval;}
        public boolean sneaking(){return sneaking;}
        public boolean selectedKnown(){return selectedKnown;}
        public boolean attackKnown(){return attackKnown;}
        public boolean useKnown(){return useKnown;}
        public boolean sneakingKnown(){return sneakingKnown;}
        public void rememberAttackInterval(int value){attackInterval=PlayerInventoryBehaviorV1.clampInterval(value);}
        public void rememberUseInterval(int value){useInterval=PlayerInventoryBehaviorV1.clampInterval(value);}

        public void observeAuthoritative(int selectedHotbar,int attackMode,int attackInterval,int useMode,int useInterval,boolean sneaking){
            preferLocalSelected(selectedHotbar);
            preferLocalAttack(attackMode,attackInterval);
            preferLocalUse(useMode,useInterval);
            preferLocalSneaking(sneaking);
        }

        public boolean preferLocalSelected(int authoritativeSelected){
            int value=Math.max(0,Math.min(8,authoritativeSelected));
            if(selectedPendingUntil==0L){selected=value;selectedKnown=true;return false;}
            long now=System.nanoTime();
            if(value==selected||now>=selectedPendingUntil){selected=value;selectedKnown=true;selectedPendingUntil=0L;return false;}
            return true;
        }
        public boolean preferLocalAttack(int authoritativeMode,int authoritativeInterval){return preferLocalAction(true,authoritativeMode,authoritativeInterval,System.nanoTime());}
        public boolean preferLocalUse(int authoritativeMode,int authoritativeInterval){return preferLocalAction(false,authoritativeMode,authoritativeInterval,System.nanoTime());}
        public boolean preferLocalSneaking(boolean authoritativeSneaking){
            if(sneakPendingUntil==0L){sneaking=authoritativeSneaking;sneakingKnown=true;return false;}
            long now=System.nanoTime();
            if(authoritativeSneaking==sneaking||now>=sneakPendingUntil){sneaking=authoritativeSneaking;sneakingKnown=true;sneakPendingUntil=0L;return false;}
            return true;
        }
        private boolean preferLocalAction(boolean attack,int authoritativeMode,int authoritativeInterval,long now){
            long until=attack?attackPendingUntil:usePendingUntil;
            int mode=normalizeMode(authoritativeMode),interval=PlayerInventoryBehaviorV1.clampInterval(authoritativeInterval);
            int localMode=attack?attackMode:useMode,localInterval=attack?attackInterval:useInterval;
            boolean matched=mode==localMode&&(localMode!=PlayerInventoryBehaviorV1.MODE_INTERVAL||interval==localInterval);
            if(until!=0L&&!matched&&now<until)return true;
            if(attack){attackMode=mode;attackInterval=interval;attackKnown=true;attackPendingUntil=0L;}
            else{useMode=mode;useInterval=interval;useKnown=true;usePendingUntil=0L;}
            return false;
        }
        private static int normalizeMode(int mode){
            return mode==PlayerInventoryBehaviorV1.MODE_INTERVAL||mode==PlayerInventoryBehaviorV1.MODE_CONTINUOUS
                    ?mode:PlayerInventoryBehaviorV1.MODE_NONE;
        }
        private static long pendingUntil(){return System.nanoTime()+PENDING_NANOS;}
        private void markSelectedPending(){selectedPendingUntil=pendingUntil();}
        private void markAttackPending(){attackPendingUntil=pendingUntil();}
        private void markUsePending(){usePendingUntil=pendingUntil();}
        private void markSneakPending(){sneakPendingUntil=pendingUntil();}

        public void record(PlayerInventoryAction action,int value){
            if(action==null)return;
            switch(action){
                case SELECT_HOTBAR:selected=Math.max(0,Math.min(8,value-1));selectedKnown=true;markSelectedPending();break;
                case ATTACK_INTERVAL:attackMode=PlayerInventoryBehaviorV1.MODE_INTERVAL;attackInterval=PlayerInventoryBehaviorV1.clampInterval(value);attackKnown=true;markAttackPending();break;
                case ATTACK_CONTINUOUS:attackMode=PlayerInventoryBehaviorV1.MODE_CONTINUOUS;attackKnown=true;markAttackPending();break;
                case ATTACK_ONCE:
                case STOP_ATTACK:attackMode=PlayerInventoryBehaviorV1.MODE_NONE;attackKnown=true;markAttackPending();break;
                case USE_INTERVAL:useMode=PlayerInventoryBehaviorV1.MODE_INTERVAL;useInterval=PlayerInventoryBehaviorV1.clampInterval(value);useKnown=true;markUsePending();break;
                case USE_CONTINUOUS:useMode=PlayerInventoryBehaviorV1.MODE_CONTINUOUS;useKnown=true;markUsePending();break;
                case USE_ONCE:
                case STOP_USE:useMode=PlayerInventoryBehaviorV1.MODE_NONE;useKnown=true;markUsePending();break;
                case STOP_ALL:attackMode=useMode=PlayerInventoryBehaviorV1.MODE_NONE;attackKnown=useKnown=true;markAttackPending();markUsePending();break;
                case TOGGLE_SNEAK:sneaking=value!=0;sneakingKnown=true;markSneakPending();break;
                default:break;
            }
        }
    }
}
