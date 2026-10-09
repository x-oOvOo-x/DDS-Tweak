package ddstweak.network.transport;

import ddstweak.network.DdsProtocol;
import ddstweak.network.DdsWirePayloadBridge;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
//#if MC >= 12002
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#endif

/** Object-payload transport shared by DDS_PAYLOAD_CODEC and DDS_PAYLOAD_READER_MAP targets. */
public final class PayloadCodecClientTransport implements DdsClientTransport {
    @Override
    public boolean send(ClientPacketListener connection, String packetId, CompoundTag data) {
        //#if MC >= 12002
        if (connection == null) return false;
        Object wirePayload = DdsWirePayloadBridge.outbound(packetId, data);
        if (!(wirePayload instanceof CustomPacketPayload)) return false;
        connection.getConnection().send(new ServerboundCustomPayloadPacket((CustomPacketPayload) wirePayload));
        return true;
        //#else
        //$$ return false;
        //#endif
    }

    @Override
    public DdsProtocol.Payload decode(Object wirePayload) {
        //#if MC >= 12002
        return DdsWirePayloadBridge.inbound(wirePayload);
        //#else
        //$$ return null;
        //#endif
    }
}
