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
/**
 * Observes vanilla level event packets for block sound tracking.
 * 监听原版世界事件包，记录方块发声位置。
 */
public abstract class ClientLevelEventMixin {
    @Inject(method="handleLevelEvent",at=@At("TAIL"))
    private void ddsTweak$trackBlockSoundEvent(ClientboundLevelEventPacket packet,CallbackInfo ci){
        BlockSoundTracker.onWorldEvent(packet.getType(),packet.getPos());
    }
}
//#endif
