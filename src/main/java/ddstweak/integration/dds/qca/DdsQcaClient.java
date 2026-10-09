package ddstweak.integration.dds.qca;

import ddstweak.mixin.integration.dds.qca.CreativeSlotWrapperAccessor;
import ddstweak.mixin.integration.dds.qca.SlotContainerIndexAccessor;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Client-side QCA intent classification; server remains authoritative. */
public final class DdsQcaClient {
    public enum StorageDragMode{ABSORB,EXTRACT}
    private DdsQcaClient(){}
    public static boolean isCreativePickerScreen(){
        Object current=MinecraftCompat.screen(Minecraft.getInstance());
        if(!(current instanceof CreativeModeInventoryScreen))return false;
        CreativeModeInventoryScreen screen=(CreativeModeInventoryScreen)current;
        //#if MC >= 11904
        return !screen.isInventoryOpen();
        //#else
        //$$ for(Slot slot:screen.getMenu().slots)if(slot instanceof CreativeSlotWrapperAccessor)return false;
        //$$ return true;
        //#endif
    }
    public static boolean isCreativeSurvivalScreen(){return MinecraftCompat.screen(Minecraft.getInstance()) instanceof CreativeModeInventoryScreen&&!isCreativePickerScreen();}
    public static boolean canUseDedicatedOpenSlot(AbstractContainerMenu menu,Slot slot,Player player){if(!DdsQcaClientNetwork.canUseOpenSlot()||MinecraftCompat.screen(Minecraft.getInstance()) instanceof CreativeModeInventoryScreen||!safeSourceSlot(menu,slot,player))return false;ItemStack target=slot.getItem();return !target.isEmpty()&&target.getCount()==1&&DdsQcaItems.kindOf(target)!=null;}
    public static int creativeInventoryOpenSlot(Slot slot,Player player){if(!DdsQcaClientNetwork.canUseOpenInventorySlot()||!isCreativeSurvivalScreen())return -1;Inventory inv=MinecraftCompat.playerInventory(player);int i=creativeInventorySlot(slot,player);if(i<0||i>=inv.getContainerSize())return -1;ItemStack target=inv.getItem(i);return !target.isEmpty()&&target.getCount()==1&&DdsQcaItems.kindOf(target)!=null?i:-1;}
    public static int creativeInventoryStorageSlot(Slot slot,Player player,ItemStack carried){if(!DdsQcaClientNetwork.canUseCreativeStorage()||!isCreativeSurvivalScreen()||carried==null||carried.isEmpty())return -1;Inventory inv=MinecraftCompat.playerInventory(player);int i=creativeInventorySlot(slot,player);if(i<0||i>=inv.getContainerSize())return -1;ItemStack target=inv.getItem(i);if(DdsQcaItems.isStorageHostCandidate(carried))return !DdsQcaItems.isShulker(carried)||!DdsQcaItems.isShulker(target)||target.getCount()==1?i:-1;return DdsQcaItems.isStorageHostCandidate(target)?i:-1;}
    public static void applyCreativeCarried(AbstractContainerMenu menu,Player player,ItemStack stack){if(menu!=null&&player!=null&&isCreativeSurvivalScreen())MinecraftCompat.setCarried(menu,player,stack==null?ItemStack.EMPTY:stack);}
    public static boolean shouldSuppressPrediction(AbstractContainerMenu menu,int slotNum,int buttonNum,Object clickType,Player player){if(!DdsQcaClientNetwork.canUseStorageClick()||MinecraftCompat.screen(Minecraft.getInstance()) instanceof CreativeModeInventoryScreen||!"PICKUP".equals(enumName(clickType))||buttonNum!=1||slotNum<0||slotNum>=menu.slots.size()||isWorkstationOwnSlot(menu,menu.getSlot(slotNum),player))return false;Slot slot=menu.getSlot(slotNum);ItemStack target=slot.getItem(),carried=MinecraftCompat.carried(menu,player);if(carried.isEmpty()){if(target.isEmpty()||target.getCount()!=1)return false;return safeSourceSlot(menu,slot,player)&&DdsQcaItems.kindOf(target)!=null;}if(!canUseDedicatedStorageSlot(menu,slot,player))return false;return DdsQcaItems.isStorageHostCandidate(carried)?canUseDedicatedStorageInteraction(menu,slot,player,carried):DdsQcaItems.isStorageHostCandidate(target);}
    public static boolean canUseDedicatedStorageInteraction(AbstractContainerMenu menu,Slot slot,Player player,ItemStack carried){if(!canUseDedicatedStorageSlot(menu,slot,player)||!DdsQcaItems.isStorageHostCandidate(carried))return false;ItemStack target=slot.getItem();if(!DdsQcaItems.isStorageHost(target))return true;return !DdsQcaItems.isShulker(carried)||!DdsQcaItems.isShulker(target)||target.getCount()==1;}
    public static boolean canUseDedicatedStorageSlot(AbstractContainerMenu menu,Slot slot,Player player){return !isWorkstationOwnSlot(menu,slot,player)&&DdsQcaClientSlotAccessPolicy.canDirectStorage(menu,slot,player);}
    private static int creativeInventorySlot(Slot slot,Player player){
        if(slot==null||player==null)return -1;
        Inventory inv=MinecraftCompat.playerInventory(player);
        Slot backing=slot instanceof CreativeSlotWrapperAccessor?((CreativeSlotWrapperAccessor)slot).ddsTweak$getTarget():slot;
        if(backing.container!=inv||!backing.mayPickup(player))return -1;
        //#if MC >= 11700
        return backing.getContainerSlot();
        //#else
        //$$ return ((SlotContainerIndexAccessor)(Object)backing).ddsTweak$getContainerSlot();
        //#endif
    }
    private static boolean safeSourceSlot(AbstractContainerMenu menu,Slot slot,Player player){return !isWorkstationOwnSlot(menu,slot,player)&&DdsQcaClientSlotAccessPolicy.safeSourceSlot(menu,slot,player);}
    private static boolean isWorkstationOwnSlot(AbstractContainerMenu menu,Slot slot,Player player){
        if(menu==null||slot==null||player==null)return false;
        boolean workstation=menu instanceof CraftingMenu||menu instanceof StonecutterMenu||menu instanceof AnvilMenu||menu instanceof LoomMenu||menu instanceof GrindstoneMenu||menu instanceof EnchantmentMenu||menu instanceof CartographyTableMenu
                //#if MC >= 11600
                ||menu instanceof SmithingMenu
                //#endif
                ;
        return workstation&&slot.container!=MinecraftCompat.playerInventory(player);
    }
    private static String enumName(Object v){return v instanceof Enum<?>?((Enum<?>)v).name():String.valueOf(v);}
}
