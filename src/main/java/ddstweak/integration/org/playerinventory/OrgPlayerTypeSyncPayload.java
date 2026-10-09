package ddstweak.integration.org.playerinventory;

//#if MC >= 260100
//$$ import ddstweak.platform.minecraft.MinecraftCompat;
//$$ import net.minecraft.network.RegistryFriendlyByteBuf;
//$$ import net.minecraft.network.codec.StreamCodec;
//$$ import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//$$ import java.util.UUID;

//$$ /** Wire-compatible with Org's player_type_sync; used only when the Org client is absent. */
//$$ public record OrgPlayerTypeSyncPayload(UUID uuid,boolean fake) implements CustomPacketPayload {
//$$     public static final Type<OrgPlayerTypeSyncPayload> TYPE=new Type<>(MinecraftCompat.id("carpet-org-addition","player_type_sync"));
//$$     public static final StreamCodec<RegistryFriendlyByteBuf,OrgPlayerTypeSyncPayload> CODEC=new StreamCodec<>(){
//$$         @Override public void encode(RegistryFriendlyByteBuf out,OrgPlayerTypeSyncPayload value){out.writeUUID(value.uuid());out.writeBoolean(value.fake());}
//$$         @Override public OrgPlayerTypeSyncPayload decode(RegistryFriendlyByteBuf in){return new OrgPlayerTypeSyncPayload(in.readUUID(),in.readBoolean());}
//$$     };
//$$     @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
//$$ }
//#endif
