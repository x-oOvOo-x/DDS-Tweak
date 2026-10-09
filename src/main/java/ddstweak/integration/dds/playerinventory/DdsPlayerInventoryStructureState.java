package ddstweak.integration.dds.playerinventory;

import java.util.Objects;

/**
 * One strict structural transition at a time.
 * Identity is connection + exact source menu object + target + generation + request id.
 * A matching next-generation Context only stages the destination; OpenScreen completes it.
 */
public final class DdsPlayerInventoryStructureState<C,M,T>{
    public enum Operation{CANONICAL_ENDER,RETURN_PARENT}
    public enum Start{STARTED,EXISTING,BUSY,INVALID}
    public enum Result{ACCEPTED,REJECTED,IGNORED}

    private final long timeoutNanos;
    private Pending<C,M,T> pending;

    public DdsPlayerInventoryStructureState(long timeoutNanos){
        if(timeoutNanos<=0L)throw new IllegalArgumentException("timeoutNanos");
        this.timeoutNanos=timeoutNanos;
    }

    public synchronized Start begin(C connection,M sourceMenu,T target,int sourceContainerId,
                                    long sourceGeneration,long requestId,Operation operation,long now){
        if(connection==null||sourceMenu==null||target==null||sourceContainerId<0
                ||sourceGeneration<=0L||requestId<=0L||operation==null)return Start.INVALID;
        if(pending!=null){
            return pending.matches(connection,sourceMenu,target,sourceContainerId,sourceGeneration,requestId,operation)
                    ?Start.EXISTING:Start.BUSY;
        }
        pending=new Pending<>(connection,sourceMenu,target,sourceContainerId,sourceGeneration,requestId,operation,now);
        return Start.STARTED;
    }

    public synchronized Pending<C,M,T> current(C connection,long now){
        return pending!=null&&pending.connection==connection?pending:null;
    }

    public synchronized Result applyResult(C connection,int sourceContainerId,T target,long sourceGeneration,
                                           long requestId,String status,long now){
        if(pending==null||expired(now)||pending.connection!=connection
                ||pending.sourceContainerId!=sourceContainerId
                ||pending.sourceGeneration!=sourceGeneration
                ||pending.requestId!=requestId
                ||!Objects.equals(pending.target,target))return Result.IGNORED;
        if("accepted".equals(status)){
            pending.accepted=true;
            return Result.ACCEPTED;
        }
        if("rejected".equals(status)){
            pending=null;
            return Result.REJECTED;
        }
        return Result.IGNORED;
    }

    public synchronized boolean observeNextContext(C connection,T target,int destinationContainerId,
                                                   long nextGeneration,long now){
        if(pending==null||expired(now)||pending.connection!=connection||destinationContainerId<0
                ||!Objects.equals(pending.target,target)||!isNextGeneration(pending.sourceGeneration,nextGeneration))return false;
        if(pending.destinationContainerId>=0)
            return pending.destinationContainerId==destinationContainerId&&pending.destinationGeneration==nextGeneration;
        pending.destinationContainerId=destinationContainerId;
        pending.destinationGeneration=nextGeneration;
        return true;
    }

    public synchronized Pending<C,M,T> consumeObservedOpen(C connection,int destinationContainerId,T target,
                                                           long destinationGeneration,long now){
        if(pending==null||expired(now)||pending.connection!=connection
                ||pending.destinationContainerId!=destinationContainerId
                ||pending.destinationGeneration!=destinationGeneration
                ||!Objects.equals(pending.target,target))return null;
        Pending<C,M,T> completed=pending;
        pending=null;
        return completed;
    }

    public synchronized Pending<C,M,T> expire(C connection,long now){
        if(pending==null||pending.connection!=connection||!expired(now))return null;
        Pending<C,M,T> expired=pending;
        pending=null;
        return expired;
    }

    public synchronized Pending<C,M,T> clear(){
        Pending<C,M,T> cleared=pending;
        pending=null;
        return cleared;
    }

    private boolean expired(long now){
        long age=now-pending.startedNanos;
        return age<0L||age>=timeoutNanos;
    }

    private static boolean isNextGeneration(long source,long next){
        return source>0L&&source<Long.MAX_VALUE&&next==source+1L;
    }

    public static final class Pending<C,M,T>{
        private final C connection;
        private final M sourceMenu;
        private final T target;
        private final int sourceContainerId;
        private final long sourceGeneration;
        private final long requestId;
        private final Operation operation;
        private final long startedNanos;
        private boolean accepted;
        private int destinationContainerId=-1;
        private long destinationGeneration;

        private Pending(C connection,M sourceMenu,T target,int sourceContainerId,long sourceGeneration,
                        long requestId,Operation operation,long startedNanos){
            this.connection=connection;this.sourceMenu=sourceMenu;this.target=target;
            this.sourceContainerId=sourceContainerId;this.sourceGeneration=sourceGeneration;
            this.requestId=requestId;this.operation=operation;this.startedNanos=startedNanos;
        }

        private boolean matches(C c,M menu,T t,int id,long generation,long request,Operation op){
            return connection==c&&sourceMenu==menu&&Objects.equals(target,t)
                    &&sourceContainerId==id&&sourceGeneration==generation&&requestId==request&&operation==op;
        }

        public C connection(){return connection;}
        public M sourceMenu(){return sourceMenu;}
        public T target(){return target;}
        public int sourceContainerId(){return sourceContainerId;}
        public long sourceGeneration(){return sourceGeneration;}
        public long requestId(){return requestId;}
        public Operation operation(){return operation;}
        public long startedNanos(){return startedNanos;}
        public boolean accepted(){return accepted;}
        public int destinationContainerId(){return destinationContainerId;}
        public long destinationGeneration(){return destinationGeneration;}
        public boolean destinationObserved(){return destinationContainerId>=0&&destinationGeneration>0L;}
    }
}
