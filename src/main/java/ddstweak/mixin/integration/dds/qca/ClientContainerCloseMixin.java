package ddstweak.mixin.integration.dds.qca;

import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientContainerCloseMixin {
    @Inject(method="handleContainerClose",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$suppressQcaTransitionClose(ClientboundContainerClosePacket packet,CallbackInfo ci){int id=((ClientboundContainerClosePacketAccessor)(Object)packet).ddsTweak$getContainerId();if(DdsQcaClientNetwork.shouldSuppressTransitionClose(id))ci.cancel();}
}
