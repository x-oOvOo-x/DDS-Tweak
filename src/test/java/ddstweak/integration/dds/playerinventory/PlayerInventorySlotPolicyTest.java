package ddstweak.integration.dds.playerinventory;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PlayerInventorySlotPolicyTest {
    @Test void gcaNormalStorageExcludesControlsAndEquipment(){
        List<Integer> ids=PlayerInventorySlotPolicy.targetStorageSlots(18,45,45,54,
                PlayerInventoryKind.INVENTORY,
                id->id<18?PlayerInventorySlotRole.CONTROL:
                    id<45?PlayerInventorySlotRole.MAIN:PlayerInventorySlotRole.HOTBAR);
        assertEquals(36,ids.size());
        assertEquals(Integer.valueOf(18),ids.get(0));
        assertEquals(Integer.valueOf(44),ids.get(26));
        assertEquals(Integer.valueOf(45),ids.get(27));
        assertEquals(Integer.valueOf(53),ids.get(35));
        assertFalse(ids.contains(7));
    }

    @Test void orgPreservesMainBeforeHotbarDespiteReversedLayout(){
        List<Integer> ids=PlayerInventorySlotPolicy.targetStorageSlots(9,36,0,9,
                PlayerInventoryKind.INVENTORY,
                id->id<9?PlayerInventorySlotRole.HOTBAR:PlayerInventorySlotRole.MAIN);
        assertEquals(36,ids.size());
        assertEquals(Integer.valueOf(9),ids.get(0));
        assertEquals(Integer.valueOf(35),ids.get(26));
        assertEquals(Integer.valueOf(0),ids.get(27));
        assertEquals(Integer.valueOf(8),ids.get(35));
    }

    @Test void enderAndControllerHaveDistinctStorageSemantics(){
        List<Integer> ids=PlayerInventorySlotPolicy.targetStorageSlots(27,54,54,54,
                PlayerInventoryKind.ENDER_CHEST,
                id->id<27?PlayerInventorySlotRole.CONTROL:PlayerInventorySlotRole.ENDER_STORAGE);
        assertEquals(27,ids.size());
        assertEquals(Integer.valueOf(27),ids.get(0));
        assertTrue(PlayerInventorySlotPolicy.targetStorageSlots(18,45,45,54,
                PlayerInventoryKind.CONTROLLER,id->PlayerInventorySlotRole.MAIN).isEmpty());
        for(PlayerInventorySlotRole role:new PlayerInventorySlotRole[]{
                PlayerInventorySlotRole.EQUIPMENT,PlayerInventorySlotRole.CONTROL,
                PlayerInventorySlotRole.DISABLED,PlayerInventorySlotRole.EXTRA,
                PlayerInventorySlotRole.PLACEHOLDER,PlayerInventorySlotRole.UNKNOWN})
            assertFalse(PlayerInventorySlotPolicy.isAutomaticStorageRole(role));
    }

    @Test void bogusRangesCannotGrantAutomaticAccess(){
        List<Integer> ids=PlayerInventorySlotPolicy.targetStorageSlots(-4,4,4,7,
                PlayerInventoryKind.INVENTORY,
                id->id==1?PlayerInventorySlotRole.MAIN:PlayerInventorySlotRole.EQUIPMENT);
        assertEquals(List.of(1),ids);
    }
}
