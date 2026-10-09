package ddstweak;

import ddstweak.feature.cameraplayerselector.CameraPlayerSelectorFeature;
import ddstweak.feature.windowplayerhead.WindowPlayerHeadFeature;
//#if MC >= 12111
import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandActionBinding;
//#endif
import ddstweak.integration.dds.DdsIntegrationBootstrap;
//#if MC >= 12111
import ddstweak.feature.bettersnowcoverage.BetterSnowCoverage;
//#endif
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;

public final class DDSTweak implements ClientModInitializer {
    public static final String MOD_ID=DDSTweakReference.MOD_ID;
    @Override public void onInitializeClient(){
        //#if MC >= 12111
        BetterSnowCoverage.registerModelLoading();
        //#endif
        InitializationHandler.getInstance().registerInitializationHandler(new DDSTweakInitHandler());
        WindowPlayerHeadFeature.initialize();
        CameraPlayerSelectorFeature.initialize();
        //#if MC >= 12111
        CarpetPlayerCommandActionBinding.initialize();
        //#endif
        DdsIntegrationBootstrap.initialize();
    }
}
