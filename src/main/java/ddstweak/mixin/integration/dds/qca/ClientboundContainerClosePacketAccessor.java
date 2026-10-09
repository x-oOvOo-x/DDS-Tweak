package ddstweak.mixin.integration.dds.qca;

import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundContainerClosePacket.class)
public interface ClientboundContainerClosePacketAccessor {
    @Accessor("containerId") int ddsTweak$getContainerId();
}
