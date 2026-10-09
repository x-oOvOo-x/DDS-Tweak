package ddstweak.integration.dds.qca;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Client-side vanilla QCA target classification. */
public final class DdsQcaItems {
    public enum MenuKind{SHULKER,ENDER_CHEST,CRAFTING_TABLE,STONECUTTER,ANVIL,LOOM,CARTOGRAPHY_TABLE,GRINDSTONE,SMITHING_TABLE,ENCHANTING_TABLE}
    private DdsQcaItems(){}

    public static MenuKind kindOf(ItemStack s){
        if(s==null||s.isEmpty())return null;
        if(isShulker(s))return MenuKind.SHULKER;
        if(isItem(s,Items.ENDER_CHEST))return MenuKind.ENDER_CHEST;
        if(isItem(s,Items.CRAFTING_TABLE))return MenuKind.CRAFTING_TABLE;
        if(isItem(s,Items.STONECUTTER))return MenuKind.STONECUTTER;
        if(isAnvil(s))return MenuKind.ANVIL;
        if(isItem(s,Items.LOOM))return MenuKind.LOOM;
        if(isItem(s,Items.CARTOGRAPHY_TABLE))return MenuKind.CARTOGRAPHY_TABLE;
        if(isItem(s,Items.GRINDSTONE))return MenuKind.GRINDSTONE;
        if(isItem(s,Items.SMITHING_TABLE))return MenuKind.SMITHING_TABLE;
        if(isItem(s,Items.ENCHANTING_TABLE))return MenuKind.ENCHANTING_TABLE;
        return null;
    }

    public static boolean isItem(ItemStack stack,Item item){
        if(stack==null)return false;
        //#if MC >= 11700
        return stack.is(item);
        //#else
        //$$ return stack.getItem()==item;
        //#endif
    }

    private static boolean isAnvil(ItemStack stack){
        //#if MC >= 11700
        return stack.is(net.minecraft.tags.ItemTags.ANVIL);
        //#else
        //$$ Item item=stack.getItem();
        //$$ return item==Items.ANVIL||item==Items.CHIPPED_ANVIL||item==Items.DAMAGED_ANVIL;
        //#endif
    }

    public static boolean isStorageHost(ItemStack s){return s!=null&&!s.isEmpty()&&(isShulker(s)||isItem(s,Items.ENDER_CHEST));}
    public static boolean isStorageHostCandidate(ItemStack s){return s!=null&&s.getCount()==1&&isStorageHost(s);}
    public static boolean isShulker(ItemStack s){
        if(s==null||!(s.getItem() instanceof BlockItem))return false;
        BlockItem item=(BlockItem)s.getItem();
        return Block.byItem(item)==item.getBlock()&&item.getBlock() instanceof ShulkerBoxBlock;
    }
    public static boolean sameItemAndData(ItemStack a,ItemStack b){
        //#if MC >= 12005
        return ItemStack.isSameItemSameComponents(a,b);
        //#elseif MC >= 11700
        //$$ return ItemStack.isSameItemSameTags(a,b);
        //#else
        //$$ return ItemStack.isSame(a,b)&&ItemStack.tagMatches(a,b);
        //#endif
    }
}
