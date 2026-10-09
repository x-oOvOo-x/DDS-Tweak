package ddstweak.mixin.feature.disableautosleepchat;

import ddstweak.config.Configs;
import ddstweak.feature.disableautosleepchat.QuietInBedChatScreen;
//#if MC >= 12109
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
//#endif
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 260200
//$$ import net.minecraft.client.gui.Gui;
//#else
import net.minecraft.client.Minecraft;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC >= 260200
//$$ @Mixin(Gui.class)
//$$ public abstract class SleepChatMixin {
//$$     @Redirect(
//$$             method="tick",
//$$             at=@At(
//$$                     value="INVOKE",
//$$                     target="Lnet/minecraft/client/gui/components/ChatComponent;openScreen(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;Lnet/minecraft/client/gui/screens/ChatScreen$ChatConstructor;)V"
//$$             )
//$$     )
//$$     private void ddstweak$replaceAutomaticSleepChat(ChatComponent chat,ChatComponent.ChatMethod method,ChatScreen.ChatConstructor<?> constructor){
//$$         chat.openScreen(method,Configs.Tweaks.DISABLE_AUTO_SLEEP_CHAT.getBooleanValue()?QuietInBedChatScreen::new:constructor);
//$$     }
//$$ }
//#else
@Mixin(Minecraft.class)
public abstract class SleepChatMixin {
    //#if MC >= 12109
    @Redirect(
            method="tick",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/client/gui/components/ChatComponent;openScreen(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;Lnet/minecraft/client/gui/screens/ChatScreen$ChatConstructor;)V"
            )
    )
    private void ddstweak$replaceAutomaticSleepChat(ChatComponent chat,ChatComponent.ChatMethod method,ChatScreen.ChatConstructor<?> constructor){
        chat.openScreen(method,Configs.Tweaks.DISABLE_AUTO_SLEEP_CHAT.getBooleanValue()?QuietInBedChatScreen::new:constructor);
    }
    //#else
    //$$ @Redirect(
    //$$         method="tick",
    //$$         at=@At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
    //$$ )
    //$$ private void ddstweak$replaceAutomaticSleepChat(Minecraft client,Screen screen){
    //$$     if(Configs.Tweaks.DISABLE_AUTO_SLEEP_CHAT.getBooleanValue()
    //$$             && screen instanceof InBedChatScreen
    //$$             && !(screen instanceof QuietInBedChatScreen)){
    //$$         client.setScreen(new QuietInBedChatScreen());
    //$$         return;
    //$$     }
    //$$     client.setScreen(screen);
    //$$ }
    //#endif
}
//#endif
