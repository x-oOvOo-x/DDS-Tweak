package ddstweak.feature.dropconfirmation;

import net.minecraft.client.Minecraft;
//#if MC >= 12111
import net.minecraft.server.permissions.Permissions;
//#endif
import net.minecraft.world.item.CreativeModeTab;
//#if MC >= 11903
import net.minecraft.world.item.CreativeModeTabs;
//#else
//$$ import net.minecraft.core.NonNullList;
//#endif
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class CreativeTabCompat {
    private CreativeTabCompat(){}

    public static List<Tab> tabs(Minecraft minecraft){
        List<Tab> result=new ArrayList<>();
        if(minecraft==null)return Collections.singletonList(Tab.all(Collections.<Item>emptySet()));
        //#if MC >= 11903
        if(minecraft.level==null)return Collections.singletonList(Tab.all(Collections.<Item>emptySet()));
        //#if MC >= 12111
        boolean operator=minecraft.player!=null&&minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //#else
        //$$ boolean operator=minecraft.player!=null&&minecraft.player.hasPermissions(2);
        //#endif
        CreativeModeTabs.tryRebuildTabContents(minecraft.level.enabledFeatures(),operator,minecraft.level.registryAccess());
        for(CreativeModeTab tab:CreativeModeTabs.tabs()){
            if(tab.getType()!=CreativeModeTab.Type.CATEGORY||!tab.shouldDisplay())continue;
            add(result,title(tab),tab.getIconItem(),tab.getDisplayItems());
        }
        //#else
        //$$ for(CreativeModeTab tab:CreativeModeTab.TABS){
        //$$     if(tab==null||tab==CreativeModeTab.TAB_SEARCH||tab==CreativeModeTab.TAB_HOTBAR||tab==CreativeModeTab.TAB_INVENTORY)continue;
        //$$     NonNullList<ItemStack> stacks=NonNullList.create();
        //$$     tab.fillItemList(stacks);
        //$$     add(result,title(tab),tab.getIconItem(),stacks);
        //$$ }
        //#endif
        Set<Item> all=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
        for(Tab tab:result)all.addAll(tab.items);
        result.add(0,Tab.all(all));
        return result;
    }

    public static List<Tab> tabs(Minecraft minecraft,Collection<Item> candidates){
        Set<Item> allowed=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
        if(candidates!=null)allowed.addAll(candidates);

        List<Tab> result=new ArrayList<>();
        result.add(Tab.all(allowed));
        for(Tab tab:tabs(minecraft)){
            if(tab.isAll())continue;
            Set<Item> matching=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
            for(Item item:allowed)if(tab.contains(item))matching.add(item);
            if(!matching.isEmpty())result.add(new Tab(tab.title,tab.icon,matching,false));
        }
        return result;
    }

    private static String title(CreativeModeTab tab){
        //#if MC >= 11600
        return tab.getDisplayName().getString();
        //#else
        //$$ return tab.getName();
        //#endif
    }

    private static void add(List<Tab> result,String title,ItemStack icon,Collection<ItemStack> stacks){
        Set<Item> items=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
        for(ItemStack stack:stacks)if(stack!=null&&!stack.isEmpty())items.add(stack.getItem());
        if(!items.isEmpty())result.add(new Tab(title,icon,items,false));
    }

    public static final class Tab{
        private final String title;
        private final ItemStack icon;
        private final Set<Item> items;
        private final boolean all;

        private Tab(String title,ItemStack icon,Set<Item> items,boolean all){
            this.title=title;this.icon=icon;this.items=items;this.all=all;
        }

        private static Tab all(Set<Item> items){
            return new Tab("",ItemStack.EMPTY,items,true);
        }

        public String title(){return title;}
        public ItemStack icon(){return icon;}
        public boolean contains(Item item){return items.contains(item);}
        public boolean isAll(){return all;}
    }
}
