package ddstweak.feature.carpetrules;

import java.util.Objects;
import java.util.regex.Pattern;

/** Constructs only Carpet's three supported settings-command forms. */
public final class CarpetRuleCommands {
    private static final Pattern COMMAND_WORD = Pattern.compile("[A-Za-z0-9_:.\\-]{1,128}");
    private CarpetRuleCommands() {}

    public enum Mode { TEMPORARY, SET_DEFAULT, REMOVE_DEFAULT }

    public static String build(CarpetRuleCatalog.RuleKey key, Mode mode, String targetValue) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(mode, "mode");
        String root = word(key.manager());
        String rule = word(key.name());
        if (mode == Mode.REMOVE_DEFAULT) return root + " removeDefault " + rule;
        if (targetValue == null || targetValue.length() > 2048 || targetValue.isEmpty())
            throw new IllegalArgumentException("Invalid rule value");
        for (int i=0;i<targetValue.length();i++) {
            char c=targetValue.charAt(i);
            if (Character.isISOControl(c)) throw new IllegalArgumentException("Control character in value");
        }
        return root + (mode == Mode.SET_DEFAULT ? " setDefault " : " ") + rule + " " + targetValue;
    }

    private static String word(String text) {
        if (!COMMAND_WORD.matcher(text).matches()) throw new IllegalArgumentException("Invalid command word");
        return text;
    }
}
