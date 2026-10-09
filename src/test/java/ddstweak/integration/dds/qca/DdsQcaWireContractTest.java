package ddstweak.integration.dds.qca;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

final class DdsQcaWireContractTest {
    private static Path root(){for(Path p=Path.of("").toAbsolutePath();p!=null;p=p.getParent())if(Files.isDirectory(p.resolve("src/main")))return p;throw new IllegalStateException("repo root not found");}
    private static String read(String path)throws IOException{return Files.readString(root().resolve(path));}
    @Test void packetIdsStayStable(){assertEquals("qca_open_slot_c2s",DdsQcaPackets.OPEN_SLOT_C2S);assertEquals("qca_open_inventory_slot_c2s",DdsQcaPackets.OPEN_INVENTORY_SLOT_C2S);assertEquals("qca_storage_click_c2s",DdsQcaPackets.STORAGE_CLICK_C2S);assertEquals("qca_creative_storage_c2s",DdsQcaPackets.CREATIVE_STORAGE_C2S);assertEquals("qca_navigate_back_c2s",DdsQcaPackets.NAVIGATE_BACK_C2S);assertEquals("qca_creative_storage_result_s2c",DdsQcaPackets.CREATIVE_STORAGE_RESULT_S2C);assertEquals("qca_navigation_state_s2c",DdsQcaPackets.NAVIGATION_STATE_S2C);}
    @Test void stackCodecUsesFullComponentCodec()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaStackCodec.java");assertTrue(code.contains("ItemStack.CODEC.encodeStart"));assertTrue(code.contains("ItemStack.CODEC.parse"));assertTrue(code.contains("RegistryOps.create(NbtOps.INSTANCE"));assertTrue(code.contains("key+\"_empty\""));}
    @Test void itemClassifierCoversStorageAndWorkstations()throws IOException{String code=read("src/main/java/ddstweak/integration/dds/qca/DdsQcaItems.java");for(String token:new String[]{"ShulkerBoxBlock","Items.ENDER_CHEST","Items.CRAFTING_TABLE","Items.STONECUTTER","ItemTags.ANVIL","Items.SMITHING_TABLE"})assertTrue(code.contains(token),token);}
}
