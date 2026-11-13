package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleWireContractTest {
    private static String wire() throws Exception {
        return Files.readString(Path.of(
                "src/main/java/ddstweak/feature/carpetrules/CarpetRuleWire.java"))
                .replace("\r\n","\n");
    }

    @Test void oldCarpetFramesDataWithVarIntBeforeNbt() throws Exception {
        String source=wire();
        assertTrue(source.contains("private static final int LEGACY_DATA = 1;"));
        int inbound=source.indexOf("public static CompoundTag inbound(Object raw)");
        int outbound=source.indexOf("public static boolean send(",inbound);
        assertTrue(inbound>=0&&outbound>inbound);
        String receive=source.substring(inbound,outbound);
        assertTrue(receive.contains("//#if MC < 12002"));
        assertTrue(receive.contains("CarpetRuleProtocol.CHANNEL.equals(packet.getIdentifier())"));
        assertTrue(receive.contains("new FriendlyByteBuf(packet.getData().duplicate())"),
                "Observe the packet through an independent reader index");
        assertTrue(receive.contains("if(data.readVarInt()!=LEGACY_DATA)return null;"),
                "CarpetClient.HI=69 is a handshake and must not be read as NBT");
        assertTrue(receive.indexOf("data.readVarInt()")<receive.indexOf("return data.readNbt()"));
        assertTrue(receive.contains("catch (RuntimeException ignored)"),
                "Malformed packets must not disconnect the player");
        assertFalse(receive.contains("packet.getData().duplicate()).readNbt()"),
                "Never interpret the legacy packet header as an NBT tag");
    }

    @Test void oldCarpetCommandsUseTheSameDataFrame() throws Exception {
        String source=wire();
        int start=source.indexOf("public static boolean send(");
        int end=source.indexOf("public static boolean hello(",start);
        assertTrue(start>=0&&end>start);
        String send=source.substring(start,end);
        assertTrue(send.contains("//#if MC >= 12002"));
        int legacy=send.indexOf("new FriendlyByteBuf(Unpooled.buffer())");
        int header=send.indexOf("buf.writeVarInt(LEGACY_DATA);",legacy);
        int data=send.indexOf("buf.writeNbt(envelope);",legacy);
        int packet=send.indexOf("new ServerboundCustomPayloadPacket(CarpetRuleProtocol.CHANNEL,buf)",legacy);
        assertTrue(legacy>=0&&header>legacy&&data>header&&packet>data,
                "CarpetClient.DATA=1 must precede NBT for 1.20.1 and older");
    }
}
