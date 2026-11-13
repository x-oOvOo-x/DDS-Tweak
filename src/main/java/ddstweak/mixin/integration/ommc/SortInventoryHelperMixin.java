package ddstweak.mixin.integration.ommc;

import ddstweak.integration.ommc.OmmcSortCompatibility;
//#if MC < 260200
import net.minecraft.util.Tuple;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional legacy Nyan-Work OMMC sorter; gated by bytecode signature probe. */
@Pseudo
@Mixin(targets="com.plusls.ommc.feature.sortInventory.SortInventoryUtil",remap=false)
/**
 * Guards legacy OMMC sorting through a verified bytecode signature.
 * 仅对字节码验证通过的旧版 OMMC 整理器注入。
 */
public abstract class SortInventoryHelperMixin {
    //#if MC < 260200
    @Inject(method="getSortRange",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void ddsTweak$range(AbstractContainerMenu menu,Slot hovered,
            CallbackInfoReturnable<Tuple<Integer,Integer>> cir){
        OmmcSortCompatibility.range(menu,hovered,cir);
    }

    @Inject(method="doClick",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void ddsTweak$clicks(Player player,int syncId,MultiPlayerGameMode gameMode,
            List<Integer> merges,List<Tuple<Integer,Integer>> swaps,CallbackInfo ci){
        OmmcSortCompatibility.clicks(player,syncId,gameMode,merges,swaps,ci);
    }
    //#endif
}
