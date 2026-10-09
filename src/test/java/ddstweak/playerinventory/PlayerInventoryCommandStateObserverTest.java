package ddstweak.playerinventory;

import ddstweak.integration.dds.playerinventory.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventoryCommandStateObserverTest {
    @AfterEach void clear(){PlayerInventoryClientActionState.reset();}

    @Test void manualPlayerCommandsUpdateTheSamePredictionBeforeAndAfterUuidBinding(){
        PlayerInventoryCommandStateObserver.observeCommand("/player bot use interval 23");
        PlayerInventoryClientActionState.State byName=PlayerInventoryClientActionState.forTargetName("bot");
        assertEquals(PlayerInventoryBehaviorV1.MODE_INTERVAL,byName.useMode());
        assertEquals(23,byName.useInterval());

        PlayerInventoryContext context=context(UUID.randomUUID());
        PlayerInventoryClientActionState.State byUuid=PlayerInventoryClientActionState.forContext(context);
        assertSame(byName,byUuid);

        PlayerInventoryCommandStateObserver.observeCommand("player bot use stop");
        assertEquals(PlayerInventoryBehaviorV1.MODE_NONE,byUuid.useMode());
        assertEquals(23,byUuid.useInterval());

        PlayerInventoryCommandStateObserver.observeCommand("player bot attack continuous");
        assertEquals(PlayerInventoryBehaviorV1.MODE_CONTINUOUS,byUuid.attackMode());
        PlayerInventoryCommandStateObserver.observeCommand("player bot attack once");
        assertEquals(PlayerInventoryBehaviorV1.MODE_NONE,byUuid.attackMode());

        PlayerInventoryCommandStateObserver.observeCommand("player bot hotbar 7");
        assertEquals(6,byUuid.selected());
        PlayerInventoryCommandStateObserver.observeCommand("player bot sneak");
        assertTrue(byUuid.sneaking());
        PlayerInventoryCommandStateObserver.observeCommand("player bot unsneak");
        assertFalse(byUuid.sneaking());
    }

    @Test void unrelatedOrInvalidCommandsDoNotMutatePrediction(){
        PlayerInventoryClientActionState.State state=PlayerInventoryClientActionState.forTargetName("bot");
        PlayerInventoryCommandStateObserver.observeCommand("say hello");
        PlayerInventoryCommandStateObserver.observeCommand("player bot use interval nope");
        PlayerInventoryCommandStateObserver.observeCommand("player bot hotbar 10");
        assertFalse(state.attackKnown());assertFalse(state.useKnown());assertFalse(state.selectedKnown());
    }

    private static PlayerInventoryContext context(UUID uuid){
        return new PlayerInventoryContext(1,1,"gca",uuid,"bot",PlayerInventoryTargetType.FAKE,PlayerInventoryTargetState.ONLINE,
                PlayerInventoryKind.INVENTORY,PlayerInventoryLayouts.GCA_INVENTORY_V1,1,"test",
                EnumSet.allOf(PlayerInventoryAction.class),Set.of(),PlayerInventoryFallbackPolicy.KEEP_SOURCE_SCREEN);
    }
}
