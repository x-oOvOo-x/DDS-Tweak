package ddstweak.mixin.fakeplayer;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the hovered slot for validated fake-player actions.
 * 向已验证的假人操作提供鼠标悬停槽位。
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor{
    @Accessor("hoveredSlot") Slot ddsTweak$getHoveredSlot();
}
