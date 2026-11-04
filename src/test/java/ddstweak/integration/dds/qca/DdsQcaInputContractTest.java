package ddstweak.integration.dds.qca;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsQcaInputContractTest {
    private static Path root(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root not found");}
    private static String read(String p)throws IOException{return Files.readString(root().resolve(p));}
    @Test void firstClickConsumesOnlyAfterSuccessfulDedicatedSend()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");assertTrue(c.contains("if(id>=0&&DdsQcaClientNetwork.sendOpenSlot(menu,id)){"));assertTrue(c.contains("PlayerInventoryOpenContext.expectEnder(((UnifiedPlayerInventoryMirrorMenu)menu).context())"));assertTrue(c.contains("sendStorageClick(menu.containerId,id))return false"));assertTrue(c.contains("if(!ddsTweak$isRightMouseButton(button))return false"));}
    @Test void sdl263MouseEventsUseRightConstantButLegacyKeepsGlfwRight()throws IOException{
        String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");
        assertTrue(c.contains("//#if MC >= 260300"));
        assertTrue(c.contains("//$$ return button == InputConstants.MOUSE_BUTTON_RIGHT;"));
        int modern = c.indexOf("//#if MC >= 260300");
        int modernRight = c.indexOf("InputConstants.MOUSE_BUTTON_RIGHT;",modern);
        int legacy = c.indexOf("//#else",modernRight);
        int legacyRight = c.indexOf("return button == 1;",legacy);
        int end = c.indexOf("//#endif",legacyRight);
        assertTrue(modern >= 0 && modernRight > modern && legacy > modernRight
                && legacyRight > legacy && end > legacyRight);
        assertTrue(c.contains("if(!ddsTweak$isRightMouseButton(button))return false"));
        assertEquals(2,c.split(java.util.regex.Pattern.quote("if(!ddsTweak$storageRightDown||!ddsTweak$isRightMouseButton(button))return false;"),-1).length-1);
        assertFalse(c.contains("if(button==1)ddsTweak$resetGesture()"));
    }
    @Test void capabilityLossResetsActiveDrag()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");assertTrue(c.contains("!DdsQcaClientNetwork.canUseStorageClick()){ddsTweak$resetGesture();return false;"));assertTrue(c.contains("!DdsQcaClientNetwork.canUseCreativeStorage()"));}
    @Test void escapeAndCloseCancelOnlyWhenNavigateSendSucceeds()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");assertTrue(c.contains("sendNavigateBack(menu))ci.cancel()"));assertTrue(c.contains("InputConstants.KEY_ESCAPE&&DdsQcaClientNetwork.sendNavigateBack(menu)"));assertTrue(c.contains("if(key==256&&DdsQcaClientNetwork.sendNavigateBack(menu))"));}
    @Test void legacyInputSignaturesUseActivePreprocessMarkers()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");for(String s:new String[]{"//$$ @Inject(method=\"keyPressed\"","//$$ @Inject(method=\"mouseClicked\"","//$$ @Inject(method=\"mouseDragged\"","//$$ @Inject(method=\"mouseReleased\""})assertTrue(c.contains(s),s);assertFalse(c.contains("\n    //$ @Inject(method=\"mouseDragged\""));}
    @Test void legacyCreativeOverridesShareGestureState()throws IOException{
        String bridge=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaMouseInputBridge.java");
        String parent=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");
        String legacy=read("src/main/java/ddstweak/mixin/integration/dds/qca/LegacyCreativeScreenInputMixin.java");
        String config=read("src/main/resources/dds-tweak.mixins.json");
        String plugin=read("src/main/java/ddstweak/mixin/DdsTweakMixinConfigPlugin.java");
        assertTrue(bridge.contains("package ddstweak.integration.dds.qca;"));
        assertTrue(parent.contains("import ddstweak.integration.dds.qca.DdsQcaMouseInputBridge;"));
        assertTrue(legacy.contains("import ddstweak.integration.dds.qca.DdsQcaMouseInputBridge;"));
        assertTrue(parent.contains("implements DdsQcaMouseInputBridge"));
        assertFalse(Files.exists(root().resolve("src/main/java/ddstweak/mixin/integration/dds/qca/DdsQcaMouseInputBridge.java")));
        for(String method:new String[]{"ddstweak$qcaMouseClicked","ddstweak$qcaMouseDragged","ddstweak$qcaMouseReleased"}){
            assertTrue(bridge.contains(method));assertTrue(parent.contains(method));assertTrue(legacy.contains(method));
        }
        assertTrue(legacy.contains("//#if MC < 11700"));
        for(String target:new String[]{"mouseClicked","mouseDragged","mouseReleased"})assertTrue(legacy.contains("//$$ @Inject(method=\""+target+"\""));
        assertTrue(config.contains("\"integration.dds.qca.LegacyCreativeScreenInputMixin\""));
        assertTrue(plugin.contains("LegacyCreativeScreenInputMixin"));
    }
    @Test void predictionUsesDedicatedEligibility()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ClientPredictionMixin.java");assertTrue(c.contains("DdsQcaClient.shouldSuppressPrediction"));assertTrue(c.contains("menu.clicked"));}
    @Test void legacyPredictionUsesReturnableClickThrough11605()throws IOException{
        String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ClientPredictionMixin.java");
        assertTrue(c.contains("//#elseif MC < 11700"));
        assertTrue(c.contains("clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;"));
        assertTrue(c.contains("//$$ private ItemStack ddsTweak$suppressQcaPrediction("));
        assertTrue(c.contains("return ItemStack.EMPTY;"));
        assertTrue(c.contains("return menu.clicked(slotNum,buttonNum,clickType,player);"));
        assertTrue(c.contains("clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"));
    }
}
