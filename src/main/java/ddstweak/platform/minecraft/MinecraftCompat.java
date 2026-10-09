package ddstweak.platform.minecraft;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
//#if MC >= 11900
import net.minecraft.network.chat.contents.TranslatableContents;
//#endif
//#if MC >= 12103
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
//#endif
//#if MC < 11900
//$$ import net.minecraft.network.chat.TextComponent;
//$$ import net.minecraft.network.chat.TranslatableComponent;
//#endif
//#if MC >= 12111
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import java.util.UUID;

public final class MinecraftCompat {
    private MinecraftCompat(){}

    //#if MC >= 12111
    public static Identifier id(String namespace,String path){return Identifier.fromNamespaceAndPath(namespace,path);}
    //#elseif MC >= 12101
    //$$ public static ResourceLocation id(String namespace,String path){return ResourceLocation.fromNamespaceAndPath(namespace,path);}
    //#else
    //$$ public static ResourceLocation id(String namespace,String path){return new ResourceLocation(namespace,path);}
    //#endif

    public static Component literal(String text){
        //#if MC >= 11900
        return Component.literal(text);
        //#else
        //$$ return new TextComponent(text);
        //#endif
    }

    public static Component translatable(String key,Object...args){
        //#if MC >= 11900
        return Component.translatable(key,args);
        //#else
        //$$ return new TranslatableComponent(key,args);
        //#endif
    }

    public static Component grayLiteral(String text){
        //#if MC >= 11900
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
        //#else
        //$$ return new TextComponent(text).withStyle(ChatFormatting.GRAY);
        //#endif
    }

    public static Component grayTranslatable(String key,Object...args){
        //#if MC >= 11900
        return Component.translatable(key,args).withStyle(ChatFormatting.GRAY);
        //#else
        //$$ return new TranslatableComponent(key,args).withStyle(ChatFormatting.GRAY);
        //#endif
    }

    public static String translationKey(Component component){
        if(component==null)return null;
        //#if MC >= 11900
        return component.getContents() instanceof TranslatableContents?((TranslatableContents)component.getContents()).getKey():null;
        //#else
        //$$ return component instanceof TranslatableComponent?((TranslatableComponent)component).getKey():null;
        //#endif
    }

    public static Object[] translationArgs(Component component){
        if(component==null)return new Object[0];
        //#if MC >= 11900
        return component.getContents() instanceof TranslatableContents?((TranslatableContents)component.getContents()).getArgs():new Object[0];
        //#else
        //$$ return component instanceof TranslatableComponent?((TranslatableComponent)component).getArgs():new Object[0];
        //#endif
    }

    public static long windowHandle(Minecraft minecraft){
        //#if MC >= 11800
        return minecraft.getWindow().handle();
        //#elseif MC >= 11500
        //$$ return minecraft.getWindow().getWindow();
        //#else
        //$$ return minecraft.window.getWindow();
        //#endif
    }

    public static int guiScaledWidth(Minecraft minecraft){
        //#if MC >= 11500
        return minecraft.getWindow().getGuiScaledWidth();
        //#else
        //$$ return minecraft.window.getGuiScaledWidth();
        //#endif
    }

    public static double guiScale(Minecraft minecraft){
        //#if MC >= 11500
        return minecraft.getWindow().getGuiScale();
        //#else
        //$$ return minecraft.window.getGuiScale();
        //#endif
    }

    public static int windowHeight(Minecraft minecraft){
        //#if MC >= 11500
        return minecraft.getWindow().getHeight();
        //#else
        //$$ return minecraft.window.getHeight();
        //#endif
    }

    public static UUID gameProfileId(GameProfile profile){
        if(profile==null)return null;
        //#if MC >= 12109
        return profile.id();
        //#else
        //$$ return profile.getId();
        //#endif
    }

    public static String gameProfileName(GameProfile profile){
        if(profile==null)return null;
        //#if MC >= 12109
        return profile.name();
        //#else
        //$$ return profile.getName();
        //#endif
    }

    public static UUID userProfileId(Minecraft minecraft){
        if(minecraft==null||minecraft.getUser()==null)return null;
        //#if MC >= 11900
        return minecraft.getUser().getProfileId();
        //#else
        //$$ return minecraft.getUser().getGameProfile().getId();
        //#endif
    }

    public static boolean sendCommand(Minecraft minecraft,String command){
        if(minecraft==null||command==null||command.trim().isEmpty())return false;
        //#if MC >= 11900
        ClientPacketListener connection=minecraft.getConnection();
        if(connection==null)return false;
        connection.sendCommand(command);
        //#else
        //$$ if(minecraft.player==null)return false;
        //$$ minecraft.player.chat("/"+command);
        //#endif
        return true;
    }

    /**
     * Sends a Tweak-owned server command without routing through chat/recent-command history.
     * Callers must only use commands that do not contain signable chat arguments.
     */
    public static boolean sendServerCommandSilently(Minecraft minecraft,String command){
        if(minecraft==null||command==null||command.trim().isEmpty())return false;
        //#if MC >= 12103
        ClientPacketListener connection=minecraft.getConnection();
        if(connection==null)return false;
        connection.getConnection().send(new ServerboundChatCommandPacket(command));
        return true;
        //#elseif MC >= 11900
        //$$ ClientPacketListener connection=minecraft.getConnection();
        //$$ return connection!=null&&connection.sendUnsignedCommand(command);
        //#else
        //$$ if(minecraft.player==null)return false;
        //$$ minecraft.player.chat("/"+command);
        //$$ return true;
        //#endif
    }

    public static Inventory playerInventory(Player player){
        if(player==null)return null;
        //#if MC >= 11700
        return player.getInventory();
        //#else
        //$$ return player.inventory;
        //#endif
    }

    public static Inventory playerInventory(Minecraft minecraft){
        return minecraft==null?null:playerInventory(minecraft.player);
    }

    public static ItemStack selectedItem(Player player){
        Inventory inventory=playerInventory(player);
        if(inventory==null)return ItemStack.EMPTY;
        //#if MC >= 12103
        return inventory.getSelectedItem();
        //#else
        //$$ return inventory.getSelected();
        //#endif
    }

    public static String dropKeyName(Minecraft minecraft){
        if(minecraft==null)return "Q";
        //#if MC >= 11605
        return minecraft.options.keyDrop.getTranslatedKeyMessage().getString();
        //#else
        //$$ return minecraft.options.keyDrop.getTranslatedKeyMessage();
        //#endif
    }

    public static void overlayMessage(Minecraft minecraft,Component message){
        if(minecraft==null||message==null)return;
        //#if MC >= 260200
        //$$ minecraft.gui.hud.setOverlayMessage(message,false);
        //#else
        minecraft.gui.setOverlayMessage(message,false);
        //#endif
    }

    public static float masterVolume(Minecraft minecraft){
        return minecraft==null?0.0F:minecraft.options.getSoundSourceVolume(SoundSource.MASTER);
    }

    public static void setMasterVolume(Minecraft minecraft,float volume){
        if(minecraft==null)return;
        float value=Math.max(0.0F,Math.min(1.0F,volume));
        //#if MC >= 11904
        minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set((double)value);
        //#else
        //$$ minecraft.options.setSoundCategoryVolume(SoundSource.MASTER,value);
        //#endif
    }

    public static ItemStack carried(AbstractContainerMenu menu,Player player){
        //#if MC >= 11700
        return menu.getCarried();
        //#else
        //$$ Inventory inventory=playerInventory(player);return inventory==null?ItemStack.EMPTY:inventory.getCarried();
        //#endif
    }

    public static void setCarried(AbstractContainerMenu menu,Player player,ItemStack stack){
        //#if MC >= 11700
        menu.setCarried(stack);
        //#else
        //$$ Inventory inventory=playerInventory(player);if(inventory!=null)inventory.setCarried(stack);
        //#endif
    }

    public static void copyCarried(AbstractContainerMenu source,AbstractContainerMenu target,Player player){
        //#if MC >= 11700
        ItemStack carried=source.getCarried();target.setCarried(carried.isEmpty()?ItemStack.EMPTY:carried.copy());
        //#else
        //$$ // Pre-1.17 carried stack belongs to the shared player inventory, so no menu copy is required.
        //#endif
    }

    public static int intOr(CompoundTag tag,String key,int fallback){
        //#if MC >= 12105
        return tag.getIntOr(key,fallback);
        //#else
        //$$ return tag.contains(key)?tag.getInt(key):fallback;
        //#endif
    }

    public static long longOr(CompoundTag tag,String key,long fallback){
        //#if MC >= 12105
        return tag.getLongOr(key,fallback);
        //#else
        //$$ return tag.contains(key)?tag.getLong(key):fallback;
        //#endif
    }

    public static Screen screen(Minecraft minecraft){
        //#if MC >= 260200
        //$$ return minecraft.gui.screen();
        //#else
        return minecraft.screen;
        //#endif
    }

    public static void setScreen(Minecraft minecraft,Screen screen){
        //#if MC >= 260200
        //$$ minecraft.gui.setScreen(screen);
        //#else
        minecraft.setScreen(screen);
        //#endif
    }

    public static String stringOr(CompoundTag tag,String key,String fallback){
        //#if MC >= 12105
        return tag.getStringOr(key,fallback);
        //#else
        //$$ return tag.contains(key)?tag.getString(key):fallback;
        //#endif
    }
}
