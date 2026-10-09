package ddstweak.integration.org.playerinventory;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class OrgPlayerInventoryAdapterContractTest{
    private static final Path ROOT=Path.of("").toAbsolutePath(),JAVA=ROOT.resolve("src/main/java");

    @Test void orgContextUsesProviderMarkersWithoutDds()throws IOException{
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        assertTrue(adapter.contains("carpet_Org_Addition$isOldPlayerInventoryScreen"));
        assertTrue(adapter.contains("carpet_Org_Addition$isWithButton"));
        assertTrue(adapter.contains("boat.carpetorgaddition.client.util.ClientUtils"));
        assertTrue(adapter.contains("ORG_INVENTORY_V1"));
        assertTrue(adapter.contains("ORG_CONTROLS_V1"));
        assertFalse(adapter.contains("DdsFakePlayer"));
    }

    @Test void org12111CanUpgradeFromCommandSessionWithoutAnyClientMarker()throws IOException{
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        assertTrue(adapter.contains("session==null"));
        assertTrue(adapter.contains("session.kind()==OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST"));
        assertTrue(adapter.contains("PlayerInventoryLayouts.ORG_INVENTORY_V1"));
        assertTrue(adapter.contains("PlayerInventoryLayouts.ORG_ENDER_3_V1"));
        assertTrue(dispatch.contains("PlayerInventoryContext context=DdsFakePlayerClientNetwork.consumeProviderContext(type,containerId)"));
        assertTrue(dispatch.contains("if(context==null)context=OrgPlayerInventoryAdapter.observeMenu(type,containerId,title)"));
        assertTrue(adapter.contains("trustedReturnContext(containerId,session)"));
        assertTrue(adapter.contains("session!=null&&session.kind()==OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST"));
        assertTrue(adapter.contains("return context(containerId,identity(title,session),PlayerInventoryKind.ENDER_CHEST"));
        assertFalse(dispatch.contains("OrgPlayerInventoryAdapter.tryCommandContext"));
    }

    @Test void onlineOrgTargetsExposeServerAuthorizedCarpetActions()throws IOException{
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String binding=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java");
        assertTrue(adapter.contains("identity.state!=PlayerInventoryTargetState.ONLINE||identity.type==PlayerInventoryTargetType.REAL"));
        assertTrue(adapter.contains("actions.addAll(CarpetPlayerCommandBridge.supportedActions())"));
        assertTrue(adapter.contains("actions.add(PlayerInventoryAction.STOP_ATTACK)"));
        assertTrue(adapter.contains("actions.add(PlayerInventoryAction.STOP_USE)"));
        assertFalse(adapter.contains("for(PlayerInventoryAction action:PlayerInventoryAction.values())"));
        assertTrue(adapter.contains("actions(identity,kind,controls)"));
        assertTrue(adapter.contains("actions.remove(PlayerInventoryAction.DROP_ALL)"));
        assertTrue(adapter.contains("actions.remove(PlayerInventoryAction.DISCONNECT)"));
        assertTrue(adapter.contains("actions.remove(PlayerInventoryAction.TOGGLE_SNEAK)"));
        assertTrue(adapter.contains("PlayerInventoryTargetType.UNKNOWN"));
        assertFalse(adapter.contains("getLatency()==0"));
    }

    @Test void unknownTargetsNeverFallBackToNativeControlSlots()throws IOException{
        String binding=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java");
        assertTrue(binding.contains("if(type==PlayerInventoryTargetType.UNKNOWN)return CarpetPlayerCommandBridge.available()"));
        assertTrue(binding.contains("if(type==PlayerInventoryTargetType.UNKNOWN)return CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(binding.contains("menu.targetType()==PlayerInventoryTargetType.FAKE"));
    }

    @Test void orgRoutesExactNativeThenCommandWithoutDdsDependency()throws IOException{
        String binding=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryActionBinding.java");
        int nativeRoute=binding.indexOf("sendNative(slot)");
        int command=binding.indexOf("CarpetPlayerCommandBridge.send",nativeRoute);
        assertTrue(nativeRoute>=0&&command>nativeRoute);
        assertFalse(binding.contains("DdsFakePlayer"));
        assertTrue(binding.contains("PlayerInventoryLayouts.isOrgControls"));
        assertTrue(binding.contains("case ATTACK_CONTINUOUS:return 6"));
        assertTrue(binding.contains("case USE_CONTINUOUS:return 8"));
        assertTrue(binding.contains("case STOP_ALL:return 0"));
        assertFalse(binding.contains("case STOP_ATTACK:"));
        assertFalse(binding.contains("case STOP_USE:"));
        assertFalse(binding.contains("case ATTACK_INTERVAL:"));
        assertTrue(binding.contains("PlayerInventoryClientActionState"));
    }

    @Test void ordinaryOrgLayoutPreservesAuthoritativeSlotIds()throws IOException{
        String layouts=read("ddstweak/integration/dds/playerinventory/PlayerInventoryLayouts.java");
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(layouts.contains("ORG_INVENTORY_V1"));
        assertTrue(layouts.contains("ORG_CONTROLS_V1"));
        for(String token:new String[]{"ORG_DISABLED_START=43","ORG_TARGET_END=54","addOrgInventory","targetMainStart=9","targetMainEnd=36","targetHotbarStart=0","targetHotbarEnd=9"})assertTrue(menu.contains(token),token);
    }

    @Test void ordinaryOrgArmorAndStorageKeepServerSlotMeaning()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("ORG_HOTBAR_MAPPING=createMapping(0,9)"));
        assertTrue(menu.contains("ORG_MAIN_MAPPING=createMapping(9,27)"));
        assertFalse(menu.contains("ORG_STORAGE_MAPPING=createMapping(0,36)"));
        assertTrue(menu.contains("ORG_EQUIPMENT_MAPPING=createMapping(36,7)"));
        assertTrue(menu.contains("ORG_DISABLED_MAPPING=createMapping(43,11)"));
        assertTrue(menu.contains("new EquipmentClientSlot(equipment,0,8,8,false,helmetIcon())"));
        assertTrue(menu.contains("new EquipmentClientSlot(equipment,1,8,26,false,chestplateIcon())"));
        assertTrue(menu.contains("new EquipmentClientSlot(equipment,2,8,44,false,leggingsIcon())"));
        assertTrue(menu.contains("new EquipmentClientSlot(equipment,3,8,62,false,bootsIcon())"));
        assertTrue(menu.contains("addSlot(hidden(equipment,5));addSlot(hidden(equipment,6))"));
        assertTrue(menu.contains("ORG_EQUIPMENT_END=41"));
        assertTrue(menu.contains("if(slot<ORG_EQUIPMENT_END)return PlayerInventorySlotRole.EQUIPMENT"));
    }

    @Test void orgCommandIntentAcceptsSlashAndOptionalCaseSensitiveFlag()throws IOException{
        String intent=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryOpenIntent.java");
        assertTrue(intent.contains("while(normalized.startsWith"));
        assertTrue(intent.contains("normalized.substring(1).trim()"));
        assertTrue(intent.contains("parts.length!=3&&parts.length!=4"));
        assertTrue(intent.contains("\"true\".equalsIgnoreCase(parts[3])"));
        assertTrue(intent.contains("\"false\".equalsIgnoreCase(parts[3])"));
        assertTrue(intent.contains("pending=null"));
        assertTrue(intent.contains("currentConnection()==expected"));
        assertTrue(intent.contains("candidate==expected"));
        assertFalse(intent.contains("title.getString().contains(target)"));
    }

    @Test void explicitOfflineOrgCommandKeepsDiscoveryIntentWithoutFakeAuthority() throws IOException {
        String intent=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryOpenIntent.java");
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        int observe=intent.indexOf("public static synchronized void observeCommand(");
        int trustedReturn=intent.indexOf("public static synchronized void expectReopenInventory(",observe);
        String command=intent.substring(observe,trustedReturn);
        assertTrue(command.contains("PlayerInfo targetInfo=playerInfo(connection,null,parsed.target)"));
        assertFalse(command.contains("if(targetInfo==null)return"));
        assertTrue(command.contains("pending=new Pending(parsed.target,parsed.kind"));
        assertTrue(intent.contains("if(expected==null)return true"));
        assertTrue(adapter.contains("return new Identity(null,session.target(),PlayerInventoryTargetType.REAL,PlayerInventoryTargetState.OFFLINE)"));
        assertTrue(intent.contains("carpet-org-addition.operation.offline_player_name"));
        assertTrue(intent.contains("carpet.commands.player.inventory.offline.display_name"));
        assertTrue(intent.contains("MinecraftCompat.translatable"));
        assertTrue(adapter.contains("identity.state!=PlayerInventoryTargetState.ONLINE||identity.type==PlayerInventoryTargetType.REAL"));
    }

    @Test void unrelatedOpenScreenDoesNotConsumeTrustedOrgCommandOrReturn() throws IOException {
        String intent=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryOpenIntent.java");
        int start=intent.indexOf("public static synchronized Session bind(");
        int end=intent.indexOf("public static synchronized Session session(",start);
        assertTrue(start>=0&&end>start);
        String bind=intent.substring(start,end);
        assertTrue(bind.contains("if(!sameConnection(p.connection)||!samePlayerInfo(p.connection,p.targetInfo))"));
        assertTrue(bind.contains("pending=null;"));
        assertTrue(bind.contains("if(!typeMatches||!titleMatches){"));
        assertTrue(bind.contains("return null;"));
        assertTrue(bind.indexOf("if(!typeMatches||!titleMatches){")
                <bind.lastIndexOf("pending=null;"));
        assertTrue(bind.indexOf("pending=null;",bind.indexOf("if(!typeMatches||!titleMatches){"))
                >bind.indexOf("return null;",bind.indexOf("if(!typeMatches||!titleMatches){")));
        assertTrue(intent.contains("PENDING_TTL_NANOS=3_000_000_000L"));
        assertTrue(intent.contains("pending=new Pending(source.targetName(),Kind.INVENTORY"));
        assertFalse(intent.contains("source.targetState()==PlayerInventoryTargetState.OFFLINE"));
        assertTrue(intent.contains("if(!online)return;"));
        assertTrue(intent.contains("org_bind_deferred"));
    }

    @Test void offlineOrgMenuKeepsNativeScreenAndCannotCanonicalize() throws IOException {
        String intent=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryOpenIntent.java");
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        String network=read("ddstweak/integration/dds/fakeplayer/DdsFakePlayerClientNetwork.java");
        assertTrue(intent.contains("isOfflineMenu(MenuType<?> type,int containerId,Component title)"));
        assertTrue(adapter.contains("OrgPlayerInventoryOpenIntent.isOfflineMenu(type,containerId,title)"));
        assertTrue(dispatch.contains("OrgPlayerInventoryOpenIntent.isOfflineMenu(type,containerId,title)"));
        assertTrue(coordinator.contains("OrgPlayerInventoryOpenIntent.isOfflineMenu(type,containerId,title)"));
        assertTrue(network.contains("context.targetState()==PlayerInventoryTargetState.ONLINE"));
        assertFalse(network.contains("explicitOfflineOrg"));
    }

    @Test void offlineOrgEnderCorrelatesThreeOrSixRowsAndExactComposedTitle(){
        assertTrue(OrgPlayerInventoryOpenIntent.matchesRows(
                3,OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST,false));
        assertFalse(OrgPlayerInventoryOpenIntent.matchesRows(
                6,OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST,false));
        assertTrue(OrgPlayerInventoryOpenIntent.matchesRows(
                6,OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST,true));
        assertTrue(OrgPlayerInventoryOpenIntent.matchesRows(
                6,OrgPlayerInventoryOpenIntent.Kind.INVENTORY,false));
        assertFalse(OrgPlayerInventoryOpenIntent.matchesRows(
                3,OrgPlayerInventoryOpenIntent.Kind.INVENTORY,true));
        assertFalse(OrgPlayerInventoryOpenIntent.matchesRows(
                0,OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST,true));
        assertTrue(OrgPlayerInventoryOpenIntent.matchesOfflineEnderTitle(
                "Ender Chest - Alex（离线）","Ender Chest","Alex（离线）"));
        assertFalse(OrgPlayerInventoryOpenIntent.matchesOfflineEnderTitle(
                "Ender Chest - Alexa（离线）","Ender Chest","Alex（离线）"));
        assertFalse(OrgPlayerInventoryOpenIntent.matchesOfflineEnderTitle(
                "Ender Chest","Ender Chest","Alex（离线）"));
    }

    @Test void nativeOfflineMarkerIsFoundInComposedTitleWithoutCommandIntent(){
        net.minecraft.network.chat.Component title=net.minecraft.network.chat.Component.translatable("container.enderchest")
                .append(" - ")
                .append(net.minecraft.network.chat.Component.translatable(
                        "carpet-org-addition.operation.offline_player_name","Alex"));
        assertTrue(OrgPlayerInventoryOpenIntent.isOfflineTitle(title));
        assertTrue(OrgPlayerInventoryOpenIntent.isOfflineTitle(
                net.minecraft.network.chat.Component.translatable(
                        "carpet.commands.player.inventory.offline.display_name","Alex")));
        assertFalse(OrgPlayerInventoryOpenIntent.isOfflineTitle(
                net.minecraft.network.chat.Component.translatable("container.enderchest")));
        assertFalse(OrgPlayerInventoryOpenIntent.isOfflineTitle(
                net.minecraft.network.chat.Component.literal("Alex")));
    }

    @Test void nativeOfflineClassificationSurvivesSessionTtlWhileItsMenuIsOpen() throws IOException {
        long afterTtl=61_000_000_000L;
        assertFalse(OrgPlayerInventoryOpenIntent.sessionExpired(afterTtl,0L,true));
        assertTrue(OrgPlayerInventoryOpenIntent.sessionExpired(afterTtl,0L,false));
        assertFalse(OrgPlayerInventoryOpenIntent.sessionExpired(1_000_000_000L,0L,false));
        String intent=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryOpenIntent.java");
        assertTrue(intent.contains("sessionExpired(now,session.createdNanos(),isCurrentMenu(session))"));
        assertTrue(intent.contains("mc.player.containerMenu!=mc.player.inventoryMenu"));
    }

    @Test void explicitOrgEnderCommandCanStageBeforeFirstVisibleFrame() throws IOException {
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        assertTrue(adapter.contains("session.kind()==OrgPlayerInventoryOpenIntent.Kind.ENDER_CHEST"));
        assertTrue(adapter.contains("PlayerInventoryLayouts.ORG_ENDER_3_V1"));
        assertTrue(dispatch.contains("stageProviderEnderAwaitingStrict(staging,context,title)"));
        assertTrue(dispatch.indexOf("stageProviderEnderAwaitingStrict(staging,context,title)")
                <dispatch.indexOf("MinecraftCompat.setScreen(minecraft,new EnhancedStorageScreen"));
    }

    @Test void verifiedOrgCommandUpgradesBeforeInitialFrameAndKeepsProviderLayout() throws IOException {
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String coordinator=read("ddstweak/integration/provider/ProviderPlayerInventoryScreenCoordinator.java");
        assertTrue(dispatch.contains("OrgPlayerInventoryOpenIntent.session(containerId,type,title)"));
        assertTrue(dispatch.contains("org.kind()==OrgPlayerInventoryOpenIntent.Kind.INVENTORY"));
        assertTrue(dispatch.contains("ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent()"));
        assertTrue(dispatch.indexOf("ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent()")
                <dispatch.indexOf("ci.cancel();",dispatch.indexOf("OrgPlayerInventoryOpenIntent.session(containerId,type,title)")));
        assertTrue(adapter.contains("marker(menu,OLD_MARKER),controls=marker(menu,BUTTON_MARKER)"));
        assertTrue(adapter.contains("ORG_CONTROLS_V1:PlayerInventoryLayouts.ORG_INVENTORY_V1"));
        assertTrue(coordinator.contains("OrgPlayerInventoryAdapter.tryContext(sourceMenu,title)"));
    }

    @Test void orgTitleCorrelationIsExactAndDoesNotConfusePrefixNames(){
        assertTrue(OrgPlayerInventoryOpenIntent.matchesVisibleTitle("Ann","Ann",null));
        assertFalse(OrgPlayerInventoryOpenIntent.matchesVisibleTitle("Anna","Ann",null));
        assertTrue(OrgPlayerInventoryOpenIntent.matchesVisibleTitle("[VIP] Ann","Ann","[VIP] Ann"));
        assertFalse(OrgPlayerInventoryOpenIntent.matchesVisibleTitle("Other [VIP] Ann","Ann","[VIP] Ann"));
    }

    @Test void qcaOrgReturnKeepsFullParentContextAndSkipsRawScreen()throws IOException{
        String intent=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryOpenIntent.java");
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String qca=read("ddstweak/integration/dds/qca/DdsQcaClientNetwork.java");
        String dispatch=read("ddstweak/mixin/fakeplayer/MenuScreensMixin.java");
        assertTrue(qca.contains("PlayerIdentityResolver.navigationFallback(nestedInventoryParent)"));
        assertTrue(qca.contains("OrgPlayerInventoryOpenIntent.expectReopenInventory(parent)"));
        assertTrue(intent.contains("PlayerInventoryContext returnContext"));
        assertTrue(intent.contains("source.inventoryKind()!=PlayerInventoryKind.INVENTORY"));
        assertTrue(adapter.contains("session.returnContext()"));
        assertTrue(adapter.contains("PlayerInventoryLayouts.isOrgInventory(source)&&!PlayerInventoryLayouts.isOrgControls(source)"));
        assertTrue(dispatch.contains("PlayerInventoryContext context=DdsFakePlayerClientNetwork.consumeProviderContext(type,containerId)"));
        assertTrue(dispatch.contains("if(context==null)context=OrgPlayerInventoryAdapter.observeMenu(type,containerId,title)"));
    }

    @Test void orgEnderUsesExplicitCommandCorrelationInsteadOfTitleGuessing()throws IOException{
        String intent=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryOpenIntent.java");
        String adapter=read("ddstweak/integration/org/playerinventory/OrgPlayerInventoryAdapter.java");
        String mixin=read("ddstweak/mixin/playerinventory/ClientPacketListenerMixin.java");
        String layouts=read("ddstweak/integration/dds/playerinventory/PlayerInventoryLayouts.java");
        assertTrue(intent.contains("enderChest"));
        assertTrue(intent.contains("GENERIC_9x3"));
        assertTrue(adapter.contains("observeMenu"));
        assertTrue(adapter.contains("ORG_ENDER_3_V1"));
        assertTrue(mixin.contains("OrgPlayerInventoryOpenIntent.observeCommand"));
        assertTrue(layouts.contains("ORG_ENDER_3_V1"));
    }

    private static String read(String path)throws IOException{return Files.readString(JAVA.resolve(path));}
}
