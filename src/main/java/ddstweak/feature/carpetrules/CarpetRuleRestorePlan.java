package ddstweak.feature.carpetrules;

/**
 * A manual reverse-value proposal, never an automatic rollback. Only a
 * verified temporary change from this live connection may be reversed.
 */
public final class CarpetRuleRestorePlan {
    public enum Status {
        READY, NO_OPERATION, STALE_CONNECTION, UNVERIFIED,
        NOT_TEMPORARY, RULE_MISSING, RULE_CHANGED, NO_CHANGE
    }

    private CarpetRuleRestorePlan() {}

    public static Status validate(CarpetRuleCatalog catalog,
                                  CarpetRuleOperationTracker tracker,
                                  CarpetRuleOperationTracker.Operation operation) {
        if (operation == null || tracker.find(operation.id()) != operation)
            return Status.NO_OPERATION;
        if (!catalog.isReady() || catalog.generation() != operation.generation())
            return Status.STALE_CONNECTION;
        if (operation.mode() != CarpetRuleCommands.Mode.TEMPORARY)
            return Status.NOT_TEMPORARY;
        if (operation.status() != CarpetRuleOperationTracker.Status.VERIFIED)
            return Status.UNVERIFIED;
        CarpetRuleCatalog.RemoteRule rule = catalog.get(operation.key());
        if (rule == null || !rule.managerExplicit()) return Status.RULE_MISSING;
        if (!rule.value().equals(operation.target())) return Status.RULE_CHANGED;
        if (operation.original().equals(rule.value())) return Status.NO_CHANGE;
        try {
            CarpetRuleCommands.build(operation.key(), CarpetRuleCommands.Mode.TEMPORARY,
                    operation.original());
        } catch (IllegalArgumentException e) {
            return Status.UNVERIFIED;
        }
        return Status.READY;
    }
}
