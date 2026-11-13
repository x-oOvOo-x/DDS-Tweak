package ddstweak.feature.carpetrules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Optional display hints, never used to infer which server rules exist or
 * to authorize a write. Categories may contain multiple original Carpet tags.
 */
public final class CarpetRuleMetadata {
    private final Map<CarpetRuleCatalog.RuleKey, Description> descriptions = new LinkedHashMap<>();

    public void put(CarpetRuleCatalog.RuleKey key, Description description) {
        descriptions.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(description, "description"));
    }

    public Description find(CarpetRuleCatalog.RuleKey key) { return descriptions.get(key); }

    public static final class Description {
        private final String displayName;
        private final String detail;
        private final Set<String> categories;
        private final String source;

        public Description(String displayName, String detail, Collection<String> categories, String source) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.detail = Objects.requireNonNull(detail, "detail");
            this.source = Objects.requireNonNull(source, "source");
            Set<String> values = new LinkedHashSet<>();
            if (categories != null) for (String category : categories)
                if (category != null && !category.trim().isEmpty()) values.add(category);
            this.categories = Collections.unmodifiableSet(values);
        }

        public String displayName() { return displayName; }
        public String detail() { return detail; }
        public Set<String> categories() { return categories; }
        public String source() { return source; }
    }
}
