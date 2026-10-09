package ddstweak.feature.silentautoeat;

import ddstweak.mixin.integration.dds.qca.SlotContainerIndexAccessor;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
        if(!MinecraftCompat.carried(player.containerMenu,player).isEmpty())return false;
        Screen screen=MinecraftCompat.screen(minecraft);
        if(screen!=null&&!(screen instanceof AbstractContainerScreen))return false;
        if(screen==null&&player.containerMenu!=player.inventoryMenu)return false;
        return hasPlayerInventorySlots(player);
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

    static void swapWithOffhand(Minecraft minecraft,LocalPlayer player,AbstractContainerMenu menu,int menuSlot){
        if(minecraft.gameMode==null||menu==null)return;
        //#if MC >= 260100
        //$$ minecraft.gameMode.handleContainerInput(
        //$$         menu.containerId,menuSlot,40,ContainerInput.SWAP,player
        //$$ );
        //#else
        minecraft.gameMode.handleInventoryMouseClick(
                menu.containerId,menuSlot,40,ClickType.SWAP,player
        );
        //#endif
    }
}
