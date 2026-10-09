package ddstweak.network.transport;

import ddstweak.network.DdsProtocol;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
//#if MC < 12002
//$$ import io.netty.buffer.Unpooled;
//$$ import net.minecraft.network.FriendlyByteBuf;
//$$ import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
//$$ import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
//#endif

/**
 * Legacy ResourceLocation + FriendlyByteBuf DDS transport used through 1.20.1.
 *
 * Decode uses a duplicate buffer so Tweak can observe a packet without moving
 * the original reader index when a legacy DDS client is co-installed.
 */
public final class LegacyCustomPayloadClientTransport implements DdsClientTransport {
    @Override
    public boolean send(ClientPacketListener connection,String packetId,CompoundTag data){
        //#if MC < 12002
        //$$ if(connection==null)return false;
        //$$ FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.buffer());
        //$$ new DdsProtocol.Payload(packetId,data).write(buf);
        //$$ connection.getConnection().send(new ServerboundCustomPayloadPacket(DdsProtocol.CHANNEL,buf));
        //$$ return true;
        //#else
        return false;
        //#endif
    }

    @Override
    public DdsProtocol.Payload decode(Object wirePayload){
        //#if MC < 12002
        //$$ if(!(wirePayload instanceof ClientboundCustomPayloadPacket))return null;
        //$$ ClientboundCustomPayloadPacket packet=(ClientboundCustomPayloadPacket)wirePayload;
        //$$ if(!DdsProtocol.CHANNEL.equals(packet.getIdentifier()))return null;
        //$$ FriendlyByteBuf copy=new FriendlyByteBuf(packet.getData().duplicate());
        //$$ return new DdsProtocol.Payload(copy);
        //#else
        return null;
        //#endif
    }
}
