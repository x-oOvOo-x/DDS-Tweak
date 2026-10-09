package ddstweak.integration.dds.qca;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsQcaClientNetworkContractTest {
    @Test void delayedParentNavigationDoesNotDiscardNestedFakePlayerReturn()throws IOException{
        String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        int start=code.indexOf("private static void handleNavigationState(");
        int end=code.indexOf("private static void rememberPlayerInventoryParent(",start);
        assertTrue(start>=0&&end>start);
        String state=code.substring(start,end);
        assertTrue(state.contains("id==nestedEnderSource"),"source Ender state must be distinguished from child state");
        assertTrue(state.contains("id==nestedInventorySource"),"source inventory state must be distinguished from child state");
        assertTrue(state.contains("if(!enderSourceState)clearNestedEnder()"),"late source state must preserve the Ender parent");
        assertTrue(state.contains("if(!inventorySourceState)clearNestedInventory()"),"late source state must preserve the inventory parent");
    }
    private static Path root(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root not found");}
    private static String read(String p)throws IOException{return Files.readString(root().resolve(p));}
    @Test void qcaOwnsAllFeatureCapabilities()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");for(String token:new String[]{"OPEN_SLOT_C2S","OPEN_INVENTORY_SLOT_C2S","STORAGE_CLICK_C2S","CREATIVE_STORAGE_C2S","NAVIGATE_BACK_C2S","CREATIVE_STORAGE_RESULT_S2C","NAVIGATION_STATE_S2C"})assertTrue(code.contains(token),token);}
    @Test void inputTakeoverRequiresAuthoritativeServerSchema()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");assertTrue(code.contains("canTakeOverInput(DdsQcaPackets.OPEN_SLOT_C2S)"));assertTrue(code.contains("canTakeOverInput(DdsQcaPackets.STORAGE_CLICK_C2S)"));assertTrue(code.contains("canTakeOverInput(DdsQcaPackets.NAVIGATE_BACK_C2S)"));}
    @Test void creativeTransactionKeepsQueueTimeoutAndServerResult()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");assertTrue(code.contains("5_000_000_000L"));assertTrue(code.contains("CREATIVE_QUEUE"));assertTrue(code.contains("transaction"));assertTrue(code.contains("handled"));assertTrue(code.contains("DdsQcaStackCodec.get"));}
    @Test void ownerEnderStagingIsBoundedAndObjectScoped()throws IOException{
        String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        String dispatch=read("src/main/java/ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String bootstrap=read("src/main/java/ddstweak/integration/dds/DdsIntegrationBootstrap.java");
        assertTrue(code.contains("OWNER_ENDER_TIMEOUT_NANOS=3_000_000_000L"));
        assertTrue(code.contains("AbstractContainerMenu watched=pending.stagingMenu!=null?pending.stagingMenu:pending.sourceMenu"));
        assertTrue(code.contains("sourceMenu==source&&sourceContainerId==containerId"));
        assertTrue(code.contains("pending.sourceMenu!=null&&pending.sourceMenu!=staging"));
        assertTrue(code.contains("ProviderPlayerInventoryScreenCoordinator.restoreProviderEnder"));
        assertTrue(code.contains("pendingOwnerEnder=null"));
        assertTrue(dispatch.contains("stageOwnerEnderFallback(staging,context,title)"));
        assertTrue(bootstrap.contains("DdsQcaClientNetwork.onClientTick()"));
    }
    @Test void extensionRevocationDowngradesCachedReturnContexts()throws IOException{
        String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        assertTrue(code.contains("revokeProviderExtensionAuthority(UUID targetUuid)"));
        assertTrue(code.contains("entry.setValue(PlayerIdentityResolver.withoutStrictSession(entry.getValue()))"));
        assertTrue(code.contains("nestedEnderParent=PlayerIdentityResolver.withoutStrictSession(nestedEnderParent)"));
        assertTrue(code.contains("nestedInventoryParent=PlayerIdentityResolver.withoutStrictSession(nestedInventoryParent)"));
        assertTrue(code.contains("PlayerInventoryOpenContext.revokeStrictAuthority(targetUuid)"));
    }
    @Test void ownerEnderRejectUsesDedicatedBackwardCompatibleMarker()throws IOException{
        String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        assertTrue(code.contains("\"owner_ender_rejected\""));
        assertTrue(code.contains("\"request_container_id\""));
        assertTrue(code.contains("restoreOwnerEnderFallback()"));
    }
    @Test void nestedInventoryParentSurvivesEnderShulkerBackToEnhancedProviderInventory()throws IOException{
        String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        assertTrue(code.contains("nestedInventoryViaEnder"));
        assertTrue(code.contains("Objects.equals(nestedInventoryParent.targetUuid(),mirror.context().targetUuid())"));
        assertTrue(code.contains("&&(!nestedInventoryViaEnder||ownerEnderMatchesInventory)"));
        assertTrue(code.contains("PlayerIdentityResolver.navigationFallback(nestedEnderParent)"));
        assertTrue(code.contains("PlayerIdentityResolver.navigationFallback(nestedInventoryParent)"));
        assertTrue(code.contains("OrgPlayerInventoryOpenIntent.expectReopenInventory(parent)"));
        assertTrue(code.contains("PlayerInventoryOpenContext.expectInventory(parent)"));
    }
    @Test void connectionResetClearsFeatureState()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");assertTrue(code.contains("DdsClientSessionHooks.registerResetHook(DdsQcaClientNetwork::resetSession)"));assertTrue(code.contains("resetSession(){restoreOwnerEnderFallback();"));assertTrue(code.contains("clearNavigation()"));assertTrue(code.contains("resetCreativeStorage()"));}
}
