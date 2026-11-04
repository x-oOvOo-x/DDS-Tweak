package ddstweak.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

final class LocalizationContractTest {
    private static final Path LANG = Path.of("src/main/resources/assets/dds-tweak/lang");
    private static final Pattern ARGUMENT = Pattern.compile("%(?:\\d+\\$)?[sdif]");

    private static JsonObject load(String locale) throws Exception {
        return JsonParser.parseString(Files.readString(LANG.resolve(locale + ".json"))).getAsJsonObject();
    }

    private static List<String> arguments(String text) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = ARGUMENT.matcher(text);
        while (matcher.find()) matches.add(matcher.group());
        return matches;
    }

    @Test void localesCoverEveryVisibleTextWithMatchingFormatArguments() throws Exception {
        JsonObject en = load("en_us");
        JsonObject cn = load("zh_cn");
        JsonObject tw = load("zh_tw");
        assertEquals(en.keySet(), cn.keySet(), "Simplified Chinese keyset");
        assertEquals(en.keySet(), tw.keySet(), "Traditional Chinese keyset");
        for (String key : en.keySet()) {
            String expected = en.get(key).getAsString();
            for (JsonObject locale : new JsonObject[]{cn, tw}) {
                String translated = locale.get(key).getAsString();
                assertFalse(translated.isBlank(), key);
                assertEquals(arguments(expected), arguments(translated), key);
            }
        }
    }

    @Test void canonicalConfigNamesAndTipsRemainReadableAndConcise() throws Exception {
        JsonObject en = load("en_us");
        JsonObject cn = load("zh_cn");
        JsonObject tw = load("zh_tw");
        int canonical = 0;
        for (String prettyKey : en.keySet()) {
            if (!prettyKey.contains(".config.") || !prettyKey.contains(".prettyName.")) continue;
            String nameKey = prettyKey.replace(".prettyName.", ".name.");
            if (!en.has(nameKey)) continue; // Old lower-case aliases are not registered configs.
            String commentKey = prettyKey.replace(".prettyName.", ".comment.");
            String rawName = nameKey.substring(nameKey.lastIndexOf('.') + 1);
            canonical++;
            for (JsonObject locale : new JsonObject[]{en, cn, tw}) {
                assertTrue(locale.has(nameKey), nameKey);
                assertTrue(locale.has(prettyKey), prettyKey);
                assertTrue(locale.has(commentKey), commentKey);
                assertNotEquals(rawName, locale.get(nameKey).getAsString(), nameKey);
                assertNotEquals(rawName, locale.get(prettyKey).getAsString(), prettyKey);
                String tip = locale.get(commentKey).getAsString();
                int limit = locale == en ? 80 : 40;
                assertTrue(tip.codePointCount(0, tip.length()) <= limit, commentKey + ": " + tip);
                assertFalse(tip.contains("\\n"), commentKey + ": literal escaped newline");
            }
            // Keep legacy MaLiLib case-fallback strings identical until lookup behavior is proven.
            int index = prettyKey.lastIndexOf('.') + 1;
            String aliasPretty = prettyKey.substring(0, index) + rawName.toLowerCase(Locale.ROOT);
            String aliasComment = commentKey.substring(0, commentKey.lastIndexOf('.') + 1)
                    + rawName.toLowerCase(Locale.ROOT);
            for (JsonObject locale : new JsonObject[]{en, cn, tw}) {
                if (locale.has(aliasPretty) && !aliasPretty.equals(prettyKey))
                    assertEquals(locale.get(prettyKey), locale.get(aliasPretty), aliasPretty);
                if (locale.has(aliasComment) && !aliasComment.equals(commentKey))
                    assertEquals(locale.get(commentKey), locale.get(aliasComment), aliasComment);
            }
        }
        assertTrue(canonical >= 80, "Configuration translations must not disappear");
    }
}
