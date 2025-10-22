package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(9)
@Slf4j
public class GfrRuleApplier implements TreatmentRuleApplier {

    //      "Class B - 12h" или "Class C - avoid"
    //      "<30 mL/min - avoid" или "<60 mL/min - reduce by 50%"
    //      Ловит несколько классов в одной строке (class B … class C …).
    //      Теперь также поддерживает и вариант без слова "class", например "B - 12h".
    //      Не зависим от регистра ((?i)).
    private static final Pattern COMPLEX_PATTERN = Pattern.compile(
            "(?i)" +
                    //  1) первая ветка — буквенные классы (A–F)
                    //     ловит и "class B ..." и просто "B ..."
                    "(?:class\\s*)?([A-F])(?:\\s*action)?\\s*[:\\-]\\s*([^<]+?)(?=$|\\bclass\\b|<\\d+\\s*mL/min)" +
                    "|" +
                    //  2) вторая ветка — числовые пороги, например "<30 mL/min - avoid"
                    "(<\\d+\\s*mL/min)\\s*[-:]\\s*([^<]+)"
    );

    //    Диапазоны для категорий GFR (в мл/мин)
    private static final Map<String, double[]> GFR_CLASSES = Map.of(
            "A", new double[]{90, Double.MAX_VALUE},
            "B", new double[]{60, 89},
            "C", new double[]{45, 59},
            "D", new double[]{30, 44},
            "E", new double[]{15, 29},
            "F", new double[]{0, 14}
    );

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        // Если препарат не имеет данных (например, очищен предыдущим фильтром) — пропускаем
        if (!DrugUtils.hasInfo(drug)) return;

        String patientGfr = patient.getEmr().getLast().getGfr();  // берём данные gfr пациента (букву или цифру 0-120)
        String normalizedGfr = normalizeGfrValue(patientGfr);     // нормализуем эти данные, вернёт конкретную группу A|B|C|D|E|F
        String gfrRule = tp.getGfr();                             // берём правило протокола

        // Если правило не задано или "NA" — выходим
        if (gfrRule == null || gfrRule.isBlank() || gfrRule.equalsIgnoreCase("NA")) return;

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        //   Разбор текста из столбца GFR в протоколе (Class B ... или <30 ...)
        Map<String, String> rules = new LinkedHashMap<>(); // создание LinkedHashMap для каждого правила из строки протокола, с сохранением порядка добавления из протокола

        //  Парсинг строки протокола регуляркой, которая разделит все совпадения по группам (group 1/2/3/4)
        Matcher matcher = COMPLEX_PATTERN.matcher(gfrRule);
        while (matcher.find()) {
            // --- Новый механизм групп:
            // group(1) → буква класса (A–F)
            // group(2) → действие для класса (например, "interval 12h" или "avoid use")
            // group(3) → числовой порог (<30 mL/min)
            // group(4) → действие для числового порога (например, "reduce by 50%")

            String key;
            String value;

            if (matcher.group(1) != null) {
                // === Случай 1: найден буквенный класс (A–F)
                // Пример: "class B action: interval 12h"
                // group(1) = "B"
                // group(2) = "interval 12h"
                key = matcher.group(1).toUpperCase();          // ключ = "B" (буква класса)
                value = matcher.group(2).trim();               // значение = действие ("interval 12h")
                // Примерный результат: ("B", "interval 12h")

            } else if (matcher.group(3) != null) {
                // === Случай 2: найден числовой порог (<30 mL/min)
                // Пример: "<30 mL/min - avoid"
                // group(3) = "<30 mL/min"
                // group(4) = "avoid"
                key = matcher.group(3).trim();                 // ключ = "<30 mL/min"
                value = matcher.group(4).trim();               // значение = действие ("avoid")
                // Примерный результат: ("<30 mL/min", "avoid")

            } else {
                // если по какой-то причине ничего не поймано — пропускаем итерацию
                continue;
            }

            //  Добавляем найденную пару в LinkedHashMap, чтобы сохранить порядок из Excel
            rules.put(key, value);

            //  Для наглядности логируем найденные соответствия
            log.debug("[GFR] Parsed rule: key='{}' → value='{}'", key, value);
        }

//  После этого у нас может быть, например:
//  rules = { "B"="interval 12h", "C"="avoid use", "<30 mL/min"="avoid" }
        //    Определяем правило, соответствующее GFR пациента
        String matchedRule = findMatchingRule(rules, normalizedGfr, patientGfr);

        if (matchedRule == null) {
            log.info("No GFR rule matched for patient={} (value={})", patient.getId(), patientGfr);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        matchedRule = matchedRule.toLowerCase();
        recommendation.getComments().add("System: applied GFR rule → " + gfrRule);

        //   AVOID CASE — исключить всю рекомендацию
        if (matchedRule.contains("avoid")) {
            rejectionReasons.add(String.format(
                    "[%s] Avoid all drugs for patient (GFR=%s, rule='%s')",
                    getClass().getSimpleName(), patientGfr, gfrRule
            ));
            log.warn("[GFR] Avoid triggered → patient={} value={} rule={}", patient.getId(), patientGfr, gfrRule);

            for (DrugRecommendation d : recommendation.getDrugs()) {
                DrugUtils.clearDrug(d);
            }
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        //   REDUCE CASE — уменьшить дозировку или добавить комментарий
        if (matchedRule.contains("reduce")) {
            applyReduction(drug, recommendation, matchedRule, gfrRule);
        }

        //   INTERVAL CASE — изменить интервал приёма (8h, 12h и т.д.)
        applyIntervalChange(drug, recommendation, matchedRule, gfrRule);

        log.info("[GFR] Applied rule '{}' for {} (GFR={}, protocol={})",
                matchedRule, drug.getActiveMoiety(), patientGfr, tp.getId());
        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }

    // ---------------------- Вспомогательные методы ----------------------

    /** Нормализация значения GFR (буква → класс, число → диапазон) */
    private String normalizeGfrValue(String rawGfr) {
        if (rawGfr == null || rawGfr.isBlank()) return null;

        // Если ввели букву A – F
        if (rawGfr.matches("(?i)[A-F]")) {
            return rawGfr.toUpperCase();
        }

        // Если ввели число (например, "78 ml/min")
        try {
            double value = Double.parseDouble(rawGfr.replaceAll("[^\\d.]", "")); // очищаем число от всего лишнего
            for (Map.Entry<String, double[]> entry : GFR_CLASSES.entrySet()) {                  // пробегаем GFR_CLASSES с этим числом и ищем букву, в диапазон которой оно попадает
                double min = entry.getValue()[0];
                double max = entry.getValue()[1];
                if (value >= min && value <= max) {
                    return entry.getKey();
                }
            }
            // если ввели белиберду
        } catch (NumberFormatException e) {
            log.warn("[GFR] Invalid input '{}', skipping normalization", rawGfr);
        }
        return null;
    }

    /* Подбор подходящего правила по классу или порогу */
    private String findMatchingRule(Map<String, String> rules, String normalizedGfr, String rawGfr) {
        // 1 Прямое совпадение по классу (A – F)
        if (normalizedGfr != null && rules.containsKey(normalizedGfr)) {
            return rules.get(normalizedGfr);
        }

        // 2 Сопоставление буквенных категорий с числовыми порогами (<60 и т.п.)
        if (normalizedGfr != null) {
            double[] range = GFR_CLASSES.getOrDefault(normalizedGfr, new double[]{0, 0});
            double lowerBound = range[0];
            double upperBound = range[1];

            for (Map.Entry<String, String> e : rules.entrySet()) {
                if (e.getKey().startsWith("<")) {
                    double limit = Double.parseDouble(e.getKey().replaceAll("[^\\d.]", ""));
                    // если верхняя граница класса меньше лимита (<60 для C=45-59)
                    if (upperBound < limit) {
                        return e.getValue();
                    }
                }
            }
        }

        // 3 Поиск числового совпадения (если пациент ввёл значение числом)
        try {
            double gfrValue = Double.parseDouble(rawGfr.replaceAll("[^\\d.]", ""));
            for (Map.Entry<String, String> e : rules.entrySet()) {
                if (e.getKey().startsWith("<")) {
                    double limit = Double.parseDouble(e.getKey().replaceAll("[^\\d.]", ""));
                    if (gfrValue < limit) {
                        return e.getValue();
                    }
                }
            }

            // --------------------- [НОВЫЙ БЛОК] ---------------------
            // 4. Если пациент ввёл GFR числом, а в протоколе указаны буквенные классы (A–F)
            //    Тогда нужно определить, к какому классу относится это число, и применить соответствующее правило
            String gfrClassFromNumber = mapGfrToLetter(gfrValue);
            if (gfrClassFromNumber != null && rules.containsKey(gfrClassFromNumber)) {
                log.info("[GFR] Matched numeric value {} → class {} (rule={})", gfrValue, gfrClassFromNumber, rules.get(gfrClassFromNumber));
                return rules.get(gfrClassFromNumber);
            }
            // --------------------------------------------------------

        } catch (NumberFormatException ignored) {}

        return null;
    }

    /** Преобразование числового значения GFR в буквенную категорию (A–F) */
    private String mapGfrToLetter(double gfrValue) {
        for (Map.Entry<String, double[]> entry : GFR_CLASSES.entrySet()) {
            double min = entry.getValue()[0];
            double max = entry.getValue()[1];
            if (gfrValue >= min && gfrValue <= max) {
                return entry.getKey();
            }
        }
        return null;
    }
    /** Применение правила снижения дозировки */
    private void applyReduction(DrugRecommendation drug, Recommendation rec, String rule, String gfrRule) {
        Matcher m = Pattern.compile("(\\d+)%").matcher(rule);
        if (m.find() && drug.getDosing() != null) {
            String percent = m.group(1);
            String oldDose = drug.getDosing();

            //  Можно вычислить новую дозу, если это число
            Matcher d = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(oldDose);
            if (d.find()) {
                double original = Double.parseDouble(d.group(1));
                double reduced = original * (1 - Integer.parseInt(percent) / 100.0);
                drug.setDosing(String.format("%.0f mg", reduced));
                rec.getComments().add(String.format(
                        "System: reduced dose by %s%% (%s → %.0f mg) due to GFR rule: %s",
                        percent, oldDose, reduced, gfrRule
                ));
                log.info("[GFR] Dose reduced: {} mg → {} mg ({}%)", original, reduced, percent);
            } else {
                rec.getComments().add(String.format(
                        "System: reduce dose by %s%% (original dosing: %s)", percent, oldDose));
            }
        }
    }

    /** Применение изменения интервала (8h, 12h и т.п.) */
    private void applyIntervalChange(DrugRecommendation drug, Recommendation rec, String rule, String gfrRule) {
        Matcher m = Pattern.compile("(\\d+)\\s*h").matcher(rule);
        if (m.find()) {
            String newInterval = m.group(1) + "h";

            //  Если указано "first drug" → применяем только к MAIN
            if (rule.contains("first") && drug.getRole() != DrugRole.MAIN) return;

            drug.setInterval(newInterval);
            rec.getComments().add(String.format("System: interval set to %s due to GFR rule: %s", newInterval, gfrRule));
            log.info("[GFR] Interval changed to {} for {}", newInterval, drug.getActiveMoiety());
        }
    }
}

