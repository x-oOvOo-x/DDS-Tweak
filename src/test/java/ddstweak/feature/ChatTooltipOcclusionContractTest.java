package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class ChatTooltipOcclusionContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}

    @Test void tweakIsDefaultOffAndLivesInTweaksTab() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        String screen=read("src/main/java/ddstweak/gui/DDSTweakConfigScreen.java");
        assertTrue(configs.contains("CHAT_TOOLTIP_OCCLUSION=MalilibCompat.bool(\"chatTooltipOcclusion\",false,TWEAKS_KEY)"));
        int tweaks=screen.indexOf("private static List<IConfigBase> tweakConfigs()");
        int visual=screen.indexOf("private static List<IConfigBase> visualConfigs()");
        assertTrue(tweaks>=0&&visual>tweaks);
        assertTrue(screen.substring(tweaks,visual).contains("CHAT_TOOLTIP_OCCLUSION"));
        assertTrue(configs.contains("MC >= 11502"));
    }

    @Test void unsupported1144TargetOmitsTheFeatureCompletely() throws Exception {
        String contract=read("config/version-capability-boundaries.json");
        assertTrue(contract.contains("\"id\": \"chatTooltipOcclusion\""));
        assertTrue(contract.contains("\"minMcVersionCode\": 11502"));
        assertTrue(contract.contains("ddstweak/feature/chattooltip/"));
        assertTrue(contract.contains("ddstweak/mixin/feature/chattooltip/"));
        for(String mixin:new String[]{
                "feature.chattooltip.ChatScreenAccessor",
                "feature.chattooltip.CommandSuggestionsAccessor",
                "feature.chattooltip.SuggestionsListAccessor",
                "feature.chattooltip.ChatTooltipRenderMixin"
        })assertTrue(contract.contains(mixin),mixin);
    }

    @Test void suppressionIsLimitedToVisibleSuggestionBounds() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/chattooltip/ChatTooltipOcclusionFeature.java");
        assertTrue(feature.contains("screen instanceof ChatScreen"));
        assertTrue(feature.contains("ddstweak$suggestions()"));
        assertTrue(feature.contains("rect.contains(mouseX,mouseY)"));
        assertFalse(feature.contains("mouseClicked"));
        assertFalse(feature.contains("mouseScrolled"));
    }

    @Test void rendererHookOnlyCancelsHoverRendering() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/chattooltip/ChatTooltipRenderMixin.java");
        String json=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(mixin.contains("renderComponentHoverEffect"));
        assertTrue(mixin.contains("componentHoverEffect"));
        assertTrue(mixin.contains("ChatTooltipOcclusionFeature.shouldSuppress(mouseX,mouseY)"));
        assertTrue(mixin.contains("ci.cancel()"));
        assertFalse(mixin.contains("mouseClicked"));
        assertFalse(mixin.contains("mouseScrolled"));
        for(String name:new String[]{
                "feature.chattooltip.ChatScreenAccessor",
                "feature.chattooltip.CommandSuggestionsAccessor",
                "feature.chattooltip.SuggestionsListAccessor",
                "feature.chattooltip.ChatTooltipRenderMixin"
        })assertTrue(json.contains(name),name);
    }
}
