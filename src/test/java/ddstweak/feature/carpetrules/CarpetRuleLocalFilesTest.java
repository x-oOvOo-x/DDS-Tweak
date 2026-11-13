package ddstweak.feature.carpetrules;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import org.junit.jupiter.api.io.TempDir;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleLocalFilesTest {
    @TempDir Path folder;

    @Test void inGameBrowserShowsDirectoriesAndJsonFilesWithoutDesktop() throws Exception {
        Path child=Files.createDirectory(folder.resolve("subfolder"));
        Path json=Files.createFile(folder.resolve("preset.JSON"));
        Files.createFile(folder.resolve("notes.txt"));
        assertEquals(Arrays.asList(child,json),CarpetRuleLocalFiles.jsonEntries(folder));
        assertEquals(folder.toAbsolutePath().normalize(),
                CarpetRuleLocalFiles.browseDirectory(json,folder));
        assertEquals(child.toAbsolutePath().normalize(),
                CarpetRuleLocalFiles.browseDirectory(child,folder));
        assertEquals(folder.toAbsolutePath().normalize(),
                CarpetRuleLocalFiles.browseDirectory(folder.resolve("missing.json"),folder));
        assertTrue(CarpetRuleLocalFiles.jsonEntries(folder.resolve("missing")).isEmpty());
    }


    @Test void nativeDialogIsPrimaryWithVersionSpecificApisAndNoExternalProcess() throws Exception {
        String screen=Files.readString(Paths.get(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRulePresetScreen.java"))
                .replace("\r\n","\n");
        String helper=Files.readString(Paths.get(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleLocalFiles.java"));
        assertTrue(screen.contains("CarpetRuleLocalFiles.chooseNativeJson(directory,"));
        assertTrue(screen.contains("if(unavailable){\n            browseImportInGame();"));
        assertTrue(screen.contains("else if(chosen!=null)"));
        assertTrue(screen.contains("if(picker!=null)picker.cancel();"));
        assertTrue(screen.contains("return CarpetRulePresetTransfer.exportDirectory();"));
        assertFalse(screen.contains("importFilePath=CarpetRulePresetTransfer.exchangeFile()"));
        assertTrue(helper.contains("TinyFileDialogs.tinyfd_openFileDialog("));
        assertTrue(helper.contains("SDLDialog.SDL_ShowOpenFileDialog("));
        assertTrue(helper.contains("completed.thenAcceptAsync(choice->"));
        assertTrue(helper.contains("net.minecraft.client.Minecraft.getInstance().execute(()->{"),
                "SDL callback cleanup must be handed back to the game thread");
        assertTrue(helper.contains("//#elseif MC >= 11500"));
        assertTrue(helper.contains("WINDOWS_DIALOG"));
        assertFalse(helper.contains("Add-Type -TypeDefinition"));
    }

    @Test void cancelledNativePickerCannotApplySelectionOrFallback() {
        java.util.concurrent.atomic.AtomicInteger chosen=new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger failures=new java.util.concurrent.atomic.AtomicInteger();
        CarpetRuleLocalFiles.NativePicker picker=new CarpetRuleLocalFiles.NativePicker(
                path->chosen.incrementAndGet(),failures::incrementAndGet);
        picker.cancel();
        picker.select(folder.resolve("stale.json"));
        picker.fail();
        assertTrue(picker.isCancelled());
        assertEquals(0,chosen.get());
        assertEquals(0,failures.get());
    }

    @Test void osCancelIsNotAPickerFailure() {
        java.util.concurrent.atomic.AtomicReference<Path> chosen=new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
        CarpetRuleLocalFiles.NativePicker picker=new CarpetRuleLocalFiles.NativePicker(
                path->{chosen.set(path);calls.incrementAndGet();},
                ()->fail("System Cancel must not trigger the in-game fallback"));
        picker.select(null);
        assertEquals(1,calls.get());
        assertNull(chosen.get());
    }

    @Test void inGameBrowserShowsDtExportsAndNavigatesBackWithoutLock() throws Exception {
        Path dt=Files.createDirectories(folder.resolve("dds-tweak"));
        Path exports=Files.createDirectories(dt.resolve("carpet-exports"));
        Path plan=Files.createFile(exports.resolve("plan.json"));
        Files.createFile(exports.resolve("ignore.txt"));
        assertEquals(Collections.singletonList(plan),CarpetRuleLocalFiles.jsonEntries(exports));
        assertEquals(exports.toAbsolutePath().normalize(),
                CarpetRuleLocalFiles.browseDirectory(exports,dt));
        assertEquals(dt.toAbsolutePath().normalize(),
                CarpetRuleLocalFiles.browseDirectory(dt.resolve("missing.json"),dt));
    }

    @Test void windowsFolderFallbackKeepsDirectoryAsOneArgument(){
        Path folder=Paths.get("config","exports with spaces; demo");
        assertEquals(Arrays.asList("explorer.exe",folder.toAbsolutePath().toString()),
                CarpetRuleLocalFiles.fallbackDirectoryCommand(folder,"Windows 11"));
        assertEquals(Collections.emptyList(),
                CarpetRuleLocalFiles.fallbackDirectoryCommand(folder,"Linux"));
        assertEquals(Collections.emptyList(),
                CarpetRuleLocalFiles.fallbackDirectoryCommand(null,"Windows 11"));
    }


    @Test void windowsFallbackOpensTheExactFilePathWithoutShellParsing(){
        Path file=Paths.get("config","rule plan; test.json");
        assertEquals(Arrays.asList("notepad.exe",file.toAbsolutePath().toString()),
                CarpetRuleLocalFiles.fallbackEditorCommand(file,"Windows 11"));
        assertEquals(Collections.emptyList(),
                CarpetRuleLocalFiles.fallbackEditorCommand(file,"Linux"));
        assertEquals(Collections.emptyList(),
                CarpetRuleLocalFiles.fallbackEditorCommand(null,"Windows 11"));
    }

    @Test void openingMissingFileReportsFailureWithoutBlockingCaller() throws Exception {
        CountDownLatch finished=new CountDownLatch(1);
        AtomicBoolean success=new AtomicBoolean(true);
        CarpetRuleLocalFiles.open(null,opened->{
            success.set(opened);
            finished.countDown();
        });
        assertTrue(finished.await(5,TimeUnit.SECONDS));
        assertFalse(success.get());
    }

    @Test void cancelAndReopenNeverKeepTheBrowseButtonLocked() throws Exception {
        String screen=Files.readString(Paths.get(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRulePresetScreen.java"))
                .replace("\r\n","\n");
        assertTrue(screen.contains("private void cancelImport(){\n        cancelNativePicker();"));
        assertTrue(screen.contains("public void onClose(){\n        cancelNativePicker();"));
        assertTrue(screen.contains("nativePickerPending=false;"));
        assertTrue(screen.contains("if(request!=nativePickerGeneration)return;"));
        assertTrue(screen.contains("browseImportButton.active=!nativePickerPending;"));
        assertTrue(screen.contains("MinecraftCompat.screen(minecraft)!=this"));
        assertTrue(screen.contains("minecraft.execute(()->finishNativePicker("));
    }

}
