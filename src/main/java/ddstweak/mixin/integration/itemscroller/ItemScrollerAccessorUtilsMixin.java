package ddstweak.mixin.integration.itemscroller;

import ddstweak.integration.dds.playerinventory.ItemScrollerDirectTransfer;
import ddstweak.integration.dds.playerinventory.PlayerInventoryKind;
import ddstweak.integration.dds.playerinventory.PlayerInventorySlotPolicy;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ItemScroller's final valid-slot click gateway. Vanilla/player clicks do not
 * traverse this optional class and remain unchanged.
 * 在 ItemScroller 最终点击入口校验统一背包槽位。
 */
@Pseudo
@Mixin(targets="fi.dy.masa.itemscroller.util.AccessorUtils",remap=false)
public abstract class ItemScrollerAccessorUtilsMixin {
    @Unique private static final boolean TRACE=Boolean.getBoolean("dds.playerinventory.trace");
    @Unique private static final org.apache.logging.log4j.Logger LOG=
            org.apache.logging.log4j.LogManager.getLogger("dds.playerinventory.dt");

    //#if MC >= 12108
    @Inject(method="handleMouseClick",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private static void ddsTweak$guardAutomation(AbstractContainerScreen<?> screen,Slot slot,
                                                   int slotId,int button,@Coerce Object action,CallbackInfo ci){
        if(!(screen.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu))return;
        UnifiedPlayerInventoryMirrorMenu menu=(UnifiedPlayerInventoryMirrorMenu)screen.getMenu();
        int actual=slot==null?-1:menu.slots.indexOf(slot);
        if(actual<0||actual!=slotId||!PlayerInventorySlotPolicy.allowsAutomaticClick(menu,actual)){
            if(TRACE)LOG.info("itemscroller_click_block slot={} actual={} action={} reason=slot_policy",
                    slotId,actual,action);
            ci.cancel();
            return;
        }
        String type=action instanceof Enum<?>?((Enum<?>)action).name():String.valueOf(action);
        if(!"QUICK_MOVE".equals(type)||menu.kind()!=PlayerInventoryKind.INVENTORY)return;
        // Do not invoke server-native QUICK_MOVE: it can choose equipment/offhand.
        // The planner explicitly addresses each allowed target slot via PICKUP.
        boolean moved=ItemScrollerDirectTransfer.moveSingle(menu,actual);
        if(TRACE)LOG.info("itemscroller_click_direct slot={} moved={}",actual,moved);
        ci.cancel();
    }
    //#endif
}
