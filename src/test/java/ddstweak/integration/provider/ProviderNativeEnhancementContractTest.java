package ddstweak.integration.provider;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class ProviderNativeEnhancementContractTest{
    private static final Path ROOT=Path.of("").toAbsolutePath(),JAVA=ROOT.resolve("src/main/java");

    @Test void providerCoordinatorHasNoDdsCapabilityOverlay()throws IOException{
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        assertTrue(coordinator.contains("OrgPlayerInventoryAdapter.tryContext"));
        assertTrue(coordinator.contains("GcaPlayerInventoryAdapter.tryContext"));
        assertTrue(coordinator.contains("new GcaPlayerInventoryActionBinding()"));
        assertTrue(coordinator.contains("new OrgPlayerInventoryActionBinding()"));
        assertTrue(coordinator.contains("Configs.Tweaks.ENHANCED_INVENTORY_GUI"));
        assertFalse(coordinator.contains("DdsFakePlayer"));
        assertFalse(coordinator.contains("peekExtension"));
        assertFalse(coordinator.contains("extension.merge"));
    }

    @Test void trustedOrgReturnUpgradesImmediatelyWhileOrdinaryCommandCanUpgradeAfterMenuCreation()throws IOException{
        String mixin=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String screenMixin=read("ddstweak/mixin/fakeplayer/AbstractContainerScreenMixin.java");
        assertTrue(mixin.contains("DdsFakePlayerClientNetwork.consumeProviderContext(type,containerId)"));
        assertTrue(mixin.contains("OrgPlayerInventoryAdapter.observeMenu(type,containerId,title)"));
        assertTrue(mixin.contains("GcaPlayerInventoryAdapter.tryContext"));
        assertTrue(coordinator.contains("OrgPlayerInventoryAdapter.tryContext(sourceMenu,title)"));
        assertTrue(screenMixin.contains("containerTick"));
    }

    @Test void canonicalStorageScreenIsNeverReinterpretedAsLegacyProviderEnder()throws IOException{
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        int start=coordinator.indexOf("public static boolean tryUpgradeCurrent()");
        int end=coordinator.indexOf("private static boolean tryUpgrade(",start);
        assertTrue(start>=0&&end>start);
        String entry=coordinator.substring(start,end);
        assertTrue(entry.contains("source.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu"));
        assertTrue(entry.indexOf("source.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu")
                <entry.indexOf("return tryUpgrade(minecraft,source)"));
        assertTrue(coordinator.contains("new EnhancedStorageScreen("));
    }

    @Test void providerEnderAlwaysSkipsTheIntermediateSourceScreenDuringCanonicalReopen()throws IOException{
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String extension=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionContext.java");
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        assertTrue(extension.contains("canonicalSize"));
        assertTrue(extension.contains("\"canonical_size\""));
        assertTrue(network.contains("consumeProviderContext(MenuType<?> type,int containerId)"));
        assertTrue(dispatch.contains("if(providerEnder){"));
        assertTrue(dispatch.contains("DdsQcaClientNetwork.sendOpenOwnerEnder(containerId,context)"));
        assertTrue(dispatch.contains("PlayerInventoryOpenContext.expectEnder(context)"));
        assertTrue(dispatch.contains("MinecraftCompat.setScreen(minecraft,null)"));
        assertFalse(dispatch.contains("extension.canonicalSize()==54"));
        assertTrue(dispatch.contains("dds_qca"));
        assertTrue(dispatch.contains("discardExtension(containerId,extension)"));
    }

    @Test void lateAuthoritativeContextAndStagingFallbackAreExplicit()throws IOException{
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        assertTrue(coordinator.contains("registerAuthoritativeContextResolver"));
        assertTrue(coordinator.contains("authoritativeContext(type,containerId)"));
        assertTrue(network.contains("registerAuthoritativeContextResolver(DdsFakePlayerClientNetwork::consumeProviderContext)"));
        assertTrue(coordinator.contains("restoreProviderEnder"));
        assertTrue(dispatch.contains("stageOwnerEnderFallback(staging,context,title)"));
    }

    @Test void localControlStateIsRememberedInsteadOfExposingUnknownServerWarnings()throws IOException{
        String state=read("ddstweak/playerinventory/PlayerInventoryClientActionState.java");
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        assertTrue(state.contains("Map<String,State> STATES"));
        assertTrue(state.contains("attackInterval=PlayerInventoryBehaviorV1.DEFAULT_INTERVAL"));
        assertTrue(state.contains("useInterval=PlayerInventoryBehaviorV1.DEFAULT_INTERVAL"));
        assertTrue(screen.contains("PlayerInventoryClientActionState.forContext(menu.context())"));
        assertTrue(screen.contains("dds-tweak.fakeplayer.interval.tooltip"));
        assertFalse(screen.contains("tooltip.authoritative"));
        assertFalse(screen.contains("tooltip.local"));
        assertFalse(screen.contains("tooltip.unknown"));
    }

    @Test void semanticSlotRolesRemainTheFutureQcaBoundary()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        for(String token:new String[]{"PlayerInventorySlotRole.MAIN","PlayerInventorySlotRole.HOTBAR","PlayerInventorySlotRole.ENDER_STORAGE","PlayerInventorySlotRole.VIEWER_MAIN","PlayerInventorySlotRole.VIEWER_HOTBAR","PlayerInventorySlotRole.CONTROL","PlayerInventorySlotRole.DISABLED"})assertTrue(menu.contains(token),token);
    }

    @Test void ipnUsesStorageSemanticsForInventoryAndEnderButNotController()throws IOException{
        String compat=read("ddstweak/integration/dds/fakeplayer/client/ipn/DdsIpnCompat.java");
        String hints=read("ddstweak/mixin/integration/inventoryprofilesnext/IpnHintsManagerMixin.java");
        assertTrue(compat.contains("menu.kind()==PlayerInventoryKind.INVENTORY||menu.kind()==PlayerInventoryKind.ENDER_CHEST"));
        assertTrue(compat.contains("case MAIN:case ENDER_STORAGE:return menu.targetMainStart()"));
        assertTrue(compat.contains("case MAIN:case ENDER_STORAGE:return menu.targetMainEnd()"));
        assertTrue(compat.contains("default:return -1"));
        assertTrue(hints.contains("type==UnifiedPlayerInventoryMainScreen.class"));
        assertTrue(hints.contains("type==UnifiedPlayerEnderChestScreen.class"));
        assertFalse(hints.contains("UnifiedPlayerInventoryScreen.class.isAssignableFrom"));
    }

    @Test void controllerAndEnderScreenPolicyIsCentralizedInProviderCoordinator()throws IOException{
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        assertTrue(dispatch.contains("ProviderPlayerInventoryScreenCoordinator.showEnhancedScreen(minecraft,menu,title)"));
        assertTrue(coordinator.contains("menu.kind()==PlayerInventoryKind.CONTROLLER"));
        assertTrue(coordinator.contains("new UnifiedPlayerInventoryScreen"));
        assertTrue(coordinator.contains("menu.kind()==PlayerInventoryKind.ENDER_CHEST"));
    }

    @Test void realAndOfflineEnderUseStorageWhileFakeKeepsItsOwnScreen()throws IOException{
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        int start=coordinator.indexOf("public static void showEnhancedScreen(");
        int end=coordinator.indexOf("private static Component nonFakeEnderTitle(",start);
        assertTrue(start>=0&&end>start);
        String policy=coordinator.substring(start,end);
        assertTrue(policy.contains("menu.kind()==PlayerInventoryKind.ENDER_CHEST"));
        assertTrue(policy.contains("menu.targetType()!=PlayerInventoryTargetType.FAKE"));
        assertTrue(policy.contains("new EnhancedStorageScreen("));
        assertTrue(policy.contains("nonFakeEnderTitle(menu,title),menu.visualRows()"));
        assertTrue(policy.contains("new UnifiedPlayerEnderChestScreen("));
        assertTrue(policy.contains("new UnifiedPlayerInventoryMainScreen("));
        assertTrue(policy.contains("new UnifiedPlayerInventoryRealScreen("));
        assertTrue(screen.contains("menu.targetType()==PlayerInventoryTargetType.FAKE&&(isEnder()||supported(button))"));
    }

    @Test void itemScrollerFailsClosedOnProviderControlSlots()throws IOException{
        String mixin=read("ddstweak/mixin/integration/itemscroller/InventoryUtilsMixin.java");
        assertTrue(mixin.contains("case VIEWER_MAIN:case VIEWER_HOTBAR:return fallback;default:return 0"));
        assertTrue(mixin.contains("case MAIN:case ENDER_STORAGE:return menu.targetMainStart()"));
        assertTrue(mixin.contains("case HOTBAR:return menu.targetHotbarStart()"));
        assertTrue(mixin.contains("int slot=ddsTweak$menuSlot()"));
        assertTrue(mixin.contains("menu.context().allows(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP)"));
        assertTrue(mixin.contains("return ddsTweak$useCarriedScratch()?UnifiedPlayerInventoryMirrorMenu.CARRIED_SCRATCH_BUTTON:original"));
    }

    @Test void gcaMainAndHotbarKeepDistinctContainerIdentityForItemScroller()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("GCA_MAIN_MAPPING=createMapping(18,27)"));
        assertTrue(menu.contains("GCA_HOTBAR_MAPPING=createMapping(45,9)"));
        assertTrue(menu.contains("main=new MappedContainer(target,GCA_MAIN_MAPPING),hotbar=new MappedContainer(target,GCA_HOTBAR_MAPPING)"));
        assertTrue(menu.contains("new Slot(main,i"));
        assertTrue(menu.contains("new Slot(hotbar,i"));
        assertFalse(menu.contains("GCA_STORAGE_MAPPING=createMapping(18,36)"));
        assertTrue(menu.contains("sharing one 36-slot"));
    }

    @Test void orgMainAndHotbarKeepDistinctContainerIdentityForItemScroller()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("ORG_HOTBAR_MAPPING=createMapping(0,9)"));
        assertTrue(menu.contains("ORG_MAIN_MAPPING=createMapping(9,27)"));
        assertTrue(menu.contains("hotbar=new MappedContainer(target,ORG_HOTBAR_MAPPING),main=new MappedContainer(target,ORG_MAIN_MAPPING)"));
        assertTrue(menu.contains("new Slot(hotbar,i"));
        assertTrue(menu.contains("new Slot(main,i"));
        assertFalse(menu.contains("ORG_STORAGE_MAPPING=createMapping(0,36)"));
    }

    @Test void itemScrollerNormalizesAllSlotBasedClicksToUnifiedMenuIds()throws IOException{
        String mixin=read("ddstweak/mixin/integration/itemscroller/InventoryUtilsMixin.java");
        assertTrue(mixin.contains("@ModifyArgs(method=\"clickSlot(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;II"));
        assertTrue(mixin.contains("AccessorUtils;handleMouseClick"));
        assertTrue(mixin.contains("gui.getMenu().slots.indexOf(slot)"));
        assertTrue(mixin.contains("args.set(2,actual)"));
        assertTrue(mixin.contains("gui.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu"));
        assertTrue(mixin.contains("itemscroller_slot_normalize"));
        assertFalse(mixin.contains("@ModifyArgs(method=\"addStackTo\""));
    }

    @Test void inventoryIpnPlacementMatchesActualScreenAndEnderRetainsTitle()throws IOException{
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        String fakeHints=read("ddstweak/mixin/integration/inventoryprofilesnext/UnifiedPlayerInventoryScreenIpnHintsMixin.java");
        String realHints=read("ddstweak/mixin/integration/inventoryprofilesnext/UnifiedPlayerInventoryRealScreenIpnHintsMixin.java");
        String realScreen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryRealScreen.java");
        String hintManager=read("ddstweak/mixin/integration/inventoryprofilesnext/IpnHintsManagerMixin.java");
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        assertTrue(coordinator.contains("nonFakeEnderTitle(menu,title)"));
        assertTrue(coordinator.contains("dds-tweak.playerinventory.offline_ender_title"));
        assertTrue(screen.contains("renderTargetName(g)"));
        assertTrue(screen.contains("dds-tweak.playerinventory.offline_name"));
        assertTrue(realScreen.contains("extends UnifiedPlayerInventoryScreen"));
        assertTrue(hintManager.contains("type==UnifiedPlayerInventoryRealScreen.class"));
        assertEquals(4,fakeHints.split("top=44",-1).length-1);
        // All four controls form a single horizontal toolbar at the requested screen offset.
        assertEquals(4,realHints.split("top=66",-1).length-1);
        assertFalse(realHints.contains("bottom="));
        assertTrue(network.contains("&&isStructuralTarget(context)"));
    }

    @Test void providerControlClickPredictionNeverMutatesButtonStacks()throws IOException{
        for(String path:new String[]{
                "ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java",
                "ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java"}){
            String binding=read(path);
            assertFalse(binding.contains("menu.getSlot(slotId).set"));
            assertTrue(binding.contains("menu!=null&&menu.slotRole(slotId)==PlayerInventorySlotRole.CONTROL"));
        }
    }

    @Test void enhancedGcaEnderUsesOnlyExposedStorageRows()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("GCA_ENDER_STORAGE_START=27"));
        assertTrue(menu.contains("GCA_ENDER_TARGET_END=54"));
        assertTrue(menu.contains("for(int i=0;i<GCA_ENDER_STORAGE_START;i++)addSlot(hidden(target,i))"));
        assertTrue(menu.contains("addSlot(new Slot(storage,i,8+c*18,18+r*18))"));
        assertTrue(menu.contains("visualRows(){return rows;}"));
    }

    @Test void unifiedSwitchLetsTweakOwnGenericThreeAndSixRowStorageIncludingAms()throws IOException{
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String screen=read("ddstweak/integration/provider/EnhancedStorageScreen.java");
        assertTrue(coordinator.contains("Configs.Tweaks.ENHANCED_INVENTORY_GUI"));
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        assertTrue(coordinator.contains("type==MenuType.GENERIC_9x3"));
        assertTrue(coordinator.contains("type==MenuType.GENERIC_9x6"));
        assertTrue(coordinator.contains("type==MenuType.SHULKER_BOX"));
        assertTrue(dispatch.contains("type==MenuType.GENERIC_9x6?6:type==MenuType.GENERIC_9x3||type==MenuType.SHULKER_BOX?3:0"));
        assertTrue(dispatch.contains("new EnhancedStorageScreen"));
        assertTrue(screen.contains("114+rows*18"));
        assertTrue(screen.contains("generic_54.png"));
    }

    @Test void qcaEnderTransitionPreservesFakePlayerContextAndAmsRows()throws IOException{
        String open=read("ddstweak/playerinventory/PlayerInventoryOpenContext.java");
        String input=read("ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String network=read("ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        assertTrue(input.contains("PlayerInventoryOpenContext.expectEnder(((UnifiedPlayerInventoryMirrorMenu)menu).context())"));
        assertTrue(dispatch.contains("PlayerInventoryOpenContext.consumeEnder(type,containerId,title)"));
        assertTrue(open.contains("rows==6?PlayerInventoryLayouts.ENDER_6_V1:PlayerInventoryLayouts.ENDER_3_V1"));
        assertTrue(open.contains("CarpetPlayerCommandBridge.supports(PlayerInventoryAction.TOGGLE_SNEAK)"));
        assertTrue(open.contains("actions.add(PlayerInventoryAction.TOGGLE_SNEAK)"));
        assertTrue(open.contains("PlayerInventoryAction.ENDER_DROP_ALL"));
        assertTrue(open.contains("PlayerInventoryAction.ENDER_STORE_ALL"));
        assertTrue(open.contains("PlayerInventoryAction.ENDER_TAKE_ALL"));
        assertTrue(coordinator.contains("DdsQcaClientNetwork.sendOpenOwnerEnder(sourceMenu,context)"));
        assertTrue(dispatch.contains("DdsQcaClientNetwork.sendOpenOwnerEnder(containerId,context)"));
        assertTrue(network.contains("qca_open_owner_ender_c2s")||read("ddstweak/integration/dds/qca/DdsQcaPackets.java").contains("qca_open_owner_ender_c2s"));
    }

    @Test void fakeOnlyEnderRpcRequiresAuthoritativeIdentity()throws IOException{
        String resolver=read("ddstweak/integration/provider/PlayerIdentityResolver.java");
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        String network=read("ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        String open=read("ddstweak/playerinventory/PlayerInventoryOpenContext.java");
        assertTrue(resolver.contains("isAuthoritativeFake(PlayerInventoryContext context)"));
        assertTrue(resolver.contains("\"dds\".equals(context.source())||\"dds_qca\".equals(context.source())"));
        assertTrue(network.contains("PlayerIdentityResolver.isAuthoritativeFake(context)"));
        assertTrue(network.contains("PlayerIdentityResolver.isAuthoritativeFake(mirror.context())"));
        assertTrue(menu.contains("PlayerIdentityResolver.isAuthoritativeFake(context)"));
        assertTrue(open.contains("PlayerIdentityResolver.isAuthoritativeFake(source)"));
    }

    @Test void revokedProviderIdentityCannotReuseStaleFakeCapabilities()throws IOException{
        String resolver=read("ddstweak/integration/provider/PlayerIdentityResolver.java");
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        String extension=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionActionBinding.java");
        assertTrue(resolver.contains("currentType(PlayerInventoryContext context)"));
        assertTrue(resolver.contains("return context.targetUuid()==null?PlayerInventoryTargetType.UNKNOWN:resolve(context.targetUuid())"));
        assertTrue(menu.contains("PlayerIdentityResolver.currentType(context)"));
        assertTrue(menu.contains("action==PlayerInventoryAction.DROP_ALL"));
        assertTrue(menu.contains("action==PlayerInventoryAction.DISCONNECT"));
        assertTrue(menu.contains("action==PlayerInventoryAction.TOGGLE_SNEAK"));
        assertTrue(extension.contains("PlayerIdentityResolver.isAuthoritativeFake(menu.context())"));
    }

    @Test void preferredDdsRouteIsComposedOutsideProviderBindingsAndRecomputedLate()throws IOException{
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        String extension=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerExtensionActionBinding.java");
        String router=read("ddstweak/integration/dds/playerinventory/PlayerInventoryActionBindingRouter.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");

        assertTrue(network.contains("PlayerInventoryActionBindingRouter.registerPreferred(DdsFakePlayerExtensionActionBinding::new)"));
        assertTrue(network.contains("DdsFakePlayerPackets.PROVIDER_ACTION_C2S"));
        assertTrue(network.contains("canSendProviderAction(UnifiedPlayerInventoryMirrorMenu menu,PlayerInventoryAction action)"));
        assertTrue(network.contains("action!=PlayerInventoryAction.STOP_ATTACK&&action!=PlayerInventoryAction.STOP_USE"));
        assertTrue(network.contains("PlayerIdentityResolver.isAuthoritativeFake(context)"));
        assertTrue(extension.contains("DdsFakePlayerClientNetwork.sendProviderAction(menu,action,value)"));
        assertTrue(extension.contains("state().record(action,value)"));
        assertTrue(network.contains("activeExtension(UnifiedPlayerInventoryMirrorMenu menu)"));
        assertTrue(network.contains("extension.inventoryKind()==menu.kind()"));
        assertTrue(network.contains("extension.providerMatches(base)"));
        assertTrue(network.contains("PlayerIdentityResolver.isAuthoritativeFake(base)"));
        assertTrue(extension.contains("!isEnderBatch(action)"));
        assertTrue(router.contains("for(PlayerInventoryActionBinding route:routes)if(route.supports(action))return route"));
        assertTrue(router.contains("return route!=null&&route.send(action,value)"));
        assertTrue(router.contains("current.outranks(bestConfidence)"));
        assertTrue(router.contains("current==PlayerInventoryStateConfidence.AUTHORITATIVE"));
        assertTrue(coordinator.contains("PlayerInventoryActionBindingRouter.compose(provider)"));
        assertTrue(menu.contains("context.supports(action)&&actions.supports(action)"));

        for(String path:new String[]{
                "ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java",
                "ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java"}){
            String binding=read(path);
            assertFalse(binding.contains("DdsFakePlayer"),path);
            int nativeRoute=binding.indexOf("sendNative(slot)");
            int command=binding.indexOf("CarpetPlayerCommandBridge.send",nativeRoute);
            assertTrue(nativeRoute>=0&&command>nativeRoute,path);
        }
    }

    @Test void providerBindingsRouteEnderBatchActionsThroughQca()throws IOException{
        for(String path:new String[]{"ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java","ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java"}){
            String binding=read(path);
            assertTrue(binding.contains("DdsQcaClientNetwork.sendOwnerEnderAction(menu,action)"));
            assertTrue(binding.contains("ENDER_DROP_ALL"));
            assertTrue(binding.contains("ENDER_STORE_ALL"));
            assertTrue(binding.contains("ENDER_TAKE_ALL"));
        }
    }

    private static String read(String path)throws IOException{return Files.readString(JAVA.resolve(path));}
}
