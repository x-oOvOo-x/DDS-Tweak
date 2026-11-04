package ddstweak.feature;

import ddstweak.feature.blocksoundtracker.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

final class BlockSoundPolicyTest {
    @Test void filtersUseActualBlockIdsAndTreatUnknownConservatively(){
        String dispenser="minecraft:dispenser",dropper="minecraft:dropper";
        assertTrue(BlockSoundPolicy.accepted(dispenser,BlockSoundFilterMode.ALL,List.of(dispenser),List.of()));
        assertTrue(BlockSoundPolicy.accepted(null,BlockSoundFilterMode.ALL,List.of(),List.of()));
        assertFalse(BlockSoundPolicy.accepted(dispenser,BlockSoundFilterMode.BLACKLIST,List.of(dispenser),List.of()));
        assertTrue(BlockSoundPolicy.accepted(dropper,BlockSoundFilterMode.BLACKLIST,List.of(dispenser),List.of()));
        assertTrue(BlockSoundPolicy.accepted(null,BlockSoundFilterMode.BLACKLIST,List.of(dispenser),List.of()));
        assertTrue(BlockSoundPolicy.accepted(dispenser,BlockSoundFilterMode.WHITELIST,List.of(),List.of(dispenser)));
        assertFalse(BlockSoundPolicy.accepted(dropper,BlockSoundFilterMode.WHITELIST,List.of(),List.of(dispenser)));
        assertFalse(BlockSoundPolicy.accepted(null,BlockSoundFilterMode.WHITELIST,List.of(),List.of(dispenser)));
        assertFalse(BlockSoundPolicy.accepted("minecraft:piston",BlockSoundFilterMode.WHITELIST,List.of(),List.of()));
    }

    @Test void redstoneDefaultWhitelistUsesBlockRegistryIdsNotSoundEvents()throws Exception {
        String config=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/ddstweak/config/Configs.java"));
        assertTrue(config.contains("optionList(\"blockSoundFilterMode\",BlockSoundFilterMode.WHITELIST,VISUAL_KEY)"));
        assertTrue(config.contains("stringList(\"blockSoundBlockWhitelist\",BlockSoundBlockDefaults.REDSTONE_BLOCKS,VISUAL_KEY)"));
        List<String> ids=BlockSoundBlockDefaults.REDSTONE_BLOCKS;
        assertEquals(ids.size(),new HashSet<>(ids).size());
        for(String id:ids)assertTrue(id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"),id);
        for(String id:List.of("minecraft:dispenser","minecraft:dropper","minecraft:piston",
                "minecraft:sticky_piston","minecraft:crafter","minecraft:note_block","minecraft:lever"))
            assertTrue(ids.contains(id),id);
        assertFalse(ids.contains("minecraft:block.piston.extend"));
        assertFalse(ids.contains("event:1000"));
    }

    @Test void markerIdentityGroupsConfirmedBlockActionsButNotUnknownSounds(){
        String block="minecraft:piston";
        assertEquals(BlockSoundPolicy.markerSourceKey("minecraft:block.piston.extend",block),
                BlockSoundPolicy.markerSourceKey("minecraft:block.piston.contract",block));
        assertNotEquals(BlockSoundPolicy.markerSourceKey("minecraft:block.piston.extend",null),
                BlockSoundPolicy.markerSourceKey("minecraft:block.piston.contract",null));
        assertNotEquals(BlockSoundPolicy.markerSourceKey("minecraft:block.piston.extend",block),
                BlockSoundPolicy.markerSourceKey("minecraft:block.piston.extend",null));
        assertNotEquals(BlockSoundPolicy.markerSourceKey("minecraft:block.piston.extend","minecraft:piston"),
                BlockSoundPolicy.markerSourceKey("minecraft:block.piston.extend","minecraft:sticky_piston"));
    }

    @Test void categoryAloneMustNotMisclassifyKnownEntitySound(){
        assertTrue(BlockSoundPolicy.isBlockSound("minecraft:block.piston.extend",true,false));
        assertTrue(BlockSoundPolicy.isBlockSound("example:machine.buzz",true,false));
        assertFalse(BlockSoundPolicy.isBlockSound("minecraft:entity.zombie.step",true,false));
        assertFalse(BlockSoundPolicy.isBlockSound("minecraft:block.piston.extend",true,true));
    }

    @Test void dispenserWorldEventsRetainDedicatedIdsAndAvoidDoubleCounting(){
        for(int id=1000;id<=1002;id++)assertTrue(BlockSoundPolicy.isDispenserWorldEvent(id));
        assertFalse(BlockSoundPolicy.isDispenserWorldEvent(1010));
        assertTrue(BlockSoundPolicy.isWorldEventSoundDuplicate("minecraft:block.dispenser.launch"));
        assertFalse(BlockSoundPolicy.isWorldEventSoundDuplicate("minecraft:block.piston.extend"));
    }

    @Test void actualWorldBlockIsConsultedBeforeSoundFiltering()throws Exception {
        String tracker=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/ddstweak/feature/blocksoundtracker/BlockSoundTracker.java"));
        assertTrue(tracker.contains("BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()"));
        assertTrue(tracker.contains("BlockSoundSourceResolver.atBlockCenter("));
        assertTrue(tracker.contains("BlockSoundSourceResolver.matches(id,candidate)"));
        assertTrue(tracker.contains("record(mc,\"event:\"+type,pos,sourceBlockId)"));
        assertTrue(tracker.contains("accepted(record.sourceBlockId)"));
        assertTrue(tracker.contains("BlockSoundPolicy.markerSourceKey(id,sourceBlockId)"));
        assertFalse(tracker.contains("BlockSoundPolicy.accepted(id,"));
    }
}
