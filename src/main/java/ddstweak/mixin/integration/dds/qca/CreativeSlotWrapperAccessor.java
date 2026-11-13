package ddstweak.mixin.integration.dds.qca;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Unwraps creative inventory slots for validated QCA interactions.
 * 为已验证的 QCA 操作解析创造模式包装槽位。
 */
@Mixin(targets="net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public interface CreativeSlotWrapperAccessor {
    @Accessor("target") Slot ddsTweak$getTarget();
}
