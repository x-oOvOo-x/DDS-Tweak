package ddstweak.feature.silentautoeat;

import ddstweak.mixin.integration.dds.qca.SlotContainerIndexAccessor;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//#if MC >= 260100
//$$ import net.minecraft.world.inventory.ContainerInput;
//#else
import net.minecraft.world.inventory.ClickType;
//#endif
//#if MC >= 12005
import net.minecraft.core.component.DataComponents;
//#endif
//#if MC >= 11903
import net.minecraft.core.registries.BuiltInRegistries;
//#else
//$$ import net.minecraft.core.Registry;
//#endif

final class SilentAutoEatCompat {
    private SilentAutoEatCompat(){}

    static Inventory inventory(LocalPlayer player){
        //#if MC >= 11700
        return player.getInventory();
        //#else
        //$$ return player.inventory;
        //#endif
    }

    static int selectedSlot(Inventory inventory){
        //#if MC >= 12105
        return inventory.getSelectedSlot();
        //#else
        //$$ return inventory.selected;
        //#endif
    }

    /** Selects only the hotbar index; never changes the ItemStack stored in it. */
    static void selectHotbar(Inventory inventory,int slot){
        if(slot<0||slot>=9)throw new IllegalArgumentException("Invalid hotbar slot: "+slot);
        //#if MC >= 12105
        inventory.setSelectedSlot(slot);
        //#else
        //$$ inventory.selected=slot;
        //#endif
    }

    static void useMainhand(Minecraft minecraft,LocalPlayer player){
        if(minecraft.gameMode==null)return;
        //#if MC >= 11900
        minecraft.gameMode.useItem(player,InteractionHand.MAIN_HAND);
        //#else
        //$$ minecraft.gameMode.useItem(player,minecraft.level,InteractionHand.MAIN_HAND);
        //#endif
    }

    static boolean isFood(ItemStack stack){
        if(stack.isEmpty())return false;
        //#if MC >= 12102
        return stack.get(DataComponents.FOOD)!=null&&stack.get(DataComponents.CONSUMABLE)!=null;
        //#elseif MC >= 12005
        //$$ return stack.get(DataComponents.FOOD)!=null;
        //#else
        //$$ return stack.getItem().isEdible();
        //#endif
    }

    static String itemId(ItemStack stack){
        //#if MC >= 11903
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        //#else
        //$$ return Registry.ITEM.getKey(stack.getItem()).toString();
        //#endif
    }

    static boolean isSafeAutomationContext(Minecraft minecraft,LocalPlayer player){
        if(minecraft==null||player==null||player.containerMenu==null)return false;
        Screen screen=MinecraftCompat.screen(minecraft);
        if(!SilentAutoEatRules.isSafeInventoryContext(
                player.containerMenu==player.inventoryMenu,
                screen==null,screen!=null&&screen.getClass()==InventoryScreen.class))return false;
        if(!MinecraftCompat.carried(player.containerMenu,player).isEmpty())return false;
        return hasPlayerInventorySlots(player);
    }

    /**
     * A nested QCA/container screen may still render slots backed by the local
     * player's Inventory. Keep those slots projected during a pending restore,
     * even when the cursor is occupied or the menu prevents a reverse SWAP.
     * This never authorizes automation or projection of foreign container slots.
     */
    static boolean isSafeInventoryPresentation(Minecraft minecraft,LocalPlayer player){
        return minecraft!=null&&player!=null&&player.containerMenu!=null
                &&MinecraftCompat.screen(minecraft) instanceof AbstractContainerScreen
                &&hasPlayerInventorySlots(player);
    }

    static int menuSlot(LocalPlayer player,int inventoryIndex){
        if(player==null||inventoryIndex<0||inventoryIndex>=36||player.containerMenu==null)return -1;
        AbstractContainerMenu menu=player.containerMenu;
        for(int i=0;i<menu.slots.size();i++)
            if(inventoryIndex(player,menu.slots.get(i))==inventoryIndex)return i;
        return -1;
    }

    static int inventoryIndex(LocalPlayer player,Slot slot){
        if(player==null||slot==null||slot.container!=inventory(player))return -1;
        int index=containerSlot(slot);
        return index>=0&&index<=40?index:-1;
    }

    private static boolean hasPlayerInventorySlots(LocalPlayer player){
        for(Slot slot:player.containerMenu.slots){
            int index=inventoryIndex(player,slot);
            if(index>=0&&index<36)return true;
        }
        return false;
    }

    private static int containerSlot(Slot slot){
        //#if MC >= 11700
        return slot.getContainerSlot();
        //#else
        //$$ return ((SlotContainerIndexAccessor)(Object)slot).ddsTweak$getContainerSlot();
        //#endif
    }

    static void useOffhand(Minecraft minecraft,LocalPlayer player){
        if(minecraft.gameMode==null)return;
        //#if MC >= 11900
        minecraft.gameMode.useItem(player,InteractionHand.OFF_HAND);
        //#else
        //$$ minecraft.gameMode.useItem(player,minecraft.level,InteractionHand.OFF_HAND);
        //#endif
    }

    /** Real vanilla menu SWAP, with the hotbar button number (0..8), not 40. */
    static boolean swapWithHotbar(Minecraft minecraft,LocalPlayer player,
                                  AbstractContainerMenu menu,int sourceMenuSlot,int scratchHotbar){
        if(minecraft.gameMode==null||menu!=player.inventoryMenu
                ||sourceMenuSlot<0||sourceMenuSlot>=menu.slots.size()
                ||scratchHotbar<0||scratchHotbar>=9
                ||!MinecraftCompat.carried(menu,player).isEmpty())return false;
        //#if MC >= 260100
        //$$ minecraft.gameMode.handleContainerInput(
        //$$         menu.containerId,sourceMenuSlot,scratchHotbar,ContainerInput.SWAP,player);
        //#else
        minecraft.gameMode.handleInventoryMouseClick(
                menu.containerId,sourceMenuSlot,scratchHotbar,ClickType.SWAP,player);
        //#endif
        return true;
    }

    static void swapWithOffhand(Minecraft minecraft,LocalPlayer player,AbstractContainerMenu menu,int menuSlot){
        if(minecraft.gameMode==null||menu==null)return;
        //#if MC >= 260100
        //$$ minecraft.gameMode.handleContainerInput(
        //$$         menu.containerId,menuSlot,40,ContainerInput.SWAP,player
        //$$ );
        //#elseif MC == 11404
        //$$ // In 1.14.4 the offhand-key SWAP click may be ignored in the inventory menu.
        //$$ // Use the three vanilla left-click actions instead; never change Inventory directly.
        //$$ if(menu!=player.inventoryMenu||menuSlot<0||menuSlot>=menu.slots.size()
        //$$         ||!MinecraftCompat.carried(menu,player).isEmpty())return;
        //$$ int offhandSlot=-1;
        //$$ for(int i=0;i<menu.slots.size();i++)
        //$$     if(inventoryIndex(player,menu.slots.get(i))==40){offhandSlot=i;break;}
        //$$ if(offhandSlot<0||offhandSlot==menuSlot)return;
        //$$ Slot source=menu.slots.get(menuSlot),offhand=menu.slots.get(offhandSlot);
        //$$ ItemStack sourceStack=source.getItem(),offhandStack=offhand.getItem();
        //$$ if(!source.mayPickup(player)||!offhand.mayPickup(player)
        //$$         ||(!offhandStack.isEmpty()&&!source.mayPlace(offhandStack))
        //$$         ||(!sourceStack.isEmpty()&&!offhand.mayPlace(sourceStack)))return;
        //$$ minecraft.gameMode.handleInventoryMouseClick(menu.containerId,menuSlot,0,ClickType.PICKUP,player);
        //$$ minecraft.gameMode.handleInventoryMouseClick(menu.containerId,offhandSlot,0,ClickType.PICKUP,player);
        //$$ minecraft.gameMode.handleInventoryMouseClick(menu.containerId,menuSlot,0,ClickType.PICKUP,player);
        //#else
        minecraft.gameMode.handleInventoryMouseClick(
                menu.containerId,menuSlot,40,ClickType.SWAP,player
        );
        //#endif
    }
}
