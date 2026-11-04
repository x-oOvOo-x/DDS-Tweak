package ddstweak.feature.blocksoundtracker;

//#if MC >= 12111
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** One-time, conservative conversion of legacy sound lists. Never guess shared sound owners. */
public final class BlockSoundConfigMigration {
    private BlockSoundConfigMigration(){}

    public static void migrate(JsonObject root){
        JsonElement section=root.get("Visual");
        if(section==null||!section.isJsonObject())return;
        JsonObject visual=section.getAsJsonObject();
        convert(visual,"blockSoundBlacklist","blockSoundBlockBlacklist",false);
        convert(visual,"blockSoundWhitelist","blockSoundBlockWhitelist",true);
    }

    private static void convert(JsonObject visual,String oldKey,String newKey,boolean whitelist){
        if(visual.has(newKey))return; // Repeat loads must not override saved block selections.
        JsonElement old=visual.get(oldKey);
        if(old==null||!old.isJsonArray())return;
        List<String> sounds=new ArrayList<>();
        for(JsonElement element:old.getAsJsonArray())
            if(element.isJsonPrimitive()&&element.getAsJsonPrimitive().isString())
                sounds.add(element.getAsString());

        Set<String> result=new LinkedHashSet<>();
        if(whitelist&&sounds.size()==BlockSoundDefaults.REDSTONE_WHITELIST.size()&&
                new LinkedHashSet<>(sounds).equals(new LinkedHashSet<>(BlockSoundDefaults.REDSTONE_WHITELIST))){
            result.addAll(BlockSoundBlockDefaults.REDSTONE_BLOCKS);
        }else{
            for(String sound:sounds){
                String id=unambiguousBlock(sound);
                if(id!=null)result.add(id);
            }
        }
        JsonArray converted=new JsonArray();
        for(String id:result)converted.add(id);
        visual.add(newKey,converted);
    }

    /** Shared piston/door/world-event sounds are intentionally not mapped to guessed blocks. */
    static String unambiguousBlock(String soundId){
        for(String name:new String[]{"crafter","note_block","lever","bell","iron_door","iron_trapdoor","barrel"}){
            if(soundId.startsWith("minecraft:block."+name+"."))return "minecraft:"+name;
        }
        return null;
    }
}
//#endif
