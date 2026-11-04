package ddstweak.mixin.integration.ommc;

import ddstweak.integration.ommc.OmmcSortCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Unsupported sorter shape: only disable OMMC R-sort on DT mirror menus. */
@Pseudo
@Mixin(targets="com.plusls.ommc.impl.feature.sortInventory.SortInventoryHelper",remap=false)
public abstract class SortInventoryModernFallbackMixin {
    @Inject(method="sort",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void ddsTweak$disableUnsupportedSort(CallbackInfoReturnable<Runnable> cir){
        OmmcSortCompatibility.unsupported(cir);
    }
}
