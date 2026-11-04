package ddstweak.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ddstweak.DDSTweakReference;
import ddstweak.platform.malilib.MalilibCompat;
import ddstweak.feature.dropconfirmation.DropConfirmMode;
import ddstweak.feature.autologcleanup.AutoLogCleanupMode;
import ddstweak.feature.clickthrough.ClickThroughMode;
import ddstweak.feature.silentautoeat.AutoEatOffhandPolicy;
import ddstweak.feature.shulkerrestock.ShulkerRestockFilterMode;
//#if MC >= 12111
import ddstweak.feature.blocksoundtracker.BlockSoundFilterMode;
import ddstweak.feature.blocksoundtracker.BlockSoundDefaults;
import ddstweak.feature.blocksoundtracker.BlockSoundBlockDefaults;
import ddstweak.feature.blocksoundtracker.BlockSoundConfigMigration;
//#endif
//#if MC >= 12111
import ddstweak.feature.bettersnowcoverage.SnowCoverageMode;
//#endif
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import java.nio.file.*;
import java.util.List;

public final class Configs implements IConfigHandler {
    private static boolean loading;
    public static boolean isLoading(){return loading;}
    private static final String VISUAL_KEY=DDSTweakReference.MOD_ID+".config.visual",TWEAKS_KEY=DDSTweakReference.MOD_ID+".config.tweaks";
    public static final class Generic {public static final List<IConfigBase> OPTIONS=ImmutableList.of();private Generic(){}}
    public static final class Visual {
        //#if MC >= 12108
        public static final ConfigBoolean LOCATOR_HEADS=MalilibCompat.bool("locatorHeads",true,VISUAL_KEY);
        public static final ConfigBoolean LOCATOR_OUTER_LAYER=MalilibCompat.bool("locatorOuterLayer",true,VISUAL_KEY);
        public static final ConfigBoolean LOCATOR_ALWAYS_XP=MalilibCompat.bool("locatorAlwaysXp",false,VISUAL_KEY);
        public static final ConfigInteger LOCATOR_HEAD_SIZE=MalilibCompat.integer("locatorHeadSize",100,50,150,true,VISUAL_KEY);
        public static final ConfigOptionList LOCATOR_NAME_MODE=MalilibCompat.optionList("locatorNameMode",ddstweak.feature.locatorheads.LocatorOptions.NameMode.OFF,VISUAL_KEY);
        public static final ConfigBoolean LOCATOR_COMPASS=MalilibCompat.bool("locatorCompass",false,VISUAL_KEY);
        public static final ConfigColor LOCATOR_COMPASS_COLOR=MalilibCompat.color("locatorCompassColor","#FFFFFFFF",VISUAL_KEY);
        public static final ConfigBoolean LOCATOR_COMPASS_SHADOW=MalilibCompat.bool("locatorCompassShadow",true,VISUAL_KEY);
        public static final ConfigBoolean LOCATOR_BORDER=MalilibCompat.bool("locatorBorder",false,VISUAL_KEY);
        public static final ConfigOptionList LOCATOR_BORDER_STYLE=MalilibCompat.optionList("locatorBorderStyle",ddstweak.feature.locatorheads.LocatorOptions.BorderStyle.TEAM,VISUAL_KEY);
        public static final ConfigOptionList LOCATOR_BORDER_THICKNESS=MalilibCompat.optionList("locatorBorderThickness",ddstweak.feature.locatorheads.LocatorOptions.BorderThickness.NORMAL,VISUAL_KEY);
        public static final ConfigColor LOCATOR_BORDER_COLOR=MalilibCompat.color("locatorBorderColor","#FFFFFFFF",VISUAL_KEY);
        public static final ConfigBoolean LOCATOR_NEARBY_ONLY=MalilibCompat.bool("locatorNearbyOnly",false,VISUAL_KEY);
        public static final ConfigOptionList LOCATOR_PLAYER_FILTER=MalilibCompat.optionList("locatorPlayerFilter",ddstweak.feature.locatorheads.LocatorOptions.PlayerFilter.ALL,VISUAL_KEY);
        public static final ConfigStringList LOCATOR_INCLUDE=MalilibCompat.stringList("locatorInclude",ImmutableList.of(),VISUAL_KEY);
        public static final ConfigStringList LOCATOR_EXCLUDE=MalilibCompat.stringList("locatorExclude",ImmutableList.of(),VISUAL_KEY);
        //#endif
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
        //#if MC >= 12111
        public static final ConfigBooleanHotkeyed BLOCK_SOUND_TRACKER=MalilibCompat.boolHotkeyed("blockSoundTracker",false,"",VISUAL_KEY);
        public static final ConfigOptionList BLOCK_SOUND_FILTER_MODE=MalilibCompat.optionList("blockSoundFilterMode",BlockSoundFilterMode.WHITELIST,VISUAL_KEY);
        public static final ConfigStringList BLOCK_SOUND_BLACKLIST=MalilibCompat.stringList("blockSoundBlacklist",ImmutableList.of("minecraft:block.water.ambient"),VISUAL_KEY);
        public static final ConfigStringList BLOCK_SOUND_WHITELIST=MalilibCompat.stringList("blockSoundWhitelist",BlockSoundDefaults.REDSTONE_WHITELIST,VISUAL_KEY);
        public static final ConfigStringList BLOCK_SOUND_BLOCK_BLACKLIST=MalilibCompat.stringList("blockSoundBlockBlacklist",ImmutableList.of(),VISUAL_KEY);
        public static final ConfigStringList BLOCK_SOUND_BLOCK_WHITELIST=MalilibCompat.stringList("blockSoundBlockWhitelist",BlockSoundBlockDefaults.REDSTONE_BLOCKS,VISUAL_KEY);
        private static final List<IConfigBase> LEGACY_SOUND_OPTIONS=ImmutableList.of(BLOCK_SOUND_BLACKLIST,BLOCK_SOUND_WHITELIST);
        public static final ConfigInteger BLOCK_SOUND_RANGE=MalilibCompat.integer("blockSoundRange",64,8,128,true,VISUAL_KEY);
        public static final ConfigInteger BLOCK_SOUND_HIGHLIGHT_DURATION=MalilibCompat.integer("blockSoundHighlightDuration",100,20,400,true,VISUAL_KEY);
        public static final ConfigInteger BLOCK_SOUND_MAX_HIGHLIGHTS=MalilibCompat.integer("blockSoundMaxHighlights",10,1,32,true,VISUAL_KEY);
        public static final ConfigBoolean BLOCK_SOUND_REPEATED_ONLY=MalilibCompat.bool("blockSoundRepeatedOnly",false,VISUAL_KEY);
        //#endif
        public static final ConfigBooleanHotkeyed RAIL_POWER_PLACEMENT_GUIDE=MalilibCompat.boolHotkeyed("railPowerPlacementGuide",false,"",VISUAL_KEY);
        public static final ConfigBoolean CHUNK_CENTER_INDICATOR=MalilibCompat.bool("chunkCenterIndicator",true,VISUAL_KEY);
        public static final ConfigBoolean HIDE_BLOCK_OUTLINE=MalilibCompat.bool("hideBlockOutline",false,VISUAL_KEY);
        //#if MC >= 12111
        public static final ConfigBoolean BETTER_SNOW_COVERAGE=MalilibCompat.bool("betterSnowCoverage",false,VISUAL_KEY);
        public static final ConfigBoolean TAB_FAKE_PLAYERS_LAST=MalilibCompat.bool("tabFakePlayersLast",false,VISUAL_KEY);
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
                BLOCK_SOUND_TRACKER,BLOCK_SOUND_FILTER_MODE,BLOCK_SOUND_BLOCK_BLACKLIST,BLOCK_SOUND_BLOCK_WHITELIST,
                BLOCK_SOUND_RANGE,BLOCK_SOUND_HIGHLIGHT_DURATION,BLOCK_SOUND_MAX_HIGHLIGHTS,
                BLOCK_SOUND_REPEATED_ONLY,
                //#endif
                //#if MC >= 12111
                BETTER_SNOW_COVERAGE,BETTER_SNOW_COVERAGE_MODE,BETTER_SNOW_COVERAGE_BLACKLIST,BETTER_SNOW_COVERAGE_WHITELIST,TAB_FAKE_PLAYERS_LAST,
                //#endif
                //#if MC >= 12108
                LOCATOR_HEADS,LOCATOR_OUTER_LAYER,LOCATOR_ALWAYS_XP,LOCATOR_HEAD_SIZE,LOCATOR_NAME_MODE,
                LOCATOR_COMPASS,LOCATOR_COMPASS_COLOR,LOCATOR_COMPASS_SHADOW,
                LOCATOR_BORDER,LOCATOR_BORDER_STYLE,LOCATOR_BORDER_THICKNESS,LOCATOR_BORDER_COLOR,
                LOCATOR_NEARBY_ONLY,LOCATOR_PLAYER_FILTER,LOCATOR_INCLUDE,LOCATOR_EXCLUDE,
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
        public static final ConfigOptionList AUTO_EAT_OFFHAND_POLICY=MalilibCompat.optionList("autoEatOffhandPolicy",AutoEatOffhandPolicy.PROTECT_CRITICAL,TWEAKS_KEY);
        public static final ConfigBoolean AUTO_EAT_NO_SLOWDOWN=MalilibCompat.bool("autoEatNoSlowdown",false,TWEAKS_KEY);
        public static final ConfigBoolean SHULKER_RESTOCK_ENABLED=MalilibCompat.bool("shulkerRestockEnabled",false,TWEAKS_KEY);
        public static final ConfigOptionList SHULKER_RESTOCK_MODE=MalilibCompat.optionList("shulkerRestockMode",ShulkerRestockFilterMode.WHITELIST,TWEAKS_KEY);
        public static final ConfigStringList SHULKER_RESTOCK_WHITELIST=MalilibCompat.stringList("shulkerRestockWhitelist",ImmutableList.of(),TWEAKS_KEY);
        public static final ConfigStringList SHULKER_RESTOCK_BLACKLIST=MalilibCompat.stringList("shulkerRestockBlacklist",ImmutableList.of(),TWEAKS_KEY);
        public static final ConfigInteger SHULKER_RESTOCK_THRESHOLD=MalilibCompat.integer("shulkerRestockThreshold",0,0,63,true,TWEAKS_KEY);
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
        public static final ConfigBoolean SHOW_ORIGINAL_CONFIG_NAME=MalilibCompat.bool("showOriginalConfigName",true,TWEAKS_KEY);
        public static final ConfigInteger ORIGINAL_CONFIG_NAME_SCALE=MalilibCompat.integer(
                "originalConfigNameScale",OriginalConfigNameScale.DEFAULT_PERCENT,
                OriginalConfigNameScale.MIN_PERCENT,OriginalConfigNameScale.MAX_PERCENT,true,TWEAKS_KEY);
        public static final ConfigBooleanHotkeyed HELPFUL_HITBOXES=MalilibCompat.boolHotkeyed("helpfulHitboxes",false,"",TWEAKS_KEY);
        public static final ConfigBoolean CAMERA_PLAYER_SELECTOR=MalilibCompat.bool("cameraPlayerSelector",true,TWEAKS_KEY);
        public static final ConfigBoolean SCROLL_BOOK_PAGES=MalilibCompat.bool("scrollBookPages",false,TWEAKS_KEY);
        public static final ConfigInteger SCROLL_BOOK_CTRL_MULTIPLIER=MalilibCompat.integer("scrollBookCtrlMultiplier",5,1,10,true,TWEAKS_KEY);
        //#if MC >= 12106
        public static final ConfigDouble HAPPY_GHAST_RIDING_SPEED_MULTIPLIER=MalilibCompat.decimal("happyGhastRidingSpeedMultiplier",1.0,0.5,2.0,true,TWEAKS_KEY);
        //#endif
        public static final List<IConfigBase> OPTIONS=ImmutableList.of(
                //#if MC >= 12106
                NO_COMMAND_CONFIRM,HAPPY_GHAST_RIDING_SPEED_MULTIPLIER,
                //#endif
                ENHANCED_INVENTORY_GUI,CLICK_THROUGH,CLICK_THROUGH_MODE,AUTO_EAT,AUTO_EAT_OFFHAND_POLICY,AUTO_EAT_HUNGER_THRESHOLD,AUTO_EAT_NO_SLOWDOWN,AUTO_EAT_FOOD_BLACKLIST,
                SHULKER_RESTOCK_ENABLED,SHULKER_RESTOCK_MODE,SHULKER_RESTOCK_WHITELIST,SHULKER_RESTOCK_BLACKLIST,SHULKER_RESTOCK_THRESHOLD,
                CONFIRM_ITEM_DROP,DROP_CONFIRM_MODE,DROP_CONFIRM_BLACKLIST,DROP_CONFIRM_WHITELIST,DISABLE_AUTO_SLEEP_CHAT,
                //#if MC >= 11502
                CHAT_TOOLTIP_OCCLUSION,
                //#endif
                CLIENT_STARTUP_SOUND,QUICK_MUTE,HELPFUL_HITBOXES,AUTO_LOG_CLEANUP,AUTO_LOG_CLEANUP_MODE,AUTO_LOG_KEEP_DAYS,AUTO_LOG_KEEP_LIMIT,CAMERA_PLAYER_SELECTOR,SCROLL_BOOK_PAGES,SCROLL_BOOK_CTRL_MULTIPLIER,SHOW_ORIGINAL_CONFIG_NAME,ORIGINAL_CONFIG_NAME_SCALE
        );
        private static final List<IConfigBase> LEGACY_OPTIONS=ImmutableList.of(DROP_CONFIRM_ITEMS_LEGACY);
        private Tweaks(){}
    }
    public static final List<IConfigBase> ALL=ImmutableList.<IConfigBase>builder().addAll(Generic.OPTIONS).addAll(Visual.OPTIONS).addAll(Tweaks.OPTIONS).addAll(Hotkeys.HOTKEY_LIST).build();

    @Override public void load(){
        Path f=DdsTweakConfigPaths.settings();if(!Files.isReadable(f))return;
        JsonElement e=MalilibCompat.readJson(f);if(e==null||!e.isJsonObject())throw new IllegalStateException("Cannot read DT settings: "+f);JsonObject r=e.getAsJsonObject();
        loading=true;
        try{
            ConfigUtils.readConfigBase(r,"Generic",Generic.OPTIONS);migrateLocatorConfigs(r);
            //#if MC >= 12111
            BlockSoundConfigMigration.migrate(r);
            //#endif
            ConfigUtils.readConfigBase(r,"Visual",Visual.OPTIONS);
            //#if MC >= 12111
            ConfigUtils.readConfigBase(r,"Visual",Visual.LEGACY_SOUND_OPTIONS);
            //#endif
            migrateOriginalConfigNameScale(r);
            ddstweak.feature.quickmute.QuickMute.loadState(r);
            ConfigUtils.readConfigBase(r,"Tweaks",Tweaks.OPTIONS);ConfigUtils.readConfigBase(r,"Tweaks",Tweaks.LEGACY_OPTIONS);migrateDropConfirmLists();
            ConfigUtils.readConfigBase(r,"Hotkeys",Hotkeys.HOTKEY_LIST);
        }finally{loading=false;}
        ddstweak.feature.quickmute.QuickMute.onConfigLoaded();
    }
    /** Upgrade values saved by earlier drafts without changing the public config keys. */
    private static void migrateLocatorConfigs(JsonObject root){
        //#if MC >= 12108
        ddstweak.feature.locatorheads.LocatorConfigMigration.migrate(root);
        //#endif
    }
    /** Accept existing 0.50–1.00 values, but leave already migrated 50–100 values untouched. */
    private static void migrateOriginalConfigNameScale(JsonObject root){
        JsonElement tweaks=root.get("Tweaks");
        if(tweaks==null||!tweaks.isJsonObject())return;
        JsonObject options=tweaks.getAsJsonObject();
        JsonElement old=options.get("originalConfigNameScale");
        if(old==null||!old.isJsonPrimitive()||!old.getAsJsonPrimitive().isNumber())return;
        try{
            double value=old.getAsDouble();
            if(OriginalConfigNameScale.isLegacyMultiplier(value)){
                options.addProperty("originalConfigNameScale",OriginalConfigNameScale.fromLegacyMultiplier(value));
            }
        }catch(NumberFormatException ignored){}
    }
    private static void migrateDropConfirmLists(){
        List<String> legacy=Tweaks.DROP_CONFIRM_ITEMS_LEGACY.getStrings();if(legacy.isEmpty())return;
        if(Tweaks.DROP_CONFIRM_BLACKLIST.getStrings().isEmpty())Tweaks.DROP_CONFIRM_BLACKLIST.setStrings(legacy);
        if(Tweaks.DROP_CONFIRM_WHITELIST.getStrings().isEmpty())Tweaks.DROP_CONFIRM_WHITELIST.setStrings(legacy);
    }
    @Override public void save(){
        Path f=DdsTweakConfigPaths.settings();Path d=f.getParent();if(!Files.isDirectory(d))MalilibCompat.ensureDirectory(d);
        if(!Files.isDirectory(d))throw new IllegalStateException("Cannot create DT config directory: "+d);
        JsonObject r=new JsonObject();ConfigUtils.writeConfigBase(r,"Generic",Generic.OPTIONS);ConfigUtils.writeConfigBase(r,"Visual",Visual.OPTIONS);
        //#if MC >= 12111
        ConfigUtils.writeConfigBase(r,"Visual",Visual.LEGACY_SOUND_OPTIONS);
        //#endif
        ConfigUtils.writeConfigBase(r,"Tweaks",Tweaks.OPTIONS);ConfigUtils.writeConfigBase(r,"Hotkeys",Hotkeys.HOTKEY_LIST);
        ddstweak.feature.quickmute.QuickMute.saveState(r);
        MalilibCompat.writeJson(r,f);
    }
}
