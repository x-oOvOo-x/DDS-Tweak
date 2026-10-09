package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenRenderMixin {
    @Unique
    private Slot ddstweak$silentAutoEatRenderSlot;

    @ModifyVariable(
            method={"renderSlot","extractSlot"},
            at=@At("HEAD"),
            argsOnly=true,
            ordinal=0,
            require=0
    )
    private Slot ddstweak$captureSilentAutoEatRenderSlot(Slot slot){
        this.ddstweak$silentAutoEatRenderSlot=slot;
        return slot;
    }

    @ModifyVariable(
            method={"renderSlot","extractSlot"},
            at=@At(value="STORE",ordinal=0),
            ordinal=0,
            require=0
    )
    private ItemStack ddstweak$projectSilentAutoEatRenderedStack(ItemStack upstream){
        Slot slot=this.ddstweak$silentAutoEatRenderSlot;
        LocalPlayer player=Minecraft.getInstance().player;
        return player==null||slot==null
                ?upstream:SilentAutoEatPresentation.projectContainerStack(player,slot,upstream);
    }

    @Redirect(
            method={"renderTooltip","extractTooltip"},
            at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"),
            require=0
    )
    private ItemStack ddstweak$silentAutoEatTooltipStack(Slot slot){
        ItemStack upstream=slot.getItem();
        LocalPlayer player=Minecraft.getInstance().player;
        return player==null?upstream:SilentAutoEatPresentation.projectContainerStack(player,slot,upstream);
    }

    @Redirect(
            method={"renderTooltip","extractTooltip"},
            at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;hasItem()Z"),
            require=0
    )
    private boolean ddstweak$silentAutoEatTooltipHasItem(Slot slot){
        return !ddstweak$silentAutoEatTooltipStack(slot).isEmpty();
    }
}
