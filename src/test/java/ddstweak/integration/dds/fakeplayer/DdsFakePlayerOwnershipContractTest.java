package ddstweak.integration.dds.fakeplayer;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Ensures DDS-specific clients cannot leak back into the provider-neutral player-inventory UI. */
final class DdsFakePlayerOwnershipContractTest{
    private static final Path ROOT=Path.of("").toAbsolutePath(),JAVA=ROOT.resolve("src/main/java");

    @Test void providerUiAndBindingsContainNoDdsFakePlayerDependency()throws IOException{
        for(String path:new String[]{
                "ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java",
                "ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java",
                "ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java",
                "ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java",
                "ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java"}){
            String code=read(path);assertFalse(code.contains("DdsFakePlayer"),path);assertFalse(code.contains("carpetddsaddition."),path);
        }
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        assertTrue(dispatch.contains("DdsFakePlayerClientNetwork.consumeProviderContext"));
        assertFalse(dispatch.contains("carpetddsaddition."));
    }

    @Test void carpetControllerLivesOutsideAnyProviderAdapter()throws IOException{
        Path bridge=JAVA.resolve("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandBridge.java");
        assertTrue(Files.isRegularFile(bridge));
        assertFalse(Files.exists(JAVA.resolve("ddstweak/integration/gca/playerinventory/CarpetPlayerCommandBridge.java")));
    }

    @Test void qcaStillConsumesSemanticSlotRolesWithoutOwningUi()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        String policy=read("ddstweak/integration/dds/qca/DdsQcaClientSlotAccessPolicy.java");
        assertTrue(menu.contains("DdsQcaSlotRoleProvider"));
        assertTrue(policy.contains("DdsQcaSlotRoleProvider.Role"));
        assertTrue(menu.contains("case MAIN:case HOTBAR:return Role.TARGET_STORAGE"));
        assertTrue(menu.contains("case ENDER_STORAGE:return Role.ENDER_STORAGE"));
        assertTrue(menu.contains("case CONTROL:return Role.CONTROL"));
        assertTrue(menu.contains("case DISABLED:return Role.DISABLED"));
    }

    private static String read(String path)throws IOException{return Files.readString(JAVA.resolve(path));}
}
