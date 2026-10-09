package ddstweak.integration.dds.playerinventory;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

final class DdsPlayerInventoryStrictStructureContractTest {
    private static final Path ROOT=findRepositoryRoot(), JAVA=ROOT.resolve("src/main/java");

    @Test void strictPacketsAndResultHandlerAreRegistered() throws IOException {
        String packets=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerPackets.java");
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");

        assertTrue(packets.contains("player_inventory_canonical_ender_c2s"));
        assertTrue(packets.contains("player_inventory_return_parent_c2s"));
        assertTrue(packets.contains("player_inventory_structure_result_s2c"));
        assertTrue(network.contains("registerC2S(DdsFakePlayerPackets.CANONICAL_ENDER_C2S)"));
        assertTrue(network.contains("registerC2S(DdsFakePlayerPackets.RETURN_PARENT_C2S)"));
        assertTrue(network.contains("registerOrderedS2CHandler(DdsFakePlayerPackets.STRUCTURE_RESULT_S2C"));
        assertTrue(network.contains("session_generation"));
        assertTrue(network.contains("request_id"));
    }

    @Test void strictCanonicalIsPreferredButHistoricalQcaFallbackRemains() throws IOException {
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String screens=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");

        assertTrue(coordinator.contains("tryStructuralReopen(sourceMenu,context)"));
        assertTrue(coordinator.indexOf("tryStructuralReopen(sourceMenu,context)")
                < coordinator.indexOf("DdsQcaClientNetwork.sendOpenOwnerEnder(sourceMenu,context)"));
        assertTrue(screens.contains("DdsFakePlayerClientNetwork.tryCanonicalEnder(staging,context)"));
        assertTrue(screens.indexOf("DdsFakePlayerClientNetwork.tryCanonicalEnder(staging,context)")
                < screens.indexOf("DdsQcaClientNetwork.sendOpenOwnerEnder(staging,context)"));
    }

    @Test void strictCapableProviderWaitsForContextBeforeLegacyOwnerEnderFallback() throws IOException {
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String screens=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");

        int start=network.indexOf("public static boolean shouldDeferLegacyOwnerEnder(");
        int end=network.indexOf("public static boolean tryCanonicalEnder(",start);
        assertTrue(start>=0&&end>start);
        String guard=network.substring(start,end);
        assertTrue(guard.contains("isProvider(context)"));
        assertTrue(guard.contains("isStructuralTarget(context)"),
                "strict-capable real and fake Ender must defer legacy fallback until canonical authority arrives");
        assertFalse(guard.contains("PlayerIdentityResolver.isAuthoritativeFake(context)"),
                "real-player Ender must not be routed through the 27-slot legacy fallback");
        assertTrue(network.contains("isStructuralTarget(context)"));
        assertTrue(network.contains("type==PlayerInventoryTargetType.FAKE||type==PlayerInventoryTargetType.REAL"));
        assertTrue(guard.contains("DdsClientNetwork.canSend(DdsFakePlayerPackets.CANONICAL_ENDER_C2S)"));
        assertTrue(guard.contains("return defer;"));
        assertTrue(guard.contains("return false;")); // 1.21.10 and older cannot use strict RPC

        assertTrue(screens.contains("boolean allowLegacyOwnerEnder=!DdsFakePlayerClientNetwork.shouldDeferLegacyOwnerEnder(context)"));
        assertTrue(screens.contains("allowLegacyOwnerEnder&&staging!=null&&DdsQcaClientNetwork.sendOpenOwnerEnder(staging,context)"));
        assertTrue(screens.contains("allowLegacyOwnerEnder&&staging==null&&DdsQcaClientNetwork.sendOpenOwnerEnder(containerId,context)"));
        assertTrue(coordinator.contains("&&!shouldDeferLegacyOwnerEnder(context)"));
        assertTrue(coordinator.contains("registerLegacyOwnerEnderDeferResolver(LegacyOwnerEnderDeferResolver resolver)"));
        assertFalse(coordinator.contains("DdsFakePlayer"));
        assertTrue(network.contains("registerLegacyOwnerEnderDeferResolver(DdsFakePlayerClientNetwork::shouldDeferLegacyOwnerEnder)"));
        assertTrue(coordinator.contains("DdsQcaClientNetwork.sendOpenOwnerEnder(sourceMenu,context)"));

        // Without strict transport, the historical owner-Ender fallback remains reachable.
        assertTrue(screens.indexOf("DdsFakePlayerClientNetwork.tryCanonicalEnder(staging,context)")
                <screens.indexOf("boolean allowLegacyOwnerEnder="));
    }

    @Test void providerEnderCanWaitOffscreenForStrictContextWithoutTransientThreeRowFrame() throws IOException {
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String screens=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        assertTrue(screens.contains("stageProviderEnderAwaitingStrict(staging,context,title)"));
        assertTrue(screens.indexOf("stageProviderEnderAwaitingStrict(staging,context,title)")
                <screens.indexOf("boolean allowLegacyOwnerEnder="));
        assertTrue(network.contains("PROVIDER_CONTEXT_WAIT_NANOS=750_000_000L"));
        assertTrue(network.contains("AwaitingProviderEnder awaitingProviderEnder"));
        assertFalse(network.contains("explicitOfflineOrg"));
        assertTrue(network.contains("consumeProviderContext(menuType(awaiting.menu),context.containerId())"));
        assertTrue(network.contains("tryCanonicalEnder(awaiting.menu,authoritative)"));
        assertTrue(network.contains("provider_ender_wait_complete"));
        assertTrue(network.contains("provider_ender_wait_timeout"));
        assertTrue(network.contains("ProviderPlayerInventoryScreenCoordinator.restoreProviderEnder"));
    }

    @Test void lateStrictContextReopensAlreadyVisibleProviderEnderWithoutQcaOrRemoteRule() throws IOException {
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        int start=network.indexOf("private static void handleExtensionContext(");
        int end=network.indexOf("private static void handleExtensionClear(",start);
        assertTrue(start>=0&&end>start);
        String handler=network.substring(start,end);
        assertTrue(handler.contains("if(!commitExtension(context.containerId(),context))return;"));
        assertTrue(handler.contains("PlayerInventoryContext merged=context.merge(screen.getMenu().context())"));
        assertTrue(handler.contains("PlayerInventoryLayouts.isGcaEnder(merged)"));
        assertTrue(handler.contains("PlayerInventoryLayouts.isOrgEnder(merged)"));
        assertTrue(handler.contains("tryCanonicalEnder(screen.getMenu(),merged)"));
        assertTrue(handler.indexOf("tryCanonicalEnder(screen.getMenu(),merged)")
                <handler.indexOf("stageCanonicalFallback(screen.getMenu(),merged,screen.getTitle())"));
        assertTrue(handler.indexOf("stageCanonicalFallback(screen.getMenu(),merged,screen.getTitle())")
                <handler.indexOf("MinecraftCompat.setScreen(mc,null)"));
        assertFalse(handler.contains("remoteFakePlayerEnderChest"));
        assertFalse(handler.contains("sendOpenOwnerEnder("));
    }

    @Test void strictCanonicalAcceptsOnlyOnlineRealAndFakeTargets() throws IOException {
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String extension=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionContext.java");
        assertTrue(network.contains("isStructuralExtension(extension)"));
        assertTrue(network.contains("extension.targetState()==PlayerInventoryTargetState.ONLINE"));
        assertTrue(network.contains("extension.targetType()==PlayerInventoryTargetType.FAKE"));
        assertTrue(network.contains("extension.targetType()==PlayerInventoryTargetType.REAL"));
        assertTrue(network.contains("isStructuralTarget(context)"));
        assertTrue(network.contains("context.targetState()==PlayerInventoryTargetState.ONLINE"));
        assertFalse(network.contains("context.contextSchema()==3&&context.targetState()==PlayerInventoryTargetState.OFFLINE"));
        assertTrue(network.contains("PlayerIdentityResolver.currentType(context)"));
        assertTrue(extension.contains("actionAuthority=contextSchema==1||contextSchema==3&&this.targetType==PlayerInventoryTargetType.FAKE"));
    }

    @Test void strictFakeContextRetainsServerAdvertisedActions() throws IOException {
        String extension=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionContext.java");

        assertTrue(extension.contains("contextSchema==3&&this.targetType==PlayerInventoryTargetType.FAKE"));
        assertTrue(extension.contains("schema==3&&targetType==PlayerInventoryTargetType.FAKE"));
        assertTrue(extension.contains("parseActions(MinecraftCompat.stringOr(tag,\"supported_actions\",\"\"),actions)"));
    }

    @Test void strictActionsCarrySessionGeneration() throws IOException {
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");

        assertTrue(network.contains("if(context.strictSession())data.putLong(\"session_generation\",context.sessionGeneration())"));
        assertTrue(network.contains("data.putInt(\"container_id\",context.containerId())"));
        assertTrue(network.contains("data.putString(\"target_uuid\",context.targetUuid().toString())"));
    }

    @Test void extensionOffKeepsEnhancedUiAndProviderFallbackAvailable() throws IOException {
        String configs=read("ddstweak/config/Configs.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String router=read("ddstweak/integration/dds/playerinventory/PlayerInventoryActionBindingRouter.java");
        String gca=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        String org=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java");

        assertTrue(configs.contains("ENHANCED_INVENTORY_GUI=MalilibCompat.bool(\"enhancedInventoryGui\",true"));
        assertTrue(coordinator.contains("if(!Configs.Tweaks.ENHANCED_INVENTORY_GUI.getBooleanValue()"));
        assertTrue(coordinator.contains("return PlayerInventoryActionBindingRouter.compose(provider)"));
        assertTrue(router.contains("routes.add(fallback)"));
        assertTrue(router.contains("for(PlayerInventoryActionBinding route:routes)if(route.supports(action))return route"));
        assertTrue(gca.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(gca.contains("CarpetPlayerCommandBridge.send(menu.context().targetName(),action,value)"));
        assertTrue(org.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(org.contains("CarpetPlayerCommandBridge.send(menu.context().targetName(),action,value)"));
    }

    @Test void strictContextKeepsProviderCommandActionsAndIdentityIndependentOfExtension() throws IOException {
        String extension=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionContext.java");
        String identity=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerIdentityClient.java");
        String resolver=read("ddstweak/integration/provider/PlayerIdentityResolver.java");

        assertTrue(extension.contains("for(PlayerInventoryAction action:base.supportedActions())"));
        assertTrue(extension.contains("for(PlayerInventoryAction action:supportedActions)"));
        assertTrue(resolver.contains("DdsFakePlayerIdentityClient.resolve(uuid)"));
        assertTrue(resolver.contains("OrgFakePlayerIdentityClient.isFake(uuid)"));
        assertFalse(identity.contains("PlayerInventoryExtension"));
    }

    @Test void modernStandardActionsPreferCarpetCommandsBeforeExtensionRpcFallback() throws IOException {
        String extension=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionActionBinding.java");
        String gca=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        String org=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java");

        assertTrue(extension.contains("!standardCommandPreferred(action)"));
        assertTrue(extension.contains("standardCommandPreferred(action)||!action.allowedIn(menu.kind())"));
        assertTrue(extension.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(extension.contains("#if MC >= 12111"));
        assertTrue(extension.contains("DdsFakePlayerClientNetwork.canSendExtensionAction(menu,action)"));
        assertTrue(extension.contains("DdsFakePlayerClientNetwork.canSendProviderAction(menu,action)"));

        assertTrue(gca.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(gca.contains("CarpetPlayerCommandBridge.send(menu.context().targetName(),action,value)"));
        assertTrue(org.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(org.contains("CarpetPlayerCommandBridge.send(menu.context().targetName(),action,value)"));
    }

    @Test void returnRequiresExplicitCapabilityAndFailedReturnCanCloseNormally() throws IOException {
        String capability=read("ddstweak/integration/dds/playerinventory/PlayerInventoryInteractionCapability.java");
        String extension=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionContext.java");
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String mixin=read("ddstweak/mixin/integration/dds/playerinventory/PlayerInventoryReturnMixin.java");
        String mixins=Files.readString(ROOT.resolve("src/main/resources/dds-tweak.mixins.json"));

        assertTrue(capability.contains("RETURN_PARENT"));
        assertTrue(extension.contains("\"return_parent\""));
        assertTrue(network.contains("canReturnParent(UnifiedPlayerInventoryMirrorMenu menu)"));
        assertTrue(network.contains("sendReturnParent(UnifiedPlayerInventoryMirrorMenu menu)"));
        assertTrue(network.contains("consumeReturnCloseBypass"));
        assertTrue(mixin.contains("sendReturnParent"));
        assertTrue(mixin.contains("consumeReturnCloseBypass"));
        assertTrue(mixins.contains("integration.dds.playerinventory.PlayerInventoryReturnMixin"));
    }

    @Test void pendingUsesThreeSecondTickTimeoutAndConnectionReset() throws IOException {
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String bootstrap=read("ddstweak/integration/dds/DdsIntegrationBootstrap.java");

        assertTrue(network.contains("STRUCTURE_TIMEOUT_NANOS=3_000_000_000L"));
        assertTrue(network.contains("DdsPlayerInventoryStructureState"));
        assertTrue(network.contains("onClientTick()"));
        assertTrue(network.contains("STRUCTURE_STATE.expire"));
        assertTrue(network.contains("STRUCTURE_STATE.clear()"));
        assertTrue(bootstrap.contains("DdsFakePlayerClientNetwork.onClientTick()"));
    }

    @Test void acceptedResultDoesNotFabricateMenuAndCanonicalContextIsConsumedNormally() throws IOException {
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");

        assertTrue(network.contains("DdsPlayerInventoryStructureState.Result.ACCEPTED"));
        assertTrue(network.contains("observeNextContext"));
        assertTrue(network.contains("consumeObservedOpen"));
        assertTrue(network.contains("canonical"));
        assertFalse(network.contains("new UnifiedPlayerInventoryMirrorMenu("));
    }

    private static String read(String path) throws IOException {
        return Files.readString(JAVA.resolve(path));
    }

    private static Path findRepositoryRoot() {
        for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())
            if(Files.exists(p.resolve("settings.json"))&&Files.isDirectory(p.resolve("src/main")))return p;
        throw new IllegalStateException("Unable to locate repository root");
    }
}
