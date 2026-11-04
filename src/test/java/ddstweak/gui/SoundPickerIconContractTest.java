package ddstweak.gui;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class SoundPickerIconContractTest {
    private static String read(String path)throws Exception{return Files.readString(Path.of(path));}

    @Test void copperIconsResolveFromRegistryAcrossMinecraftVersions()throws Exception {
        String icon=read("src/main/java/ddstweak/gui/itempicker/SoundPickerIcons.java");
        assertTrue(icon.contains("COPPER_ICONS=loadCopperIcons()"));
        assertTrue(icon.contains("ItemRegistryCompat.allItems()"));
        assertTrue(icon.contains("ItemRegistryCompat.id(item)"));
        for(String id:new String[]{"copper_bulb","copper_door","copper_trapdoor"}){
            assertTrue(icon.contains("\"minecraft:"+id+"\""),"Registered icon: "+id);
            assertTrue(icon.contains("copperIcon(\""+id+"\")"),"Icon selection: "+id);
        }
        assertFalse(icon.contains("Items.COPPER_"),
                "Copper item families changed shape; never depend on their versioned fields");
    }

    @Test void soundsHaveSourceFamilyIconsNotActionBadges()throws Exception {
        String icon=read("src/main/java/ddstweak/gui/itempicker/SoundPickerIcons.java");
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(icon.contains("return Items.PISTON;"));
        assertTrue(icon.contains("return Items.CRAFTER;"));
        assertTrue(icon.contains("return Items.DISPENSER;"));
        assertTrue(icon.contains("return Items.NOTE_BLOCK;"));
        assertTrue(icon.contains("return Items.MUSIC_DISC_13;"));
        assertTrue(icon.contains("sourceLabel(String soundId)"));
        assertTrue(picker.contains("SoundPickerIcons.icon(sounds.get(0))"));
        assertTrue(picker.contains("SoundPickerIcons.sourceLabel(group.getValue().get(0))"));
        assertTrue(picker.contains("dds-tweak.sound.source.group_count"));
        assertFalse(picker.contains("SoundPickerIcons.badge("));
        assertTrue(picker.contains("if(kind==ItemPickerKind.SOUND)return null;"),
                "Sound picker must not display unrelated creative item tabs");
    }

}
