package ddstweak.feature.clickthrough;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ClickThroughMode implements IConfigOptionListEntry {
    SMART("smart"),
    ALWAYS("always");

    private final String id;

    ClickThroughMode(String id){this.id=id;}

    @Override public String getStringValue(){return id;}
    @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.click_through.mode."+id);}
    @Override public IConfigOptionListEntry cycle(boolean forward){
        ClickThroughMode[] values=values();
        int next=(ordinal()+(forward?1:-1)+values.length)%values.length;
        return values[next];
    }
    @Override public IConfigOptionListEntry fromString(String value){
        if(value!=null)for(ClickThroughMode mode:values())if(mode.id.equalsIgnoreCase(value))return mode;
        return SMART;
    }
}
