package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(11)
@Slf4j
public class GfrRuleApplier implements TreatmentRuleApplier {
// GFR — это показатель фильтрационной способности почек, измеряемый в мл/мин.
// На фронте врач может ввести либо цифру (например 62), либо класс A|B|C|D|E:
// Оба типа ввода врача тут поддерживаются: и числа, и буквы.
// A >90     Норма
// B 60–89   Незначительное снижение
// C 30–59   Умеренное снижение
// D 15–29   Тяжёлое снижение
// E <15     Почечная недостаточность
// Т.е. пациент может быть описан либо как “C”, либо как “48 мл/мин”, а это одно и то же состояние.


    private static final Pattern COMPLEX_PATTERN = Pattern.compile(
            //Эта страшная строка на самом деле просто “ловит” два варианта записи в таблице: Class B - 12h или <30 ml/min - avoid
            "(Class\\s*[ABC])\\s*-\\s*([^C]+)(?=$|Class)|(<\\d+[^-]*)-\\s*([^A-Z]+)"
    );

    // Соответствие классов (A–E) диапазонам GFR в мл/мин
    private static final Map<String, double[]> GFR_CLASSES = Map.of(
            "A", new double[]{90, Double.MAX_VALUE}, // >90
            "B", new double[]{60, 89},
            "C", new double[]{30, 59},
            "D", new double[]{15, 29},
            "E", new double[]{0, 14}
    );

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        // если препарат пустой или уже "отвергнут" ранее — не трогаем
        if (!DrugUtils.hasInfo(drug)) return;

        String patientGfr = patient.getEmr().getLast().getGfr();
        String normalizedPatientGfr = normalizeGfrValue(patientGfr);
        String gfrRule = tp.getGfr();
        if (gfrRule == null || gfrRule.trim().isEmpty() || gfrRule.toUpperCase().contains("NA")) return;

        // Сопоставляем все возможные группы (Class A/B/C или <число)
        Matcher m = COMPLEX_PATTERN.matcher(gfrRule);
        Map<String, String> rules = new HashMap<>();
        while (m.find()) {
            String key = m.group(1) != null ? m.group(1).replace("Class", "").trim() : m.group(3).trim(); // "B" или "<60"
            String value = m.group(2) != null ? m.group(2).trim() : m.group(4).trim();
            rules.put(key, value);
        }
        // 1 Проверяем по ключу "Class A/B/C"
        String patientRule = rules.get(normalizedPatientGfr);  // <----Пробуем найти подходящее правило по ключу
        // 2 Если не нашли буквенное правило — ищем по числовым порогам
        if (patientRule == null) {
            double gfrValue;

            // если врач ввёл букву — переводим её в среднее числовое значение
            if (normalizedPatientGfr.matches("[A-E]")) {
                gfrValue = getAverageGfrForClass(normalizedPatientGfr);
            } else {
                gfrValue = Double.parseDouble(patientGfr.replaceAll("[^\\d.]", ""));
            }

            // сравниваем с порогами из протокола
            for (Map.Entry<String, String> e : rules.entrySet()) {
                if (e.getKey().startsWith("<")) {
                    double limit = Double.parseDouble(e.getKey().replaceAll("[^\\d.]", ""));
                    if (gfrValue < limit) {
                        patientRule = e.getValue();
                        break;
                    }
                }
            }
        }
        // 3 Если не нашли совпадений patientRule с полученными Map rules - корректировки и ограничений нет
        if (patientRule == null) return;
        //Если написано “avoid” — добавляем комментарий и обнуляем объекты DrugRecommendation и останавливаемся.
        if (patientRule.toLowerCase().contains("avoid")) {
            recommendation.getComments().add("System: avoid for GFR rule " + gfrRule);
            log.info("Avoid triggered by GFR rule: patient={}, value={}, rule={}", patient.getId(), patientGfr, gfrRule);
            for (DrugRecommendation drugs : recommendation.getDrugs()) {
                DrugUtils.clearDrug(drugs);
            }
            return;
        }

        //Если написано “reduce by 50%”: Уменьшаем дозировку и оставляем пометку в комментариях.
        if (patientRule.toLowerCase().contains("reduce")) {
            Matcher perc = Pattern.compile("(\\d+)%").matcher(patientRule);
            if (perc.find() && drug.getDosing() != null) {
                String originalDose = drug.getDosing(); // сохраняем исходное значение
                String percent = perc.group(1);
                recommendation.getComments().add(
                        "System: reduce dose by " + percent + "% (original dosing: " + originalDose + ")" + "by the reason of GFR rule:" + gfrRule
                );
                // Ничего не затираем! Просто оставляем как есть
                // drug.setDosing(originalDose); // не трогаем
            }
        }
        //Если есть упоминание 8h, 12h и т.п.:
        Matcher h = Pattern.compile("(\\d+)\\s*h").matcher(patientRule);
        if (h.find()) {
            drug.setInterval(h.group(1) + "h");
            recommendation.getComments().add("System: interval set to " + h.group(1) + "h due to GFR");
        }
    }


    //---------------------------------------- Additional Methods for reducing GFR data----------------------------------

    //Если в протоколе только буквы, а врач ввёл число - внутри метода normalizeGfrValue мы просто унифицируем входное значение
    private String normalizeGfrValue(String rawGfr) {
        if (rawGfr == null || rawGfr.isBlank()) return null;

        // 1 Если врач ввёл букву — просто возвращаем в верхнем регистре
        if (rawGfr.matches("(?i)[abcde]")) {
            return rawGfr.toUpperCase();
        }

        // 2 Если врач ввёл число — определяем класс по диапазону
        double value = Double.parseDouble(rawGfr.replaceAll("[^\\d.]", ""));
        for (Map.Entry<String, double[]> entry : GFR_CLASSES.entrySet()) {
            double min = entry.getValue()[0];
            double max = entry.getValue()[1];
            if (value >= min && value <= max) {
                return entry.getKey(); // вернёт "B", "C" и т.д.
            }
        }
        return null;
    }

    // Если наоборот (врач ввёл B, а протокол говорит <60) - возвращаем среднее значение диапазона GFR для класса A–E
    private double getAverageGfrForClass(String gfrClass) {
        double[] range = GFR_CLASSES.get(gfrClass);
        if (range == null) return Double.NaN;
        return (range[0] + range[1]) / 2.0;
    }

 /*
 Итого этот фильтр:

Принимает ввод врача в двух форматах: число или буква (A–E) — и унифицирует это через normalizeGfrValue().
Понимает протокол даже если он написан в произвольной форме (Class B - 12h, <60 ml/min - avoid).
Делает перекрёстное сопоставление, если типы не совпадают — т.е. врач ввёл букву, а в протоколе цифры, и наоборот.
Это именно то, чего часто не хватает в "умных" фильтрах.

Применяет три сценария:
avoid → исключить лекарство, добавить комментарий
reduce by N% → уменьшить дозу
8h, 12h и т. д. → изменить интервал приёма

Всё это делает в рамках интерфейса TreatmentRuleApplier, что даёт
 настоящий полиморфизм и расширяемость микросервиса.*/

}
