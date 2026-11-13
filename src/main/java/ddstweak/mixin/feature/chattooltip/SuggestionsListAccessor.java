package ddstweak.mixin.feature.chattooltip;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the suggestion popup rectangle for hit testing.
 * 提供命令补全弹窗区域，供命中检测使用。
 */
@Mixin(CommandSuggestions.SuggestionsList.class)
public interface SuggestionsListAccessor {
    @Accessor("rect")
    Rect2i ddstweak$rect();
}
