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

    public static String normalize(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u00A0\\u202F\\u2007]", " ") // убираем неразрывные пробелы
                .replaceAll("[,;]", " ")                   // убираем запятые и точки с запятой
                .replaceAll("\\s+", " ")                   // схлопываем пробелы
                .trim()
                .toUpperCase(Locale.ROOT);
    }

}

/*
* package pain_helper_back.treatment_protocol.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TreatmentSanitizer {

    public static String cleanGfr(String value) {
        value = SanitizeUtils.clean(value);
        if (value == null || value.isBlank()) return "NA";

        // Убираем единицы измерения, пробелы и приводим к виду <число> или класс (A–F)
        value = value.replaceAll("(?i)mL/?min", "")
                     .replaceAll("[^0-9A-F<>=\\-]", "")
                     .trim().toUpperCase();
        // Пример: "< 30 mL/min" → "<30"
        return value;
    }

    public static String cleanPlt(String value) {
        value = SanitizeUtils.clean(value);
        if (value == null || value.isBlank()) return "NA";
        // Убираем всё, кроме цифр и операторов
        value = value.replaceAll("[^0-9<>=\\-]", "");
        return value.trim();
    }

    public static String cleanWbc(String value) {
        value = SanitizeUtils.clean(value);
        if (value == null || value.isBlank()) return "NA";
        // Убираем единицы измерения
        return value.replaceAll("(?i)10\\s*e3/?µ?L", "")
                    .replaceAll("[^0-9<>=\\-\\.]", "")
                    .trim();
    }

    public static String cleanSodium(String value) {
        value = SanitizeUtils.clean(value);
        if (value == null || value.isBlank()) return "NA";
        // Убираем "mmol/L" и все ненужные символы
        return value.replaceAll("(?i)mmol/?l", "")
                    .replaceAll("[^0-9<>=\\-\\.]", "")
                    .trim();
    }

    public static String cleanSensitivity(String value) {
        value = SanitizeUtils.clean(value);
        if (value == null) return "NA";
        // Нормализуем пробелы и знаки OR
        return value.replaceAll("(?i)\\s*(,|;|/|\\||and|или)\\s*", " OR ")
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toUpperCase();
    }

    public static String cleanContraindications(String value) {
        value = SanitizeUtils.clean(value);
        if (value == null) return "NA";
        // Заменяем знаки-разделители на OR, как в Sensitivity
        return value.replaceAll("(?i)\\s*(,|;|/|\\||and|или)\\s*", " OR ")
                    .replaceAll("\\s+", " ")
                    .trim()
                    .toUpperCase();
    }
}*/
