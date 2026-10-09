package ddstweak.api.config;

@FunctionalInterface
public interface LegacyConfigScreenProvider {
    String ENTRYPOINT="dds-tweak:config_screen";
    void register(LegacyConfigScreenRegistrar registrar);
}
