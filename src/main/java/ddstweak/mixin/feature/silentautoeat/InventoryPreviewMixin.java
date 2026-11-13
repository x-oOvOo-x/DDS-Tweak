package ddstweak.mixin.feature.silentautoeat;

//#if MC >= 12111
import ddstweak.feature.silentautoeat.SilentAutoEat;
import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Projects only the inventory preview's extracted render state.
 * Never changes the real player Inventory, held items, or the world renderer.
 * 仅修改背包角色预览的渲染状态，不修改真实背包。
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryPreviewMixin {
    @Inject(
            method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at=@At("RETURN")
    )
    private static void ddstweak$projectPreviewHands(LivingEntity entity,CallbackInfoReturnable<EntityRenderState> cir){
        Minecraft minecraft=Minecraft.getInstance();
        LocalPlayer player=minecraft.player;
        if(player==null||entity!=player||!SilentAutoEat.isPresentationActive(player))return;
        if(!(cir.getReturnValue() instanceof ArmedEntityRenderState state))return;

        ItemStack actualMain=player.getMainHandItem();
        ItemStack visualMain=SilentAutoEatPresentation.projectMainhand(player,actualMain);
        if(!ItemStack.matches(actualMain,visualMain)){
            boolean mainRight=state.mainArm==HumanoidArm.RIGHT;
            if(mainRight){
                state.rightHandItemStack=visualMain;
                state.rightArmPose=visualMain.isEmpty()?HumanoidModel.ArmPose.EMPTY:HumanoidModel.ArmPose.ITEM;
                minecraft.getItemModelResolver().updateForLiving(
                        state.rightHandItemState,visualMain,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,player);
            }else{
                state.leftHandItemStack=visualMain;
                state.leftArmPose=visualMain.isEmpty()?HumanoidModel.ArmPose.EMPTY:HumanoidModel.ArmPose.ITEM;
                minecraft.getItemModelResolver().updateForLiving(
                        state.leftHandItemState,visualMain,ItemDisplayContext.THIRD_PERSON_LEFT_HAND,player);
            }
        }

        ItemStack actualOffhand=player.getOffhandItem();
        ItemStack visualOffhand=SilentAutoEatPresentation.projectOffhand(player,actualOffhand);
        if(!ItemStack.matches(actualOffhand,visualOffhand)){
            boolean offhandRight=state.mainArm==HumanoidArm.LEFT;
            if(offhandRight){
                state.rightHandItemStack=visualOffhand;
                state.rightArmPose=visualOffhand.isEmpty()?HumanoidModel.ArmPose.EMPTY:HumanoidModel.ArmPose.ITEM;
                minecraft.getItemModelResolver().updateForLiving(
                        state.rightHandItemState,visualOffhand,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,player);
            }else{
                state.leftHandItemStack=visualOffhand;
                state.leftArmPose=visualOffhand.isEmpty()?HumanoidModel.ArmPose.EMPTY:HumanoidModel.ArmPose.ITEM;
                minecraft.getItemModelResolver().updateForLiving(
                        state.leftHandItemState,visualOffhand,ItemDisplayContext.THIRD_PERSON_LEFT_HAND,player);
            }
        }
    }
}
//#elseif MC >= 12104
//$$ import ddstweak.feature.silentautoeat.SilentAutoEat;
//$$ import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//$$ import net.minecraft.client.player.LocalPlayer;
//$$ import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
//$$ import net.minecraft.client.renderer.item.ItemModelResolver;
//$$ import net.minecraft.world.entity.HumanoidArm;
//$$ import net.minecraft.world.entity.LivingEntity;
//$$ import net.minecraft.world.item.ItemDisplayContext;
//$$ import net.minecraft.world.item.ItemStack;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ /** Minecraft 1.21.4–1.21.10: amend only the model state extracted for an inventory preview. */
//$$ @Mixin(ArmedEntityRenderState.class)
//$$ public abstract class InventoryPreviewMixin {
//$$     @Inject(method="extractArmedEntityRenderState",at=@At("TAIL"),require=1)
//$$     private static void ddstweak$projectPreviewHandModels(
//$$             LivingEntity entity,ArmedEntityRenderState state,ItemModelResolver resolver,CallbackInfo ci){
//$$         Minecraft minecraft=Minecraft.getInstance();
//$$         LocalPlayer player=minecraft.player;
//$$         if(player==null||entity!=player||!(minecraft.screen instanceof InventoryScreen)
//$$                 ||!SilentAutoEat.isPresentationActive(player))return;
//$$         ItemStack actualMain=player.getMainHandItem(),actualOff=player.getOffhandItem();
//$$         ItemStack visualMain=SilentAutoEatPresentation.projectMainhand(player,actualMain);
//$$         ItemStack visualOff=SilentAutoEatPresentation.projectOffhand(player,actualOff);
//$$         boolean mainRight=state.mainArm==HumanoidArm.RIGHT;
//$$         if(!ItemStack.matches(actualMain,visualMain)){
//#if MC == 12104
//$$             resolver.updateForLiving(mainRight?state.rightHandItem:state.leftHandItem,
//$$                     visualMain,mainRight?ItemDisplayContext.THIRD_PERSON_RIGHT_HAND:ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
//$$                     !mainRight,player);
//#else
//$$             resolver.updateForLiving(mainRight?state.rightHandItem:state.leftHandItem,
//$$                     visualMain,mainRight?ItemDisplayContext.THIRD_PERSON_RIGHT_HAND:ItemDisplayContext.THIRD_PERSON_LEFT_HAND,player);
//#endif
//$$         }
//$$         if(!ItemStack.matches(actualOff,visualOff)){
//#if MC == 12104
//$$             resolver.updateForLiving(mainRight?state.leftHandItem:state.rightHandItem,
//$$                     visualOff,mainRight?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
//$$                     mainRight,player);
//#else
//$$             resolver.updateForLiving(mainRight?state.leftHandItem:state.rightHandItem,
//$$                     visualOff,mainRight?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,player);
//#endif
//$$         }
//$$     }
//$$ }
//#elseif MC == 12103
//$$ import ddstweak.feature.silentautoeat.SilentAutoEat;
//$$ import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//$$ import net.minecraft.client.player.AbstractClientPlayer;
//$$ import net.minecraft.client.player.LocalPlayer;
//$$ import net.minecraft.client.renderer.entity.player.PlayerRenderer;
//$$ import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//$$ import net.minecraft.world.entity.HumanoidArm;
//$$ import net.minecraft.world.item.ItemStack;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ /** 1.21.3 predates ArmedEntityRenderState: its player-specific render state owns hand models. */
//$$ @Mixin(PlayerRenderer.class)
//$$ public abstract class InventoryPreviewMixin {
//$$     @Inject(method="extractRenderState",at=@At("TAIL"),require=1)
//$$     private void ddstweak$projectLegacy12103PreviewHands(
//$$             AbstractClientPlayer entity,PlayerRenderState state,float partialTicks,CallbackInfo ci){
//$$         Minecraft minecraft=Minecraft.getInstance();
//$$         LocalPlayer player=minecraft.player;
//$$         if(player==null||entity!=player||!(minecraft.screen instanceof InventoryScreen)
//$$                 ||!SilentAutoEat.isPresentationActive(player))return;
//$$         ItemStack main=SilentAutoEatPresentation.projectMainhand(player,player.getMainHandItem());
//$$         ItemStack off=SilentAutoEatPresentation.projectOffhand(player,player.getOffhandItem());
//$$         boolean mainRight=state.mainArm==HumanoidArm.RIGHT;
//$$         if(!ItemStack.matches(player.getMainHandItem(),main)){
//$$             if(mainRight){
//$$                 state.rightHandItem=main;
//$$                 state.rightHandItemModel=minecraft.getItemRenderer().getModel(main,player.level(),player,0);
//$$             }else{
//$$                 state.leftHandItem=main;
//$$                 state.leftHandItemModel=minecraft.getItemRenderer().getModel(main,player.level(),player,0);
//$$             }
//$$         }
//$$         if(!ItemStack.matches(player.getOffhandItem(),off)){
//$$             if(mainRight){
//$$                 state.leftHandItem=off;
//$$                 state.leftHandItemModel=minecraft.getItemRenderer().getModel(off,player.level(),player,0);
//$$             }else{
//$$                 state.rightHandItem=off;
//$$                 state.rightHandItemModel=minecraft.getItemRenderer().getModel(off,player.level(),player,0);
//$$             }
//$$         }
//$$     }
//$$ }
//#else
//$$ import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//$$ import net.minecraft.client.player.LocalPlayer;
//$$ import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
//$$ import net.minecraft.world.entity.LivingEntity;
//$$ import net.minecraft.world.item.ItemStack;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$
//$$ /** 1.14.4–1.21.1: third-person held-item rendering is used by the inventory avatar. */
//$$ @Mixin(ItemInHandLayer.class)
//$$ public abstract class InventoryPreviewMixin {
//$$     @WrapOperation(
//#if MC == 12001
//$$             method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
//#else
//$$             method="render",
//#endif
//$$             at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"),
//$$             require=1
//$$     )
//$$     private ItemStack ddstweak$projectLegacyPreviewOffhand(LivingEntity entity,Operation<ItemStack> original){
//$$         ItemStack upstream=original.call(entity);
//$$         Minecraft minecraft=Minecraft.getInstance();
//$$         return entity==minecraft.player && entity instanceof LocalPlayer
//$$                 && minecraft.screen instanceof InventoryScreen
//$$                 ?SilentAutoEatPresentation.projectOffhand((LocalPlayer)entity,upstream):upstream;
//$$     }
//$$
//$$     @WrapOperation(
//#if MC == 12001
//$$             method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
//#else
//$$             method="render",
//#endif
//$$             at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"),
//$$             require=1
//$$     )
//$$     private ItemStack ddstweak$projectLegacyPreviewMainhand(LivingEntity entity,Operation<ItemStack> original){
//$$         ItemStack upstream=original.call(entity);
//$$         Minecraft minecraft=Minecraft.getInstance();
//$$         return entity==minecraft.player && entity instanceof LocalPlayer
//$$                 && minecraft.screen instanceof InventoryScreen
//$$                 ?SilentAutoEatPresentation.projectMainhand((LocalPlayer)entity,upstream):upstream;
//$$     }
//$$ }
//#endif
