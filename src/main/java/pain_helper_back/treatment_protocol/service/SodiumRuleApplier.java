package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;
import pain_helper_back.treatment_protocol.utils.PatternUtils;

import java.util.ArrayList;
import java.util.List;

/*
Sodium (Na⁺) — концентрация натрия в крови.
Измеряется в миллиэквивалентах на литр (mEq/L).
Нормальный диапазон: 135–145 mEq/L.
Если уровень Na⁺ <130 mEq/L — это гипонатриемия (пониженный натрий),
при которой рекомендуется избегать большинства препаратов (avoid).
*/

//@Component
@Order(7)
@Slf4j
public class SodiumRuleApplier implements TreatmentRuleApplier {

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        // 1 Пропуск, если препарат уже очищен или пуст
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 2 Извлекаем данные пациента и протокола
        Double patientSodium = patient.getEmr().getLast().getSodium(); // напр. 128.0
        String rule = tp.getSodium();                                 // напр. "<130 - avoid"

        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) {
            log.debug("Sodium rule empty or NA for protocol {}", tp.getId());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        if (patientSodium == null) {
            log.warn("Patient sodium is null — cannot apply {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 3 Извлекаем числовой порог из строки протокола (например "<130 - avoid" → 130)
        Integer limit = PatternUtils.extractFirstInt(rule);
        if (limit == null) {
            log.warn("Could not extract numeric limit from sodium rule '{}'", rule);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        double limitDouble = limit.doubleValue();

        // 4 Проверяем, ниже ли уровень натрия порога
        if (patientSodium < limitDouble) {

            if (recommendation.getComments() == null)
                recommendation.setComments(new ArrayList<>());

            // Формируем системное сообщение
            String comment = String.format(
                    "System: avoid for sodium < %.0f mmol/L (patient %.1f mmol/L)",
                    limitDouble,
                    patientSodium
            );

            recommendation.getComments().add(comment);
            rejectionReasons.add(String.format(
                    "[%s] Avoid triggered by sodium rule '%s' (limit=%.0f, patient=%.1f)",
                    getClass().getSimpleName(),
                    rule,
                    limitDouble,
                    patientSodium
            ));

            // Очищаем препараты
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);

            log.warn("Avoid triggered by sodium rule: patient={}, value={}, rule={}",
                    patient.getId(), patientSodium, rule);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }
}