package ddstweak.integration.provider;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class ProviderNativeEnhancementContractTest{
    private static final Path ROOT=Path.of("").toAbsolutePath(),JAVA=ROOT.resolve("src/main/java");

    @Test void clientDiagnosticsUseDtJvmProperty()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        assertTrue(screen.contains("Boolean.getBoolean(\"ddstweak.playerinventory.trace\")"));
        assertTrue(screen.contains("LogManager.getLogger(\"ddstweak.playerinventory\")"));
        assertFalse(screen.contains("Boolean.getBoolean(\"dds.playerinventory.trace\")"));
    }

    @Test void mouseButtonsUseSdlIdsOnlyOn263()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        assertTrue(screen.contains("//#if MC >= 260300"));
        assertTrue(screen.contains("return button==1;"));
        assertTrue(screen.contains("return button==3;"));
        assertTrue(screen.contains("//$$ return button==1;"));
        assertTrue(screen.contains("//$$ return button==3;"));
        assertTrue(screen.contains("        return button==0;"));
        assertTrue(screen.contains("        return button==1;"));
        assertTrue(screen.contains("if(leftClick(button)&&isInventory()"));
        assertTrue(screen.contains("if((leftClick(button)||rightClick(button))&&pressActionButton"));
        assertTrue(screen.contains("if(leftClick(button)){if(supported(b))b.press()"));
        assertTrue(screen.contains("PlayerInventoryAction action=leftClick(button)?buttonAction(b):onceAction(b)"));
        assertTrue(screen.contains("if(leftClick(e.button()))pressedStepper=-1"));
    }

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
        String policy=read("ddstweak/integration/dds/playerinventory/PlayerInventorySlotPolicy.java");
        assertTrue(compat.contains("PlayerInventorySlotPolicy.targetSortableRange(menu,hovered)"));
        assertTrue(compat.contains("PlayerInventorySlotPolicy.targetStorageSlots(menu)"));
        assertTrue(policy.contains("isTargetStorageRole(menu.kind(),menu.slotRole(hoveredId))"));
        assertTrue(policy.contains("kind==PlayerInventoryKind.ENDER_CHEST&&role==PlayerInventorySlotRole.ENDER_STORAGE"));
        assertTrue(policy.contains("kind!=PlayerInventoryKind.INVENTORY&&kind!=PlayerInventoryKind.ENDER_CHEST"));
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

    @Test void onlyAuthoritativeFakeEnderReceivesToolbar()throws IOException{
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        int start=coordinator.indexOf("public static void showEnhancedScreen(");
        int end=coordinator.indexOf("private static Component nonFakeEnderTitle(",start);
        assertTrue(start>=0&&end>start);
        String policy=coordinator.substring(start,end);
        assertTrue(policy.contains("menu.kind()==PlayerInventoryKind.ENDER_CHEST"));
        assertTrue(policy.contains("boolean playerEnder=PlayerIdentityResolver.isAuthoritativeFake(menu.context())"));
        assertTrue(policy.contains("menu.context().targetUuid()!=null"));
        assertTrue(policy.contains("!menu.context().targetUuid().equals(minecraft.player.getUUID())"));
        assertFalse(policy.contains("boolean playerEnder=menu.context().targetState()!=PlayerInventoryTargetState.OFFLINE"));
        assertTrue(policy.contains("//$$ boolean playerEnder=menu.targetType()==PlayerInventoryTargetType.FAKE;"));
        assertTrue(policy.contains("new EnhancedStorageScreen("));
        assertTrue(policy.contains("nonFakeEnderTitle(menu,title),menu.visualRows()"));
        assertTrue(policy.contains("new UnifiedPlayerEnderChestScreen("));
        assertTrue(policy.contains("new UnifiedPlayerInventoryMainScreen("));
        assertTrue(policy.contains("new UnifiedPlayerInventoryRealScreen("));
        int visible=screen.indexOf("private boolean visible(ActionButton button)");
        assertTrue(visible>=0);
        String visibility=screen.substring(visible,screen.indexOf("//#else",visible));
        assertTrue(visibility.contains("return true;"));
        assertFalse(visibility.contains("targetType()"));
        assertTrue(screen.contains("addButton(ENDER_SNEAK_X,ENDER_BUTTON_Y,ICON_SNEAK"));
        assertTrue(screen.contains("addButton(ENDER_DISCONNECT_X,ENDER_BUTTON_Y,ICON_DISCONNECT"));
    }

    @Test void modernProviderControlsUseCarpetCommandsWithoutFakeIdentity()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        String carpet=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandActionBinding.java");
        String gca=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        String org=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String resolver=read("ddstweak/integration/provider/PlayerIdentityResolver.java");

        assertTrue(screen.contains("return canUse(buttonAction(button))"));
        assertTrue(screen.contains("case ICON_ATTACK_INTERVAL:return button.isSelected()?stopAction(true):PlayerInventoryAction.ATTACK_INTERVAL"));
        assertTrue(screen.contains("private boolean visible(ActionButton button)"));
        assertTrue(screen.contains("boolean transport=menu.canSendActions()"));
        assertTrue(menu.contains("return action.allowedWithoutFakeIdentity()"));
        assertTrue(menu.contains("return canonical&&PlayerIdentityResolver.isAuthoritativeFake(context)"));
        assertTrue(carpet.contains("&&CarpetPlayerCommandBridge.available()"));
        assertTrue(gca.contains("actions.addAll(CarpetPlayerCommandBridge.supportedActions())"));
        assertFalse(gca.contains("if(type==PlayerInventoryTargetType.REAL)actions.remove(PlayerInventoryAction.DISCONNECT)"));
        assertFalse(org.contains("if(identity.type==PlayerInventoryTargetType.REAL)\n                actions.remove(PlayerInventoryAction.DISCONNECT)"));
        assertFalse(gca.contains("\n            actions.remove(PlayerInventoryAction.DROP_ALL);"));
        assertTrue(legacyActionPolicy(gca).contains("//$$     actions.remove(PlayerInventoryAction.TOGGLE_SNEAK);"));
        assertTrue(gca.contains("actions.addAll(CarpetPlayerCommandBridge.supportedActions())"));
        assertTrue(org.contains("actions.addAll(CarpetPlayerCommandBridge.supportedActions())"));
        assertTrue(resolver.contains("return context.targetUuid()==null?PlayerInventoryTargetType.UNKNOWN:resolve(context.targetUuid())"));
        assertFalse(gca.contains("getLatency()==0"));
    }

    @Test void unresolvedIdentityStillDisplaysControlsButOfflineCannotSend()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        String carpet=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandActionBinding.java");
        String gca=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        String org=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java");
        int visible=screen.indexOf("private boolean visible(ActionButton button)");
        String policy=screen.substring(visible,screen.indexOf("//#else",visible));
        assertTrue(policy.contains("return true;"));
        assertTrue(screen.contains("attackInterval.visible=true"));
        assertTrue(screen.contains("useInterval.visible=true"));
        assertTrue(screen.contains("if(showHotbarControls())"));
        assertTrue(menu.contains("if(context.targetState()==PlayerInventoryTargetState.OFFLINE"));
        assertTrue(menu.contains("&&CarpetPlayerCommandBridge.validTargetName(context.targetName())"));
        assertTrue(carpet.contains("menu.context().targetState()!=PlayerInventoryTargetState.OFFLINE"));
        assertTrue(gca.contains("if(menu.context().targetState()==PlayerInventoryTargetState.OFFLINE)return false"));
        assertTrue(org.contains("if(menu.context().targetState()==PlayerInventoryTargetState.OFFLINE)return false"));
        assertTrue(menu.contains("return canonical&&PlayerIdentityResolver.isAuthoritativeFake(context)"));
    }

    @Test void carpetCanAuthorizeDropAndServerValidatesFakeOnlyDisconnect()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        String carpet=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandActionBinding.java");
        String carpetPolicy=modernCarpetPolicy(menu);
        assertFalse(carpetPolicy.contains("PlayerInventoryAction.DROP_ALL"));
        assertFalse(carpetPolicy.contains("PlayerInventoryAction.DISCONNECT"));
        assertFalse(carpetPolicy.contains("isAuthoritativeFake"));
        assertTrue(menu.contains("return canonical&&PlayerIdentityResolver.isAuthoritativeFake(context)"));
        assertFalse(menu.contains("action==PlayerInventoryAction.DISCONNECT&&targetType()==PlayerInventoryTargetType.REAL"));
        assertFalse(menu.contains("action==PlayerInventoryAction.TOGGLE_SNEAK&&!verifiedFake"));
        assertTrue(menu.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertFalse(carpet.contains("if(action==PlayerInventoryAction.DISCONNECT)return menu.targetType()!=PlayerInventoryTargetType.REAL"));
    }

    @Test void enderSneakUsesStandardCarpetPermissionWithoutClaimingFakeIdentity()throws IOException{
        String action=read("ddstweak/integration/dds/playerinventory/PlayerInventoryAction.java");
        String carpet=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandActionBinding.java");
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        String gca=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        String org=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        assertTrue(action.contains("case TOGGLE_SNEAK: // Carpet's sneak/unsneak"));
        assertFalse(carpet.contains("if(action==PlayerInventoryAction.TOGGLE_SNEAK)return PlayerIdentityResolver.isAuthoritativeFake"));
        assertFalse(menu.contains("action==PlayerInventoryAction.TOGGLE_SNEAK&&!verifiedFake"));
        assertFalse(carpet.contains("if(action==PlayerInventoryAction.DISCONNECT)return menu.targetType()!=PlayerInventoryTargetType.REAL"));
        assertTrue(legacyActionPolicy(gca).contains("//$$     actions.remove(PlayerInventoryAction.TOGGLE_SNEAK);"));
        assertTrue(legacyActionPolicy(org).contains("//$$     actions.remove(PlayerInventoryAction.TOGGLE_SNEAK);"));
        assertTrue(menu.contains("PlayerIdentityResolver.isAuthoritativeFake(context)"));
    }

    @Test void selectedActionNeedsExactStopAndDedicatedStopAllClearsBothModes()throws IOException{
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        String state=read("ddstweak/playerinventory/PlayerInventoryClientActionState.java");
        assertTrue(screen.contains("return attack?PlayerInventoryAction.STOP_ATTACK:PlayerInventoryAction.STOP_USE"));
        assertTrue(screen.contains("if(!canUse(action)||!send(action,0))return"));
        assertTrue(screen.contains("boolean noExactStop=button.isSelected()"));
        assertTrue(screen.contains("dds-tweak.fakeplayer.action.active_stop_all_only"));
        assertTrue(screen.contains("dds-tweak.fakeplayer.action.active_no_stop"));
        assertTrue(screen.contains("return supported(b)||b.isSelected()"));
        assertTrue(screen.contains("if(send(PlayerInventoryAction.STOP_ALL,0))menu.predictStopAll()"));
        assertTrue(state.contains("case STOP_ALL:attackMode=useMode=PlayerInventoryBehaviorV1.MODE_NONE"));
        assertFalse(screen.contains("if(!canUse(exact)&&canUse(PlayerInventoryAction.STOP_ALL))"));
        assertFalse(screen.contains("if(canUse(once))return once"));
    }

    @Test void modernProvidersUseCarpetForActionsAndKeepLegacyNativeCompatibility()throws IOException{
        String gca=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        String gcaAdapter=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        String org=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java");
        String orgAdapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String screen=read("ddstweak/integration/dds/playerinventory/screen/UnifiedPlayerInventoryScreen.java");
        assertTrue(gca.contains("if(CarpetPlayerCommandBridge.supports(action))"));
        int send=gca.indexOf("@Override public boolean send(");
        int modern=gca.indexOf("//#if MC >= 12111",send);
        int legacy=gca.indexOf("//#else",modern);
        assertTrue(send>=0&&modern>send&&legacy>modern);
        String route=gca.substring(modern,legacy);
        assertTrue(route.contains("CarpetPlayerCommandBridge.send(menu.context().targetName(),action,value)"));
        assertFalse(route.contains("sendNative(slot)"));
        assertTrue(route.contains("return false;"));
        int nativeCheck=gca.indexOf("private boolean nativeAvailable()");
        int nativeModern=gca.indexOf("//#if MC >= 12111",nativeCheck);
        int nativeLegacy=gca.indexOf("//#else",nativeModern);
        assertTrue(nativeCheck>=0&&nativeModern>nativeCheck&&nativeLegacy>nativeModern);
        assertTrue(gca.substring(nativeModern,nativeLegacy).contains("return false;"));
        assertTrue(gcaAdapter.contains("//#if MC < 12111"));
        assertFalse(gcaAdapter.contains("if(kind==PlayerInventoryKind.INVENTORY)actions.addAll(GcaPlayerInventoryActionBinding.nativeActions());\n        //#endif"));
        assertTrue(org.contains("private boolean nativeAvailable()"));
        assertTrue(org.contains("return false;"));
        assertTrue(orgAdapter.contains("//#if MC < 12111"));
        assertTrue(legacyActionPolicy(orgAdapter).contains("//$$     if(controls)actions.addAll(OrgPlayerInventoryActionBinding.nativeActions());"));
        assertTrue(screen.contains("attackInterval.active=editable;"));
        assertTrue(screen.contains("useInterval.active=editable;"));
        assertTrue(screen.contains("menu.context().targetState()!=PlayerInventoryTargetState.OFFLINE"));
    }

    @Test void unifiedIpnHintsMatchFakeLayoutAcrossVersions()throws IOException{
        String fake=read("ddstweak/mixin/integration/inventoryprofilesnext/UnifiedPlayerInventoryScreenIpnHintsMixin.java");
        String real=read("ddstweak/mixin/integration/inventoryprofilesnext/UnifiedPlayerInventoryRealScreenIpnHintsMixin.java");
        String legacy=read("ddstweak/mixin/integration/inventoryprofilesnext/DdsFakePlayerScreenIpnHintsMixin.java");
        for(String button:new String[]{"MOVE_TO_PLAYER","SORT","SORT_COLUMNS","SORT_ROWS"}){
            assertTrue(fake.contains("@IPNGuiHint(button=IPNButton."+button+",top=44)"));
            assertTrue(real.contains("@IPNGuiHint(button=IPNButton."+button+",top=66)"));
            assertTrue(legacy.contains("@IPNGuiHint(button=IPNButton."+button+",top=44)"));
        }
        assertFalse(fake.contains("top=22"));
        assertFalse(fake.contains("horizontalOffset"));
        assertFalse(real.contains("horizontalOffset"));
    }

    @Test void itemScrollerFailsClosedOnProviderControlSlots()throws IOException{
        String mixin=read("ddstweak/mixin/integration/itemscroller/InventoryUtilsMixin.java");
        String policy=read("ddstweak/integration/dds/playerinventory/PlayerInventorySlotPolicy.java");
        String gateway=read("ddstweak/mixin/integration/itemscroller/ItemScrollerAccessorUtilsMixin.java");
        assertTrue(mixin.contains("PlayerInventorySlotPolicy.sortableRange(menu,slot)"));
        assertTrue(mixin.contains("ItemScrollerDirectTransfer.moveMatching(menu,id)"));
        assertTrue(policy.contains("isAutomaticStorageRole(menu.slotRole(slotId))"));
        assertTrue(gateway.contains("PlayerInventorySlotPolicy.allowsAutomaticClick(menu,actual)"));
        assertTrue(gateway.contains("ci.cancel()"));
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
        assertFalse(fakeHints.contains("top=22"));
        // UNKNOWN uses the fake-layout marker, not the REAL-screen IPN offset.
        assertEquals(4,realHints.split("top=66",-1).length-1);
        assertFalse(realHints.contains("bottom="));
        assertTrue(network.contains("&&isStructuralTarget(context)"));
    }

    @Test void unknownOnlineProviderUsesFakeInventoryVisualLayoutWithoutEscalatingIdentity()throws IOException{
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String hints=read("ddstweak/mixin/integration/inventoryprofilesnext/UnifiedPlayerInventoryScreenIpnHintsMixin.java");
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(coordinator.contains("menu.context().targetState()==PlayerInventoryTargetState.ONLINE"));
        assertTrue(coordinator.contains("menu.targetType()!=PlayerInventoryTargetType.REAL"));
        assertTrue(coordinator.contains("new UnifiedPlayerInventoryMainScreen("));
        assertTrue(coordinator.contains("new UnifiedPlayerInventoryRealScreen("));
        assertTrue(hints.contains("@IPNGuiHint(button=IPNButton.SORT,top=44)"));
        assertFalse(hints.contains("top=22"));
        assertTrue(menu.contains("PlayerIdentityResolver.isAuthoritativeFake(context)"));
        assertTrue(menu.contains("return canonical&&PlayerIdentityResolver.isAuthoritativeFake(context)"));
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
        assertTrue(menu.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(menu.contains("return action.allowedWithoutFakeIdentity()"));
        assertTrue(menu.contains("return canonical&&PlayerIdentityResolver.isAuthoritativeFake(context)"));
        assertFalse(menu.contains("action==PlayerInventoryAction.DISCONNECT&&targetType()==PlayerInventoryTargetType.REAL"));
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
            int command=binding.indexOf("CarpetPlayerCommandBridge.send");
            int nativeRoute=binding.indexOf("sendNative(slot)");
            // Four modern GCA targets are command-first; Org and legacy stay native-first.
            if(binding.contains("if(CarpetPlayerCommandBridge.supports(action)){"))
                assertTrue(command>=0&&nativeRoute>command,path);
            else
                assertTrue(nativeRoute>=0&&binding.indexOf("CarpetPlayerCommandBridge.send",nativeRoute)>nativeRoute,path);
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

    private static String legacyActionPolicy(String adapter){
        int commands=adapter.indexOf("actions.addAll(CarpetPlayerCommandBridge.supportedActions())");
        assertTrue(commands>=0,"missing Carpet action discovery");
        int start=adapter.indexOf("//#if MC < 12111",commands);
        int end=adapter.indexOf("//#endif",start);
        assertTrue(start>commands&&end>start,"missing legacy action guard");
        return adapter.substring(start,end);
    }

    private static String modernCarpetPolicy(String menu){
        int start=menu.indexOf("// Ordinary Carpet controls are identical for REAL, FAKE and UNKNOWN.");
        int end=menu.indexOf("//#else",start);
        assertTrue(start>=0&&end>start,"missing modern Carpet policy");
        String policy=menu.substring(start,end);
        assertTrue(policy.contains("CarpetPlayerCommandBridge.validTargetName(context.targetName())"));
        assertTrue(policy.contains("CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(policy.contains("action.allowedWithoutFakeIdentity()"));
        return policy;
    }

    private static String read(String path)throws IOException{return Files.readString(JAVA.resolve(path));}
}
