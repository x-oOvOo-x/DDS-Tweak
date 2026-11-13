package ddstweak.feature.carpetrules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Connection-owned server facts. Neither locally installed extensions nor a
 * metadata database can add entries to this catalog.
 *
 * The connection token is compared by identity, not by hostname or server name:
 * reconnecting to the same address must invalidate old packets and drafts.
 */
public final class CarpetRuleCatalog {
    private Object connection;
    private long generation;
    private boolean synchronizedSnapshot;
    private final Map<RuleKey, RemoteRule> rules = new LinkedHashMap<>();

    public synchronized long connect(Object token) {
        Objects.requireNonNull(token, "connection");
        if (connection != token) {
            connection = token;
            generation++;
            synchronizedSnapshot = false;
            rules.clear();
        }
        return generation;
    }

    public synchronized void disconnect() {
        connection = null;
        generation++;
        synchronizedSnapshot = false;
        rules.clear();
    }

    /** Atomically replace the entire server catalog, rejecting duplicate rule identities. */
    public synchronized boolean replace(Object token, Collection<RemoteRule> snapshot) {
        if (connection == null || connection != token || snapshot == null) return false;
        Map<RuleKey, RemoteRule> incoming = new LinkedHashMap<>();
        for (RemoteRule rule : snapshot) {
            if (rule == null || incoming.putIfAbsent(rule.key(), rule) != null) return false;
        }
        rules.clear();
        rules.putAll(incoming);
        synchronizedSnapshot = true;
        return true;
    }

    /** An incremental Carpet rule update is accepted only after a full snapshot. */
    public synchronized boolean update(Object token, RemoteRule rule) {
        if (connection == null || connection != token || !synchronizedSnapshot || rule == null) return false;
        rules.put(rule.key(), rule);
        return true;
    }

    public synchronized boolean isReady() {
        return connection != null && synchronizedSnapshot;
    }

    public synchronized long generation() {
        return generation;
    }

    public synchronized RemoteRule get(RuleKey key) {
        return rules.get(key);
    }

    public synchronized List<RemoteRule> snapshot() {
        if (!isReady()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(rules.values()));
    }

    public static final class RuleKey {
        private final String manager;
        private final String name;

        public RuleKey(String manager, String name) {
            this.manager = required(manager, "manager");
            this.name = required(name, "name");
        }

        public String manager() { return manager; }
        public String name() { return name; }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RuleKey)) return false;
            RuleKey key = (RuleKey) other;
            return manager.equals(key.manager) && name.equals(key.name);
        }

        @Override public int hashCode() { return Objects.hash(manager, name); }

        @Override public String toString() { return manager + ":" + name; }
    }

    public static final class RemoteRule {
        private final RuleKey key;
        private final String value;
        private final boolean managerExplicit;

        /** managerExplicit=false identifies legacy wire data, not a verified Carpet owner. */
        public RemoteRule(RuleKey key, String value, boolean managerExplicit) {
            this.key = Objects.requireNonNull(key, "key");
            this.value = Objects.requireNonNull(value, "value");
            this.managerExplicit = managerExplicit;
        }

        public RuleKey key() { return key; }
        public String value() { return value; }
        public boolean managerExplicit() { return managerExplicit; }
    }

    private static String required(String value, String label) {
        if (value == null || value.isEmpty() || !value.equals(value.trim()))
            throw new IllegalArgumentException("Invalid " + label);
        return value;
    }
}
