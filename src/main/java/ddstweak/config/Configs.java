package ddstweak.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ddstweak.DDSTweakReference;
import ddstweak.platform.malilib.MalilibCompat;
import ddstweak.feature.dropconfirmation.DropConfirmMode;
import ddstweak.feature.autologcleanup.AutoLogCleanupMode;
import ddstweak.feature.clickthrough.ClickThroughMode;
//#if MC >= 12111
import ddstweak.feature.bettersnowcoverage.SnowCoverageMode;
//#endif
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import java.nio.file.*;
import java.util.List;

public final class Configs implements IConfigHandler {
    private static final String FILE=DDSTweakReference.MOD_ID+".json",VISUAL_KEY=DDSTweakReference.MOD_ID+".config.visual",TWEAKS_KEY=DDSTweakReference.MOD_ID+".config.tweaks";
    public static final class Generic {public static final List<IConfigBase> OPTIONS=ImmutableList.of();private Generic(){}}
    public static final class Visual {
        //#if MC >= 12109
        public static final ConfigBoolean DISABLE_END_FLASH=MalilibCompat.bool("disableEndFlash",false,VISUAL_KEY);
        //#endif
        public static final ConfigBooleanHotkeyed GAMMA_OVERRIDE=MalilibCompat.boolHotkeyed("gammaOverride",false,"",VISUAL_KEY);
        public static final ConfigDouble GAMMA_OVERRIDE_VALUE=MalilibCompat.decimal("gammaOverrideValue",16.0,0.0,32.0,true,VISUAL_KEY);
        public static final ConfigBoolean PLAYER_HEAD_WINDOW_ICON=MalilibCompat.bool("playerHeadWindowIcon",false,VISUAL_KEY);
        public static final ConfigBoolean STACK_COUNT_SCALE_ENABLED=MalilibCompat.bool("stackCountScaleEnabled",false,VISUAL_KEY);
        //#if MC >= 12101
        public static final ConfigBoolean COMPACT_STATUS_EFFECTS=MalilibCompat.bool("compactStatusEffects",false,VISUAL_KEY);
        public static final ConfigBoolean COMPACT_STATUS_EFFECTS_ON_LEFT=MalilibCompat.bool("compactStatusEffectsOnLeft",false,VISUAL_KEY);
        public static final ConfigBoolean COMPACT_STATUS_EFFECTS_HOLD_TAB=MalilibCompat.bool("compactStatusEffectsHoldTab",false,VISUAL_KEY);
        //#endif
        public static final ConfigBooleanHotkeyed RAIL_POWER_PLACEMENT_GUIDE=MalilibCompat.boolHotkeyed("railPowerPlacementGuide",false,"",VISUAL_KEY);
        public static final ConfigBoolean CHUNK_CENTER_INDICATOR=MalilibCompat.bool("chunkCenterIndicator",true,VISUAL_KEY);
        public static final ConfigBoolean HIDE_BLOCK_OUTLINE=MalilibCompat.bool("hideBlockOutline",false,VISUAL_KEY);
        //#if MC >= 12111
        public static final ConfigBoolean BETTER_SNOW_COVERAGE=MalilibCompat.bool("betterSnowCoverage",false,VISUAL_KEY);
        public static final ConfigOptionList BETTER_SNOW_COVERAGE_MODE=MalilibCompat.optionList("betterSnowCoverageMode",SnowCoverageMode.PREFER_SNOW,VISUAL_KEY);
        public static final ConfigStringList BETTER_SNOW_COVERAGE_BLACKLIST=MalilibCompat.stringList("betterSnowCoverageBlacklist",ImmutableList.of("#minecraft:buttons","#minecraft:pressure_plates","#minecraft:rails","#minecraft:leaves","minecraft:wildflowers","minecraft:pink_petals","minecraft:leaf_litter"),VISUAL_KEY);
        public static final ConfigStringList BETTER_SNOW_COVERAGE_WHITELIST=MalilibCompat.stringList("betterSnowCoverageWhitelist",ImmutableList.of(),VISUAL_KEY);
        //#endif
        public static final ConfigInteger STACK_COUNT_SCALE=MalilibCompat.integer("stackCountScale",100,50,100,true,VISUAL_KEY);
        public static final List<IConfigBase> OPTIONS=ImmutableList.of(
                //#if MC >= 12109
                DISABLE_END_FLASH,
                //#endif
                GAMMA_OVERRIDE,GAMMA_OVERRIDE_VALUE,PLAYER_HEAD_WINDOW_ICON,STACK_COUNT_SCALE_ENABLED,STACK_COUNT_SCALE,
                //#if MC >= 12101
                COMPACT_STATUS_EFFECTS,COMPACT_STATUS_EFFECTS_ON_LEFT,COMPACT_STATUS_EFFECTS_HOLD_TAB,
                //#endif
                RAIL_POWER_PLACEMENT_GUIDE,CHUNK_CENTER_INDICATOR,
                //#if MC >= 12111
                BETTER_SNOW_COVERAGE,BETTER_SNOW_COVERAGE_MODE,BETTER_SNOW_COVERAGE_BLACKLIST,BETTER_SNOW_COVERAGE_WHITELIST,
                //#endif
                HIDE_BLOCK_OUTLINE);
        private Visual(){}
    }
    public static final class Tweaks {
        //#if MC >= 12106
        public static final ConfigBoolean NO_COMMAND_CONFIRM=MalilibCompat.bool("noCommandConfirm",false,TWEAKS_KEY);
        //#endif
        public static final ConfigBoolean ENHANCED_INVENTORY_GUI=MalilibCompat.bool("enhancedInventoryGui",true,TWEAKS_KEY);
        public static final ConfigBoolean CLICK_THROUGH=MalilibCompat.bool("clickThrough",false,TWEAKS_KEY);
        public static final ConfigOptionList CLICK_THROUGH_MODE=MalilibCompat.optionList("clickThroughMode",ClickThroughMode.SMART,TWEAKS_KEY);
        public static final ConfigBoolean AUTO_EAT=MalilibCompat.bool("autoEat",false,TWEAKS_KEY);
        public static final ConfigInteger AUTO_EAT_HUNGER_THRESHOLD=MalilibCompat.integer("autoEatHungerThreshold",14,1,19,true,TWEAKS_KEY);
        public static final ConfigStringList AUTO_EAT_FOOD_BLACKLIST=MalilibCompat.stringList("autoEatFoodBlacklist",ImmutableList.of(
                "minecraft:pufferfish","minecraft:rotten_flesh","minecraft:spider_eye",
                "minecraft:enchanted_golden_apple","minecraft:golden_apple"
        ),TWEAKS_KEY);
        public static final ConfigBoolean CONFIRM_ITEM_DROP=MalilibCompat.bool("confirmItemDrop",false,TWEAKS_KEY);
        public static final ConfigOptionList DROP_CONFIRM_MODE=MalilibCompat.optionList("dropConfirmMode",DropConfirmMode.ALL,TWEAKS_KEY);
        public static final ConfigStringList DROP_CONFIRM_BLACKLIST=MalilibCompat.stringList("dropConfirmBlacklist",ImmutableList.of(),TWEAKS_KEY);
        public static final ConfigStringList DROP_CONFIRM_WHITELIST=MalilibCompat.stringList("dropConfirmWhitelist",ImmutableList.of(),TWEAKS_KEY);
        private static final ConfigStringList DROP_CONFIRM_ITEMS_LEGACY=MalilibCompat.stringList("dropConfirmItems",ImmutableList.of(),TWEAKS_KEY);
        public static final ConfigBoolean DISABLE_AUTO_SLEEP_CHAT=MalilibCompat.bool("disableAutoSleepChat",false,TWEAKS_KEY);
        //#if MC >= 11502
        public static final ConfigBoolean CHAT_TOOLTIP_OCCLUSION=MalilibCompat.bool("chatTooltipOcclusion",false,TWEAKS_KEY);
        //#endif
        public static final ConfigBoolean CLIENT_STARTUP_SOUND=MalilibCompat.bool("clientStartupSound",true,TWEAKS_KEY);
        public static final ConfigBooleanHotkeyed QUICK_MUTE=MalilibCompat.boolHotkeyed("quickMute",false,"LEFT_CONTROL,M",TWEAKS_KEY);
        public static final ConfigBoolean AUTO_LOG_CLEANUP=MalilibCompat.bool("autoLogCleanup",false,TWEAKS_KEY);
        public static final ConfigOptionList AUTO_LOG_CLEANUP_MODE=MalilibCompat.optionList("autoLogCleanupMode",AutoLogCleanupMode.DAYS,TWEAKS_KEY);
        public static final ConfigInteger AUTO_LOG_KEEP_DAYS=MalilibCompat.integer("autoLogKeepDays",14,1,365,true,TWEAKS_KEY);
        public static final ConfigInteger AUTO_LOG_KEEP_LIMIT=MalilibCompat.integer("autoLogKeepLimit",10,1,100,true,TWEAKS_KEY);
        public static final ConfigBooleanHotkeyed HELPFUL_HITBOXES=MalilibCompat.boolHotkeyed("helpfulHitboxes",false,"",TWEAKS_KEY);
        public static final ConfigBoolean CAMERA_PLAYER_SELECTOR=MalilibCompat.bool("cameraPlayerSelector",true,TWEAKS_KEY);
        public static final ConfigBoolean SCROLL_BOOK_PAGES=MalilibCompat.bool("scrollBookPages",false,TWEAKS_KEY);
        public static final ConfigInteger SCROLL_BOOK_CTRL_MULTIPLIER=MalilibCompat.integer("scrollBookCtrlMultiplier",5,1,10,true,TWEAKS_KEY);
        //#if MC >= 12106
        //#endif
        public static final List<IConfigBase> OPTIONS=ImmutableList.of(
                //#if MC >= 12106
                NO_COMMAND_CONFIRM,
                //#endif
                ENHANCED_INVENTORY_GUI,CLICK_THROUGH,CLICK_THROUGH_MODE,AUTO_EAT,AUTO_EAT_HUNGER_THRESHOLD,AUTO_EAT_FOOD_BLACKLIST,CONFIRM_ITEM_DROP,DROP_CONFIRM_MODE,DROP_CONFIRM_BLACKLIST,DROP_CONFIRM_WHITELIST,DISABLE_AUTO_SLEEP_CHAT,
                //#if MC >= 11502
                CHAT_TOOLTIP_OCCLUSION,
                //#endif
                CLIENT_STARTUP_SOUND,QUICK_MUTE,HELPFUL_HITBOXES,AUTO_LOG_CLEANUP,AUTO_LOG_CLEANUP_MODE,AUTO_LOG_KEEP_DAYS,AUTO_LOG_KEEP_LIMIT,CAMERA_PLAYER_SELECTOR,SCROLL_BOOK_PAGES,SCROLL_BOOK_CTRL_MULTIPLIER
        );
        private static final List<IConfigBase> LEGACY_OPTIONS=ImmutableList.of(DROP_CONFIRM_ITEMS_LEGACY);
        private Tweaks(){}
    }
    public static final List<IConfigBase> ALL=ImmutableList.<IConfigBase>builder().addAll(Generic.OPTIONS).addAll(Visual.OPTIONS).addAll(Tweaks.OPTIONS).addAll(Hotkeys.HOTKEY_LIST).build();

    @Override public void load(){
        Path f=MalilibCompat.configDirectory().resolve(FILE);if(!Files.isReadable(f))return;
        JsonElement e=MalilibCompat.readJson(f);if(e==null||!e.isJsonObject())return;JsonObject r=e.getAsJsonObject();
        ConfigUtils.readConfigBase(r,"Generic",Generic.OPTIONS);ConfigUtils.readConfigBase(r,"Visual",Visual.OPTIONS);
        ConfigUtils.readConfigBase(r,"Tweaks",Tweaks.OPTIONS);ConfigUtils.readConfigBase(r,"Tweaks",Tweaks.LEGACY_OPTIONS);migrateDropConfirmLists();
        ConfigUtils.readConfigBase(r,"Hotkeys",Hotkeys.HOTKEY_LIST);
    }
    private static void migrateDropConfirmLists(){
        List<String> legacy=Tweaks.DROP_CONFIRM_ITEMS_LEGACY.getStrings();if(legacy.isEmpty())return;
        if(Tweaks.DROP_CONFIRM_BLACKLIST.getStrings().isEmpty())Tweaks.DROP_CONFIRM_BLACKLIST.setStrings(legacy);
        if(Tweaks.DROP_CONFIRM_WHITELIST.getStrings().isEmpty())Tweaks.DROP_CONFIRM_WHITELIST.setStrings(legacy);
    }
    @Override public void save(){
        Path d=MalilibCompat.configDirectory();if(!Files.isDirectory(d))MalilibCompat.ensureDirectory(d);if(!Files.isDirectory(d))return;
        JsonObject r=new JsonObject();ConfigUtils.writeConfigBase(r,"Generic",Generic.OPTIONS);ConfigUtils.writeConfigBase(r,"Visual",Visual.OPTIONS);ConfigUtils.writeConfigBase(r,"Tweaks",Tweaks.OPTIONS);ConfigUtils.writeConfigBase(r,"Hotkeys",Hotkeys.HOTKEY_LIST);MalilibCompat.writeJson(r,d.resolve(FILE));
    }
}
