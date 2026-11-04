package ddstweak.mixin.feature.blocksoundtracker;

//#if MC >= 12111
import ddstweak.feature.blocksoundtracker.BlockSoundTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientLevelEventMixin {
    @Inject(method="handleLevelEvent",at=@At("TAIL"))
    private void ddsTweak$trackBlockSoundEvent(ClientboundLevelEventPacket packet,CallbackInfo ci){
        BlockSoundTracker.onWorldEvent(packet.getType(),packet.getPos());
    }
}
//#endif
