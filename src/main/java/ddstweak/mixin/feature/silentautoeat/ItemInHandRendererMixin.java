package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEat;
import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
//#if MC >= 260300
//$$ import net.minecraft.client.player.FirstPersonHandsAndItems;
//$$ import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.world.entity.HumanoidArm;
//$$ import net.minecraft.world.item.ItemDisplayContext;
//#else
import net.minecraft.client.renderer.ItemInHandRenderer;
//#endif
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//#if MC == 12111
import net.minecraft.world.InteractionHand;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//#endif
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 260300
//$$ @Mixin(FirstPersonHandsAndItems.class)
//#else
@Mixin(ItemInHandRenderer.class)
//#endif
public abstract class ItemInHandRendererMixin {
    @Shadow private ItemStack mainHandItem;
    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;
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
        boolean active=player!=null&&SilentAutoEat.isPresentationActive(player);
        //#if MC >= 260300
        //$$ active|=SilentAutoEat.isOwned26HudSwap(player);
        //#endif
        if(active){
            // 26.3 caches must remain owned by vanilla across swaps and interruptions.
            // Only the final render state receives the projected stacks.
            //#if MC < 260300
            //$$ this.offHandItem=SilentAutoEatPresentation.projectOffhand(player,player.getOffhandItem());
            //#endif
            this.offHandHeight=1.0F;
            this.oOffHandHeight=1.0F;
            ItemStack mainhand=player.getMainHandItem();
            ItemStack visualMainhand=SilentAutoEatPresentation.projectMainhand(player,mainhand);
            if(visualMainhand!=mainhand){
                //#if MC < 260300
                //$$ this.mainHandItem=visualMainhand;
                //#endif
                this.mainHandHeight=1.0F;
                this.oMainHandHeight=1.0F;
            }
        }
        if(player!=null&&SilentAutoEat.isMainhandVisualSourceActive(player)){
            // Vanilla selects the food for real. The first-person hand keeps
            // its original item, without a fake re-equip dip.
            //#if MC < 260300
            //$$ this.mainHandItem=SilentAutoEatPresentation.projectMainhand(player,player.getMainHandItem());
            //#endif
            this.mainHandHeight=1.0F;
            this.oMainHandHeight=1.0F;
        }
        if(player!=null&&SilentAutoEat.isStagedMainhandVisualActive(player)){
            this.mainHandHeight=1.0F;
            this.oMainHandHeight=1.0F;
        }
        if(player!=null&&SilentAutoEat.isRestoredMainhandVisualActive(player)){
            // Vanilla equip interpolation can still react to the temporary SWAP
            // after the server-confirmed restore; pin only this brief visual phase.
            this.mainHandHeight=1.0F;
            this.oMainHandHeight=1.0F;
        }
    }

    //#if MC == 12111
    /**
     * 1.21.11 renders the stack passed to renderArmWithItem, which may be the
     * actual offhand item temporarily stored in the selected food source.
     * Project that argument at the final consumer, not only the tick cache.
     */
    @ModifyVariable(
            method="renderArmWithItem",
            at=@At("HEAD"),
            argsOnly=true,
            ordinal=0,
            require=1
    )
    private ItemStack ddstweak$project12111RenderHandStack(
            ItemStack upstream,
            @Local(argsOnly=true) AbstractClientPlayer entity,
            @Local(argsOnly=true) InteractionHand hand){
        if(!(entity instanceof LocalPlayer player))return upstream;
        return hand==InteractionHand.MAIN_HAND
                ?SilentAutoEatPresentation.projectMainhand(player,upstream)
                :SilentAutoEatPresentation.projectOffhand(player,upstream);
    }
    //#endif

    //#if MC >= 260300
    //$$ /** Project final 26.3 hand item models, not the persistent vanilla equip caches. */
    //$$ @Inject(method="extractRenderState",at=@At("TAIL"),require=1)
    //$$ private void ddstweak$projectFirstPersonHands(
    //$$         LocalPlayer player,float partialTicks,FirstPersonHandsAndItemsRenderState state,CallbackInfo ci){
    //$$     if(SilentAutoEat.isStagedMainhandVisualActive(player)){
    //$$         // The selected slot physically holds food; only render the
    //$$         // displaced original item. Retain server-owned food/use state.
    //$$         state.mainHandHeight=1.0F;
    //$$         state.oldMainHandHeight=1.0F;
    //$$         state.useItemRemainingTicks=0;
    //$$         ItemStack visual=SilentAutoEatPresentation.projectMainhand(player,state.mainHandItem);
    //$$         if(!ItemStack.matches(state.mainHandItem,visual)){
    //$$             state.mainHandItem=visual;
    //$$             boolean mainRight=player.getMainArm()==HumanoidArm.RIGHT;
    //$$             Minecraft.getInstance().getItemModelResolver().updateForLiving(
    //$$                     state.mainHandRenderState,visual,
    //$$                     mainRight?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND,player);
    //$$         }
    //$$         return;
    //$$     }
    //$$     if(SilentAutoEat.isMainhandVisualSourceActive(player)){
    //$$         // Selected hotbar food is real; only first-person visuals are projected.
    //$$         state.mainHandHeight=1.0F;
    //$$         state.oldMainHandHeight=1.0F;
    //$$         state.useItemRemainingTicks=0;
    //$$         ItemStack visual=SilentAutoEatPresentation.projectMainhand(player,state.mainHandItem);
    //$$         if(!ItemStack.matches(state.mainHandItem,visual)){
    //$$             state.mainHandItem=visual;
    //$$             boolean mainRight=player.getMainArm()==HumanoidArm.RIGHT;
    //$$             Minecraft.getInstance().getItemModelResolver().updateForLiving(
    //$$                     state.mainHandRenderState,visual,
    //$$                     mainRight?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND,player);
    //$$         }
    //$$         return;
    //$$     }
    //$$     boolean active=SilentAutoEat.isPresentationActive(player)
    //$$             ||SilentAutoEat.isOwned26HudSwap(player);
    //$$     if(!active&&!SilentAutoEat.isRestoredMainhandVisualActive(player))return;
    //$$     Minecraft minecraft=Minecraft.getInstance();
    //$$     boolean mainRight=player.getMainArm()==HumanoidArm.RIGHT;
    //$$     ItemStack main=SilentAutoEatPresentation.projectMainhand(player,
    //$$             active?player.getMainHandItem():state.mainHandItem);
    //$$     if(!active){
    //$$         // Only the mainhand vanilla cache can lag after a confirmed restore.
    //$$         if(ItemStack.matches(state.mainHandItem,main))return;
    //$$         state.mainHandItem=main;
    //$$         minecraft.getItemModelResolver().updateForLiving(state.mainHandRenderState,main,
    //$$                 mainRight?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND,player);
    //$$         return;
    //$$     }
    //$$     ItemStack off=SilentAutoEatPresentation.projectOffhand(player,player.getOffhandItem());
    //$$     state.offHandHeight=1.0F;
    //$$     state.oldOffHandHeight=1.0F;
    //$$     state.mainHandItem=main;
    //$$     state.offHandItem=off;
    //$$     minecraft.getItemModelResolver().updateForLiving(state.mainHandRenderState,main,
    //$$             mainRight?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND,player);
    //$$     minecraft.getItemModelResolver().updateForLiving(state.offHandRenderState,off,
    //$$             mainRight?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,player);
    //$$     state.useItemRemainingTicks=0;
    //$$ }
    //#else
    @WrapOperation(
            method="renderArmWithItem",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"),
            require=0
    )
    private boolean ddstweak$silentAutoEatHideUseAnimation(AbstractClientPlayer player,Operation<Boolean> original){
        return player instanceof LocalPlayer&&SilentAutoEat.shouldHideUseAnimation((LocalPlayer)player)
                ?false:original.call(player);
    }
    //#endif

    @WrapOperation(
            method="evaluateWhichHandsToRender",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"),
            require=0
    )
    private static boolean ddstweak$silentAutoEatHideHandSelectionUse(LocalPlayer player,Operation<Boolean> original){
        return SilentAutoEat.shouldHideUseAnimation(player)?false:original.call(player);
    }

    @WrapOperation(
            method="evaluateWhichHandsToRender",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"),
            require=0
    )
    private static ItemStack ddstweak$silentAutoEatVisualOffhand(LocalPlayer player,Operation<ItemStack> original){
        return SilentAutoEatPresentation.projectOffhand(player,original.call(player));
    }
}
