package ddstweak.network;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
import java.util.function.*;

/** Local DDS client capability/handler registry. */
public final class DdsClientCapabilityRegistry {
    private final Set<String> c2s=new LinkedHashSet<>();
    private final Map<String,Consumer<CompoundTag>> asyncS2C=new LinkedHashMap<>();
    private final Map<String,BiConsumer<ClientPacketListener,CompoundTag>> orderedS2C=new LinkedHashMap<>();
    public synchronized void registerC2S(String id){validateId(id);if(!c2s.add(id))throw new IllegalStateException("Duplicate DDS C2S capability: "+id);}
    public synchronized void registerS2C(String id,Consumer<CompoundTag> handler){validate(id,handler);if(orderedS2C.containsKey(id)||asyncS2C.putIfAbsent(id,handler)!=null)throw new IllegalStateException("Duplicate DDS S2C handler: "+id);}
    public synchronized void registerOrderedS2C(String id,BiConsumer<ClientPacketListener,CompoundTag> handler){validate(id,handler);if(asyncS2C.containsKey(id)||orderedS2C.putIfAbsent(id,handler)!=null)throw new IllegalStateException("Duplicate DDS S2C handler: "+id);}
    public synchronized boolean hasC2S(String id){return c2s.contains(id);}
    public synchronized Set<String> snapshotC2S(){return Collections.unmodifiableSet(new LinkedHashSet<>(c2s));}
    public synchronized Set<String> snapshotS2C(){LinkedHashSet<String> ids=new LinkedHashSet<>(orderedS2C.keySet());ids.addAll(asyncS2C.keySet());return Collections.unmodifiableSet(ids);}
    public synchronized Consumer<CompoundTag> asyncS2C(String id){return asyncS2C.get(id);}
    public synchronized BiConsumer<ClientPacketListener,CompoundTag> orderedS2C(String id){return orderedS2C.get(id);}
    private static void validate(String id,Object handler){validateId(id);if(handler==null)throw new IllegalArgumentException("DDS handler must not be null: "+id);}
    private static void validateId(String id){if(id==null||id.trim().isEmpty())throw new IllegalArgumentException("DDS packet id must not be blank");}
}
