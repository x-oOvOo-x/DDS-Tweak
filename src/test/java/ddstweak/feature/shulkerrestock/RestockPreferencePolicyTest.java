package ddstweak.feature.shulkerrestock;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class RestockPreferencePolicyTest {
    private static String source(String path) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null) {
            Path target = root.resolve(path);
            if (Files.isRegularFile(target)) return Files.readString(target);
            root = root.getParent();
        }
        throw new IllegalStateException("Missing source: " + path);
    }

    @Test void allTargetsRegisterPreferencePacketsAndConfiguration() throws Exception {
        String network = source("src/main/java/ddstweak/feature/shulkerrestock/ShulkerRestockClientNetwork.java");
        assertTrue(network.contains("registerC2S(PREFERENCES_C2S)"));
        assertTrue(network.contains("registerS2CHandler(STATUS_S2C"));
        assertFalse(network.contains("//#if MC >= 12111"));
        String integration = source("src/main/java/ddstweak/integration/dds/DdsIntegrationBootstrap.java");
        assertTrue(integration.contains("ShulkerRestockClientNetwork.initialize()"));
        assertTrue(integration.contains("ShulkerRestockClientNetwork.onClientTick()"));
        String config = source("src/main/java/ddstweak/config/Configs.java");
        assertTrue(config.contains("SHULKER_RESTOCK_ENABLED,SHULKER_RESTOCK_MODE,SHULKER_RESTOCK_WHITELIST"));
        assertTrue(source("src/main/java/ddstweak/feature/shulkerrestock/ShulkerRestockFilterMode.java")
                .contains("implements IConfigOptionListEntry"));
    }

    @Test void allIgnoresEvenAnInvalidStoredWhitelist(){
        List<String> invalid=Collections.nCopies(129,"minecraft:stone");
        assertFalse(RestockPreferencePolicy.valid(invalid));
        assertTrue(RestockPreferencePolicy.effectiveIds("ALL",invalid).isEmpty());
        assertTrue(RestockPreferencePolicy.valid(RestockPreferencePolicy.effectiveIds("ALL",invalid)));
    }

    @Test void enforcesItemAndPayloadBounds(){
        List<String> items=new ArrayList<>();
        for(int i=0;i<128;i++)items.add("custom:item_"+i);
        assertTrue(RestockPreferencePolicy.valid(items));
        items.add("custom:overflow");
        assertFalse(RestockPreferencePolicy.valid(items));
        assertFalse(RestockPreferencePolicy.valid(List.of("minecraft:stone","minecraft:stone")));
        assertFalse(RestockPreferencePolicy.valid(List.of("not-a-registry-id")));
        assertFalse(RestockPreferencePolicy.valid(List.of("mod:"+ "a".repeat(125))));
        List<String> longIds=new ArrayList<>();
        for(int i=0;i<128;i++)longIds.add("mod:"+"a".repeat(63)+i);
        assertFalse(RestockPreferencePolicy.valid(longIds));
    }

    @Test void rejectedUnchangedPreferencesDoNotTriggerEndlessResends(){
        assertFalse(RestockPreferencePolicy.retryAfterModeChange(7,"client","client"));
        assertFalse(RestockPreferencePolicy.retryAfterModeChange(0,"client","client"));
        assertFalse(RestockPreferencePolicy.retryAfterModeChange(0,"client","off"));
        assertFalse(RestockPreferencePolicy.retryAfterModeChange(0,"client","always"));
    }

    @Test void enteringClientModeResubmitsOnServerNotification(){
        assertTrue(RestockPreferencePolicy.retryAfterModeChange(0,"off","client"));
        assertTrue(RestockPreferencePolicy.retryAfterModeChange(0,"always","client"));
        assertTrue(RestockPreferencePolicy.retryAfterModeChange(0,"unknown","client"));
        assertFalse(RestockPreferencePolicy.retryAfterModeChange(9,"off","client"));
    }

    @Test void whitelistAndBlacklistPreserveTheirDistinctSelections(){
        List<String> white=List.of("minecraft:bread"),black=List.of("minecraft:rotten_flesh");
        assertSame(white,RestockPreferencePolicy.effectiveIds("WHITELIST",white));
        assertSame(black,RestockPreferencePolicy.effectiveIds("BLACKLIST",black));
    }
}
