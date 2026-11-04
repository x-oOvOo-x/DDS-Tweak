package ddstweak.gui;

import ddstweak.gui.itempicker.SoundSourceGroups;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class SoundSourceGroupsTest {
    @Test void mergesOperationSoundsAndNoteBlockInstruments() {
        List<String> sounds=List.of(
                "minecraft:block.piston.extend",
                "minecraft:block.piston.contract",
                "minecraft:block.note_block.harp",
                "minecraft:block.note_block.imitate.creeper",
                "event:1000","event:1001","event:1002");
        Map<String,List<String>> groups=SoundSourceGroups.group(sounds,List.of());
        assertEquals(3,groups.size());
        assertEquals(List.of("minecraft:block.piston.extend","minecraft:block.piston.contract"),
                groups.get("minecraft:block.piston"));
        assertEquals(2,groups.get("minecraft:block.note_block").size());
        assertEquals(3,groups.get("event:dispenser_dropper").size());
    }

    @Test void addingAGroupIncludesAllSoundsAndRemovingClearsWholeGroup() {
        List<String> group=List.of("minecraft:block.piston.extend","minecraft:block.piston.contract");
        List<String> initial=List.of(group.get(0),"minecraft:block.lever.click");
        Map<String,List<String>> choices=SoundSourceGroups.group(group,initial);
        assertEquals(List.of("minecraft:block.piston","minecraft:block.lever"),
                SoundSourceGroups.selectedKeys(choices,initial));
        assertEquals(List.of(group.get(0),"minecraft:block.lever.click",group.get(1)),
                SoundSourceGroups.toggle(initial,group,true));
        assertEquals(List.of("minecraft:block.lever.click"),
                SoundSourceGroups.toggle(initial,group,false));
        assertEquals(List.of("minecraft:block.lever.click"),
                SoundSourceGroups.toggle(SoundSourceGroups.toggle(initial,group,true),group,false));
    }

    @Test void unknownSavedIdsAreVisibleAndUnrelatedSourcesDoNotCollapse() {
        List<String> unknown=List.of("modded:machine.start","modded:machine.stop");
        Map<String,List<String>> groups=SoundSourceGroups.group(
                List.of("minecraft:block.stone_button.click_on","minecraft:block.wooden_button.click_on"),unknown);
        assertTrue(groups.containsKey("modded:machine.start"));
        assertTrue(groups.containsKey("modded:machine.stop"));
        assertNotEquals(SoundSourceGroups.key("minecraft:block.stone_button.click_on"),
                SoundSourceGroups.key("minecraft:block.wooden_button.click_on"));
        assertEquals(2,SoundSourceGroups.selectedKeys(groups,unknown).size());
    }
}
