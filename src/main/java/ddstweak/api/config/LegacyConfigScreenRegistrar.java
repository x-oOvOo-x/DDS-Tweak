package ddstweak.api.config;

import net.minecraft.client.gui.screens.Screen;
import java.util.function.Supplier;

/**
 * Defines the registration interface for optional configuration screens.
 * 定义可选配置界面的注册接口。
 */
public interface LegacyConfigScreenRegistrar {
    String modId();
    void register(Supplier<? extends Screen> factory);
    void register(String displayName,Supplier<? extends Screen> factory);
}
