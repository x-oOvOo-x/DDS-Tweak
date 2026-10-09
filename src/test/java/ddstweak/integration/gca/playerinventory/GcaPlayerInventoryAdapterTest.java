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

    @Test void commandBridgeSupportsArbitraryCarpetIntervalsAndStandardControls()throws IOException{
        String bridge=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandBridge.java");
        assertTrue(bridge.contains("attack interval "));
        assertTrue(bridge.contains("use interval "));
        assertTrue(bridge.contains("attack continuous"));
        assertTrue(bridge.contains("use continuous"));
        assertTrue(bridge.contains(" hotbar "));
        assertTrue(bridge.contains("child(drop,\"all\")"));
        assertTrue(bridge.contains(" kill"));
        assertTrue(bridge.contains("PlayerInventoryBehaviorV1.clampInterval(value)"));
        assertTrue(bridge.contains("path(target,\"attack\",\"interval\")"));
        assertTrue(bridge.contains("path(target,\"use\",\"interval\")"));
        assertTrue(bridge.contains("path(target,\"attack\",\"stop\")"));
        assertTrue(bridge.contains("path(target,\"use\",\"stop\")"));
        assertTrue(bridge.contains("path(target,\"attack\",\"once\")"));
        assertTrue(bridge.contains("path(target,\"use\",\"once\")"));
        assertTrue(bridge.contains("attack once"));
        assertTrue(bridge.contains("use once"));
        assertTrue(bridge.contains("child(target,\"sneak\")"));
        assertTrue(bridge.contains("child(target,\"kill\")"));
    }

    @Test void dropAllPrefersExplicitInventoryScopeWhenProviderAddsIt()throws IOException{
        String bridge=read("ddstweak/integration/carpet/playerinventory/CarpetPlayerCommandBridge.java");
        assertTrue(bridge.contains("dropAllArgument(target)!=null"));
        assertTrue(bridge.contains("child(drop,\"inventory\")"));
        assertTrue(bridge.contains("\"player \"+target+\" dropStack \"+argument"));
        assertFalse(bridge.contains("case DROP_ALL:return \"player \"+target+\" dropStack all\""));
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
