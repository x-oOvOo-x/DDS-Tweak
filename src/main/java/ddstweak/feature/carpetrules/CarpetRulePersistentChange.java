package ddstweak.feature.carpetrules;

/**
 * An explicitly reviewed persistent-rule command. Kept separate from the
 * temporary-value diff because setting an identical default is still a write
 * and removing a default does not need a target value.
 */
public final class CarpetRulePersistentChange {
    public enum Status { READY, NOT_STAGED, STALE_CONNECTION, MISSING, CHANGED }

    private CarpetRuleCatalog.RuleKey key;
    private CarpetRuleCommands.Mode mode;
    private String target;
    private String original;
    private long generation;

    public boolean stage(CarpetRuleCatalog catalog, CarpetRuleCatalog.RuleKey rule,
                         CarpetRuleCommands.Mode action, String value) {
        if (!catalog.isReady() || action == CarpetRuleCommands.Mode.TEMPORARY) return false;
        CarpetRuleCatalog.RemoteRule current = catalog.get(rule);
        if (current == null || !current.managerExplicit()) return false;
        try {
            CarpetRuleCommands.build(rule, action, value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        key = rule;
        mode = action;
        target = action == CarpetRuleCommands.Mode.REMOVE_DEFAULT ? null : value;
        original = current.value();
        generation = catalog.generation();
        return true;
    }

    public Status validate(CarpetRuleCatalog catalog) {
        if (key == null) return Status.NOT_STAGED;
        if (!catalog.isReady() || catalog.generation() != generation) return Status.STALE_CONNECTION;
        CarpetRuleCatalog.RemoteRule current = catalog.get(key);
        if (current == null || !current.managerExplicit()) return Status.MISSING;
        return original.equals(current.value()) ? Status.READY : Status.CHANGED;
    }

    public CarpetRuleCatalog.RuleKey key() { return key; }
    public CarpetRuleCommands.Mode mode() { return mode; }
    public String target() { return target; }
    public String original() { return original; }

    public void clear() {
        key = null;
        target = null;
        original = null;
        mode = null;
    }
}
