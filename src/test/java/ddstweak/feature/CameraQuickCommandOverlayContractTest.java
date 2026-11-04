package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class CameraQuickCommandOverlayContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String file) throws Exception {return Files.readString(ROOT.resolve(file));}

    @Test void launcherPreservesFixedCameraRowsAndExistingActions() throws Exception {
        String screen=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraPlayerSelectorScreen.java");
        String overlay=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandOverlay.java");
        assertTrue(screen.contains("new CameraQuickCommandOverlay(this)"));
        assertTrue(screen.contains("quickOverlay.render(graphics,font,width,l.actionY,mouseX,mouseY)"));
        assertTrue(screen.contains("quickOverlay.handleClick(minecraft,width,l.actionY"));
        assertTrue(screen.contains("quickOverlay.scroll(width,l.actionY"));
        assertTrue(screen.contains("quickOverlay.isOpen()"));
        assertTrue(screen.contains("boolean both=!fake.isEmpty()&&!realRow.isEmpty()"));
        assertTrue(screen.contains("top+DIMENSION_Y,fakeY,realY,top+DESTINATION_Y,top+ACTION_Y"));
        assertTrue(screen.contains("DIMENSION_Y=27,FAKE_Y=70,REAL_Y=116,SOLO_PLAYER_Y=93"));
        assertTrue(overlay.contains("static int buttonX(int width)"));
        assertTrue(overlay.contains("private int popupY(int actionY)"));
        assertTrue(overlay.contains("MAX_ROWS=6"));
        assertTrue(overlay.contains("Math.min(360,width-16)"));
        assertTrue(screen.contains("DESTINATION_Y=163,ACTION_Y=200"));
    }

    @Test void mouseActionsAreSingleCommandAndEditorOwnsMutations() throws Exception {
        String overlay=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandOverlay.java");
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandEditorScreen.java");
        String store=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandStore.java");
        assertTrue(overlay.contains("CameraSelectorCompat.leftMouse(button)"));
        assertTrue(overlay.contains("CameraSelectorCompat.rightMouse(button)"));
        assertTrue(overlay.contains("if(right)MinecraftCompat.setScreen"));
        assertTrue(overlay.contains("CameraCommandSyntax.send(minecraft,command)"));
        assertTrue(overlay.contains("return true; // Do not inadvertently activate a player"));
        assertTrue(editor.contains("CameraQuickCommandStore.upsert(minecraft"));
        assertTrue(editor.contains("CameraQuickCommandStore.delete(minecraft"));
        assertTrue(editor.contains("CameraSelectorCompat.shiftDown(minecraft)"));
        assertTrue(editor.contains("ScreenRenderCompat.crossButton(graphics,deleteX,deleteY,shift&&hover,shift)"));
        assertTrue(editor.contains("bottomY(){return top()+CameraBackground.FOOTER_ROW_Y;}"));
        assertTrue(editor.contains("CameraBackground.drawNavigationRules(graphics,width,y)"));
        assertTrue(editor.contains("FOOTER_WIDTH=72,FOOTER_GAP=4"));
        assertTrue(editor.contains("deleteY+(DELETE_ICON-font.lineHeight)/2+1"));
        assertFalse(editor.contains("applyServerPreset("));
        assertTrue(editor.contains("this.originalScope=entry==null?CameraQuickCommandStore.Scope.GLOBAL:entry.scope"));
        assertTrue(editor.contains("CameraQuickCommandStore.Scope.LOCAL"));
        assertTrue(editor.contains("CameraCommandProfileStore.requireServerIdentity(openedServerKey,"));
        assertTrue(store.contains("CameraCommandProfileStore.requireServerIdentity(key,key)"));
        assertTrue(editor.contains("scopeButtons[0]=button(x,y+154,half"));
        assertTrue(editor.contains("syncScopeHover(mouseX,mouseY)"));
        assertTrue(editor.contains("CameraOptionStyle.update(scopeButtons[0]"));
        assertTrue(editor.contains("CameraOptionStyle.update(scopeButtons[1]"));
        assertTrue(editor.contains("return handleEditorClick(event.x(),event.y(),event.button())"));
        assertTrue(editor.contains("new CameraQuickCommandStore.Entry(existingId,draftName,draftCommand,scope),originalScope"));
        assertTrue(store.contains("if(ensurePresets(root))write(root)"));
        assertTrue(store.contains("merged")||store.contains("return Collections.unmodifiableList(merge("));
        assertTrue(store.contains("String[] servers={\"survival\",\"creative\",\"mirror\"}"));
        assertTrue(store.contains("PRESETS_INITIALIZED"));
        assertTrue(store.contains("CameraCommandProfileStore.serverKey(minecraft)"));
        assertTrue(store.contains("DdsTweakConfigPaths.cameraQuickCommands()"));
        assertFalse(store.contains("sendChat"));
        assertFalse(store.contains("sendServerCommandSilently"));
    }

    @Test void threeLanguagesPresentAndBackdropIsShared() throws Exception {
        String editor=read("src/main/java/ddstweak/feature/cameraplayerselector/CameraQuickCommandEditorScreen.java");
        assertTrue(editor.contains("CameraBackground.NAV_HEIGHT+8"));
        assertTrue(editor.contains("drawOverlay(graphics,mouseX,mouseY)"));
        assertTrue(editor.contains("drawOverlay(null,mouseX,mouseY)"));
        assertFalse(editor.contains("drawOverlay(graphics);"));
        for(String lang:new String[]{"zh_cn","zh_tw","en_us"}){
            String source=read("src/main/resources/assets/dds-tweak/lang/"+lang+".json");
            assertTrue(source.contains("dds-tweak.camera.quick.title"));
            assertTrue(source.contains("dds-tweak.camera.quick.editHint"));
            assertTrue(source.contains("dds-tweak.camera.quick.preset.survival"));
            assertTrue(source.contains("dds-tweak.camera.quick.preset.creative"));
            assertTrue(source.contains("dds-tweak.camera.quick.preset.mirror"));
        }
    }
}
