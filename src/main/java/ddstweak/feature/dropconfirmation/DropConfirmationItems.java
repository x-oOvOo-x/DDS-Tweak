package ddstweak.feature.dropconfirmation;

import ddstweak.config.Configs;
import fi.dy.masa.malilib.config.IConfigStringList;
import net.minecraft.world.item.Item;

public final class DropConfirmationItems {
    private DropConfirmationItems(){}

    private static boolean contains(IConfigStringList list,Item item){return list.getStrings().contains(ItemRegistryCompat.id(item));}
    public static boolean blacklistContains(Item item){return contains(Configs.Tweaks.DROP_CONFIRM_BLACKLIST,item);}
    public static boolean whitelistContains(Item item){return contains(Configs.Tweaks.DROP_CONFIRM_WHITELIST,item);}
}
