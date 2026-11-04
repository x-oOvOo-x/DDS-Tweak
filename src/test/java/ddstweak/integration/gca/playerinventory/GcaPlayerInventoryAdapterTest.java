package ddstweak.integration.gca.playerinventory;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class GcaPlayerInventoryAdapterTest{
    private static final Path ROOT=Path.of("").toAbsolutePath(),JAVA=ROOT.resolve("src/main/java");

    @Test void gcaInventoryControllerAndEnderStayDependencyFree()throws IOException{
        String adapter=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        for(String token:new String[]{"gca.player.inventory","gca.player.other_controller","gca.player.ender_chest","GCA_INVENTORY_V1","GCA_CONTROLLER_3_V1","GCA_ENDER_3_V1"})assertTrue(adapter.contains(token),token);
        assertFalse(adapter.contains("DdsFakePlayer"));
        assertFalse(adapter.contains("carpetddsaddition."));
        assertTrue(adapter.contains("PlayerIdentityResolver.resolve(player.getUUID())"));
        assertFalse(adapter.contains("getLatency()==0"),"ping must never classify a player as fake or real");
        assertTrue(adapter.contains("renderedTitleMatches"));
        assertTrue(adapter.contains("的末影箱"));
    }

    @Test void remoteGcaRenderedTitlesMatchOnlineProfilesExactly(){
        java.util.List<String> online=java.util.Arrays.asList("Alex","Alexa");
        assertEquals("Alex",GcaPlayerInventoryAdapter.matchingRenderedTarget("Alex's Inventory",online));
        assertEquals("Alexa",GcaPlayerInventoryAdapter.matchingRenderedTarget("Alexa's Ender Chest",online));
        assertEquals("Alex",GcaPlayerInventoryAdapter.matchingRenderedTarget("Alex的末影箱",online));
        assertEquals("Alex",GcaPlayerInventoryAdapter.matchingRenderedTarget("Alex's Other Controller",online));
        assertEquals("",GcaPlayerInventoryAdapter.matchingRenderedTarget("Alex's Inventory",java.util.Collections.singletonList("Alexa")));
        assertEquals("",GcaPlayerInventoryAdapter.matchingRenderedTarget("Alex's Shulker Box",online));
        assertEquals("",GcaPlayerInventoryAdapter.matchingRenderedTarget("Inventory",online));
    }

    @Test void onlyUniqueAccountEvidenceCanBecomeACarpetTarget(){
        java.util.List<String> online=java.util.Arrays.asList("Log","Alex","Bot_Alex");
        java.util.Map<String,String> displays=new java.util.LinkedHashMap<>();
        displays.put("Log","[Log head] 假的Log");
        displays.put("Alex","假的Alex");
        assertEquals("Log",GcaPlayerInventoryAdapter.verifiedAccountName("Log",online,displays));
        assertEquals("Log",GcaPlayerInventoryAdapter.verifiedAccountName(
                "[Log head] 假的Log",online,displays));
        assertEquals("Alex",GcaPlayerInventoryAdapter.verifiedAccountName("假的Alex",online,displays));
        assertEquals("Bot_Alex",GcaPlayerInventoryAdapter.verifiedAccountName(
                "Bot_Alex",online,java.util.Collections.emptyMap()));
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "假的Log",online,java.util.Collections.emptyMap()));
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "fake_Alex",online,java.util.Collections.emptyMap()));
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "Alex_Bot",online,java.util.Collections.emptyMap()));
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "[Log head] 假的Log",online,java.util.Collections.emptyMap()));
    }

    @Test void differentProfilesWithSameDisplayOrNameConflictAreRejected(){
        java.util.Map<String,String> displays=new java.util.LinkedHashMap<>();
        displays.put("Alex","Bot_Alex");
        displays.put("Bot_Alex","Different");
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "Bot_Alex",java.util.Arrays.asList("Alex","Bot_Alex"),displays));
        displays.put("Bot_Alex","Bot_Alex");
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "Bot_Alex",java.util.Arrays.asList("Alex","Bot_Alex"),displays));
        displays.clear();
        displays.put("Alex","假的玩家");
        displays.put("Steve","假的玩家");
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "假的玩家",java.util.Arrays.asList("Alex","Steve"),displays));
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "Alex",java.util.Collections.emptyList(),java.util.Collections.emptyMap()));
    }

    @Test void arbitraryAccountNamesAndChineseProfilesAreNeverRewritten(){
        java.util.List<String> online=java.util.Arrays.asList(
                "假的Alex","Bot_Alex","Alex_bots","测试员");
        java.util.Map<String,String> noDisplay=java.util.Collections.emptyMap();
        for(String name:online)
            assertEquals(name,GcaPlayerInventoryAdapter.verifiedAccountName(name,online,noDisplay));
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "假的测试员",online,noDisplay));
        assertEquals("",GcaPlayerInventoryAdapter.verifiedAccountName(
                "bot_测试员",online,noDisplay));
        assertTrue(GcaPlayerInventoryAdapter.isPlainPlayerName("Log_123"));
        assertTrue(GcaPlayerInventoryAdapter.isPlainPlayerName("中文角色"));
        assertFalse(GcaPlayerInventoryAdapter.isPlainPlayerName("中文 角色"));
        assertFalse(GcaPlayerInventoryAdapter.isPlainPlayerName("[Log head] 假的Log"));
    }

    @Test void gcaIdentityFallbackDoesNotInferProfilesFromTitles()throws IOException{
        String source=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        assertTrue(source.contains("verifiedAccountName(fallback,online,loadedDisplays)"));
        assertTrue(source.contains("if(name.isEmpty())return new Identity(null,\"\",PlayerInventoryTargetType.UNKNOWN)"));
        assertFalse(source.contains("removeKnownAffix("));
        assertFalse(source.contains("decoratedTargetName("));
        assertFalse(source.contains("structuredPlayerName("));
        assertFalse(source.contains("displayTargetName("));
        assertTrue(source.contains("GcaPlayerInteractionIntent.consume(containerId,title,kind)"));
    }

    @Test void remoteTargetDiscoveryDoesNotUseTitlesForFakeAuthorization()throws IOException{
        String adapter=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        assertTrue(adapter.contains("getOnlinePlayers()"));
        assertTrue(adapter.contains("matchingRenderedTarget(title,candidates)"));
        assertTrue(adapter.contains("PlayerIdentityResolver.resolve(uuid)"));
        String modern=adapter.substring(adapter.indexOf("private static String findTargetName("),
                adapter.indexOf("//#else",adapter.indexOf("private static String findTargetName(")));
        assertFalse(modern.contains("title.contains(name)"));
        assertFalse(modern.contains("title.contains(display)"));
        assertTrue(adapter.contains("//#if MC >= 12111"));
    }

    @Test void unknownIdentityKeepsOnlyServerValidatedOrdinaryActions()throws IOException{
        String adapter=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        assertTrue(adapter.contains("if(type==PlayerInventoryTargetType.REAL)return Collections.emptySet()"));
        assertTrue(adapter.contains("actions.addAll(CarpetPlayerCommandBridge.supportedActions())"));
        assertTrue(adapter.contains("if(type==PlayerInventoryTargetType.FAKE)"));
        assertTrue(adapter.contains("actions.add(PlayerInventoryAction.STOP_ATTACK)"));
        assertTrue(adapter.contains("actions.add(PlayerInventoryAction.STOP_USE)"));
        assertFalse(adapter.contains("for(PlayerInventoryAction action:PlayerInventoryAction.values())"));
        assertTrue(adapter.contains("actions.remove(PlayerInventoryAction.DROP_ALL)"));
        assertTrue(adapter.contains("actions.remove(PlayerInventoryAction.DISCONNECT)"));
        assertTrue(adapter.contains("actions.remove(PlayerInventoryAction.TOGGLE_SNEAK)"));
    }

    @Test void carpetCommandsNeverContainDecoratedGcaDisplayNames(){
        assertFalse(ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge
                .validTargetName("[Log head] 假的Log"));
        assertNull(ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge.commandFor(
                "[Log head] 假的Log",ddstweak.integration.dds.playerinventory.PlayerInventoryAction.SELECT_HOTBAR,1));
        assertEquals("player Log hotbar 1",
                ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandBridge.commandFor(
                        "Log",ddstweak.integration.dds.playerinventory.PlayerInventoryAction.SELECT_HOTBAR,1));
    }

    @Test void gcaMainhandHotbarAndOffhandKeepTheirUpstreamSlotIds()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("GCA_EQUIPMENT_MAPPING={1,2,3,4,7}"));
        assertTrue(menu.contains("GCA_HOTBAR_MAPPING=createMapping(45,9)"));
        assertTrue(menu.contains("new EquipmentClientSlot(equipment,4,77,62,false,shieldIcon())"));
        assertTrue(menu.contains("addSlot(new Slot(hotbar,i,8+i*18,142))"));
    }

    @Test void commandBridgeKeepsCarpetSyntaxWithoutUsingPermissionFilteredTree()throws IOException{
        String bridge=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandBridge.java");
        assertTrue(bridge.contains("attack interval "));
        assertTrue(bridge.contains("use interval "));
        assertTrue(bridge.contains("attack continuous"));
        assertTrue(bridge.contains("use continuous"));
        assertTrue(bridge.contains(" hotbar "));
        assertTrue(bridge.contains(" kill"));
        assertTrue(bridge.contains("PlayerInventoryBehaviorV1.clampInterval(value)"));
        assertTrue(bridge.contains("attack once"));
        assertTrue(bridge.contains("use once"));
        assertTrue(bridge.contains("path(targetNode(),\"attack\",\"stop\")"));
        assertTrue(bridge.contains("path(targetNode(),\"use\",\"stop\")"));
        int start=bridge.indexOf("public static boolean supports(PlayerInventoryAction action)");
        int end=bridge.indexOf("public static Set<PlayerInventoryAction> supportedActions()",start);
        assertTrue(start>=0&&end>start);
        String standard=bridge.substring(start,end);
        assertTrue(standard.contains("return true;"));
        assertFalse(standard.contains("path(target,\"attack\",\"interval\")"));
        assertFalse(standard.contains("path(target,\"use\",\"interval\")"));
    }

    @Test void dropAllUsesInventoryScopeWhenKnownAndVanillaAllOtherwise()throws IOException{
        String bridge=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandBridge.java");
        assertTrue(bridge.contains("child(drop,\"inventory\")!=null?\"inventory\":\"all\""));
        assertTrue(bridge.contains("\"player \"+target+\" dropStack \"+argument"));
        assertFalse(bridge.contains("dropAllArgument(target)!=null"));
    }

    @Test void unknownTargetsNeverFallBackToNativeControlSlots()throws IOException{
        String binding=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        assertTrue(binding.contains("if(type==PlayerInventoryTargetType.UNKNOWN)return CarpetPlayerCommandBridge.available()"));
        assertTrue(binding.contains("if(type==PlayerInventoryTargetType.UNKNOWN)return CarpetPlayerCommandBridge.supports(action)"));
        assertTrue(binding.contains("menu.targetType()==PlayerInventoryTargetType.FAKE"));
    }

    @Test void gcaRoutesExactNativeThenCommandWithoutDdsDependency()throws IOException{
        String binding=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        int nativeRoute=binding.indexOf("sendNative(slot)");
        int command=binding.indexOf("CarpetPlayerCommandBridge.send",nativeRoute);
        assertTrue(nativeRoute>=0&&command>nativeRoute);
        assertFalse(binding.contains("DdsFakePlayer"));
        assertTrue(binding.contains("case STOP_ATTACK:return on(5)?5:on(6)?6:-1"));
        assertTrue(binding.contains("case STOP_USE:return on(8)?8:-1"));
        assertTrue(binding.contains("if(action==PlayerInventoryAction.STOP_ATTACK)return on(5)||on(6)"));
        assertTrue(binding.contains("if(action==PlayerInventoryAction.STOP_USE)return on(8)"));
        assertFalse(binding.contains("state().attackMode()==PlayerInventoryBehaviorV1.MODE_INTERVAL?5"));
        assertFalse(binding.contains("state().useMode()==PlayerInventoryBehaviorV1.MODE_CONTINUOUS?8"));
        assertFalse(binding.contains("case TOGGLE_SNEAK->0"));
        assertFalse(binding.contains("case DISCONNECT->26"));
    }

    @Test void unsupportedGcaEnderBatchActionsAreNotAdvertised()throws IOException{
        String adapter=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        for(String action:new String[]{"ENDER_DROP_ALL","ENDER_STORE_ALL","ENDER_TAKE_ALL"})assertFalse(adapter.contains("PlayerInventoryAction."+action),action);
        assertFalse(adapter.contains("NATIVE_CONTROLLER_ACTIONS"));
        assertTrue(adapter.contains("CarpetPlayerCommandBridge.supportedActions()"));
    }

    @Test void replaceableControllerSlotsAreNeverParsedAsFixedActionState()throws IOException{
        String binding=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        assertTrue(binding.contains("if(isFixedInventory())for(int i=0;i<9;i++)if(on(9+i))return i"));
        assertTrue(binding.contains("isFixedInventory()&&(on(5)||on(6))"));
        assertTrue(binding.contains("isFixedInventory()&&on(8)"));
        assertFalse(binding.contains("isControllerLike()"));
    }

    @Test void customCommandIntervalDoesNotSnapBackToGcaFixedTwelveTickSlot()throws IOException{
        String binding=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        assertTrue(binding.contains("if(state().attackKnown())return state().attackInterval()"));
        assertTrue(binding.contains("state().attackKnown()?PlayerInventoryStateConfidence.LOCAL:isFixedInventory()&&on(5)"));
        assertTrue(binding.contains("if(state().attackKnown())return state().attackMode()"));
    }

    @Test void localCommandStateIsSharedAcrossScreenBindings()throws IOException{
        String binding=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryActionBinding.java");
        String state=read("ddstweak/playerinventory/PlayerInventoryClientActionState.java");
        assertTrue(binding.contains("PlayerInventoryClientActionState.forContext(menu.context())"));
        assertTrue(binding.contains("PlayerInventoryStateConfidence.LOCAL"));
        assertTrue(state.contains("Map<String,State> STATES"));
        assertTrue(state.contains("record(PlayerInventoryAction action,int value)"));
    }

    @Test void controllerScreenKeepsCustomSlotsButEnderShowsOnlyNineByThreeStorage()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("addGcaController"));
        assertTrue(menu.contains("addSlot(new Slot(target,i,8+c*18,84+r*18))"));
        assertTrue(menu.contains("for(int i=0;i<GCA_ENDER_STORAGE_START;i++)addSlot(hidden(target,i))"));
        assertTrue(menu.contains("addSlot(new Slot(storage,i,8+c*18,18+r*18))"));
        assertTrue(menu.contains("addViewer(viewer,85,143)"));
        assertTrue(menu.contains("visualRows(){return rows;}"));
        assertFalse(menu.contains("GCA_ENDER_CONTROL_MAPPING"));
    }

    @Test void gcaEnderPreservesServerSlotIdsWhileHidingControllerSlots()throws IOException{
        String menu=read("ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");
        assertTrue(menu.contains("GCA_CONTROL_MAPPING={0,5,6,8,9,10,11,12,13,14,15,16,17}"));
        assertTrue(menu.contains("GCA_MAIN_MAPPING=createMapping(18,27)"));
        assertTrue(menu.contains("GCA_HOTBAR_MAPPING=createMapping(45,9)"));
        assertTrue(menu.contains("GCA_ENDER_STORAGE_MAPPING=createMapping(27,27)"));
        assertTrue(menu.contains("new MappedContainer(target,GCA_CONTROL_MAPPING)"));
        assertTrue(menu.contains("new MappedContainer(target,GCA_ENDER_STORAGE_MAPPING)"));
        assertTrue(menu.contains("if(slot<GCA_ENDER_STORAGE_START)return PlayerInventorySlotRole.CONTROL"));
    }

    @Test void gcaRenderedFallbackDoesNotUseBroadVanillaContainerTitleMatching()throws IOException{
        String adapter=read("ddstweak/integration/gca/playerinventory/GcaPlayerInventoryAdapter.java");
        assertTrue(adapter.contains("isRenderedGcaEnder"));
        assertTrue(adapter.contains("isRenderedGcaController"));
        assertTrue(adapter.contains("isRenderedGcaInventory"));
        assertFalse(adapter.contains("lower.contains(\"ender chest\")"));
        assertFalse(adapter.contains("visible.contains(\"末影箱\")"));
        assertFalse(adapter.contains("lower.contains(\"inventory\")"));
    }

    private static String read(String path)throws IOException{return Files.readString(JAVA.resolve(path));}
}
