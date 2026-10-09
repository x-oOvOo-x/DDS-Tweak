package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
//#if MC >= 260300
//$$ import net.minecraft.client.player.FirstPersonHandsAndItems;
//$$ import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
//#else
import net.minecraft.client.renderer.ItemInHandRenderer;
//#endif
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 260300
//$$ @Mixin(FirstPersonHandsAndItems.class)
//#else
@Mixin(ItemInHandRenderer.class)
//#endif
public abstract class ItemInHandRendererMixin {
    @Shadow private ItemStack offHandItem;
    @Shadow private float offHandHeight;
    @Shadow private float oOffHandHeight;

    //#if MC >= 260300
    //$$ @Inject(method="tick",at=@At("TAIL"))
    //$$ private void ddstweak$silentAutoEatStableOffhand(LocalPlayer player,CallbackInfo ci){
    //$$     ddstweak$pinVisualOffhand(player);
    //$$ }
    //#else
    @Inject(method="tick",at=@At("TAIL"))
    private void ddstweak$silentAutoEatStableOffhand(CallbackInfo ci){
        ddstweak$pinVisualOffhand(net.minecraft.client.Minecraft.getInstance().player);
    }
    //#endif

    private void ddstweak$pinVisualOffhand(LocalPlayer player){
        if(player!=null&&SilentAutoEat.isPresentationActive(player)){
            this.offHandItem=SilentAutoEatPresentation.projectOffhand(player,player.getOffhandItem());
            this.offHandHeight=1.0F;
            this.oOffHandHeight=1.0F;
        }
    }

    //#if MC >= 260300
    //$$ @Inject(method="extractRenderState",at=@At("TAIL"))
    //$$ private void ddstweak$silentAutoEatExtract(
    //$$         LocalPlayer player,float partialTicks,FirstPersonHandsAndItemsRenderState state,CallbackInfo ci){
    //$$     if(SilentAutoEat.isPresentationActive(player))state.useItemRemainingTicks=0;
    //$$ }
    //#else
    @Redirect(
            method="renderArmWithItem",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"),
            require=0
    )
    private boolean ddstweak$silentAutoEatHideUseAnimation(AbstractClientPlayer player){
        return player instanceof LocalPlayer&&SilentAutoEat.isPresentationActive((LocalPlayer)player)
                ?false:player.isUsingItem();
    }
    //#endif

    @Redirect(
            method="evaluateWhichHandsToRender",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"),
            require=0
    )
    private static boolean ddstweak$silentAutoEatHideHandSelectionUse(LocalPlayer player){
        return SilentAutoEat.isPresentationActive(player)?false:player.isUsingItem();
    }

    @Redirect(
            method="evaluateWhichHandsToRender",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"),
            require=0
    )
    private static ItemStack ddstweak$silentAutoEatVisualOffhand(LocalPlayer player){
        return SilentAutoEatPresentation.projectOffhand(player,player.getOffhandItem());
    }
}
