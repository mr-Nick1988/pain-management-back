package pain_helper_back.treatment_protocol.service.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.service.CorrectionAggregator;
import pain_helper_back.treatment_protocol.service.TreatmentRuleApplier;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(9)
@Slf4j
public class GfrRuleApplier implements TreatmentRuleApplier {

    // Поддержка шаблонов вида:
    // "Class B - 12h", "Class C - avoid", "<30 mL/min - avoid", "<60 mL/min - reduce by 50%"
    private static final Pattern COMPLEX_PATTERN = Pattern.compile(
            "(?i)" +
                    "(?:class\\s*)?([A-F])(?:\\s*action)?\\s*[:\\-]\\s*([^<]+?)(?=$|\\bclass\\b|<\\d+\\s*mL/min)" +
                    "|" +
                    "(<\\d+\\s*mL/min)\\s*[-:]\\s*([^<]+)"
    );

    private final CorrectionAggregator correctionAggregator;

    // Диапазоны для категорий GFR (в мл/мин)
    private static final Map<String, double[]> GFR_CLASSES = Map.of(
            "A", new double[]{90, Double.MAX_VALUE},
            "B", new double[]{60, 89},
            "C", new double[]{45, 59},
            "D", new double[]{30, 44},
            "E", new double[]{15, 29},
            "F", new double[]{0, 14}
    );

    public GfrRuleApplier(CorrectionAggregator correctionAggregator) {
        this.correctionAggregator = correctionAggregator;
    }

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        if (!DrugUtils.hasInfo(drug)) return;

        String patientGfr = patient.getEmr().getLast().getGfr();  // буква или число
        String normalizedGfr = normalizeGfrValue(patientGfr);
        String gfrRule = tp.getGfr();

        if (gfrRule == null || gfrRule.isBlank() || gfrRule.equalsIgnoreCase("NA")) return;

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        // Разбор строки GFR из протокола
        Map<String, String> rules = new LinkedHashMap<>();
        Matcher matcher = COMPLEX_PATTERN.matcher(gfrRule);
        while (matcher.find()) {
            String key;
            String value;

            if (matcher.group(1) != null) {
                key = matcher.group(1).toUpperCase();
                value = matcher.group(2).trim();
            } else if (matcher.group(3) != null) {
                key = matcher.group(3).trim();
                value = matcher.group(4).trim();
            } else continue;

            rules.put(key, value);
            log.debug("[GFR] Parsed rule: '{}' → '{}'", key, value);
        }

        String matchedRule = findMatchingRule(rules, normalizedGfr, patientGfr);
        if (matchedRule == null) {
            log.info("No GFR rule matched for patient={} (value={})", patient.getId(), patientGfr);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        matchedRule = matchedRule.toLowerCase();
        recommendation.getComments().add("System: applied GFR rule → " + gfrRule);

        // --- 1. AVOID CASE ---
        if (matchedRule.contains("avoid")) {
            rejectionReasons.add(String.format(
                    "[%s] Avoid all drugs for patient (GFR=%s, rule='%s')",
                    getClass().getSimpleName(), patientGfr, gfrRule
            ));
            for (DrugRecommendation d : recommendation.getDrugs()) {
                DrugUtils.clearDrug(d);
            }
            log.warn("[GFR] Avoid triggered → patient={} (rule='{}')", patient.getId(), matchedRule);
            return;
        }

        // --- 2. REDUCE CASE ---
        if (matchedRule.contains("reduce")) {
            applyReduction(drug, recommendation, matchedRule, gfrRule);
        }

        // --- 3. INTERVAL CASE ---
        applyIntervalChange(drug, recommendation, matchedRule, gfrRule);

        log.info("[GFR] Applied rule '{}' for {} (GFR={}, protocol={})",
                matchedRule, drug.getActiveMoiety(), patientGfr, tp.getId());
        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }

    // ---------------------- Вспомогательные методы ----------------------

    private String normalizeGfrValue(String rawGfr) {
        if (rawGfr == null || rawGfr.isBlank()) return null;

        if (rawGfr.matches("(?i)[A-F]")) {
            return rawGfr.toUpperCase();
        }

        try {
            double value = Double.parseDouble(rawGfr.replaceAll("[^\\d.]", ""));
            for (Map.Entry<String, double[]> entry : GFR_CLASSES.entrySet()) {
                double min = entry.getValue()[0];
                double max = entry.getValue()[1];
                if (value >= min && value <= max) return entry.getKey();
            }
        } catch (NumberFormatException e) {
            log.warn("[GFR] Invalid input '{}', skipping normalization", rawGfr);
        }
        return null;
    }

    private String findMatchingRule(Map<String, String> rules, String normalizedGfr, String rawGfr) {
        if (normalizedGfr != null && rules.containsKey(normalizedGfr)) {
            return rules.get(normalizedGfr);
        }

        if (normalizedGfr != null) {
            double[] range = GFR_CLASSES.getOrDefault(normalizedGfr, new double[]{0, 0});
            double upperBound = range[1];

            for (Map.Entry<String, String> e : rules.entrySet()) {
                if (e.getKey().startsWith("<")) {
                    double limit = Double.parseDouble(e.getKey().replaceAll("[^\\d.]", ""));
                    if (upperBound < limit) return e.getValue();
                }
            }
        }

        try {
            double gfrValue = Double.parseDouble(rawGfr.replaceAll("[^\\d.]", ""));
            for (Map.Entry<String, String> e : rules.entrySet()) {
                if (e.getKey().startsWith("<")) {
                    double limit = Double.parseDouble(e.getKey().replaceAll("[^\\d.]", ""));
                    if (gfrValue < limit) return e.getValue();
                }
            }

            String gfrClassFromNumber = mapGfrToLetter(gfrValue);
            if (gfrClassFromNumber != null && rules.containsKey(gfrClassFromNumber)) {
                log.info("[GFR] Matched numeric value {} → class {} (rule={})",
                        gfrValue, gfrClassFromNumber, rules.get(gfrClassFromNumber));
                return rules.get(gfrClassFromNumber);
            }

        } catch (NumberFormatException ignored) {}
        return null;
    }

    private String mapGfrToLetter(double gfrValue) {
        for (Map.Entry<String, double[]> entry : GFR_CLASSES.entrySet()) {
            double min = entry.getValue()[0];
            double max = entry.getValue()[1];
            if (gfrValue >= min && gfrValue <= max) return entry.getKey();
        }
        return null;
    }

    /* Применение снижения дозировки */
    private void applyReduction(DrugRecommendation drug, Recommendation rec, String rule, String gfrRule) {
        Matcher m = Pattern.compile("(\\d+)%").matcher(rule);
        if (m.find() && drug.getDosing() != null) {
            String percent = m.group(1);
            String oldDose = drug.getDosing();

            Matcher d = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(oldDose);
            if (d.find()) {
                double original = Double.parseDouble(d.group(1));
                double reduced = original * (1 - Integer.parseInt(percent) / 100.0);
                drug.setDosing(String.format("%.0f mg", reduced));

                //  записываем в CorrectionAggregator
                correctionAggregator.addDoseCorrection(drug, (int) reduced);

                rec.getComments().add(String.format(
                        "System: reduced dose by %s%% (%s → %.0f mg) due to GFR rule: %s",
                        percent, oldDose, reduced, gfrRule
                ));
                log.info("[GFR] Dose reduced {} → {} mg ({}%)", original, reduced, percent);
            } else {
                rec.getComments().add(String.format(
                        "System: reduce dose by %s%% (original dosing: %s)", percent, oldDose));
            }
        }
    }

    /* Применение изменения интервала (8h, 12h и т.п.) */
    private void applyIntervalChange(DrugRecommendation drug, Recommendation rec, String rule, String gfrRule) {
        Matcher m = Pattern.compile("(\\d+)\\s*h").matcher(rule);
        if (m.find()) {
            int numericInterval = Integer.parseInt(m.group(1));
            String newInterval = numericInterval + "h";

            if (rule.contains("first") && drug.getRole() != DrugRole.MAIN) return;

            drug.setInterval(newInterval);
            rec.getComments().add(String.format("System: interval set to %s due to GFR rule: %s", newInterval, gfrRule));

            //  записываем в CorrectionAggregator
            correctionAggregator.addIntervalCorrection(drug, numericInterval);

            log.info("[GFR] Interval changed to {} for {}", newInterval, drug.getActiveMoiety());
        }
    }
}