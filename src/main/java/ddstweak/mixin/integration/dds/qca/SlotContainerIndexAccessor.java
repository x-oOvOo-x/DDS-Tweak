package ddstweak.mixin.integration.dds.qca;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
//#if MC < 11700
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//#endif

/** Pre-1.17 bridge for Slot#getContainerSlot(), which did not exist yet. */
@Mixin(Slot.class)
public interface SlotContainerIndexAccessor {
    //#if MC < 11700
    //$$ @Accessor("slot")
    //$$ int ddsTweak$getContainerSlot();
    //#endif
}
