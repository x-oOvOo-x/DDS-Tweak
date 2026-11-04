package ddstweak.feature.silentautoeat;

import java.util.function.IntPredicate;

final class SilentAutoEatRules {
    private SilentAutoEatRules(){}

    /** Legacy ordering: inventory before the remaining hotbar. */
    static int selectFoodSource(int selected,IntPredicate eligible){
        return selectFoodSource(selected,eligible,false);
    }

    /** 26.3 may prefer a hotbar source to avoid real offhand SWAP animations. */
    static int selectFoodSource(int selected,IntPredicate eligible,boolean preferHotbar){
        if(preferHotbar)
            for(int i=0;i<9;i++)if(i!=selected&&eligible.test(i))return i;
        for(int i=9;i<36;i++)if(eligible.test(i))return i;
        for(int i=0;i<9;i++)if(i!=selected&&eligible.test(i))return i;
        return selected>=0&&selected<9&&eligible.test(selected)?selected:Integer.MIN_VALUE;
    }

    static boolean sourceSelectionConflicts(int sourceIndex,boolean selectedMainhandSource,int selected){
        if(sourceIndex<0||sourceIndex>=9)return false;
        return selectedMainhandSource?selected!=sourceIndex:selected==sourceIndex;
    }

    /** -1 means offhand/no player-inventory slot, never a renderable source. */
    static boolean isOwnedSourceIndex(int index,int sourceIndex){
        return index>=0&&index<36&&sourceIndex>=0&&sourceIndex<36&&index==sourceIndex;
    }

    static boolean isSafeInventoryContext(boolean personalMenu,boolean noScreen,boolean vanillaInventoryScreen){
        return personalMenu&&(noScreen||vanillaInventoryScreen);
    }

    static boolean isTransactionSwapSlot(int inventoryIndex,int sourceIndex){
        return sourceIndex>=0&&sourceIndex<36
                &&(inventoryIndex==sourceIndex||inventoryIndex==40);
    }

    static boolean hasConfirmedSwap(boolean sourceOwnsOriginal,boolean offhandOwnsFood){
        return sourceOwnsOriginal&&offhandOwnsFood;
    }

    static boolean hasConfirmedRestore(boolean sourceOwnsFood,boolean offhandOwnsOriginal){
        return sourceOwnsFood&&offhandOwnsOriginal;
    }

    /** Maintain the selected-source hand only during a confirmed, brief visual restoration. */
    static boolean keepRestoredMainhandVisual(boolean idle,boolean sameOwner,
                                              boolean withinWindow,boolean selectedSource,
                                              boolean sourceRestored){
        return idle&&sameOwner&&withinWindow&&selectedSource&&sourceRestored;
    }

    /** Never let a manual use consume or place an item from an in-flight offhand SWAP. */
    static boolean shouldDeferExplicitUse(boolean active,boolean swapped,int sourceIndex){
        return active&&swapped&&sourceIndex>=0&&sourceIndex<36;
    }

    static boolean isWithinUserOverride(long tick,long deadline){
        return tick<deadline;
    }

    static boolean canRestoreOffhand(boolean foodStillOwned,boolean consumptionObserved,boolean vanillaRemainder){
        return foodStillOwned||consumptionObserved&&vanillaRemainder;
    }

    /** Never reverse an inventory SWAP while the vanilla offhand use is still active. */
    static boolean canSendRestoreSwap(long currentTick,long readyTick,boolean usingOffhand){
        return currentTick>=readyTick&&!usingOffhand;
    }

    static boolean shouldMaskPendingRestore(boolean sourceOwned,boolean offhandOwned){
        return sourceOwned&&offhandOwned;
    }

    static boolean shouldBypassSlowdown(boolean optionEnabled,boolean ownsActiveEat){
        return optionEnabled&&ownsActiveEat;
    }

    static boolean shouldTrigger(boolean enabled,boolean safeMenu,
                                 boolean usingItem,boolean spectator,boolean canEat,
                                 int foodLevel,int threshold){
        return enabled&&safeMenu&&!usingItem&&!spectator&&canEat&&foodLevel<=threshold;
    }
}
