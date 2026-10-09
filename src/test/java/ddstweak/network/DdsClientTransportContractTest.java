package ddstweak.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class DdsClientTransportContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void genericClientNetworkDoesNotOwnWirePacketClasses() throws Exception {
        String source = Files.readString(ROOT.resolve("src/main/java/ddstweak/network/DdsClientNetwork.java"));
        assertTrue(source.contains("DdsClientTransports.current().send"));
        assertFalse(source.contains("ServerboundCustomPayloadPacket"));
        assertFalse(source.contains("DdsWirePayloadBridge"));
    }

    @Test
    void clientPayloadMixinDelegatesDecodeToTransport() throws Exception {
        String source = Files.readString(ROOT.resolve("src/main/java/ddstweak/mixin/network/ClientCustomPayloadMixin.java"));
        assertTrue(source.contains("DdsClientTransports.current().decode"));
        assertFalse(source.contains("DdsWirePayloadBridge.inbound"));
    }

    @Test
    void payloadMixinCancelsOnlyPacketsConsumedByTweakAcrossAllTransports() throws Exception {
        String source=Files.readString(ROOT.resolve("src/main/java/ddstweak/mixin/network/ClientCustomPayloadMixin.java"));
        String guarded="if(DdsClientNetwork.handlePayload(this,payload))ci.cancel();";
        assertEquals(3,count(source,guarded));
        assertEquals(3,count(source,"DdsClientNetwork.handlePayload(this,payload)"));
        assertEquals(3,count(source,"ci.cancel();"));
        assertFalse(source.contains("DdsClientNetwork.handlePayload(this,payload);\n        ci.cancel();"));
    }

    @Test
    void readerMapPayloadDispatchUsesTheOuterCommonPacketHandler() throws Exception {
        String source=Files.readString(ROOT.resolve("src/main/java/ddstweak/mixin/network/ClientCustomPayloadMixin.java"));
        assertTrue(source.contains("MC >= 12006"));
        assertTrue(source.contains("ClientCommonPacketListenerImpl"));
        assertTrue(source.contains("ClientboundCustomPayloadPacket"));
        assertTrue(source.contains("priority=900"));
        assertTrue(source.contains("packet.payload()"));
    }

    @Test
    void readerMapRegistrationIsKeptBehindThe1206WireBoundary() throws Exception {
        String protocol = Files.readString(ROOT.resolve("src/main/java/ddstweak/network/DdsProtocol.java"));
        String clientbound = Files.readString(ROOT.resolve("src/main/java/ddstweak/mixin/network/ClientboundCustomPayloadPacketMixin.java"));
        String serverbound = Files.readString(ROOT.resolve("src/main/java/ddstweak/mixin/network/ServerboundCustomPayloadPacketMixin.java"));
        assertTrue(protocol.contains("MC >= 12006"));
        assertTrue(protocol.contains("public void write(FriendlyByteBuf buf)"));
        assertTrue(protocol.contains("MC >= 12002"));
        assertTrue(protocol.contains("@Override public ResourceLocation id()"));
        for (String source : new String[]{clientbound, serverbound}) {
            assertTrue(source.contains("KNOWN_TYPES"));
            assertTrue(source.contains("DdsProtocol.Payload::new"));
            assertTrue(source.contains("MC >= 12006"));
        }
    }

    @Test
    void legacyTransportUsesDuplicateBufferAndRawPacketEnvelope() throws Exception {
        String source=Files.readString(ROOT.resolve("src/main/java/ddstweak/network/transport/LegacyCustomPayloadClientTransport.java"));
        assertTrue(source.contains("ServerboundCustomPayloadPacket(DdsProtocol.CHANNEL,buf)"));
        assertTrue(source.contains("packet.getData().duplicate()"));
        assertTrue(source.contains("packet.getIdentifier()"));
        String selector=Files.readString(ROOT.resolve("src/main/java/ddstweak/network/transport/DdsClientTransports.java"));
        assertTrue(selector.contains("MC >= 12002"));
        assertTrue(selector.contains("LegacyCustomPayloadClientTransport"));
    }

    @Test
    void payloadCodecTransportOwnsCurrentWireBridge() throws Exception {
        String source = Files.readString(ROOT.resolve("src/main/java/ddstweak/network/transport/PayloadCodecClientTransport.java"));
        assertTrue(source.contains("DdsWirePayloadBridge.outbound"));
        assertTrue(source.contains("DdsWirePayloadBridge.inbound"));
        assertTrue(source.contains("ServerboundCustomPayloadPacket"));
    }

    private static int count(String source,String token){
        int count=0,from=0;
        while((from=source.indexOf(token,from))>=0){count++;from+=token.length();}
        return count;
    }
}
