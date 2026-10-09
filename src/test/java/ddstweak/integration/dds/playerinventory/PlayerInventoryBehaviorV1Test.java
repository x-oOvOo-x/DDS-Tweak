package ddstweak.integration.dds.playerinventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PlayerInventoryBehaviorV1Test {
    @Test void intervalRangeIsTheFullPositiveIntDomain(){
        assertEquals(1,PlayerInventoryBehaviorV1.clampInterval(Integer.MIN_VALUE));
        assertEquals(1,PlayerInventoryBehaviorV1.clampInterval(-1));
        assertEquals(1,PlayerInventoryBehaviorV1.clampInterval(0));
        assertEquals(1,PlayerInventoryBehaviorV1.clampInterval(1));
        assertEquals(12000,PlayerInventoryBehaviorV1.clampInterval(12000));
        assertEquals(12001,PlayerInventoryBehaviorV1.clampInterval(12001));
        assertEquals(Integer.MAX_VALUE,PlayerInventoryBehaviorV1.clampInterval(Integer.MAX_VALUE));
    }
}
