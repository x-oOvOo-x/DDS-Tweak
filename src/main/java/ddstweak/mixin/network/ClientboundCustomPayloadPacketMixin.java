package ddstweak.mixin.network;

//#if MC >= 12002
import ddstweak.network.DdsProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC < 12006
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import org.spongepowered.asm.mixin.injection.At;
//#else
//$$ import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
//#endif
import org.spongepowered.asm.mixin.Mixin;
//#if MC >= 12006
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//#else
//#if MC >= 12002
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mutable;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import java.util.Collections;
//$$ import java.util.LinkedHashMap;
//$$ import java.util.Map;
//#endif
//#endif

@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class ClientboundCustomPayloadPacketMixin {
    //#if MC >= 12006
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;"))
    private static List<?> ddsTweak$registerCodec(List<CustomPacketPayload.TypeAndCodec<?, ?>> codecs) {
        List<CustomPacketPayload.TypeAndCodec<?, ?>> copy = new ArrayList<>(codecs);
        copy.add(new CustomPacketPayload.TypeAndCodec<>(DdsProtocol.Payload.TYPE, DdsProtocol.Payload.CODEC));
        return Collections.unmodifiableList(copy);
    }
    //#elseif MC >= 12002
    //$$ @Mutable @Shadow @Final
    //$$ private static Map<ResourceLocation,FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES;
    //$$
    //$$ @Inject(method="<clinit>",at=@At("RETURN"))
    //$$ private static void ddsTweak$registerReader(CallbackInfo ci){
    //$$     Map<ResourceLocation,FriendlyByteBuf.Reader<? extends CustomPacketPayload>> copy=new LinkedHashMap<>(KNOWN_TYPES);
    //$$     copy.put(DdsProtocol.CHANNEL,DdsProtocol.Payload::new);
    //$$     KNOWN_TYPES=Collections.unmodifiableMap(copy);
    //$$ }
    //#endif
}
