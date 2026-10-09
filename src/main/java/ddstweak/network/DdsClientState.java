package ddstweak.network;

import net.minecraft.client.multiplayer.ClientPacketListener;
import java.lang.ref.WeakReference;
import java.util.*;

final class DdsClientState {
    private volatile boolean serverProtocolActive;private volatile int c2sAvailabilitySchema;private volatile Set<String> serverSupportedC2S=Collections.emptySet();private volatile long lastHelloNanos;private volatile WeakReference<ClientPacketListener> connectionRef=new WeakReference<>(null);
    boolean isServerProtocolActive(){return serverProtocolActive;}boolean doesServerSupport(String id){return serverProtocolActive&&serverSupportedC2S.contains(id);}boolean hasC2SAvailabilitySchema(){return serverProtocolActive&&c2sAvailabilitySchema==1;}boolean canTakeOverInput(String id){return hasC2SAvailabilitySchema()&&serverSupportedC2S.contains(id);}ClientPacketListener connection(){return connectionRef.get();}long lastHelloNanos(){return lastHelloNanos;}
    void bindConnection(ClientPacketListener c){connectionRef=new WeakReference<>(c);}void markHelloSent(long n){lastHelloNanos=n;}
    void updateHandshake(boolean active,Set<String> supported,int schema){serverSupportedC2S=!active||supported==null||supported.isEmpty()?Collections.emptySet():Collections.unmodifiableSet(new LinkedHashSet<>(supported));c2sAvailabilitySchema=active?schema:0;serverProtocolActive=active;}
    void resetProtocolState(){serverSupportedC2S=Collections.emptySet();c2sAvailabilitySchema=0;serverProtocolActive=false;lastHelloNanos=0L;}
}
