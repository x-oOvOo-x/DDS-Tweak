package ddstweak.mixin.feature.nocommandconfirm;

import ddstweak.feature.nocommandconfirm.NoCommandConfirmFeature;
import ddstweak.platform.minecraft.MinecraftCompat;
import ddstweak.playerinventory.PlayerInventoryCommandStateObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
//#if MC >= 12002
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
//#endif
//#if MC >= 12106
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
//#endif
import org.spongepowered.asm.mixin.Mixin;
//#if MC >= 12106
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin
//#if MC >= 12002
        extends ClientCommonPacketListenerImpl
//#endif
{
    //#if MC >= 12002
    protected ClientPacketListenerMixin(Minecraft minecraft,Connection connection,CommonListenerCookie cookie){super(minecraft,connection,cookie);}
    //#endif

    //#if MC >= 12106
    @Inject(method="openCommandSendConfirmationWindow",at=@At("HEAD"),cancellable=true)
    private void ddstweak$skipOrdinaryCommandConfirm(String command,String messageKey,Screen screenAfterCommand,CallbackInfo ci){
        if(!NoCommandConfirmFeature.enabled())return;
        //#if MC >= 12111
        PlayerInventoryCommandStateObserver.observeCommand(command);
        //#endif
        send(new ServerboundChatCommandPacket(command));
        MinecraftCompat.setScreen(minecraft,screenAfterCommand);
        ci.cancel();
    }
    //#endif
}
