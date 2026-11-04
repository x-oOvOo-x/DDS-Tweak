package ddstweak.feature.silentautoeat;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class LegacyScrollMixinContractTest {
    private static String source(String path) throws Exception {
        return Files.readString(Path.of("src/main/java/ddstweak/mixin/feature/silentautoeat/"+path));
    }

    @Test void versionedScrollTargetsExistAndNeverSwallowInput() throws Exception {
        String container=source("ContainerScreenInputMixin.java");
        String legacy=source("LegacyContainerScrollMixin.java");
        String config=Files.readString(Path.of("src/main/resources/dds-tweak.mixins.json"));

        assertTrue(container.contains("//#if MC >= 12102"));
        assertTrue(container.contains("@Inject(method=\"mouseScrolled\",at=@At(\"HEAD\"))"));
        assertFalse(container.contains("//#if MC >= 12002"));
        assertTrue(legacy.contains("//#if MC < 12102"));
        assertTrue(legacy.contains("@Mixin(MouseHandler.class)"));
        assertTrue(legacy.contains("@Inject(method=\"onScroll\",at=@At(\"HEAD\"))"));
        assertTrue(legacy.contains("minecraft.execute(()->{"));
        assertTrue(legacy.contains("MinecraftCompat.screen(minecraft) instanceof AbstractContainerScreen"));
        assertFalse(legacy.contains("ci.cancel()"));
        assertTrue(config.contains("\"feature.silentautoeat.LegacyContainerScrollMixin\""));
    }
}
