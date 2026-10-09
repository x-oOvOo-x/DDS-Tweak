package ddstweak.feature.dropconfirmation;

import ddstweak.config.Configs;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DropConfirmationGuard {
    private static final long WINDOW_NS=1_000_000_000L;
    private static Pending pending;

    private DropConfirmationGuard(){}

    /**
     * @return true when the current world-drop should be cancelled and wait for a second press.
     */
    public static boolean shouldCancel(LocalPlayer player,boolean entireStack){
        Minecraft minecraft=Minecraft.getInstance();
        if(player==null||!Configs.Tweaks.CONFIRM_ITEM_DROP.getBooleanValue()||MinecraftCompat.screen(minecraft)!=null){
            clear(minecraft);
            return false;
        }

        ItemStack stack=MinecraftCompat.selectedItem(player);
        if(stack.isEmpty()||!requiresConfirmation(stack)){
            clear(minecraft);
            return false;
        }

        long now=System.nanoTime();
        Item item=stack.getItem();
        int count=stack.getCount();

        if(pending!=null&&pending.matches(stack,item,count,entireStack,now)){
            pending=null;
            MinecraftCompat.overlayMessage(minecraft,MinecraftCompat.literal(""));
            return false;
        }

        pending=new Pending(stack,item,count,entireStack,now+WINDOW_NS);
        String key=MinecraftCompat.dropKeyName(minecraft);
        MinecraftCompat.overlayMessage(
                minecraft,
                entireStack
                        ?MinecraftCompat.translatable("dds-tweak.drop_confirm.prompt.stack",key)
                        :MinecraftCompat.translatable("dds-tweak.drop_confirm.prompt.single",key)
        );
        return true;
    }

    public static void reset(){pending=null;}

    private static boolean requiresConfirmation(ItemStack stack){
        DropConfirmMode mode=(DropConfirmMode)Configs.Tweaks.DROP_CONFIRM_MODE.getOptionListValue();
        if(mode==DropConfirmMode.BLACKLIST)return DropConfirmationItems.blacklistContains(stack.getItem());
        if(mode==DropConfirmMode.WHITELIST)return !DropConfirmationItems.whitelistContains(stack.getItem());
        return true;
    }

    private static void clear(Minecraft minecraft){
        if(pending!=null){
            pending=null;
            MinecraftCompat.overlayMessage(minecraft,MinecraftCompat.literal(""));
        }
    }

    private static final class Pending{
        final ItemStack stack;
        final int count;
        final Item item;
        final boolean entireStack;
        final long expiresAt;

        Pending(ItemStack stack,Item item,int count,boolean entireStack,long expiresAt){
            this.stack=stack;this.item=item;this.count=count;this.entireStack=entireStack;this.expiresAt=expiresAt;
        }

        boolean matches(ItemStack stack,Item item,int count,boolean entireStack,long now){
            return now<=expiresAt&&this.stack==stack&&this.item==item&&this.count==count&&this.entireStack==entireStack;
        }
    }
}
