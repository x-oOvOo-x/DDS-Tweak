package ddstweak.mixin.integration.ommc;

import ddstweak.integration.ommc.OmmcSortCompatibility;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/**
 * AyakaCraft v0.8+: sorter uses com.plusls.ommc.util.Tuple rather than
 * removed net.minecraft.util.Tuple. This injection never links OMMC classes.
 */
@Pseudo
@Mixin(targets="com.plusls.ommc.impl.feature.sortInventory.SortInventoryHelper",remap=false)
public abstract class SortInventoryModernTupleAdapterMixin {
    @Inject(method="getSortRange",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void ddsTweak$range(AbstractContainerMenu menu,Slot hovered,
            CallbackInfoReturnable<Object> cir){
        OmmcSortCompatibility.rangeTupleAdapter(menu,hovered,cir);
    }

    @Inject(method="doClick",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void ddsTweak$clicks(Player player,int syncId,MultiPlayerGameMode gameMode,
            List<Integer> merges,List<?> swaps,CallbackInfo ci){
        OmmcSortCompatibility.clicksTupleAdapter(player,syncId,gameMode,merges,swaps,ci);
    }
}
