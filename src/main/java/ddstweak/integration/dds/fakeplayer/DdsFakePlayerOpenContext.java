package ddstweak.integration.dds.fakeplayer;

import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Backward-compatible metadata carried by fake_player_open_menu_s2c. */
public final class DdsFakePlayerOpenContext {
    private static final int MAX_NAME=64,MAX_ACTIONS_TEXT=512,MAX_ACTION_TOKENS=32,MAX_ACTION_TOKEN=64;
    private final int containerId,layout,menuSchema;
    private final UUID targetUuid;
    private final String targetName;
    private final boolean hasExplicitSupportedActions,mirrorCompatible;
    private final Set<DdsFakePlayerAction> supportedActions;

    public DdsFakePlayerOpenContext(int containerId,int layout,int menuSchema,UUID targetUuid,String targetName,
                                    boolean hasExplicitSupportedActions,Set<DdsFakePlayerAction> supportedActions,
                                    boolean mirrorCompatible){
        this.containerId=containerId;this.layout=layout;this.menuSchema=menuSchema;this.targetUuid=targetUuid;
        this.targetName=targetName==null?"":targetName;
        Set<DdsFakePlayerAction> copy=supportedActions==null?Collections.<DdsFakePlayerAction>emptySet():new LinkedHashSet<DdsFakePlayerAction>(supportedActions);
        this.supportedActions=Collections.unmodifiableSet(copy);
        this.hasExplicitSupportedActions=hasExplicitSupportedActions;this.mirrorCompatible=mirrorCompatible;
    }

    public int containerId(){return containerId;}public int layout(){return layout;}public int menuSchema(){return menuSchema;}
    public UUID targetUuid(){return targetUuid;}public String targetName(){return targetName;}
    public boolean hasExplicitSupportedActions(){return hasExplicitSupportedActions;}
    public Set<DdsFakePlayerAction> supportedActions(){return supportedActions;}
    public boolean mirrorCompatible(){return mirrorCompatible;}

    public static DdsFakePlayerOpenContext parse(CompoundTag tag){
        if(tag==null)return null;int id=MinecraftCompat.intOr(tag,"container_id",-1);if(id<0)return null;int layout=MinecraftCompat.intOr(tag,"layout",DdsFakePlayerPackets.MENU_NONE),schema=MinecraftCompat.intOr(tag,"menu_schema",0);boolean compatible=(schema==0||schema==1)&&DdsFakePlayerPackets.isMenuLayout(layout);
        String uuidText=bounded(MinecraftCompat.stringOr(tag,"target_uuid",""),36),name=bounded(MinecraftCompat.stringOr(tag,"target_name",""),MAX_NAME);UUID uuid=null;if(!uuidText.trim().isEmpty())try{uuid=UUID.fromString(uuidText);}catch(IllegalArgumentException ignored){}
        boolean explicit=tag.contains("supported_actions");EnumSet<DdsFakePlayerAction> actions=EnumSet.noneOf(DdsFakePlayerAction.class);
        if(compatible){if(explicit)parseActions(MinecraftCompat.stringOr(tag,"supported_actions",""),actions);else actions.addAll(Arrays.asList(DdsFakePlayerAction.values()));}
        return new DdsFakePlayerOpenContext(id,layout,schema,uuid,name,explicit,actions,compatible);
    }
    private static void parseActions(String raw,EnumSet<DdsFakePlayerAction> out){if(raw==null||raw.length()>MAX_ACTIONS_TEXT||raw.trim().isEmpty())return;String[] tokens=raw.split(",",MAX_ACTION_TOKENS+1);if(tokens.length>MAX_ACTION_TOKENS)return;for(String token:tokens){String value=token.trim();if(value.isEmpty()||value.length()>MAX_ACTION_TOKEN)continue;for(DdsFakePlayerAction action:DdsFakePlayerAction.values())if(action.wireName().equals(value)){out.add(action);break;}}}
    private static String bounded(String value,int max){return value==null||value.length()>max?"":value;}
    public boolean supportsAction(DdsFakePlayerAction action){return mirrorCompatible&&action!=null&&supportedActions.contains(action);}

    @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof DdsFakePlayerOpenContext))return false;DdsFakePlayerOpenContext c=(DdsFakePlayerOpenContext)o;return containerId==c.containerId&&layout==c.layout&&menuSchema==c.menuSchema&&hasExplicitSupportedActions==c.hasExplicitSupportedActions&&mirrorCompatible==c.mirrorCompatible&&Objects.equals(targetUuid,c.targetUuid)&&Objects.equals(targetName,c.targetName)&&Objects.equals(supportedActions,c.supportedActions);}
    @Override public int hashCode(){return Objects.hash(containerId,layout,menuSchema,targetUuid,targetName,hasExplicitSupportedActions,supportedActions,mirrorCompatible);}
}