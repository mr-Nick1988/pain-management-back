package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
WeightRuleApplier — корректировка дозировок или интервалов для пациентов с низким весом (<50 кг).
Если в протоколе указано "<50kg - 8h" или "<50kg - 50mg", применяется соответствующее правило.
*/

//@Component
@Order(10)
@Slf4j
public class WeightRuleApplier implements TreatmentRuleApplier {

    // Паттерн для поиска последнего числа (поддержка десятичных)
    private static final Pattern LAST_NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)(?=[^0-9]*$)");

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        // 1 Если препарат уже отклонён или пустой — выходим
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 2 Извлекаем вес пациента
        Double patientWeight = patient.getEmr().getLast().getWeight();
        if (patientWeight == null) {
            log.warn("Patient weight is null — cannot apply {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // По протоколу корректировка применяется только если вес < 50 кг
        if (patientWeight >= 50.0) {
            log.debug("Patient weight {}kg ≥ 50kg — rule not applied", patientWeight);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 3 Получаем правило из протокола
        String weightRule = (drug.getRole() == DrugRole.MAIN) ? tp.getWeightKg() : tp.getSecondWeightKg();
        if (weightRule == null || weightRule.trim().isEmpty() || weightRule.toUpperCase().contains("NA")) {
            log.debug("Weight rule empty or NA for protocol {}", tp.getId());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 4 Извлекаем последнее число из строки (например "<50kg - 8h" → 8)
        Matcher m = LAST_NUMBER_PATTERN.matcher(weightRule);
        if (!m.find()) {
            log.warn("No numeric value found in weight rule '{}'", weightRule);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        String lastNumber = m.group(1);
        // Определяем единицу измерения (mg или h)
        int afterIndex = m.end();
        String suffix = weightRule.substring(afterIndex).replaceAll("[^a-zA-Z%]", "").toLowerCase();

        if (recommendation.getComments() == null)
            recommendation.setComments(new ArrayList<>());

        // 5 Применяем корректировку по единице измерения
        if (suffix.contains("mg")) {
            // Корректировка дозы
            String newDose = lastNumber + " mg";
            drug.setDosing(newDose);

            String comment = String.format(
                    "System: dose adjusted for weight <50kg → %s for %s (%.1fkg)",
                    newDose,
                    (drug.getDrugName() != null ? drug.getDrugName() : drug.getActiveMoiety()),
                    patientWeight
            );
            recommendation.getComments().add(comment);
            rejectionReasons.add(String.format(
                    "[%s] Dose adjusted due to low weight (%.1fkg): set dosing to %s",
                    getClass().getSimpleName(),
                    patientWeight,
                    newDose
            ));

            log.info("Dose adjusted for patient={} weight={}kg, new dosing={}",
                    patient.getId(), patientWeight, newDose);

        } else if (suffix.contains("h")) {
            // Корректировка интервала
            String newInterval = lastNumber + "h";
            drug.setInterval(newInterval);

            String comment = String.format(
                    "System: interval adjusted for weight <50kg → %s for %s (%.1fkg)",
                    newInterval,
                    (drug.getDrugName() != null ? drug.getDrugName() : drug.getActiveMoiety()),
                    patientWeight
            );
            recommendation.getComments().add(comment);
            rejectionReasons.add(String.format(
                    "[%s] Interval adjusted due to low weight (%.1fkg): set interval to %s",
                    getClass().getSimpleName(),
                    patientWeight,
                    newInterval
            ));

            log.info("Interval adjusted for patient={} weight={}kg, new interval={}",
                    patient.getId(), patientWeight, newInterval);

        } else {
            // Неизвестная единица измерения
            String message = String.format("Unknown unit '%s' in weight rule '%s'", suffix, weightRule);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }
}