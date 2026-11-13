package ddstweak.mixin.feature.silentautoeat;

import ddstweak.feature.silentautoeat.SilentAutoEatPresentation;
//#if MC >= 12111
import ddstweak.feature.silentautoeat.SilentAutoEat;
//#endif
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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//#if MC >= 260200
//$$ @Mixin(Hud.class)
//#else
@Mixin(Gui.class)
//#endif
/**
 * Adapts HUD presentation while Auto Eat temporarily owns a swap.
 * 自动进食临时持有交换事务时调整 HUD 显示。
 */
public abstract class GuiMixin {
    //#if MC >= 260300
    //$$ // Replace the actual Player.getOffhandItem read, BEFORE the vanilla
    //$$ // if(!offhand.isEmpty()) decides whether an offhand HUD slot exists.
    //$$ @WrapOperation(
    //$$         method="extractItemHotbar",
    //$$         at=@At(value="INVOKE",
    //$$                 target="Lnet/minecraft/world/entity/player/Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"),
    //$$         require=1
    //$$ )
    //$$ private ItemStack ddstweak$silentAutoEatOffhandBeforeHudVisibility(
    //$$         Player player,Operation<ItemStack> original){
    //$$     ItemStack upstream=original.call(player);
    //$$     return player instanceof LocalPlayer
    //$$             ?SilentAutoEatPresentation.projectHudOffhand26((LocalPlayer)player,upstream)
    //$$             :upstream;
    //$$ }
    //#elseif MC >= 260200
    //$$ @ModifyVariable(
    //$$         method="extractItemHotbar",
    //$$         at=@At(value="STORE",ordinal=0),
    //$$         ordinal=0,
    //$$         require=1
    //$$ )
    //$$ private ItemStack ddstweak$silentAutoEatModernOffhand(ItemStack upstream,@Local Player player){
    //$$     return player instanceof LocalPlayer
    //$$             ?SilentAutoEatPresentation.projectOffhand((LocalPlayer)player,upstream):upstream;
    //$$ }
    //#elseif MC >= 260100
    //$$ @ModifyVariable(
    //$$         method="extractItemHotbar",
    //$$         at=@At(value="STORE",ordinal=0),
    //$$         ordinal=0,
    //$$         require=1
    //$$ )
    //$$ private ItemStack ddstweak$silentAutoEatModernOffhand(ItemStack upstream,@Local Player player){
    //$$     return player instanceof LocalPlayer
    //$$             ?SilentAutoEatPresentation.projectOffhand((LocalPlayer)player,upstream):upstream;
    //$$ }
    //#elseif MC >= 12111
    @ModifyVariable(
            method="renderItemHotbar",
            at=@At(value="STORE",ordinal=0),
            ordinal=0,
            require=1
    )
    private ItemStack ddstweak$silentAutoEatModernOffhand(ItemStack upstream,@Local Player player){
        return player instanceof LocalPlayer
                ?SilentAutoEatPresentation.projectOffhand((LocalPlayer)player,upstream):upstream;
    }
    //#else
    //$$ @WrapOperation(
    //$$         method="renderHotbar",
    //$$         at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"),
    //$$         require=0
    //$$ )
    //$$ private ItemStack ddstweak$silentAutoEatLegacyHotbar(Player player,Operation<ItemStack> original){
    //$$     ItemStack upstream=original.call(player);
    //$$     return player instanceof LocalPlayer
    //$$             ?SilentAutoEatPresentation.projectOffhand((LocalPlayer)player,upstream):upstream;
    //$$ }
    //$$
    //$$ @WrapOperation(
    //$$         method={"renderItemHotbar","extractItemHotbar"},
    //$$         at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"),
    //$$         require=0
    //$$ )
    //$$ private ItemStack ddstweak$silentAutoEatLegacyItemHotbar(Player player,Operation<ItemStack> original){
    //$$     ItemStack upstream=original.call(player);
    //$$     return player instanceof LocalPlayer
    //$$             ?SilentAutoEatPresentation.projectOffhand((LocalPlayer)player,upstream):upstream;
    //$$ }
    //#endif

    //#if MC >= 260300
    //$$ // 26.3's Hud.extractItemHotbar reads the nine displayed source stacks here.
    //$$ // Fail Mixin startup if this target moves; a missed optional hook would show
    //$$ // the original offhand stack in the exchanged selected-mainhand slot.
    //$$ @WrapOperation(
    //$$         method="extractItemHotbar",
    //$$         at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"),
    //$$         require=1
    //$$ )
    //$$ private ItemStack ddstweak$silentAutoEatModernSourceSlot(Inventory inventory,int index,Operation<ItemStack> original){
    //$$     ItemStack upstream=original.call(inventory,index);
    //$$     LocalPlayer player=Minecraft.getInstance().player;
    //$$     return player==null?upstream
    //$$             :SilentAutoEatPresentation.projectHudSource26(player,inventory,index,upstream);
    //$$ }
    //#else
    @WrapOperation(
            method={"renderHotbar","renderItemHotbar","extractItemHotbar"},
            at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"),
            require=0
    )
    private ItemStack ddstweak$silentAutoEatModernSourceSlot(Inventory inventory,int index,Operation<ItemStack> original){
        ItemStack upstream=original.call(inventory,index);
        LocalPlayer player=Minecraft.getInstance().player;
        return player==null?upstream
                :SilentAutoEatPresentation.projectHotbarInventoryStack(player,inventory,index,upstream);
    }
    //#endif

    @WrapOperation(
            method="renderHotbar",
            at=@At(value="INVOKE",target="Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"),
            require=0
    )
    private Object ddstweak$silentAutoEatLegacySourceSlot(NonNullList<?> list,int index,Operation<Object> original){
        Object upstream=original.call(list,index);
        LocalPlayer player=Minecraft.getInstance().player;
        return player!=null&&upstream instanceof ItemStack
                ?SilentAutoEatPresentation.projectInventoryStack(player,index,(ItemStack)upstream):upstream;
    }

    //#if MC >= 260300
    //$$ /**
    //$$  * 26.3's HUD extracts deferred item render states. Give the extractor
    //$$  * a presentation-only clone so popTime never reaches its squeeze path.
    //$$  * The live Inventory ItemStack and server clicks remain untouched.
    //$$  */
    //$$ @ModifyVariable(
    //$$         method="extractSlot",
    //$$         at=@At("HEAD"),
    //$$         argsOnly=true,
    //$$         ordinal=0,
    //$$         require=1
    //$$ )
    //$$ private ItemStack ddstweak$silentAutoEatHideExtractedHotbarPop(
    //$$         ItemStack upstream,@Local(argsOnly=true) Player player,
    //$$         @Local(argsOnly=true,ordinal=2) int seed){
    //$$     int index=seed==10?40:seed-1;
    //$$     if(!(player instanceof LocalPlayer)
    //$$             ||!SilentAutoEat.shouldSuppressExtractedHotbarPop((LocalPlayer)player,index))
    //$$         return upstream;
    //$$     // ItemStack.copy() preserves popTime in 26.3: clear ONLY the clone.
    //$$     ItemStack renderOnly=upstream.copy();
    //$$     renderOnly.setPopTime(0);
    //$$     return renderOnly;
    //$$ }
    //#endif

    //#if MC >= 12111
    /** Vanilla slot popTime squeezes the HUD icon after an automated SWAP. */
    @WrapOperation(
            //#if MC >= 260100
            //$$ method="extractSlot",
            //#else
            method="renderSlot",
            //#endif
            at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;getPopTime()I"),
            require=1
    )
    private int ddstweak$silentAutoEatHideOwnedHotbarPop(
            ItemStack stack,Operation<Integer> original,
            @Local(argsOnly=true) Player player,
            @Local(argsOnly=true,ordinal=2) int seed){
        int slot=seed==10?40:seed-1;
        if(player instanceof LocalPlayer
                &&SilentAutoEat.shouldSuppressHotbarPop((LocalPlayer)player,slot,stack))
            return 0;
        return original.call(stack);
    }
    //#endif
}
