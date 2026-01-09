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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
WeightRuleApplier — корректировка дозировок or интервалов for patientов с нfromким весом (<50 кг).
if в protocolе указано "<50kg - 8h" or "<50kg - 50mg", применяется соresponseствующее правило.
*/

@Component
@Order(10)
@Slf4j
public class WeightRuleApplier implements TreatmentRuleApplier {

    // Паттерн for точного разбора действия по весу: "<50kg - 8h" or "<50kg - 50mg" fromвлекает after тире цифру(group 1) и меру (group 2)
    private static final Pattern WEIGHT_ACTION_PATTERN = Pattern.compile(
            "(?i)<\\s*50\\s*kg\\s*[-:]\\s*(\\d+(?:\\.\\d+)?)\\s*(mg|h)\\b"
    );

    private final CorrectionAggregator correctionAggregator;

    public WeightRuleApplier(CorrectionAggregator correctionAggregator) {
        this.correctionAggregator = correctionAggregator;
    }

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        // 1 if drug уже отклонён or пустой — выходим
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 2 Extract вес patient
        Double patientWeight = patient.getEmr().getLast().getWeight();
        if (patientWeight == null) {
            log.warn("Patient weight is null — cannot apply {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // По protocolу корректировка применяется only if вес < 50 кг
        if (patientWeight >= 50.0) {
            log.debug("Patient weight {}kg ≥ 50kg — rule not applied", patientWeight);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 3 Get правило from нужной колонки (первое or второе medicine)
        String weightRule = (drug.getRole() == DrugRole.MAIN) ? tp.getWeightKg() : tp.getSecondWeightKg();
        if (weightRule == null || weightRule.trim().isEmpty() || weightRule.trim().toUpperCase().contains("NA")) {
            log.debug("Weight rule empty or NA for protocol {}", tp.getId());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 4 Чёткий парсинг действия вида "<50kg - 8h" or "<50kg - 50mg"
        Matcher m = WEIGHT_ACTION_PATTERN.matcher(weightRule);
        if (!m.find()) {
            log.warn("Weight rule didn't match expected pattern '<50kg - X[h|mg]': '{}'", weightRule);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        String number = m.group(1);            // "8" or "50"
        String unit   = m.group(2).toLowerCase(); // "h" or "mg"

        // 5 Apply корректировку по единице fromмерения
        String drugLabel = (drug.getDrugName() != null ? drug.getDrugName() : drug.getActiveMoiety());
        if ("mg".equals(unit)) {
            // Корректировка дозы, наExample "<50kg - 50mg"
            String newDose = number + " mg";
            drug.setDosing(newDose);
            correctionAggregator.addDoseCorrection(drug,Integer.parseInt(number));
            recommendation.getComments().add(String.format(
                    "System: dose adjusted for weight <50kg → %s for %s (%.1fkg) [rule column=%s]",
                    newDose, drugLabel, patientWeight,
                    (drug.getRole() == DrugRole.MAIN ? "weight (kg)" : "2nd weight (kg)")
            ));
            log.info("Dose adjusted for patient={} weight={}kg, new dosing={}, rule='{}'",
                    patient.getId(), patientWeight, newDose, weightRule);

        } else { // "h"
            // Корректировка интервала, наExample "<50kg - 8h"
            String newInterval = number + "h";
            drug.setInterval(newInterval);
            correctionAggregator.addIntervalCorrection(drug,Integer.parseInt(number));
            recommendation.getComments().add(String.format(
                    "System: interval adjusted for weight <50kg → %s for %s (%.1fkg) [rule column=%s]",
                    newInterval, drugLabel, patientWeight,
                    (drug.getRole() == DrugRole.MAIN ? "weight (kg)" : "2nd weight (kg)")
            ));
            log.info("Interval adjusted for patient={} weight={}kg, new interval={}, rule='{}'",
                    patient.getId(), patientWeight, newInterval, weightRule);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }
}