package pain_helper_back.treatment_protocol.service.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.service.TreatmentRuleApplier;
import pain_helper_back.treatment_protocol.utils.DrugUtils;
import pain_helper_back.treatment_protocol.utils.SafeValueUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * WBC (White Blood Cells) — лейкоциты, белые кровяные клетки.
 * fromмеряются в 10³ клеток на микролитр крови (10³/µL).
 * Норма у взрослых: 4.0 – 10.0 ×10³/µL.
 * if WBC < 4.0 or ≥ 10.0 — риск инфекций / воспалений, drugы следует fromбегать (avoid).
 */

@Component
@Order(5)
@Slf4j
public class WbcRuleApplier implements TreatmentRuleApplier {

    private static final Pattern WBC_PATTERN =
            Pattern.compile("([<>]=?)\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        // 1 Пропускаем, if drug уже отклонён or пустой
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 2 Extract data
        String rule = tp.getWbc(); // наExample, "<4.0 - avoid"
        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) {
            log.debug("WBC rule empty or NA for protocol {}", tp.getId());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        Double patientWbc = patient.getEmr().getLast().getWbc(); // наExample, 3.5
        if (patientWbc == null) {
            log.warn("Patient WBC is null — cannot apply {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 3 Extract числовой порог и оператор from правила
        Matcher m = WBC_PATTERN.matcher(rule);
        if (!m.find()) {
            log.warn("Could not extract numeric limit from WBC rule '{}'", rule);
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        String operator = m.group(1);
        double limit = Double.parseDouble(m.group(2));

        boolean below = operator.contains("<") && patientWbc < limit;
        boolean above = operator.contains(">") && patientWbc > limit;

        // 4 withoutопасно Extract имена drugов
        String mainDrugName = SafeValueUtils.safeValue(recommendation.getDrugs().getFirst());
        String altMoiety = SafeValueUtils.safeValue(recommendation.getDrugs().get(1));

        log.warn("[WBC CHECK] value={} | belowLimit={} | aboveLimit={} | triggered={}",
                patientWbc, below, above, (below || above));

        // 5 Check условие avoid и отклоняем
        if ((below || above) && rule.toLowerCase().contains("avoid")) {

            String reasonText = String.format(
                    "[%s] Avoid recommendation with drugs (%s and %s) triggered by WBC rule '%s' " +
                            "(limit=%.1f×10³/µL, patient=%.1f×10³/µL)",
                    getClass().getSimpleName(),
                    mainDrugName,
                    altMoiety,
                    rule,
                    limit,
                    patientWbc
            );

            rejectionReasons.add(reasonText);

            // Обнуляем drugы, чтобы recommendation исключалась
            for (DrugRecommendation d : recommendation.getDrugs()) {
                DrugUtils.clearDrug(d);
            }

            log.warn("Avoid triggered by WBC rule: patient={}, value={}, rule={}",
                    patient.getId(), patientWbc, rule);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }
}