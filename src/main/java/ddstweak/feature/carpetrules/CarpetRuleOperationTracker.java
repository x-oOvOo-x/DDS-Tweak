package ddstweak.feature.carpetrules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Carpet command reply confirms command dispatch, not necessarily rule
 * acceptance: Carpet's temporary rule path can return 1 after validation
 * failure. Temporary changes require a matching rule sync; persistent changes
 * in an integrated world additionally require a matching on-disk config read.
 */
public final class CarpetRuleOperationTracker {
    public enum Status {
        SENT, WAITING_FOR_SYNC, VERIFIED, COMMAND_ACKNOWLEDGED,
        WAITING_FOR_PERSISTENCE, PERSISTED, PERSISTENCE_MISMATCH,
        PERSISTENCE_UNCONFIRMED, REJECTED, UNCONFIRMED
    }

    private static final int MAX_HISTORY = 128;
    private final Map<String, Operation> operations=new LinkedHashMap<>();
    private long sequence;

    public synchronized Operation begin(CarpetRuleCatalog catalog, CarpetRuleCatalog.RuleKey key,
                                         CarpetRuleCommands.Mode mode, String target, long nowMillis) {
        CarpetRuleCatalog.RemoteRule current=catalog.get(key);
        expire(nowMillis);
        if (!catalog.isReady() || current == null || pendingCount() != 0) return null;
        if (mode==CarpetRuleCommands.Mode.TEMPORARY && target.equals(current.value())) return null;
        String id="dt-rules-"+catalog.generation()+"-"+(++sequence);
        Operation operation=new Operation(id,catalog.generation(),key,mode,
                current.value(),target,nowMillis);
        operations.put(id,operation);
        trimHistory();
        return operation;
    }

    public synchronized void failToSend(String id) { operations.remove(id); }

    /**
     * 1.14 Carpet has no clientCommand reply channel. After dispatch via vanilla
     * player chat, await actual rule sync or a fresh persisted config snapshot.
     * 旧版不伪造回执，仅依据服务端规则或持久化配置的变化确认结果。
     */
    public synchronized void awaitLocalVerification(String id,long generation){
        Operation op=operations.get(id);
        if(op==null||op.generation!=generation||op.status!=Status.SENT)return;
        if(op.mode==CarpetRuleCommands.Mode.TEMPORARY)
            op.status=op.observedTarget?Status.VERIFIED:Status.WAITING_FOR_SYNC;
        else op.status=Status.WAITING_FOR_PERSISTENCE;
    }


    public synchronized void acknowledge(String id,int returnCode,boolean hasError,long generation){
        acknowledge(id,returnCode,hasError,generation,false);
    }

    /**
     * Carpet replies vary by version: old versions send "code", some send no
     * numeric result, and newer versions send "return". A missing/zero result
     * without an explicit error is not evidence of rejection or success.
     */
    public synchronized void acknowledge(String id,int returnCode,boolean hasError,
                                          long generation,boolean verifyPersistence){
        Operation op=operations.get(id);
        if(op==null||generation!=op.generation||op.status!=Status.SENT)return;
        if(hasError||returnCode<0){
            op.status=Status.REJECTED;
        }else if(op.mode!=CarpetRuleCommands.Mode.TEMPORARY){
            op.status=verifyPersistence?Status.WAITING_FOR_PERSISTENCE:
                    returnCode>0?Status.COMMAND_ACKNOWLEDGED:Status.UNCONFIRMED;
        }else{
            op.status=op.observedTarget?Status.VERIFIED:Status.WAITING_FOR_SYNC;
        }
    }

    public synchronized void observe(CarpetRuleCatalog.RuleKey key,String value,long generation){
        for(Operation op:operations.values()){
            if(op.generation!=generation||!op.key.equals(key)||
                    (op.status!=Status.SENT&&op.status!=Status.WAITING_FOR_SYNC))continue;
            if(op.mode==CarpetRuleCommands.Mode.TEMPORARY&&op.target.equals(value)){
                op.observedTarget=true;
                if(op.status==Status.WAITING_FOR_SYNC)op.status=Status.VERIFIED;
            }
        }
    }

    /** Verify persistent operations only against a known manager's fresh disk snapshot. */
    public synchronized void observePersistence(Map<CarpetRuleCatalog.RuleKey,String> values,
                                                Set<String> knownManagers,long generation){
        for(Operation op:operations.values()){
            if(op.generation!=generation||op.status!=Status.WAITING_FOR_PERSISTENCE
                    ||!knownManagers.contains(op.key.manager()))continue;
            boolean present=values.containsKey(op.key);
            op.persistedValue=values.get(op.key);
            boolean matches=op.mode==CarpetRuleCommands.Mode.SET_DEFAULT
                    ?op.target.equals(op.persistedValue):!present;
            op.status=matches?Status.PERSISTED:Status.PERSISTENCE_MISMATCH;
        }
    }

    public synchronized void expire(long nowMillis){
        for(Operation op:operations.values()){
            if(nowMillis-op.sentAt<=15000)continue;
            if(op.status==Status.WAITING_FOR_PERSISTENCE)
                op.status=Status.PERSISTENCE_UNCONFIRMED;
            else if(op.status==Status.SENT||op.status==Status.WAITING_FOR_SYNC)
                op.status=Status.UNCONFIRMED;
        }
    }

    public synchronized Operation find(String id){return operations.get(id);}

    public synchronized List<Operation> history(){
        return Collections.unmodifiableList(new ArrayList<>(operations.values()));
    }

    /** Prevent overlapping writes while a command or its verification is pending. */
    public synchronized boolean hasPending(long nowMillis){
        expire(nowMillis);
        return pendingCount()!=0;
    }

    public synchronized void reset(){operations.clear();}

    private void trimHistory(){
        while(operations.size()>MAX_HISTORY){
            String oldestFinished=null;
            for(Map.Entry<String,Operation> entry:operations.entrySet()){
                Status status=entry.getValue().status;
                if(status!=Status.SENT&&status!=Status.WAITING_FOR_SYNC
                        &&status!=Status.WAITING_FOR_PERSISTENCE){
                    oldestFinished=entry.getKey();
                    break;
                }
            }
            if(oldestFinished==null)break;
            operations.remove(oldestFinished);
        }
    }

    private int pendingCount(){
        int count=0;
        for(Operation op:operations.values())
            if(op.status==Status.SENT||op.status==Status.WAITING_FOR_SYNC
                    ||op.status==Status.WAITING_FOR_PERSISTENCE)count++;
        return count;
    }

    public static final class Operation {
        private final String id;
        private final long generation,sentAt;
        private final CarpetRuleCatalog.RuleKey key;
        private final CarpetRuleCommands.Mode mode;
        private final String original,target;
        private Status status=Status.SENT;
        private boolean observedTarget;
        private String persistedValue;

        private Operation(String id,long generation,CarpetRuleCatalog.RuleKey key,
                          CarpetRuleCommands.Mode mode,String original,String target,long sentAt){
            this.id=id;this.generation=generation;this.key=key;this.mode=mode;
            this.original=original;this.target=target;this.sentAt=sentAt;
        }
        public String id(){return id;}
        public CarpetRuleCatalog.RuleKey key(){return key;}
        public CarpetRuleCommands.Mode mode(){return mode;}
        public String target(){return target;}
        public String original(){return original;}
        public long generation(){return generation;}
        public long sentAt(){return sentAt;}
        public Status status(){return status;}
        public String persistedValue(){return persistedValue;}
    }
}
