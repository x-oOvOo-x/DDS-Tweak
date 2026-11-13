package ddstweak.mixin.integration.ommc;

import ddstweak.integration.ommc.OmmcSortCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Unsupported sorter shape: only disable OMMC R-sort on DT mirror menus.
 * 只在 DT 镜像背包禁用不兼容的旧版 OMMC 整理。
 */
@Pseudo
@Mixin(targets="com.plusls.ommc.feature.sortInventory.SortInventoryUtil",remap=false)
public abstract class SortInventoryLegacyFallbackMixin {
    @Inject(method="sort",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void ddsTweak$disableUnsupportedSort(CallbackInfoReturnable<Runnable> cir){
        OmmcSortCompatibility.unsupported(cir);
    }
}
