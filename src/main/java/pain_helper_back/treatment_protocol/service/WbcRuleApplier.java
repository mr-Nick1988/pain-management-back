package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(6)
@Slf4j
public class WbcRuleApplier implements TreatmentRuleApplier {
/*
WBC (White Blood Cells) — лейкоциты, белые кровяные клетки.
Измеряются в 10³ клеток на микролитр крови (10³/µL).
Норма у взрослых: 4.0 – 10.0 ×10³/µL.
Если WBC < 4.0 — риск инфекций, препараты следует избегать (avoid).
*/

    private static final Pattern WBC_PATTERN =
            Pattern.compile("([<>]=?)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:\\(.*?\\))?\\s*-\\s*(avoid)", Pattern.CASE_INSENSITIVE);

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        if (!DrugUtils.hasInfo(drug)) return;

        Double patientWbc = patient.getEmr().getLast().getWbc();
        String rule = tp.getWbc(); // пример: "<4.0 (10e3/microliter) - avoid"

        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) return;
        if (patientWbc == null) {
            throw new IllegalArgumentException("Patient WBC is null");
        }

        Matcher m = WBC_PATTERN.matcher(rule);
        if (!m.find()) return;

        String operator = m.group(1); // "<"
        double limit = Double.parseDouble(m.group(2)); // 4.0

        boolean below = operator.contains("<") && patientWbc < limit;
        boolean above = operator.contains(">") && patientWbc > limit;

        if ((below || above) && rule.toLowerCase().contains("avoid")) {
            // если WBC ниже нормы — запрещаем оба препарата
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);
            recommendation.getComments().add(
                    "System: avoid for WBC < " + limit + "×10³/µL (" + patientWbc + "×10³/µL)"
            );
            log.info("Avoid triggered by WBC rule: patient={}, value={}, rule={}", patient.getId(), patientWbc, rule);
        }
    }
}

