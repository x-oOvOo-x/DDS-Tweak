package ddstweak.mixin.integration.dds.playerinventory;

import ddstweak.integration.dds.fakeplayer.DdsFakePlayerClientNetwork;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Turns close into a strict parent return only when the server explicitly advertised a real parent. */
@Mixin(AbstractContainerScreen.class)
public abstract class PlayerInventoryReturnMixin {
    @Shadow @Final protected AbstractContainerMenu menu;

    //#if MC >= 12111
    @Inject(method="onClose",at=@At("HEAD"),cancellable=true)
    private void ddsTweak$returnCanonicalEnderParent(CallbackInfo ci){
        if(DdsFakePlayerClientNetwork.consumeReturnCloseBypass(menu))return;
        if(menu instanceof UnifiedPlayerInventoryMirrorMenu
                &&DdsFakePlayerClientNetwork.sendReturnParent((UnifiedPlayerInventoryMirrorMenu)menu))
            ci.cancel();
    }
    //#endif
}
