package ddstweak.feature.dropconfirmation;

//#if MC > 11802
import net.minecraft.core.registries.BuiltInRegistries;
//#else
//$$ import net.minecraft.core.Registry;
//#endif
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

public final class ItemRegistryCompat {
    private ItemRegistryCompat(){}

    public static List<Item> allItems(){
        List<Item> items=new ArrayList<>();
        //#if MC > 11802
        for(Item item:BuiltInRegistries.ITEM)if(item!=Items.AIR)items.add(item);
        //#else
        //$$ for(Item item:Registry.ITEM)if(item!=Items.AIR)items.add(item);
        //#endif
        items.sort(Comparator.comparing(ItemRegistryCompat::id));
        return items;
    }

    public static String id(Item item){
        if(item==null)return "minecraft:air";
        //#if MC > 11802
        return String.valueOf(BuiltInRegistries.ITEM.getKey(item));
        //#else
        //$$ return String.valueOf(Registry.ITEM.getKey(item));
        //#endif
    }

    public static ItemStack stack(Item item){
        if(item==null||item==Items.AIR)return ItemStack.EMPTY;
        try{return new ItemStack(item);}catch(Throwable ignored){return ItemStack.EMPTY;}
    }

    public static String name(Item item){
        ItemStack stack=stack(item);
        if(stack.isEmpty())return id(item);
        try{return stack.getHoverName().getString();}catch(Throwable ignored){return id(item);}
    }

}
