package ddstweak.integration.dds.fakeplayer;

import java.util.ArrayDeque;
import java.util.Deque;

/** Connection-bound FIFO pairing DDS menu markers with following vanilla OpenScreen packets. */
public final class DdsFakePlayerMenuOpenState<C>{
    private static final int DEFAULT_MAX_PENDING=128;private final long maxAgeNanos;private final int maxPending;private final Deque<Pending<C>> pending=new ArrayDeque<>();
    public DdsFakePlayerMenuOpenState(long maxAgeNanos){this(maxAgeNanos,DEFAULT_MAX_PENDING);}DdsFakePlayerMenuOpenState(long maxAgeNanos,int maxPending){if(maxAgeNanos<=0||maxPending<=0)throw new IllegalArgumentException("limits must be positive");this.maxAgeNanos=maxAgeNanos;this.maxPending=maxPending;}
    public synchronized void expect(C connection,DdsFakePlayerOpenContext context,long now){if(connection==null||context==null)return;if(!pending.isEmpty()&&pending.peekLast().connection!=connection)pending.clear();else prune(now);if(pending.size()>=maxPending)pending.removeFirst();pending.addLast(new Pending<>(connection,context,now));}
    public synchronized DdsFakePlayerOpenContext peek(C connection,int containerId,long now){prune(now);while(!pending.isEmpty()){Pending<C> p=pending.peekFirst();if(p.connection!=connection)return null;if(p.context.containerId()==containerId)return p.context;pending.removeFirst();}return null;}
    public synchronized boolean commit(C connection,int containerId,DdsFakePlayerOpenContext context){Pending<C> p=pending.peekFirst();if(p==null||p.connection!=connection||p.context!=context||p.context.containerId()!=containerId)return false;pending.removeFirst();return true;}
    public synchronized void clear(){pending.clear();}
    private void prune(long now){while(!pending.isEmpty()){long age=now-pending.peekFirst().createdAtNanos;if(age>=0&&age<=maxAgeNanos)return;pending.removeFirst();}}
    private static final class Pending<C>{final C connection;final DdsFakePlayerOpenContext context;final long createdAtNanos;Pending(C connection,DdsFakePlayerOpenContext context,long createdAtNanos){this.connection=connection;this.context=context;this.createdAtNanos=createdAtNanos;}}
}
