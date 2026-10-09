package ddstweak.integration.dds.playerinventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DdsPlayerInventoryStructureStateTest {
    private static final long TIMEOUT = 3_000_000_000L;

    @Test void pendingIsBoundToConnectionMenuTargetGenerationAndRequest() {
        DdsPlayerInventoryStructureState<Object,Object,String> state =
                new DdsPlayerInventoryStructureState<>(TIMEOUT);
        Object connection=new Object(), menu=new Object();
        long now=10L;

        assertEquals(DdsPlayerInventoryStructureState.Start.STARTED,
                state.begin(connection,menu,"target",7,11L,1L,
                        DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,now));
        assertEquals(DdsPlayerInventoryStructureState.Start.EXISTING,
                state.begin(connection,menu,"target",7,11L,1L,
                        DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,now+1));
        assertEquals(DdsPlayerInventoryStructureState.Start.BUSY,
                state.begin(connection,new Object(),"target",7,11L,2L,
                        DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,now+1));

        DdsPlayerInventoryStructureState.Pending<Object,Object,String> pending=state.current(connection,now+1);
        assertNotNull(pending);
        assertSame(menu,pending.sourceMenu());
        assertEquals("target",pending.target());
        assertEquals(7,pending.sourceContainerId());
        assertEquals(11L,pending.sourceGeneration());
        assertEquals(1L,pending.requestId());
    }

    @Test void acceptedAndContextDoNotFinishUntilMatchingOpenScreenIsConsumed() {
        DdsPlayerInventoryStructureState<Object,Object,String> state =
                new DdsPlayerInventoryStructureState<>(TIMEOUT);
        Object connection=new Object(), menu=new Object();
        state.begin(connection,menu,"target",7,11L,9L,
                DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,100L);

        assertEquals(DdsPlayerInventoryStructureState.Result.IGNORED,
                state.applyResult(connection,7,"target",11L,8L,"accepted",101L));
        assertEquals(DdsPlayerInventoryStructureState.Result.ACCEPTED,
                state.applyResult(connection,7,"target",11L,9L,"accepted",102L));
        assertNotNull(state.current(connection,103L),"accepted is only server conversion");

        assertFalse(state.observeNextContext(connection,"target",20,11L,104L));
        assertFalse(state.observeNextContext(new Object(),"target",20,12L,104L));
        assertFalse(state.observeNextContext(connection,"other",20,12L,104L));
        assertTrue(state.observeNextContext(connection,"target",20,12L,104L));
        assertNotNull(state.current(connection,104L),"Context alone must not complete staging");

        assertNull(state.consumeObservedOpen(connection,21,"target",12L,105L));
        assertNull(state.consumeObservedOpen(connection,20,"other",12L,105L));
        DdsPlayerInventoryStructureState.Pending<Object,Object,String> completed =
                state.consumeObservedOpen(connection,20,"target",12L,105L);
        assertNotNull(completed);
        assertNull(state.current(connection,106L));
    }

    @Test void observedContextStillExpiresIfOpenScreenNeverArrives() {
        DdsPlayerInventoryStructureState<Object,Object,String> state =
                new DdsPlayerInventoryStructureState<>(TIMEOUT);
        Object connection=new Object(), menu=new Object();
        state.begin(connection,menu,"target",2,8L,3L,
                DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,0L);
        assertTrue(state.observeNextContext(connection,"target",9,9L,1L));
        assertTrue(state.current(connection,1L).destinationObserved());
        assertNotNull(state.expire(connection,TIMEOUT));
    }

    @Test void matchingRejectClearsPendingButLateOrWrongRejectDoesNot() {
        DdsPlayerInventoryStructureState<Object,Object,String> state =
                new DdsPlayerInventoryStructureState<>(TIMEOUT);
        Object connection=new Object(), menu=new Object();
        state.begin(connection,menu,"target",3,4L,5L,
                DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,0L);

        assertEquals(DdsPlayerInventoryStructureState.Result.IGNORED,
                state.applyResult(connection,3,"target",4L,4L,"rejected",1L));
        assertNotNull(state.current(connection,1L));
        assertEquals(DdsPlayerInventoryStructureState.Result.REJECTED,
                state.applyResult(connection,3,"target",4L,5L,"rejected",2L));
        assertNull(state.current(connection,2L));
        assertEquals(DdsPlayerInventoryStructureState.Result.IGNORED,
                state.applyResult(connection,3,"target",4L,5L,"accepted",3L));
    }

    @Test void returnAndCanonicalRequestsCannotOverlapAndRepeatedReturnIsIdempotent() {
        DdsPlayerInventoryStructureState<Object,Object,String> state =
                new DdsPlayerInventoryStructureState<>(TIMEOUT);
        Object connection=new Object(), menu=new Object();

        assertEquals(DdsPlayerInventoryStructureState.Start.STARTED,
                state.begin(connection,menu,"target",8,20L,1L,
                        DdsPlayerInventoryStructureState.Operation.RETURN_PARENT,0L));
        assertEquals(DdsPlayerInventoryStructureState.Start.EXISTING,
                state.begin(connection,menu,"target",8,20L,1L,
                        DdsPlayerInventoryStructureState.Operation.RETURN_PARENT,1L));
        assertEquals(DdsPlayerInventoryStructureState.Start.BUSY,
                state.begin(connection,menu,"target",8,20L,2L,
                        DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,1L));
    }

    @Test void timeoutIsDeterministicAndDoesNotSleep() {
        DdsPlayerInventoryStructureState<Object,Object,String> state =
                new DdsPlayerInventoryStructureState<>(TIMEOUT);
        Object connection=new Object(), menu=new Object();
        state.begin(connection,menu,"target",1,1L,1L,
                DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,100L);

        assertNull(state.expire(connection,100L+TIMEOUT-1));
        DdsPlayerInventoryStructureState.Pending<Object,Object,String> expired =
                state.expire(connection,100L+TIMEOUT);
        assertNotNull(expired);
        assertSame(menu,expired.sourceMenu());
        assertNull(state.current(connection,100L+TIMEOUT));
    }

    @Test void lateResultCannotSilentlyConsumeTimeoutFallback() {
        DdsPlayerInventoryStructureState<Object,Object,String> state =
                new DdsPlayerInventoryStructureState<>(TIMEOUT);
        Object connection=new Object(), menu=new Object();
        state.begin(connection,menu,"target",1,1L,1L,
                DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,0L);

        assertEquals(DdsPlayerInventoryStructureState.Result.IGNORED,
                state.applyResult(connection,1,"target",1L,1L,"accepted",TIMEOUT));
        assertNotNull(state.current(connection,TIMEOUT));
        assertNotNull(state.expire(connection,TIMEOUT));
    }

    @Test void clearReturnsPendingForCallerControlledFallback() {
        DdsPlayerInventoryStructureState<Object,Object,String> state =
                new DdsPlayerInventoryStructureState<>(TIMEOUT);
        Object connection=new Object(), menu=new Object();
        state.begin(connection,menu,"target",1,1L,1L,
                DdsPlayerInventoryStructureState.Operation.CANONICAL_ENDER,0L);

        DdsPlayerInventoryStructureState.Pending<Object,Object,String> cleared=state.clear();
        assertNotNull(cleared);
        assertSame(menu,cleared.sourceMenu());
        assertNull(state.current(connection,0L));
        assertNull(state.clear());
    }
}
