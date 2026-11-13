package ddstweak.feature.bettersnowcoverage;

//#if MC >= 12111
import ddstweak.config.Configs;
import net.minecraft.client.Minecraft;

/**
 * Registers snow model hooks and triggers visual refresh on config changes.
 * 注册积雪模型适配，并在设置变更时刷新画面。
 */
public final class BetterSnowCoverage {
    private BetterSnowCoverage(){}

    public static void registerModelLoading(){BetterSnowModelLoading.register();}

    public static void initialize(){
        Configs.Visual.BETTER_SNOW_COVERAGE.setValueChangeCallback(config->changed());
        Configs.Visual.BETTER_SNOW_COVERAGE_MODE.setValueChangeCallback(config->changed());
        Configs.Visual.BETTER_SNOW_COVERAGE_BLACKLIST.setValueChangeCallback(config->changed());
        Configs.Visual.BETTER_SNOW_COVERAGE_WHITELIST.setValueChangeCallback(config->changed());
    }

    private static void changed(){
        SnowCoverageBuildContext.invalidate();
        rebuild();
    }

    private static void rebuild(){
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft==null)return;
        minecraft.execute(()->{
            if(minecraft.level==null||minecraft.levelRenderer==null)return;
            //#if MC >= 260200
            //$$ minecraft.levelExtractor.allChanged();
            //#else
            minecraft.levelRenderer.allChanged();
            //#endif
        });
    }
}
//#endif
