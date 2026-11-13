package ddstweak.feature.carpetrules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Only one rule write may be in flight. An explicit confirmation
 * starts a batch; failures stop subsequent writes without automatic rollback.
 */
public final class CarpetRuleBatchExecutor {
    public static final int MAX_CHANGES = 128;

    public enum Phase { IDLE, RUNNING, COMPLETED, STOPPED, CANCELLED }
    public enum Failure {
        NONE, NOT_CONNECTED, NO_CHANGES, TOO_MANY, UNSUPPORTED_RULE,
        INVALID_TARGET, NO_PERMISSION, CHANGED_SINCE_REVIEW, SEND_FAILED,
        SERVER_REJECTED, PERSISTENCE_MISMATCH, UNCONFIRMED, CONNECTION_CHANGED
    }

    public interface Sender {
        boolean canSend(CarpetRuleCatalog.RuleKey key);
        CarpetRuleOperationTracker.Operation send(CarpetRuleCatalog.RuleKey key, String target);
        default boolean canSend(CarpetRuleCatalog.RuleKey key,CarpetRuleCommands.Mode mode){
            return mode==CarpetRuleCommands.Mode.TEMPORARY&&canSend(key);
        }
        default CarpetRuleOperationTracker.Operation send(CarpetRuleCatalog.RuleKey key,
                                                           String target,CarpetRuleCommands.Mode mode){
            return mode==CarpetRuleCommands.Mode.TEMPORARY?send(key,target):null;
        }
    }

    private final List<CarpetRuleBatchReview.Entry> queue = new ArrayList<>();
    private Phase phase = Phase.IDLE;
    private Failure failure = Failure.NONE;
    private long generation;
    private int completed;
    private CarpetRuleOperationTracker.Operation active;
    private CarpetRuleCommands.Mode mode=CarpetRuleCommands.Mode.TEMPORARY;
    private int skippedMissing,skippedInvalid,skippedPermission,matched;
    private boolean persistenceVerified;

    public boolean start(CarpetRuleCatalog catalog, CarpetRulePresetStore.Preset preset, Sender sender) {
        return start(catalog,preset,sender,
                preset==null?CarpetRuleCommands.Mode.TEMPORARY:preset.applyMode());
    }

    public boolean start(CarpetRuleCatalog catalog, CarpetRulePresetStore.Preset preset,
                         Sender sender,CarpetRuleCommands.Mode applyMode) {
        if (phase == Phase.RUNNING) return false;
        queue.clear();
        active = null;
        completed = 0;
        failure = Failure.NONE;
        persistenceVerified=false;
        skippedMissing=skippedInvalid=skippedPermission=matched=0;
        phase = Phase.STOPPED;
        if (applyMode!=CarpetRuleCommands.Mode.TEMPORARY
                &&applyMode!=CarpetRuleCommands.Mode.SET_DEFAULT)return stop(Failure.INVALID_TARGET);
        mode=applyMode;
        if (!catalog.isReady()) return stop(Failure.NOT_CONNECTED);
        if (preset == null) return stop(Failure.NO_CHANGES);
        for (CarpetRuleBatchReview.Entry entry : CarpetRuleBatchReview.preview(catalog, preset, mode)) {
            switch (entry.status()) {
                case SAME:
                    matched++;
                    break;
                case NEEDS_PERMISSION_CHECK:
                    if (!sender.canSend(entry.key(),mode)){skippedPermission++;break;}
                    queue.add(entry);
                    break;
                case MISSING:
                    skippedMissing++;
                    break;
                case INVALID_COMMAND:
                    skippedInvalid++;
                    break;
                default:
                    return stop(Failure.NOT_CONNECTED);
            }
        }
        if (queue.isEmpty()) return stop(Failure.NO_CHANGES);
        if (queue.size() > MAX_CHANGES) return stop(Failure.TOO_MANY);
        // Permission-related rules may disable later commands: submit them last.
        queue.sort(Comparator.comparingInt(e -> e.key().name().startsWith("command") ? 1 : 0));
        generation = catalog.generation();
        phase = Phase.RUNNING;
        return true;
    }

    public void advance(CarpetRuleCatalog catalog, CarpetRuleOperationTracker tracker,
                        Sender sender, long nowMillis) {
        if (phase != Phase.RUNNING) return;
        if (!catalog.isReady() || catalog.generation() != generation) {
            stop(Failure.CONNECTION_CHANGED);
            return;
        }
        tracker.expire(nowMillis);
        if (active != null) {
            switch (active.status()) {
                case VERIFIED:
                    completed++;
                    active = null;
                    break;
                case COMMAND_ACKNOWLEDGED:
                    if(mode!=CarpetRuleCommands.Mode.SET_DEFAULT)return;
                    // Remote servers may acknowledge without exposing their config files.
                    completed++;
                    active=null;
                    break;
                case PERSISTED:
                    if(mode!=CarpetRuleCommands.Mode.SET_DEFAULT){
                        stop(Failure.UNCONFIRMED);
                        return;
                    }
                    completed++;
                    persistenceVerified=true;
                    active=null;
                    break;
                case WAITING_FOR_PERSISTENCE:
                    return;
                case PERSISTENCE_MISMATCH:
                    stop(Failure.PERSISTENCE_MISMATCH);
                    return;
                case PERSISTENCE_UNCONFIRMED:
                    stop(Failure.UNCONFIRMED);
                    return;
                case REJECTED:
                    stop(Failure.SERVER_REJECTED);
                    return;
                case UNCONFIRMED:
                    stop(Failure.UNCONFIRMED);
                    return;
                default:
                    return;
            }
        }
        if (completed == queue.size()) {
            phase = Phase.COMPLETED;
            return;
        }
        CarpetRuleBatchReview.Entry next = queue.get(completed);
        CarpetRuleCatalog.RemoteRule current = catalog.get(next.key());
        if (current == null || !current.value().equals(next.current())) {
            stop(Failure.CHANGED_SINCE_REVIEW);
            return;
        }
        if (!sender.canSend(next.key(),mode)) {
            stop(Failure.NO_PERMISSION);
            return;
        }
        active = sender.send(next.key(), next.target(),mode);
        if (active == null) stop(Failure.SEND_FAILED);
    }

    public void cancel() {
        if (phase == Phase.RUNNING) {
            phase = Phase.CANCELLED;
            active = null;
        }
    }

    public Phase phase() { return phase; }
    public Failure failure() { return failure; }
    public int completed() { return completed; }
    public int total() { return queue.size(); }
    public int skippedMissing(){return skippedMissing;}
    public int skippedInvalid(){return skippedInvalid;}
    public int skippedPermission(){return skippedPermission;}
    public int matched(){return matched;}
    public CarpetRuleCommands.Mode mode(){return mode;}
    public boolean persistenceVerified(){return persistenceVerified;}

    private boolean stop(Failure reason) {
        failure = reason;
        phase = Phase.STOPPED;
        active = null;
        return false;
    }
}
