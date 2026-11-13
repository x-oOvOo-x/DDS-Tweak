package ddstweak.network;

import net.minecraft.client.multiplayer.ClientPacketListener;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Stores handshake, server capability and connection-scoped state.
 * 保存握手、服务端能力及连接级状态。
 */
final class DdsClientState {
    private static final int MAX_HELLO_ATTEMPTS = 3;
    private static final long FIRST_RETRY_NANOS = 1_000_000_000L;
    private static final long SECOND_RETRY_NANOS = 3_000_000_000L;

    private volatile boolean serverProtocolActive;
    private volatile int c2sAvailabilitySchema;
    private volatile Set<String> serverSupportedC2S = Collections.emptySet();
    private volatile long lastHelloNanos;
    private volatile int helloAttempts;
    private volatile boolean helloAnswered;
    private volatile WeakReference<ClientPacketListener> connectionRef = new WeakReference<>(null);

    boolean isServerProtocolActive(){return serverProtocolActive;}
    boolean doesServerSupport(String id){return serverProtocolActive&&serverSupportedC2S.contains(id);}
    boolean hasC2SAvailabilitySchema(){return serverProtocolActive&&c2sAvailabilitySchema==1;}
    boolean canTakeOverInput(String id){return hasC2SAvailabilitySchema()&&serverSupportedC2S.contains(id);}
    ClientPacketListener connection(){return connectionRef.get();}
    long lastHelloNanos(){return lastHelloNanos;}

    void bindConnection(ClientPacketListener c){connectionRef=new WeakReference<>(c);}
    void markHelloSent(long now){
        lastHelloNanos=now;
        helloAttempts=Math.min(helloAttempts+1,MAX_HELLO_ATTEMPTS);
    }

    /** A silent server gets two retries; any HELLO response ends probing for this connection. */
    boolean shouldRetryHello(long now){
        if(helloAnswered||helloAttempts==0||helloAttempts>=MAX_HELLO_ATTEMPTS)return false;
        long delay=helloAttempts==1?FIRST_RETRY_NANOS:SECOND_RETRY_NANOS;
        return now-lastHelloNanos>=delay;
    }

    void updateHandshake(boolean active,Set<String> supported,int schema){
        serverSupportedC2S=!active||supported==null||supported.isEmpty()
                ?Collections.emptySet():Collections.unmodifiableSet(new LinkedHashSet<>(supported));
        c2sAvailabilitySchema=active?schema:0;
        serverProtocolActive=active;
        helloAnswered=true;
    }

    void resetProtocolState(){
        serverSupportedC2S=Collections.emptySet();
        c2sAvailabilitySchema=0;
        serverProtocolActive=false;
        lastHelloNanos=0L;
        helloAttempts=0;
        helloAnswered=false;
    }
}
