package ddstweak.feature.blocksoundtracker;

//#if MC >= 12111
import com.google.common.collect.ImmutableList;

/** Actual block registry IDs with commonly audible redstone interactions (not sound IDs). */
public final class BlockSoundBlockDefaults {
    private BlockSoundBlockDefaults(){}

    public static final ImmutableList<String> REDSTONE_BLOCKS=ImmutableList.of(
            "minecraft:dispenser","minecraft:dropper",
            "minecraft:piston","minecraft:sticky_piston","minecraft:crafter","minecraft:note_block",
            "minecraft:lever","minecraft:tripwire","minecraft:tripwire_hook",
            "minecraft:stone_button","minecraft:polished_blackstone_button",
            "minecraft:oak_button","minecraft:spruce_button","minecraft:birch_button",
            "minecraft:crimson_button","minecraft:warped_button","minecraft:bamboo_button","minecraft:cherry_button",
            "minecraft:stone_pressure_plate","minecraft:polished_blackstone_pressure_plate",
            "minecraft:oak_pressure_plate","minecraft:heavy_weighted_pressure_plate",
            "minecraft:light_weighted_pressure_plate","minecraft:crimson_pressure_plate",
            "minecraft:bamboo_pressure_plate","minecraft:cherry_pressure_plate",
            "minecraft:oak_door","minecraft:iron_door","minecraft:crimson_door",
            "minecraft:bamboo_door","minecraft:cherry_door","minecraft:copper_door",
            "minecraft:oak_trapdoor","minecraft:iron_trapdoor","minecraft:crimson_trapdoor",
            "minecraft:bamboo_trapdoor","minecraft:cherry_trapdoor","minecraft:copper_trapdoor",
            "minecraft:oak_fence_gate","minecraft:crimson_fence_gate",
            "minecraft:bamboo_fence_gate","minecraft:cherry_fence_gate",
            "minecraft:copper_bulb","minecraft:sculk_sensor","minecraft:calibrated_sculk_sensor",
            "minecraft:chest","minecraft:trapped_chest","minecraft:bell"
    );
}
//#endif
