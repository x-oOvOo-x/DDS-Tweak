package ddstweak.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.*;

final class ConfigRowGroupingTest {
    @Test void legacyTranslationKeysRemainDynamicUntilGuiRender() throws Exception {
        String compat=java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/ddstweak/platform/malilib/MalilibCompat.java"));
        assertTrue(compat.contains("key(prefix,\"prettyName\",name)"),
                "Legacy display keys must remain untranslated until the GUI requests them");
        assertTrue(compat.contains("new LegacyTranslatedInteger(name,value,min,max,slider,translationPrefix)"));
        assertTrue(compat.contains("new LegacyTranslatedDouble(name,value,min,max,slider,translationPrefix)"));
        assertTrue(compat.contains("private interface LegacyGuiName extends IConfigBase"));
        assertTrue(compat.contains("@Override default String getConfigGuiDisplayName(){return getPrettyName();}"));
        for(String type:new String[]{"Boolean","BooleanHotkeyed","Integer","Double","OptionList","StringList","Hotkey"}){
            assertTrue(compat.contains("class LegacyTranslated"+type),type);
        }
        assertTrue(compat.contains("@Override public String getPrettyName(){return StringUtils.translate(displayKey);}"));
        assertFalse(compat.contains("this.prettyName=tr(prefix,\"prettyName\",name)"),
                "Do not freeze names during eager config initialization");
        assertFalse(compat.contains("private static String tr("));
        assertFalse(compat.contains("tr(prefix,\"comment\",name)"));
        assertTrue(compat.contains("key(prefix,\"comment\",name)"));
        assertTrue(compat.contains("super(name,value,min,max,slider,key(prefix,\"comment\",name))"));
    }

    @Test void allTabsUseMaLiLibPlainTextSearchWithoutChangingKeybinds() throws IOException {
        String gui=Files.readString(Path.of("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java"));
        assertFalse(gui.contains("useKeybindSearch(){return tab==ConfigGuiTab.FEATURES;}"),
                "The feature tab must not use a taller key-combination search bar");
        assertFalse(gui.contains("protected boolean useKeybindSearch()"),
                "Leave MaLiLib's plain-text search enabled for every tab");
        assertTrue(gui.contains("getConfigWidth(),useKeybindSearch(),this)"),
                "Both modern and legacy config lists must use the same search-mode contract");
        assertTrue(gui.contains("return labelWidth;"),
                "Each tab's control X must remain adaptive to its own visible labels");
        assertTrue(gui.contains("ConfigRowGrouping.controlWidth(browserEntryWidth,sharedLabelWidth,"),
                "Retain the cross-tab available-control-width constraint");
        String keys=Files.readString(Path.of("src/main/java/ddstweak/config/DDSTweakHotkeyProvider.java"));
        assertTrue(keys.contains("for(IConfigBase config:Configs.ALL)if(config instanceof IHotkey)"));
    }

    @Test void originalNameIsLocalizedScopedAndClippedWithoutSplittingUnicode(){
        assertTrue(ConfigRowGrouping.showOriginalName("自动进食","autoEat"));
        assertFalse(ConfigRowGrouping.showOriginalName("autoEat","autoEat"));
        assertFalse(ConfigRowGrouping.showOriginalName("§aautoEat","autoEat"));
        assertFalse(ConfigRowGrouping.showOriginalName("dds-tweak.config.tweaks.prettyName.unknown","unknown"));
        assertFalse(ConfigRowGrouping.showOriginalName("","unknown"));
        assertFalse(ConfigRowGrouping.showOriginalName(null,"unknown"));
        assertFalse(ConfigRowGrouping.showOriginalName("翻译",null));
        assertTrue(ConfigRowGrouping.showOriginalName("§6自动进食§r","autoEat"));
        assertEquals("autoEat",ConfigRowGrouping.fitOriginalName("autoEat",100,String::length));
        assertEquals("abc…",ConfigRowGrouping.fitOriginalName("abcdef",3,
                s->s.codePointCount(0,s.length())));
        assertEquals("…",ConfigRowGrouping.fitOriginalName("长文字",1,
                s->s.codePointCount(0,s.length())));
        assertEquals("",ConfigRowGrouping.fitOriginalName("longName",0,String::length));
        assertEquals("abcdef",ConfigRowGrouping.fitOriginalName("abcdef",6,String::length,1f));
        assertEquals("abc…",ConfigRowGrouping.fitOriginalName("abcdef",4,String::length,1f));
        assertEquals(162,ConfigRowGrouping.displayWidth(60,160,ConfigRowGrouping.MAX_NAME_SCALE));
        assertEquals(106,ConfigRowGrouping.displayWidth(60,160,ConfigRowGrouping.DEFAULT_NAME_SCALE));
    }

    @Test void dtOriginalNameHasPriorityWithoutOverridingOtherModScreens() throws IOException {
        String gui=Files.readString(Path.of("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java"));
        assertFalse(gui.contains("externalNameLabels"),
                "Do not access subclass-initialized collections during the parent constructor's virtual callback");
        assertTrue(gui.contains("if(firstLabel==null)return;"),
                "An external label subclass must not disable DT's own name rendering");
        assertTrue(gui.contains("plainLabel!=null?plainLabel:new WidgetLabel("),
                "DT must supply a native label when another mod replaces it");
        assertTrue(gui.contains("boolean externalFallback=!enabled&&hasExternalLabels"),
                "DT-off must leave the external label renderer in control");
        assertTrue(gui.contains("if(enabled&&hasExternalLabels"),
                "Foreign labels must only be suppressed when DT is enabled");
        assertTrue(gui.contains("subWidgets.removeIf(widget->widget instanceof WidgetLabel&&widget!=nativeNameLabel)"));
        assertTrue(gui.contains("if(!subWidgets.contains(nativeNameLabel))subWidgets.add(0,nativeNameLabel)"));
        assertTrue(gui.contains("}finally{"));
        assertTrue(gui.contains("subWidgets.addAll(originalWidgets)"),
                "External widgets must be restored after DT's local render");
        assertFalse(gui.contains("if(labelCount!=1||plainLabel==null)return;"));
        assertTrue(gui.contains("boolean hasExternalLabels=false;"),
                "Discover external labels at render time, after subclass field initialization");
    }

    @Test void originalNameUsesTweakerMoreRelativeContrast() throws IOException {
        assertEquals(0xFF999999,ConfigRowGrouping.darkerColor(0xFFFFFFFF));
        assertEquals(0x801E3D7A,ConfigRowGrouping.darkerColor(0x803366CC));
        assertEquals(0xFF000000,ConfigRowGrouping.darkerColor(0xFF000000));
        String gui=Files.readString(Path.of("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java"));
        assertTrue(gui.contains("SECONDARY_NAME_COLOR=ConfigRowGrouping.darkerColor(0xFFFFFFFF)"));
        assertFalse(gui.contains("0xFF888888"),"No fixed original-key gray may remain");
    }

    @Test void onlyChildrenOfExpandedParentsAreVisibleWithoutChangingTheirOrder(){
        String camera="camera",shortcut="shortcut",click="click",mode="mode",standalone="standalone";
        List<String> all=List.of(camera,shortcut,click,mode,standalone);
        Map<String,String> parent=new IdentityHashMap<>();
        parent.put(shortcut,camera);
        parent.put(mode,click);
        Set<String> expanded=new LinkedHashSet<>();

        assertEquals(List.of(camera,click,standalone),
                ConfigRowGrouping.visible(all,parent,expanded,false));
        expanded.add(camera);
        assertEquals(List.of(camera,shortcut,click,standalone),
                ConfigRowGrouping.visible(all,parent,expanded,false));
        expanded.add(click);
        assertEquals(all,ConfigRowGrouping.visible(all,parent,expanded,false));
        expanded.clear();
        assertEquals(all,ConfigRowGrouping.visible(all,parent,expanded,true),
                "Searching must find children even if their parent is folded");
        assertEquals(5,all.size(),"Disclosure must not mutate the registered config list");
    }

    @Test void labelWidthTracksVisibleIndentedNamesWithoutUsingConfigValues(){
        String parent=new String("compact"),child=new String("holdTab"),other=new String("snow");
        Map<String,String> parents=new IdentityHashMap<>();
        parents.put(child,parent);
        ToIntFunction<String> nameWidth=name->name==parent?96:name==child?144:110;

        int folded=ConfigRowGrouping.maxLabelWidth(List.of(parent,other),parents,nameWidth,14);
        int expanded=ConfigRowGrouping.maxLabelWidth(List.of(parent,child,other),parents,nameWidth,14);
        assertEquals(110,folded);
        assertEquals(158,expanded,"A child name must include its indentation in the label column");
        assertEquals(48,expanded-folded,"Every right-hand control should move by the same delta");
        assertEquals(158,ConfigRowGrouping.maxLabelWidth(List.of(other,child),parents,nameWidth,14),
                "Search results must also account for a visible child row");
        assertEquals(0,ConfigRowGrouping.maxLabelWidth(List.of(),parents,nameWidth,14));
    }

    @Test void rightSideControlsMoveTogetherAndUseOneAvailableWidth(){
        assertEquals(106,ConfigRowGrouping.displayWidth(60,160));
        assertEquals(120,ConfigRowGrouping.displayWidth(120,160));
        int entryWidth=395,preferred=260,gap=18,trailing=32;
        int folded=ConfigRowGrouping.controlWidth(entryWidth,72,preferred,gap,trailing);
        int expanded=ConfigRowGrouping.controlWidth(entryWidth,96,preferred,gap,trailing);
        assertEquals(260,folded);
        assertEquals(249,expanded);
        assertEquals(24,96-72,"An English child key should move every control start by 24px");
        assertEquals(11,folded-expanded,"Controls shrink together only when the Reset area would overflow");
        assertEquals(96+gap+expanded,363,"Slider/editor, ordinary buttons and Reset share the same column end");
        assertEquals(96+gap+expanded,72+gap+folded+13,
                "Only the available control space changes, never per-row positioning");
    }

    @Test void sameWidthAcrossTabsKeepsControlsAlignedToEachTabsVisibleLabels(){
        int entryWidth=395,preferred=260,gap=18,trailing=32;
        int clientLabel=72,tweakLabel=96;
        int sharedWidth=ConfigRowGrouping.controlWidth(entryWidth,Math.max(clientLabel,tweakLabel),
                preferred,gap,trailing);
        assertEquals(249,sharedWidth);
        assertEquals(sharedWidth,ConfigRowGrouping.controlWidth(entryWidth,tweakLabel,
                preferred,gap,trailing));
        int clientStart=clientLabel+gap,tweakStart=tweakLabel+gap;
        assertEquals(24,tweakStart-clientStart);
        assertEquals(24,(tweakStart+sharedWidth)-(clientStart+sharedWidth),
                "The complete control group follows each tab's label column without resizing");
    }

    @Test void featureGroupsExistOnEveryTabAndSpacingIsUniform() throws IOException {
        String gui=Files.readString(Path.of("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java"));
        for(String token:new String[]{
                "group(parents,Configs.Tweaks.CLICK_THROUGH,",
                "group(parents,Configs.Tweaks.AUTO_EAT,",
                "group(parents,Configs.Tweaks.CONFIRM_ITEM_DROP,",
                "group(parents,Configs.Tweaks.CAMERA_PLAYER_SELECTOR,Hotkeys.CAMERA_PLAYER_SELECTOR)",
                "group(parents,Configs.Tweaks.SCROLL_BOOK_PAGES,",
                "group(parents,Configs.Visual.GAMMA_OVERRIDE,",
                "group(parents,Configs.Visual.STACK_COUNT_SCALE_ENABLED,",
                "group(parents,Configs.Visual.COMPACT_STATUS_EFFECTS,",
                "group(parents,Configs.Visual.LOCATOR_HEADS,",
                "group(parents,Configs.Visual.BETTER_SNOW_COVERAGE,",
                "group(parents,Configs.Tweaks.AUTO_LOG_CLEANUP,",
                "group(parents,Configs.Tweaks.SHOW_ORIGINAL_CONFIG_NAME,Configs.Tweaks.ORIGINAL_CONFIG_NAME_SCALE)",
                "LABEL_TO_CONTROL_GAP=8",
                "CHILD_LABEL_INDENT=14",
                "MALILIB_LABEL_GAP=10",
                "RESET_AND_EDGE_SPACE=32",
                "ConfigRowGrouping.displayWidth(getStringWidth(config.getConfigGuiDisplayName())",
                "ConfigRowGrouping.controlWidth(browserEntryWidth,sharedLabelWidth,",
                "for(ConfigGuiTab configTab:ConfigGuiTab.values())",
                "sharedLabelWidth=Math.max(sharedLabelWidth,visibleLabelWidth(tabOptions))",
                "ConfigRowGrouping.maxLabelWidth(options,PARENTS,",
                "this::labelWidth,CHILD_LABEL_INDENT)",
                "return PARENTS.containsKey(config)?CHILD_LABEL_INDENT:0;",
                "new ButtonGeneric(x+labelWidth+10+LABEL_TO_CONTROL_GAP,y+1,20,20",
                "disclosure=isGroupParent(config)?22:0",
                "width-disclosure-LABEL_TO_CONTROL_GAP,config",
                "getListWidget().refreshEntries()",
                "rows(!getFilterText().isEmpty())"}) {
            assertTrue(gui.contains(token),token);
        }
        int labelEnd=100,arrowX=labelEnd+10+8,controlX=labelEnd+10+22+8;
        assertEquals(controlX,arrowX+20+2,
                "Arrow and the adjacent setting must share one row with a 2 GUI-pixel gap");
        assertTrue(gui.contains("new ButtonGeneric(x+labelWidth+10+LABEL_TO_CONTROL_GAP,y+1,20,20"));
        assertFalse(gui.contains("new ButtonGeneric(x+labelWidth+10,y+1,20,20"));
        assertFalse(gui.contains("locatorExpanded"));
        assertFalse(gui.contains("if(Configs.Visual.LOCATOR_BORDER.getBooleanValue())"));
        assertTrue(gui.contains("ConfigOptionWrapper.createFor(ConfigRowGrouping.visible("));
        assertFalse(gui.contains("return ConfigOptionWrapper.createFor(options)"),
                "Legacy views must share the complete parent/child grouping contract");
        assertTrue(gui.contains("class OriginalNameConfigOption extends WidgetConfigOption"));
        assertTrue(gui.contains("class LegacyConfigList extends WidgetListConfigOptions"));
        assertTrue(gui.contains("class LegacyConfigOption extends OriginalNameConfigOption"));
        assertTrue(gui.contains("captureOriginalName(x+indent,y,Math.max(20,labelWidth-indent),config)"));
        assertFalse(gui.contains("legacyScaleExpanded"));
        assertTrue(gui.contains("int disclosure=PARENTS.containsValue(config)?22:0"));
        assertTrue(gui.contains("EXPANDED.contains(config)?MaLiLibIcons.ARROW_UP:MaLiLibIcons.ARROW_DOWN"));
        assertTrue(gui.contains("addButton(arrow,(button,mouse)->screen.toggleGroup(config))"));
        assertTrue(gui.contains("return (a,b)->Integer.compare(order.indexOf(a.getConfig()),order.indexOf(b.getConfig()))"));
        assertTrue(gui.contains("screen.rows(!getFilterText().isEmpty())"));
        assertTrue(gui.contains("super.addConfigOption(x+indent,y,zLevel,Math.max(20,labelWidth-indent)+disclosure+LABEL_TO_CONTROL_GAP"));
        assertTrue(gui.contains("onMouseReleasedImpl(")==false,
                "The option row must not intercept MaLiLib's mouse release path");
        assertFalse(gui.contains("/"+"//$".substring(1)+" "), 
                "Preprocessed legacy branches must use exactly two dollar signs");
    }
}
