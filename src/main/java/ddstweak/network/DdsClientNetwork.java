package ddstweak.network;

import ddstweak.DDSTweak;
import ddstweak.network.ownership.DdsLegacyClientCapabilityBridge;
import ddstweak.network.ownership.DdsLocalOwnershipDecision;
import ddstweak.network.ownership.DdsLocalOwnershipResolver;
import ddstweak.network.transport.DdsClientTransports;
import ddstweak.platform.minecraft.MinecraftCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
import java.util.function.*;

/** Generic DDS client protocol/session layer. Wire details belong to DdsClientTransport. */
public final class DdsClientNetwork {
    private static final long HELLO_RETRY_NANOS=1_000_000_000L;
    private static final DdsClientState STATE=new DdsClientState();
    private static final DdsClientCapabilityRegistry CAPABILITIES=new DdsClientCapabilityRegistry();
    private static volatile boolean deferredHello;
    private DdsClientNetwork(){}
    public static void registerC2S(String id){CAPABILITIES.registerC2S(id);}
    public static void registerS2CHandler(String id,Consumer<CompoundTag> handler){CAPABILITIES.registerS2C(id,handler);}
    public static void registerOrderedS2CHandler(String id,BiConsumer<ClientPacketListener,CompoundTag> handler){CAPABILITIES.registerOrderedS2C(id,handler);}
    public static void onConnectedToServer(){ClientPacketListener c=Minecraft.getInstance().getConnection();resetConnection(c);if(c==null)return;if(DdsLocalOwnershipResolver.current()==DdsLocalOwnershipDecision.LEGACY_COMPAT)deferredHello=true;else sendHello();}
    public static void onClientTick(){if(!ensureCurrentConnection()){deferredHello=false;return;}if(!deferredHello)return;deferredHello=false;sendHello();}
    public static boolean isServerProtocolActive(){return STATE.isServerProtocolActive();}
    public static boolean hasC2SAvailabilitySchema(){return STATE.hasC2SAvailabilitySchema();}
    public static boolean canTakeOverInput(String id){return CAPABILITIES.hasC2S(id)&&ensureCurrentConnection()&&STATE.canTakeOverInput(id);}
    public static boolean canSend(String id){if(!CAPABILITIES.hasC2S(id)||!ensureCurrentConnection())return false;if(!STATE.isServerProtocolActive()){retryHelloIfNeeded();return false;}return STATE.doesServerSupport(id);}
    public static boolean send(String id,CompoundTag data){if(!canSend(id))return false;if(DdsLocalOwnershipResolver.current()==DdsLocalOwnershipDecision.LEGACY_COMPAT)sendHello();return sendRaw(id,data);}
    public static boolean handlePayload(Object source,DdsProtocol.Payload payload){Minecraft mc=Minecraft.getInstance();ClientPacketListener c=mc.getConnection();if(c==null||source!=c||STATE.connection()!=c)return false;String id=payload.packetId();if(DdsProtocol.HELLO_S2C.equals(id)){handleHello(c,payload.data());return false;}BiConsumer<ClientPacketListener,CompoundTag> ordered=CAPABILITIES.orderedS2C(id);Consumer<CompoundTag> handler=CAPABILITIES.asyncS2C(id);boolean legacy=DdsLocalOwnershipResolver.current()==DdsLocalOwnershipDecision.LEGACY_COMPAT;if(ordered!=null&&(STATE.isServerProtocolActive()||legacy)){ordered.accept(c,payload.data());return true;}if(!STATE.isServerProtocolActive())return handler!=null;if(handler==null)return false;Runnable dispatch=()->{if(mc.getConnection()==c&&STATE.connection()==c&&STATE.isServerProtocolActive())handler.accept(payload.data());};if(mc.isSameThread())dispatch.run();else mc.execute(dispatch);return true;}
    private static void handleHello(ClientPacketListener c,CompoundTag data){Set<String> local=CAPABILITIES.snapshotC2S();int protocol=MinecraftCompat.intOr(data,"protocol",0);boolean enabled=MinecraftCompat.intOr(data,"protocol_enabled",0)!=0;Set<String> supported=protocol==DdsProtocol.VERSION&&enabled?DdsProtocol.decodeKnownIds(MinecraftCompat.stringOr(data,"supported_c2s",""),local):Collections.emptySet();synchronized(STATE){if(Minecraft.getInstance().getConnection()!=c||STATE.connection()!=c)return;STATE.updateHandshake(protocol==DdsProtocol.VERSION&&enabled,supported,MinecraftCompat.intOr(data,"c2s_availability_schema",0));}if(!STATE.isServerProtocolActive()){deferredHello=false;DdsClientSessionHooks.resetProtocol();}}
    private static boolean ensureCurrentConnection(){ClientPacketListener current=Minecraft.getInstance().getConnection();ClientPacketListener previous;boolean stale;synchronized(STATE){previous=STATE.connection();stale=STATE.isServerProtocolActive()||STATE.lastHelloNanos()!=0L;}if(current==null){if(previous!=null||stale)resetConnection(null);return false;}if(previous!=current)resetConnection(current);return true;}
    private static void resetConnection(ClientPacketListener connection){deferredHello=false;DdsClientSessionHooks.resetConnection();synchronized(STATE){STATE.resetProtocolState();STATE.bindConnection(connection);}}
    private static void retryHelloIfNeeded(){long now=System.nanoTime();if(STATE.lastHelloNanos()==0L||now-STATE.lastHelloNanos()>=HELLO_RETRY_NANOS)sendHello();}
    private static void sendHello(){CompoundTag d=new CompoundTag();d.putInt("protocol",DdsProtocol.VERSION);d.putString("mod_version",modVersion());d.putString("supported_s2c",DdsProtocol.encodeIds(advertisedS2C()));d.putString("supported_c2s",DdsProtocol.encodeIds(advertisedC2S()));if(sendRaw(DdsProtocol.HELLO_C2S,d))STATE.markHelloSent(System.nanoTime());}
    private static Set<String> advertisedC2S(){LinkedHashSet<String> ids=new LinkedHashSet<>(CAPABILITIES.snapshotC2S());if(DdsLocalOwnershipResolver.current()==DdsLocalOwnershipDecision.LEGACY_COMPAT)ids.addAll(DdsLegacyClientCapabilityBridge.snapshot().c2s());return ids;}
    private static Set<String> advertisedS2C(){LinkedHashSet<String> ids=new LinkedHashSet<>(CAPABILITIES.snapshotS2C());if(DdsLocalOwnershipResolver.current()==DdsLocalOwnershipDecision.LEGACY_COMPAT)ids.addAll(DdsLegacyClientCapabilityBridge.snapshot().s2c());return ids;}
    private static String modVersion(){return FabricLoader.getInstance().getModContainer(DDSTweak.MOD_ID).map(c->c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");}
    private static boolean sendRaw(String id,CompoundTag data){ClientPacketListener c=Minecraft.getInstance().getConnection();return c!=null&&DdsClientTransports.current().send(c,id,data);}
}
