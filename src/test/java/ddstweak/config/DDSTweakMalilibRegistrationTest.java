package ddstweak.config;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class DDSTweakMalilibRegistrationTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}
    private static long count(String s,String token){return s.split(java.util.regex.Pattern.quote(token),-1).length-1;}
    private static Set<String> configTranslationKeys(String json){
        Set<String> keys=new LinkedHashSet<>();
        for(String line:json.split("\\R")){
            String value=line.trim();
            if(!value.startsWith("\"dds-tweak.config."))continue;
            int end=value.indexOf('\"',1);
            if(end>1)keys.add(value.substring(1,end));
        }
        return keys;
    }
    private static String translationValue(String json,String key){
        String marker="\""+key+"\"";
        for(String line:json.split("\\R")){
            String value=line.trim();
            if(!value.startsWith(marker))continue;
            int colon=value.indexOf(':',marker.length()),start=value.indexOf('\"',colon+1),end=value.lastIndexOf('\"');
            return colon>=0&&start>=0&&end>start?value.substring(start+1,end):null;
        }
        return null;
    }

    @Test void clientEntrypointRegistersMaLiLibEagerly() throws Exception {
        String entry=read("src/main/java/ddstweak/DDSTweak.java");
        assertTrue(entry.contains("InitializationHandler.getInstance().registerInitializationHandler(new DDSTweakInitHandler())"));
        assertFalse(entry.contains("new DDSTweakConfigScreen"));
    }

    @Test void initHandlerRegistersGlobalConfigInfrastructure() throws Exception {
        String init=read("src/main/java/ddstweak/DDSTweakInitHandler.java");
        assertTrue(init.contains("ConfigManager.getInstance().registerConfigHandler"));
        assertTrue(init.contains("Registry.CONFIG_SCREEN.registerConfigScreenFactory"));
        assertTrue(init.contains("InputEventHandler.getKeybindManager().registerKeybindProvider"));
        assertTrue(init.contains("HotkeyCallbacks.init()"));
    }

    @Test void configScreenDefinesFourSemanticTabsAndKeepsLegacyPersistence() throws Exception {
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(screen.contains("extends GuiConfigsBase"));
        for(String id:new String[]{"FEATURES","TWEAKS","VISUAL","CLIENT"})assertEquals(1,count(screen,id+"("),id);
        assertFalse(screen.contains("GENERIC("));
        assertFalse(screen.contains("HOTKEYS("));
        assertTrue(screen.contains("tab=ConfigGuiTab.FEATURES"));
        assertTrue(configs.contains("class Generic {public static final List<IConfigBase> OPTIONS=ImmutableList.of()"));
        assertTrue(configs.contains("ConfigUtils.readConfigBase(r,\"Generic\",Generic.OPTIONS)"));
        assertTrue(configs.contains("ConfigUtils.writeConfigBase(r,\"Generic\",Generic.OPTIONS)"));
        assertTrue(configs.contains("ConfigUtils.readConfigBase(r,\"Hotkeys\",Hotkeys.HOTKEY_LIST)"));
        assertTrue(configs.contains("ConfigUtils.writeConfigBase(r,\"Hotkeys\",Hotkeys.HOTKEY_LIST)"));
        assertTrue(screen.contains("Registry.CONFIG_SCREEN" )||read("src/main/java/ddstweak/DDSTweakInitHandler.java").contains("Registry.CONFIG_SCREEN"));
    }

    @Test void configScreenGroupsOptionsByUserFacingPurpose() throws Exception {
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        int features=screen.indexOf("private static List<IConfigBase> featureConfigs()");
        int tweaks=screen.indexOf("private static List<IConfigBase> tweakConfigs()");
        int visual=screen.indexOf("private static List<IConfigBase> visualConfigs()");
        int client=screen.indexOf("private static List<IConfigBase> clientConfigs()");
        int listener=screen.indexOf("private static final class ButtonListener");
        assertTrue(features>=0&&features<tweaks&&tweaks<visual&&visual<client&&client<listener);

        String featureGroup=screen.substring(features,tweaks);
        for(String token:new String[]{"ENHANCED_INVENTORY_GUI","CLICK_THROUGH","CONFIRM_ITEM_DROP","HELPFUL_HITBOXES","CAMERA_PLAYER_SELECTOR","QUICK_MUTE","HAPPY_GHAST_RIDING_SPEED_MULTIPLIER","RAIL_POWER_PLACEMENT_GUIDE","Hotkeys.CAMERA_PLAYER_SELECTOR"})assertTrue(featureGroup.contains(token),token);
        assertFalse(featureGroup.contains("Hotkeys.OPEN_CONFIG_SCREEN"));
        assertFalse(featureGroup.contains("Hotkeys.OPEN_CARPET_RULE_MANAGER"));
        assertTrue(featureGroup.indexOf("Configs.Tweaks.CAMERA_PLAYER_SELECTOR")<featureGroup.indexOf("Hotkeys.CAMERA_PLAYER_SELECTOR"));
        assertTrue(featureGroup.indexOf("Hotkeys.CAMERA_PLAYER_SELECTOR")<featureGroup.indexOf("Configs.Tweaks.QUICK_MUTE"));

        String tweakGroup=screen.substring(tweaks,visual);
        for(String token:new String[]{"NO_COMMAND_CONFIRM","HIDE_BLOCK_OUTLINE","DISABLE_AUTO_SLEEP_CHAT","CHAT_TOOLTIP_OCCLUSION","SCROLL_BOOK_PAGES","SCROLL_BOOK_CTRL_MULTIPLIER"})assertTrue(tweakGroup.contains(token),token);

        String visualGroup=screen.substring(visual,client);
        for(String token:new String[]{"DISABLE_END_FLASH","GAMMA_OVERRIDE","STACK_COUNT_SCALE_ENABLED","COMPACT_STATUS_EFFECTS","CHUNK_CENTER_INDICATOR","BETTER_SNOW_COVERAGE"})assertTrue(visualGroup.contains(token),token);

        String clientGroup=screen.substring(client,listener);
        for(String token:new String[]{"PLAYER_HEAD_WINDOW_ICON","CLIENT_STARTUP_SOUND","AUTO_LOG_CLEANUP","AUTO_LOG_CLEANUP_MODE","AUTO_LOG_KEEP_DAYS","AUTO_LOG_KEEP_LIMIT","Hotkeys.DDS_UNDO","Hotkeys.DDS_REDO","Hotkeys.OPEN_CONFIG_SCREEN","Hotkeys.OPEN_CARPET_RULE_MANAGER"})assertTrue(clientGroup.contains(token),token);
    }

    @Test void enhancedInventoryGuiIsDefaultOnAndKeepsTweaksStorage() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(configs.contains("CHUNK_CENTER_INDICATOR=MalilibCompat.bool(\"chunkCenterIndicator\",true,VISUAL_KEY)"));
        assertTrue(configs.contains("ENHANCED_INVENTORY_GUI=MalilibCompat.bool(\"enhancedInventoryGui\",true,TWEAKS_KEY)"));
        assertTrue(configs.contains("CLICK_THROUGH=MalilibCompat.bool(\"clickThrough\",false,TWEAKS_KEY)"));
        assertTrue(configs.contains("ENHANCED_INVENTORY_GUI"));assertFalse(configs.contains("public static final class DDS"));
        String en=read("src/main/resources/assets/dds-tweak/lang/en_us.json"),zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");
        assertTrue(en.contains("dds-tweak.config.tweaks.prettyName.enhancedinventorygui"));
        assertTrue(zh.contains("dds-tweak.config.tweaks.prettyName.enhancedinventorygui"));
    }

    @Test void chineseLocalesCoverEveryConfigTranslationKey() throws Exception {
        Set<String> en=configTranslationKeys(read("src/main/resources/assets/dds-tweak/lang/en_us.json"));
        assertFalse(en.isEmpty());
        for(String locale:new String[]{"zh_cn","zh_tw"}){
            Set<String> translated=configTranslationKeys(read("src/main/resources/assets/dds-tweak/lang/"+locale+".json"));
            assertTrue(translated.containsAll(en),locale+" missing: "+difference(en,translated));
        }
    }

    @Test void malilibTranslationContractsArePresent() throws Exception {
        String[][] options={
                {"visual","disableEndFlash"},
                {"visual","gammaOverride"},
                {"visual","gammaOverrideValue"},
                {"visual","playerHeadWindowIcon"},
                {"visual","stackCountScaleEnabled"},
                {"visual","stackCountScale"},
                {"visual","compactStatusEffects"},
                {"visual","compactStatusEffectsOnLeft"},
                {"visual","compactStatusEffectsHoldTab"},
                {"visual","railPowerPlacementGuide"},
                {"visual","chunkCenterIndicator"},
                {"visual","hideBlockOutline"},
                {"visual","betterSnowCoverage"},
                {"visual","betterSnowCoverageMode"},
                {"visual","betterSnowCoverageBlacklist"},
                {"visual","betterSnowCoverageWhitelist"},
                {"tweaks","autoEat"},
                {"tweaks","autoEatOffhandPolicy"},
                {"tweaks","autoEatHungerThreshold"},
                {"tweaks","autoEatNoSlowdown"},
                {"tweaks","autoEatFoodBlacklist"},
                {"tweaks","shulkerRestockEnabled"},
                {"tweaks","shulkerRestockMode"},
                {"tweaks","shulkerRestockWhitelist"},
                {"tweaks","shulkerRestockBlacklist"},
                {"tweaks","shulkerRestockThreshold"},
                {"tweaks","noCommandConfirm"},
                {"tweaks","enhancedInventoryGui"},
                {"tweaks","clickThrough"},
                {"tweaks","confirmItemDrop"},
                {"tweaks","dropConfirmMode"},
                {"tweaks","dropConfirmBlacklist"},
                {"tweaks","dropConfirmWhitelist"},
                {"tweaks","disableAutoSleepChat"},
                {"tweaks","chatTooltipOcclusion"},
                {"tweaks","clientStartupSound"},
                {"tweaks","showOriginalConfigName"},
                {"tweaks","originalConfigNameScale"},
                {"tweaks","quickMute"},
                {"tweaks","autoLogCleanup"},
                {"tweaks","autoLogCleanupMode"},
                {"tweaks","autoLogKeepDays"},
                {"tweaks","autoLogKeepLimit"},
                {"tweaks","helpfulHitboxes"},
                {"tweaks","cameraPlayerSelector"},
                {"tweaks","scrollBookPages"},
                {"tweaks","scrollBookCtrlMultiplier"},
                {"tweaks","happyGhastRidingSpeedMultiplier"},
                {"hotkey","ddsUndo"},
                {"hotkey","ddsRedo"},
                {"hotkey","openConfigScreen"},
                {"hotkey","openCarpetRuleManager"},
                {"hotkey","cameraPlayerSelector"}
        };
        String en=read("src/main/resources/assets/dds-tweak/lang/en_us.json");
        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            Set<String> keys=configTranslationKeys(read("src/main/resources/assets/dds-tweak/lang/"+locale+".json"));
            for(String[] option:options){
                String prefix="dds-tweak.config."+option[0],name=option[1],lower=name.toLowerCase(Locale.ROOT);
                for(String key:new String[]{
                        prefix+".name."+name,
                        prefix+".prettyName."+name,
                        prefix+".comment."+name,
                        prefix+".prettyName."+lower,
                        prefix+".comment."+lower
                })assertTrue(keys.contains(key),locale+" missing: "+key);
                // MaLiLib looks up lowercased prettyName/comment keys at runtime.
                // Key existence alone is insufficient if the value is blank or raw.
                if(option[0].equals("tweaks")&&(name.startsWith("shulkerRestock")
                        ||name.startsWith("autoEat"))){
                    String lang=read("src/main/resources/assets/dds-tweak/lang/"+locale+".json");
                    for(String part:new String[]{"prettyName","comment"}){
                        String key=prefix+"."+part+"."+lower;
                        String value=translationValue(lang,key);
                        assertNotNull(value,locale+" missing visible label: "+key);
                        assertFalse(value.isBlank()||value.startsWith("dds-tweak.config."),
                                locale+" raw/blank label: "+key);
                    }
                }
                String englishName=translationValue(en,prefix+".name."+name);
                String englishPretty=translationValue(en,prefix+".prettyName."+name);
                assertNotNull(englishName,"Missing English name: "+name);
                assertNotNull(englishPretty,"Missing English pretty name: "+name);
                assertFalse(englishName.isBlank(),"Empty English name: "+name);
                assertFalse(englishPretty.isBlank(),"Empty English pretty name: "+name);
                assertNotEquals(name,englishName,"English name must be readable: "+name);
                assertNotEquals(name,englishPretty,"English pretty name must be readable: "+name);
            }
        }
    }

    @Test void legacyAutoEatNoSlowdownTranslationsAndSliderBackdrop() throws Exception {
        String prefix="dds-tweak.config.tweaks.";
        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            String language=read("src/main/resources/assets/dds-tweak/lang/"+locale+".json");
            for(String suffix:new String[]{"prettyName.","comment."}){
                String key=prefix+suffix+"autoeatnoslowdown";
                String label=translationValue(language,key);
                assertNotNull(label,locale+" missing: "+key);
                assertFalse(label.isBlank(),key);
                assertNotEquals(key,label);
            }
        }
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        assertTrue(screen.contains("//#if MC <= 12001"));
        assertTrue(screen.contains("new LegacyAtlasSlider(x,y,configWidth,configHeight,callback)"));
        assertTrue(screen.contains("LegacySliderTextureSegments.forEachBackground(this.width"));
        assertTrue(screen.contains("super.addConfigSliderEntry(x,y,resetX,configWidth,configHeight,config)"));
        assertFalse(screen.contains("sliderWidth=Math.min(configWidth,200)"));
        assertFalse(screen.contains("hungerSliderWidth"));
        assertFalse(screen.contains("ctx.fill(hungerSliderX"));
        assertTrue(screen.contains("super.render(mouseX,mouseY,selected,ctx)"));
    }

    @Test void cdaUndoRedoUseLocalizedPrimaryNamesAndKeepOriginalKeysSeparate() throws Exception {
        String[][] cases={
                {"en_us","CDA Undo","CDA Redo"},
                {"zh_cn","CDA 撤销","CDA 重做"},
                {"zh_tw","CDA 撤銷","CDA 重做"}
        };
        for(String[] locale:cases){
            String language=read("src/main/resources/assets/dds-tweak/lang/"+locale[0]+".json");
            for(int i=0;i<2;i++){
                String key=i==0?"ddsUndo":"ddsRedo",expected=locale[i+1];
                String prefix="dds-tweak.config.hotkey.";
                assertEquals(expected,translationValue(language,prefix+"name."+key));
                assertEquals(expected,translationValue(language,prefix+"prettyName."+key));
                assertNotEquals(key,translationValue(language,prefix+"name."+key),
                        locale[0]+" shows raw key instead of localized primary label");
            }
        }
    }

    @Test void betterSnowConfigNamesStayLocalizedInChinese() throws Exception {
        String[] names={"betterSnowCoverage","betterSnowCoverageMode","betterSnowCoverageBlacklist","betterSnowCoverageWhitelist"};
        for(String locale:new String[]{"zh_cn","zh_tw"}){
            String lang=read("src/main/resources/assets/dds-tweak/lang/"+locale+".json");
            for(String name:names){
                String key="dds-tweak.config.visual.name."+name,value=translationValue(lang,key);
                assertNotNull(value,locale+" missing: "+key);
                assertFalse(value.trim().isEmpty(),locale+" empty: "+key);
                assertNotEquals(name,value,locale+" fell back to raw config name: "+key);
            }
        }
    }


    @Test void compactStatusEffectsOwnTheirPlacementAndTrackRenderedBounds() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/compactstatuseffects/CompactStatusEffectsFeature.java");
        assertTrue(feature.contains("return Configs.Visual.COMPACT_STATUS_EFFECTS_ON_LEFT.getBooleanValue()"));
        assertFalse(feature.contains("roughlyenoughitems"));
        assertFalse(feature.contains("effectsleft"));

        String modern=read("src/main/java/ddstweak/mixin/feature/compactstatuseffects/EffectsInInventoryMixin.java");
        String legacy=read("src/main/java/ddstweak/mixin/feature/compactstatuseffects/EffectRenderingInventoryScreenMixin.java");
        assertTrue(modern.contains("ddstweak$beginExpandedTranslation"));
        assertTrue(legacy.contains("ddstweak$beginExpandedTranslation"));
        assertTrue(modern.contains("CompactStatusEffectLayout.FRAME_SIZE"));
        assertTrue(legacy.contains("CompactStatusEffectLayout.FRAME_SIZE"));
        assertTrue(modern.contains("ddstweak$drawExpandedEffect"));
        assertTrue(legacy.contains("ddstweak$drawExpandedEffect"));
        assertFalse(modern.contains("ddstweak$drawTooltip"));
        assertFalse(legacy.contains("ddstweak$drawTooltip"));
        assertTrue(modern.contains("ddstweak$drawLevelBadge"));
        assertTrue(legacy.contains("ddstweak$drawLevelBadge"));
        assertTrue(modern.contains("CompactStatusEffectFormat.durationText"));
        assertTrue(legacy.contains("CompactStatusEffectFormat.durationText"));
        assertTrue(modern.contains(".stream().sorted().toList()"));
        assertTrue(legacy.contains(".stream().sorted().toList()"));
        assertTrue(modern.contains("@Invoker(\"extractEffects\")"));
        assertTrue(modern.contains("@Invoker(\"renderEffects\")"));
        assertTrue(modern.contains("ddstweak$expanded=ddstweak$isTabDown()"));
        assertTrue(legacy.contains("ddstweak$effectsExpanded=ddstweak$isTabDown()"));
        assertFalse(modern.contains("skip(Math.max(0,effects-4))"));
        assertFalse(legacy.contains("skip(Math.max(0,effects-4))"));
    }

    @Test void configCommentsAvoidMalilibFormatHazards() throws Exception {
        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            String lang=read("src/main/resources/assets/dds-tweak/lang/"+locale+".json");
            assertFalse(lang.contains("%%"),locale+" still contains doubled percent formatting");
            assertFalse(lang.contains("\\\\n"),locale+" still contains a literal escaped newline");
        }
    }

    private static Set<String> difference(Set<String> expected,Set<String> actual){
        Set<String> missing=new LinkedHashSet<>(expected);missing.removeAll(actual);return missing;
    }

    @Test void interactiveFeatureTogglesUseUnifiedHotkeyedConfig() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        for(String line:new String[]{
                "GAMMA_OVERRIDE=MalilibCompat.boolHotkeyed(\"gammaOverride\",false,\"\",VISUAL_KEY)",
                "RAIL_POWER_PLACEMENT_GUIDE=MalilibCompat.boolHotkeyed(\"railPowerPlacementGuide\",false,\"\",VISUAL_KEY)",
                "HELPFUL_HITBOXES=MalilibCompat.boolHotkeyed(\"helpfulHitboxes\",false,\"\",TWEAKS_KEY)",
                "QUICK_MUTE=MalilibCompat.boolHotkeyed(\"quickMute\",false,\"LEFT_CONTROL,M\",TWEAKS_KEY)"
        })assertTrue(configs.contains(line),line);
        assertTrue(configs.contains("ENHANCED_INVENTORY_GUI=MalilibCompat.bool(\"enhancedInventoryGui\",true,TWEAKS_KEY)"));
        assertTrue(configs.contains("CONFIRM_ITEM_DROP=MalilibCompat.bool(\"confirmItemDrop\",false,TWEAKS_KEY)"));
        assertTrue(configs.contains("CAMERA_PLAYER_SELECTOR=MalilibCompat.bool(\"cameraPlayerSelector\",true,TWEAKS_KEY)"));
    }

    @Test void originalNamePercentUsesIntegerStorageAndLoadsLegacyScaleBeforeConfig() throws Exception {
        String config=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(config.contains("public static final ConfigInteger ORIGINAL_CONFIG_NAME_SCALE=MalilibCompat.integer("));
        assertTrue(config.contains("OriginalConfigNameScale.DEFAULT_PERCENT"));
        assertTrue(config.indexOf("migrateOriginalConfigNameScale(r);")<
                config.indexOf("ConfigUtils.readConfigBase(r,\"Tweaks\",Tweaks.OPTIONS)"));
        assertTrue(config.contains("options.addProperty(\"originalConfigNameScale\""));
    }

    @Test void semanticConfigTabsUseUnifiedTextSearchAndAdaptiveWidth() throws Exception {
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        assertTrue(screen.contains("@Override protected int getConfigWidth(){return 260;}"));
        assertFalse(screen.contains("protected boolean useKeybindSearch()"),
                "All tabs must use MaLiLib's ordinary text search and the same vertical offset");
        assertTrue(screen.contains("getConfigWidth(),useKeybindSearch(),this)"),
                "Modern and legacy list constructors must receive the common search mode");
        assertTrue(screen.contains("return labelWidth;"),
                "Each tab must retain its own adaptive label/control X origin");
        assertTrue(screen.contains("ConfigRowGrouping.controlWidth(browserEntryWidth,sharedLabelWidth,"),
                "The shared control-width bound must still prevent overflow");
    }

    @Test void hotkeyProviderAndIdentityArePresent() throws Exception {
        String ref=read("src/main/java/ddstweak/DDSTweakReference.java"),provider=read("src/main/java/ddstweak/config/DDSTweakHotkeyProvider.java");
        assertTrue(ref.contains("MOD_ID=\"dds-tweak\""));
        assertTrue(ref.contains("MOD_NAME=\"DDS Tweak\""));
        assertTrue(ref.contains("new ModInfo(MOD_ID,MOD_NAME,DDSTweakConfigScreen::new)"));
        assertTrue(provider.contains("implements IKeybindProvider"));
        assertTrue(provider.contains("Configs.ALL"));
        assertTrue(provider.contains("config instanceof IHotkey"));
        String hotkeys=read("src/main/java/ddstweak/config/Hotkeys.java");
        assertTrue(hotkeys.contains("MalilibCompat.hotkey(\"openConfigScreen\",\"LEFT_ALT,V\",KEY)"));
        assertTrue(hotkeys.contains("MalilibCompat.hotkey(\"cameraPlayerSelector\",\"R\",KEY)"));
    }
}
