package ddstweak.integration.dds.playerinventory;

import ddstweak.integration.dds.fakeplayer.*;
import ddstweak.integration.dds.playerinventory.legacy.DdsFakePlayerLegacyContextAdapter;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryLegacyAdapterTest{
    @Test void inventoryLegacyContextMapsWithoutChangingWireSemantics(){
        UUID uuid=UUID.randomUUID();
        EnumSet<DdsFakePlayerAction> wire=EnumSet.allOf(DdsFakePlayerAction.class);
        DdsFakePlayerOpenContext legacy=new DdsFakePlayerOpenContext(
                7,DdsFakePlayerPackets.MENU_INVENTORY,1,uuid,"bot",true,wire,true);
        PlayerInventoryContext c=DdsFakePlayerLegacyContextAdapter.adapt(legacy);

        assertNotNull(c);
        assertEquals(7,c.containerId());
        assertEquals("dds",c.source());
        assertEquals(uuid,c.targetUuid());
        assertEquals("bot",c.targetName());
        assertEquals(PlayerInventoryTargetType.FAKE,c.targetType());
        assertEquals(PlayerInventoryTargetState.ONLINE,c.targetState());
        assertEquals(PlayerInventoryKind.INVENTORY,c.inventoryKind());
        assertEquals(PlayerInventoryLayouts.INVENTORY_V1,c.layoutId());
        assertEquals(PlayerInventoryBehaviorV1.ID,c.behaviorId());
        assertEquals(PlayerInventoryFallbackPolicy.REJECT_INCOMPATIBLE_CONTAINER,c.fallbackPolicy());
        assertEquals(legacyAllowedCount(PlayerInventoryKind.INVENTORY),c.supportedActions().size());
        assertTrue(c.allows(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP));

        for(DdsFakePlayerAction action:wire){
            PlayerInventoryAction neutral=DdsFakePlayerLegacyContextAdapter.action(action);
            assertNotNull(neutral,action.name());
            assertEquals(action,DdsFakePlayerLegacyContextAdapter.legacyAction(neutral),action.name());
            assertEquals(neutral.allowedIn(PlayerInventoryKind.INVENTORY),
                    DdsFakePlayerLegacyContextAdapter.supports(c,action),action.name());
        }
    }

    @Test void enderRowsRemainDistinctLayouts(){
        DdsFakePlayerOpenContext three=new DdsFakePlayerOpenContext(
                1,3,1,null,"bot",true,EnumSet.allOf(DdsFakePlayerAction.class),true);
        DdsFakePlayerOpenContext six=new DdsFakePlayerOpenContext(
                2,6,1,null,"bot",true,EnumSet.allOf(DdsFakePlayerAction.class),true);
        PlayerInventoryContext c3=DdsFakePlayerLegacyContextAdapter.adapt(three),
                c6=DdsFakePlayerLegacyContextAdapter.adapt(six);

        assertEquals(PlayerInventoryLayouts.ENDER_3_V1,c3.layoutId());
        assertEquals(PlayerInventoryLayouts.ENDER_6_V1,c6.layoutId());
        assertEquals(3,DdsFakePlayerLegacyContextAdapter.enderRows(c3));
        assertEquals(6,DdsFakePlayerLegacyContextAdapter.enderRows(c6));
        assertEquals(legacyAllowedCount(PlayerInventoryKind.ENDER_CHEST),c3.supportedActions().size());
        assertEquals(legacyAllowedCount(PlayerInventoryKind.ENDER_CHEST),c6.supportedActions().size());
        assertTrue(c3.allows(PlayerInventoryInteractionCapability.QUICK_MOVE_PREDICTION));
        assertTrue(c6.allows(PlayerInventoryInteractionCapability.QUICK_MOVE_PREDICTION));
    }

    @Test void allLegacyWireActionsRemainMappedButAuthorizationFollowsMenuKind(){
        for(DdsFakePlayerAction action:DdsFakePlayerAction.values()){
            PlayerInventoryAction neutral=DdsFakePlayerLegacyContextAdapter.action(action);
            assertNotNull(neutral,action.name());
            assertEquals(action,DdsFakePlayerLegacyContextAdapter.legacyAction(neutral),action.name());
        }

        PlayerInventoryContext inventory=DdsFakePlayerLegacyContextAdapter.adapt(new DdsFakePlayerOpenContext(
                3,DdsFakePlayerPackets.MENU_INVENTORY,1,null,"bot",true,
                EnumSet.allOf(DdsFakePlayerAction.class),true));
        PlayerInventoryContext ender=DdsFakePlayerLegacyContextAdapter.adapt(new DdsFakePlayerOpenContext(
                4,DdsFakePlayerPackets.MENU_ENDER_3,1,null,"bot",true,
                EnumSet.allOf(DdsFakePlayerAction.class),true));

        for(DdsFakePlayerAction action:DdsFakePlayerAction.values()){
            PlayerInventoryAction neutral=DdsFakePlayerLegacyContextAdapter.action(action);
            assertEquals(neutral.allowedIn(PlayerInventoryKind.INVENTORY),
                    DdsFakePlayerLegacyContextAdapter.supports(inventory,action),"inventory "+action);
            assertEquals(neutral.allowedIn(PlayerInventoryKind.ENDER_CHEST),
                    DdsFakePlayerLegacyContextAdapter.supports(ender,action),"ender "+action);
        }
        assertFalse(DdsFakePlayerLegacyContextAdapter.supports(inventory,null));
        assertFalse(DdsFakePlayerLegacyContextAdapter.supports(ender,null));
    }

    @Test void incompatibleLegacyContextDoesNotEnterNeutralPipeline(){
        assertNull(DdsFakePlayerLegacyContextAdapter.adapt(new DdsFakePlayerOpenContext(
                1,99,2,null,"bad",true,Set.of(),false)));
    }

    private static long legacyAllowedCount(PlayerInventoryKind kind){
        return Arrays.stream(DdsFakePlayerAction.values())
                .map(DdsFakePlayerLegacyContextAdapter::action)
                .filter(Objects::nonNull)
                .filter(action->action.allowedIn(kind))
                .count();
    }
}
