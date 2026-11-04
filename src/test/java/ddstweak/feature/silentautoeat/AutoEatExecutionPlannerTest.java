package ddstweak.feature.silentautoeat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AutoEatExecutionPlannerTest {
    @Test void protectedOffhandSkipsUnavailableInventoryStagingButUsesHotbar(){
        for(AutoEatOffhandPolicy policy:new AutoEatOffhandPolicy[]{
                AutoEatOffhandPolicy.PROTECT_CRITICAL,AutoEatOffhandPolicy.NEVER_REPLACE}){
            int source=AutoEatExecutionPlanner.selectFoodSource(policy,false,true,0,
                    i->i==1||i==9,false,false);
            assertEquals(1,source);
            assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR,
                    AutoEatExecutionPlanner.plan(policy,false,false,true,source,0,false));
            assertEquals(Integer.MIN_VALUE,AutoEatExecutionPlanner.selectFoodSource(
                    policy,false,true,0,i->i==9,false,false));
        }
    }

    @Test void feasibleSourcesRetainTheExistingOrder(){
        assertEquals(9,AutoEatExecutionPlanner.selectFoodSource(
                AutoEatOffhandPolicy.ALLOW_REPLACE,false,true,0,i->i==1||i==9,false,false));
        assertEquals(9,AutoEatExecutionPlanner.selectFoodSource(
                AutoEatOffhandPolicy.PROTECT_CRITICAL,true,false,0,i->i==1||i==9,false,false));
        assertEquals(9,AutoEatExecutionPlanner.selectFoodSource(
                AutoEatOffhandPolicy.PROTECT_CRITICAL,false,true,0,i->i==1||i==9,false,true));
        assertEquals(1,AutoEatExecutionPlanner.selectFoodSource(
                AutoEatOffhandPolicy.PROTECT_CRITICAL,false,true,0,i->i==1||i==9,true,true));
    }

    @Test void offhandFoodDoesNotMoveRegardlessOfPolicy(){
        for(AutoEatOffhandPolicy policy:AutoEatOffhandPolicy.values())
            assertEquals(AutoEatExecutionPlanner.Path.USE_OFFHAND_DIRECT,
                    AutoEatExecutionPlanner.plan(policy,true,false,true,-1,0));
    }

    @Test void criticalOffhandUsesMainhandWithoutDisplacingTotemOrShield(){
        assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.PROTECT_CRITICAL,false,false,true,4,0));
        assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_STAGED,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.PROTECT_CRITICAL,false,false,true,16,0));
    }

    @Test void neverReplaceProtectsEveryOccupiedOffhand(){
        assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.NEVER_REPLACE,false,false,false,8,0));
        assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_STAGED,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.NEVER_REPLACE,false,false,true,35,0));
        assertEquals(AutoEatExecutionPlanner.Path.SWAP_TO_OFFHAND,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.NEVER_REPLACE,false,true,false,12,0));
    }

    @Test void defaultPolicyPreservesExistingOrdinaryOffhandSwap(){
        assertEquals(AutoEatExecutionPlanner.Path.SWAP_TO_OFFHAND,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.PROTECT_CRITICAL,false,false,false,12,0));
        assertEquals(AutoEatExecutionPlanner.Path.SWAP_TO_OFFHAND,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.PROTECT_CRITICAL,false,true,false,2,0));
        assertEquals(AutoEatExecutionPlanner.Path.SWAP_TO_OFFHAND,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.ALLOW_REPLACE,false,false,true,17,0));
    }

    @Test void invalidOrMissingSourceNeverStartsNewTransaction(){
        for(int source:new int[]{-2,-1,36,40})
            assertEquals(AutoEatExecutionPlanner.Path.NONE,
                    AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.PROTECT_CRITICAL,false,false,true,source,0));
    }

    @Test void eligibleSelectedMainhandAlwaysWinsOverOffhandRegardlessOfPolicy(){
        for(AutoEatOffhandPolicy policy:AutoEatOffhandPolicy.values()){
            // Even a valid edible offhand stack does not displace already held food.
            assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR,
                    AutoEatExecutionPlanner.plan(policy,true,false,true,5,5));
            // Never swap held food into the offhand, including when offhand is empty.
            assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR,
                    AutoEatExecutionPlanner.plan(policy,false,true,false,0,0));
            assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR,
                    AutoEatExecutionPlanner.plan(policy,false,false,false,8,8));
        }
    }

    @Test void anotherHotbarFoodDoesNotSkipOffhandPolicy(){
        assertEquals(AutoEatExecutionPlanner.Path.SWAP_TO_OFFHAND,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.ALLOW_REPLACE,false,false,true,6,1));
        assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.NEVER_REPLACE,false,false,true,6,1));
    }

    @Test void v263DefaultHotbarUseNeverSendsAnOffhandSwap(){
        for(AutoEatOffhandPolicy policy:new AutoEatOffhandPolicy[]{
                AutoEatOffhandPolicy.PROTECT_CRITICAL,AutoEatOffhandPolicy.NEVER_REPLACE}){
            for(boolean empty:new boolean[]{true,false})
                for(boolean critical:new boolean[]{true,false})
                    assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_HOTBAR,
                            AutoEatExecutionPlanner.plan(policy,false,empty,critical,6,1,true));
            assertEquals(AutoEatExecutionPlanner.Path.MAIN_HAND_STAGED,
                    AutoEatExecutionPlanner.plan(policy,false,false,true,16,1,true));
        }
        // An explicit ALLOW_REPLACE continues to opt into the old SWAP path.
        assertEquals(AutoEatExecutionPlanner.Path.SWAP_TO_OFFHAND,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.ALLOW_REPLACE,
                        false,false,false,6,1,false));
        // Already edible offhand must still use the offhand directly.
        assertEquals(AutoEatExecutionPlanner.Path.USE_OFFHAND_DIRECT,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.PROTECT_CRITICAL,
                        true,false,false,-1,1,true));
        // A main-inventory-only source cannot be used as a safe mainhand hotbar source.
        assertEquals(AutoEatExecutionPlanner.Path.SWAP_TO_OFFHAND,
                AutoEatExecutionPlanner.plan(AutoEatOffhandPolicy.PROTECT_CRITICAL,
                        false,true,false,16,1,true));
    }

    @Test void hotbarFirstOnlyWhenAvoidingOffhandSwap(){
        java.util.function.IntPredicate eligible=index->index==6||index==16;
        assertEquals(16,SilentAutoEatRules.selectFoodSource(1,eligible));
        assertEquals(16,SilentAutoEatRules.selectFoodSource(1,eligible,false));
        assertEquals(6,SilentAutoEatRules.selectFoodSource(1,eligible,true));
        assertEquals(Integer.MIN_VALUE,
                SilentAutoEatRules.selectFoodSource(1,index->false,true));
        // Previously held food is always handled before this fallback selector.
    }

    @Test void policyCycleAndInvalidSavedValueAreDeterministic(){
        assertEquals(AutoEatOffhandPolicy.NEVER_REPLACE,
                AutoEatOffhandPolicy.PROTECT_CRITICAL.cycle(true));
        assertEquals(AutoEatOffhandPolicy.ALLOW_REPLACE,
                AutoEatOffhandPolicy.PROTECT_CRITICAL.cycle(false));
        assertEquals(AutoEatOffhandPolicy.PROTECT_CRITICAL,
                AutoEatOffhandPolicy.ALLOW_REPLACE.cycle(true));
        assertEquals(AutoEatOffhandPolicy.PROTECT_CRITICAL,
                AutoEatOffhandPolicy.ALLOW_REPLACE.fromString("unrecognized"));
        assertEquals(AutoEatOffhandPolicy.NEVER_REPLACE,
                AutoEatOffhandPolicy.PROTECT_CRITICAL.fromString("NEVER_REPLACE"));
    }
}
