package ddstweak.integration.dds;

import ddstweak.integration.dds.fakeplayer.client.DdsBuiltinResourcePackCompat;
import ddstweak.integration.dds.undoredo.DdsUndoRedoClientNetwork;
import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import ddstweak.integration.dds.fakeplayer.DdsFakePlayerIdentityClient;
import ddstweak.integration.dds.fakeplayer.DdsFakePlayerClientNetwork;
import ddstweak.integration.org.playerinventory.OrgFakePlayerIdentityClient;
import ddstweak.network.ownership.DdsLocalOwnershipResolver;
import ddstweak.network.DdsClientNetwork;
import ddstweak.playerinventory.PlayerInventorySessionState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Registers DDS protocol slices that still require a DDS server. Player-inventory UI is intentionally not one of them. */
public final class DdsIntegrationBootstrap {
    private static boolean initialized;
    private DdsIntegrationBootstrap(){}
    public static synchronized void initialize(){if(initialized)return;PlayerInventorySessionState.initialize();DdsBuiltinResourcePackCompat.register();DdsFakePlayerIdentityClient.initialize();OrgFakePlayerIdentityClient.initialize();if(DdsLocalOwnershipResolver.current().tweakOwnsFeatureClient()){DdsFakePlayerClientNetwork.initializeExtension();DdsQcaClientNetwork.initialize();DdsUndoRedoClientNetwork.initialize();}ClientTickEvents.END_CLIENT_TICK.register(client->{DdsClientNetwork.onClientTick();DdsFakePlayerClientNetwork.onClientTick();DdsQcaClientNetwork.onClientTick();});initialized=true;}
}
