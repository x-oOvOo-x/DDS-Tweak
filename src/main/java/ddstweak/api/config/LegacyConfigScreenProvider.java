package ddstweak.api.config;

/**
 * Registers optional legacy configuration screens through one entry point.
 * 通过统一入口注册可选的旧版配置界面。
 */
@FunctionalInterface
public interface LegacyConfigScreenProvider {
    String ENTRYPOINT="dds-tweak:config_screen";
    void register(LegacyConfigScreenRegistrar registrar);
}
