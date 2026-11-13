package ddstweak.feature.carpetrules;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

final class CarpetRuleBatchReviewTest {
    @Test void previewsMissingSameAndChangedWithoutWritingToServer() {
        CarpetRuleCatalog.RuleKey same = new CarpetRuleCatalog.RuleKey("carpet", "same");
        CarpetRuleCatalog.RuleKey changed = new CarpetRuleCatalog.RuleKey("tis", "changed");
        CarpetRuleCatalog.RuleKey missing = new CarpetRuleCatalog.RuleKey("extra", "missing");
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        Object connection = new Object();
        catalog.connect(connection);
        catalog.replace(connection, java.util.Arrays.asList(
                new CarpetRuleCatalog.RemoteRule(same, "true", true),
                new CarpetRuleCatalog.RemoteRule(changed, "false", true)));
        Map<CarpetRuleCatalog.RuleKey, String> values = new LinkedHashMap<>();
        values.put(same, "true");
        values.put(changed, "true");
        values.put(missing, "false");
        CarpetRulePresetStore store = new CarpetRulePresetStore();
        assertTrue(store.put("plan", "server", 1L, false, values));
        List<CarpetRuleBatchReview.Entry> results = CarpetRuleBatchReview.preview(catalog, store.get("plan"));
        assertEquals(CarpetRuleBatchReview.Status.SAME, results.get(0).status());
        assertEquals(CarpetRuleBatchReview.Status.NEEDS_PERMISSION_CHECK, results.get(1).status());
        assertEquals(CarpetRuleBatchReview.Status.MISSING, results.get(2).status());
        assertEquals("false", catalog.get(changed).value());
    }

    @Test void incompleteConnectionNeverClaimsReady() {
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        CarpetRulePresetStore store = new CarpetRulePresetStore();
        Map<CarpetRuleCatalog.RuleKey, String> values = Collections.singletonMap(
                new CarpetRuleCatalog.RuleKey("carpet", "example"), "true");
        assertTrue(store.put("preset", "", 1L, false, values));
        assertEquals(CarpetRuleBatchReview.Status.UNAVAILABLE,
                CarpetRuleBatchReview.preview(catalog, store.get("preset")).get(0).status());
    }

    @Test void invalidTargetIsNeverMarkedAsSendable() {
        CarpetRuleCatalog.RuleKey key = new CarpetRuleCatalog.RuleKey("carpet", "x");
        CarpetRuleCatalog catalog = new CarpetRuleCatalog();
        Object connection = new Object();
        catalog.connect(connection);
        catalog.replace(connection, Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(key, "false", true)));
        Map<CarpetRuleCatalog.RuleKey, String> targets = Collections.singletonMap(key, "true\n/kill @a");
        CarpetRulePresetStore store = new CarpetRulePresetStore();
        assertTrue(store.put("invalid", "", 1L, false, targets));
        assertEquals(CarpetRuleBatchReview.Status.INVALID_COMMAND,
                CarpetRuleBatchReview.preview(catalog, store.get("invalid")).get(0).status());
    }
    @Test void persistentDefaultWithMatchingRuntimeValueStillRequiresWrite() {
        CarpetRuleCatalog.RuleKey key=new CarpetRuleCatalog.RuleKey("carpet","same");
        CarpetRuleCatalog catalog=new CarpetRuleCatalog();
        Object connection=new Object();
        catalog.connect(connection);
        assertTrue(catalog.replace(connection,Collections.singletonList(
                new CarpetRuleCatalog.RemoteRule(key,"true",true))));
        CarpetRulePresetStore store=new CarpetRulePresetStore();
        assertTrue(store.put("plan","",1L,false,Collections.singletonMap(key,"true")));
        assertEquals(CarpetRuleBatchReview.Status.SAME,
                CarpetRuleBatchReview.preview(catalog,store.get("plan")).get(0).status());
        assertTrue(store.setApplyMode("plan",CarpetRuleCommands.Mode.SET_DEFAULT));
        assertEquals(CarpetRuleBatchReview.Status.NEEDS_PERMISSION_CHECK,
                CarpetRuleBatchReview.preview(catalog,store.get("plan")).get(0).status());
        assertEquals("true",catalog.get(key).value());
    }

}
