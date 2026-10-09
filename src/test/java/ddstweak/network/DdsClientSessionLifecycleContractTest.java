package ddstweak.network;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

final class DdsClientSessionLifecycleContractTest {
    private static final Path JAVA=Path.of("").toAbsolutePath().resolve("src/main/java");

    @Test void tickDetectsDisconnectAndConnectionSwitchEvenWithoutFeatureTraffic() throws IOException {
        String network=read("ddstweak/network/DdsClientNetwork.java");
        assertTrue(network.contains("onClientTick(){if(!ensureCurrentConnection())"));
        assertTrue(network.contains("if(previous!=current)resetConnection(current)"));
        assertTrue(network.contains("resetConnection(null)"));
        assertTrue(network.contains("deferredHello=false;DdsClientSessionHooks.resetConnection()"));
        assertTrue(network.contains("STATE.resetProtocolState();STATE.bindConnection(connection)"));
    }

    @Test void payloadDispatchKeepsHelloUnknownAndStaleConnectionShareable() throws IOException {
        String network=read("ddstweak/network/DdsClientNetwork.java");
        assertTrue(network.contains("if(c==null||source!=c||STATE.connection()!=c)return false;"));
        assertTrue(network.contains("if(DdsProtocol.HELLO_S2C.equals(id)){handleHello(c,payload.data());return false;}"));
        assertTrue(network.contains("if(handler==null)return false;"));
        assertTrue(network.contains("ordered.accept(c,payload.data());return true;"));
        assertTrue(network.contains("handler.accept(payload.data())"));
    }

    @Test void providerStateJoinsTheSameConnectionResetRegistry() throws IOException {
        String registry=read("ddstweak/playerinventory/PlayerInventorySessionState.java");
        String bootstrap=read("ddstweak/integration/dds/DdsIntegrationBootstrap.java");
        assertTrue(registry.contains("registerConnectionResetHook(OrgPlayerInventoryOpenIntent::reset"));
        assertTrue(registry.contains("registerConnectionResetHook(PlayerInventoryClientActionState::reset"));
        assertTrue(registry.contains("registerConnectionResetHook(PlayerInventoryOpenContext::clear"));
        assertTrue(bootstrap.contains("PlayerInventorySessionState.initialize()"));
    }

    @Test void protocolLossDoesNotClearProviderSessionState() throws IOException {
        String hooks=read("ddstweak/network/DdsClientSessionHooks.java");
        String network=read("ddstweak/network/DdsClientNetwork.java");
        assertTrue(hooks.contains("resetProtocol()"));
        assertTrue(hooks.contains("resetConnection()"));
        assertTrue(network.contains("DdsClientSessionHooks.resetProtocol()"));
        assertTrue(network.contains("DdsClientSessionHooks.resetConnection()"));
    }

    @Test void orgFakeUuidStateUsesTheUnifiedResetInsteadOfSeparateConnectionEvents() throws IOException {
        String identity=read("ddstweak/integration/org/playerinventory/OrgFakePlayerIdentityClient.java");
        assertTrue(identity.contains("DdsClientSessionHooks.registerConnectionResetHook(OrgFakePlayerIdentityClient::clear)"));
        assertTrue(identity.contains("public static void clear(){FAKE.clear();}"));
        assertFalse(identity.contains("ClientPlayConnectionEvents.JOIN"));
        assertFalse(identity.contains("ClientPlayConnectionEvents.DISCONNECT"));
    }

    @Test void loginMixinOnlyObservesProviderCommands() throws IOException {
        String mixin=read("ddstweak/mixin/playerinventory/ClientPacketListenerMixin.java");
        assertTrue(mixin.contains("OrgPlayerInventoryOpenIntent.observeCommand(command)"));
        assertFalse(mixin.contains("PlayerInventoryClientActionState.reset()"));
        assertFalse(mixin.contains("PlayerInventoryOpenContext.clear()"));
    }

    private static String read(String path) throws IOException { return Files.readString(JAVA.resolve(path)); }
}
