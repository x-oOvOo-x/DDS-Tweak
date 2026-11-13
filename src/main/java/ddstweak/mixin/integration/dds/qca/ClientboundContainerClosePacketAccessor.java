package ddstweak.mixin.integration.dds.qca;

import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the container ID before suppressing a QCA transition-close packet.
 * 拦截 QCA 切换关闭包前读取容器 ID。
 */
@Mixin(ClientboundContainerClosePacket.class)
public interface ClientboundContainerClosePacketAccessor {
    @Accessor("containerId") int ddsTweak$getContainerId();
}
