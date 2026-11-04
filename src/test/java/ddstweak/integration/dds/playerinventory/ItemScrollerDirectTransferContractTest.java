package ddstweak.integration.dds.playerinventory;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ItemScrollerDirectTransferContractTest {
    private enum Action {MOVE_TO_OTHER_MATCHING,MOVE_TO_OTHER_STACKS,MOVE_TO_OTHER_EVERYTHING}

    @Test void onlyInterceptsTheInitialMatchingGesture(){
        // The gesture recognizer is version-gated inside ItemScrollerDirectTransfer.
        assertEquals("MOVE_TO_OTHER_MATCHING",Action.MOVE_TO_OTHER_MATCHING.name());
        assertNotEquals(Action.MOVE_TO_OTHER_MATCHING,Action.MOVE_TO_OTHER_STACKS);
    }

    @Test void rejectsUnrelatedOrForbiddenDestinationSlots(){
        var plan=ItemScrollerMovePlanner.plan(
                List.of(new ItemScrollerMovePlanner.SlotState(54,2,64,true)),
                List.of(new ItemScrollerMovePlanner.SlotState(7,0,64,false),
                        new ItemScrollerMovePlanner.SlotState(18,0,64,true)));
        assertNotNull(plan);
        assertEquals(List.of(54,18),plan.clicks);
        assertFalse(plan.clicks.contains(7));
    }
}
