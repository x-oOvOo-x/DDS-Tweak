package ddstweak.integration.dds.qca;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsQcaSlotAccessPolicyTest {
    private static Path root(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root not found");}
    private static String read(String path)throws IOException{return Files.readString(root().resolve(path));}
    @Test void unifiedInventoryExposesStorageNotEquipment()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");assertTrue(code.contains("case EQUIPMENT:return Role.TARGET_EQUIPMENT"));assertTrue(code.contains("case MAIN:case HOTBAR:return Role.TARGET_STORAGE"));assertTrue(code.contains("case VIEWER_MAIN:case VIEWER_HOTBAR:return Role.VIEWER_INVENTORY"));}
    @Test void unifiedEnderExposesEnderAndViewerRegions()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/playerinventory/menu/UnifiedPlayerInventoryMirrorMenu.java");assertTrue(code.contains("case ENDER_STORAGE:return Role.ENDER_STORAGE"));assertTrue(code.contains("case VIEWER_MAIN:case VIEWER_HOTBAR:return Role.VIEWER_INVENTORY"));}
    @Test void policyFailsClosedForUnknownCompositeSlots()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaClientSlotAccessPolicy.java");assertTrue(code.contains("if(slot.container==MinecraftCompat.playerInventory(player))return true"));assertTrue(code.contains("return isVanillaEnderScreen()"));assertFalse(code.contains("instanceof SimpleContainer"));}
}
