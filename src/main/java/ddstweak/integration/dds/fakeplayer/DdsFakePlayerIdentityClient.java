package ddstweak.integration.dds.fakeplayer;

import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import ddstweak.network.DdsClientNetwork;
import ddstweak.network.DdsClientSessionHooks;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** Optional DDS authority. Absent/disabled DDS leaves provider UI independent. */
public final class DdsFakePlayerIdentityClient {
    public static final String PACKET_ID="fake_player_identity_s2c";
    private static final FakePlayerIdentityState STATE=new FakePlayerIdentityState();
    private static boolean initialized;
    private DdsFakePlayerIdentityClient(){}
    public static synchronized void initialize(){
        if(initialized)return;
        DdsClientNetwork.registerS2CHandler(PACKET_ID,DdsFakePlayerIdentityClient::receive);
        DdsClientSessionHooks.registerResetHook(STATE::clear);
        initialized=true;
    }
    public static PlayerInventoryTargetType resolve(UUID uuid){
        return DdsClientNetwork.isServerProtocolActive()?STATE.resolve(uuid):PlayerInventoryTargetType.UNKNOWN;
    }
    private static void receive(CompoundTag data){
        if(MinecraftCompat.intOr(data,"schema",0)!=1)return;
        String kind=MinecraftCompat.stringOr(data,"kind","");
        if("snapshot".equals(kind)){
            STATE.acceptSnapshot(MinecraftCompat.stringOr(data,"uuids",""));
            return;
        }
        if(!"add".equals(kind)&&!"remove".equals(kind))return;
        try{
            UUID uuid=UUID.fromString(MinecraftCompat.stringOr(data,"uuid",""));
            STATE.applyChange(uuid,"add".equals(kind));
        }catch(IllegalArgumentException ignored){}
    }
}
