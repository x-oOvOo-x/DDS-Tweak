package ddstweak.network;

import ddstweak.integration.dds.fakeplayer.DdsFakePlayerPackets;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

final class DdsProtocolContractTest {
    @Test void keepsProtocolV1Channel(){assertEquals(1,DdsProtocol.VERSION);assertEquals("carpet-dds-addition:network/v1",DdsProtocol.CHANNEL.toString());}
    @Test void keepsFeatureIdsOutOfGenericProtocol(){assertFalse(hasField("FAKE_PLAYER_ACTION_C2S"));assertFalse(hasField("FAKE_PLAYER_OPEN_MENU_S2C"));assertEquals("fake_player_action_c2s",DdsFakePlayerPackets.ACTION_C2S);assertEquals("fake_player_open_menu_s2c",DdsFakePlayerPackets.OPEN_MENU_S2C);}
    @Test void legacySortPacketStaysRemoved(){assertFalse(hasField("FAKE_PLAYER_SORT_C2S"));}
    @Test void legacyCoexistenceReassertsMergedHelloBeforeFeatureSend() throws Exception {String source=java.nio.file.Files.readString(java.nio.file.Path.of("").toAbsolutePath().resolve("src/main/java/ddstweak/network/DdsClientNetwork.java"));assertTrue(source.contains("if(DdsLocalOwnershipResolver.current()==DdsLocalOwnershipDecision.LEGACY_COMPAT)sendHello();return sendRaw(id,data);"));}
    private static boolean hasField(String name){return Arrays.stream(DdsProtocol.class.getDeclaredFields()).map(Field::getName).anyMatch(name::equals);}
}
