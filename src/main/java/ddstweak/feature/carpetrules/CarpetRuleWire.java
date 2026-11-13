package ddstweak.feature.carpetrules;

import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import net.fabricmc.loader.api.FabricLoader;
//#if MC >= 12002
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#else
//$$ import io.netty.buffer.Unpooled;
//$$ import net.minecraft.network.FriendlyByteBuf;
//$$ import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
//$$ import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
//#endif

/**
 * Read-only observation of a co-installed Carpet client; no compile-time dependency.
 * Legacy raw packets are decoded from duplicate buffers.
 */
public final class CarpetRuleWire {
    private static final String NATIVE_PAYLOAD = "carpet.network.CarpetClient$CarpetPayload";
    // CarpetClient.DATA in the legacy carpet:hello framing (before MC 1.20.2).
    private static final int LEGACY_DATA = 1;

    private CarpetRuleWire() {}

    public static CompoundTag inbound(Object raw) {
        if (raw instanceof CarpetRuleProtocol.Payload)
            return ((CarpetRuleProtocol.Payload) raw).data();
        //#if MC < 12002
        //$$ if (raw instanceof ClientboundCustomPayloadPacket) {
        //$$     ClientboundCustomPayloadPacket packet=(ClientboundCustomPayloadPacket)raw;
        //$$     if (!CarpetRuleProtocol.CHANNEL.equals(packet.getIdentifier())) return null;
        //$$     try {
        //$$         FriendlyByteBuf data=new FriendlyByteBuf(packet.getData().duplicate());
        //$$         if(data.readVarInt()!=LEGACY_DATA)return null;
        //$$         return data.readNbt();
        //$$     } catch (RuntimeException ignored) {
        //$$         return null; // malformed or handshake packet; never disconnect
        //$$     }
        //$$ }
        //#endif
        if (raw == null || !NATIVE_PAYLOAD.equals(raw.getClass().getName())) return null;
        try {
            Method data = raw.getClass().getMethod("data");
            Object result = data.invoke(raw);
            return result instanceof CompoundTag ? (CompoundTag)result : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null; // unknown Carpet version: don't interfere with the native handler
        }
    }

    public static boolean send(ClientPacketListener connection, CompoundTag envelope) {
        if (connection == null || envelope == null) return false;
        //#if MC >= 12002
        CustomPacketPayload payload;
        if(FabricLoader.getInstance().isModLoaded("carpet")){
            try {
                Class<?> clazz=Class.forName(NATIVE_PAYLOAD,false,CarpetRuleWire.class.getClassLoader());
                Constructor<?> ctor=clazz.getConstructor(CompoundTag.class);
                Object created=ctor.newInstance(envelope);
                if(!(created instanceof CustomPacketPayload))return false;
                payload=(CustomPacketPayload)created;
            } catch (ReflectiveOperationException | LinkageError ex) {
                return false;
            }
        } else payload=new CarpetRuleProtocol.Payload(envelope);
        connection.getConnection().send(new ServerboundCustomPayloadPacket(payload));
        //#else
        //$$ FriendlyByteBuf buf=new FriendlyByteBuf(Unpooled.buffer());
        //$$ buf.writeVarInt(LEGACY_DATA);
        //$$ buf.writeNbt(envelope);
        //$$ connection.getConnection().send(new ServerboundCustomPayloadPacket(CarpetRuleProtocol.CHANNEL,buf));
        //#endif
        return true;
    }

    public static boolean hello(ClientPacketListener connection) {
        if (connection == null) return false;
        CompoundTag envelope = new CompoundTag();
        envelope.putString("420", "DDS-Tweak rule client");
        return send(connection,envelope);
    }
}
