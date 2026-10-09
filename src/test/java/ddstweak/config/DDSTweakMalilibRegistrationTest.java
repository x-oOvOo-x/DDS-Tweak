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
        for(String token:new String[]{"ENHANCED_INVENTORY_GUI","CLICK_THROUGH","CONFIRM_ITEM_DROP","HELPFUL_HITBOXES","CAMERA_PLAYER_SELECTOR","QUICK_MUTE","RAIL_POWER_PLACEMENT_GUIDE","Hotkeys.OPEN_CONFIG_SCREEN","Hotkeys.CAMERA_PLAYER_SELECTOR"})assertTrue(featureGroup.contains(token),token);
        assertTrue(featureGroup.indexOf("Configs.Tweaks.CAMERA_PLAYER_SELECTOR")<featureGroup.indexOf("Hotkeys.CAMERA_PLAYER_SELECTOR"));
        assertTrue(featureGroup.indexOf("Hotkeys.CAMERA_PLAYER_SELECTOR")<featureGroup.indexOf("Configs.Tweaks.QUICK_MUTE"));

        String tweakGroup=screen.substring(tweaks,visual);
        for(String token:new String[]{"NO_COMMAND_CONFIRM","HIDE_BLOCK_OUTLINE","DISABLE_AUTO_SLEEP_CHAT","CHAT_TOOLTIP_OCCLUSION","SCROLL_BOOK_PAGES","SCROLL_BOOK_CTRL_MULTIPLIER"})assertTrue(tweakGroup.contains(token),token);

        String visualGroup=screen.substring(visual,client);
        for(String token:new String[]{"DISABLE_END_FLASH","GAMMA_OVERRIDE","STACK_COUNT_SCALE_ENABLED","COMPACT_STATUS_EFFECTS","CHUNK_CENTER_INDICATOR","BETTER_SNOW_COVERAGE"})assertTrue(visualGroup.contains(token),token);

        String clientGroup=screen.substring(client,listener);
        for(String token:new String[]{"PLAYER_HEAD_WINDOW_ICON","CLIENT_STARTUP_SOUND","AUTO_LOG_CLEANUP","AUTO_LOG_CLEANUP_MODE","AUTO_LOG_KEEP_DAYS","AUTO_LOG_KEEP_LIMIT"})assertTrue(clientGroup.contains(token),token);
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
                {"tweaks","quickMute"},
                {"tweaks","autoLogCleanup"},
                {"tweaks","autoLogCleanupMode"},
                {"tweaks","autoLogKeepDays"},
                {"tweaks","autoLogKeepLimit"},
                {"tweaks","helpfulHitboxes"},
                {"tweaks","cameraPlayerSelector"},
                {"tweaks","scrollBookPages"},
                {"tweaks","scrollBookCtrlMultiplier"},
                {"hotkey","openConfigScreen"},
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
                assertTrue(en.contains("\""+prefix+".name."+name+"\": \""+name+"\""),"English display name must stay raw: "+name);
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

    @Test void semanticConfigTabsUseUnifiedWidthAndFeatureKeybindSearch() throws Exception {
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        assertTrue(screen.contains("@Override protected int getConfigWidth(){return 260;}"));
        assertTrue(screen.contains("@Override protected boolean useKeybindSearch(){return tab==ConfigGuiTab.FEATURES;}"));
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
