package ddstweak.integration.provider;

import ddstweak.integration.dds.fakeplayer.DdsFakePlayerIdentityClient;
import ddstweak.integration.dds.playerinventory.PlayerInventoryContext;
import ddstweak.integration.dds.playerinventory.PlayerInventoryInteractionCapability;
import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetState;
import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import ddstweak.integration.org.playerinventory.OrgFakePlayerIdentityClient;
import java.util.LinkedHashSet;
import java.util.UUID;

/** Composes optional authoritative UUID sources without coupling provider adapters to DDS. */
public final class PlayerIdentityResolver {
    private PlayerIdentityResolver(){}

    public static PlayerInventoryTargetType resolve(UUID uuid){
        if(uuid==null)return PlayerInventoryTargetType.UNKNOWN;
        PlayerInventoryTargetType dds=DdsFakePlayerIdentityClient.resolve(uuid);
        if(dds!=PlayerInventoryTargetType.UNKNOWN)return dds;
        return OrgFakePlayerIdentityClient.isFake(uuid)?PlayerInventoryTargetType.FAKE:PlayerInventoryTargetType.UNKNOWN;
    }

    /**
     * A strict extension session is already bound by the server to the exact viewer connection,
     * menu generation and live target instance. Provider-only fallback contexts still re-resolve
     * their UUID so stale local identity snapshots never manufacture fake-player authority.
     */
    public static PlayerInventoryTargetType currentType(PlayerInventoryContext context){
        if(context==null)return PlayerInventoryTargetType.UNKNOWN;
        if(context.contextSchema()==3&&context.targetType()!=PlayerInventoryTargetType.UNKNOWN
                &&context.targetState()!=PlayerInventoryTargetState.UNKNOWN)return context.targetType();
        if("gca".equals(context.source())||"org".equals(context.source()))
            return context.targetUuid()==null?PlayerInventoryTargetType.UNKNOWN:resolve(context.targetUuid());
        return context.targetType();
    }

    /**
     * Drops Extension-only menu capabilities for a navigation hand-off while preserving
     * a server-confirmed REAL/OFFLINE identity as discovery metadata. Fake targets still
     * go through the stricter legacy re-resolution path.
     */
    public static PlayerInventoryContext navigationFallback(PlayerInventoryContext context){
        if(context==null||context.contextSchema()!=3||context.targetType()!=PlayerInventoryTargetType.REAL)
            return withoutStrictSession(context);
        LinkedHashSet<PlayerInventoryInteractionCapability> capabilities=new LinkedHashSet<>(context.interactionCapabilities());
        capabilities.remove(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP);
        capabilities.remove(PlayerInventoryInteractionCapability.RETURN_PARENT);
        return new PlayerInventoryContext(context.containerId(),1,context.source(),context.targetUuid(),context.targetName(),
                PlayerInventoryTargetType.REAL,context.targetState(),context.inventoryKind(),context.layoutId(),context.layoutSchema(),
                context.behaviorId(),java.util.Collections.emptySet(),capabilities,context.fallbackPolicy());
    }

    /** Drops Extension-only authority while keeping the Provider menu/navigation metadata usable. */
    public static PlayerInventoryContext withoutStrictSession(PlayerInventoryContext context){
        if(context==null||context.contextSchema()!=3)return context;
        LinkedHashSet<PlayerInventoryInteractionCapability> capabilities=new LinkedHashSet<>(context.interactionCapabilities());
        capabilities.remove(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP);
        PlayerInventoryTargetType type=context.targetUuid()==null?PlayerInventoryTargetType.UNKNOWN:resolve(context.targetUuid());
        return new PlayerInventoryContext(context.containerId(),1,context.source(),context.targetUuid(),context.targetName(),
                type,context.targetState(),context.inventoryKind(),context.layoutId(),context.layoutSchema(),context.behaviorId(),
                context.supportedActions(),capabilities,context.fallbackPolicy());
    }

    /**
     * Fake-only actions require authoritative server/provider identity, never a ping/title heuristic.
     * Strict schema-3 extension contexts carry that authority themselves; provider fallbacks still
     * require the independently synchronized positive fake identity.
     */
    public static boolean isAuthoritativeFake(PlayerInventoryContext context){
        if(context==null||context.targetState()!=PlayerInventoryTargetState.ONLINE
                ||currentType(context)!=PlayerInventoryTargetType.FAKE)return false;
        if(context.contextSchema()==3)return true;
        if("dds".equals(context.source())||"dds_qca".equals(context.source()))return true;
        return context.targetUuid()!=null&&resolve(context.targetUuid())==PlayerInventoryTargetType.FAKE;
    }
}
