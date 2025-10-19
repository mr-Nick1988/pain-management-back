package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
WBC (White Blood Cells) — лейкоциты, белые кровяные клетки.
Измеряются в 10³ клеток на микролитр крови (10³/µL).
Норма у взрослых: 4.0 – 10.0 ×10³/µL.
Если WBC < 4.0 — риск инфекций, препараты следует избегать (avoid).
*/

//@Component
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

        // 1 Пропускаем, если препарат уже был отклонён или пустой
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 2 Извлекаем данные
        Double patientWbc = patient.getEmr().getLast().getWbc();  // например, 3.5
        String rule = tp.getWbc();                               // например, "<4.0 - avoid"

        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) {
            log.debug("WBC rule empty or NA for protocol {}", tp.getId());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        if (patientWbc == null) {
            log.warn("Patient WBC is null — cannot apply {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // 3 Извлекаем числовой порог и оператор из правила
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

        // 4 Проверяем условие avoid
        if ((below || above) && rule.toLowerCase().contains("avoid")) {

            if (recommendation.getComments() == null)
                recommendation.setComments(new ArrayList<>());

            String comment = String.format(
                    "System: avoid for WBC %s %.1f×10³/µL (patient %.1f×10³/µL)",
                    operator,
                    limit,
                    patientWbc
            );

            recommendation.getComments().add(comment);
            rejectionReasons.add(String.format(
                    "[%s] Avoid triggered by WBC rule '%s' (limit=%.1f, patient=%.1f)",
                    getClass().getSimpleName(),
                    rule,
                    limit,
                    patientWbc
            ));

            // Очищаем препараты
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);

            log.warn("Avoid triggered by WBC rule: patient={}, value={}, rule={}",
                    patient.getId(), patientWbc, rule);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }
}