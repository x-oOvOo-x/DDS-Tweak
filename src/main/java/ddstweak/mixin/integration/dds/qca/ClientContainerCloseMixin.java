package ddstweak.mixin.integration.dds.qca;

import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
/**
 * Suppresses QCA transition-close packets only for matching menu IDs.
 * 仅对匹配菜单 ID 的 QCA 切换关闭包进行拦截。
 */
public abstract class ClientContainerCloseMixin {
    @Inject(method="handleContainerClose",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$suppressQcaTransitionClose(ClientboundContainerClosePacket packet,CallbackInfo ci){int id=((ClientboundContainerClosePacketAccessor)(Object)packet).ddsTweak$getContainerId();if(DdsQcaClientNetwork.shouldSuppressTransitionClose(id))ci.cancel();}
}
