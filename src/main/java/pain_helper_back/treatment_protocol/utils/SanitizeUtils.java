package pain_helper_back.treatment_protocol.utils;

import java.util.Locale;

public class SanitizeUtils {
    //sanitize-метод
   public static String clean(String value) {
        if (value == null) return null;

        return value
                .replace("–", "-")     // en dash (короткое длинное тире)
                .replace("—", "-")      // em dash (длинное тире)
                .replace("\u00A0", " ") // неразрывный пробел
                .trim();                               // убираем пробелы по краям
    }

  public  static  String normalize(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u00A0\\u202F\\u2007]", " ") // неразрывные пробелы
                .replaceAll("[,;]", " ")                   // убираем запятые и точки с запятой
                .replaceAll("\\s+", " ")                   // схлопываем двойные пробелы
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
