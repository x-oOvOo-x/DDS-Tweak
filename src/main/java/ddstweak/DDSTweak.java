package ddstweak;

import ddstweak.feature.cameraplayerselector.CameraPlayerSelectorFeature;
import ddstweak.feature.carpetrules.CarpetRuleClientNetwork;
//#if MC >= 260100
//#if MC < 260300
//$$ import ddstweak.gui.LegacyGpuSdfRenderer;
//#endif
//#endif
import ddstweak.feature.windowplayerhead.WindowPlayerHeadFeature;
//#if MC >= 12111
import ddstweak.integration.carpet.playerinventory.CarpetPlayerCommandActionBinding;
//#endif
import ddstweak.integration.dds.DdsIntegrationBootstrap;
//#if MC >= 12111
import ddstweak.feature.bettersnowcoverage.BetterSnowCoverage;
//#endif
//#if MC >= 12108
import ddstweak.feature.locatorheads.LocatorCompass;
import ddstweak.feature.locatorheads.LocatorContourCache;
import ddstweak.feature.locatorheads.LocatorNameFades;
//#endif
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;

/**
 * Initializes the DDS Tweak client mod.
 * 初始化 DDS Tweak 客户端模组。
 */
public final class DDSTweak implements ClientModInitializer {
    public static final String MOD_ID=DDSTweakReference.MOD_ID;
    @Override public void onInitializeClient(){
        //#if MC >= 260100
        //#if MC < 260300
        //$$ LegacyGpuSdfRenderer.initialize();
        //#endif
        //#endif
        //#if MC >= 12111
        BetterSnowCoverage.registerModelLoading();
        //#endif
        InitializationHandler.getInstance().registerInitializationHandler(new DDSTweakInitHandler());
        WindowPlayerHeadFeature.initialize();
        //#if MC >= 12108
        LocatorCompass.initialize();
        LocatorContourCache.initialize();
        LocatorNameFades.initialize();
        //#endif
        CameraPlayerSelectorFeature.initialize();
        //#if MC >= 12111
        CarpetPlayerCommandActionBinding.initialize();
        //#endif
        CarpetRuleClientNetwork.initialize();
        DdsIntegrationBootstrap.initialize();
    }
}
