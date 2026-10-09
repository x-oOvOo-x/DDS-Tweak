package ddstweak.integration.dds.fakeplayer;

import java.util.ArrayDeque;
import java.util.Deque;

/** Connection-bound FIFO pairing DDS extension contexts with the following GCA OpenScreen packet. */
final class DdsFakePlayerExtensionOpenState<C>{
    private static final int MAX_PENDING=128;
    private final long maxAgeNanos;
    private final Deque<Pending<C>> pending=new ArrayDeque<>();
    DdsFakePlayerExtensionOpenState(long maxAgeNanos){if(maxAgeNanos<=0)throw new IllegalArgumentException("maxAgeNanos");this.maxAgeNanos=maxAgeNanos;}
    synchronized void expect(C connection,DdsFakePlayerExtensionContext context,long now){
        if(connection==null||context==null)return;
        if(!pending.isEmpty()&&pending.peekLast().connection!=connection)pending.clear();else prune(now);
        if(pending.size()>=MAX_PENDING)pending.removeFirst();
        pending.addLast(new Pending<>(connection,context,now));
    }
    synchronized DdsFakePlayerExtensionContext peek(C connection,int containerId,long now){
        prune(now);
        while(!pending.isEmpty()){
            Pending<C> p=pending.peekFirst();
            if(p.connection!=connection)return null;
            if(p.context.containerId()==containerId)return p.context;
            pending.removeFirst();
        }
        return null;
    }
    synchronized boolean commit(C connection,int containerId,DdsFakePlayerExtensionContext context){
        Pending<C> p=pending.peekFirst();
        if(p==null||p.connection!=connection||p.context!=context||p.context.containerId()!=containerId)return false;
        pending.removeFirst();return true;
    }
    synchronized void discard(C connection,int containerId,long generation){
        for(java.util.Iterator<Pending<C>> it=pending.iterator();it.hasNext();){
            Pending<C> p=it.next();
            if(p.connection==connection&&p.context.containerId()==containerId
                    &&p.context.strictSession()&&p.context.sessionGeneration()==generation)it.remove();
        }
    }
    synchronized void clear(){pending.clear();}
    private void prune(long now){while(!pending.isEmpty()){long age=now-pending.peekFirst().createdAt;if(age>=0&&age<=maxAgeNanos)return;pending.removeFirst();}}
    private static final class Pending<C>{final C connection;final DdsFakePlayerExtensionContext context;final long createdAt;Pending(C connection,DdsFakePlayerExtensionContext context,long createdAt){this.connection=connection;this.context=context;this.createdAt=createdAt;}}
}
