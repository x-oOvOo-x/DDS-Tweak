package ddstweak.network.transport;

import ddstweak.network.DdsProtocol;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;

/**
 * Version-specific DDS client wire transport.
 *
 * Feature code must use DdsClientNetwork rather than depending on concrete
 * Minecraft custom-payload packet classes.
 */
public interface DdsClientTransport {
    boolean send(ClientPacketListener connection, String packetId, CompoundTag data);

    /** Decodes either a modern CustomPacketPayload or a legacy packet envelope. */
    DdsProtocol.Payload decode(Object wirePayload);
}
