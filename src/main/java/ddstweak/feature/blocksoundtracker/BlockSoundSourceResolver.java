package ddstweak.feature.blocksoundtracker;

//#if MC >= 12111
/**
 * Conservative sound-to-block attribution. A matching location alone is not evidence:
 * the emitted sound family must also be compatible with the actual block registry ID.
 */
public final class BlockSoundSourceResolver {
    private BlockSoundSourceResolver(){}

    public static boolean atBlockCenter(double x,double y,double z,int bx,int by,int bz){
        return Math.abs(x-bx-0.5)<0.2&&Math.abs(y-by-0.5)<0.2&&Math.abs(z-bz-0.5)<0.2;
    }

    public static boolean matches(String soundId,String blockId){
        if(soundId==null||blockId==null)return false;
        int sc=soundId.indexOf(':'),bc=blockId.indexOf(':');
        if(sc<0||bc<0||!soundId.substring(0,sc).equals(blockId.substring(0,bc)))return false;
        String sound=soundId.substring(sc+1),block=blockId.substring(bc+1);
        if(!sound.startsWith("block."))return false;
        int end=sound.indexOf('.',6);
        if(end<0)return false;
        String family=sound.substring(6,end);
        switch(family){
            case "piston":return block.equals("piston")||block.equals("sticky_piston");
            case "note_block":case "crafter":case "lever":case "bell":
            case "barrel":case "copper_bulb":case "iron_door":case "iron_trapdoor":
                return block.equals(family)||family.equals("copper_bulb")&&block.endsWith("_copper_bulb");
            case "chest":return block.equals("chest")||block.equals("trapped_chest");
            case "sculk_sensor":return block.equals("sculk_sensor")||block.equals("calibrated_sculk_sensor");
            case "tripwire":return block.equals("tripwire")||block.equals("tripwire_hook");
            case "metal_pressure_plate":return block.equals("light_weighted_pressure_plate")||block.equals("heavy_weighted_pressure_plate");
            case "stone_button":return block.equals("stone_button")||block.equals("polished_blackstone_button");
            case "stone_pressure_plate":return block.equals("stone_pressure_plate")||block.equals("polished_blackstone_pressure_plate");
            case "wooden_button":return wooden(block,"_button");
            case "wooden_pressure_plate":return wooden(block,"_pressure_plate");
            case "wooden_door":return wooden(block,"_door");
            case "wooden_trapdoor":return wooden(block,"_trapdoor");
            case "nether_wood_button":return netherWood(block,"_button");
            case "nether_wood_pressure_plate":return netherWood(block,"_pressure_plate");
            case "nether_wood_door":return netherWood(block,"_door");
            case "nether_wood_trapdoor":return netherWood(block,"_trapdoor");
            case "nether_wood_fence_gate":return netherWood(block,"_fence_gate");
            case "bamboo_wood_button":return block.equals("bamboo_button");
            case "bamboo_wood_pressure_plate":return block.equals("bamboo_pressure_plate");
            case "bamboo_wood_door":return block.equals("bamboo_door");
            case "bamboo_wood_trapdoor":return block.equals("bamboo_trapdoor");
            case "bamboo_wood_fence_gate":return block.equals("bamboo_fence_gate");
            case "cherry_wood_button":return block.equals("cherry_button");
            case "cherry_wood_pressure_plate":return block.equals("cherry_pressure_plate");
            case "cherry_wood_door":return block.equals("cherry_door");
            case "cherry_wood_trapdoor":return block.equals("cherry_trapdoor");
            case "cherry_wood_fence_gate":return block.equals("cherry_fence_gate");
            case "copper_door":return block.equals("copper_door")||block.endsWith("_copper_door");
            case "copper_trapdoor":return block.equals("copper_trapdoor")||block.endsWith("_copper_trapdoor");
            case "fence_gate":return wooden(block,"_fence_gate");
            default:return family.equals(block);
        }
    }

    private static boolean netherWood(String block,String suffix){
        return block.equals("crimson"+suffix)||block.equals("warped"+suffix);
    }
    private static boolean wooden(String block,String suffix){
        if(!block.endsWith(suffix)||netherWood(block,suffix)||
                block.equals("bamboo"+suffix)||block.equals("cherry"+suffix))return false;
        if(suffix.equals("_door")||suffix.equals("_trapdoor"))
            return !block.equals("iron"+suffix)&&!block.contains("copper");
        return !block.contains("copper")&&!block.contains("iron")&&!block.contains("stone");
    }
}
//#endif
