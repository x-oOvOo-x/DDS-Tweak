package ddstweak.gui.itempicker;

//#if MC >= 12111
import ddstweak.feature.dropconfirmation.ItemRegistryCompat;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;

/**
 * Representative icons for sound events; a shared sound is not proof of a unique block source.
 * 为声音提供示意图标，不推断唯一发声方块。
 */
public final class SoundPickerIcons {
    private SoundPickerIcons(){}

    // Registry IDs remain stable when Minecraft changes how copper item families are exposed.
    private static final Map<String,Item> COPPER_ICONS=loadCopperIcons();

    private static Map<String,Item> loadCopperIcons(){
        Map<String,Item> icons=new HashMap<>();
        for(Item item:ItemRegistryCompat.allItems()){
            String id=ItemRegistryCompat.id(item);
            if(id.equals("minecraft:copper_bulb")||id.equals("minecraft:copper_door")||
                    id.equals("minecraft:copper_trapdoor"))icons.put(id,item);
        }
        return icons;
    }

    private static Item copperIcon(String path){
        return COPPER_ICONS.getOrDefault("minecraft:"+path,Items.MUSIC_DISC_13);
    }

    public static ItemStack icon(String id){return ItemRegistryCompat.stack(representative(id));}

    /** Source family display, not an assertion of exact block identity. */
    public static String sourceLabel(String soundId){
        if(soundId.equals("event:1000")||soundId.equals("event:1001")||
                soundId.equals("event:1002"))return tr("dds-tweak.sound.source.dispenser");
        String path=soundId.substring(soundId.indexOf(':')+1);
        if(path.startsWith("block.")){
            String source=path.substring(6).split("\\.",2)[0];
            switch(source){
                case "piston":return tr("dds-tweak.sound.source.piston");
                case "wooden_door":case "nether_wood_door":
                    return tr("dds-tweak.sound.source.wooden_door");
                case "wooden_trapdoor":case "nether_wood_trapdoor":
                    return tr("dds-tweak.sound.source.wooden_trapdoor");
                case "fence_gate":case "nether_wood_fence_gate":
                    return tr("dds-tweak.sound.source.fence_gate");
                case "wooden_button":case "nether_wood_button":
                    return tr("dds-tweak.sound.source.wooden_button");
                case "wooden_pressure_plate":case "nether_wood_pressure_plate":
                    return tr("dds-tweak.sound.source.wooden_pressure_plate");
            }
        }
        Item item=representative(soundId);
        return item==Items.MUSIC_DISC_13?tr("dds-tweak.sound.source.other"):ItemRegistryCompat.name(item);
    }

    private static Item representative(String id){
        if(id.equals("event:1000")||id.equals("event:1001")||id.equals("event:1002"))return Items.DISPENSER;
        String path=id.substring(id.indexOf(':')+1);
        if(!path.startsWith("block."))return Items.MUSIC_DISC_13;
        String sound=path.substring("block.".length());
        int dot=sound.indexOf('.');
        if(dot<0)return Items.MUSIC_DISC_13;
        String source=sound.substring(0,dot);
        switch(source){
            case "piston":return Items.PISTON;
            case "crafter":return Items.CRAFTER;
            case "lever":return Items.LEVER;
            case "tripwire":return Items.TRIPWIRE_HOOK;
            case "copper_bulb":return copperIcon("copper_bulb");
            case "sculk_sensor":return Items.SCULK_SENSOR;
            case "chest":return Items.CHEST;
            case "bell":return Items.BELL;
            case "water":return Items.WATER_BUCKET;
            case "lava":return Items.LAVA_BUCKET;
            case "amethyst_block":return Items.AMETHYST_BLOCK;
            case "fence_gate":return Items.OAK_FENCE_GATE;
            case "metal_pressure_plate":return Items.HEAVY_WEIGHTED_PRESSURE_PLATE;
            case "wooden_pressure_plate":return Items.OAK_PRESSURE_PLATE;
            case "stone_pressure_plate":return Items.STONE_PRESSURE_PLATE;
            case "wooden_button":return Items.OAK_BUTTON;
            case "stone_button":return Items.STONE_BUTTON;
            case "wooden_door":return Items.OAK_DOOR;
            case "wooden_trapdoor":return Items.OAK_TRAPDOOR;
            case "iron_door":return Items.IRON_DOOR;
            case "iron_trapdoor":return Items.IRON_TRAPDOOR;
        }
        if(source.startsWith("note_block"))return Items.NOTE_BLOCK;
        if(source.endsWith("_button")){
            if(source.startsWith("bamboo"))return Items.BAMBOO_BUTTON;
            if(source.startsWith("cherry"))return Items.CHERRY_BUTTON;
            return Items.CRIMSON_BUTTON;
        }
        if(source.endsWith("_pressure_plate")){
            if(source.startsWith("bamboo"))return Items.BAMBOO_PRESSURE_PLATE;
            if(source.startsWith("cherry"))return Items.CHERRY_PRESSURE_PLATE;
            return Items.CRIMSON_PRESSURE_PLATE;
        }
        if(source.endsWith("_trapdoor")){
            if(source.startsWith("bamboo"))return Items.BAMBOO_TRAPDOOR;
            if(source.startsWith("cherry"))return Items.CHERRY_TRAPDOOR;
            if(source.startsWith("copper"))return copperIcon("copper_trapdoor");
            return Items.CRIMSON_TRAPDOOR;
        }
        if(source.endsWith("_door")){
            if(source.startsWith("bamboo"))return Items.BAMBOO_DOOR;
            if(source.startsWith("cherry"))return Items.CHERRY_DOOR;
            if(source.startsWith("copper"))return copperIcon("copper_door");
            return Items.CRIMSON_DOOR;
        }
        if(source.endsWith("_fence_gate")){
            if(source.startsWith("bamboo"))return Items.BAMBOO_FENCE_GATE;
            if(source.startsWith("cherry"))return Items.CHERRY_FENCE_GATE;
            return Items.CRIMSON_FENCE_GATE;
        }
        return Items.MUSIC_DISC_13;
    }

    private static String tr(String key){return MinecraftCompat.translatable(key).getString();}
}
//#endif
