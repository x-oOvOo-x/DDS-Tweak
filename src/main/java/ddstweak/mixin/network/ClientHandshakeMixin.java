package ddstweak.mixin.network;

import ddstweak.network.DdsClientNetwork;
import ddstweak.feature.carpetrules.CarpetRuleClientNetwork;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Starts DDS client handshake after the vanilla login handler returns.
 * 原版登录处理完成后启动 DDS 客户端握手。
 */

@Mixin(value=ClientPacketListener.class,priority=500)
public abstract class ClientHandshakeMixin {
    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void ddsTweak$startHandshake(CallbackInfo ci) { DdsClientNetwork.onConnectedToServer(); CarpetRuleClientNetwork.connected((ClientPacketListener)(Object)this); }
}
