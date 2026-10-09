package ddstweak.integration.dds.fakeplayer;

import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.legacy.DdsFakePlayerLegacyContextAdapter;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.MenuType;
import java.util.*;

public final class DdsFakePlayerExtensionContext {
    private static final int MAX_NAME=64,MAX_ACTIONS_TEXT=512,MAX_ACTION_TOKENS=32,MAX_ACTION_TOKEN=64;
    private final int containerId,contextSchema,layoutSchema,selectedHotbar,attackMode,attackInterval,useMode,useInterval,canonicalSize;
    private final long sessionGeneration,stateRevision;
    private final String provider,layoutId,behaviorId,targetName;
    private final PlayerInventoryTargetType targetType;
    private final PlayerInventoryTargetState targetState;
    private final PlayerInventoryKind inventoryKind;
    private final UUID targetUuid;
    private final Set<PlayerInventoryAction> supportedActions;
    private final boolean sneaking,carriedScratch,returnParent,authoritativeState;

    public DdsFakePlayerExtensionContext(int containerId,int contextSchema,String provider,PlayerInventoryTargetType targetType,
                                         PlayerInventoryTargetState targetState,PlayerInventoryKind inventoryKind,
                                         String layoutId,int layoutSchema,String behaviorId,UUID targetUuid,String targetName,
                                         Set<PlayerInventoryAction> supportedActions,int selectedHotbar,int attackMode,
                                         int attackInterval,int useMode,int useInterval,boolean sneaking,boolean carriedScratch){
        this(containerId,contextSchema,provider,targetType,targetState,inventoryKind,layoutId,layoutSchema,behaviorId,targetUuid,targetName,
                supportedActions,selectedHotbar,attackMode,attackInterval,useMode,useInterval,sneaking,carriedScratch,0);
    }

    public DdsFakePlayerExtensionContext(int containerId,int contextSchema,String provider,PlayerInventoryTargetType targetType,
                                         PlayerInventoryTargetState targetState,PlayerInventoryKind inventoryKind,
                                         String layoutId,int layoutSchema,String behaviorId,UUID targetUuid,String targetName,
                                         Set<PlayerInventoryAction> supportedActions,int selectedHotbar,int attackMode,
                                         int attackInterval,int useMode,int useInterval,boolean sneaking,boolean carriedScratch,int canonicalSize){
        this(containerId,contextSchema,provider,targetType,targetState,inventoryKind,layoutId,layoutSchema,behaviorId,targetUuid,targetName,
                supportedActions,selectedHotbar,attackMode,attackInterval,useMode,useInterval,sneaking,carriedScratch,false,canonicalSize,
                0L,0L,contextSchema==1&&targetType==PlayerInventoryTargetType.FAKE);
    }

    private DdsFakePlayerExtensionContext(int containerId,int contextSchema,String provider,PlayerInventoryTargetType targetType,
                                          PlayerInventoryTargetState targetState,PlayerInventoryKind inventoryKind,
                                          String layoutId,int layoutSchema,String behaviorId,UUID targetUuid,String targetName,
                                          Set<PlayerInventoryAction> supportedActions,int selectedHotbar,int attackMode,
                                          int attackInterval,int useMode,int useInterval,boolean sneaking,boolean carriedScratch,
                                          boolean returnParent,int canonicalSize,long sessionGeneration,long stateRevision,boolean authoritativeState){
        this.containerId=containerId;this.contextSchema=contextSchema;this.provider=bounded(provider,32);
        this.targetType=targetType==null?PlayerInventoryTargetType.UNKNOWN:targetType;
        this.targetState=targetState==null?PlayerInventoryTargetState.UNKNOWN:targetState;
        this.inventoryKind=inventoryKind==null?PlayerInventoryKind.INVENTORY:inventoryKind;
        this.layoutId=bounded(layoutId,64);this.layoutSchema=layoutSchema;this.behaviorId=bounded(behaviorId,64);
        this.targetUuid=targetUuid;this.targetName=targetName==null?"":targetName;
        boolean actionAuthority=contextSchema==1||contextSchema==3&&this.targetType==PlayerInventoryTargetType.FAKE;
        Set<PlayerInventoryAction> source=actionAuthority&&supportedActions!=null?supportedActions:Collections.<PlayerInventoryAction>emptySet();
        LinkedHashSet<PlayerInventoryAction> actions=new LinkedHashSet<>();
        for(PlayerInventoryAction action:source)if(action!=null&&action.allowedIn(this.inventoryKind))actions.add(action);
        this.supportedActions=Collections.unmodifiableSet(actions);
        this.selectedHotbar=Math.max(0,Math.min(8,selectedHotbar));
        this.attackMode=mode(attackMode);this.useMode=mode(useMode);
        this.attackInterval=PlayerInventoryBehaviorV1.clampInterval(attackInterval);
        this.useInterval=PlayerInventoryBehaviorV1.clampInterval(useInterval);
        this.sneaking=sneaking;this.carriedScratch=carriedScratch;this.returnParent=returnParent;this.canonicalSize=canonicalSize==27||canonicalSize==54?canonicalSize:0;
        this.sessionGeneration=Math.max(0L,sessionGeneration);this.stateRevision=Math.max(0L,stateRevision);
        this.authoritativeState=authoritativeState;
    }

    public int containerId(){return containerId;}public int contextSchema(){return contextSchema;}public String provider(){return provider;}
    public PlayerInventoryTargetType targetType(){return targetType;}public PlayerInventoryTargetState targetState(){return targetState;}
    public PlayerInventoryKind inventoryKind(){return inventoryKind;}public String layoutId(){return layoutId;}public int layoutSchema(){return layoutSchema;}
    public String behaviorId(){return behaviorId;}public UUID targetUuid(){return targetUuid;}public String targetName(){return targetName;}
    public Set<PlayerInventoryAction> supportedActions(){return supportedActions;}public int selectedHotbar(){return selectedHotbar;}
    public int attackMode(){return attackMode;}public int attackInterval(){return attackInterval;}public int useMode(){return useMode;}
    public int useInterval(){return useInterval;}public boolean sneaking(){return sneaking;}public boolean carriedScratch(){return carriedScratch;}public boolean returnParent(){return returnParent;}
    public int canonicalSize(){return canonicalSize;}public long sessionGeneration(){return sessionGeneration;}public long stateRevision(){return stateRevision;}
    public boolean strictSession(){return contextSchema==3&&sessionGeneration>0L;}public boolean authoritativeState(){return authoritativeState;}

    public static boolean isClear(CompoundTag tag){
        return tag!=null&&MinecraftCompat.intOr(tag,"context_schema",0)==3
                &&"clear".equals(MinecraftCompat.stringOr(tag,"event",""))
                &&MinecraftCompat.intOr(tag,"container_id",-1)>=0
                &&MinecraftCompat.longOr(tag,"session_generation",0L)>0L;
    }

    public static DdsFakePlayerExtensionContext parseContext(CompoundTag tag){
        if(tag==null)return null;
        int schema=MinecraftCompat.intOr(tag,"context_schema",0);if(schema!=1&&schema!=2&&schema!=3)return null;
        if(schema==3&&!"open".equals(MinecraftCompat.stringOr(tag,"event","")))return null;
        int id=MinecraftCompat.intOr(tag,"container_id",-1);if(id<0)return null;
        long generation=schema==3?MinecraftCompat.longOr(tag,"session_generation",0L):0L;if(schema==3&&generation<=0L)return null;
        String uuidText=bounded(MinecraftCompat.stringOr(tag,"target_uuid",""),36),name=bounded(MinecraftCompat.stringOr(tag,"target_name",""),MAX_NAME);
        UUID uuid;try{uuid=UUID.fromString(uuidText);}catch(IllegalArgumentException ignored){return null;}
        String provider=bounded(MinecraftCompat.stringOr(tag,"provider",MinecraftCompat.stringOr(tag,"source","")),32);if(provider.trim().isEmpty())return null;
        String targetWire=MinecraftCompat.stringOr(tag,"target_type","");
        PlayerInventoryTargetType targetType;
        PlayerInventoryTargetState targetState;
        if("fake".equals(targetWire)){targetType=PlayerInventoryTargetType.FAKE;targetState=PlayerInventoryTargetState.ONLINE;}
        else if("live_player".equals(targetWire)){targetType=PlayerInventoryTargetType.REAL;targetState=PlayerInventoryTargetState.ONLINE;}
        else if("offline_player".equals(targetWire)){targetType=PlayerInventoryTargetType.REAL;targetState=PlayerInventoryTargetState.OFFLINE;}
        else return null;
        if(schema==1&&targetType!=PlayerInventoryTargetType.FAKE||schema==2&&targetType!=PlayerInventoryTargetType.REAL)return null;
        PlayerInventoryKind kind=parseKind(MinecraftCompat.stringOr(tag,"inventory_kind","inventory"));
        String layout=bounded(MinecraftCompat.stringOr(tag,"layout_id",""),64);
        int layoutSchema=MinecraftCompat.intOr(tag,"layout_schema",layout.trim().isEmpty()?1:0);
        String behavior=bounded(MinecraftCompat.stringOr(tag,"behavior_id",schema==1?PlayerInventoryBehaviorV1.ID:""),64);
        if((schema==2||schema==3)&&(layout.trim().isEmpty()||layoutSchema!=1||behavior.trim().isEmpty()))return null;
        if(!layout.trim().isEmpty()&&layoutSchema!=1)return null;
        EnumSet<PlayerInventoryAction> actions=EnumSet.noneOf(PlayerInventoryAction.class);
        if(schema==1||schema==3&&targetType==PlayerInventoryTargetType.FAKE)
            parseActions(MinecraftCompat.stringOr(tag,"supported_actions",""),actions);
        long revision=schema==3?MinecraftCompat.longOr(tag,"state_revision",0L):0L;
        boolean authoritative=targetType==PlayerInventoryTargetType.FAKE&&(schema==1||schema==3&&revision>0L);
        if(schema==3&&targetType==PlayerInventoryTargetType.FAKE&&revision<=0L)return null;
        return new DdsFakePlayerExtensionContext(id,schema,provider,targetType,targetState,kind,layout,layoutSchema,behavior,uuid,name,actions,
                MinecraftCompat.intOr(tag,"selected_hotbar",0),
                MinecraftCompat.intOr(tag,"attack_mode",PlayerInventoryBehaviorV1.MODE_NONE),
                MinecraftCompat.intOr(tag,"attack_interval",PlayerInventoryBehaviorV1.DEFAULT_INTERVAL),
                MinecraftCompat.intOr(tag,"use_mode",PlayerInventoryBehaviorV1.MODE_NONE),
                MinecraftCompat.intOr(tag,"use_interval",PlayerInventoryBehaviorV1.DEFAULT_INTERVAL),
                MinecraftCompat.intOr(tag,"sneaking",0)!=0,
                hasCapability(MinecraftCompat.stringOr(tag,"interaction_capabilities",""),"carried_scratch_swap"),
                hasCapability(MinecraftCompat.stringOr(tag,"interaction_capabilities",""),"return_parent"),
                MinecraftCompat.intOr(tag,"canonical_size",0),generation,revision,authoritative);
    }

    public DdsFakePlayerExtensionContext applyState(CompoundTag tag){
        if(targetType!=PlayerInventoryTargetType.FAKE||tag==null||MinecraftCompat.intOr(tag,"container_id",-1)!=containerId)return this;
        String uuidText=bounded(MinecraftCompat.stringOr(tag,"target_uuid",""),36);
        if(targetUuid==null||!targetUuid.toString().equals(uuidText))return this;
        int stateSchema=MinecraftCompat.intOr(tag,"state_schema",0);
        long revision=stateRevision;
        if(contextSchema==1){
            if(stateSchema!=1)return this;
        }else if(strictSession()){
            if(stateSchema!=2||MinecraftCompat.longOr(tag,"session_generation",0L)!=sessionGeneration)return this;
            long incoming=MinecraftCompat.longOr(tag,"state_revision",0L);if(incoming<=stateRevision)return this;revision=incoming;
        }else return this;
        return new DdsFakePlayerExtensionContext(containerId,contextSchema,provider,targetType,targetState,inventoryKind,layoutId,layoutSchema,
                behaviorId,targetUuid,targetName,supportedActions,MinecraftCompat.intOr(tag,"selected_hotbar",selectedHotbar),
                MinecraftCompat.intOr(tag,"attack_mode",attackMode),MinecraftCompat.intOr(tag,"attack_interval",attackInterval),
                MinecraftCompat.intOr(tag,"use_mode",useMode),MinecraftCompat.intOr(tag,"use_interval",useInterval),
                MinecraftCompat.intOr(tag,"sneaking",sneaking?1:0)!=0,carriedScratch,returnParent,canonicalSize,sessionGeneration,revision,true);
    }

    public PlayerInventoryContext standalone(MenuType<?> type){
        String layout=resolvedLayout(type);if(layout==null)return null;
        EnumSet<PlayerInventoryInteractionCapability> capabilities=EnumSet.noneOf(PlayerInventoryInteractionCapability.class);
        if(carriedScratch)capabilities.add(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP);
        if(returnParent)capabilities.add(PlayerInventoryInteractionCapability.RETURN_PARENT);
        PlayerInventoryContext context=new PlayerInventoryContext(containerId,contextSchema,provider,targetUuid,targetName,targetType,targetState,inventoryKind,
                layout,1,behaviorId.trim().isEmpty()?PlayerInventoryBehaviorV1.ID:behaviorId,supportedActions,capabilities,
                PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
        boolean inventory=PlayerInventoryLayouts.isInventory(context),controller=PlayerInventoryLayouts.isGcaController(context);
        boolean gcaEnder=PlayerInventoryLayouts.isGcaEnder(context);
        int rows=PlayerInventoryLayouts.enderRows(context);
        if(inventory?type!=MenuType.GENERIC_9x6:controller?type!=MenuType.GENERIC_9x3:gcaEnder?type!=MenuType.GENERIC_9x6:rows==3?type!=MenuType.GENERIC_9x3:rows!=6||type!=MenuType.GENERIC_9x6)return null;
        return context;
    }

    public boolean providerMatches(PlayerInventoryContext base){
        return base!=null&&provider.equals(base.source())&&(layoutId.trim().isEmpty()||layoutId.equals(base.layoutId()));
    }

    public PlayerInventoryContext merge(PlayerInventoryContext base){
        if(base==null||base.containerId()!=containerId||!providerMatches(base))return base;
        EnumSet<PlayerInventoryAction> actions=EnumSet.noneOf(PlayerInventoryAction.class);
        if(targetType==PlayerInventoryTargetType.FAKE)for(PlayerInventoryAction action:base.supportedActions())
            if(action.allowedIn(base.inventoryKind()))actions.add(action);
        for(PlayerInventoryAction action:supportedActions)if(action.allowedIn(base.inventoryKind()))actions.add(action);
        EnumSet<PlayerInventoryInteractionCapability> capabilities=EnumSet.noneOf(PlayerInventoryInteractionCapability.class);
        capabilities.addAll(base.interactionCapabilities());
        if(carriedScratch)capabilities.add(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP);
        if(returnParent)capabilities.add(PlayerInventoryInteractionCapability.RETURN_PARENT);
        return new PlayerInventoryContext(base.containerId(),contextSchema,base.source(),targetUuid,
                targetName.trim().isEmpty()?base.targetName():targetName,targetType,targetState,base.inventoryKind(),base.layoutId(),base.layoutSchema(),
                behaviorId.trim().isEmpty()?base.behaviorId():behaviorId,actions,capabilities,base.fallbackPolicy());
    }

    public boolean supports(PlayerInventoryAction action){return action!=null&&action.allowedIn(inventoryKind)&&supportedActions.contains(action);}
    private String resolvedLayout(MenuType<?> type){
        if(!layoutId.trim().isEmpty())return PlayerInventoryLayouts.supportedLayout(layoutId,inventoryKind)?layoutId:null;
        if(contextSchema==1&&inventoryKind==PlayerInventoryKind.ENDER_CHEST&&"dds_qca".equals(provider))
            return type==MenuType.GENERIC_9x3?PlayerInventoryLayouts.ENDER_3_V1:type==MenuType.GENERIC_9x6?PlayerInventoryLayouts.ENDER_6_V1:null;
        return null;
    }
    private static void parseActions(String raw,EnumSet<PlayerInventoryAction> out){
        if(raw==null||raw.trim().isEmpty()||raw.length()>MAX_ACTIONS_TEXT)return;
        String[] tokens=raw.split(",",MAX_ACTION_TOKENS+1);if(tokens.length>MAX_ACTION_TOKENS)return;
        for(String token:tokens){
            String wire=token.trim();if(wire.isEmpty()||wire.length()>MAX_ACTION_TOKEN)continue;
            for(DdsFakePlayerAction legacy:DdsFakePlayerAction.values())if(legacy.wireName().equals(wire)){
                PlayerInventoryAction mapped=DdsFakePlayerLegacyContextAdapter.action(legacy);if(mapped!=null)out.add(mapped);break;
            }
        }
    }
    private static boolean hasCapability(String raw,String expected){if(raw==null||raw.trim().isEmpty())return false;for(String token:raw.split(","))if(expected.equals(token.trim()))return true;return false;}
    private static PlayerInventoryKind parseKind(String raw){return "ender_chest".equals(raw)?PlayerInventoryKind.ENDER_CHEST:"controller".equals(raw)?PlayerInventoryKind.CONTROLLER:PlayerInventoryKind.INVENTORY;}
    private static int mode(int value){return value==PlayerInventoryBehaviorV1.MODE_INTERVAL||value==PlayerInventoryBehaviorV1.MODE_CONTINUOUS?value:PlayerInventoryBehaviorV1.MODE_NONE;}
    private static String bounded(String value,int max){return value==null||value.length()>max?"":value;}

    @Override public boolean equals(Object o){
        if(this==o)return true;if(!(o instanceof DdsFakePlayerExtensionContext))return false;DdsFakePlayerExtensionContext c=(DdsFakePlayerExtensionContext)o;
        return containerId==c.containerId&&contextSchema==c.contextSchema&&layoutSchema==c.layoutSchema&&selectedHotbar==c.selectedHotbar&&attackMode==c.attackMode&&attackInterval==c.attackInterval&&useMode==c.useMode&&useInterval==c.useInterval&&canonicalSize==c.canonicalSize&&sessionGeneration==c.sessionGeneration&&stateRevision==c.stateRevision&&sneaking==c.sneaking&&carriedScratch==c.carriedScratch&&returnParent==c.returnParent&&authoritativeState==c.authoritativeState&&Objects.equals(provider,c.provider)&&targetType==c.targetType&&targetState==c.targetState&&inventoryKind==c.inventoryKind&&Objects.equals(layoutId,c.layoutId)&&Objects.equals(behaviorId,c.behaviorId)&&Objects.equals(targetUuid,c.targetUuid)&&Objects.equals(targetName,c.targetName)&&Objects.equals(supportedActions,c.supportedActions);
    }
    @Override public int hashCode(){return Objects.hash(containerId,contextSchema,provider,targetType,targetState,inventoryKind,layoutId,layoutSchema,behaviorId,targetUuid,targetName,supportedActions,selectedHotbar,attackMode,attackInterval,useMode,useInterval,canonicalSize,sessionGeneration,stateRevision,sneaking,carriedScratch,returnParent,authoritativeState);}
}