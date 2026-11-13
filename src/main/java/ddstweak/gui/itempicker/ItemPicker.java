package ddstweak.gui.itempicker;

import ddstweak.feature.dropconfirmation.DropConfirmationItemScreen;
import fi.dy.masa.malilib.config.IConfigStringList;
import net.minecraft.client.gui.screens.Screen;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared picker entry points; selection policy and persistence remain with callers.
 * 提供可复用的单选与多选物品选择界面。
 */
public final class ItemPicker {
    private ItemPicker() {}

    /** Multi-select, retaining the existing MaLiLib live-write and close-save behavior. */
    public static Screen multi(Screen parent, IConfigStringList target, ItemPickerKind kind, String titleKey) {
        return new DropConfirmationItemScreen(parent, Objects.requireNonNull(target, "target"),
                Objects.requireNonNull(kind, "kind"), titleKey);
    }

    /** Standalone multi-select: caller-supplied values, Done commits, Esc cancels. */
    public static Screen multi(Screen parent, Collection<String> initialIds, ItemPickerKind kind,
                               Consumer<List<String>> onSelected, String titleKey) {
        return new DropConfirmationItemScreen(parent, initialIds, Objects.requireNonNull(kind, "kind"),
                Objects.requireNonNull(onSelected, "onSelected"), titleKey);
    }

    /** Required single-select: Done commits; Esc cancels; Reset restores defaultId. */
    public static Screen single(Screen parent, String selectedId, String defaultId,
                                Consumer<String> onSelected, String titleKey) {
        return new DropConfirmationItemScreen(parent, selectedId, defaultId,
                Objects.requireNonNull(onSelected, "onSelected"), titleKey);
    }
}
