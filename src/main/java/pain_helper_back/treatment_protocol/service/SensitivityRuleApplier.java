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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/*
AVOID if sensitivity — правило исключения препаратов при индивидуальной чувствительности (аллергии).
Пациент может иметь список чувствительных веществ (например, ["PARACETAMOL", "TRAMADOL"]).
Если в Treatment Protocol указано "PARACETAMOL OR TRAMADOL",
и одно из веществ совпадает с пациентскими, препараты из рекомендации исключаются (avoid).
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

        //  Проверка: если препарат пустой — пропускаем
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        //  Данные пациента и протокола
        Emr emr = patient.getEmr().getLast();
        List<String> sensitivities = emr.getSensitivities();
        String rule = tp.getAvoidIfSensitivity();

        //  Предварительные проверки (ранний выход)
        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")
                || sensitivities == null || sensitivities.isEmpty()) {
            log.debug("No sensitivity data or rule NA for {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        //  Приводим обе стороны к верхнему регистру
        List<String> ruleSensitivities = Stream.of(rule.split("OR"))
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        List<String> normalizedPatientSens = sensitivities.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        //  Проверяем совпадения
        boolean hasMatch = ruleSensitivities.stream().anyMatch(normalizedPatientSens::contains);

        if (hasMatch) {
            // Защита от NPE
            if (recommendation.getComments() == null)
                recommendation.setComments(new ArrayList<>());

            // Формируем человекочитаемое сообщение
            String comment = String.format(
                    "System: avoid due to sensitivity match. Rule=%s, Patient=%s",
                    ruleSensitivities,
                    normalizedPatientSens
            );

            // Добавляем комментарии и причину
            recommendation.getComments().add(comment);
            rejectionReasons.add(String.format(
                    "[%s] Avoid triggered by sensitivity match. Rule=%s, Patient=%s",
                    getClass().getSimpleName(),
                    ruleSensitivities,
                    normalizedPatientSens
            ));

            // Очищаем препараты
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);

            log.warn("Avoid triggered by sensitivity rule: patient={}, sensitivities={}, rule={}",
                    patient.getId(), normalizedPatientSens, ruleSensitivities);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }
}