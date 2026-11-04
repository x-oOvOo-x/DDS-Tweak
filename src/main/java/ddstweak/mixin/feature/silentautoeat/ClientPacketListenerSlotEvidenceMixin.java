package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Read-only server packet observation. TAIL runs after vanilla has applied its
 * slot update on the client thread; it never changes or cancels a packet.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerSlotEvidenceMixin {
    @Inject(method="handleContainerSetSlot",at=@At("TAIL"))
    private void ddstweak$observeAutoEatServerSlot(ClientboundContainerSetSlotPacket packet,CallbackInfo ci){
        SilentAutoEat.observeServerSlot(packet.getContainerId(),packet.getSlot(),packet.getItem());
    }

    //#if MC >= 12111
    //$$ @Inject(method="handleSetPlayerInventory",at=@At("TAIL"))
    //$$ private void ddstweak$observeAutoEatPlayerInventory(
    //$$         net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket packet,
    //$$         CallbackInfo ci){
    //$$     SilentAutoEat.observeServerInventorySlot(packet.slot(),packet.contents());
    //$$ }
    //#endif

    @Inject(method="handleContainerContent",at=@At("TAIL"))
    private void ddstweak$observeAutoEatServerSnapshot(ClientboundContainerSetContentPacket packet,CallbackInfo ci){
        //#if MC >= 12105
        SilentAutoEat.observeServerSnapshot(packet.containerId(),packet.items());
        //#else
        //$$ SilentAutoEat.observeServerSnapshot(packet.getContainerId(),packet.getItems());
        //#endif
    }
}
