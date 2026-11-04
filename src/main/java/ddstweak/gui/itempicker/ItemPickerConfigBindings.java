package ddstweak.gui.itempicker;

import ddstweak.config.Configs;
import ddstweak.platform.minecraft.MinecraftCompat;
import fi.dy.masa.malilib.config.IConfigStringList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Only registered DT config objects open the shared picker; unrelated string lists keep MaLiLib's editor. */
public final class ItemPickerConfigBindings {
    private static final Map<IConfigStringList, Binding> BINDINGS = new IdentityHashMap<>();

    static {
        register(Configs.Tweaks.AUTO_EAT_FOOD_BLACKLIST, ItemPickerKind.FOOD,
                "dds-tweak.config.tweaks.prettyName.autoEatFoodBlacklist");
        register(Configs.Tweaks.DROP_CONFIRM_BLACKLIST, ItemPickerKind.ITEM,
                "dds-tweak.config.tweaks.prettyName.dropConfirmBlacklist");
        register(Configs.Tweaks.DROP_CONFIRM_WHITELIST, ItemPickerKind.ITEM,
                "dds-tweak.config.tweaks.prettyName.dropConfirmWhitelist");
        register(Configs.Tweaks.SHULKER_RESTOCK_WHITELIST, ItemPickerKind.ITEM,
                "dds-tweak.config.tweaks.prettyName.shulkerRestockWhitelist");
        register(Configs.Tweaks.SHULKER_RESTOCK_BLACKLIST, ItemPickerKind.ITEM,
                "dds-tweak.config.tweaks.prettyName.shulkerRestockBlacklist");
        //#if MC >= 12111
        register(Configs.Visual.BETTER_SNOW_COVERAGE_BLACKLIST, ItemPickerKind.BLOCK,
                "dds-tweak.config.visual.prettyName.betterSnowCoverageBlacklist");
        register(Configs.Visual.BETTER_SNOW_COVERAGE_WHITELIST, ItemPickerKind.BLOCK,
                "dds-tweak.config.visual.prettyName.betterSnowCoverageWhitelist");
        register(Configs.Visual.BLOCK_SOUND_BLOCK_BLACKLIST, ItemPickerKind.SOUND_BLOCK,
                "dds-tweak.config.visual.prettyName.blockSoundBlockBlacklist");
        register(Configs.Visual.BLOCK_SOUND_BLOCK_WHITELIST, ItemPickerKind.SOUND_BLOCK,
                "dds-tweak.config.visual.prettyName.blockSoundBlockWhitelist");
        //#endif
    }

    private ItemPickerConfigBindings() {}

    /** Register once for a specific config object, not its potentially colliding name. */
    public static void register(IConfigStringList config, ItemPickerKind kind, String titleKey) {
        Objects.requireNonNull(config, "config");
        Binding binding = new Binding(Objects.requireNonNull(kind, "kind"),
                Objects.requireNonNull(titleKey, "titleKey"));
        if (titleKey.isEmpty()) throw new IllegalArgumentException("titleKey must not be empty");
        if (BINDINGS.containsKey(config)) throw new IllegalArgumentException("Picker already registered: " + config.getName());
        BINDINGS.put(config, binding);
    }

    /** Only these two server-bound lists have the negotiated restock protocol limits. */
    public static boolean isRestockList(IConfigStringList config) {
        return config == Configs.Tweaks.SHULKER_RESTOCK_WHITELIST
                || config == Configs.Tweaks.SHULKER_RESTOCK_BLACKLIST;
    }

    /** @return false for an unregistered MaLiLib string-list button. */
    public static boolean open(Screen parent, IConfigStringList config) {
        Binding binding = BINDINGS.get(config);
        if (binding == null) return false;
        MinecraftCompat.setScreen(Minecraft.getInstance(),
                ItemPicker.multi(parent, config, binding.kind, binding.titleKey));
        return true;
    }

    private static final class Binding {
        final ItemPickerKind kind;
        final String titleKey;
        Binding(ItemPickerKind kind, String titleKey) {
            this.kind = kind;
            this.titleKey = titleKey;
        }
    }
}
