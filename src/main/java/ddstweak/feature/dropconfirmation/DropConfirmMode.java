package ddstweak.feature.dropconfirmation;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum DropConfirmMode implements IConfigOptionListEntry {
    ALL("all"),
    WHITELIST("whitelist"),
    BLACKLIST("blacklist");

    private final String id;
    DropConfirmMode(String id){this.id=id;}

    @Override public String getStringValue(){return id;}
    @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.drop_confirm.mode."+id);}
    @Override public IConfigOptionListEntry cycle(boolean forward){
        DropConfirmMode[] values=values();
        int next=(ordinal()+(forward?1:-1)+values.length)%values.length;
        return values[next];
    }
    @Override public IConfigOptionListEntry fromString(String value){
        if(value!=null)for(DropConfirmMode mode:values())if(mode.id.equalsIgnoreCase(value))return mode;
        return ALL;
    }
}
