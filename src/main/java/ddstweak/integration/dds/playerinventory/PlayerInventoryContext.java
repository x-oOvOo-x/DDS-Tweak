package ddstweak.integration.dds.playerinventory;

import java.util.*;

/** Neutral client description of one authoritative player-inventory menu. It contains UI metadata only, never ItemStack state. */
public final class PlayerInventoryContext {
    private final int containerId,contextSchema,layoutSchema;
    private final String source,targetName,layoutId,behaviorId;
    private final UUID targetUuid;
    private final PlayerInventoryTargetType targetType;
    private final PlayerInventoryTargetState targetState;
    private final PlayerInventoryKind inventoryKind;
    private final Set<PlayerInventoryAction> supportedActions;
    private final Set<PlayerInventoryInteractionCapability> interactionCapabilities;
    private final PlayerInventoryFallbackPolicy fallbackPolicy;

    public PlayerInventoryContext(int containerId,int contextSchema,String source,UUID targetUuid,String targetName,
                                  PlayerInventoryTargetType targetType,PlayerInventoryTargetState targetState,
                                  PlayerInventoryKind inventoryKind,String layoutId,int layoutSchema,String behaviorId,
                                  Set<PlayerInventoryAction> supportedActions,
                                  Set<PlayerInventoryInteractionCapability> interactionCapabilities,
                                  PlayerInventoryFallbackPolicy fallbackPolicy){
        if(containerId<0)throw new IllegalArgumentException("containerId");
        if(contextSchema<1||layoutSchema<1)throw new IllegalArgumentException("schema");
        this.containerId=containerId;this.contextSchema=contextSchema;this.source=bounded(source,32);this.targetUuid=targetUuid;
        this.targetName=bounded(targetName,64);this.targetType=targetType==null?PlayerInventoryTargetType.UNKNOWN:targetType;
        this.targetState=targetState==null?PlayerInventoryTargetState.UNKNOWN:targetState;
        this.inventoryKind=Objects.requireNonNull(inventoryKind);this.layoutId=bounded(layoutId,64);this.layoutSchema=layoutSchema;
        this.behaviorId=bounded(behaviorId,64);
        LinkedHashSet<PlayerInventoryAction> actions=new LinkedHashSet<>();
        if(supportedActions!=null)for(PlayerInventoryAction action:supportedActions)
            if(action!=null&&action.allowedIn(this.inventoryKind))actions.add(action);
        this.supportedActions=Collections.unmodifiableSet(actions);
        this.interactionCapabilities=Collections.unmodifiableSet(new LinkedHashSet<PlayerInventoryInteractionCapability>(interactionCapabilities==null?Collections.<PlayerInventoryInteractionCapability>emptySet():interactionCapabilities));
        this.fallbackPolicy=Objects.requireNonNull(fallbackPolicy);
    }

    public int containerId(){return containerId;}public int contextSchema(){return contextSchema;}public String source(){return source;}
    public UUID targetUuid(){return targetUuid;}public String targetName(){return targetName;}public PlayerInventoryTargetType targetType(){return targetType;}
    public PlayerInventoryTargetState targetState(){return targetState;}public PlayerInventoryKind inventoryKind(){return inventoryKind;}
    public String layoutId(){return layoutId;}public int layoutSchema(){return layoutSchema;}public String behaviorId(){return behaviorId;}
    public Set<PlayerInventoryAction> supportedActions(){return supportedActions;}
    public Set<PlayerInventoryInteractionCapability> interactionCapabilities(){return interactionCapabilities;}
    public PlayerInventoryFallbackPolicy fallbackPolicy(){return fallbackPolicy;}

    private static String bounded(String v,int max){return v==null||v.length()>max?"":v;}
    public boolean supports(PlayerInventoryAction action){return action!=null&&action.allowedIn(inventoryKind)&&supportedActions.contains(action);}
    public boolean allows(PlayerInventoryInteractionCapability capability){return capability!=null&&interactionCapabilities.contains(capability);}

    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof PlayerInventoryContext))return false;PlayerInventoryContext c=(PlayerInventoryContext)o;return containerId==c.containerId&&contextSchema==c.contextSchema&&layoutSchema==c.layoutSchema&&Objects.equals(source,c.source)&&Objects.equals(targetUuid,c.targetUuid)&&Objects.equals(targetName,c.targetName)&&targetType==c.targetType&&targetState==c.targetState&&inventoryKind==c.inventoryKind&&Objects.equals(layoutId,c.layoutId)&&Objects.equals(behaviorId,c.behaviorId)&&Objects.equals(supportedActions,c.supportedActions)&&Objects.equals(interactionCapabilities,c.interactionCapabilities)&&fallbackPolicy==c.fallbackPolicy;}
    @Override public int hashCode(){return Objects.hash(containerId,contextSchema,source,targetUuid,targetName,targetType,targetState,inventoryKind,layoutId,layoutSchema,behaviorId,supportedActions,interactionCapabilities,fallbackPolicy);}
}