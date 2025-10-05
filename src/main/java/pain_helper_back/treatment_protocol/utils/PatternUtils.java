package pain_helper_back.treatment_protocol.utils;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternUtils {
    private static final Pattern FIRST_INT_PATTERN = Pattern.compile("(\\d+)");

    public static Integer extractFirstInt(String s) {
        if (s == null) return null;
        Matcher m = FIRST_INT_PATTERN.matcher(s);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}

