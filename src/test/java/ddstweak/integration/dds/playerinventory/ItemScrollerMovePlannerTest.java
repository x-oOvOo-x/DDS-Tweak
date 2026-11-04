package ddstweak.integration.dds.playerinventory;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ItemScrollerMovePlannerTest {
    private static ItemScrollerMovePlanner.SlotState slot(int id,int count,int max){
        return new ItemScrollerMovePlanner.SlotState(id,count,max,true);
    }

    @Test void mergesFirstThenFillsOnlyExplicitDestinations(){
        var p=ItemScrollerMovePlanner.plan(List.of(slot(54,64,64),slot(55,32,64)),
                List.of(slot(18,48,64),slot(19,0,64),slot(20,0,64)));
        assertNotNull(p);
        assertEquals(96,p.moved);
        assertEquals(64,Integer.valueOf(p.after.get(18)));
        assertEquals(64,Integer.valueOf(p.after.get(19)));
        assertEquals(16,Integer.valueOf(p.after.get(20)));
        assertEquals(0,Integer.valueOf(p.after.get(54)));
        assertEquals(0,Integer.valueOf(p.after.get(55)));
        assertEquals(List.of(54,18,19,55,19,20),p.clicks);
    }

    @Test void mergesExistingStacksEvenWhenAnEmptySlotAppearsFirst(){
        var p=ItemScrollerMovePlanner.plan(List.of(slot(54,16,64)),
                List.of(slot(18,0,64),slot(19,60,64),slot(20,56,64)));
        assertNotNull(p);
        assertEquals(List.of(54,19,20,18),p.clicks);
        assertEquals(64,Integer.valueOf(p.after.get(19)));
        assertEquals(64,Integer.valueOf(p.after.get(20)));
        assertEquals(4,Integer.valueOf(p.after.get(18)));
    }

    @Test void fullInventoryAndForbiddenSlotsDoNotReceiveClicks(){
        var p=ItemScrollerMovePlanner.plan(List.of(slot(54,64,64)),
                List.of(new ItemScrollerMovePlanner.SlotState(7,0,64,false),
                        slot(18,64,64)));
        assertNotNull(p);
        assertTrue(p.clicks.isEmpty());
        assertEquals(64,Integer.valueOf(p.after.get(54)));
    }

    @Test void partialCapacityReturnsRemainderToOriginalSource(){
        var p=ItemScrollerMovePlanner.plan(List.of(slot(54,32,64)),
                List.of(slot(18,60,64),slot(19,0,8)));
        assertNotNull(p);
        assertEquals(12,p.moved);
        assertEquals(20,Integer.valueOf(p.after.get(54)));
        assertEquals(List.of(54,18,19,54),p.clicks);
    }

    @Test void reversedOrgMenuIdsAndUnstackableItemsWork(){
        var p=ItemScrollerMovePlanner.plan(List.of(slot(9,1,1),slot(10,1,1)),
                List.of(slot(54,0,1),slot(55,0,1)));
        assertNotNull(p);
        assertEquals(List.of(9,54,10,55),p.clicks);
        assertEquals(2,p.moved);
    }

    @Test void refusesDuplicateOrInvalidSnapshots(){
        assertNull(ItemScrollerMovePlanner.plan(List.of(slot(1,1,64)),List.of(slot(1,0,64))));
        assertThrows(IllegalArgumentException.class,()->slot(0,5,4));
        assertEquals(0,ItemScrollerMovePlanner.plan(List.of(),List.of()).moved);
    }
}
