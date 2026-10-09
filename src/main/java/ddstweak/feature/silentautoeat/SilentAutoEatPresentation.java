package ddstweak.feature.silentautoeat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Pure presentation projection for the temporary inventory state owned by Silent Auto Eat.
 *
 * <p>The upstream stack is whatever the active render chain resolved before DDS Tweak
 * participates. Non-target slots must preserve that value unchanged so other render
 * overlays can compose with this projection.</p>
 */
public final class SilentAutoEatPresentation {
    private SilentAutoEatPresentation(){}

    public static ItemStack projectOffhand(LocalPlayer player,ItemStack upstream){
        return SilentAutoEat.isPresentationActive(player)
                ?SilentAutoEat.presentationOriginalOffhand():upstream;
    }

    public static ItemStack projectInventoryStack(LocalPlayer player,int inventoryIndex,ItemStack upstream){
        return SilentAutoEat.isPresentationActive(player)
                &&SilentAutoEat.presentationSourceIndex()==inventoryIndex
                ?player.getOffhandItem():upstream;
    }

    public static ItemStack projectContainerStack(LocalPlayer player,Slot slot,ItemStack upstream){
        if(!SilentAutoEat.isPresentationActive(player))return upstream;
        int inventoryIndex=SilentAutoEatCompat.inventoryIndex(player,slot);
        if(inventoryIndex==40)return SilentAutoEat.presentationOriginalOffhand();
        return inventoryIndex==SilentAutoEat.presentationSourceIndex()?player.getOffhandItem():upstream;
    }
}
