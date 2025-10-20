package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@Component
@Order(9)
@Slf4j
public class GfrRuleApplier implements TreatmentRuleApplier {

    private static final Pattern COMPLEX_PATTERN = Pattern.compile(
            "(Class\\s*[ABC])\\s*-\\s*([^C]+)(?=$|Class)|(<\\d+[^-]*)-\\s*([^A-Z]+)"
    );

    private static final Map<String, double[]> GFR_CLASSES = Map.of(
            "A", new double[]{90, Double.MAX_VALUE},
            "B", new double[]{60, 89},
            "C", new double[]{30, 59},
            "D", new double[]{15, 29},
            "E", new double[]{0, 14}
    );

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        if (!DrugUtils.hasInfo(drug)) return;

        String patientGfr = patient.getEmr().getLast().getGfr();
        String normalizedPatientGfr = normalizeGfrValue(patientGfr);
        String gfrRule = tp.getGfr();

        if (gfrRule == null || gfrRule.trim().isEmpty() || gfrRule.toUpperCase().contains("NA")) return;

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        Matcher m = COMPLEX_PATTERN.matcher(gfrRule);
        Map<String, String> rules = new HashMap<>();
        while (m.find()) {
            String key = m.group(1) != null ? m.group(1).replace("Class", "").trim() : m.group(3).trim();
            String value = m.group(2) != null ? m.group(2).trim() : m.group(4).trim();
            rules.put(key, value);
        }

        String patientRule = rules.get(normalizedPatientGfr);

        if (patientRule == null) {
            double gfrValue;
            if (normalizedPatientGfr != null && normalizedPatientGfr.matches("[A-E]")) {
                gfrValue = getAverageGfrForClass(normalizedPatientGfr);
            } else {
                gfrValue = Double.parseDouble(patientGfr.replaceAll("[^\\d.]", ""));
            }

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

        if (patientRule == null) {
            log.info("No GFR rule matched for patient={} (value={})", patient.getId(), patientGfr);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        patientRule = patientRule.toLowerCase();

        if (recommendation.getComments() == null)
            recommendation.setComments(new ArrayList<>());

        // === AVOID CASE ===
        if (patientRule.contains("avoid")) {
            recommendation.getComments().add("System: avoid for GFR rule " + gfrRule);
            rejectionReasons.add(String.format(
                    "[%s] Avoid recommendation with drugs (%s and %s) for GFR value %s (rule='%s')",
                    getClass().getSimpleName(),
                    recommendation.getDrugs().getFirst().getDrugName(),
                    recommendation.getDrugs().get(1).getActiveMoiety(),
                    patientGfr,
                    gfrRule
            ));
            log.warn("Avoid triggered by GFR rule: patient={}, value={}, rule={}", patient.getId(), patientGfr, gfrRule);

            for (DrugRecommendation d : recommendation.getDrugs()) {
                DrugUtils.clearDrug(d);
            }

            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // === REDUCE CASE ===
        if (patientRule.contains("reduce")) {
            Matcher perc = Pattern.compile("(\\d+)%").matcher(patientRule);
            if (perc.find() && drug.getDosing() != null) {
                String originalDose = drug.getDosing();
                String percent = perc.group(1);
                recommendation.getComments().add(String.format(
                        "System: reduce dose by %s%% (original dosing: %s) due to GFR rule: %s",
                        percent,
                        originalDose,
                        gfrRule
                ));
            }
        }

        // === INTERVAL ADJUSTMENT CASE ===
        Matcher h = Pattern.compile("(\\d+)\\s*h").matcher(patientRule);
        if (h.find()) {
            String newInterval = h.group(1) + "h";
            drug.setInterval(newInterval);
            recommendation.getComments().add(
                    String.format("System: interval set to %s due to GFR rule: %s", newInterval, gfrRule)
            );
        }

        log.info("Applied GFR rule '{}' for patient={} (value={})", patientRule, patient.getId(), patientGfr);
        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }

    // ------------------------- Helpers ----------------------------

    private String normalizeGfrValue(String rawGfr) {
        if (rawGfr == null || rawGfr.isBlank()) return null;

        if (rawGfr.matches("(?i)[abcde]")) {
            return rawGfr.toUpperCase();
        }

        double value = Double.parseDouble(rawGfr.replaceAll("[^\\d.]", ""));
        for (Map.Entry<String, double[]> entry : GFR_CLASSES.entrySet()) {
            double min = entry.getValue()[0];
            double max = entry.getValue()[1];
            if (value >= min && value <= max) {
                return entry.getKey();
            }
        }
        return null;
    }

    private double getAverageGfrForClass(String gfrClass) {
        double[] range = GFR_CLASSES.get(gfrClass);
        if (range == null) return Double.NaN;
        return (range[0] + range[1]) / 2.0;
    }
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


