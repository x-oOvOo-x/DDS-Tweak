package ddstweak.feature.locatorheads;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Output-level coverage of the production preview-config migration, without a game client. */
final class LocatorConfigMigrationTest {
    @Test void migratesOldSizeAndRgbThenRemainsStableOnSecondLoad(){
        JsonObject root=JsonParser.parseString("""
                {"Generic":{"other":true},"Visual":{
                  "locatorHeadSize":5,
                  "locatorBorderColor":16711680,
                  "locatorCompassColor":255,
                  "locatorHeads":false,
                  "anotherSetting":"preserved"
                }}
                """).getAsJsonObject();
        LocatorConfigMigration.migrate(root);
        JsonObject visual=root.getAsJsonObject("Visual");
        assertEquals(100,visual.get("locatorHeadSize").getAsInt());
        assertEquals("#FFFF0000",visual.get("locatorBorderColor").getAsString());
        assertEquals("#FF0000FF",visual.get("locatorCompassColor").getAsString());
        assertFalse(visual.get("locatorHeads").getAsBoolean());
        assertEquals("preserved",visual.get("anotherSetting").getAsString());
        assertTrue(root.getAsJsonObject("Generic").get("other").getAsBoolean());
        String once=root.toString();
        LocatorConfigMigration.migrate(root);
        assertEquals(once,root.toString(),"Repeated loads must not migrate twice");
    }

    @Test void preservesModernValuesAndUnrelatedSections(){
        JsonObject root=JsonParser.parseString("""
                {"Visual":{"locatorHeadSize":101,"locatorBorderColor":"#80112233",
                  "locatorCompassColor":"#00123456","locatorOuterLayer":true},
                 "Tweaks":{"keep":"yes"}}
                """).getAsJsonObject();
        String original=root.toString();
        LocatorConfigMigration.migrate(root);
        assertEquals(original,root.toString());
    }

    @Test void preservesDisplayedPercentagesAndLeavesDefaultToConfig(){
        JsonObject saved=new JsonObject();
        JsonObject visual=new JsonObject();
        visual.addProperty("locatorHeadSize",100);
        saved.add("Visual",visual);
        LocatorConfigMigration.migrate(saved);
        assertEquals(100,visual.get("locatorHeadSize").getAsInt(),
                "Existing 100% UI setting must remain unchanged");

        JsonObject fresh=new JsonObject();
        JsonObject emptyVisual=new JsonObject();
        fresh.add("Visual",emptyVisual);
        LocatorConfigMigration.migrate(fresh);
        assertFalse(emptyVisual.has("locatorHeadSize"),
                "Missing size uses the UI configuration's 100% default");
    }

    @Test void reproducesAllLegacySizeBuckets(){
        int[] oldSizes={1,2,3,4,5,6,7,8,9};
        int[] expected={50,60,70,80,100,110,120,130,150};
        for(int i=0;i<oldSizes.length;i++){
            JsonObject root=new JsonObject();
            JsonObject visual=new JsonObject();
            visual.addProperty("locatorHeadSize",oldSizes[i]);
            root.add("Visual",visual);
            LocatorConfigMigration.migrate(root);
            assertEquals(expected[i],visual.get("locatorHeadSize").getAsInt(),
                    "Legacy size "+oldSizes[i]);
        }
    }

    @Test void ignoresInvalidOrAbsentValues(){
        JsonObject root=JsonParser.parseString("""
                {"Visual":{"locatorHeadSize":"not-a-number",
                   "locatorBorderColor":null,"locatorCompassColor":{}},
                 "Unrelated":{"size":9}}
                """).getAsJsonObject();
        String original=root.toString();
        LocatorConfigMigration.migrate(root);
        assertEquals(original,root.toString());
        LocatorConfigMigration.migrate(new JsonObject());
    }
}
