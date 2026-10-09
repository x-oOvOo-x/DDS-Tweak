package ddstweak.mixin.fakeplayer;

import ddstweak.integration.org.playerinventory.OrgPlayerInventoryOpenIntent;
import ddstweak.integration.provider.ProviderPlayerInventoryScreenCoordinator;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin{
    @Shadow @Final protected AbstractContainerMenu menu;

    //#if MC >= 11700
    @Inject(method="containerTick",at=@At("TAIL"))
    private void ddsTweak$upgradeProviderPlayerInventory(CallbackInfo ci){
        ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent();
    }
    //#else
    //$$ @Inject(method="tick",at=@At("TAIL"))
    //$$ private void ddsTweak$upgradeProviderPlayerInventory(CallbackInfo ci){
    //$$     ProviderPlayerInventoryScreenCoordinator.tryUpgradeCurrent();
    //$$ }
    //#endif

    //#if MC >= 11602
    @Inject(method="onClose",at=@At("TAIL"))
    private void ddsTweak$clearProviderPlayerInventorySession(CallbackInfo ci){
        OrgPlayerInventoryOpenIntent.close(menu.containerId);
    }
    //#else
    //$$ @Inject(method="removed",at=@At("TAIL"))
    //$$ private void ddsTweak$clearProviderPlayerInventorySession(CallbackInfo ci){
    //$$     OrgPlayerInventoryOpenIntent.close(menu.containerId);
    //$$ }
    //#endif
}
