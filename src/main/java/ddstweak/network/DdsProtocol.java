package ddstweak.network;

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
import java.util.*;

/** DDS Protocol v1 generic wire envelope shared with Carpet DDS Addition. */
public final class DdsProtocol {
    public static final int VERSION=1;
    public static final String HELLO_C2S="hello_c2s",HELLO_S2C="hello_s2c";
    //#if MC >= 12111
    public static final Identifier CHANNEL=MinecraftCompat.id("carpet-dds-addition","network/v1");
    //#else
    //$$ public static final ResourceLocation CHANNEL=MinecraftCompat.id("carpet-dds-addition","network/v1");
    //#endif
    private DdsProtocol(){}
    public static String encodeIds(Collection<String> ids){return String.join(",",ids);}
    public static Set<String> decodeKnownIds(String encoded,Collection<String> known){if(encoded==null||encoded.trim().isEmpty()||known==null||known.isEmpty())return Collections.emptySet();Set<String> out=new LinkedHashSet<>();for(String part:encoded.split(",")){String id=part.trim();if(known.contains(id))out.add(id);}return out.isEmpty()?Collections.emptySet():Collections.unmodifiableSet(out);}

    //#if MC >= 12002
    public static final class Payload implements CustomPacketPayload{
    //#else
    //$$ public static final class Payload{
    //#endif
        //#if MC >= 12006
        public static final Type<Payload> TYPE=new Type<>(CHANNEL);
        public static final StreamCodec<FriendlyByteBuf,Payload> CODEC=CustomPacketPayload.codec(Payload::write,Payload::new);
        //#endif
        private final String packetId;private final CompoundTag data;
        public Payload(String packetId,CompoundTag data){this.packetId=packetId;this.data=data==null?new CompoundTag():data;}
        public Payload(FriendlyByteBuf buf){packetId=buf.readUtf();CompoundTag read=buf.readNbt();data=read==null?new CompoundTag():read;}
        //#if MC >= 12006
        private void write(FriendlyByteBuf buf){buf.writeUtf(packetId);buf.writeNbt(data);}
        //#else
        //#if MC >= 12002
        //$$ @Override
        //#endif
        //$$ public void write(FriendlyByteBuf buf){buf.writeUtf(packetId);buf.writeNbt(data);}
        //#endif
        public String packetId(){return packetId;}
        public CompoundTag data(){return data;}
        //#if MC >= 12006
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
        //#elseif MC >= 12002
        //$$ @Override public ResourceLocation id(){return CHANNEL;}
        //#endif
    }
}
