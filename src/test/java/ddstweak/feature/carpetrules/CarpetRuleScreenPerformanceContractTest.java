package ddstweak.feature.carpetrules;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleScreenPerformanceContractTest {
    @Test void expensiveRuleAndSidebarListsAreReusedWithinEachRenderFrame() throws Exception {
        String screen=Files.readString(Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleManagerScreen.java"));
        assertTrue(screen.contains("beginRenderFrame();"));
        assertTrue(screen.contains("finally{endRenderFrame();}"));
        assertTrue(screen.contains("frameVisibleRules!=null)return frameVisibleRules;"));
        assertTrue(screen.contains("frameUiRules!=null)return frameUiRules;"));
        assertTrue(screen.contains("frameSidebarEntries!=null)return frameSidebarEntries;"));
        assertTrue(screen.contains("frameCategoryEntries!=null)return frameCategoryEntries;"));
        assertTrue(screen.contains("Set<CarpetRuleCatalog.RuleKey> saved=defaultsFilter&&defaultsReady"));
        assertFalse(screen.contains("!CarpetRuleClientNetwork.savedDefaults().contains(rule.key())"));
    }
}
