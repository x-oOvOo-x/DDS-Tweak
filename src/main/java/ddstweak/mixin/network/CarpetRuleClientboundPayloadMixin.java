package ddstweak.mixin.network;

import ddstweak.feature.carpetrules.CarpetRuleProtocol;
//#if MC >= 12002
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
//#if MC >= 12006
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.ArrayList;
import java.util.List;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mutable;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import java.util.LinkedHashMap;
//$$ import java.util.Map;
//#endif
//#else
//$$ import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
//$$ import org.spongepowered.asm.mixin.Mixin;
//#endif

@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class CarpetRuleClientboundPayloadMixin {
    //#if MC >= 12006
    @ModifyArg(method="<clinit>",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;"))
    private static List<?> ddsTweak$registerCarpet(List<CustomPacketPayload.TypeAndCodec<?, ?>> codecs) {
        List<CustomPacketPayload.TypeAndCodec<?, ?>> copy = new ArrayList<>(codecs);
        copy.add(new CustomPacketPayload.TypeAndCodec<>(CarpetRuleProtocol.Payload.TYPE,CarpetRuleProtocol.Payload.CODEC));
        return copy;
    }
    //#elseif MC >= 12002
    //$$ @Mutable @Shadow @Final
    //$$ private static Map<ResourceLocation,FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES;
    //$$ @Inject(method="<clinit>",at=@At("RETURN"))
    //$$ private static void ddsTweak$registerCarpet(CallbackInfo ci) {
    //$$     Map<ResourceLocation,FriendlyByteBuf.Reader<? extends CustomPacketPayload>> copy = new LinkedHashMap<>(KNOWN_TYPES);
    //$$     copy.putIfAbsent(CarpetRuleProtocol.CHANNEL,CarpetRuleProtocol.Payload::new);
    //$$     KNOWN_TYPES = java.util.Collections.unmodifiableMap(copy);
    //$$ }
    //#endif
}
