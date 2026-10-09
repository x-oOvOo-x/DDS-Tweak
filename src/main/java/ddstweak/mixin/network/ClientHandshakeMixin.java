package ddstweak.mixin.network;

import ddstweak.network.DdsClientNetwork;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ClientPacketListener.class,priority=500)
public abstract class ClientHandshakeMixin {
    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void ddsTweak$startHandshake(CallbackInfo ci) { DdsClientNetwork.onConnectedToServer(); }
}
