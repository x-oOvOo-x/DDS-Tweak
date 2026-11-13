package ddstweak.feature.carpetrules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Diff staging is local-only; it never modifies the server catalog.
 * Every draft is bound to the catalog generation which created it.
 */
public final class CarpetRuleChanges {
    private final Map<CarpetRuleCatalog.RuleKey, Draft> drafts = new LinkedHashMap<>();

    public boolean stage(CarpetRuleCatalog catalog, CarpetRuleCatalog.RuleKey key, String desired) {
        Objects.requireNonNull(desired, "desired");
        CarpetRuleCatalog.RemoteRule current = catalog.get(key);
        if (!catalog.isReady() || current == null) return false;
        if (desired.equals(current.value())) {
            drafts.remove(key);
            return true;
        }
        drafts.put(key, new Draft(key, current.value(), desired, catalog.generation()));
        return true;
    }

    public void clear() { drafts.clear(); }

    public String desired(CarpetRuleCatalog.RuleKey key) {
        Draft draft=drafts.get(key);
        return draft==null?null:draft.desired;
    }

    public boolean remove(CarpetRuleCatalog.RuleKey key) { return drafts.remove(key) != null; }

    public List<Diff> preview(CarpetRuleCatalog catalog) {
        List<Diff> result = new ArrayList<>();
        for (Draft draft : drafts.values()) {
            CarpetRuleCatalog.RemoteRule current = catalog.get(draft.key);
            Status status;
            if (!catalog.isReady() || draft.generation != catalog.generation()) status = Status.STALE_CONNECTION;
            else if (current == null) status = Status.MISSING;
            else if (current.value().equals(draft.desired)) status = Status.NO_CHANGE;
            else if (!current.value().equals(draft.original)) status = Status.CONFLICT;
            else status = Status.READY;
            result.add(new Diff(draft.key, current == null ? null : current.value(), draft.desired, status));
        }
        return Collections.unmodifiableList(result);
    }

    public enum Status { READY, NO_CHANGE, CONFLICT, MISSING, STALE_CONNECTION }

    public static final class Diff {
        private final CarpetRuleCatalog.RuleKey key;
        private final String current, desired;
        private final Status status;
        private Diff(CarpetRuleCatalog.RuleKey key, String current, String desired, Status status) {
            this.key = key; this.current = current; this.desired = desired; this.status = status;
        }
        public CarpetRuleCatalog.RuleKey key() { return key; }
        public String current() { return current; }
        public String desired() { return desired; }
        public Status status() { return status; }
    }

    private static final class Draft {
        final CarpetRuleCatalog.RuleKey key;
        final String original, desired;
        final long generation;
        Draft(CarpetRuleCatalog.RuleKey key, String original, String desired, long generation) {
            this.key=key;this.original=original;this.desired=desired;this.generation=generation;
        }
    }
}
