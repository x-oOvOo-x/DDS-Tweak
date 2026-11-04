package ddstweak.integration.dds.playerinventory;

import org.junit.jupiter.api.Test;
import ddstweak.playerinventory.PlayerInventoryClientActionState;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryActionPolicyTest {
    @Test void ordinaryCarpetCommandsDoNotRequireFakeIdentity() {
        for (PlayerInventoryAction action : new PlayerInventoryAction[] {
                PlayerInventoryAction.SELECT_HOTBAR,
                PlayerInventoryAction.ATTACK_ONCE, PlayerInventoryAction.ATTACK_INTERVAL,
                PlayerInventoryAction.ATTACK_CONTINUOUS, PlayerInventoryAction.STOP_ATTACK,
                PlayerInventoryAction.USE_ONCE, PlayerInventoryAction.USE_INTERVAL,
                PlayerInventoryAction.USE_CONTINUOUS, PlayerInventoryAction.STOP_USE,
                PlayerInventoryAction.STOP_ALL, PlayerInventoryAction.DROP_ALL,
                PlayerInventoryAction.TOGGLE_SNEAK, PlayerInventoryAction.DISCONNECT
        }) {
            assertTrue(action.allowedWithoutFakeIdentity(), action.name());
        }
    }

    @Test void cdaOwnerEnderBatchActionsStillRequireAuthoritativeFakeIdentity() {
        for (PlayerInventoryAction action : new PlayerInventoryAction[] {
                PlayerInventoryAction.ENDER_DROP_ALL,
                PlayerInventoryAction.ENDER_STORE_ALL, PlayerInventoryAction.ENDER_TAKE_ALL
        }) {
            assertFalse(action.allowedWithoutFakeIdentity(), action.name());
        }
    }
    @Test void stopAllClearsBothSelectedActionIndicators() {
        PlayerInventoryClientActionState.State state = new PlayerInventoryClientActionState.State();
        state.record(PlayerInventoryAction.ATTACK_CONTINUOUS, 0);
        state.record(PlayerInventoryAction.USE_INTERVAL, 12);
        assertEquals(PlayerInventoryBehaviorV1.MODE_CONTINUOUS, state.attackMode());
        assertEquals(PlayerInventoryBehaviorV1.MODE_INTERVAL, state.useMode());

        state.record(PlayerInventoryAction.STOP_ALL, 0);
        assertEquals(PlayerInventoryBehaviorV1.MODE_NONE, state.attackMode());
        assertEquals(PlayerInventoryBehaviorV1.MODE_NONE, state.useMode());
    }

}
