package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleRestorePlanTest {
    private static final CarpetRuleCatalog.RuleKey KEY =
            new CarpetRuleCatalog.RuleKey("carpet", "commandPlayer");

    private static final class Fixture {
        final CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        final CarpetRuleOperationTracker tracker = new CarpetRuleOperationTracker();
        final Object connection = new Object();

        Fixture() {
            catalog.connect(connection);
            assertTrue(catalog.replace(connection, Collections.singletonList(
                    new CarpetRuleCatalog.RemoteRule(KEY, "ops", true))));
        }

        CarpetRuleOperationTracker.Operation verified() {
            CarpetRuleOperationTracker.Operation op = tracker.begin(catalog, KEY,
                    CarpetRuleCommands.Mode.TEMPORARY, "true", 1000);
            assertNotNull(op);
            tracker.acknowledge(op.id(), 1, false, catalog.generation());
            catalog.update(connection, new CarpetRuleCatalog.RemoteRule(KEY, "true", true));
            tracker.observe(KEY, "true", catalog.generation());
            assertEquals(CarpetRuleOperationTracker.Status.VERIFIED, op.status());
            return op;
        }

        CarpetRuleRestorePlan.Status check(CarpetRuleOperationTracker.Operation op) {
            return CarpetRuleRestorePlan.validate(catalog, tracker, op);
        }
    }

    @Test void verifiedTemporaryChangeMayBeReversedOnlyWhenCurrentValueMatches() {
        Fixture f = new Fixture();
        CarpetRuleOperationTracker.Operation op = f.verified();
        assertEquals("ops", op.original());
        assertEquals(CarpetRuleRestorePlan.Status.READY, f.check(op));
        f.catalog.update(f.connection, new CarpetRuleCatalog.RemoteRule(KEY, "other", true));
        assertEquals(CarpetRuleRestorePlan.Status.RULE_CHANGED, f.check(op));
    }

    @Test void pendingAndPermanentCommandsAreNotRestorable() {
        Fixture f = new Fixture();
        CarpetRuleOperationTracker.Operation pending = f.tracker.begin(f.catalog, KEY,
                CarpetRuleCommands.Mode.TEMPORARY, "true", 1000);
        assertEquals(CarpetRuleRestorePlan.Status.UNVERIFIED, f.check(pending));
        f.tracker.acknowledge(pending.id(), 0, true, f.catalog.generation());
        CarpetRuleOperationTracker.Operation persistent = f.tracker.begin(f.catalog, KEY,
                CarpetRuleCommands.Mode.SET_DEFAULT, "true", 1100);
        f.tracker.acknowledge(persistent.id(), 1, false, f.catalog.generation());
        assertEquals(CarpetRuleRestorePlan.Status.NOT_TEMPORARY, f.check(persistent));
    }

    @Test void switchingServersClearsEligibilityAndIdentity() {
        Fixture f = new Fixture();
        CarpetRuleOperationTracker.Operation op = f.verified();
        f.catalog.connect(new Object());
        assertEquals(CarpetRuleRestorePlan.Status.STALE_CONNECTION, f.check(op));
        f.tracker.reset();
        assertEquals(CarpetRuleRestorePlan.Status.NO_OPERATION, f.check(op));
    }

    @Test void historyIsBoundedButUnconfirmedOperationIsRetained() {
        Fixture f = new Fixture();
        for(int i=0;i<150;i++) {
            CarpetRuleOperationTracker.Operation op = f.tracker.begin(f.catalog, KEY,
                    CarpetRuleCommands.Mode.SET_DEFAULT, "true", i * 10L);
            assertNotNull(op);
            f.tracker.acknowledge(op.id(), 1, false, f.catalog.generation());
        }
        assertEquals(128, f.tracker.history().size());
        assertEquals("ops", f.tracker.history().get(0).original());
        CarpetRuleOperationTracker.Operation last = f.tracker.begin(f.catalog, KEY,
                CarpetRuleCommands.Mode.TEMPORARY, "true", 2000);
        assertNotNull(last);
        assertTrue(f.tracker.history().contains(last));
        assertEquals(128, f.tracker.history().size());
    }
}
