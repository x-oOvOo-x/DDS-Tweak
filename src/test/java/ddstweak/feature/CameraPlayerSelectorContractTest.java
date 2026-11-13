package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class CameraPlayerSelectorContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}

    @Test void selectorIsOptOutAndPressToOpen() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String hotkeys=read("src/main/java/ddstweak/config/Hotkeys.java");
        String feature=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorFeature.java");
        assertTrue(configs.contains("cameraPlayerSelector\",true"));
        assertTrue(hotkeys.contains("cameraPlayerSelector\",\"R"));
        assertTrue(feature.contains("setCallback((action,key)->open())"));
        assertTrue(feature.contains("MinecraftCompat.screen(minecraft)!=null"));
        assertFalse(feature.contains("isKeybindHeld()"));
        assertFalse(feature.contains("isPhysicalHotkeyHeld"));
        assertFalse(feature.contains("suppressUntilRelease"));
        assertFalse(feature.contains("ClientTickEvents"));
    }

    @Test void configuredTemplatesAreSentWithoutClientCapabilityFiltering() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        String feature=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorFeature.java");
        String profile=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandProfile.java");
        String store=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandProfileStore.java");
        assertFalse(compat.contains("root.getChild(rootName)"));
        assertTrue(compat.contains("configured(CameraCommandProfile profile,CameraCommandProfile.Action action)"));
        assertTrue(compat.contains("profile.renderPlayer(action,name)"));
        assertTrue(compat.contains("CameraCommandSyntax.send(minecraft,profile.renderPlayer(action,name))"));
        String syntax=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandSyntax.java");
        assertTrue(syntax.contains("MinecraftCompat.sendCommandWithoutHistory(minecraft,value)"));
        assertTrue(syntax.contains("MinecraftCompat.sendChatWithoutHistory(minecraft,value)"));
        assertFalse(compat.contains("sendServerCommandSilently"));
        assertTrue(feature.contains("CameraCommandProfileStore.load(minecraft)"));
        assertTrue(profile.contains("/c tp {player}"));
        assertTrue(profile.contains("/c follow {player}"));
        assertTrue(profile.contains("/tp {player}"));
        assertTrue(profile.contains("StringArgumentType.escapeIfRequired"));
        assertTrue(store.contains("DdsTweakConfigPaths.cameraCommandProfiles()"));
        assertTrue(store.contains("global"));
        assertTrue(store.contains("servers"));
        assertTrue(store.contains("serverKey(minecraft)"));
    }

    @Test void playerClickRevalidatesNameAndUuidBeforeCommandDispatch() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(compat.contains("current=info(minecraft,entry.uuid)"));
        assertTrue(compat.contains("entry.name.equals(MinecraftCompat.gameProfileName(current.getProfile()))"));
        assertTrue(screen.contains("if(!CameraSelectorCompat.matchesSnapshot(minecraft,entry))return left||right;"));
    }

    @Test void snapshotKeepsIdentityTriStateAndExcludesSelf() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(compat.contains("PlayerIdentityResolver.resolve(uuid)"));
        assertTrue(compat.contains("uuid.equals(self)"));
        assertTrue(compat.contains("Collections.unmodifiableList(result)"));
        assertTrue(screen.contains("PlayerInventoryTargetType.REAL"));
        assertTrue(screen.contains("PlayerInventoryTargetType.FAKE"));
        assertTrue(screen.contains("unknown.add(entry)"));
        assertTrue(screen.contains("realRow.addAll(unknown)"));
        assertTrue(screen.contains("dds-tweak.camera.identity.unknown"));
    }

    @Test void screenUsesCompactFiveRowLayoutWithPersistentAddSlot() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(screen.contains("Items.GRASS_BLOCK"));
        assertTrue(screen.contains("Items.NETHERRACK"));
        assertTrue(screen.contains("Items.END_PORTAL_FRAME"));
        assertTrue(screen.contains("Items.ENDER_EYE"));
        assertTrue(screen.contains("Items.COMPASS"));
        assertTrue(screen.contains("MAX_VISIBLE=5"));
        assertTrue(screen.contains("CameraBackground.navigationTop(height)"));
        assertTrue(screen.contains("l.destinationY"));
        assertFalse(screen.contains("centered(graphics,\"+\""));
        assertTrue(screen.contains("int plusX=addX+SLOT/2,plusY=l.destinationY+SLOT/2"));
        assertTrue(screen.contains("ScreenRenderCompat.fill(graphics,plusX-4,plusY-1,plusX+4,plusY+1,plusColor)"));
        assertTrue(screen.contains("ScreenRenderCompat.fill(graphics,plusX-1,plusY-4,plusX+1,plusY+4,plusColor)"));
        assertTrue(screen.contains("addHover?0xE8FFFFFF:0x547E858B"));
        assertTrue(screen.contains("destinations.size()+1"));
        assertTrue(screen.contains("Math.max(0,l.capacity-1)"));
        assertTrue(screen.contains("new CameraDestinationEditorScreen(this,null)"));
    }

    @Test void destinationsPersistAsRegistryIdsAndEditorProvidesCurrentValues() throws Exception {
        String destination=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestination.java");
        String store=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationStore.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        assertTrue(destination.contains("DEFAULT_ICON_ID=\"minecraft:compass\""));
        assertTrue(destination.contains("validResourceId"));
        assertTrue(destination.contains("Double.isFinite"));
        assertTrue(store.contains("DdsTweakConfigPaths.cameraDestinations()"));
        assertTrue(store.contains("entry.addProperty(\"icon\",destination.iconId())"));
        assertFalse(store.contains("ItemStack"));
        assertTrue(editor.contains("useCurrentPosition()"));
        assertTrue(editor.contains("CameraSelectorCompat.formatCapturedCoordinate(CameraSelectorCompat.playerX(minecraft))"));
        assertTrue(editor.contains("CameraSelectorCompat.formatCapturedCoordinate(CameraSelectorCompat.playerY(minecraft))"));
        assertTrue(editor.contains("CameraSelectorCompat.formatCapturedCoordinate(CameraSelectorCompat.playerZ(minecraft))"));
        assertTrue(read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java").contains("minecraft.player.x"));
        assertTrue(editor.contains("isVanillaDimension(current)?current:OVERWORLD"));
        assertTrue(editor.contains("CameraSelectorCompat.currentDimension(minecraft)"));
        assertTrue(editor.contains("minecraft:overworld"));
        assertTrue(editor.contains("minecraft:the_nether"));
        assertTrue(editor.contains("minecraft:the_end"));
        assertTrue(editor.contains("CameraDestinationStore.upsert(destination)"));
        assertTrue(editor.contains("CameraDestinationStore.delete(existingId)"));
        assertTrue(editor.contains("parent.reloadDestinations()"));
    }

    @Test void capturedCoordinatesUseBlockFloorAndOnlyAcceptIntegerDestinationInputs() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        assertTrue(compat.contains("static String formatCapturedCoordinate(double value)"));
        assertTrue(compat.contains("Math.floor(value)"));
        assertFalse(compat.contains("setScale(2,RoundingMode.DOWN)"));
        assertTrue(editor.contains("Long.parseLong(draftX.trim())"));
        assertTrue(editor.contains("Long.parseLong(draftY.trim())"));
        assertTrue(editor.contains("Long.parseLong(draftZ.trim())"));
    }

    @Test void destinationEditorUsesSharedPanelAndAdjustedDeleteHint() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        assertTrue(editor.contains("int panelHeight=CameraBackground.NAV_HEIGHT"));
        assertTrue(editor.contains("top=CameraBackground.navigationTop(height)"));
        assertTrue(editor.contains("int nameRowY=top+50"));
        assertTrue(editor.contains("bottomY=top+CameraBackground.FOOTER_ROW_Y"));
        assertTrue(editor.contains("int hintY=deleteY+(DELETE_ICON-font.lineHeight)/2+1"));
        assertTrue(editor.contains("CameraSelectorCompat.formatCapturedCoordinate(destination.x())"));
    }

    @Test void destinationEditorUsesVanillaEditBoxesForImeAndInlineIcon() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        assertTrue(editor.contains("import net.minecraft.client.gui.components.EditBox"));
        assertTrue(editor.contains("new EditBox(font"));
        assertTrue(editor.contains("field.setResponder(responder)"));
        assertTrue(editor.contains("addRenderableWidget(widget)"));
        assertTrue(editor.contains("addButton(widget)"));
        assertFalse(editor.contains("Character.isLetterOrDigit"));
        assertFalse(editor.contains("charTyped(CharacterEvent"));
        assertTrue(editor.contains("nameFieldX=iconX+ICON_SLOT+GAP"));
        assertTrue(editor.contains("selectedDimension=DIMENSIONS[i]"));
        assertTrue(editor.contains("Button iconButton"));
        assertTrue(editor.contains("iconButton=button(l.iconX,l.nameFieldY,ICON_SLOT,\"\",this::openIconPicker)"));
        assertTrue(editor.contains("addWidgetCompat(iconButton)"));
        assertTrue(editor.contains("int iconInsetX=(ICON_SLOT-16)/2"));
        assertTrue(editor.contains("int iconInsetY=(BUTTON_H-16)/2"));
        assertFalse(editor.contains("boolean iconPressed"));
        assertFalse(editor.contains("releaseIcon("));
    }

    @Test void destinationEditorUsesVanillaButtonsAndIconOnlyDimensionChoices() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        assertTrue(editor.contains("import net.minecraft.client.gui.components.Button"));
        assertTrue(editor.contains("Button.builder"));
        assertTrue(editor.contains("new Button(x,y,w,BUTTON_H"));
        assertTrue(editor.contains("DIMENSION_ICONS"));
        assertTrue(editor.contains("new ItemStack(Items.GRASS_BLOCK)"));
        assertTrue(editor.contains("new ItemStack(Items.NETHERRACK)"));
        assertTrue(editor.contains("new ItemStack(Items.END_PORTAL_FRAME)"));
        assertTrue(editor.contains("CameraSlotRender.outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT,0xFFFFFFFF)"));
        assertTrue(editor.contains("CameraSlotRender.outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT,0xFFAAAAAA)"));
        assertTrue(editor.contains("0x50505050"));
        assertFalse(editor.contains("drawChoiceButton"));
        assertTrue(editor.contains("ICON_SLOT=24,DIM_SLOT=24"));
        assertTrue(editor.contains("iconButton=button(l.iconX,l.nameFieldY,ICON_SLOT,\"\",this::openIconPicker)"));
        assertTrue(editor.contains("int iconInsetX=(ICON_SLOT-16)/2"));
        assertTrue(editor.contains("int iconInsetY=(BUTTON_H-16)/2"));
        assertFalse(editor.contains("outline(graphics,l.iconX,l.nameRowY,ICON_SLOT,ICON_SLOT"));
        assertTrue(editor.contains("int saveX=l.innerLeft+l.innerWidth-FOOTER_WIDTH"));
        assertTrue(editor.contains("int cancelX=saveX-footerGap-FOOTER_WIDTH"));
        assertFalse(editor.contains("button(l.innerLeft,l.bottomY"));
        assertTrue(editor.contains("ScreenRenderCompat.crossButton(graphics,deleteX,deleteY,shift&&hover,shift)"));
        assertTrue(editor.contains("CameraSelectorCompat.shiftDown(minecraft)"));
        assertTrue(editor.contains("if(CameraSelectorCompat.shiftDown(minecraft))deleteDestination()"));
        assertTrue(editor.contains("deleteX(Layout l)"));
        assertTrue(editor.contains("deleteY(Layout l)"));
        assertTrue(editor.contains("private int deleteX(Layout l){return l.innerLeft;}"));
        assertTrue(editor.contains("text(graphics,hint,deleteX+DELETE_ICON+6,hintY"));
    }

    @Test void selectableIconsUseSharedPixelRoundedFramesWithoutChangingBackground() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String rounded=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSlotRender.java");
        String backdrop=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraBackground.java");
        assertTrue(rounded.contains("static void fill("));
        assertTrue(rounded.contains("static void outline("));
        assertTrue(rounded.contains("SdfRoundedRectRenderer.fill("));
        assertTrue(rounded.contains("MAX_RADIUS = 7"));
        assertTrue(rounded.contains("SdfRoundedRectRenderer.outline("));
        assertTrue(screen.contains("CameraSlotRender.fill(graphics,x,y,SLOT,SLOT"));
        assertTrue(screen.contains("CameraSlotRender.outline(graphics,hoveredX,y,SLOT,SLOT"));
        assertTrue(screen.contains("CameraSlotRender.outline(graphics,addX,l.destinationY,SLOT,SLOT"));
        assertTrue(screen.contains("new ItemStack[]{CAMERA,BACK,SETTINGS}"));
        assertTrue(screen.contains("new ItemStack(Items.WRITABLE_BOOK)"));
        assertTrue(editor.contains("CameraSlotRender.fill(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT"));
        assertTrue(editor.contains("CameraSlotRender.outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT"));
        String shared=read("src/main/java/ddstweak/gui/SharedScreenBackground.java");
        assertTrue(backdrop.contains("SharedScreenBackground.draw("));
        assertTrue(shared.contains("ScreenRenderCompat.fill(graphics,0,0,width,height"));
        assertFalse(rounded.contains("blurBeforeThisStratum"));
    }

    @Test void roundedSlotsKeepCachedCpuBordersBefore26AndGpuSdfOn26() throws Exception {
        String slots=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSlotRender.java");
        String renderer=read("src/main/java/ddstweak/gui/SdfRoundedRectRenderer.java");
        assertTrue(slots.contains("SdfRoundedRectRenderer.fill("));
        assertTrue(slots.contains("SdfRoundedRectRenderer.outline("));
        assertTrue(renderer.contains("LegacyGpuSdfRenderer.draw("));
        assertTrue(slots.contains("draw(graphics,x,y,w,h,color,true)"));
        assertTrue(slots.contains("COVERAGE[border?1:0][radius]"));
        assertTrue(slots.contains("private static double smooth(double distance)"));
        assertTrue(slots.contains("//#if MC < 260100"));
        assertFalse(Files.exists(Path.of("src/main/java/ddstweak/gui/SdfCornerCoverage.java")));
    }

    @Test void cameraRetainsSlotStateAndHitGeometry() throws Exception {
        String slots=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSlotRender.java");
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(slots.contains("Math.min(MAX_RADIUS, Math.min(w, h) / 2)"));
        assertTrue(slots.contains("SdfRoundedRectRenderer.outline("));
        assertTrue(slots.contains("draw(graphics,x,y,w,h,color,true)"));
        assertTrue(screen.contains("hover?0x58000000:0x22000000"));
        assertTrue(screen.contains("addHover?0x65000000:0x22000000"));
        assertTrue(screen.contains("if(hover)CameraSlotRender.fill(graphics,x,y,SLOT,SLOT"));
    }

    @Test void reference4KSpacingUsesGuiCoordinatesAndSharedHeader() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        assertTrue(screen.contains("CameraBackground.drawNavigationRules(graphics,width,l.top)"));
        assertTrue(config.contains("CameraBackground.drawNavigationRules(graphics,width,y)"));
        assertTrue(screen.contains("centered(graphics,tr(\"dds-tweak.camera.title\"),width/2,l.top,0xFFFFFFFF)"));
        assertTrue(config.contains("centered(graphics,tr(\"dds-tweak.camera.command.title\"),width/2,y,0xFFFFFFFF)"));
        assertTrue(editor.contains("CameraBackground.navigationTop(height),0xFFFFFFFF"));
        assertTrue(screen.contains("boolean both=!fake.isEmpty()&&!realRow.isEmpty()"));
        assertTrue(screen.contains("both?FAKE_Y:SOLO_PLAYER_Y"));
        assertTrue(screen.contains("both?REAL_Y:SOLO_PLAYER_Y"));
        assertTrue(screen.contains("CameraBackground.drawNavigationRules(graphics,width,l.top)"));
        assertFalse(screen.contains("int panelWidth=Math.min(320,width-16)"));
    }

    @Test void commandEditorsShareMainHeaderAndFooterRules() throws Exception {
        String backdrop=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraBackground.java");
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        String quick=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandEditorScreen.java");
        String shared=read("src/main/java/ddstweak/gui/SharedScreenBackground.java");
        assertTrue(backdrop.contains("HEADER_RULE_Y=SharedScreenBackground.HEADER_RULE_Y"));
        assertTrue(shared.contains("HEADER_RULE_Y=16,FOOTER_RULE_Y=192,FOOTER_ROW_Y=200"));
        assertTrue(shared.contains("int ruleWidth=Math.min(360,width-16)"));
        assertTrue(screen.contains("CameraBackground.drawNavigationRules(graphics,width,l.top)"));
        assertTrue(config.contains("CameraBackground.drawNavigationRules(graphics,width,y)"));
        assertTrue(quick.contains("CameraBackground.drawNavigationRules(graphics,width,y)"));
        assertTrue(config.contains("button(x,y+169,third"));
        assertTrue(config.contains("button(x,y+CameraBackground.FOOTER_ROW_Y,half"));
        assertTrue(quick.contains("bottomY(){return top()+CameraBackground.FOOTER_ROW_Y;}"));
        assertFalse(config.contains("button(x,y+197,half"));
        assertFalse(quick.contains("bottomY(){return top()+187;}"));
    }

    @Test void destinationIconPickerAccepts26_3MouseAndRestoresStoredIcon() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        String store=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationStore.java");
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(picker.contains("MC >= 260300"));
        assertTrue(picker.contains("InputConstants.MOUSE_BUTTON_LEFT"));
        assertTrue(picker.contains("if(button!=0)return false"));
        assertTrue(picker.contains("singleSelection.accept(singleSelectedId)"));
        assertTrue(editor.contains("this,iconId,CameraDestination.DEFAULT_ICON_ID,this::setIcon"));
        assertTrue(editor.contains("setIcon(destination==null?CameraDestination.DEFAULT_ICON_ID:destination.iconId())"));
        assertTrue(editor.contains("CameraDestination(existingId,name,dimension,x,y,z,iconId)"));
        assertTrue(store.contains("entry.addProperty(\"icon\",destination.iconId())"));
        assertTrue(screen.contains("destinationIcons.put(destination.id()"));
        assertTrue(screen.contains("parent.reloadDestinations()")==false);
        assertTrue(editor.contains("parent.reloadDestinations()"));
    }

    @Test void destinationEditorKeepsItsExistingIconSelectionsWhileSettingsUseVanillaButtons() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        assertTrue(editor.contains("CameraBackground.navigationTop(height)-8"));
        assertTrue(editor.contains("CameraBackground.navigationTop(height)+CameraBackground.NAV_HEIGHT+8"));
        assertTrue(editor.contains("centered(graphics,title.getString(),l.left+l.width/2,CameraBackground.navigationTop(height),0xFFFFFFFF)"));
        assertTrue(editor.contains("CameraSlotRender.outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT,0xFFFFFFFF)"));
        assertTrue(editor.contains("CameraSlotRender.outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT,0xFFAAAAAA)"));
        assertTrue(config.contains("scopeButtons[0]=button(x,y+24,half"));
        assertTrue(config.contains("modeButtons[0]=button(x,y+68,half"));
        assertFalse(config.contains("private void drawOption("));
    }

    @Test void quickScopeAndCameraTabsReuseNativeButtonsAndDisabledStateAppearance() throws Exception {
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        String quick=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandEditorScreen.java");
        String style=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraOptionStyle.java");
        for(String screen:new String[]{config,quick}){
            assertTrue(screen.contains("CameraOptionStyle.update("));
            assertTrue(screen.contains("super.render(graphics,sync")||screen.contains("super.extractRenderState(graphics,sync"));
            assertTrue(screen.contains("addWidgetCompat(option)"));
        }
        assertTrue(style.contains("button.active=available&&(selected||hovered)"));
        assertTrue(style.contains("if(selected)button.setFocused(false)"));
        assertTrue(config.contains("categoryButtons[i]=button("));
        assertTrue(config.contains("modeButtons[1]=button("));
        assertTrue(quick.contains("scopeButtons[1]=button("));
        assertTrue(config.contains("return handleOptionClick("));
        assertTrue(quick.contains("return handleEditorClick("));
    }

    @Test void selectedTabsReturnToVanillaNormalStateWithoutMouseMovement() throws Exception {
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        String quick=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandEditorScreen.java");
        String style=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraOptionStyle.java");
        assertTrue(config.contains("overSelected|=tab.ordinal()==i&&hover"));
        assertTrue(config.contains("return CameraOptionStyle.renderMouseX(mouseX,overSelected)"));
        assertTrue(quick.contains("return CameraOptionStyle.renderMouseX(mouseX,"));
        assertTrue(config.contains("super.render(graphics,syncOptionHover(mouseX,mouseY),mouseY"));
        assertTrue(quick.contains("super.render(graphics,syncScopeHover(mouseX,mouseY),mouseY"));
        assertTrue(config.contains("super.extractRenderState(graphics,syncOptionHover(mouseX,mouseY),mouseY"));
        assertTrue(quick.contains("super.extractRenderState(graphics,syncScopeHover(mouseX,mouseY),mouseY"));
        assertTrue(style.contains("return selectedHovered?-1:mouseX"));
        assertTrue(style.contains("if(selected)button.setFocused(false)"));
        assertFalse(config.contains("CameraOptionStyle.drawHover("));
        assertFalse(quick.contains("CameraOptionStyle.drawHover("));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/ddstweak/feature/cameraplayerselector/CameraVanillaSlot.java")));
    }

    @Test void destinationIconPickerResetsToDefaultInsteadOfClearing() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String destination=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestination.java");
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(destination.contains("DEFAULT_ICON_ID=\"minecraft:compass\""));
        assertTrue(editor.contains("this,iconId,CameraDestination.DEFAULT_ICON_ID,this::setIcon"));
        assertTrue(picker.contains("private final String singleDefaultId"));
        assertTrue(picker.contains("Single-selection default ID must not be empty"));
        assertTrue(picker.contains("selectedId==null||selectedId.trim().isEmpty()?singleDefaultId:selectedId.trim()"));
        assertTrue(picker.contains("singleSelectedId=singleDefaultId;"));
        assertTrue(picker.contains("singleSelection.accept(singleSelectedId)"));
        assertTrue(picker.contains("singleSelection==null?\"dds-tweak.list_picker.clear\":\"dds-tweak.list_picker.reset\""));
        assertTrue(picker.contains("setMultiValues(Collections.emptyList())"));
        assertTrue(picker.contains("\"dds-tweak.list_picker.current\":\"dds-tweak.list_picker.selected\""));
        for(String lang:new String[]{"zh_cn","zh_tw","en_us"}){
            String strings=read("src/main/resources/assets/dds-tweak/lang/"+lang+".json");
            assertTrue(strings.contains("\"dds-tweak.list_picker.reset\""),lang);
            assertTrue(strings.contains("\"dds-tweak.list_picker.current\""),lang);
        }
    }

    @Test void iconPickerStaysOpenUntilDoneAndCanCancel() throws Exception {
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(picker.contains("singleSelectedId=id;"));
        assertTrue(picker.contains("private void confirmSelection()"));
        assertTrue(picker.contains("singleSelection.accept(singleSelectedId)"));
        assertTrue(picker.contains("this::confirmSelection"));
        assertFalse(picker.contains("singleSelection.accept(id)"));
        assertTrue(picker.contains("public void onClose()"));
    }

    @Test void navigationIconsHaveFaintPersistentFramesAndVanillaTooltips() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(screen.contains("hover?0x58000000:0x22000000"));
        assertTrue(screen.contains("0x547E858B"));
        assertTrue(screen.contains("if(hover)CameraSlotRender.fill(graphics,x,y,SLOT,SLOT")); // avatars retain hover-only frames
        assertTrue(screen.contains("private List<Component> tooltip("));
        assertTrue(screen.contains("MinecraftCompat.grayLiteral"));
        assertTrue(screen.contains("setTooltipForNextFrame(font,lines,Optional.empty()"));
        assertTrue(screen.contains("renderComponentTooltip(graphics,lines,mouseX,mouseY)"));
        assertTrue(screen.contains("groupLabel(graphics,\"dds-tweak.camera.group.fake\""));
        assertTrue(screen.contains("groupLabel(graphics,\"dds-tweak.camera.group.real\""));
        assertTrue(screen.contains("CameraBackground.drawNavigationRules(graphics,width,l.top)"));
        for(String lang:new String[]{"zh_cn","en_us","zh_tw"}){
            String content=read("src/main/resources/assets/dds-tweak/lang/"+lang+".json");
            assertTrue(content.contains("dds-tweak.camera.tooltip.gesture"));
            assertTrue(content.contains("dds-tweak.camera.group.dimension"));
        }
    }

    @Test void playerTooltipAlwaysShowsConfiguredActionsAndServerOwnsPermissions() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(screen.contains("if(command==null)return;"));
        assertFalse(screen.contains("if(command==null||!can(action))return;"));
        assertFalse(screen.contains("dds-tweak.camera.playerTp.unavailable"));
        assertFalse(screen.contains("dds-tweak.camera.offline"));
        assertTrue(screen.contains("CameraCommandProfile.Action.PLAYER_DIRECT_TP"));
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        assertFalse(compat.contains("connection.getCommands().getRoot()"));
        assertFalse(compat.contains("root.getChild(rootName)"));
        assertTrue(compat.contains("static boolean configured(CameraCommandProfile profile,CameraCommandProfile.Action action)"));
        assertTrue(compat.contains("CameraCommandProfile.validate(action,profile.get(action))==null"));
        assertTrue(compat.contains("if(!configured(profile,action))return false;"));
        assertFalse(screen.contains("CameraSelectorCompat.available(minecraft,profile,action)"));
        assertTrue(screen.contains("lines.add(MinecraftCompat.grayLiteral(CameraCommandSyntax.display(command)))"));
        assertFalse(screen.contains("MinecraftCompat.grayTranslatable(\"dds-tweak.camera.unsupported\")"));
        assertTrue(screen.contains("CameraCommandProfile.Action.PLAYER_DIRECT_TP"));
        assertTrue(screen.contains("CameraCommandProfile.Action.PLAYER_FOLLOW"));
        assertTrue(screen.contains("CameraCommandProfile.Action.PLAYER_TP"));
    }

    @Test void destinationDeleteUsesVanillaCrossAndShiftGate() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        String render=read("src/main/java/ddstweak/platform/minecraft/ScreenRenderCompat.java");
        assertTrue(render.contains("textures/gui/sprites/widget/cross_button"));
        assertTrue(render.contains("_highlighted"));
        assertTrue(render.contains("MC >= 12002"));
        assertFalse(render.contains("if(!enabled)fill(graphics,x,y,x+14,y+14,0x70000000)"));
        assertTrue(editor.contains("DELETE_ICON=14"));
        assertTrue(editor.contains("shift&&hover"));
        assertTrue(editor.contains("delete_shift"));
        assertTrue(editor.contains("delete_ready"));
        // Camera and Carpet deliberately delegate to one version-aware helper.
        String shared=read("src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java");
        String preset=read("src/main/java/ddstweak/feature/carpetrules/CarpetRulePresetScreen.java");
        assertTrue(compat.contains("return MinecraftCompat.shiftDown(minecraft);"));
        assertTrue(preset.contains("return MinecraftCompat.shiftDown(minecraft);"));
        assertTrue(preset.contains("if(!shiftHeld())"));
        assertTrue(shared.contains("public static boolean shiftDown(Minecraft minecraft)"));
        assertTrue(shared.contains("if(minecraft==null)return false;"));
        assertTrue(shared.contains("MC >= 12109"));
        assertTrue(shared.contains("return minecraft.hasShiftDown();"));
        assertTrue(shared.contains("//$$ return Screen.hasShiftDown();"));
        assertFalse(compat.contains("Screen.hasShiftDown()"));
        assertFalse(preset.contains("Screen.hasShiftDown()"));
    }

    @Test void cameraScreensUseSharedBackdropAndVersionAwareBlur() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        String backdrop=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraBackground.java");
        for(String source:new String[]{screen,editor,config}){
            assertTrue(source.contains("CameraBackground.draw"));
            assertTrue(source.contains("MC >= 12005"));
            assertTrue(source.contains("super.renderBackground"));
            assertTrue(source.contains("super.extractBackground"));
            assertTrue(source.contains("renderTransparentBackground"));
        }
        String shared=read("src/main/java/ddstweak/gui/SharedScreenBackground.java");
        assertTrue(backdrop.contains("SharedScreenBackground.draw("));
        assertTrue(shared.contains("ScreenRenderCompat.fill(graphics,0,0,width,height"));
        assertTrue(shared.contains("ScreenRenderCompat.fill(graphics,0,top,width,bottom"));
        assertTrue(shared.contains("0x66D8DEE3"));
        assertFalse(editor.contains("0xEE101010"));
    }

    @Test void profileOverrideIsSparseAndWorldIdentityIsFolderBased() throws Exception {
        String store=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandProfileStore.java");
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        assertTrue(store.contains("world-folder:"));
        assertTrue(store.contains("LevelResource.ROOT"));
        assertTrue(store.contains("profile.clearAllOverrides()"));
        assertTrue(store.contains("serializeOverrides(profile)"));
        assertTrue(store.contains("root.addProperty(\"schema\",2)"));
        assertTrue(config.contains("serverDraft.inheritFrom(globalDraft)"));
        assertTrue(config.contains("restoreInherited"));
        assertTrue(config.contains("applyPreset(vanilla)"));
        assertFalse(config.contains("handleShortcut("));
        assertFalse(config.contains("dds-tweak.camera.command.shortcuts"));
        assertFalse(config.contains("boolean keyPressed("));
        assertFalse(read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java").contains("altDown("));
    }

    @Test void commandEditorOpensOnGlobalDefaultsAndKeepsServerOverrideAvailable() throws Exception {
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        assertTrue(config.contains("globalDraft=CameraCommandProfileStore.loadGlobal()"));
        assertTrue(config.contains("serverDraft=CameraCommandProfileStore.load(minecraft)"));
        assertTrue(config.contains("global=true; // Open on global defaults"));
        assertTrue(config.contains("if(!globalScope&&!hasServer())return"));
        assertTrue(config.contains("CameraCommandProfileStore.saveBoth(minecraft,globalDraft,dirtyGlobal,serverDraft,"));
        assertTrue(config.contains("CameraCommandProfileStore.requireServerIdentity(openedServerKey,"));
        assertTrue(config.contains("dirtyGlobal,serverDraft,dirtyServer)"));
    }

    @Test void navigationAndEditorShareHeightAndLocalizedCategorizedInputs() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        String backdrop=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraBackground.java");
        String shared=read("src/main/java/ddstweak/gui/SharedScreenBackground.java");
        assertTrue(backdrop.contains("NAV_HEIGHT=SharedScreenBackground.NAV_HEIGHT"));
        assertTrue(shared.contains("NAV_HEIGHT=222"));
        assertTrue(screen.contains("CameraBackground.navigationTop(height)"));
        assertTrue(config.contains("CameraBackground.navigationTop(height)"));
        assertTrue(screen.contains("top+CameraBackground.NAV_HEIGHT+8"));
        assertTrue(config.contains("top+CameraBackground.NAV_HEIGHT+8"));
        assertTrue(screen.contains("both?FAKE_Y:SOLO_PLAYER_Y"));
        assertTrue(screen.contains("both?REAL_Y:SOLO_PLAYER_Y"));
        assertTrue(screen.contains("DIMENSION_Y=27,FAKE_Y=70,REAL_Y=116,SOLO_PLAYER_Y=93"));
        assertTrue(screen.contains("DESTINATION_Y=163,ACTION_Y=200"));
        assertTrue(screen.contains("top+DIMENSION_Y,fakeY,realY,top+DESTINATION_Y,top+ACTION_Y"));
        assertTrue(screen.contains("boolean both=!fake.isEmpty()&&!realRow.isEmpty()"));
        assertTrue(config.contains("enum Tab { PLAYER, CAMERA, DESTINATION }"));
        assertTrue(config.contains("new EditBox[3]"));
        assertTrue(config.contains("private boolean handleOptionClick(double mouseX,double mouseY,int button)"));
        assertTrue(config.contains("modeButtons[1]=button(x+half+GAP,y+68"));
        assertTrue(config.contains("private boolean handleOptionClick("));
        assertTrue(config.contains("categoryButtons[i]=button(bx,y+46,bw"));
        assertTrue(config.contains("CameraOptionStyle.update(categoryButtons[i],tab.ordinal()==i,true,hover)"));
        assertFalse(config.contains("show(index-1)"));
        assertFalse(config.contains("show(index+1)"));
        assertTrue(config.contains("markDirty()"));
        assertTrue(config.contains("dirtyGlobal"));
        assertTrue(config.contains("dirtyServer"));
        assertTrue(screen.contains("dds-tweak.camera.tooltip.gesture"));
        assertTrue(config.contains("dds-tweak.camera.command.title"));
        for(String lang:new String[]{"zh_cn","en_us","zh_tw"}){
            String content=read("src/main/resources/assets/dds-tweak/lang/"+lang+".json");
            assertTrue(content.contains("dds-tweak.camera.tooltip.mouse.shiftLeft"));
            assertFalse(content.contains("dds-tweak.camera.tooltip.noRightAction"));
            assertTrue(content.contains("dds-tweak.camera.command.tab.player"));
            assertTrue(content.contains("dds-tweak.camera.command.tab.destination"));
        }
    }

    @Test void destinationIconReusesExistingItemPicker() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(editor.contains("ItemPicker.single("));
        assertTrue(editor.contains("this,iconId,CameraDestination.DEFAULT_ICON_ID,this::setIcon"));
        assertFalse(editor.contains("this,iconId,this::setIcon"));
        assertTrue(picker.contains("Consumer<String> singleSelection"));
        assertTrue(picker.contains("singleSelection.accept(singleSelectedId)"));
        assertTrue(picker.contains("MinecraftCompat.setScreen(minecraft,parent)"));
    }

    @Test void playerAndDestinationRowsStaySeparatelyScrollable() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String render=read("src/main/java/ddstweak/platform/minecraft/ScreenRenderCompat.java");
        assertTrue(screen.contains("fakeOffset=step"));
        assertTrue(screen.contains("realOffset=step"));
        assertTrue(screen.contains("destinationOffset=step"));
        assertTrue(screen.contains("renderEdgePeek"));
        assertTrue(screen.contains("beginScissor"));
        assertTrue(screen.contains("endScissor"));
        assertTrue(screen.contains("CameraSelectorCompat.online"));
        assertTrue(screen.contains("ScreenRenderCompat.playerHead"));
        assertTrue(render.contains("drawSkinLayer(graphics,texture,x,y,size,8,8)"));
        assertTrue(render.contains("int outer=size+2"));
        assertTrue(render.contains("drawSkinLayer(graphics,texture,x-1,y-1,outer,40,8)"));
    }

    @Test void gesturesAndCommandsAreIndependent() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String config=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraCommandConfigScreen.java");
        assertTrue(screen.contains("CameraSelectorCompat.shiftDown(minecraft)?CameraCommandProfile.Action.PLAYER_DIRECT_TP"));
        assertTrue(screen.contains("CameraCommandProfile.Action.PLAYER_TP"));
        assertTrue(screen.contains("CameraCommandProfile.Action.PLAYER_FOLLOW"));
        assertTrue(screen.contains("CameraCommandProfile.Mode.SEPARATE"));
        assertTrue(screen.contains("CameraCommandProfile.Action.EXIT"));
        assertTrue(screen.contains("CameraCommandProfile.Action.BACK"));
        assertTrue(screen.contains("new CameraCommandConfigScreen(this)"));
        assertTrue(screen.contains("SETTINGS=new ItemStack(Items.WRITABLE_BOOK)"));
        assertTrue(config.contains("CameraCommandProfileStore.saveBoth(minecraft,globalDraft,dirtyGlobal,serverDraft,"));
        assertTrue(config.contains("CameraCommandProfileStore.requireServerIdentity(openedServerKey,"));
        assertTrue(config.contains("dirtyGlobal,serverDraft,dirtyServer)"));
        assertTrue(config.contains("CameraCommandProfile.validate"));
        assertTrue(config.contains("CameraCommandProfile.Mode.SEPARATE"));
        assertTrue(config.contains("parent.reloadProfile()"));
    }

    @Test void currentDimensionHasExplicitVersionBoundary() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        assertTrue(compat.contains("MC >= 11600"));
        assertTrue(compat.contains("MC <= 12110"));
        assertTrue(compat.contains("dimension().location().toString()"));
        assertTrue(compat.contains("dimension().identifier().toString()"));
        assertTrue(compat.contains("return \"\";"));
    }

    @Test void playerTipsDoNotAdvertiseScrollingAndHotkeyCopyIsPressBased() throws Exception {
        String zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");
        String en=read("src/main/resources/assets/dds-tweak/lang/en_us.json");
        assertFalse(zh.contains("滚轮：滚动"));
        assertFalse(en.contains("wheel: scroll"));
        assertFalse(zh.contains("按住时显示 Camera 玩家选择器"));
        assertFalse(en.contains("Hold to show the Camera player selector"));
        var zhStrings=com.google.gson.JsonParser.parseString(zh).getAsJsonObject();
        var enStrings=com.google.gson.JsonParser.parseString(en).getAsJsonObject();
        for(String key:new String[]{"dds-tweak.config.hotkey.comment.cameraPlayerSelector",
                "dds-tweak.config.tweaks.comment.cameraPlayerSelector"}){
            String zhTip=zhStrings.get(key).getAsString();
            String enTip=enStrings.get(key).getAsString();
            assertTrue(zhTip.contains("快捷键")&&zhTip.contains("打开 Camera 玩家选择器"),key);
            assertTrue(enTip.contains("hotkey")&&enTip.contains("Open the Camera player selector"),key);
        }
    }

    @Test void mouseButtonsUse26_3SdlCompatibility() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        assertTrue(compat.contains("MC >= 260300"));
        assertTrue(compat.contains("InputConstants.MOUSE_BUTTON_LEFT"));
        assertTrue(compat.contains("InputConstants.MOUSE_BUTTON_RIGHT"));
        assertTrue(compat.contains("return button==0"));
        assertTrue(compat.contains("return button==1"));
    }

    @Test void renderAndInputHaveExplicitVersionBoundaries() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String render=read("src/main/java/ddstweak/platform/minecraft/ScreenRenderCompat.java");
        assertTrue(screen.contains("MC >= 260100"));
        assertTrue(screen.contains("MC >= 12109"));
        assertTrue(screen.contains("MC >= 12002"));
        assertTrue(editor.contains("MC >= 260100"));
        assertTrue(editor.contains("MC >= 12109"));
        assertTrue(render.contains("GuiGraphicsExtractor"));
        assertTrue(render.contains("GuiGraphics"));
        assertTrue(render.contains("PoseStack"));
        assertTrue(render.contains("GlStateManager"));
    }

    @Test void featureIsRegisteredWithoutProtocolChanges() throws Exception {
        String entry=read("src/main/java/ddstweak/DDSTweak.java");
        assertTrue(entry.contains("CameraPlayerSelectorFeature.initialize()"));
        assertFalse(read("src/main/java/ddstweak/network/DdsProtocol.java").contains("camera_player_selector"));
        assertFalse(read("src/main/java/ddstweak/network/DdsProtocol.java").contains("camera_destination"));
    }
}
