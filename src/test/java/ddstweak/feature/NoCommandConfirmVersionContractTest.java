package ddstweak.feature;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class NoCommandConfirmVersionContractTest {
    @Test
    void confirmationMixinOnlyTargetsVersionsThatHaveTheConfirmationMethod() throws Exception {
        String source=Files.readString(Path.of("").toAbsolutePath()
                .resolve("src/main/java/ddstweak/mixin/feature/nocommandconfirm/ClientPacketListenerMixin.java"));
        assertTrue(source.contains("//#if MC >= 12106"));
        assertTrue(source.contains("openCommandSendConfirmationWindow"));
        assertTrue(source.contains("new ServerboundChatCommandPacket(command)"));
    }
}
