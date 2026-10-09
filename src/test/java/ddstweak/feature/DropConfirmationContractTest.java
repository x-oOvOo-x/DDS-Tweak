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
        assertTrue(screen.contains("public enum Kind{ITEM,FOOD,BLOCK}"));
        assertTrue(screen.contains("selectedRowOffset"));
        assertTrue(screen.contains("chipWidth"));
        assertTrue(screen.contains("ItemRegistryCompat.allItems"));
        assertTrue(screen.contains("query"));
        assertTrue(items.contains("Configs.Tweaks.DROP_CONFIRM_BLACKLIST"));
        assertTrue(items.contains("Configs.Tweaks.DROP_CONFIRM_WHITELIST"));
        assertTrue(buttonMixin.contains("ConfigButtonStringList"));
        assertTrue(buttonMixin.contains("\"dropConfirmBlacklist\""));
        assertTrue(buttonMixin.contains("\"dropConfirmWhitelist\""));
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

    @Test void itemPickerPreservesScrollAndUsesSubtleSectionSpacing() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        for(String token:new String[]{
                "SEPARATOR_COLOR=0x40606060",
                "separator(graphics,l.innerLeft,l.selectedTop-3,l.innerWidth)",
                "separator(graphics,l.innerLeft,l.searchTop-4,l.innerWidth)",
                "separator(graphics,l.innerLeft,l.bottomTop-4,l.innerWidth)",
                "BUTTON_H=16,BOTTOM_MARGIN=6,BOTTOM_GAP=6",
                "private void refreshQuery()",
                "remove(selected);refresh()",
                "toggle(item);refresh()",
                "private boolean searchFocused",
                "searchFocused?0xD0282828:0xC0202020",
                "searchFocused?0xFFC0C0C0:0xFF606060",
                "if(inside(mouseX,mouseY,l.innerLeft,l.searchTop,l.innerWidth,18)){searchFocused=true;return true;}",
                "searchFocused=false;",
                "System.currentTimeMillis()/500L"
        })assertTrue(screen.contains(token),token);
        assertFalse(screen.contains("itemRowOffset=0;selectedRowOffset=0"));
        assertTrue(screen.replace("\r\n","\n").contains("itemRowOffset=0;\n        refresh();"));
    }

    @Test void itemPickerUsesActualCreativeTabsAndGlobalSearch() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String creative=read("src/main/java/ddstweak/feature/dropconfirmation/CreativeTabCompat.java");
        String registry=read("src/main/java/ddstweak/feature/dropconfirmation/ItemRegistryCompat.java");
        String zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");
        for(String token:new String[]{
                "CreativeTabCompat.tabs(Minecraft.getInstance())",
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
        assertTrue(screen.contains("kind==Kind.FOOD"));
        assertTrue(screen.contains("CreativeTabCompat.tabs(Minecraft.getInstance(),allItems)"));
        assertTrue(zh.contains("\"dds-tweak.list_picker.category.all\""));
    }

    @Test void betterSnowListsReuseCompactPickerAndPreserveBlockTags() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String mixin=read("src/main/java/ddstweak/mixin/feature/dropconfirmation/DropConfirmStringListButtonMixin.java");
        for(String token:new String[]{
                "\"betterSnowCoverageBlacklist\"",
                "\"betterSnowCoverageWhitelist\"",
                "DropConfirmationItemScreen.Kind.BLOCK"
        })assertTrue(mixin.contains(token),token);
        assertTrue(screen.contains("item instanceof BlockItem"));
        assertTrue(screen.contains("value.startsWith(\"#\")"));
        assertTrue(screen.contains("addQueryEntry()"));
        assertTrue(screen.contains("for(String id:selectedIds())result.add(new SelectedEntry(id,byId.get(id)))"));
    }

}
