package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class HappyGhastRidingSpeedContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();

    private static String read(String path) throws Exception {
        Path file=ROOT.resolve(path);
        return Files.exists(file)?Files.readString(file):"";
    }

    @Test void configUsesVanillaDefaultAndConservativeRange() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(configs.contains("//#if MC >= 12106"));
        assertTrue(configs.contains("HAPPY_GHAST_RIDING_SPEED_MULTIPLIER=MalilibCompat.decimal(\"happyGhastRidingSpeedMultiplier\",1.0,0.5,2.0,true,TWEAKS_KEY)"));
        assertTrue(configs.contains("NO_COMMAND_CONFIRM,HAPPY_GHAST_RIDING_SPEED_MULTIPLIER"));
    }

    @Test void riddenInputIsScaledWithoutReplacingVanillaMovement() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/happyghastspeed/HappyGhastRidingInputMixin.java");
        String feature=read("src/main/java/ddstweak/feature/happyghastspeed/HappyGhastRidingSpeedFeature.java");

        assertTrue(mixin.contains("@Inject(method=\"getRiddenInput\",at=@At(\"RETURN\"),cancellable=true)"));
        assertTrue(mixin.contains("HappyGhastRidingSpeedFeature.scaleRiddenInput(controller,input)"));
        assertFalse(mixin.contains("setDeltaMovement"));
        assertTrue(feature.contains("controller==minecraft.player?input.scale(Configs.Tweaks.HAPPY_GHAST_RIDING_SPEED_MULTIPLIER.getDoubleValue()):input"));
    }

    @Test void altWheelAdjustsOnlyVerticalInputForTheControllingRider() throws Exception {
        String mouse=read("src/main/java/ddstweak/mixin/feature/happyghastspeed/HappyGhastSpeedMouseHandlerMixin.java");
        String feature=read("src/main/java/ddstweak/feature/happyghastspeed/HappyGhastRidingSpeedFeature.java");

        assertTrue(mouse.contains("handleScroll(vertical)"));
        assertFalse(mouse.contains("handleScroll(horizontal)"));
        assertTrue(mouse.contains("if(HappyGhastRidingSpeedFeature.handleScroll(vertical))ci.cancel()"));

        assertTrue(feature.contains("ddstweak$hasScreen(minecraft)"));
        assertTrue(feature.contains("//$$ return minecraft.gui.screen()!=null;"));
        assertTrue(feature.contains("return minecraft.screen!=null;"));
        assertTrue(feature.contains("ddstweak$altDown()"));
        assertTrue(feature.contains("vehicle instanceof HappyGhastRidingSpeedTarget&&vehicle.getControllingPassenger()==player"));
        assertTrue(feature.contains("current+steps*STEP"));
        assertTrue(feature.contains("MIN=0.5D,MAX=2.0D,STEP=0.1D"));
        assertTrue(feature.contains("Component.translatable(\"dds-tweak.happy_ghast.riding_speed\""));
        assertTrue(feature.contains("player.displayClientMessage(message,true)"));
        assertTrue(feature.contains("//$$ player.sendOverlayMessage(message)"));
    }

    @Test void highResolutionWheelInputAccumulatesAndResetsAfterIdle() throws Exception {
        String feature=read("src/main/java/ddstweak/feature/happyghastspeed/HappyGhastRidingSpeedFeature.java");
        assertTrue(feature.contains("SCROLL_RESET_NANOS=500_000_000L"));
        assertTrue(feature.contains("scrollProgress+=vertical"));
        assertTrue(feature.contains("if(steps==0)return true"));
        assertTrue(feature.contains("if(now-lastScrollNanos>SCROLL_RESET_NANOS)scrollProgress=0.0D"));
    }

    @Test void versionBoundariesCoverIntroductionAndPackageMove() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/happyghastspeed/HappyGhastRidingInputMixin.java");
        String common=read("common.gradle");
        String build=read("build.gradle");

        assertTrue(mixin.contains("//#if MC >= 12106"));
        assertTrue(mixin.contains("//#if MC >= 12109"));
        assertTrue(mixin.contains("net.minecraft.world.entity.animal.happyghast.HappyGhast"));
        assertTrue(mixin.contains("//$$ import net.minecraft.world.entity.animal.HappyGhast;"));

        String contract=read("config/version-capability-boundaries.json");
        assertTrue(contract.contains("\"id\": \"happyGhastRidingSpeed\""));
        assertTrue(contract.contains("\"minMcVersionCode\": 12106"));
        assertTrue(contract.contains("ddstweak/feature/happyghastspeed/"));
        assertTrue(contract.contains("feature.happyghastspeed.HappyGhastRidingInputMixin"));
        assertTrue(contract.contains("feature.happyghastspeed.HappyGhastSpeedMouseHandlerMixin"));
        assertTrue(common.contains("unavailableVersionFeatures"));
        assertTrue(common.contains("unavailableJavaPrefixes"));
        assertTrue(build.contains("Version capability Java boundary mismatch"));
        assertTrue(build.contains("Version capability Mixin boundary mismatch"));
    }

    @Test void mixinsAndTranslationsAreRegistered() throws Exception {
        String json=read("src/main/resources/dds-tweak.mixins.json");
        String en=read("src/main/resources/assets/dds-tweak/lang/en_us.json");
        String zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");

        assertTrue(json.contains("feature.happyghastspeed.HappyGhastRidingInputMixin"));
        assertTrue(json.contains("feature.happyghastspeed.HappyGhastSpeedMouseHandlerMixin"));
        assertTrue(en.contains("dds-tweak.config.tweaks.prettyName.happyghastridingspeedmultiplier"));
        assertTrue(zh.contains("快乐恶魂骑乘速度"));
        assertTrue(en.contains("dds-tweak.happy_ghast.riding_speed"));
        assertTrue(zh.contains("dds-tweak.happy_ghast.riding_speed"));
    }
}
