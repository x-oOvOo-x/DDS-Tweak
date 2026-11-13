package ddstweak.feature.shulkerrestock;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * Exact registry-ID filters; no implicit food tags or wildcard matching.
 * 定义客户端补货筛选模式。
 */
public enum ShulkerRestockFilterMode implements IConfigOptionListEntry {
    ALL("all"), WHITELIST("whitelist"), BLACKLIST("blacklist");
    private final String value;
    ShulkerRestockFilterMode(String value){this.value=value;}
    @Override public String getStringValue(){return value;}
    @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.shulker_restock.mode."+value);}
    @Override public IConfigOptionListEntry cycle(boolean forward){
        ShulkerRestockFilterMode[] modes=values();
        return modes[(ordinal()+(forward?1:modes.length-1))%modes.length];
    }
    @Override public IConfigOptionListEntry fromString(String value){
        for(ShulkerRestockFilterMode mode:values())if(mode.value.equalsIgnoreCase(value))return mode;
        return WHITELIST;
    }
}
