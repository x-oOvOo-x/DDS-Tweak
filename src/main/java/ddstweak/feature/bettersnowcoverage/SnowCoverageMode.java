package ddstweak.feature.bettersnowcoverage;

//#if MC >= 12111
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum SnowCoverageMode implements IConfigOptionListEntry {
    PREFER_SNOW("prefer_snow"),
    ALL_SIDES("all_sides");

    private final String id;

    SnowCoverageMode(String id){this.id=id;}

    @Override public String getStringValue(){return id;}
    @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.better_snow.mode."+id);}
    @Override public IConfigOptionListEntry cycle(boolean forward){
        SnowCoverageMode[] values=values();
        int next=(ordinal()+(forward?1:-1)+values.length)%values.length;
        return values[next];
    }
    @Override public IConfigOptionListEntry fromString(String value){
        if(value!=null)for(SnowCoverageMode mode:values())if(mode.id.equalsIgnoreCase(value))return mode;
        return PREFER_SNOW;
    }
}
//#endif
