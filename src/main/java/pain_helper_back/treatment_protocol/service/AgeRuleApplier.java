package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.PatternUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(1)
public class AgeRuleApplier implements TreatmentRuleApplier {

    /**
     * Применяет возрастное правило к конкретной прописке препарата.
     * Если препарат разрешён — заполняет данные из TP.
     * Если противопоказан — добавляет запись в rejectionReasons и comments.
     */
    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        int patientAge = patient.getAge();
        String ageAdjustment = (drug.getRole() == DrugRole.MAIN)
                ? tp.getFirstAgeAdjustments()
                : tp.getSecondAgeAdjustments();

        //  1 Проверка: если правило пустое или "NA" — ограничений нет
        if (ageAdjustment == null || ageAdjustment.trim().isEmpty() || ageAdjustment.equalsIgnoreCase("NA")) {
            fillDrugFromProtocol(drug, tp);
            log.debug("{}: No age restriction (NA)", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 2 Извлекаем числовой порог (например из ">75 years - avoid" → 75)
        Integer limit = PatternUtils.extractFirstInt(ageAdjustment);
        if (limit == null) {
            log.error("Invalid protocol config: '{}'", ageAdjustment);
            throw new IllegalArgumentException("Invalid protocol config: " + ageAdjustment);
        }

        if (recommendation.getComments() == null)
            recommendation.setComments(new ArrayList<>());

        // 3 Применяем возрастное правило
        if (drug.getRole() == DrugRole.MAIN) {
            // Основной препарат запрещён при возрасте выше лимита
            if (patientAge > limit) {
                String comment = String.format(
                        "System: avoid main drug %s — patient age (%d) > %d",
                        drug.getActiveMoiety(), patientAge, limit
                );
                recommendation.getComments().add(comment);
                rejectionReasons.add(String.format(
                        "[%s] Avoid triggered: main drug %s rejected (age=%d > limit=%d)",
                        getClass().getSimpleName(), drug.getActiveMoiety(), patientAge, limit
                ));

                log.warn("{} rejected main drug (age={} > {}) for patient {}",
                        getClass().getSimpleName(), patientAge, limit, patient.getId());
            } else {
                fillDrugFromProtocol(drug, tp);
                log.info("{} accepted main drug {} (age={} ≤ {})",
                        getClass().getSimpleName(), drug.getDrugName(), patientAge, limit);
            }

        } else {
            // Альтернативный препарат запрещён при возрасте ниже лимита
            if (patientAge < limit) {
                String comment = String.format(
                        "System: avoid alternative drug %s — patient age (%d) < %d",
                        drug.getActiveMoiety(), patientAge, limit
                );
                recommendation.getComments().add(comment);
                rejectionReasons.add(String.format(
                        "[%s] Avoid triggered: alternative drug %s rejected (age=%d < limit=%d)",
                        getClass().getSimpleName(), drug.getActiveMoiety(), patientAge, limit
                ));

                log.warn("{} rejected alternative drug (age={} < {}) for patient {}",
                        getClass().getSimpleName(), patientAge, limit, patient.getId());
            } else {
                fillDrugFromProtocol(drug, tp);
                log.info("{} accepted alternative drug {} (age={} ≥ {})",
                        getClass().getSimpleName(), drug.getDrugName(), patientAge, limit);
            }
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }

    /**
     * Копирует данные препарата из TreatmentProtocol.
     */
    private void fillDrugFromProtocol(DrugRecommendation drug, TreatmentProtocol tp) {
        if (drug.getRole() == DrugRole.MAIN) {
            drug.setDrugName(tp.getFirstDrug());
            drug.setActiveMoiety(tp.getFirstDrugActiveMoiety());
            drug.setDosing(tp.getFirstDosingMg());
            drug.setInterval(tp.getFirstIntervalHrs());
            drug.setAgeAdjustment(tp.getFirstAgeAdjustments());
            drug.setWeightAdjustment(tp.getWeightKg());
            drug.setChildPugh(tp.getFirstChildPugh());
        } else {
            drug.setDrugName(null);
            drug.setActiveMoiety(tp.getSecondDrugActiveMoiety());
            drug.setDosing(tp.getSecondDosingMg());
            drug.setInterval(tp.getSecondIntervalHrs());
            drug.setAgeAdjustment(tp.getSecondAgeAdjustments());
            drug.setWeightAdjustment(tp.getSecondWeightKg());
            drug.setChildPugh(tp.getSecondChildPugh());
        }
    }
}