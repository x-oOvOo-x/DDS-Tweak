package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class DropConfirmationContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {
        Path f=ROOT.resolve(p);
        return Files.exists(f)?Files.readString(f):"";
    }

    @Test void featureIsOptInAndUsesThreeListModes() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String mode=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmMode.java");
        assertTrue(configs.contains("CONFIRM_ITEM_DROP=MalilibCompat.bool(\"confirmItemDrop\",false,TWEAKS_KEY)"));
        assertTrue(configs.contains("DROP_CONFIRM_MODE=MalilibCompat.optionList(\"dropConfirmMode\",DropConfirmMode.ALL,TWEAKS_KEY)"));
        assertTrue(configs.contains("DROP_CONFIRM_BLACKLIST=MalilibCompat.stringList(\"dropConfirmBlacklist\""));
        assertTrue(configs.contains("DROP_CONFIRM_WHITELIST=MalilibCompat.stringList(\"dropConfirmWhitelist\""));
        assertTrue(configs.contains("DROP_CONFIRM_ITEMS_LEGACY=MalilibCompat.stringList(\"dropConfirmItems\""));
        assertTrue(configs.contains("migrateDropConfirmLists()"));
        for(String value:new String[]{"ALL","WHITELIST","BLACKLIST"})assertTrue(mode.contains(value+"("),value);
    }

    @Test void onlyWorldDropPathIsIntercepted() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/dropconfirmation/WorldDropConfirmationMixin.java");
        String json=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(mixin.contains("@Mixin(LocalPlayer.class)"));
        assertTrue(mixin.contains("@Mixin(MultiPlayerGameMode.class)"));
        assertTrue(mixin.contains("method=\"drop\""));
        assertTrue(mixin.contains("method=\"dropItem\""));
        assertFalse(mixin.contains("AbstractContainerScreen"));
        assertFalse(mixin.contains("ClickType.THROW"));
        assertTrue(json.contains("feature.dropconfirmation.WorldDropConfirmationMixin"));
    }

    @Test void secondPressMustMatchSelectedStackItemCountAndDropModeWithinOneSecond() throws Exception {
        String guard=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationGuard.java");
        assertTrue(guard.contains("WINDOW_NS=1_000_000_000L"));
        for(String token:new String[]{"this.stack==stack","this.item==item","this.count==count","this.entireStack==entireStack","now<=expiresAt"})
            assertTrue(guard.contains(token),token);
        assertTrue(guard.contains("MinecraftCompat.screen(minecraft)!=null"));
    }

    @Test void whitelistBypassesListedItemsAndBlacklistProtectsListedItems() throws Exception {
        String guard=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationGuard.java");
        assertTrue(guard.contains("DropConfirmationItems.blacklistContains"));
        assertTrue(guard.contains("DropConfirmationItems.whitelistContains"));
    }

    @Test void iconPickerIsOwnedByDdsTweakAndPersistsIds() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String items=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItems.java");
        String configScreen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        String buttonMixin=read("src/main/java/ddstweak/mixin/feature/dropconfirmation/DropConfirmStringListButtonMixin.java");
        assertTrue(screen.contains("ScreenRenderCompat.item"));
        assertTrue(screen.contains("IConfigStringList target"));
        assertTrue(screen.contains("target.setStrings(next)"));
        assertTrue(read("src/main/java/ddstweak/gui/itempicker/ItemPickerKind.java").contains("ITEM, FOOD, BLOCK"));
        assertTrue(screen.contains("selectedRowOffset"));
        assertTrue(screen.contains("selectedColumns"));
        assertTrue(screen.contains("ItemRegistryCompat.allItems"));
        assertTrue(screen.contains("query"));
        assertTrue(items.contains("Configs.Tweaks.DROP_CONFIRM_BLACKLIST"));
        assertTrue(items.contains("Configs.Tweaks.DROP_CONFIRM_WHITELIST"));
        assertTrue(buttonMixin.contains("ConfigButtonStringList"));
        assertTrue(read("src/main/java/ddstweak/gui/itempicker/ItemPickerConfigBindings.java").contains("Configs.Tweaks.DROP_CONFIRM_BLACKLIST"));
        assertTrue(read("src/main/java/ddstweak/gui/itempicker/ItemPickerConfigBindings.java").contains("Configs.Tweaks.DROP_CONFIRM_WHITELIST"));
        assertFalse(configScreen.contains("createDropConfirmItemsButton"));
        assertFalse(configScreen.contains("fi.dy.masa.tweakeroo"));
    }

    @Test void actionBarUsesActualDropKeyAndSeparateWholeStackPrompt() throws Exception {
        String guard=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationGuard.java");
        String compat=read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        assertTrue(guard.contains("drop_confirm.prompt.single"));
        assertTrue(guard.contains("drop_confirm.prompt.stack"));
        assertTrue(compat.contains("keyDrop.getTranslatedKeyMessage"));
        assertTrue(compat.contains("setOverlayMessage"));
    }
    @Test void actionBarPromptDoesNotRepeatItemNameOrCount() throws Exception {
        String guard=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationGuard.java");
        String zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");
        assertTrue(guard.contains("translatable(\"dds-tweak.drop_confirm.prompt.single\",key)"));
        assertTrue(guard.contains("translatable(\"dds-tweak.drop_confirm.prompt.stack\",key)"));
        assertFalse(guard.contains("getHoverName"));
        assertTrue(zh.contains("\"dds-tweak.drop_confirm.prompt.single\": \"再次按 %s 丢弃\""));
        assertTrue(zh.contains("\"dds-tweak.drop_confirm.prompt.stack\": \"再次按 Ctrl + %s 丢弃整组\""));
    }

    @Test void sharedPickerUsesCameraSizedTwoPaneLayoutAndVanillaControls() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String camera=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraBackground.java");
        String shared=read("src/main/java/ddstweak/gui/SharedScreenBackground.java");
        for(String token:new String[]{
                "NAV_HEIGHT=222",
                "HEADER_RULE_Y=16,FOOTER_RULE_Y=192,FOOTER_ROW_Y=200",
                "navigationTop(int height)",
                "void drawNavigationRules(",
                "void rule("
        })assertTrue(shared.contains(token),token);
        assertTrue(camera.contains("SharedScreenBackground.draw("));
        assertTrue(camera.contains("SharedScreenBackground.drawNavigationRules("));
        assertTrue(screen.contains("SharedScreenBackground.navigationTop(height)"));
        assertTrue(screen.contains("SharedScreenBackground.drawNavigationRules("));
        assertTrue(screen.contains("0x60000000"));
        assertTrue(screen.contains("(innerWidth-10)*28/100"));
        assertTrue(screen.contains("l.selectedLeft"));
        assertTrue(screen.contains("l.rightLeft"));
        assertTrue(screen.contains("SharedScreenBackground.rule(graphics,l.selectedLeft"));
        assertTrue(screen.contains("SharedScreenBackground.rule(graphics,l.rightLeft"));
        assertTrue(screen.contains("tr(\"dds-tweak.list_picker.selected_header\""));
        assertTrue(screen.contains("kind==ItemPickerKind.SOUND?\"dds-tweak.list_picker.sound_search_title\":"),
                "Sound picker must use a dedicated search title");
        assertTrue(screen.contains("\"dds-tweak.list_picker.search_title\""),
                "Other picker kinds must retain the shared item search title");
        assertTrue(screen.contains("searchField=new EditBox("));
        assertTrue(screen.contains("return Button.builder("));
        assertTrue(screen.contains("searchField.setResponder("));
        assertTrue(screen.contains("categoryOffset"));
        assertTrue(screen.contains("selectedRowOffset"));
        assertTrue(screen.contains("itemRowOffset"));
        assertTrue(screen.contains("remove(selected);"));
        assertTrue(screen.contains("toggleSoundGroup(selected,false)"));
        assertTrue(java.util.regex.Pattern.compile("remove\\(selected\\);\\s*refresh\\(\\);")
                        .matcher(screen).find(),
                "Removing a selected source must refresh both panes regardless of preprocessing whitespace");
        assertTrue(screen.contains("toggle(item);refresh()"));
        assertTrue(screen.contains("entry.id.startsWith(\"#\")"));
        assertFalse(screen.contains("l.chipWidth"));
        assertFalse(screen.contains("searchFocused"));
        assertFalse(screen.contains("text(graphics,fit(label"));
        String zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");
        String en=read("src/main/resources/assets/dds-tweak/lang/en_us.json");
        assertTrue(zh.contains("\"dds-tweak.list_picker.search_title\""));
        assertTrue(en.contains("\"dds-tweak.list_picker.search_title\""));
    }

    @Test void pickerHeaderRulesAndFooterMatchCameraDestinationEditor() throws Exception {
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String background=read("src/main/java/ddstweak/gui/SharedScreenBackground.java");
        // Same 380-wide frame gives both screens the 360-wide centered navigation rules.
        String width="int panelWidth=Math.min(380,Math.max(250,width-24))";
        assertTrue(picker.contains(width));
        assertTrue(editor.contains(width));
        assertTrue(picker.contains("int innerLeft=left+10,innerWidth=panelWidth-20"));
        assertTrue(editor.contains("int innerLeft=left+10,innerWidth=panelWidth-20"));
        assertTrue(background.contains("int ruleWidth=Math.min(360,width-16)"));
        assertTrue(picker.contains("centered(graphics,tr(titleKey),width/2,l.top,0xFFFFFFFF)"));
        assertFalse(picker.contains("centered(graphics,tr(titleKey),width/2,l.top+2"));
        // Both footer buttons use the Camera editor's 72×20 sizing and 4px right-aligned gap.
        assertTrue(editor.contains("BUTTON_H=20"));
        assertTrue(editor.contains("FOOTER_WIDTH=72"));
        assertTrue(editor.contains("int footerGap=4"));
        assertTrue(picker.contains("BUTTON_H=20"));
        assertTrue(picker.contains("FOOTER_WIDTH=72,FOOTER_GAP=4"));
        assertTrue(picker.contains("int doneX=l.rightLeft+l.rightWidth-FOOTER_WIDTH"));
        assertTrue(picker.contains("int clearX=doneX-FOOTER_GAP-FOOTER_WIDTH"));
        assertTrue(picker.contains("top+SharedScreenBackground.FOOTER_ROW_Y"));
        assertTrue(editor.contains("bottomY=top+CameraBackground.FOOTER_ROW_Y"));
        assertTrue(picker.contains("SharedScreenBackground.rule(graphics,l.selectedLeft"));
        assertTrue(picker.contains("SharedScreenBackground.rule(graphics,l.rightLeft"));
    }

    @Test void pickerPreservesIdSearchTagAndSingleSelectionContracts() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(screen.contains("ItemRegistryCompat.id"));
        assertTrue(screen.contains("validRawId(id)"));
        assertTrue(screen.contains("searchField.setValue(\"\")"));
        assertTrue(screen.contains("singleSelection.accept(singleSelectedId)"));
        assertTrue(screen.contains("if(target!=null)new Configs().save()"));
        assertTrue(screen.contains("if(event.isEscape()){onClose();return true;}"));
        assertTrue(screen.contains("InputConstants.MOUSE_BUTTON_LEFT"));
    }

    @Test void itemPickerUsesActualCreativeTabsAndGlobalSearch() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String creative=read("src/main/java/ddstweak/feature/dropconfirmation/CreativeTabCompat.java");
        String registry=read("src/main/java/ddstweak/feature/dropconfirmation/ItemRegistryCompat.java");
        String zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");
        for(String token:new String[]{
                "CreativeTabCompat.tabs(Minecraft.getInstance(),allItems)",
                "private int categoryIndex",
                "CreativeTabCompat.Tab active=categories.get",
                "!searching&&!active.contains(item)",
                "categoryIndex=clickedCategory",
                "tab.icon()",
                "tab.title()"
        })assertTrue(screen.contains(token),token);
        for(String token:new String[]{
                "CreativeModeTabs.tryRebuildTabContents",
                "CreativeModeTabs.tabs()",
                "tab.getType()!=CreativeModeTab.Type.CATEGORY",
                "tab.getDisplayItems()",
                "CreativeModeTab.TABS",
                "tab.fillItemList(stacks)"
        })assertTrue(creative.contains(token),token);
        assertFalse(registry.contains("DropItemCategory"));
        assertFalse(registry.contains("instanceof BlockItem"));
        assertFalse(registry.contains("redstone"));
        assertTrue(creative.contains("for(Tab tab:result)all.addAll(tab.items)"));
        assertTrue(creative.contains("result.add(0,Tab.all(all))"));
        assertTrue(creative.contains("tabs(Minecraft minecraft,Collection<Item> candidates)"));
        assertTrue(creative.contains("if(!matching.isEmpty())"));
        assertTrue(screen.contains("CreativeTabCompat.tabs(Minecraft.getInstance(),allItems)"));
        assertTrue(zh.contains("\"dds-tweak.list_picker.category.all\""));
    }

    @Test void blockIdEntryRequiresFocusedSearchField() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(screen.contains("if(searchField==null||!searchField.isFocused())return false;"));
        assertTrue(screen.contains("if(kind!=ItemPickerKind.BLOCK&&kind!=ItemPickerKind.SOUND_BLOCK&&kind!=ItemPickerKind.SOUND)return false;"));
        assertTrue(screen.contains("//$$ if(kind!=ItemPickerKind.BLOCK)return false;"));
        assertTrue(screen.contains("private void unfocusSearch()"));
        assertTrue(screen.contains("unfocusSearch();"));
        assertTrue(screen.contains("//$$     unfocusSearch();"));
        String focus=read("src/main/java/ddstweak/platform/minecraft/ItemPickerFocusCompat.java");
        assertTrue(screen.contains("ItemPickerFocusCompat.unfocus(searchField);"));
        assertTrue(screen.contains("//#elseif MC >= 12000"));
        assertFalse(screen.contains("//#elseif MC >= 11904"));
        assertTrue(focus.contains("//#if MC >= 11904"));
        assertTrue(focus.contains("field.setFocused(false);"));
        assertTrue(focus.contains("//$$ field.setFocus(false);"));
        assertFalse(screen.contains("searchField.setFocused(false);"));
        assertTrue(screen.contains("if(event.key()==257&&addQueryEntry())return true;"));
        assertTrue(screen.contains("searchField.setValue(\"\")"));
        assertTrue(screen.contains("if(!validRawId(id))return false"));
        assertTrue(screen.contains("if(!next.contains(value)){next.add(value);setMultiValues(next);}"));
    }

    @Test void itemPickerUsesVanillaBoundedTooltipsForAllEntries() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String camera=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(screen.contains("showTooltip(graphics,mouseX,mouseY,ItemRegistryCompat.name(item),entryId(item),state)"));
        assertTrue(screen.contains("showTooltip(graphics,mouseX,mouseY,value)"));
        assertTrue(screen.contains("private void appendTooltipLines("));
        assertTrue(screen.contains("int maxWidth=Math.max(32,width-24)"));
        assertTrue(screen.contains("font.width(value.substring(start,next))>maxWidth"));
        assertTrue(screen.contains("value.offsetByCodePoints(end,1)"));
        assertTrue(screen.contains("setTooltipForNextFrame(font,lines,Optional.empty(),mouseX,mouseY)"));
        assertTrue(camera.contains("renderComponentTooltip(graphics,lines,mouseX,mouseY)"));
        assertTrue(screen.contains("renderComponentTooltip(graphics,lines,mouseX,mouseY)"));
        assertFalse(screen.contains("0xF0101010"));
    }

    @Test void itemPickerShowsRegistryItemsOutsideLoadedWorld() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String creative=read("src/main/java/ddstweak/feature/dropconfirmation/CreativeTabCompat.java");
        assertTrue(screen.contains("this.categories=CreativeTabCompat.tabs(Minecraft.getInstance(),allItems)"));
        assertTrue(creative.contains("result.add(Tab.all(allowed))"));
        assertTrue(creative.contains("if(minecraft.level==null)return Collections.singletonList(Tab.all(Collections.<Item>emptySet()))"));
        assertTrue(screen.contains("for(Item item:allItems)"));
        assertTrue(screen.contains("if(!searching&&!active.contains(item))continue"));
    }

    @Test void betterSnowListsReuseCompactPickerAndPreserveBlockTags() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String bindings=read("src/main/java/ddstweak/gui/itempicker/ItemPickerConfigBindings.java");
        for(String token:new String[]{
                "Configs.Visual.BETTER_SNOW_COVERAGE_BLACKLIST",
                "Configs.Visual.BETTER_SNOW_COVERAGE_WHITELIST",
                "ItemPickerKind.BLOCK"
        })assertTrue(bindings.contains(token),token);
        assertTrue(screen.contains("item instanceof BlockItem"));
        assertTrue(screen.contains("value.startsWith(\"#\")"));
        assertTrue(screen.contains("addQueryEntry()"));
        assertTrue(screen.contains("for(String id:selectedIds())result.add(new SelectedEntry(id,byId.get(id)))"));
    }

}
