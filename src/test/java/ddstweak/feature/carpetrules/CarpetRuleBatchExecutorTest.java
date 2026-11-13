package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleBatchExecutorTest {
    private static final CarpetRuleCatalog.RuleKey FIRST =
            new CarpetRuleCatalog.RuleKey("carpet", "fastRule");
    private static final CarpetRuleCatalog.RuleKey LAST =
            new CarpetRuleCatalog.RuleKey("carpet", "commandPlayer");

    private static final class Fixture {
        final CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        final CarpetRuleOperationTracker tracker = new CarpetRuleOperationTracker();
        final CarpetRulePresetStore store = new CarpetRulePresetStore();
        final CarpetRuleBatchExecutor batch = new CarpetRuleBatchExecutor();
        final Object session = new Object();
        int sent;
        CarpetRuleCatalog.RuleKey sentKey;
        CarpetRuleOperationTracker.Operation operation;
        boolean permitted = true;

        final CarpetRuleBatchExecutor.Sender sender = new CarpetRuleBatchExecutor.Sender() {
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key) { return permitted; }
            @Override public CarpetRuleOperationTracker.Operation send(CarpetRuleCatalog.RuleKey key, String target) {
                sent++;
                sentKey=key;
                operation=tracker.begin(catalog,key,CarpetRuleCommands.Mode.TEMPORARY,target,1000);
                return operation;
            }
        };

        Fixture() {
            catalog.connect(session);
            assertTrue(catalog.replace(session,Arrays.asList(
                    new CarpetRuleCatalog.RemoteRule(FIRST,"false",true),
                    new CarpetRuleCatalog.RemoteRule(LAST,"false",true))));
            Map<CarpetRuleCatalog.RuleKey,String> targets=new LinkedHashMap<>();
            targets.put(LAST,"true");
            targets.put(FIRST,"true");
            assertTrue(store.put("batch","",0,false,targets));
        }

        void confirmed() {
            tracker.acknowledge(operation.id(),1,false,catalog.generation());
            catalog.update(session,new CarpetRuleCatalog.RemoteRule(sentKey,"true",true));
            tracker.observe(sentKey,"true",catalog.generation());
        }
    }

    @Test void sequentialWritesWaitForSyncAndPutPermissionRulesLast() {
        Fixture f=new Fixture();
        assertTrue(f.batch.start(f.catalog,f.store.get("batch"),f.sender));
        f.batch.advance(f.catalog,f.tracker,f.sender,1000);
        assertEquals(FIRST,f.sentKey);
        f.batch.advance(f.catalog,f.tracker,f.sender,1300);
        assertEquals(1,f.sent);
        f.confirmed();
        f.batch.advance(f.catalog,f.tracker,f.sender,1500);
        assertEquals(LAST,f.sentKey);
        assertEquals(2,f.sent);
        f.confirmed();
        f.batch.advance(f.catalog,f.tracker,f.sender,1700);
        assertEquals(CarpetRuleBatchExecutor.Phase.COMPLETED,f.batch.phase());
        assertEquals(2,f.batch.completed());
    }

    @Test void refusedAndTimedOutCommandsStopWithoutSendingNext() {
        Fixture f=new Fixture();
        f.batch.start(f.catalog,f.store.get("batch"),f.sender);
        f.batch.advance(f.catalog,f.tracker,f.sender,1000);
        f.tracker.acknowledge(f.operation.id(),0,true,f.catalog.generation());
        f.batch.advance(f.catalog,f.tracker,f.sender,1300);
        assertEquals(CarpetRuleBatchExecutor.Failure.SERVER_REJECTED,f.batch.failure());
        assertEquals(1,f.sent);
        f.batch.start(f.catalog,f.store.get("batch"),f.sender);
        f.batch.advance(f.catalog,f.tracker,f.sender,1000);
        f.batch.advance(f.catalog,f.tracker,f.sender,17000);
        assertEquals(CarpetRuleBatchExecutor.Failure.UNCONFIRMED,f.batch.failure());
        assertEquals(2,f.sent);
    }

    @Test void changedConnectionAndChangedRulePreventAnyWrite() {
        Fixture f=new Fixture();
        f.batch.start(f.catalog,f.store.get("batch"),f.sender);
        f.catalog.update(f.session,new CarpetRuleCatalog.RemoteRule(FIRST,"other",true));
        f.batch.advance(f.catalog,f.tracker,f.sender,1000);
        assertEquals(CarpetRuleBatchExecutor.Failure.CHANGED_SINCE_REVIEW,f.batch.failure());
        assertEquals(0,f.sent);
        f.catalog.connect(new Object());
        f.batch.advance(f.catalog,f.tracker,f.sender,1000);
        assertEquals(0,f.sent);
    }

    @Test void missingAndUnauthorizedTargetsAreSkippedAndReported() {
        Fixture f=new Fixture();
        Map<CarpetRuleCatalog.RuleKey,String> targets=new LinkedHashMap<>();
        targets.put(FIRST,"true");
        targets.put(new CarpetRuleCatalog.RuleKey("unknown","missing"),"false");
        assertTrue(f.store.put("missing","",0,false,targets));
        assertTrue(f.batch.start(f.catalog,f.store.get("missing"),f.sender));
        assertEquals(1,f.batch.skippedMissing());
        assertEquals(1,f.batch.total());
        f.batch.cancel();
        f.permitted=false;
        assertFalse(f.batch.start(f.catalog,f.store.get("batch"),f.sender));
        assertEquals(CarpetRuleBatchExecutor.Failure.NO_CHANGES,f.batch.failure());
        assertEquals(2,f.batch.skippedPermission());
        assertEquals(0,f.sent);
    }

    @Test void persistentDefaultsRequireModeAwareSenderAndAckNotFileProof(){
        Fixture f=new Fixture();
        assertTrue(f.store.setApplyMode("batch",CarpetRuleCommands.Mode.SET_DEFAULT));
        assertFalse(f.batch.start(f.catalog,f.store.get("batch"),f.sender));
        assertEquals(2,f.batch.skippedPermission());
        CarpetRuleBatchExecutor.Sender persistent=new CarpetRuleBatchExecutor.Sender(){
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key){return true;}
            @Override public CarpetRuleOperationTracker.Operation send(
                    CarpetRuleCatalog.RuleKey key,String target){return null;}
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key,CarpetRuleCommands.Mode mode){
                return mode==CarpetRuleCommands.Mode.SET_DEFAULT;
            }
            @Override public CarpetRuleOperationTracker.Operation send(
                    CarpetRuleCatalog.RuleKey key,String target,CarpetRuleCommands.Mode mode){
                f.sent++;
                return f.tracker.begin(f.catalog,key,mode,target,1000);
            }
        };
        assertTrue(f.batch.start(f.catalog,f.store.get("batch"),persistent));
        f.batch.advance(f.catalog,f.tracker,persistent,1000);
        CarpetRuleOperationTracker.Operation first=f.tracker.history().get(0);
        f.tracker.acknowledge(first.id(),1,false,f.catalog.generation());
        f.batch.advance(f.catalog,f.tracker,persistent,1300);
        CarpetRuleOperationTracker.Operation second=f.tracker.history().get(1);
        f.tracker.acknowledge(second.id(),1,false,f.catalog.generation());
        f.batch.advance(f.catalog,f.tracker,persistent,1600);
        assertEquals(CarpetRuleBatchExecutor.Phase.COMPLETED,f.batch.phase());
        assertEquals(2,f.batch.completed());
        assertFalse(f.batch.persistenceVerified());
        assertEquals(CarpetRuleOperationTracker.Status.COMMAND_ACKNOWLEDGED,first.status());
    }

    @Test void integratedPersistentBatchWaitsForEachDiskVerification(){
        Fixture f=new Fixture();
        assertTrue(f.store.setApplyMode("batch",CarpetRuleCommands.Mode.SET_DEFAULT));
        CarpetRuleBatchExecutor.Sender persistent=new CarpetRuleBatchExecutor.Sender(){
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key){return true;}
            @Override public CarpetRuleOperationTracker.Operation send(
                    CarpetRuleCatalog.RuleKey key,String target){return null;}
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key,CarpetRuleCommands.Mode mode){
                return mode==CarpetRuleCommands.Mode.SET_DEFAULT;
            }
            @Override public CarpetRuleOperationTracker.Operation send(
                    CarpetRuleCatalog.RuleKey key,String target,CarpetRuleCommands.Mode mode){
                f.sent++;
                f.sentKey=key;
                return f.tracker.begin(f.catalog,key,mode,target,1000);
            }
        };
        assertTrue(f.batch.start(f.catalog,f.store.get("batch"),persistent));
        f.batch.advance(f.catalog,f.tracker,persistent,1000);
        CarpetRuleOperationTracker.Operation first=f.tracker.history().get(0);
        f.tracker.acknowledge(first.id(),1,false,f.catalog.generation(),true);
        f.batch.advance(f.catalog,f.tracker,persistent,1200);
        assertEquals(1,f.sent);
        assertEquals(CarpetRuleBatchExecutor.Phase.RUNNING,f.batch.phase());

        f.tracker.observePersistence(Collections.singletonMap(FIRST,"true"),
                Collections.singleton("carpet"),f.catalog.generation());
        f.batch.advance(f.catalog,f.tracker,persistent,1400);
        assertEquals(2,f.sent);
        assertEquals(LAST,f.sentKey);

        CarpetRuleOperationTracker.Operation second=f.tracker.find(
                f.tracker.history().get(1).id());
        f.tracker.acknowledge(second.id(),1,false,f.catalog.generation(),true);
        Map<CarpetRuleCatalog.RuleKey,String> saved=new LinkedHashMap<>();
        saved.put(FIRST,"true");
        saved.put(LAST,"true");
        f.tracker.observePersistence(saved,Collections.singleton("carpet"),f.catalog.generation());
        f.batch.advance(f.catalog,f.tracker,persistent,1600);
        assertEquals(CarpetRuleBatchExecutor.Phase.COMPLETED,f.batch.phase());
        assertEquals(2,f.batch.completed());
        assertTrue(f.batch.persistenceVerified());
    }

    @Test void integratedPersistentBatchStopsOnDiskMismatch(){
        Fixture f=new Fixture();
        assertTrue(f.store.setApplyMode("batch",CarpetRuleCommands.Mode.SET_DEFAULT));
        CarpetRuleBatchExecutor.Sender persistent=new CarpetRuleBatchExecutor.Sender(){
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key){return true;}
            @Override public CarpetRuleOperationTracker.Operation send(
                    CarpetRuleCatalog.RuleKey key,String target){return null;}
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key,CarpetRuleCommands.Mode mode){
                return mode==CarpetRuleCommands.Mode.SET_DEFAULT;
            }
            @Override public CarpetRuleOperationTracker.Operation send(
                    CarpetRuleCatalog.RuleKey key,String target,CarpetRuleCommands.Mode mode){
                f.sent++;
                return f.tracker.begin(f.catalog,key,mode,target,1000);
            }
        };
        assertTrue(f.batch.start(f.catalog,f.store.get("batch"),persistent));
        f.batch.advance(f.catalog,f.tracker,persistent,1000);
        CarpetRuleOperationTracker.Operation op=f.tracker.history().get(0);
        f.tracker.acknowledge(op.id(),1,false,f.catalog.generation(),true);
        f.tracker.observePersistence(Collections.singletonMap(FIRST,"false"),
                Collections.singleton("carpet"),f.catalog.generation());
        f.batch.advance(f.catalog,f.tracker,persistent,1300);
        assertEquals(CarpetRuleBatchExecutor.Phase.STOPPED,f.batch.phase());
        assertEquals(CarpetRuleBatchExecutor.Failure.PERSISTENCE_MISMATCH,f.batch.failure());
        assertEquals(1,f.sent);
    }

    @Test void cancelledBatchDoesNotSendMoreCommands() {
        Fixture f=new Fixture();
        f.batch.start(f.catalog,f.store.get("batch"),f.sender);
        f.batch.advance(f.catalog,f.tracker,f.sender,1000);
        f.batch.cancel();
        f.confirmed();
        f.batch.advance(f.catalog,f.tracker,f.sender,1600);
        assertEquals(CarpetRuleBatchExecutor.Phase.CANCELLED,f.batch.phase());
        assertEquals(1,f.sent);
    }
    @Test void explicitApplyModeOverridesPresetModeWhenBuildingTheQueue(){
        Fixture f=new Fixture();
        // The first rule already has the requested live value. It still needs
        // a SET_DEFAULT command when the caller explicitly chooses persistence.
        f.catalog.update(f.session,new CarpetRuleCatalog.RemoteRule(FIRST,"true",true));
        CarpetRuleBatchExecutor.Sender persistent=new CarpetRuleBatchExecutor.Sender(){
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key){return false;}
            @Override public CarpetRuleOperationTracker.Operation send(
                    CarpetRuleCatalog.RuleKey key,String target){return null;}
            @Override public boolean canSend(CarpetRuleCatalog.RuleKey key,CarpetRuleCommands.Mode mode){
                return mode==CarpetRuleCommands.Mode.SET_DEFAULT;
            }
            @Override public CarpetRuleOperationTracker.Operation send(
                    CarpetRuleCatalog.RuleKey key,String target,CarpetRuleCommands.Mode mode){
                return f.tracker.begin(f.catalog,key,mode,target,1000);
            }
        };

        assertTrue(f.batch.start(f.catalog,f.store.get("batch"),persistent,
                CarpetRuleCommands.Mode.SET_DEFAULT));
        assertEquals(2,f.batch.total());
        assertEquals(0,f.batch.matched());
    }

}
