package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class DisableEndFlashContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}

    @Test void visualToggleIsDefaultOff() throws Exception {
        String c=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(c.contains("ConfigBoolean DISABLE_END_FLASH"));
        assertTrue(c.contains("MalilibCompat.bool(\"disableEndFlash\",false,VISUAL_KEY)"));
        assertTrue(c.contains("OPTIONS=ImmutableList.of("));
        assertTrue(c.contains("DISABLE_END_FLASH"));
    }

    @Test void featureOwnsEnablementDecision() throws Exception {
        String f=read("src/main/java/ddstweak/feature/disableendflash/DisableEndFlashFeature.java");
        assertTrue(f.contains("Configs.Visual.DISABLE_END_FLASH.getBooleanValue()"));
    }

    @Test void mixinCancelsOnlyEndFlashRenderPass() throws Exception {
        String m=read("src/main/java/ddstweak/mixin/feature/disableendflash/SkyRendererMixin.java"),j=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(m.contains("@Mixin(SkyRenderer.class)"));
        assertTrue(m.contains("method=\"renderEndFlash\""));
        assertTrue(m.contains("at=@At(\"HEAD\")"));
        assertTrue(m.contains("cancellable=true"));
        assertTrue(m.contains("DisableEndFlashFeature.enabled()"));
        assertTrue(m.contains("ci.cancel()"));
        assertTrue(m.contains("private void ddstweak$disableEndFlash(CallbackInfo ci)"));
        assertFalse(m.contains("PoseStack"));
        assertFalse(m.contains("RenderPass"));
        assertFalse(m.contains("renderEndSky"));
        assertTrue(j.contains("feature.disableendflash.SkyRendererMixin"));
    }

    @Test void lightmapIgnoresEndFlashStateWhenDisabled() throws Exception {
        String m=read("src/main/java/ddstweak/mixin/feature/disableendflash/LightTextureMixin.java"),j=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(m.contains("@Mixin(LightTexture.class)"));
        assertTrue(m.contains("@Redirect"));
        assertTrue(m.contains("method=\"updateLightTexture\""));
        assertTrue(m.contains("ClientLevel;endFlashState()Lnet/minecraft/client/renderer/EndFlashState;"));
        assertTrue(m.contains("DisableEndFlashFeature.enabled()?null:level.endFlashState()"));
        assertTrue(j.contains("feature.disableendflash.LightTextureMixin"));
    }
}
