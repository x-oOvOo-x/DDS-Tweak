package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class WindowPlayerHeadContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}
    private static Class<?> type(String name){return assertDoesNotThrow(()->Class.forName(name),"missing "+name);}

    @Test void visualConfigIsDefaultOff() throws Exception {
        String c=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(c.contains("PLAYER_HEAD_WINDOW_ICON=MalilibCompat.bool(\"playerHeadWindowIcon\",false,VISUAL_KEY)"));
    }

    @Test void clientLifecycleModuleAndFeatureRegistrationArePresent() throws Exception {
        String b=read("common.gradle"),e=read("src/main/java/ddstweak/DDSTweak.java");
        assertTrue(b.contains("net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"));
        assertFalse(b.contains("fabricApi.module("));
        assertTrue(e.contains("WindowPlayerHeadFeature.initialize()"));
    }

    @Test void skinCacheIsTextureKeyedAndNamespaced() throws Exception {
        Class<?> c=type("ddstweak.feature.windowplayerhead.PlayerSkinCache");
        Method m=c.getDeclaredMethod("path",Path.class,String.class);m.setAccessible(true);
        Path actual=(Path)m.invoke(null,Path.of("game"),"abcdef123456");
        assertEquals(Path.of("game","cache","dds-tweak","player-skin","abcdef123456.png"),actual);
    }

    @Test void resolverUsesOfficialProfileAndTextureServicesNotHeadRendererApi() throws Exception {
        String r=read("src/main/java/ddstweak/feature/windowplayerhead/PlayerSkinDownloader.java");
        assertTrue(r.contains("sessionserver.mojang.com/session/minecraft/profile/"));
        assertTrue(r.contains("textures.minecraft.net"));
        assertFalse(r.toLowerCase().contains("minotar"));
        assertTrue(r.contains("status>=200&&status<300"));
    }

    @Test void rendererBuildsFaceAndHatAtTwoIconSizes() throws Exception {
        String r=read("src/main/java/ddstweak/feature/windowplayerhead/PlayerHeadRenderer.java");
        assertTrue(r.contains("FACE_X=8"));
        assertTrue(r.contains("FACE_Y=8"));
        assertTrue(r.contains("HAT_X=40"));
        assertTrue(r.contains("HAT_Y=8"));
        assertTrue(r.contains("render(source,32)"));
        assertTrue(r.contains("render(source,64)"));
        assertTrue(r.contains("size/18.0"));
    }

    @Test void featureIsAsyncCacheFirstAndAppliesLayeredIconsOnClientThread() throws Exception {
        String f=read("src/main/java/ddstweak/feature/windowplayerhead/WindowPlayerHeadFeature.java");
        assertTrue(f.contains("ClientLifecycleEvents.CLIENT_STARTED.register"));
        assertTrue(f.contains("Configs.Visual.PLAYER_HEAD_WINDOW_ICON.getBooleanValue()"));
        assertTrue(f.contains("PlayerSkinDownloader.resolve"));
        assertTrue(f.contains("PlayerHeadRenderer.renderIcons"));
        assertTrue(f.contains("client.execute"));
        assertTrue(f.contains("WindowIcon.set"));
        assertFalse(f.contains("PlayerHeadDownloader"));
    }

    @Test void glfwReceivesBothIconSizes() throws Exception {
        String w=read("src/main/java/ddstweak/feature/windowplayerhead/WindowIcon.java");
        assertTrue(w.contains("GLFWImage.malloc(icons.length"));
        assertTrue(w.contains("glfwSetWindowIcon"));
    }

    @Test void translationsDescribeOptInBehavior() throws Exception {
        String en=read("src/main/resources/assets/dds-tweak/lang/en_us.json"),zh=read("src/main/resources/assets/dds-tweak/lang/zh_cn.json");
        assertTrue(en.contains("playerheadwindowicon"));
        assertTrue(zh.contains("playerheadwindowicon"));
    }
}
