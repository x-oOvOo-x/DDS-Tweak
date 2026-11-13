package ddstweak.mixin.feature.chattooltip;

import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the active command suggestion list without changing its contents.
 * 读取当前命令补全列表，不修改其内容。
 */
@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsAccessor {
    @Accessor("suggestions")
    CommandSuggestions.SuggestionsList ddstweak$suggestions();
}
