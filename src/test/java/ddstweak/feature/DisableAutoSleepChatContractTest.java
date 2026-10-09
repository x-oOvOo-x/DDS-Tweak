package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class DisableAutoSleepChatContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {
        Path f=ROOT.resolve(p);
        return Files.exists(f)?Files.readString(f):"";
    }

    @Test void configIsOptInAndLivesInTweaks() throws Exception {
        String configs=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(configs.contains("DISABLE_AUTO_SLEEP_CHAT=MalilibCompat.bool(\"disableAutoSleepChat\",false,TWEAKS_KEY)"));
        int options=configs.indexOf("public static final List<IConfigBase> OPTIONS=ImmutableList.of(",configs.indexOf("DISABLE_AUTO_SLEEP_CHAT"));
        String registered=configs.substring(options,configs.indexOf(");",options));
        assertTrue(registered.contains("DISABLE_AUTO_SLEEP_CHAT"));
    }

    @Test void automaticOpenUsesQuietScreenButManualChatUsesVanillaScreen() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/disableautosleepchat/SleepChatMixin.java");
        String quiet=read("src/main/java/ddstweak/feature/disableautosleepchat/QuietInBedChatScreen.java");
        assertTrue(mixin.contains("QuietInBedChatScreen::new"));
        assertTrue(mixin.contains("screen instanceof InBedChatScreen"));
        assertTrue(mixin.contains("new QuietInBedChatScreen()"));
        assertTrue(quiet.contains("extends InBedChatScreen"));
        assertTrue(quiet.contains("this.input.active=false"));
        assertTrue(quiet.contains("this.input.visible=false"));
        assertTrue(quiet.contains("keyChat.matches"));
        assertTrue(quiet.contains("InBedChatScreen::new"));
        assertTrue(quiet.contains("new InBedChatScreen()"));
    }

    @Test void quietScreenKeepsVanillaLeaveBedAndEscapeBehavior() throws Exception {
        String quiet=read("src/main/java/ddstweak/feature/disableautosleepchat/QuietInBedChatScreen.java");
        assertTrue(quiet.contains("super.init()"));
        assertTrue(quiet.contains("return super.keyPressed"));
        assertFalse(quiet.contains("STOP_SLEEPING"));
        assertFalse(quiet.contains("ServerboundPlayerCommandPacket"));
    }

    @Test void versionBoundaryMovesFromMinecraftToGuiAt26_2() throws Exception {
        String mixin=read("src/main/java/ddstweak/mixin/feature/disableautosleepchat/SleepChatMixin.java");
        assertTrue(mixin.contains("MC >= 260200"));
        assertTrue(mixin.contains("@Mixin(Gui.class)"));
        assertTrue(mixin.contains("@Mixin(Minecraft.class)"));
        assertTrue(mixin.contains("ChatComponent;openScreen"));
    }

    @Test void quietScreenSuppressesChatByRemainingAChatScreenWithoutCallingSuperRender() throws Exception {
        String quiet=read("src/main/java/ddstweak/feature/disableautosleepchat/QuietInBedChatScreen.java");
        assertTrue(quiet.contains("extends InBedChatScreen"));
        assertFalse(quiet.contains("super.render("));
        assertFalse(quiet.contains("super.extractRenderState("));
    }

    @Test void mixinIsRegisteredForEverySupportedVersion() throws Exception {
        String json=read("src/main/resources/dds-tweak.mixins.json");
        String common=read("common.gradle");
        assertTrue(json.contains("feature.disableautosleepchat.SleepChatMixin"));
        assertFalse(json.contains("feature.disableautosleepchat.ChatComponentMixin"));
        assertFalse(common.contains("unavailable.add('feature.disableautosleepchat.SleepChatMixin')"));
    }

    @Test void allLocalesDescribeManualChatAccess() throws Exception {
        for(String locale:new String[]{"en_us","zh_cn","zh_tw"}){
            String lang=read("src/main/resources/assets/dds-tweak/lang/"+locale+".json");
            assertTrue(lang.contains("disableAutoSleepChat"));
            assertTrue(lang.contains("disableautosleepchat"));
        }
    }
}
