package ddstweak.mixin.network;

import ddstweak.network.DdsClientNetwork;
import ddstweak.network.DdsProtocol;
import ddstweak.network.transport.DdsClientTransports;
import net.minecraft.client.multiplayer.ClientPacketListener;
//#if MC >= 12006
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#else
//#if MC >= 12002
//$$ import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
//$$ import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
//#else
//$$ import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
//#endif
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles DDS protocol payloads without taking ownership of the provider/menu layer.
 * Legacy raw packets are decoded from a duplicate buffer so a co-installed old DDS
 * client can still consume the original packet.
 */
//#if MC >= 12006
@Mixin(ClientPacketListener.class)
//#else
//#if MC >= 12002
//$$ @Mixin(value=ClientCommonPacketListenerImpl.class,priority=900)
//#else
//$$ @Mixin(value=ClientPacketListener.class,priority=900)
//#endif
//#endif
public abstract class ClientCustomPayloadMixin {
    //#if MC >= 12006
    @Inject(method="handleCustomPayload(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$handleCustomPayload(CustomPacketPayload raw,CallbackInfo ci){
        DdsProtocol.Payload payload=DdsClientTransports.current().decode(raw);
        if(payload==null)return;
        if(DdsClientNetwork.handlePayload(this,payload))ci.cancel();
    }
    //#elseif MC >= 12002
    //$$ @Inject(method="handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V",at=@At("HEAD"),cancellable=true)
    //$$ private void ddsTweak$handleCustomPayload(ClientboundCustomPayloadPacket packet,CallbackInfo ci){
    //$$     DdsProtocol.Payload payload=DdsClientTransports.current().decode(packet.payload());
    //$$     if(payload==null)return;
    //$$     if(DdsClientNetwork.handlePayload(this,payload))ci.cancel();
    //$$ }
    //#else
    //$$ @Inject(method="handleCustomPayload(Lnet/minecraft/network/protocol/game/ClientboundCustomPayloadPacket;)V",at=@At("HEAD"),cancellable=true)
    //$$ private void ddsTweak$handleCustomPayload(ClientboundCustomPayloadPacket packet,CallbackInfo ci){
    //$$     DdsProtocol.Payload payload=DdsClientTransports.current().decode(packet);
    //$$     if(payload==null)return;
    //$$     if(DdsClientNetwork.handlePayload(this,payload))ci.cancel();
    //$$ }
    //#endif
}
