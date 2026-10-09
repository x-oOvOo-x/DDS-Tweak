package ddstweak.integration.org.playerinventory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import ddstweak.network.DdsClientSessionHooks;
//#if MC >= 260100
//$$ import net.fabricmc.loader.api.FabricLoader;
//$$ import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//$$ import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//#endif

/** Org's explicit positive fake-player notifications; false means removal, not REAL. */
public final class OrgFakePlayerIdentityClient {
    private static final Set<UUID> FAKE=ConcurrentHashMap.newKeySet();
    private static boolean initialized;
    private OrgFakePlayerIdentityClient(){}
    public static synchronized void initialize(){
        if(initialized)return;
        //#if MC >= 260100
        //$$ if(!FabricLoader.getInstance().isModLoaded("carpet-org-addition")){
        //$$     PayloadTypeRegistry.clientboundPlay().register(OrgPlayerTypeSyncPayload.TYPE,OrgPlayerTypeSyncPayload.CODEC);
        //$$     ClientPlayNetworking.registerGlobalReceiver(OrgPlayerTypeSyncPayload.TYPE,(payload,context)->receive(payload.uuid(),payload.fake()));
        //$$ }
        //#endif
        DdsClientSessionHooks.registerConnectionResetHook(OrgFakePlayerIdentityClient::clear);
        initialized=true;
    }
    public static boolean isFake(UUID uuid){return uuid!=null&&FAKE.contains(uuid);}
    public static void receive(UUID uuid,boolean fake){if(uuid==null)return;if(fake)FAKE.add(uuid);else FAKE.remove(uuid);}
    public static void clear(){FAKE.clear();}
}
