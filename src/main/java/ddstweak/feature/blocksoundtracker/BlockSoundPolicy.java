package ddstweak.feature.blocksoundtracker;

//#if MC >= 12111
import java.util.List;

/**
 * Pure filtering and classification rules. No world or renderer dependency.
 * 独立判断声音类型及是否通过过滤。
 */
public final class BlockSoundPolicy {
    private BlockSoundPolicy(){}

    public static boolean accepted(String id,BlockSoundFilterMode mode,
                                   List<String> blacklist,List<String> whitelist){
        if(mode==BlockSoundFilterMode.ALL)return true;
        if(id==null||id.isEmpty())return mode==BlockSoundFilterMode.BLACKLIST;
        return mode==BlockSoundFilterMode.BLACKLIST?!blacklist.contains(id):whitelist.contains(id);
    }

    /** Confirmed block events share a marker; unresolved sounds retain separate identities. */
    public static String markerSourceKey(String soundId,String blockId){
        return blockId!=null?"block:"+blockId:"sound:"+soundId;
    }

    public static boolean isBlockSound(String id,boolean blocksCategory,boolean relative){
        if(relative||id==null)return false;
        String path=id.substring(id.indexOf(':')+1);
        if(path.startsWith("entity.")||path.startsWith("item.")||path.startsWith("music.")||
                path.startsWith("ambient.")||path.startsWith("event."))return false;
        return path.startsWith("block.")||blocksCategory;
    }

    public static boolean isDispenserWorldEvent(int type){
        return type==1000||type==1001||type==1002;
    }

    public static boolean isWorldEventSoundDuplicate(String id){
        return id!=null&&(id.equals("minecraft:block.dispenser.dispense")||
                id.equals("minecraft:block.dispenser.fail")||
                id.equals("minecraft:block.dispenser.launch"));
    }
}
//#endif
