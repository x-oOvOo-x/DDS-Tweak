package ddstweak.feature.blocksoundtracker;

//#if MC >= 12111
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * Defines how sound whitelist and blacklist rules are applied.
 * 定义声音黑白名单的过滤模式。
 */
public enum BlockSoundFilterMode implements IConfigOptionListEntry {
    ALL("all"), BLACKLIST("blacklist"), WHITELIST("whitelist");

    private final String id;

    BlockSoundFilterMode(String id){this.id=id;}

    @Override public String getStringValue(){return id;}
    @Override public String getDisplayName(){
        return StringUtils.translate("dds-tweak.block_sound.mode."+id);
    }
    @Override public IConfigOptionListEntry cycle(boolean forward){
        BlockSoundFilterMode[] modes=values();
        return modes[(ordinal()+(forward?1:modes.length-1))%modes.length];
    }
    @Override public IConfigOptionListEntry fromString(String value){
        for(BlockSoundFilterMode mode:values())if(mode.id.equalsIgnoreCase(value))return mode;
        return WHITELIST;
    }
}
//#endif
