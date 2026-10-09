package ddstweak;

import ddstweak.config.*;
import ddstweak.feature.railpowerguide.RailPowerPlacementGuide;
import ddstweak.feature.autologcleanup.AutoLogCleanup;
import ddstweak.feature.startupsound.ClientStartupSound;
import ddstweak.feature.silentautoeat.SilentAutoEat;
//#if MC >= 12111
import ddstweak.feature.bettersnowcoverage.BetterSnowCoverage;
//#endif
import ddstweak.platform.malilib.LegacyConfigScreenRegistry;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
//#if MC >= 12101
import fi.dy.masa.malilib.registry.Registry;
//#endif

public final class DDSTweakInitHandler implements IInitializationHandler {
    @Override public void registerModHandlers(){
        ConfigManager.getInstance().registerConfigHandler(DDSTweakReference.MOD_ID,new Configs());
        //#if MC >= 12101
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(DDSTweakReference.MOD_INFO);
        //#else
        //$$ LegacyConfigScreenRegistry.initialize();
        //#endif
        InputEventHandler.getKeybindManager().registerKeybindProvider(DDSTweakHotkeyProvider.INSTANCE);
        HotkeyCallbacks.init();
        RailPowerPlacementGuide.initialize();
        ClientStartupSound.initialize();
        AutoLogCleanup.initialize();
        SilentAutoEat.initialize();
        //#if MC >= 12111
        BetterSnowCoverage.initialize();
        //#endif
    }
}
