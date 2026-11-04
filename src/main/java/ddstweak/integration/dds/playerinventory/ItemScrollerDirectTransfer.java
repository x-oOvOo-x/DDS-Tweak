package ddstweak.integration.dds.playerinventory;

//#if MC >= 12108
import ddstweak.integration.dds.playerinventory.menu.UnifiedPlayerInventoryMirrorMenu;
import ddstweak.platform.minecraft.MinecraftCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//#if MC >= 260102
//$$ import net.minecraft.world.inventory.ContainerInput;
//#else
import net.minecraft.world.inventory.ClickType;
//#endif

/**
 * Direct, explicitly addressed ItemScroller transfers on GCA/Org mirror menus.
 * One gesture plans all PICKUP slots before sending any packets. The native
 * QUICK_MOVE auto-equip/offhand path is never used here.
 */
public final class ItemScrollerDirectTransfer {
    private static final boolean TRACE=Boolean.getBoolean("dds.playerinventory.trace");
    private static final org.apache.logging.log4j.Logger LOG=
            org.apache.logging.log4j.LogManager.getLogger("dds.playerinventory.dt");
    private static UnifiedPlayerInventoryMirrorMenu guardedDragMenu;

    private ItemScrollerDirectTransfer(){}

    public static boolean isMatchingGesture(Object action,boolean initialClick){
        return initialClick&&action instanceof Enum<?>
                &&"MOVE_TO_OTHER_MATCHING".equals(((Enum<?>)action).name());
    }

    public static boolean suppressDrag(UnifiedPlayerInventoryMirrorMenu menu){
        return guardedDragMenu==menu;
    }

    public static void stopDragging(){guardedDragMenu=null;}

    public static void cancelOnManualClick(UnifiedPlayerInventoryMirrorMenu menu){
        if(guardedDragMenu==menu)guardedDragMenu=null;
    }

    /** True means the gesture was consumed, including safe no-op/fail-closed. */
    public static boolean moveMatching(UnifiedPlayerInventoryMirrorMenu menu,int hoveredId){
        guardedDragMenu=menu;
        return move(menu,hoveredId,true);
    }

    /** Called from ItemScroller's QUICK_MOVE gateway for non-matching actions. */
    public static boolean moveSingle(UnifiedPlayerInventoryMirrorMenu menu,int sourceId){
        return move(menu,sourceId,false);
    }

    private static boolean move(UnifiedPlayerInventoryMirrorMenu menu,int hoveredId,boolean matching){
        Minecraft mc=Minecraft.getInstance();
        if(menu==null||menu.kind()!=PlayerInventoryKind.INVENTORY
                ||mc.player==null||mc.gameMode==null||mc.player.containerMenu!=menu
                ||!(MinecraftCompat.screen(mc) instanceof AbstractContainerScreen<?>)
                ||((AbstractContainerScreen<?>)MinecraftCompat.screen(mc)).getMenu()!=menu
                ||!MinecraftCompat.carried(menu,mc.player).isEmpty()
                ||!PlayerInventorySlotPolicy.allowsAutomaticClick(menu,hoveredId))return false;
        List<Integer> destinations=PlayerInventorySlotPolicy.directMoveDestinations(menu,hoveredId);
        if(destinations.isEmpty())return false;
        Slot hovered=menu.getSlot(hoveredId);
        ItemStack reference=hovered.getItem();
        if(reference.isEmpty()||!hovered.mayPickup(mc.player))return false;

        boolean fromViewer=PlayerInventorySlotPolicy.isViewerToTarget(menu,hoveredId);
        List<ItemScrollerMovePlanner.SlotState> sources=new ArrayList<>();
        if(matching){
            for(int id=0;id<menu.slots.size();id++){
                if(fromViewer!=PlayerInventorySlotPolicy.isViewerToTarget(menu,id))continue;
                if(!fromViewer&&!PlayerInventorySlotPolicy.isTargetStorageRole(menu.kind(),menu.slotRole(id)))continue;
                appendSource(sources,menu.getSlot(id),id,reference,mc);
            }
        }else{
            appendSource(sources,hovered,hoveredId,reference,mc);
        }
        if(sources.isEmpty())return false;

        List<ItemScrollerMovePlanner.SlotState> targets=new ArrayList<>();
        for(int id:destinations){
            Slot slot=menu.getSlot(id);
            ItemStack current=slot.getItem();
            if(!slot.mayPlace(reference)||!current.isEmpty()&&!sameStack(current,reference))continue;
            int capacity=Math.min(reference.getMaxStackSize(),slot.getMaxStackSize(reference));
            if(capacity<current.getCount())continue;
            targets.add(new ItemScrollerMovePlanner.SlotState(id,current.getCount(),capacity,true));
        }
        ItemScrollerMovePlanner.Plan plan=ItemScrollerMovePlanner.plan(sources,targets);
        if(plan==null||plan.clicks.isEmpty()){
            if(TRACE)LOG.info("itemscroller_direct_noop hovered={} matching={}",hoveredId,matching);
            return false;
        }

        // Re-check preflight once against the same menu; no guessed slot IDs.
        if(!matchesCounts(menu,plan.before,reference)||!MinecraftCompat.carried(menu,mc.player).isEmpty())
            return false;
        if(TRACE)LOG.info("itemscroller_direct_begin hovered={} matching={} moved={} clicks={}",
                hoveredId,matching,plan.moved,plan.clicks.size());
        for(int id:plan.clicks){
            if(mc.player.containerMenu!=menu||
                    !(MinecraftCompat.screen(mc) instanceof AbstractContainerScreen<?>)||
                    ((AbstractContainerScreen<?>)MinecraftCompat.screen(mc)).getMenu()!=menu){
                if(TRACE)LOG.warn("itemscroller_direct_abort reason=menu_changed");
                return false;
            }
            //#if MC >= 260102
            //$$ mc.gameMode.handleContainerInput(menu.containerId,id,0,ContainerInput.PICKUP,mc.player);
            //#else
            mc.gameMode.handleInventoryMouseClick(menu.containerId,id,0,ClickType.PICKUP,mc.player);
            //#endif
        }
        boolean predicted=matchesCounts(menu,plan.after,reference)
                &&MinecraftCompat.carried(menu,mc.player).isEmpty();
        if(TRACE)LOG.info("itemscroller_direct_complete moved={} clicks={} predicted={}",
                plan.moved,plan.clicks.size(),predicted);
        if(!predicted)LOG.warn("ItemScroller direct transfer local prediction diverged; wait for server inventory sync");
        return predicted;
    }

    private static void appendSource(List<ItemScrollerMovePlanner.SlotState> sources,
                                     Slot slot,int id,ItemStack reference,Minecraft mc){
        ItemStack stack=slot.getItem();
        if(stack.isEmpty()||!sameStack(stack,reference)||!slot.mayPickup(mc.player)
                ||!slot.mayPlace(stack)||stack.getCount()>slot.getMaxStackSize(stack))return;
        sources.add(new ItemScrollerMovePlanner.SlotState(id,stack.getCount(),
                Math.max(stack.getCount(),slot.getMaxStackSize(stack)),true));
    }

    private static boolean matchesCounts(UnifiedPlayerInventoryMirrorMenu menu,
                                         Map<Integer,Integer> counts,ItemStack reference){
        for(Map.Entry<Integer,Integer> entry:counts.entrySet()){
            ItemStack actual=menu.getSlot(entry.getKey()).getItem();
            if(actual.getCount()!=entry.getValue())return false;
            if(!actual.isEmpty()&&!sameStack(actual,reference))return false;
        }
        return true;
    }

    private static boolean sameStack(ItemStack left,ItemStack right){
        return ItemStack.isSameItemSameComponents(left,right);
    }
}
//#else
//$$ public final class ItemScrollerDirectTransfer {
//$$     private ItemScrollerDirectTransfer(){}
//$$ }
//#endif
