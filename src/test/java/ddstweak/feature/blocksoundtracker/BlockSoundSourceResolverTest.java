package ddstweak.feature.blocksoundtracker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BlockSoundSourceResolverTest {
    @Test void blockCenterAndFamilyMustBothMatch(){
        assertTrue(BlockSoundSourceResolver.atBlockCenter(1.5,2.5,-3.5,1,2,-4));
        assertFalse(BlockSoundSourceResolver.atBlockCenter(1.0,2.5,-3.5,1,2,-4));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.piston.extend","minecraft:piston"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.piston.contract","minecraft:sticky_piston"));
        assertFalse(BlockSoundSourceResolver.matches("minecraft:block.piston.extend","minecraft:crafter"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.crafter.craft","minecraft:crafter"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.note_block.harp","minecraft:note_block"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.stone_button.click_on","minecraft:polished_blackstone_button"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.wooden_door.open","minecraft:oak_door"));
        assertFalse(BlockSoundSourceResolver.matches("minecraft:block.wooden_door.open","minecraft:iron_door"));
        assertFalse(BlockSoundSourceResolver.matches("minecraft:block.wooden_door.open","minecraft:cherry_door"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.cherry_wood_door.close","minecraft:cherry_door"));
        assertFalse(BlockSoundSourceResolver.matches("minecraft:block.cherry_wood_door.close","minecraft:oak_door"));
    }

    @Test void unknownAndUnrelatedSoundsCannotClaimSourceBlocks(){
        assertFalse(BlockSoundSourceResolver.matches("minecraft:block.piston.extend","example:piston"));
        assertFalse(BlockSoundSourceResolver.matches("minecraft:entity.zombie.step","minecraft:piston"));
        assertFalse(BlockSoundSourceResolver.matches("minecraft:block.dispenser.dispense","minecraft:dropper"));
        assertFalse(BlockSoundSourceResolver.matches("example:block.machine.tick","minecraft:machine"));
        assertTrue(BlockSoundSourceResolver.matches("example:block.machine.tick","example:machine"));
        assertFalse(BlockSoundSourceResolver.matches(null,"minecraft:piston"));
        assertFalse(BlockSoundSourceResolver.matches("minecraft:block.piston.extend",null));
    }

    @Test void copperWeatheringAndSharedFamiliesKeepActualBlockIds(){
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.copper_bulb.turn_on","minecraft:copper_bulb"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.copper_bulb.turn_off","minecraft:waxed_oxidized_copper_bulb"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.fence_gate.open","minecraft:oak_fence_gate"));
        assertFalse(BlockSoundSourceResolver.matches("minecraft:block.fence_gate.open","minecraft:warped_fence_gate"));
        assertTrue(BlockSoundSourceResolver.matches("minecraft:block.nether_wood_fence_gate.open","minecraft:warped_fence_gate"));
    }
}
