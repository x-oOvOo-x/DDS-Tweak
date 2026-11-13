package ddstweak.mixin.feature.chattooltip;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes chat completions for tooltip overlap checks.
 * 向悬浮提示遮挡判断提供聊天补全对象。
 */
@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {
    @Accessor("commandSuggestions")
    CommandSuggestions ddstweak$commandSuggestions();
}
