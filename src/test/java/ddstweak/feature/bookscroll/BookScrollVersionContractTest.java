package ddstweak.feature.bookscroll;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class BookScrollVersionContractTest {
    @Test void scrollCallbackUsesFourParametersStartingAt1202()throws IOException{
        Path root=Path.of("").toAbsolutePath();
        while(root!=null&&!Files.exists(root.resolve("settings.json")))root=root.getParent();
        assertNotNull(root,"DT repository root");
        String source=Files.readString(root.resolve(
                "src/main/java/ddstweak/mixin/feature/bookscroll/BookScrollContainerEventHandlerMixin.java"));

        assertTrue(source.contains("//#if MC >= 12002"));
        assertFalse(source.contains("//#if MC >= 12003"));
        assertTrue(source.contains(
                "double mouseX,double mouseY,double horizontal,double vertical,CallbackInfoReturnable<Boolean> cir"));
        assertTrue(source.contains(
                "double mouseX,double mouseY,double amount,CallbackInfoReturnable<Boolean> cir"));
        assertTrue(source.contains("ddstweak$handleBookScroll(vertical)"));
        assertTrue(source.contains("ddstweak$handleBookScroll(amount)"));
    }
}
