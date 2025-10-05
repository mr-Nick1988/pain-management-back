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
import java.util.regex.Pattern;


/*
Sodium (Na⁺) — концентрация натрия в крови.
Измеряется в миллиэквивалентах на литр (mEq/L).
Нормальный диапазон: 135–145 mEq/L.
Если уровень Na⁺ <130 mEq/L — это гипонатриемия (пониженный натрий),
при которой рекомендуется избегать большинства препаратов (avoid).
*/

@Component
@Order(8)
@Slf4j
public class SodiumRuleApplier implements TreatmentRuleApplier{

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        if (!DrugUtils.hasInfo(drug)) return;

        Double patientSodium = patient.getEmr().getLast().getSodium();
        String rule = tp.getSodium();
        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) return;
        if (patientSodium == null) {
            throw new IllegalArgumentException("Patient sodium is null");
        }
        Integer limit = PatternUtils.extractFirstInt(rule);
        double limitDouble = limit.doubleValue();
        if (patientSodium<limitDouble){
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);
            recommendation.getComments().add(
                    "System: avoid for sodium < " + limit + " mmol/L (" + patientSodium + " mmol/L)"
            );
            log.info("Avoid triggered by sodium rule: patient={}, value={}, rule={}", patient.getId(), patientSodium, rule);
        }

    }


}
