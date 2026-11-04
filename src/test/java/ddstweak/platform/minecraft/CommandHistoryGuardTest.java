package ddstweak.platform.minecraft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CommandHistoryGuardTest {
    @Test void suppressesOnlyMatchingCommandWithinDispatch(){
        assertFalse(CommandHistoryGuard.shouldSuppress("/server survival"));
        boolean sent=CommandHistoryGuard.run("server survival",()->{
            assertTrue(CommandHistoryGuard.shouldSuppress("/server survival"));
            assertTrue(CommandHistoryGuard.shouldSuppress("server survival"));
            assertFalse(CommandHistoryGuard.shouldSuppress("/server creative"));
            assertFalse(CommandHistoryGuard.shouldSuppress("/home"));
            return true;
        });
        assertTrue(sent);
        assertFalse(CommandHistoryGuard.shouldSuppress("/server survival"));
    }

    @Test void chatScopesSuppressExactMessagesWithoutAddingSlash(){
        CommandHistoryGuard.runChat("!!tp Steve",()->{
            assertTrue(CommandHistoryGuard.shouldSuppress("!!tp Steve"));
            assertFalse(CommandHistoryGuard.shouldSuppress("/!!tp Steve"));
            assertFalse(CommandHistoryGuard.shouldSuppress("!!tp Alex"));
            CommandHistoryGuard.run("tp Alex",()->{
                assertFalse(CommandHistoryGuard.shouldSuppress("!!tp Steve"));
                assertTrue(CommandHistoryGuard.shouldSuppress("/tp Alex"));
                return true;
            });
            assertTrue(CommandHistoryGuard.shouldSuppress("!!tp Steve"));
            return true;
        });
        assertFalse(CommandHistoryGuard.shouldSuppress("!!tp Steve"));
        assertThrows(IllegalStateException.class,()->CommandHistoryGuard.runChat("!!back",()->{
            throw new IllegalStateException("simulated send error");
        }));
        assertFalse(CommandHistoryGuard.shouldSuppress("!!back"));
    }

    @Test void nestedScopesRestorePreviousCommand(){
        CommandHistoryGuard.run("home",()->{
            assertTrue(CommandHistoryGuard.shouldSuppress("/home"));
            CommandHistoryGuard.run("/server lobby",()->{
                assertFalse(CommandHistoryGuard.shouldSuppress("/home"));
                assertTrue(CommandHistoryGuard.shouldSuppress("/server lobby"));
                return true;
            });
            assertTrue(CommandHistoryGuard.shouldSuppress("/home"));
            return true;
        });
        assertFalse(CommandHistoryGuard.shouldSuppress("/home"));
    }

    @Test void exceptionsCannotLeakHistorySuppression(){
        assertThrows(IllegalStateException.class,()->CommandHistoryGuard.run("msg Alice hi",()->{
            assertTrue(CommandHistoryGuard.shouldSuppress("/msg Alice hi"));
            throw new IllegalStateException("simulated failed send");
        }));
        assertFalse(CommandHistoryGuard.shouldSuppress("/msg Alice hi"));
    }

    @Test void scopedMixinAndGuiDispatchAreWired()throws Exception{
        String root=java.nio.file.Path.of("").toAbsolutePath().toString();
        String mixin=java.nio.file.Files.readString(java.nio.file.Path.of(root,"src/main/java/ddstweak/mixin/feature/cameraplayerselector/ChatHistoryGuardMixin.java"));
        String config=java.nio.file.Files.readString(java.nio.file.Path.of(root,"src/main/resources/dds-tweak.mixins.json"));
        String compat=java.nio.file.Files.readString(java.nio.file.Path.of(root,"src/main/java/ddstweak/platform/minecraft/MinecraftCompat.java"));
        assertTrue(mixin.contains("@Inject(method=\"addRecentChat\",at=@At(\"HEAD\"),cancellable=true)"));
        assertTrue(mixin.contains("CommandHistoryGuard.shouldSuppress(message)"));
        assertTrue(config.contains("feature.cameraplayerselector.ChatHistoryGuardMixin"));
        assertTrue(compat.contains("CommandHistoryGuard.run(command,()->sendCommand(minecraft,command))"));
        assertTrue(compat.contains("connection.sendCommand(command)"));
    }
}
