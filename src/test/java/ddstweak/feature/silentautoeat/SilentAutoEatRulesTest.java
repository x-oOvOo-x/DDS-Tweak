package ddstweak.feature.silentautoeat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SilentAutoEatRulesTest {
    @Test void automationOnlyRunsWithPersonalInventoryMenu(){
        assertTrue(SilentAutoEatRules.isSafeInventoryContext(true,true,false));
        assertTrue(SilentAutoEatRules.isSafeInventoryContext(true,false,true));
        assertFalse(SilentAutoEatRules.isSafeInventoryContext(true,false,false));
        assertFalse(SilentAutoEatRules.isSafeInventoryContext(false,true,false));
        assertFalse(SilentAutoEatRules.isSafeInventoryContext(false,false,true));
        assertFalse(SilentAutoEatRules.isSafeInventoryContext(false,false,false));
    }

    @Test void swapAndFailedRestoreNeedBothConfirmedSlots(){
        assertTrue(SilentAutoEatRules.hasConfirmedSwap(true,true));
        assertFalse(SilentAutoEatRules.hasConfirmedSwap(false,true));
        assertFalse(SilentAutoEatRules.hasConfirmedSwap(true,false));
        assertTrue(SilentAutoEatRules.hasConfirmedRestore(true,true));
        assertFalse(SilentAutoEatRules.hasConfirmedRestore(true,false));
        assertFalse(SilentAutoEatRules.hasConfirmedRestore(false,true));
    }

    @Test void selectedMainhandIsOnlyFallbackFoodSource(){
        assertEquals(4,SilentAutoEatRules.selectFoodSource(4,i->i==4));
        assertEquals(12,SilentAutoEatRules.selectFoodSource(4,i->i==4||i==12));
        assertEquals(2,SilentAutoEatRules.selectFoodSource(4,i->i==2||i==4));
        assertEquals(Integer.MIN_VALUE,SilentAutoEatRules.selectFoodSource(4,i->false));
        assertEquals(Integer.MIN_VALUE,SilentAutoEatRules.selectFoodSource(10,i->false));
    }

    @Test void selectedSourceRemainsSelectedUntilSwapRestoration(){
        assertFalse(SilentAutoEatRules.sourceSelectionConflicts(4,true,4));
        assertTrue(SilentAutoEatRules.sourceSelectionConflicts(4,true,3));
        assertFalse(SilentAutoEatRules.sourceSelectionConflicts(4,false,3));
        assertTrue(SilentAutoEatRules.sourceSelectionConflicts(4,false,4));
        assertFalse(SilentAutoEatRules.sourceSelectionConflicts(12,false,4));
        assertFalse(SilentAutoEatRules.sourceSelectionConflicts(-1,false,4));
    }

    @Test void mainhandProjectionFollowsCurrentSelectionEvenWhenItChangedMidEat(){
        // Source hotbar slot 4 was not selected at transaction start.
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(2,4));
        assertTrue(SilentAutoEatRules.isOwnedSourceIndex(4,4));
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(5,4));
        // Inventory slots 9..35 and offhand-only eating are never mainhand sources.
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(4,14));
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(4,-1));
        // Selection changes still cause a normal restore, not a silent source swap.
        assertTrue(SilentAutoEatRules.sourceSelectionConflicts(4,false,4));
        assertTrue(SilentAutoEatRules.sourceSelectionConflicts(4,true,5));
    }

    @Test void inFlightSourceAndOffhandMustNotBeClicked(){
        assertTrue(SilentAutoEatRules.isTransactionSwapSlot(12,12));
        assertTrue(SilentAutoEatRules.isTransactionSwapSlot(40,12));
        assertFalse(SilentAutoEatRules.isTransactionSwapSlot(13,12));
        assertFalse(SilentAutoEatRules.isTransactionSwapSlot(12,13));
        assertFalse(SilentAutoEatRules.isTransactionSwapSlot(-1,12));
        assertFalse(SilentAutoEatRules.isTransactionSwapSlot(40,-1));
        assertFalse(SilentAutoEatRules.isTransactionSwapSlot(36,12));
    }

    @Test void offhandSentinelNeverProjectsUnrelatedSlots(){
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(-1,-1));
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(12,-1));
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(-1,12));
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(40,12));
        assertFalse(SilentAutoEatRules.isOwnedSourceIndex(36,36));
        assertTrue(SilentAutoEatRules.isOwnedSourceIndex(12,12));
        assertTrue(SilentAutoEatRules.isOwnedSourceIndex(0,0));
        assertTrue(SilentAutoEatRules.isOwnedSourceIndex(35,35));
    }

    @Test void restoredMainhandCacheMayBeMaskedOnlyInOwnedTimeBoundedWindow(){
        assertTrue(SilentAutoEatRules.keepRestoredMainhandVisual(true,true,true,true,true));
        assertFalse(SilentAutoEatRules.keepRestoredMainhandVisual(false,true,true,true,true));
        assertFalse(SilentAutoEatRules.keepRestoredMainhandVisual(true,false,true,true,true));
        assertFalse(SilentAutoEatRules.keepRestoredMainhandVisual(true,true,false,true,true));
        assertFalse(SilentAutoEatRules.keepRestoredMainhandVisual(true,true,true,false,true));
        assertFalse(SilentAutoEatRules.keepRestoredMainhandVisual(true,true,true,true,false));
    }

    @Test void manualUseCannotRunOnTemporarilySwappedOffhandOrSource(){
        // Scaffold originally in offhand is physically in the hotbar food slot
        // until a reverse SWAP is acknowledged; never let vanilla use it there.
        assertTrue(SilentAutoEatRules.shouldDeferExplicitUse(true,true,4));
        assertTrue(SilentAutoEatRules.shouldDeferExplicitUse(true,true,12));
        // Covers WAIT_SWAP, active eating and unsent/sent RESTORE_OFFHAND alike.
        assertFalse(SilentAutoEatRules.shouldDeferExplicitUse(false,true,4));
        assertFalse(SilentAutoEatRules.shouldDeferExplicitUse(true,false,4));
        assertFalse(SilentAutoEatRules.shouldDeferExplicitUse(true,true,-1));
        assertFalse(SilentAutoEatRules.shouldDeferExplicitUse(true,true,36));
    }

    @Test void playerOverrideSuppressesRestartUntilDeadline(){
        assertTrue(SilentAutoEatRules.isWithinUserOverride(10,30));
        assertTrue(SilentAutoEatRules.isWithinUserOverride(29,30));
        assertFalse(SilentAutoEatRules.isWithinUserOverride(30,30));
        assertFalse(SilentAutoEatRules.isWithinUserOverride(31,30));
    }

    @Test void recoveryRejectsForeignOffhandState(){
        assertTrue(SilentAutoEatRules.canRestoreOffhand(true,false,false));
        assertTrue(SilentAutoEatRules.canRestoreOffhand(false,true,true));
        assertFalse(SilentAutoEatRules.canRestoreOffhand(false,true,false));
        assertFalse(SilentAutoEatRules.canRestoreOffhand(false,false,true));
    }

    @Test void interruptionCannotReverseSwapBeforeOffhandUseIsReleased(){
        assertFalse(SilentAutoEatRules.canSendRestoreSwap(9,10,false));
        assertFalse(SilentAutoEatRules.canSendRestoreSwap(10,10,true));
        assertFalse(SilentAutoEatRules.canSendRestoreSwap(11,10,true));
        assertTrue(SilentAutoEatRules.canSendRestoreSwap(10,10,false));
        assertTrue(SilentAutoEatRules.canSendRestoreSwap(11,10,false));
    }

    @Test void pendingRestoreKeepsOffhandHiddenOnlyWhileBothSlotsAreOwned(){
        assertTrue(SilentAutoEatRules.shouldMaskPendingRestore(true,true));
        assertFalse(SilentAutoEatRules.shouldMaskPendingRestore(false,true));
        assertFalse(SilentAutoEatRules.shouldMaskPendingRestore(true,false));
        assertFalse(SilentAutoEatRules.shouldMaskPendingRestore(false,false));
    }

    @Test void movementBypassRequiresOptInAndActiveAutoEat(){
        assertFalse(SilentAutoEatRules.shouldBypassSlowdown(false,false));
        assertFalse(SilentAutoEatRules.shouldBypassSlowdown(false,true));
        assertFalse(SilentAutoEatRules.shouldBypassSlowdown(true,false));
        assertTrue(SilentAutoEatRules.shouldBypassSlowdown(true,true));
    }

    @Test void triggerRequiresEverySafetyGate(){
        assertTrue(SilentAutoEatRules.shouldTrigger(true,true,false,false,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(false,true,false,false,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,false,false,false,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,true,true,false,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,true,false,true,true,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,true,false,false,false,14,14));
        assertFalse(SilentAutoEatRules.shouldTrigger(true,true,false,false,true,15,14));
    }
}
