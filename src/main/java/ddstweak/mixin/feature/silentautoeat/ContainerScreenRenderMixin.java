package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.ModifyArgs;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenRenderMixin {
    //#if MC >= 12111
    @Shadow protected Slot hoveredSlot;
    //#if MC < 260100
    @Unique private Slot ddstweak$silentAutoEatRenderSlot;
    //#endif
    //#else
    //$$ @Unique private Slot ddstweak$silentAutoEatRenderSlot;
    //#endif

    //#if MC >= 12111
    //#if MC >= 260100
    //$$ @ModifyVariable(
    //$$         method="extractSlot",
    //$$         at=@At(value="STORE",ordinal=0),
    //$$         ordinal=0,
    //$$         require=1
    //$$ )
    //$$ private ItemStack ddstweak$projectExtractedSlotStack(
    //$$         ItemStack upstream, net.minecraft.client.gui.GuiGraphicsExtractor graphics,
    //$$         Slot slot, int mouseX, int mouseY){
    //$$     LocalPlayer player=Minecraft.getInstance().player;
    //$$     return player==null?upstream:SilentAutoEatPresentation.projectContainerStack(player,slot,upstream);
    //$$ }
    //#else
    @ModifyVariable(
            method="renderSlot",
            at=@At("HEAD"),
            argsOnly=true,
            ordinal=0,
            require=1
    )
    private Slot ddstweak$captureRenderSlot(Slot slot){
        this.ddstweak$silentAutoEatRenderSlot=slot;
        return slot;
    }

    @ModifyVariable(
            method="renderSlot",
            at=@At(value="STORE",ordinal=0),
            ordinal=0,
            require=1
    )
    private ItemStack ddstweak$projectRenderedStack(ItemStack upstream){
        Slot slot=this.ddstweak$silentAutoEatRenderSlot;
        LocalPlayer player=Minecraft.getInstance().player;
        return player==null||slot==null
                ?upstream:SilentAutoEatPresentation.projectContainerStack(player,slot,upstream);
    }
    //#endif

    //#if MC >= 260100
    //$$ @ModifyVariable(
    //$$         method="extractTooltip",
    //$$         at=@At(value="STORE",ordinal=0),
    //$$         ordinal=0,
    //$$         require=1
    //$$ )
    //#else
    @ModifyVariable(
            method="renderTooltip",
            at=@At(value="STORE",ordinal=0),
            ordinal=0,
            require=1
    )
    //#endif
    private ItemStack ddstweak$projectTooltipStack(ItemStack upstream){
        LocalPlayer player=Minecraft.getInstance().player;
        Slot slot=this.hoveredSlot;
        return player==null||slot==null
                ?upstream:SilentAutoEatPresentation.projectContainerStack(player,slot,upstream);
    }

    //#if MC >= 260100
    //$$ @ModifyExpressionValue(
    //$$         method="extractTooltip",
    //$$         at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;hasItem()Z"),
    //$$         require=1
    //$$ )
    //#else
    @ModifyExpressionValue(
            method="renderTooltip",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;hasItem()Z"),
            require=1
    )
    //#endif
    private boolean ddstweak$projectTooltipHasItem(boolean upstream){
        LocalPlayer player=Minecraft.getInstance().player;
        Slot slot=this.hoveredSlot;
        if(player==null||slot==null||!SilentAutoEatPresentation.isPresentedContainerSlot(player,slot))
            return upstream;
        return !SilentAutoEatPresentation.projectContainerStack(player,slot,ItemStack.EMPTY).isEmpty();
    }
    //#elseif MC == 12001
    //$$ @WrapOperation(
    //$$         method="renderSlot",
    //$$         at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"),
    //$$         require=1
    //$$ )
    //$$ private ItemStack ddstweak$projectLegacy12001SlotStack(Slot slot,Operation<ItemStack> original){
    //$$     ItemStack upstream=original.call(slot);
    //$$     LocalPlayer player=Minecraft.getInstance().player;
    //$$     if(player==null)return upstream;
    //$$     ItemStack projected=SilentAutoEatPresentation.projectContainerStack(player,slot,upstream);
    //$$     SilentAutoEatPresentation.traceLegacy12001OffhandSlot(player,slot,upstream,projected);
    //$$     return projected;
    //$$ }
    //#else
    //$$ @ModifyVariable(
    //$$         method={"renderSlot","extractSlot"},
    //$$         at=@At("HEAD"),
    //$$         argsOnly=true,
    //$$         ordinal=0,
    //$$         require=0
    //$$ )
    //$$ private Slot ddstweak$captureSilentAutoEatRenderSlot(Slot slot){
    //$$     this.ddstweak$silentAutoEatRenderSlot=slot;
    //$$     return slot;
    //$$ }
    //$$
    //$$ @ModifyVariable(
    //$$         method={"renderSlot","extractSlot"},
    //$$         at=@At(value="STORE",ordinal=0),
    //$$         ordinal=0,
    //$$         require=0
    //$$ )
    //$$ private ItemStack ddstweak$projectSilentAutoEatRenderedStack(ItemStack upstream){
    //$$     Slot slot=this.ddstweak$silentAutoEatRenderSlot;
    //$$     LocalPlayer player=Minecraft.getInstance().player;
    //$$     return player==null||slot==null
    //$$             ?upstream:SilentAutoEatPresentation.projectContainerStack(player,slot,upstream);
    //$$ }
    //$$
    //$$ @WrapOperation(
    //$$         method={"renderTooltip","extractTooltip"},
    //$$         at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"),
    //$$         require=0
    //$$ )
    //$$ private ItemStack ddstweak$silentAutoEatTooltipStack(Slot slot,Operation<ItemStack> original){
    //$$     ItemStack upstream=original.call(slot);
    //$$     LocalPlayer player=Minecraft.getInstance().player;
    //$$     return player==null?upstream:SilentAutoEatPresentation.projectContainerStack(player,slot,upstream);
    //$$ }
    //$$
    //$$ @WrapOperation(
    //$$         method={"renderTooltip","extractTooltip"},
    //$$         at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;hasItem()Z"),
    //$$         require=0
    //$$ )
    //$$ private boolean ddstweak$silentAutoEatTooltipHasItem(Slot slot,Operation<Boolean> original){
    //$$     LocalPlayer player=Minecraft.getInstance().player;
    //$$     if(player==null||!SilentAutoEatPresentation.isPresentedContainerSlot(player,slot))
    //$$         return original.call(slot);
    //$$     return !SilentAutoEatPresentation.projectContainerStack(player,slot,ItemStack.EMPTY).isEmpty();
    //$$ }
    //#endif
}
