package ddstweak.feature.carpetrules;

import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
//#if MC >= 12002
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#endif
//#if MC >= 12006
import net.minecraft.network.codec.StreamCodec;
//#endif
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif

/**
 * Carpet's unmodified carpet:hello envelope: one NBT compound, no DDS wire fields.
 * Registered only when no native Carpet client provides this payload type.
 */
public final class CarpetRuleProtocol {
    //#if MC >= 12111
    public static final Identifier CHANNEL = MinecraftCompat.id("carpet", "hello");
    //#else
    //$$ public static final ResourceLocation CHANNEL = MinecraftCompat.id("carpet", "hello");
    //#endif

    private CarpetRuleProtocol() {}

    //#if MC >= 12002
    public static final class Payload implements CustomPacketPayload {
    //#else
    //$$ public static final class Payload {
    //#endif
        //#if MC >= 12006
        public static final Type<Payload> TYPE = new Type<>(CHANNEL);
        public static final StreamCodec<FriendlyByteBuf,Payload> CODEC =
                CustomPacketPayload.codec(Payload::write, Payload::new);
        //#endif
        private final CompoundTag data;

        public Payload(CompoundTag data) { this.data = data == null ? new CompoundTag() : data; }
        public Payload(FriendlyByteBuf buf) {
            CompoundTag decoded = buf.readNbt();
            data = decoded == null ? new CompoundTag() : decoded;
        }

        //#if MC >= 12006
        private void write(FriendlyByteBuf buf) { buf.writeNbt(data); }
        //#else
        //$$ public void write(FriendlyByteBuf buf) { buf.writeNbt(data); }
        //#endif

        public CompoundTag data() { return data; }

        //#if MC >= 12006
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        //#elseif MC >= 12002
        //$$ @Override public ResourceLocation id() { return CHANNEL; }
        //#endif
    }
}
