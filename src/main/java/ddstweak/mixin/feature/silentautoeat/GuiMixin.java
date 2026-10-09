package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
import net.minecraft.client.Minecraft;
//#if MC >= 260200
//$$ import net.minecraft.client.gui.Hud;
//#else
import net.minecraft.client.gui.Gui;
//#endif
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC >= 260200
//$$ @Mixin(Hud.class)
//#else
@Mixin(Gui.class)
//#endif
public abstract class GuiMixin {
    @Redirect(
            method="renderHotbar",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"),
            require=0
    )
    private ItemStack ddstweak$silentAutoEatLegacyHotbar(Player player){
        ItemStack upstream=player.getOffhandItem();
        return player instanceof LocalPlayer
                ?SilentAutoEatPresentation.projectOffhand((LocalPlayer)player,upstream):upstream;
    }

    @Redirect(
            method={"renderItemHotbar","extractItemHotbar"},
            at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"),
            require=0
    )
    private ItemStack ddstweak$silentAutoEatModernHotbar(Player player){
        ItemStack upstream=player.getOffhandItem();
        return player instanceof LocalPlayer
                ?SilentAutoEatPresentation.projectOffhand((LocalPlayer)player,upstream):upstream;
    }

    @Redirect(
            method={"renderHotbar","renderItemHotbar","extractItemHotbar"},
            at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"),
            require=0
    )
    private ItemStack ddstweak$silentAutoEatModernSourceSlot(Inventory inventory,int index){
        ItemStack upstream=inventory.getItem(index);
        LocalPlayer player=Minecraft.getInstance().player;
        return player==null?upstream:SilentAutoEatPresentation.projectInventoryStack(player,index,upstream);
    }

    @Redirect(
            method="renderHotbar",
            at=@At(value="INVOKE",target="Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"),
            require=0
    )
    private Object ddstweak$silentAutoEatLegacySourceSlot(NonNullList<?> list,int index){
        Object upstream=list.get(index);
        LocalPlayer player=Minecraft.getInstance().player;
        return player!=null&&upstream instanceof ItemStack
                ?SilentAutoEatPresentation.projectInventoryStack(player,index,(ItemStack)upstream):upstream;
    }
}
