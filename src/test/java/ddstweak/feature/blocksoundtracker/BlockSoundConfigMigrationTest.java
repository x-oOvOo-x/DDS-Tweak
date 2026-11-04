package ddstweak.feature.blocksoundtracker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BlockSoundConfigMigrationTest {
    private static JsonArray values(String...ids){
        JsonArray a=new JsonArray();
        for(String id:ids)a.add(id);
        return a;
    }
    @Test void convertsOnlyUnambiguousLegacySourcesAndPreservesOriginalLists(){
        JsonObject root=new JsonObject(),visual=new JsonObject();
        visual.add("blockSoundBlacklist",values("minecraft:block.crafter.craft",
                "minecraft:block.piston.extend","event:1000","minecraft:block.wooden_door.open"));
        visual.add("blockSoundWhitelist",values("minecraft:block.note_block.harp","minecraft:block.iron_door.close"));
        root.add("Visual",visual);
        BlockSoundConfigMigration.migrate(root);
        assertEquals(values("minecraft:crafter"),visual.getAsJsonArray("blockSoundBlockBlacklist"));
        assertEquals(values("minecraft:note_block","minecraft:iron_door"),visual.getAsJsonArray("blockSoundBlockWhitelist"));
        assertEquals(4,visual.getAsJsonArray("blockSoundBlacklist").size());
        visual.add("blockSoundBlockBlacklist",values("minecraft:dropper"));
        BlockSoundConfigMigration.migrate(root);
        assertEquals(values("minecraft:dropper"),visual.getAsJsonArray("blockSoundBlockBlacklist"));
    }
    @Test void fullUnchangedLegacyRedstoneDefaultMapsToBlockDefault(){
        JsonObject root=new JsonObject(),visual=new JsonObject();
        JsonArray ids=new JsonArray();
        for(String id:BlockSoundDefaults.REDSTONE_WHITELIST)ids.add(id);
        visual.add("blockSoundWhitelist",ids);
        root.add("Visual",visual);
        BlockSoundConfigMigration.migrate(root);
        assertEquals(BlockSoundBlockDefaults.REDSTONE_BLOCKS.size(),
                visual.getAsJsonArray("blockSoundBlockWhitelist").size());
        assertTrue(visual.getAsJsonArray("blockSoundBlockWhitelist").toString().contains("minecraft:dropper"));
    }
}
