package ddstweak.gui;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class SharedItemPickerContractTest {
    private static String read(String relative) throws Exception {
        return Files.readString(Path.of("").toAbsolutePath().resolve(relative));
    }

    @Test void sharedPickerHasBothStableModesAndScopes() throws Exception {
        String picker = read("src/main/java/ddstweak/gui/itempicker/ItemPicker.java");
        String kinds = read("src/main/java/ddstweak/gui/itempicker/ItemPickerKind.java");
        String screen = read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(picker.contains("Screen multi(Screen parent, IConfigStringList target, ItemPickerKind kind"));
        assertTrue(picker.contains("Screen single(Screen parent, String selectedId, String defaultId"));
        assertTrue(picker.contains("Screen multi(Screen parent, Collection<String> initialIds"));
        assertTrue(screen.contains("Consumer<List<String>> multiSelection"));
        assertTrue(screen.contains("multiSelection.accept(new ArrayList<>(stagedIds))"));
        assertTrue(screen.contains("if(initialIds!=null)stagedIds.addAll(new LinkedHashSet<>(initialIds))"));
        assertTrue(screen.contains("setMultiValues(Collections.emptyList())"));
        assertTrue(kinds.contains("ITEM, FOOD, BLOCK"));
        assertTrue(kinds.contains("SOUND"));
        assertTrue(screen.contains("private final ItemPickerKind kind;"));
        assertFalse(screen.contains("public enum Kind{"));
        assertTrue(screen.contains("if(target!=null)new Configs().save()"));
        assertTrue(screen.contains("singleSelection.accept(singleSelectedId)"));
        assertTrue(screen.contains("if(event.isEscape()){onClose();return true;}"));
    }

    @Test void configurationBindingsAreByIdentityNotHardCodedInMixin() throws Exception {
        String bindings = read("src/main/java/ddstweak/gui/itempicker/ItemPickerConfigBindings.java");
        String mixin = read("src/main/java/ddstweak/mixin/feature/dropconfirmation/DropConfirmStringListButtonMixin.java");
        assertTrue(bindings.contains("new IdentityHashMap<>()"));
        assertTrue(bindings.contains("BINDINGS.get(config)"));
        assertTrue(bindings.contains("BINDINGS.containsKey(config)"));
        assertTrue(bindings.contains("if (binding == null) return false;"));
        assertTrue(mixin.contains("ItemPickerConfigBindings.open(GuiUtils.getCurrentScreen(),config)"));
        for (String config : new String[] {
                "AUTO_EAT_FOOD_BLACKLIST", "DROP_CONFIRM_BLACKLIST", "DROP_CONFIRM_WHITELIST",
                "BETTER_SNOW_COVERAGE_BLACKLIST", "BETTER_SNOW_COVERAGE_WHITELIST",
                "BLOCK_SOUND_BLOCK_BLACKLIST", "BLOCK_SOUND_BLOCK_WHITELIST"
        }) assertTrue(bindings.contains(config), config);
        assertTrue(bindings.contains("//#if MC >= 12111"));
        assertFalse(mixin.contains("\"dropConfirmBlacklist\""));
        assertFalse(mixin.contains("\"betterSnowCoverageBlacklist\""));
    }


    @Test void restockListsUseTheSharedItemPickerOnAllSupportedTargets() throws Exception {
        String bindings = read("src/main/java/ddstweak/gui/itempicker/ItemPickerConfigBindings.java")
                .replace("\r\n", "\n");
        int modernGate = bindings.indexOf("        //#if MC >= 12111\n");
        assertTrue(modernGate > 0, "Modern-only block/sound pickers must retain their version gate");
        for (String name : new String[]{"SHULKER_RESTOCK_WHITELIST", "SHULKER_RESTOCK_BLACKLIST"}) {
            int registration = bindings.indexOf("register(Configs.Tweaks." + name + ", ItemPickerKind.ITEM,");
            assertTrue(registration >= 0 && registration < modernGate,
                    name + " must be registered before the modern-version gate");
        }

        int start = bindings.indexOf("public static boolean isRestockList(IConfigStringList config)");
        assertTrue(start >= 0);
        String method = bindings.substring(start, bindings.indexOf("\n    }", start));
        assertFalse(method.contains("//#if"), "Restock validation must remain available on legacy targets");
        assertTrue(method.contains("Configs.Tweaks.SHULKER_RESTOCK_WHITELIST"));
        assertTrue(method.contains("Configs.Tweaks.SHULKER_RESTOCK_BLACKLIST"));

        String screen = read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java")
                .replace("\r\n", "\n");
        int importStart = screen.indexOf("import ddstweak.gui.itempicker.ItemPickerConfigBindings;");
        int importEnd = screen.indexOf("import ddstweak.feature.shulkerrestock.RestockPreferencePolicy;");
        assertTrue(importStart >= 0 && importEnd > importStart);
        String validatorImports = screen.substring(importStart, importEnd);
        assertFalse(validatorImports.contains("//#if") || validatorImports.contains("//#else"),
                "Restock validator import must be present on legacy Minecraft targets");
        int setterStart = screen.indexOf("private void setMultiValues(List<String> next)");
        int setterEnd = screen.indexOf("private Set<String> selectedIds()", setterStart);
        assertTrue(setterStart >= 0 && setterEnd > setterStart);
        String setter = screen.substring(setterStart, setterEnd);
        assertFalse(setter.contains("//#if") || setter.contains("//#else") || setter.contains("//#endif"),
                "Restock validation must be compiled into all Minecraft targets");
        assertTrue(setter.contains("ItemPickerConfigBindings.isRestockList(target)"));
        assertTrue(setter.contains("!RestockPreferencePolicy.valid(next)"));
        assertTrue(setter.indexOf("!RestockPreferencePolicy.valid(next)") < setter.indexOf("target.setStrings(next)"),
                "Invalid restock IDs must be rejected before writing MaLiLib config");
    }


    @Test void itemPickerDrawsBaseAfterVanillaBackgroundBlurOnModernScreens() throws Exception {
        String screen = read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        int modernBranch = screen.indexOf("//#elseif MC >= 12002");
        int legacyBranch = screen.indexOf("//#elseif MC >= 12000", modernBranch);
        assertTrue(modernBranch >= 0 && legacyBranch > modernBranch);
        String modern = screen.substring(modernBranch, legacyBranch);
        int background = modern.indexOf("super.renderBackground(graphics,mouseX,mouseY,partialTick)");
        int base = modern.indexOf("renderBase(graphics)");
        assertTrue(background >= 0 && base > background,
                "Blur must run before picker text and separator lines");
        String render = modern.substring(modern.indexOf("@Override public void render(GuiGraphics"));
        assertTrue(render.indexOf("super.render(graphics,mouseX,mouseY,partialTick)") >= 0);
        assertTrue(render.indexOf("super.render(graphics,mouseX,mouseY,partialTick)")
                < render.indexOf("renderOverlay(graphics,mouseX,mouseY)"));
        assertTrue(modern.indexOf("//#if MC >= 12005") < base,
                "1.20.5+ background blur must precede Picker labels");
        assertTrue(render.contains("//#if MC < 12005")
                        && render.indexOf("renderBackground(graphics,mouseX,mouseY,partialTick)")
                                < render.indexOf("renderBase(graphics)")
                        && render.indexOf("renderBase(graphics)") < render.indexOf("super.render(graphics"),
                "1.20.2/1.20.4 must render the background before Picker labels and widgets");
    }

    @Test void extractorPickerBaseIsAfterBackgroundBeforeWidgets() throws Exception {
        String screen = read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java")
                .replace("\r\n", "\n");
        int begin = screen.indexOf("//#if MC >= 260100\n    //$$ @Override public void extractBackground");
        int end = screen.indexOf("//#elseif MC >= 12002", begin);
        assertTrue(begin >= 0 && end > begin);
        String extractor = screen.substring(begin, end);
        assertTrue(extractor.contains("super.extractBackground(graphics,mouseX,mouseY,partialTick);renderBase(graphics)"));
        assertTrue(extractor.contains("super.extractRenderState(graphics,mouseX,mouseY,partialTick);renderOverlay(graphics,mouseX,mouseY)"));
        assertFalse(extractor.contains("renderBase(graphics);super.extractRenderState"),
                "Extractor base must not be blurred beneath the screen background");
    }

    @Test void legacyPickerUsesVanillaGuiLightingAndRestoresState() throws Exception {
        String screen = read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java")
                .replace("\r\n", "\n");
        assertTrue(screen.contains("//#if MC < 11500\n//$$ import com.mojang.blaze3d.platform.GlStateManager;\n//$$ import com.mojang.blaze3d.platform.Lighting;"));
        assertFalse(screen.contains("Lighting.turnBackOn()"),
                "The 1.14.4 mapping uses turnOnGui(), not the later turnBackOn()");
        assertFalse(screen.contains("net.minecraft.client.renderer.RenderHelper"));
        assertFalse(screen.contains("GLX.glMultiTexCoord2f"));
        assertFalse(screen.contains("configureGuiLight"));

        int begin = screen.indexOf("private void renderPickerItem(");
        int end = screen.indexOf("private String fit(", begin);
        assertTrue(begin >= 0 && end > begin);
        String draw = screen.substring(begin, end);
        int save = draw.indexOf("GlStateManager.pushLightingAttributes()");
        int white = draw.indexOf("GlStateManager.color4f(1.0F,1.0F,1.0F,1.0F)");
        int normals = draw.indexOf("GlStateManager.enableRescaleNormal()");
        int blend = draw.indexOf("GlStateManager.enableBlend()");
        int blendFunc = draw.indexOf("GlStateManager.blendFuncSeparate(770,771,1,0)");
        int lighting = draw.indexOf("Lighting.turnOnGui()");
        int item = draw.indexOf("ScreenRenderCompat.item(graphics,stack,x,y)");
        int off = draw.indexOf("Lighting.turnOff()");
        int restore = draw.indexOf("GlStateManager.popAttributes()");
        assertTrue(save >= 0 && save < white && white < normals && normals < blend
                        && blend < blendFunc && blendFunc < lighting && lighting < item
                        && item < off && off < restore,
                "Match vanilla GUI setup and always restore lighting after rendering each item");
        assertTrue(draw.contains("try {") && draw.contains("} finally {"));
        assertFalse(screen.contains("//$ "), "Preprocessor markers must use two dollar signs");
        assertFalse(screen.contains("ScreenRenderCompat.item(graphics,ItemRegistryCompat.stack("),
                "All picker icon call sites must use the scoped item-rendering helper");
    }

    @Test void cameraUsesPickerWithoutDependingOnDropConfirmationPackage() throws Exception {
        String editor = read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        assertTrue(editor.contains("import ddstweak.gui.itempicker.ItemPicker;"));
        assertTrue(editor.contains("ItemPicker.single("));
        assertFalse(editor.contains("import ddstweak.feature.dropconfirmation.DropConfirmationItemScreen;"));
        assertTrue(editor.contains("this,iconId,CameraDestination.DEFAULT_ICON_ID,this::setIcon"));
    }

    @Test void pickerRetainsNativeSearchTabAndLegacyCompatibility() throws Exception {
        String screen = read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        for (String feature : new String[] {
                "CreativeTabCompat.tabs(Minecraft.getInstance(),allItems)",
                "ItemRegistryCompat.allItems()", "ItemPickerFocusCompat.unfocus(searchField)",
                "validRawId(id)", "value.startsWith(\"#\")", "ItemPickerKind.BLOCK",
                "InputConstants.MOUSE_BUTTON_LEFT", "selectedRowOffset", "itemRowOffset",
                "target.setStrings(next)"
        }) assertTrue(screen.contains(feature), feature);
        assertTrue(screen.contains("//#if MC >= 260300"));
        assertTrue(screen.contains("//#elseif MC >= 12000"));
    }
}
