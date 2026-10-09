package ddstweak.mixin.integration.itemscroller;

import ddstweak.integration.dds.playerinventory.*;
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.integration.dds.playerinventory.screen.UnifiedPlayerInventoryScreen;
import ddstweak.mixin.fakeplayer.AbstractContainerScreenAccessor;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Keeps Item Scroller sorting inside the unified target region and off the viewer hotbar. */
@Pseudo
@Mixin(targets="fi.dy.masa.itemscroller.util.InventoryUtils",remap=false)
public abstract class InventoryUtilsMixin{
    private static final boolean TRACE=Boolean.getBoolean("dds.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger TRACE_LOG=
            org.apache.logging.log4j.LogManager.getLogger("dds.playerinventory.dt");
    @ModifyArg(method="sortInventory",at=@At(value="INVOKE",target="Lfi/dy/masa/itemscroller/util/InventoryUtils;tryMergeItems(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;II)V"),index=1,require=0,remap=false)
    private static int ddsTweak$mergeStart(int original){
        UnifiedPlayerInventoryMirrorMenu menu=ddsTweak$menu();int slot=ddsTweak$menuSlot();
        if(menu==null)return original;
        int start=ddsTweak$rangeStart(menu,slot,original),end=ddsTweak$rangeEnd(menu,slot,-1);
        if(TRACE)TRACE_LOG.info("itemscroller_range phase=merge hover="+slot+" role="+menu.slotRole(slot)
                +" start="+start+" end="+end);
        return start;
    }
    @ModifyArg(method="sortInventory",at=@At(value="INVOKE",target="Lfi/dy/masa/itemscroller/util/InventoryUtils;tryMergeItems(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;II)V"),index=2,require=0,remap=false)
    private static int ddsTweak$mergeEnd(int original){UnifiedPlayerInventoryMirrorMenu menu=ddsTweak$menu();int slot=ddsTweak$menuSlot();int end=menu==null?-1:ddsTweak$rangeEnd(menu,slot,-1);return end<0?original:end-1;}
    @ModifyVariable(method="trySort",at=@At("HEAD"),argsOnly=true,ordinal=0,require=0,remap=false)
    private static int ddsTweak$sortStart(int original){UnifiedPlayerInventoryMirrorMenu menu=ddsTweak$menu();int slot=ddsTweak$menuSlot();return menu==null?original:ddsTweak$rangeStart(menu,slot,original);}
    @ModifyVariable(method="trySort",at=@At("HEAD"),argsOnly=true,ordinal=1,require=0,remap=false)
    private static int ddsTweak$sortEnd(int original){UnifiedPlayerInventoryMirrorMenu menu=ddsTweak$menu();int slot=ddsTweak$menuSlot();return menu==null?original:ddsTweak$rangeEnd(menu,slot,original);}
    @ModifyVariable(method="trySort",at=@At("HEAD"),argsOnly=true,ordinal=2,require=0,remap=false)
    private static int ddsTweak$sortScratch(int original){return ddsTweak$scratchButton(original);}
    //#if MC >= 260102
    //$$ @ModifyArg(method="quickSort",at=@At(value="INVOKE",target="Lfi/dy/masa/itemscroller/util/InventoryUtils;clickSlot(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;IILnet/minecraft/world/inventory/ContainerInput;)V"),index=2,require=0,remap=false)
    //$$ private static int ddsTweak$quickSortScratch(int original){return ddsTweak$scratchButton(original);}
    //#else
    @ModifyArg(method="quickSort",at=@At(value="INVOKE",target="Lfi/dy/masa/itemscroller/util/InventoryUtils;clickSlot(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;IILnet/minecraft/world/inventory/ClickType;)V"),index=2,require=0,remap=false)
    private static int ddsTweak$quickSortScratch(int original){return ddsTweak$scratchButton(original);}
    //#endif

    /*
     * ItemScroller's Slot-based click helper receives both a Slot object and a numeric
     * slot id. On unified mirrors the backing container index is not necessarily the
     * menu id. Normalize at the single click choke point instead of patching individual
     * callers such as addStackTo(), so all Slot-based ItemScroller operations agree.
     */
    //#if MC >= 260102
    //$$ @ModifyArgs(method="clickSlot(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",at=@At(value="INVOKE",target="Lfi/dy/masa/itemscroller/util/AccessorUtils;handleMouseClick(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V"),require=0,remap=false)
    //$$ private static void ddsTweak$normalizeUnifiedSlotId(Args args){ddsTweak$normalizeUnifiedSlotId0(args);}
    //#else
    @ModifyArgs(method="clickSlot(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V",at=@At(value="INVOKE",target="Lfi/dy/masa/itemscroller/util/AccessorUtils;handleMouseClick(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V"),require=0,remap=false)
    private static void ddsTweak$normalizeUnifiedSlotId(Args args){ddsTweak$normalizeUnifiedSlotId0(args);}
    //#endif
    private static void ddsTweak$normalizeUnifiedSlotId0(Args args){
        Object guiObject=args.get(0),slotObject=args.get(1);
        if(!(guiObject instanceof AbstractContainerScreen<?>)||!(slotObject instanceof Slot))return;
        AbstractContainerScreen<?> gui=(AbstractContainerScreen<?>)guiObject;
        if(!(gui.getMenu() instanceof UnifiedPlayerInventoryMirrorMenu))return;
        Slot slot=(Slot)slotObject;
        int supplied=(Integer)args.get(2),actual=gui.getMenu().slots.indexOf(slot);
        if(actual<0)return;
        if(TRACE&&supplied!=actual){
            UnifiedPlayerInventoryMirrorMenu menu=(UnifiedPlayerInventoryMirrorMenu)gui.getMenu();
            TRACE_LOG.info("itemscroller_slot_normalize supplied="+supplied+" actual="+actual
                    +" role="+menu.slotRole(actual));
        }
        args.set(2,actual);
    }

    private static int ddsTweak$scratchButton(int original){return ddsTweak$useCarriedScratch()?UnifiedPlayerInventoryMirrorMenu.CARRIED_SCRATCH_BUTTON:original;}
    private static int ddsTweak$rangeStart(UnifiedPlayerInventoryMirrorMenu menu,int slot,int fallback){switch(menu.slotRole(slot)){case MAIN:case ENDER_STORAGE:return menu.targetMainStart();case HOTBAR:return menu.targetHotbarStart();case VIEWER_MAIN:case VIEWER_HOTBAR:return fallback;default:return 0;}}
    private static int ddsTweak$rangeEnd(UnifiedPlayerInventoryMirrorMenu menu,int slot,int fallback){switch(menu.slotRole(slot)){case MAIN:case ENDER_STORAGE:return menu.targetMainEnd();case HOTBAR:return menu.targetHotbarEnd();case VIEWER_MAIN:case VIEWER_HOTBAR:return fallback;default:return 0;}}
    private static boolean ddsTweak$useCarriedScratch(){UnifiedPlayerInventoryMirrorMenu menu=ddsTweak$menu();return menu!=null&&ddsTweak$hoveredTargetSlot()>=0&&menu.context().allows(PlayerInventoryInteractionCapability.CARRIED_SCRATCH_SWAP);}
    private static int ddsTweak$hoveredTargetSlot(){UnifiedPlayerInventoryMirrorMenu menu=ddsTweak$menu();if(menu==null)return -1;int slot=ddsTweak$menuSlot();PlayerInventorySlotRole role=menu.slotRole(slot);return role==PlayerInventorySlotRole.MAIN||role==PlayerInventorySlotRole.HOTBAR||role==PlayerInventorySlotRole.ENDER_STORAGE?slot:-1;}
    private static UnifiedPlayerInventoryMirrorMenu ddsTweak$menu(){Object screen=MinecraftCompat.screen(Minecraft.getInstance());return screen instanceof UnifiedPlayerInventoryScreen?((UnifiedPlayerInventoryScreen)screen).getMenu():null;}
    private static int ddsTweak$menuSlot(){Object screen=MinecraftCompat.screen(Minecraft.getInstance());if(!(screen instanceof AbstractContainerScreen<?>))return -1;AbstractContainerScreen<?> gui=(AbstractContainerScreen<?>)screen;Slot slot=((AbstractContainerScreenAccessor)(Object)gui).ddsTweak$getHoveredSlot();return slot==null?-1:gui.getMenu().slots.indexOf(slot);}
}
