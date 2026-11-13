package ddstweak.feature.carpetrules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A read-only, connection-bound batch review. It does not grant permission,
 * execute commands, or promise atomic application.
 */
public final class CarpetRuleBatchReview {
    private CarpetRuleBatchReview() {}

    public enum Status { UNAVAILABLE, SAME, MISSING, INVALID_COMMAND, NEEDS_PERMISSION_CHECK }

    public static List<Entry> preview(CarpetRuleCatalog catalog, CarpetRulePresetStore.Preset preset) {
        return preview(catalog,preset,preset==null
                ?CarpetRuleCommands.Mode.TEMPORARY:preset.applyMode());
    }
    public static List<Entry> preview(CarpetRuleCatalog catalog, CarpetRulePresetStore.Preset preset,
                                      CarpetRuleCommands.Mode mode) {
        if (preset == null) return Collections.emptyList();
        List<Entry> result = new ArrayList<>();
        for (Map.Entry<CarpetRuleCatalog.RuleKey, String> target : preset.values().entrySet()) {
            CarpetRuleCatalog.RemoteRule remote = catalog.isReady() ? catalog.get(target.getKey()) : null;
            Status status;
            if (!catalog.isReady()) status = Status.UNAVAILABLE;
            else if (remote == null) status = Status.MISSING;
            else if (mode==CarpetRuleCommands.Mode.TEMPORARY
                    &&remote.value().equals(target.getValue())) status = Status.SAME;
            else {
                try {
                    CarpetRuleCommands.build(target.getKey(),mode,target.getValue());
                    status = Status.NEEDS_PERMISSION_CHECK;
                } catch (IllegalArgumentException ex) {
                    status = Status.INVALID_COMMAND;
                }
            }
            result.add(new Entry(target.getKey(), remote == null ? null : remote.value(),
                    target.getValue(), status));
        }
        return Collections.unmodifiableList(result);
    }

    public static final class Entry {
        private final CarpetRuleCatalog.RuleKey key;
        private final String current;
        private final String target;
        private final Status status;

        private Entry(CarpetRuleCatalog.RuleKey key, String current, String target, Status status) {
            this.key = key;
            this.current = current;
            this.target = target;
            this.status = status;
        }

        public CarpetRuleCatalog.RuleKey key() { return key; }
        public String current() { return current; }
        public String target() { return target; }
        public Status status() { return status; }
    }
}
