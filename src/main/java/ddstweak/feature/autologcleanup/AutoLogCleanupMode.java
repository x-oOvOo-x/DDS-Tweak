package ddstweak.feature.autologcleanup;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum AutoLogCleanupMode implements IConfigOptionListEntry {
    DAYS("days"),
    LIMIT("limit");

    private final String id;
    AutoLogCleanupMode(String id){this.id=id;}

    @Override public String getStringValue(){return id;}
    @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.auto_log_cleanup.mode."+id);}
    @Override public IConfigOptionListEntry cycle(boolean forward){
        AutoLogCleanupMode[] values=values();
        int next=(ordinal()+(forward?1:-1)+values.length)%values.length;
        return values[next];
    }
    @Override public IConfigOptionListEntry fromString(String value){
        if(value!=null)for(AutoLogCleanupMode mode:values())if(mode.id.equalsIgnoreCase(value))return mode;
        return DAYS;
    }
}
