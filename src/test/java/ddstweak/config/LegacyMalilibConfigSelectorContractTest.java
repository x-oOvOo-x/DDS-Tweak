package ddstweak.config;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class LegacyMalilibConfigSelectorContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p)throws Exception{return Files.readString(ROOT.resolve(p));}

    @Test void currentProjectUsesLegacyThrough12006AndNativeFrom121()throws Exception{
        String m=read("src/main/java/ddstweak/mixin/config/LegacyConfigSelectorMixin.java"),
               g=read("common.gradle"),j=read("src/main/resources/dds-tweak.mixins.json"),
               i=read("src/main/java/ddstweak/DDSTweakInitHandler.java"),
               p20=read("versions/1.20.6/gradle.properties"),p21=read("versions/1.21.1/gradle.properties");
        assertTrue(m.startsWith("//#if MC <= 12006"));
        assertTrue(g.contains("if (mcVersion > 1_20_06) unavailable.add('config.LegacyConfigSelectorMixin')"));
        assertTrue(j.contains("config.LegacyConfigSelectorMixin"));
        assertTrue(i.contains("//#if MC >= 12101"));
        assertTrue(i.contains("Registry.CONFIG_SCREEN.registerConfigScreenFactory(DDSTweakReference.MOD_INFO)"));
        assertTrue(i.contains("//$$ LegacyConfigScreenRegistry.initialize();"));
        assertTrue(p20.contains("malilib_version=0.19.0"));
        assertTrue(p21.contains("malilib_version=0.21.10"));
    }

    @Test void legacyMixinTargetsInheritedGuiBaseInitOnlyForConfigScreens()throws Exception{
        String m=read("src/main/java/ddstweak/mixin/config/LegacyConfigSelectorMixin.java");
        assertTrue(m.contains("@Mixin(value=GuiBase.class,remap=false)"));
        assertTrue(m.contains("@Inject(method=\"initGui\",at=@At(\"RETURN\"),remap=false)"));
        assertTrue(m.contains("instanceof GuiConfigsBase"));assertTrue(m.contains("GuiConfigsBase gui=(GuiConfigsBase)(Object)this"));
        assertFalse(m.contains("@Mixin(value=GuiConfigsBase.class,remap=false)"));
        assertTrue(m.contains("WidgetDropDownList<Entry>"));
    }

    @Test void registryProvidesBaseEntriesAndFallbackAdapters()throws Exception{
        String r=read("src/main/java/ddstweak/platform/malilib/LegacyConfigScreenRegistry.java");
        assertTrue(r.indexOf("new Entry(\"malilib\"")<r.indexOf("DDSTweakConfigScreen::new"));
        assertTrue(r.indexOf("DDSTweakConfigScreen::new")<r.indexOf("loadEntrypoints();"));
        for(String entry:new String[]{
                "fi.dy.masa.litematica.gui.GuiConfigs",
                "fi.dy.masa.minihud.gui.GuiConfigs",
                "fi.dy.masa.tweakeroo.gui.GuiConfigs",
                "fi.dy.masa.itemscroller.gui.GuiConfigs"
        })assertTrue(r.contains(entry),entry);
        assertTrue(r.contains("!FabricLoader.getInstance().isModLoaded(modId)||find(modId).isPresent()"));
    }

    @Test void thirdPartyEntrypointIsPublicOwnedPrioritizedAndFailClosed()throws Exception{
        String p=read("src/main/java/ddstweak/api/config/LegacyConfigScreenProvider.java"),
               a=read("src/main/java/ddstweak/api/config/LegacyConfigScreenRegistrar.java"),
               r=read("src/main/java/ddstweak/platform/malilib/LegacyConfigScreenRegistry.java");
        assertTrue(p.contains("ENTRYPOINT=\"dds-tweak:config_screen\""));
        assertTrue(p.contains("void register(LegacyConfigScreenRegistrar registrar)"));
        assertTrue(a.contains("String modId()"));
        assertTrue(a.contains("Supplier<? extends Screen> factory"));
        assertTrue(r.contains("getEntrypointContainers(LegacyConfigScreenProvider.ENTRYPOINT"));
        assertTrue(r.contains("container.getProvider().getMetadata().getId()"));
        assertTrue(r.indexOf("loadEntrypoints();")<r.indexOf("registerOptional(\"litematica\""));
        assertTrue(r.contains("Ignoring duplicate legacy config-screen registration"));
        assertTrue(r.contains("Ignoring broken legacy config-screen provider"));
        assertTrue(r.contains("Skipping legacy config selector adapter"));
    }

    @Test void switchingPreservesParentAndDoesNotForkMalilibOrMagiclib()throws Exception{
        String r=read("src/main/java/ddstweak/platform/malilib/LegacyConfigScreenRegistry.java"),g=read("common.gradle");
        assertTrue(r.contains("if(next instanceof GuiBase)((GuiBase)next).setParent(parent)"));
        assertTrue(r.contains("GuiBase.openGui(next)"));
        assertTrue(g.contains("project.malilib_coordinate"));
        assertFalse(g.toLowerCase().contains("magiclib_coordinate"));
    }
}
