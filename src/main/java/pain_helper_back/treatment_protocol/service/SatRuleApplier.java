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
SAT (oxygen saturation, SpO₂) — уровень насыщения крови кислородом.
Измеряется в процентах (%).
Нормальный диапазон: 95–100%.
Если сатурация <93%, рекомендуется избегать большинства препаратов (avoid).
*/

@Component
@Order(6)
@Slf4j
public class SatRuleApplier implements TreatmentRuleApplier {

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {

        if (!DrugUtils.hasInfo(drug)) return;

        Double patientSat = patient.getEmr().getLast().getSat();
        String rule = tp.getSat();

        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) return;
        if (patientSat == null) {
            throw new IllegalArgumentException("Patient SAT is null");
        }
        // Вспомогательная: извлекает первое целое число из строки (например из ">75 years - avoid" даст 75)
        Integer limit = PatternUtils.extractFirstInt(rule);
        if (limit == null) return;
        double limitDouble = limit.doubleValue();
        if (patientSat < limitDouble) {
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);
            recommendation.getComments().add(
                    "System: avoid for SAT < " + limit + "% (" + patientSat + "%)"
            );
            log.info("Avoid triggered by SAT rule: patient={}, value={}, rule={}", patient.getId(), patientSat, rule);
        }
    }


}
