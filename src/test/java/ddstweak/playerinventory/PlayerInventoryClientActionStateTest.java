package ddstweak.playerinventory;

import ddstweak.integration.dds.playerinventory.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryClientActionStateTest{
    @AfterEach void clear(){PlayerInventoryClientActionState.reset();}

    @Test void sameTargetKeepsInputAcrossDifferentScreenInstances(){
        UUID id=UUID.randomUUID();
        PlayerInventoryContext first=context(1,id,"gca"),second=context(2,id,"org");
        PlayerInventoryClientActionState.State state=PlayerInventoryClientActionState.forContext(first);
        state.rememberAttackInterval(37);
        state.rememberUseInterval(5);
        state.record(PlayerInventoryAction.ATTACK_INTERVAL,37);
        state.record(PlayerInventoryAction.USE_CONTINUOUS,0);

        PlayerInventoryClientActionState.State reopened=PlayerInventoryClientActionState.forContext(second);
        assertSame(state,reopened);
        assertEquals(37,reopened.attackInterval());
        assertEquals(5,reopened.useInterval());
        assertEquals(PlayerInventoryBehaviorV1.MODE_INTERVAL,reopened.attackMode());
        assertEquals(PlayerInventoryBehaviorV1.MODE_CONTINUOUS,reopened.useMode());
    }

    @Test void onceEndsOnlyTheSameActionAndKeepsRememberedIntervals(){
        PlayerInventoryClientActionState.State state=PlayerInventoryClientActionState.forContext(context(1,UUID.randomUUID(),"gca"));
        state.record(PlayerInventoryAction.ATTACK_INTERVAL,37);
        state.record(PlayerInventoryAction.USE_CONTINUOUS,0);
        state.record(PlayerInventoryAction.ATTACK_ONCE,0);
        assertEquals(PlayerInventoryBehaviorV1.MODE_NONE,state.attackMode());
        assertEquals(37,state.attackInterval());
        assertEquals(PlayerInventoryBehaviorV1.MODE_CONTINUOUS,state.useMode());
        state.record(PlayerInventoryAction.USE_ONCE,0);
        assertEquals(PlayerInventoryBehaviorV1.MODE_NONE,state.useMode());
    }

    @Test void pendingIntervalMasksAnOlderAuthoritativeSnapshotUntilItCatchesUp(){
        PlayerInventoryClientActionState.State state=PlayerInventoryClientActionState.forContext(context(1,UUID.randomUUID(),"gca"));
        state.record(PlayerInventoryAction.ATTACK_INTERVAL,3);
        assertTrue(state.preferLocalAttack(PlayerInventoryBehaviorV1.MODE_INTERVAL,2));
        assertEquals(3,state.attackInterval());
        assertFalse(state.preferLocalAttack(PlayerInventoryBehaviorV1.MODE_INTERVAL,3));

        state.record(PlayerInventoryAction.USE_INTERVAL,7);
        assertTrue(state.preferLocalUse(PlayerInventoryBehaviorV1.MODE_INTERVAL,6));
        assertFalse(state.preferLocalUse(PlayerInventoryBehaviorV1.MODE_INTERVAL,7));
    }

    @Test void pendingHotbarAndSneakAlsoMaskOlderAuthoritativeSnapshots(){
        PlayerInventoryClientActionState.State state=PlayerInventoryClientActionState.forContext(context(1,UUID.randomUUID(),"gca"));
        state.record(PlayerInventoryAction.SELECT_HOTBAR,7);
        assertTrue(state.preferLocalSelected(2));
        assertEquals(6,state.selected());
        assertFalse(state.preferLocalSelected(6));

        state.record(PlayerInventoryAction.TOGGLE_SNEAK,1);
        assertTrue(state.preferLocalSneaking(false));
        assertTrue(state.sneaking());
        assertFalse(state.preferLocalSneaking(true));
    }

    @Test void authoritativeReadConvergesLocalCacheWhenNoRequestIsPending(){
        PlayerInventoryClientActionState.State state=PlayerInventoryClientActionState.forContext(context(1,UUID.randomUUID(),"gca"));
        assertFalse(state.preferLocalAttack(PlayerInventoryBehaviorV1.MODE_INTERVAL,11));
        assertEquals(PlayerInventoryBehaviorV1.MODE_INTERVAL,state.attackMode());
        assertEquals(11,state.attackInterval());
        assertFalse(state.preferLocalUse(PlayerInventoryBehaviorV1.MODE_CONTINUOUS,4));
        assertEquals(PlayerInventoryBehaviorV1.MODE_CONTINUOUS,state.useMode());
    }

    @Test void differentTargetsDoNotShareControls(){
        PlayerInventoryClientActionState.State a=PlayerInventoryClientActionState.forContext(context(1,UUID.randomUUID(),"gca"));
        PlayerInventoryClientActionState.State b=PlayerInventoryClientActionState.forContext(context(2,UUID.randomUUID(),"gca"));
        a.record(PlayerInventoryAction.ATTACK_CONTINUOUS,0);
        assertEquals(PlayerInventoryBehaviorV1.MODE_CONTINUOUS,a.attackMode());
        assertEquals(PlayerInventoryBehaviorV1.MODE_NONE,b.attackMode());
    }

    private static PlayerInventoryContext context(int id,UUID uuid,String source){
        return new PlayerInventoryContext(id,1,source,uuid,"bot",PlayerInventoryTargetType.FAKE,PlayerInventoryTargetState.ONLINE,
                PlayerInventoryKind.INVENTORY,PlayerInventoryLayouts.GCA_INVENTORY_V1,1,"test",
                EnumSet.allOf(PlayerInventoryAction.class),Set.of(),PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
    }
}
