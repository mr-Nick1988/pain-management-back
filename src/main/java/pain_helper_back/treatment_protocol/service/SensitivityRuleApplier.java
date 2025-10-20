package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;
import pain_helper_back.treatment_protocol.utils.SafeValueUtils;
import pain_helper_back.treatment_protocol.utils.SanitizeUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/*
 * AVOID if sensitivity — правило исключения препаратов при индивидуальной чувствительности (аллергии).
 * Пациент может иметь список чувствительных веществ (например, ["PARACETAMOL", "TRAMADOL"]).
 * Если в Treatment Protocol указано "PARACETAMOL OR TRAMADOL",
 * и одно из веществ совпадает с пациентскими, препараты из рекомендации исключаются (avoid).
 */

@Component
@Slf4j
@Order(3)
public class SensitivityRuleApplier implements TreatmentRuleApplier {

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        //  Пропускаем, если препарат уже отклонён или пустой
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // Извлекаем данные пациента и протокола
        Emr emr = patient.getEmr().getLast();
        List<String> sensitivities = emr.getSensitivities();
        String rule = tp.getAvoidIfSensitivity();

        //  Проверяем входные данные (ранний выход)
        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")
                || sensitivities == null || sensitivities.isEmpty()) {
            log.debug("No sensitivity data or rule NA for {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // Нормализуем данные: приводим всё к верхнему регистру
        List<String> ruleSensitivities = Stream.of(rule.split("\\s*OR\\s*")) //Разделить строку в тех местах, где встречается слово OR
                .map(SanitizeUtils::normalize)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> normalizedPatientSens = sensitivities.stream()
                .map(SanitizeUtils::normalize)
                .filter(s -> !s.isEmpty())
                .toList();

        log.warn("[DEBUG] ruleRaw='{}' | ruleSensitivities={} | patientSensitivities={}",
                rule, ruleSensitivities, normalizedPatientSens);

        //  Проверяем совпадения между протоколом и данными пациента
        boolean hasMatch = ruleSensitivities.stream().anyMatch(normalizedPatientSens::contains);

        if (hasMatch) {
            //  Безопасно извлекаем имена препаратов (избегаем NPE)
            String mainDrugName = SafeValueUtils.safeValue(recommendation.getDrugs().getFirst());
            String altMoiety = SafeValueUtils.safeValue(recommendation.getDrugs().get(1));

            //  Формируем причину исключения рекомендации (system reason)
            String reasonText = String.format(
                    "[%s] Avoid recommendation with drugs (%s and %s) triggered by sensitivity match. Rule=%s, Patient=%s",
                    getClass().getSimpleName(),
                    mainDrugName,
                    altMoiety,
                    ruleSensitivities,
                    normalizedPatientSens
            );

            rejectionReasons.add(reasonText);

            // Полностью очищаем препараты (avoid)
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);

            log.warn("Avoid triggered by sensitivity rule: patient={}, sensitivities={}, rule={}",
                    patient.getId(), normalizedPatientSens, ruleSensitivities);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }



}