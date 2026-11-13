package ddstweak.mixin.feature.cameraplayerselector;

import ddstweak.platform.minecraft.CommandHistoryGuard;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppress only the matching command while DT dispatches it.
 * Canceling before vanilla's addRecentChat also prevents command_history.txt writes in 26.3.
 */
@Mixin(ChatComponent.class)
/**
 * Excludes only matching DT-generated commands from chat history.
 * 仅排除 DT 自动生成的匹配命令，不影响手动历史。
 */
public abstract class ChatHistoryGuardMixin {
    @Inject(method="addRecentChat",at=@At("HEAD"),cancellable=true)
    private void ddstweak$hideAutomatedCommandFromHistory(String message,CallbackInfo ci){
        if(CommandHistoryGuard.shouldSuppress(message))ci.cancel();
    }
}
