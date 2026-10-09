package ddstweak.api.config;

import net.minecraft.client.gui.screens.Screen;
import java.util.function.Supplier;

public interface LegacyConfigScreenRegistrar {
    String modId();
    void register(Supplier<? extends Screen> factory);
    void register(String displayName,Supplier<? extends Screen> factory);
}
