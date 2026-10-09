package ddstweak.feature.silentautoeat;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SilentAutoEatFoodRules {
    private SilentAutoEatFoodRules(){}

    public static boolean isEdible(Item item){
        if(item==null)return false;
        try{return isEdible(new ItemStack(item));}
        catch(Throwable ignored){return false;}
    }

    public static boolean isEdible(ItemStack stack){
        return stack!=null&&SilentAutoEatCompat.isFood(stack);
    }

    public static boolean isAllowed(ItemStack stack,Iterable<String> blacklist){
        if(!isEdible(stack))return false;
        String id=SilentAutoEatCompat.itemId(stack);
        if(blacklist!=null)for(String blocked:blacklist)
            if(blocked!=null&&id.equals(blocked.trim()))return false;
        return true;
    }

    static boolean sameStackIgnoringCount(ItemStack first,ItemStack second){
        if(first==null||second==null)return false;
        if(first.isEmpty()||second.isEmpty())return first.isEmpty()&&second.isEmpty();
        ItemStack a=first.copy(),b=second.copy();
        a.setCount(1);b.setCount(1);
        return ItemStack.matches(a,b);
    }
}
