package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class BlockSoundPickerContractTest {
    private static String source(String path)throws Exception{return Files.readString(Path.of(path));}

    @Test void soundBlackAndWhitelistSelectExactBlocksRatherThanSoundEvents()throws Exception {
        String bindings=source("src/main/java/ddstweak/gui/itempicker/ItemPickerConfigBindings.java");
        String kinds=source("src/main/java/ddstweak/gui/itempicker/ItemPickerKind.java");
        String picker=source("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String config=source("src/main/java/ddstweak/config/Configs.java");
        String mixin=source("src/main/java/ddstweak/mixin/feature/dropconfirmation/DropConfirmStringListButtonMixin.java");
        assertTrue(kinds.contains("SOUND_BLOCK"));
        for(String type:new String[]{"BLACKLIST","WHITELIST"}){
            assertTrue(bindings.contains("Configs.Visual.BLOCK_SOUND_BLOCK_"+type+", ItemPickerKind.SOUND_BLOCK"),type);
            assertTrue(config.contains("BLOCK_SOUND_BLOCK_"+type+"=MalilibCompat.stringList("),type);
        }
        assertFalse(bindings.contains("Configs.Visual.BLOCK_SOUND_WHITELIST, ItemPickerKind.SOUND"));
        assertFalse(bindings.contains("Configs.Visual.BLOCK_SOUND_BLACKLIST, ItemPickerKind.SOUND"));
        assertTrue(mixin.contains("ItemPickerConfigBindings.open(GuiUtils.getCurrentScreen(),config)"));
        assertTrue(picker.contains("if(kind==ItemPickerKind.BLOCK||kind==ItemPickerKind.SOUND_BLOCK)return item instanceof BlockItem;"));
        assertTrue(picker.contains("if((kind==ItemPickerKind.BLOCK||kind==ItemPickerKind.SOUND_BLOCK)&&item instanceof BlockItem)"));
        assertTrue(picker.contains("BuiltInRegistries.BLOCK.getKey(((BlockItem)item).getBlock()).toString()"));
        assertTrue(picker.contains("if(kind!=ItemPickerKind.BLOCK&&kind!=ItemPickerKind.SOUND_BLOCK&&kind!=ItemPickerKind.SOUND)return false;"));
        assertTrue(picker.contains("if(kind==ItemPickerKind.SOUND_BLOCK&&!byId.containsKey(id)&&!itemlessBlockIcons.containsKey(id))return false;"));
        assertTrue(picker.contains("for(Block block:BuiltInRegistries.BLOCK)"));
        assertTrue(picker.contains("itemlessBlockIcons.put(id,id.equals(\"minecraft:tripwire\")?Items.TRIPWIRE_HOOK:Items.BARRIER)"));
        assertTrue(picker.contains("if(kind==ItemPickerKind.SOUND_BLOCK&&(searching||active.isAll()))"));
        assertTrue(picker.contains("String id=filteredItemlessBlocks.get(i-filtered.size())"));
        assertTrue(picker.contains("if(id!=null){toggleId(id);refresh();return true;}"));
        assertTrue(picker.contains("if(icon!=null)stack=ItemRegistryCompat.stack(icon)"));
        assertTrue(picker.contains("showTooltip(graphics,mouseX,mouseY,ItemRegistryCompat.name(item),entryId(item),state)"));
        assertTrue(picker.contains("if(kind==ItemPickerKind.SOUND_BLOCK&&value.startsWith(\"#\"))return false;"));
        assertTrue(picker.contains("if(target!=null)new Configs().save()"));
        assertTrue(picker.contains("setMultiValues(next)"));
    }
}
