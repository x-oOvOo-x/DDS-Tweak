package ddstweak.integration.dds.qca;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsQcaInputContractTest {
    private static Path root(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root not found");}
    private static String read(String p)throws IOException{return Files.readString(root().resolve(p));}
    @Test void firstClickConsumesOnlyAfterSuccessfulDedicatedSend()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");assertTrue(c.contains("if(id>=0&&DdsQcaClientNetwork.sendOpenSlot(menu,id)){"));assertTrue(c.contains("PlayerInventoryOpenContext.expectEnder(((UnifiedPlayerInventoryMirrorMenu)menu).context())"));assertTrue(c.contains("sendStorageClick(menu.containerId,id))return false"));assertTrue(c.contains("if(button!=1)return false"));}
    @Test void capabilityLossResetsActiveDrag()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");assertTrue(c.contains("!DdsQcaClientNetwork.canUseStorageClick()){ddsTweak$resetGesture();return false;"));assertTrue(c.contains("!DdsQcaClientNetwork.canUseCreativeStorage()"));}
    @Test void escapeAndCloseCancelOnlyWhenNavigateSendSucceeds()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");assertTrue(c.contains("sendNavigateBack(menu))ci.cancel()"));assertTrue(c.contains("InputConstants.KEY_ESCAPE&&DdsQcaClientNetwork.sendNavigateBack(menu)"));assertTrue(c.contains("if(key==256&&DdsQcaClientNetwork.sendNavigateBack(menu))"));}
    @Test void legacyInputSignaturesUseActivePreprocessMarkers()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ContainerScreenInputMixin.java");for(String s:new String[]{"//$$ @Inject(method=\"keyPressed\"","//$$ @Inject(method=\"mouseClicked\"","//$$ @Inject(method=\"mouseDragged\"","//$$ @Inject(method=\"mouseReleased\""})assertTrue(c.contains(s),s);assertFalse(c.contains("\n    //$ @Inject(method=\"mouseDragged\""));}
    @Test void predictionUsesDedicatedEligibility()throws IOException{String c=read("src/main/java/ddstweak/mixin/integration/dds/qca/ClientPredictionMixin.java");assertTrue(c.contains("DdsQcaClient.shouldSuppressPrediction"));assertTrue(c.contains("menu.clicked"));}
}
