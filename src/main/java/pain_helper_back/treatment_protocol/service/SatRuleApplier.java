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
SAT (oxygen saturation, SpO₂) — уровень насыщения крови кислородом.
Измеряется в процентах (%).
Нормальный диапазон: 95–100%.
Если сатурация <93%, рекомендуется избегать большинства препаратов (avoid).
*/

//@Component
@Order(6)
@Slf4j
public class SatRuleApplier implements TreatmentRuleApplier {

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        // 1 Проверка, есть ли что анализировать
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        Double patientSat = patient.getEmr().getLast().getSat();  // Например: 91.0
        String rule = tp.getSat();                                // Например: "<93 - avoid"

        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) {
            log.debug("SAT rule empty or NA for protocol {}", tp.getId());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        if (patientSat == null) {
            log.warn("Patient SAT is null — cannot apply {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 2 Извлекаем числовой лимит (например из "<93 - avoid" → 93)
        Integer limit = PatternUtils.extractFirstInt(rule);
        if (limit == null) {
            log.warn("Could not extract numeric limit from SAT rule '{}'", rule);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        double limitDouble = limit.doubleValue();

        // 3 Проверяем условие (если сатурация ниже порога)
        if (patientSat < limitDouble) {

            if (recommendation.getComments() == null)
                recommendation.setComments(new ArrayList<>());

            // Добавляем системные комментарии
            String comment = String.format(
                    "System: avoid for SAT < %.0f%% (patient %.1f%%)",
                    limitDouble,
                    patientSat
            );
            recommendation.getComments().add(comment);

            // Добавляем причину в общий список отказов
            rejectionReasons.add(String.format(
                    "[%s] Avoid triggered by SAT rule '%s' (limit=%.0f, patient=%.1f)",
                    getClass().getSimpleName(),
                    rule,
                    limitDouble,
                    patientSat
            ));

            // Очищаем препараты
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);

            log.warn("Avoid triggered by SAT rule: patient={}, value={}, rule={}", patient.getId(), patientSat, rule);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }
}