package ddstweak.feature.locatorheads;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/** User-visible options deliberately parallel Locator Heads without adding server-side identity rules. */
public final class LocatorOptions {
    private LocatorOptions(){}

    public enum NameMode implements IConfigOptionListEntry {
        OFF, ALWAYS, LOOKING_AT, PLAYER_LIST;
        @Override public String getStringValue(){return name().toLowerCase(java.util.Locale.ROOT);}
        @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.locator.name."+getStringValue());}
        @Override public IConfigOptionListEntry cycle(boolean forward){return values()[(ordinal()+(forward?1:values().length-1))%values().length];}
        @Override public IConfigOptionListEntry fromString(String input){for(NameMode option:values())if(option.getStringValue().equalsIgnoreCase(input))return option;return OFF;}
    }

    public enum BorderStyle implements IConfigOptionListEntry {
        TEAM, STATIC;
        @Override public String getStringValue(){return name().toLowerCase(java.util.Locale.ROOT);}
        @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.locator.border."+getStringValue());}
        @Override public IConfigOptionListEntry cycle(boolean forward){return values()[(ordinal()+(forward?1:values().length-1))%values().length];}
        @Override public IConfigOptionListEntry fromString(String input){for(BorderStyle option:values())if(option.getStringValue().equalsIgnoreCase(input))return option;return TEAM;}
    }

    public enum BorderThickness implements IConfigOptionListEntry {
        THIN(0.3f), MEDIUM(0.5f), NORMAL(1.0f), THICK(2.0f);
        private final float pixels;
        BorderThickness(float pixels){this.pixels=pixels;}
        public float pixels(){return pixels;}
        @Override public String getStringValue(){return name().toLowerCase(java.util.Locale.ROOT);}
        @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.locator.thickness."+getStringValue());}
        @Override public IConfigOptionListEntry cycle(boolean forward){return values()[(ordinal()+(forward?1:values().length-1))%values().length];}
        @Override public IConfigOptionListEntry fromString(String input){for(BorderThickness option:values())if(option.getStringValue().equalsIgnoreCase(input))return option;return NORMAL;}
    }

    public enum PlayerFilter implements IConfigOptionListEntry {
        ALL, INCLUDE, EXCLUDE;
        @Override public String getStringValue(){return name().toLowerCase(java.util.Locale.ROOT);}
        @Override public String getDisplayName(){return StringUtils.translate("dds-tweak.locator.filter."+getStringValue());}
        @Override public IConfigOptionListEntry cycle(boolean forward){return values()[(ordinal()+(forward?1:values().length-1))%values().length];}
        @Override public IConfigOptionListEntry fromString(String input){for(PlayerFilter option:values())if(option.getStringValue().equalsIgnoreCase(input))return option;return ALL;}
    }
}
