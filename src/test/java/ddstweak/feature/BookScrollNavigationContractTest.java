package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class BookScrollNavigationContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {
        Path f=ROOT.resolve(p);
        return Files.exists(f)?Files.readString(f):"";
    }

    @Test void featureIsOptInAndKeepsInteractionSettingsTogether() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(configs.contains("SCROLL_BOOK_PAGES=MalilibCompat.bool(\"scrollBookPages\",false,TWEAKS_KEY)"));
        assertTrue(configs.contains("SCROLL_BOOK_CTRL_MULTIPLIER=MalilibCompat.integer(\"scrollBookCtrlMultiplier\",5,1,10,true,TWEAKS_KEY)"));
        assertTrue(configs.contains("SCROLL_BOOK_PAGES,SCROLL_BOOK_CTRL_MULTIPLIER"));
    }

    @Test void scrollInputSupportsHighResolutionAndCtrlAcceleration() throws Exception {
        String state=read("src/main/java/ddstweak/feature/bookscroll/BookScrollState.java");
        String handler=read("src/main/java/ddstweak/mixin/feature/bookscroll/BookScrollContainerEventHandlerMixin.java");

        assertTrue(state.contains("WeakHashMap<Object,Double>"));
        assertTrue(state.contains("progress+=amount*multiplier"));
        assertTrue(state.contains("SCROLL_BOOK_CTRL_MULTIPLIER.getIntegerValue()"));
        assertTrue(state.contains("//#if MC >= 12109"));
        assertTrue(state.contains("Minecraft.getInstance().hasControlDown()"));
        assertTrue(state.contains("//$$ return Screen.hasControlDown()"));

        assertTrue(handler.contains("ddstweak$handleBookScroll(vertical)"));
        assertTrue(handler.contains("BookScrollState.consumeSteps(screen,amount)"));
        assertFalse(handler.contains("axisAmount(horizontal,vertical)"));
        assertTrue(handler.contains("//$$     if(ddstweak$handleBookScroll(amount))cir.setReturnValue(true)"));
    }

    @Test void vanillaBookScreensRespectPageBounds() throws Exception {
        String handler=read("src/main/java/ddstweak/mixin/feature/bookscroll/BookScrollContainerEventHandlerMixin.java");
        String view=read("src/main/java/ddstweak/mixin/feature/bookscroll/BookViewScreenInvoker.java");
        String edit=read("src/main/java/ddstweak/mixin/feature/bookscroll/BookEditScreenAccess.java");

        assertTrue(handler.contains("screen instanceof BookViewScreen"));
        assertTrue(handler.contains("screen instanceof BookEditScreen"));
        assertTrue(handler.contains("while(steps>0&&page>0)"));
        assertTrue(handler.contains("while(steps<0&&page<pages-1)"));

        assertTrue(view.contains("@Accessor(\"currentPage\")"));
        assertTrue(view.contains("@Invoker(\"getNumPages\")"));
        assertTrue(view.contains("@Invoker(\"pageBack\")"));
        assertTrue(view.contains("@Invoker(\"pageForward\")"));

        assertTrue(edit.contains("@Accessor(\"currentPage\")"));
        assertTrue(edit.contains("@Invoker(\"getNumPages\")"));
        assertTrue(edit.contains("//#if MC < 12108"));
        assertTrue(edit.contains("//$$ @Accessor(\"isSigning\")"));
        assertFalse(handler.contains("appendPageToBook"));
    }

    @Test void scribbleCompatibilityStaysOptionalAndNeverCreatesPages() throws Exception {
        String handler=read("src/main/java/ddstweak/mixin/feature/bookscroll/BookScrollContainerEventHandlerMixin.java");
        String access=read("src/main/java/ddstweak/mixin/feature/bookscroll/ScribbleBookScreenAccess.java");
        String lectern=read("src/main/java/ddstweak/mixin/feature/bookscroll/ScribbleLecternScreenMarker.java");
        String json=read("src/main/resources/dds-tweak.mixins.json");

        assertTrue(access.contains("@Pseudo"));
        assertTrue(access.contains("@Mixin(targets=\"me.chrr.scribble.screen.ScribbleBookScreen\",remap=false)"));
        assertTrue(access.contains("@Invoker(value=\"getTotalPages\",remap=false)"));
        assertTrue(access.contains("@Invoker(value=\"showPage\",remap=false)"));
        assertFalse(access.contains("import me.chrr.scribble"));

        assertTrue(lectern.contains("@Pseudo"));
        assertTrue(lectern.contains("@Accessor(value=\"effectivePage\",remap=false)"));

        assertTrue(handler.contains("//#if MC >= 12000"));
        assertTrue(handler.contains("screen instanceof ScribbleBookScreenAccess book"));
        assertTrue(handler.contains("book.ddstweak$showPage(before-1,false)"));
        assertTrue(handler.contains("book.ddstweak$showPage(before+Math.max(1,book.ddstweak$pagesToShow()),false)"));
        assertFalse(handler.contains("book.ddstweak$showPage(before+Math.max(1,book.ddstweak$pagesToShow()),true)"));

        assertTrue(json.contains("feature.bookscroll.ScribbleBookScreenAccess"));
        assertTrue(json.contains("feature.bookscroll.ScribbleLecternScreenMarker"));
    }

    @Test void temporaryRuntimeDiagnosticsAreRemoved() throws Exception {
        String handler=read("src/main/java/ddstweak/mixin/feature/bookscroll/BookScrollContainerEventHandlerMixin.java");
        String json=read("src/main/resources/dds-tweak.mixins.json");

        assertFalse(handler.contains("BookScrollDiagnostics"));
        assertFalse(json.contains("BookScrollMouseHandlerDiagnosticMixin"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/ddstweak/feature/bookscroll/BookScrollDiagnostics.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/ddstweak/mixin/feature/bookscroll/BookScrollMouseHandlerDiagnosticMixin.java")));
    }
}
