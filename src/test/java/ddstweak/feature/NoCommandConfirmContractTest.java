package ddstweak.feature;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class NoCommandConfirmContractTest {
    private static final Path ROOT=Path.of("").toAbsolutePath();
    private static String read(String p) throws Exception {Path f=ROOT.resolve(p);return Files.exists(f)?Files.readString(f):"";}

    @Test void tweakIsDefaultOffAndOnlyExposedWhereVanillaConfirmationExists() throws Exception {
        String c=read("src/main/java/ddstweak/config/Configs.java");
        assertTrue(c.contains("ConfigBoolean NO_COMMAND_CONFIRM"));
        assertTrue(c.contains("MalilibCompat.bool(\"noCommandConfirm\",false,TWEAKS_KEY)"));
        assertTrue(c.contains("MC >= 12106"));
        assertTrue(c.contains("public static final List<IConfigBase> OPTIONS=ImmutableList.of("));
        assertTrue(c.contains("NO_COMMAND_CONFIRM"));
    }

    @Test void featureOwnsEnablementDecision() throws Exception {
        String f=read("src/main/java/ddstweak/feature/nocommandconfirm/NoCommandConfirmFeature.java");
        assertTrue(f.contains("Configs.Tweaks.NO_COMMAND_CONFIRM.getBooleanValue()"));
    }

    @Test void mixinOnlyBypassesOrdinaryConfirmation() throws Exception {
        String m=read("src/main/java/ddstweak/mixin/feature/nocommandconfirm/ClientPacketListenerMixin.java"),j=read("src/main/resources/dds-tweak.mixins.json");
        assertTrue(m.contains("openCommandSendConfirmationWindow"));
        assertTrue(m.contains("cancellable=true"));
        assertTrue(m.contains("new ServerboundChatCommandPacket(command)"));
        assertTrue(m.contains("MinecraftCompat.setScreen(minecraft,screenAfterCommand)"));
        assertTrue(m.contains("ci.cancel()"));
        assertFalse(m.contains("openSignedCommandSendConfirmationWindow"));
        assertTrue(j.contains("feature.nocommandconfirm.ClientPacketListenerMixin"));
        String common=read("common.gradle"),contract=read("config/version-capability-boundaries.json"),build=read("build.gradle");
        assertTrue(contract.contains("\"id\": \"noCommandConfirm\""));
        assertTrue(contract.contains("\"minMcVersionCode\": 12106"));
        assertTrue(contract.contains("ddstweak/feature/nocommandconfirm/"));
        assertTrue(contract.contains("feature.nocommandconfirm.ClientPacketListenerMixin"));
        assertTrue(common.contains("unavailableVersionFeatures"));
        assertTrue(common.contains("unavailableJavaPrefixes"));
        assertTrue(build.contains("Version capability Java boundary mismatch"));
        assertTrue(build.contains("Version capability Mixin boundary mismatch"));
    }
}
