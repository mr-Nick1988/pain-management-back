package pain_helper_back.treatment_protocol.utils;

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
}
