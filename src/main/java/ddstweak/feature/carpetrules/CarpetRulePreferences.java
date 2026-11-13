package ddstweak.feature.carpetrules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ddstweak.config.DdsTweakConfigPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Local UI organization. These entries never establish that a server rule exists. */
public final class CarpetRulePreferences {
    private static final int MAX_ITEMS = 8192;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Set<CarpetRuleCatalog.RuleKey> favorites = new LinkedHashSet<>();
    private final Map<String, Set<CarpetRuleCatalog.RuleKey>> groups = new LinkedHashMap<>();

    public boolean isFavorite(CarpetRuleCatalog.RuleKey key) { return favorites.contains(key); }

    public boolean toggleFavorite(CarpetRuleCatalog.RuleKey key) {
        if (favorites.remove(key)) return false;
        if (favorites.size() < MAX_ITEMS) favorites.add(key);
        return favorites.contains(key);
    }

    public Set<CarpetRuleCatalog.RuleKey> favorites() {
        return Collections.unmodifiableSet(favorites);
    }

    public Set<String> groupNames() {
        return Collections.unmodifiableSet(groups.keySet());
    }

    public boolean createGroup(String name) {
        if (!validName(name) || groups.containsKey(name) || groups.size() >= 128) return false;
        groups.put(name, new LinkedHashSet<>());
        return true;
    }

    public boolean renameGroup(String oldName,String newName) {
        if(!groups.containsKey(oldName)||!validName(newName)||groups.containsKey(newName))return false;
        Map<String,Set<CarpetRuleCatalog.RuleKey>> updated=new LinkedHashMap<>();
        for(Map.Entry<String,Set<CarpetRuleCatalog.RuleKey>> e:groups.entrySet())
            updated.put(e.getKey().equals(oldName)?newName:e.getKey(),e.getValue());
        groups.clear();
        groups.putAll(updated);
        return true;
    }

    public boolean removeGroup(String name) { return groups.remove(name) != null; }

    public boolean setGroupMembership(String group, CarpetRuleCatalog.RuleKey key, boolean include) {
        Set<CarpetRuleCatalog.RuleKey> keys = groups.get(group);
        if (keys == null) return false;
        if (!include) return keys.remove(key);
        return keys.size() < MAX_ITEMS && keys.add(key);
    }

    public boolean inGroup(String group, CarpetRuleCatalog.RuleKey key) {
        Set<CarpetRuleCatalog.RuleKey> keys = groups.get(group);
        return keys != null && keys.contains(key);
    }

    public String serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("favorites", keysToJson(favorites));
        JsonObject collection = new JsonObject();
        for (Map.Entry<String, Set<CarpetRuleCatalog.RuleKey>> item : groups.entrySet())
            collection.add(item.getKey(), keysToJson(item.getValue()));
        root.add("groups", collection);
        return root.toString();
    }

    public static CarpetRulePreferences parse(String json) {
        CarpetRulePreferences out = new CarpetRulePreferences();
        if (json == null || json.length() > MAX_BYTES) return out;
        try {
            JsonElement value = new JsonParser().parse(json);
            if (!value.isJsonObject()) return out;
            JsonObject root = value.getAsJsonObject();
            if (!root.has("version") || root.get("version").getAsInt() != 1) return out;
            readKeys(root.get("favorites"), out.favorites);
            JsonElement groups = root.get("groups");
            if (groups != null && groups.isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : groups.getAsJsonObject().entrySet()) {
                    if (!validName(e.getKey()) || out.groups.size() >= 128) continue;
                    Set<CarpetRuleCatalog.RuleKey> keys = new LinkedHashSet<>();
                    readKeys(e.getValue(), keys);
                    out.groups.put(e.getKey(), keys);
                }
            }
        } catch (RuntimeException ignored) {
            return new CarpetRulePreferences(); // corrupted file should not prevent opening GUI
        }
        return out;
    }

    private static JsonArray keysToJson(Set<CarpetRuleCatalog.RuleKey> keys) {
        JsonArray a = new JsonArray();
        for (CarpetRuleCatalog.RuleKey key : keys) {
            JsonObject item = new JsonObject();
            item.addProperty("manager", key.manager());
            item.addProperty("rule", key.name());
            a.add(item);
        }
        return a;
    }

    private static void readKeys(JsonElement element, Set<CarpetRuleCatalog.RuleKey> output) {
        if (element == null || !element.isJsonArray()) return;
        for (JsonElement child : element.getAsJsonArray()) {
            if (output.size() >= MAX_ITEMS) break;
            try {
                if (!child.isJsonObject()) continue;
                JsonObject item = child.getAsJsonObject();
                String manager = item.get("manager").getAsString(), name = item.get("rule").getAsString();
                if (validId(manager) && validId(name))
                    output.add(new CarpetRuleCatalog.RuleKey(manager, name));
            } catch (RuntimeException ignored) {
                // skip a malformed entry but preserve other valid entries
            }
        }
    }

    private static boolean validId(String id) {
        if (id == null || id.isEmpty() || id.length() > 128) return false;
        for (int i = 0; i < id.length(); i++)
            if (Character.isWhitespace(id.charAt(i)) || Character.isISOControl(id.charAt(i))) return false;
        return true;
    }

    private static boolean validName(String name) {
        return name != null && name.length() > 0 && name.length() <= 64 && name.trim().equals(name)
                && name.chars().noneMatch(Character::isISOControl);
    }

    private static Path path() {
        return DdsTweakConfigPaths.carpetRulePreferences();
    }

    public static CarpetRulePreferences load() {
        try {
            Path target = path();
            if (!Files.isRegularFile(target) || Files.size(target) > MAX_BYTES) return new CarpetRulePreferences();
            byte[] bytes = Files.readAllBytes(target);
            return parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ignored) {
            return new CarpetRulePreferences();
        }
    }

    public boolean save() {
        try {
            Path target = path(), temporary = target.resolveSibling(target.getFileName() + ".tmp");
            Files.createDirectories(target.getParent());
            byte[] contents = serialize().getBytes(StandardCharsets.UTF_8);
            if (contents.length > MAX_BYTES) return false;
            Files.write(temporary, contents);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }
}
