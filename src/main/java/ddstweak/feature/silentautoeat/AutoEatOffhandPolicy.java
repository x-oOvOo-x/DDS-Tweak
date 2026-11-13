package ddstweak.feature.silentautoeat;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * Controls whether Auto Eat may temporarily remove a real offhand item.
 * 控制自动进食是否允许临时移动副手物品。
 */
public enum AutoEatOffhandPolicy implements IConfigOptionListEntry {
    PROTECT_CRITICAL("protect_critical"),
    NEVER_REPLACE("never_replace"),
    ALLOW_REPLACE("allow_replace");

    private final String value;

    AutoEatOffhandPolicy(String value){this.value=value;}

    @Override public String getStringValue(){return value;}

    @Override public String getDisplayName(){
        return StringUtils.translate("dds-tweak.auto_eat.offhand_policy."+value);
    }

    @Override public IConfigOptionListEntry cycle(boolean forward){
        AutoEatOffhandPolicy[] values=values();
        return values[(ordinal()+(forward?1:values.length-1))%values.length];
    }

    @Override public IConfigOptionListEntry fromString(String value){
        for(AutoEatOffhandPolicy policy:values())
            if(policy.value.equalsIgnoreCase(value))return policy;
        return PROTECT_CRITICAL;
    }
}
