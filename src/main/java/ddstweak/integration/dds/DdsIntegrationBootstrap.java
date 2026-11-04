package ddstweak.integration.dds;

import ddstweak.integration.dds.fakeplayer.client.DdsBuiltinResourcePackCompat;
import ddstweak.feature.shulkerrestock.ShulkerRestockClientNetwork;
import ddstweak.integration.dds.undoredo.DdsUndoRedoClientNetwork;
import ddstweak.integration.dds.qca.DdsQcaClientNetwork;
import ddstweak.integration.dds.fakeplayer.DdsFakePlayerIdentityClient;
import ddstweak.integration.dds.fakeplayer.DdsFakePlayerClientNetwork;
import ddstweak.integration.org.playerinventory.OrgFakePlayerIdentityClient;
import ddstweak.network.ownership.DdsLocalOwnershipResolver;
import ddstweak.network.ownership.DdsLocalOwnershipDecision;
import ddstweak.network.DdsClientNetwork;
import ddstweak.network.DdsClientSessionHooks;
import ddstweak.playerinventory.PlayerInventorySessionState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Registers DDS protocol slices that still require a DDS server. Player-inventory UI is intentionally not one of them. */
public final class DdsIntegrationBootstrap {
    private static boolean initialized;
    private DdsIntegrationBootstrap(){}
    public static synchronized void initialize(){
        if(initialized)return;
        PlayerInventorySessionState.initialize();
        DdsBuiltinResourcePackCompat.register();
        ShulkerRestockClientNetwork.initialize();
        DdsFakePlayerIdentityClient.initialize();
        OrgFakePlayerIdentityClient.initialize();
        DdsLocalOwnershipDecision ownership=DdsLocalOwnershipResolver.current();
        if(ownership.tweakOwnsFeatureClient()){
            DdsFakePlayerClientNetwork.initializeExtension();
            DdsQcaClientNetwork.initialize();
            DdsUndoRedoClientNetwork.initialize();
        }else{
            //#if MC >= 12111
            // CDA still owns the legacy QCA/Undo client, but does not implement
            // strict Extension Context/Session S2C. Advertise only the sidecar
            // needed for server-authorized canonical Ender reopens.
            if(ownership==DdsLocalOwnershipDecision.LEGACY_COMPAT){
                DdsFakePlayerClientNetwork.initializeExtension();
                DdsClientSessionHooks.registerResetHook(DdsQcaClientNetwork::resetSession);
            }
            //#endif
        }
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            DdsClientNetwork.onClientTick();
            ShulkerRestockClientNetwork.onClientTick();
            DdsFakePlayerClientNetwork.onClientTick();
            DdsQcaClientNetwork.onClientTick();
        });
        initialized=true;
    }
}
