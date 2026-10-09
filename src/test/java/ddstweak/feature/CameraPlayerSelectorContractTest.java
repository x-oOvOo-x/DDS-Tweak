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

    @Test void serverCommandTreeOwnsCapabilitiesAndDestinationTeleport() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        String feature=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorFeature.java");
        assertTrue(compat.contains("getCommands().getRoot().getChild(\"c\")"));
        assertTrue(compat.contains("node=node.getChild(child)"));
        assertTrue(compat.contains("supports(minecraft,\"tp\",\"dimension\",\"dimension\",\"pos\")"));
        assertTrue(compat.contains("\"c tp dimension \"+destination.dimension()"));
        assertTrue(compat.contains("c follow "));
        assertTrue(compat.contains("send(minecraft,\"c\")"));
        assertTrue(compat.contains("send(minecraft,\"c back\")"));
        assertTrue(feature.contains("supportsDestinationTeleport(minecraft)"));
        assertTrue(feature.contains("supports(minecraft,\"tp\",\"player\")"));
        assertTrue(feature.contains("supports(minecraft,\"tp\",\"dimension\")"));
        assertTrue(feature.contains("supports(minecraft,\"follow\")"));
        assertTrue(compat.contains("StringArgumentType.escapeIfRequired"));
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
        assertTrue(screen.contains("ROW_H*5"));
        assertTrue(screen.contains("l.destinationY"));
        assertFalse(screen.contains("centered(graphics,\"+\""));
        assertTrue(screen.contains("int plusX=addX+SLOT/2,plusY=l.destinationY+SLOT/2"));
        assertTrue(screen.contains("ScreenRenderCompat.fill(graphics,plusX-4,plusY-1,plusX+4,plusY+1,plusColor)"));
        assertTrue(screen.contains("ScreenRenderCompat.fill(graphics,plusX-1,plusY-4,plusX+1,plusY+4,plusColor)"));
        assertTrue(screen.contains("addHover?0xFFFFFFFF:0xFFC8C8C8"));
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
        assertTrue(store.contains("dds-tweak-camera-destinations.json"));
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

    @Test void capturedCoordinatesUseTwoDecimalTruncation() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        assertTrue(compat.contains("import java.math.RoundingMode"));
        assertTrue(compat.contains("setScale(2,RoundingMode.DOWN).toPlainString()"));
        assertTrue(compat.contains("static String formatCapturedCoordinate(double value)"));
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
        assertTrue(editor.contains("outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT,0xFFFFFFFF)"));
        assertTrue(editor.contains("outline(graphics,x,l.dimensionY,DIM_SLOT,DIM_SLOT,0xFFAAAAAA)"));
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
        assertTrue(compat.contains("minecraft.hasShiftDown()"));
        assertTrue(compat.contains("MC >= 12109"));
        assertTrue(compat.contains("Screen.hasShiftDown()"));
    }

    @Test void cameraScreensReuseContainerBackdrop() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        assertTrue(screen.contains("extractTransparentBackground(graphics)"));
        assertTrue(screen.contains("renderTransparentBackground(graphics)"));
        assertTrue(screen.contains("renderBackground(graphics);super.render"));
        assertTrue(editor.contains("extractTransparentBackground(graphics)"));
        assertTrue(editor.contains("renderTransparentBackground(graphics)"));
        assertFalse(editor.contains("fill(graphics,0,0,width,height,0x90000000)"));
    }

    @Test void destinationIconReusesExistingItemPicker() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraDestinationEditorScreen.java");
        String picker=read("src/main/java/ddstweak/feature/dropconfirmation/DropConfirmationItemScreen.java");
        assertTrue(editor.contains("new DropConfirmationItemScreen("));
        assertTrue(editor.contains("this,iconId,this::setIcon"));
        assertTrue(picker.contains("Consumer<String> singleSelection"));
        assertTrue(picker.contains("singleSelection.accept(id)"));
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

    @Test void tpAndFollowTooltipsShowExactCommands() throws Exception {
        String compat=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraSelectorCompat.java");
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        assertTrue(compat.contains("static String tpCommand"));
        assertTrue(compat.contains("tpPlayer(Minecraft minecraft,String name){return send(minecraft,tpCommand(name));}"));
        assertTrue(compat.contains("static String followCommand"));
        assertTrue(compat.contains("followPlayer(Minecraft minecraft,String name){return send(minecraft,followCommand(name));}"));
        assertTrue(screen.contains("CameraSelectorCompat.tpCommand(entry.name)"));
        assertTrue(screen.contains("CameraSelectorCompat.followCommand(entry.name)"));
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
        assertTrue(zh.contains("按下一次打开 Camera 玩家选择器"));
        assertTrue(en.contains("Press once to open the Camera player selector"));
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
