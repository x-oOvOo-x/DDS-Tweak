package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.fakeplayer.client.ipn.DdsIpnCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="org.anti_ad.mc.ipnext.inventory.AreaTypes",remap=false)
/**
 * Remaps IPN storage and sort areas for supported fake-player screens.
 * 为受支持的假人界面修正 IPN 存储及整理区域。
 */
public abstract class IpnAreaTypesMixin {
    @Inject(method="getItemStorage",at=@At("RETURN"),cancellable=true,require=0,remap=false)private void ddsTweak$fixedFakeStorage(CallbackInfoReturnable<Object> cir){cir.setReturnValue(DdsIpnCompat.wrapStorageArea(cir.getReturnValue()));}
    @Inject(method="getSortableItemStorage",at=@At("RETURN"),cancellable=true,require=0,remap=false)private void ddsTweak$fixedFakeSortArea(CallbackInfoReturnable<Object> cir){cir.setReturnValue(DdsIpnCompat.wrapSortableArea(cir.getReturnValue()));}
}
