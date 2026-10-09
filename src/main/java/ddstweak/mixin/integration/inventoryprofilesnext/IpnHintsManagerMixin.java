package ddstweak.mixin.integration.inventoryprofilesnext;

import ddstweak.integration.dds.fakeplayer.client.ipn.DdsIpnCompat;
import ddstweak.integration.dds.fakeplayer.screen.DdsFakePlayerScreen;
import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerEnderChestScreen;
import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryMainScreen;
import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryRealScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="org.anti_ad.mc.ipnext.integration.HintsManagerNG",remap=false)
public abstract class IpnHintsManagerMixin {
    @Inject(method="getIgnoredClass",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private void ddsTweak$allowPlayerInventoryScreens(Class<?> type,CallbackInfoReturnable<Class<?>> cir){
        if(DdsIpnCompat.level1Ready()&&(type==DdsFakePlayerScreen.class||type==UnifiedPlayerInventoryMainScreen.class||type==UnifiedPlayerInventoryRealScreen.class||type==UnifiedPlayerEnderChestScreen.class))cir.setReturnValue(null);
    }
}
