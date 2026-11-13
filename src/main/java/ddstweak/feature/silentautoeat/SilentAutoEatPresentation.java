package ddstweak.feature.silentautoeat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Pure presentation projection for the temporary inventory state owned by Silent Auto Eat.
 *
 * <p>The upstream stack is whatever the active render chain resolved before DDS Tweak
 * participates. Non-target slots must preserve that value unchanged so other render
 * overlays can compose with this projection.</p>
 * 仅投影临时手持与快捷栏画面，不修改实际物品槽位。
 */
public final class SilentAutoEatPresentation {
    private SilentAutoEatPresentation(){}

    //#if MC == 12001
    //$$ private static final org.apache.logging.log4j.Logger LEGACY_12001_LOG=
    //$$         org.apache.logging.log4j.LogManager.getLogger("DDS Tweak Silent Auto Eat");
    //$$ private static String legacy12001OffhandDiagnostic;
    //$$ /** Diagnoses the inventory offhand *slot* separately from the player avatar preview. */
    //$$ public static void traceLegacy12001OffhandSlot(LocalPlayer player,Slot slot,
    //$$         ItemStack upstream,ItemStack presented){
    //$$     if(!LEGACY_12001_LOG.isDebugEnabled())return;
    //$$     if(!(net.minecraft.client.Minecraft.getInstance().screen
    //$$             instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)
    //$$             ||slot.getContainerSlot()!=40)return;
    //$$     String trace="active="+SilentAutoEat.isPresentationActive(player)+
    //$$             " playerOwned="+(slot.container==player.getInventory())+
    //$$             " menuContains="+player.containerMenu.slots.contains(slot)+
    //$$             " upstream="+ddstweak$legacy12001Stack(upstream)+
    //$$             " projected="+ddstweak$legacy12001Stack(presented);
    //$$     if(!trace.equals(legacy12001OffhandDiagnostic)){
    //$$         legacy12001OffhandDiagnostic=trace;
    //$$         LEGACY_12001_LOG.debug("[1.20.1] inventory offhand slot {}",trace);
    //$$     }
    //$$ }
    //$$ private static String ddstweak$legacy12001Stack(ItemStack stack){
    //$$     return stack.isEmpty()?"empty":stack.getItem().toString()+" x"+stack.getCount();
    //$$ }
    //#endif

    /** Keep selected mainhand food visible while its real slot is swapped with the offhand. */
    public static ItemStack projectMainhand(LocalPlayer player,ItemStack upstream){
        if(SilentAutoEat.isStagedMainhandVisualActive(player))
            return SilentAutoEat.stagedMainhandVisual(player);
        if(SilentAutoEat.isPresentationActive(player)&&SilentAutoEat.presentationUsesSelectedMainhand(player))
            return player.getOffhandItem();
        // In the protected-offhand MAIN_HAND route, real food is temporarily
        // selected. Only the first-person consumer sees the original item.
        if(SilentAutoEat.isMainhandVisualSourceActive(player))
            return SilentAutoEat.mainhandOriginalVisual(player);
        // The confirmed reverse SWAP can precede vanilla's held-item cache update.
        // Keep the restored real food visible only while the cached item is the old offhand.
        return SilentAutoEat.isStaleRestoredMainhandStack(player,upstream)
                ?player.getMainHandItem():upstream;
    }

    public static ItemStack projectOffhand(LocalPlayer player,ItemStack upstream){
        boolean owned=SilentAutoEat.isPresentationActive(player);
        //#if MC >= 260300
        //$$ owned|=SilentAutoEat.isOwned26HudSwap(player);
        //#endif
        return owned?SilentAutoEat.presentationOriginalOffhand():upstream;
    }

    //#if MC >= 260300
    //$$ /**
    //$$  * 26.3 evaluates the offhand HUD sprite's very existence from this
    //$$  * stack, before it extracts the item icon. Mask food from an originally
    //$$  * empty offhand in this initial condition, not only after swap ACK.
    //$$  */
    //$$ public static ItemStack projectHudOffhand26(LocalPlayer player,ItemStack upstream){
    //$$     if(SilentAutoEat.isOwned26HudSwap(player))
    //$$         return SilentAutoEat.presentationOriginalOffhand();
    //$$     // Inventory-only sources (9..35) use the earlier verified
    //$$     // presentation rule, not the newer hotbar swap-layout overlay.
    //$$     return projectOffhand(player,upstream);
    //$$ }
    //$$
    //$$ /** The slot icon and the offhand sprite must share one projection. */
    //$$ public static ItemStack projectHudSource26(LocalPlayer player,Inventory inventory,
    //$$                                                int index,ItemStack upstream){
    //$$     if(inventory==SilentAutoEatCompat.inventory(player)
    //$$             &&SilentAutoEat.isStagedMainhandVisualActive(player)
    //$$             &&index==SilentAutoEat.stagedMainhandSlot(player))
    //$$         return SilentAutoEat.stagedMainhandVisual(player);
    //$$     if(inventory==SilentAutoEatCompat.inventory(player)
    //$$             &&SilentAutoEat.isOwned26HudSwap(player)
    //$$             &&index==SilentAutoEat.presentationSourceIndex())
    //$$         return SilentAutoEat.hudFood26(player);
    //$$     return projectHotbarInventoryStack(player,inventory,index,upstream);
    //$$ }
    //#endif

    /** Project a hotbar source only when the queried inventory belongs to the local player. */
    public static ItemStack projectHotbarInventoryStack(LocalPlayer player,Inventory inventory,
                                                       int index,ItemStack upstream){
        return inventory==SilentAutoEatCompat.inventory(player)
                ?projectInventoryStack(player,index,upstream):upstream;
    }

    public static ItemStack projectInventoryStack(LocalPlayer player,int inventoryIndex,ItemStack upstream){
        return SilentAutoEat.isPresentationActive(player)
                &&SilentAutoEatRules.isOwnedSourceIndex(inventoryIndex,SilentAutoEat.presentationSourceIndex())
                ?player.getOffhandItem():upstream;
    }

    /** True only for a player-owned slot whose displayed stack can differ from its real stack. */
    public static boolean isPresentedContainerSlot(LocalPlayer player,Slot slot){
        if(!SilentAutoEat.isPresentationActive(player)
                ||!SilentAutoEatCompat.isSafeInventoryPresentation(
                        net.minecraft.client.Minecraft.getInstance(),player))return false;
        if(!player.containerMenu.slots.contains(slot))return false;
        int index=SilentAutoEatCompat.inventoryIndex(player,slot);
        return index==40||SilentAutoEatRules.isOwnedSourceIndex(index,SilentAutoEat.presentationSourceIndex());
    }

    public static ItemStack projectContainerStack(LocalPlayer player,Slot slot,ItemStack upstream){
        if(!SilentAutoEat.isPresentationActive(player)
                ||!SilentAutoEatCompat.isSafeInventoryPresentation(
                        net.minecraft.client.Minecraft.getInstance(),player))return upstream;
        if(!player.containerMenu.slots.contains(slot))return upstream;
        int inventoryIndex=SilentAutoEatCompat.inventoryIndex(player,slot);
        if(inventoryIndex<0)return upstream;
        if(inventoryIndex==40)return SilentAutoEat.presentationOriginalOffhand();
        return SilentAutoEatRules.isOwnedSourceIndex(inventoryIndex,SilentAutoEat.presentationSourceIndex())
                ?player.getOffhandItem():upstream;
    }
}
