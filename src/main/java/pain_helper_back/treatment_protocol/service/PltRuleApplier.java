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

//@Component
@Order(4)
@Slf4j
public class PltRuleApplier implements TreatmentRuleApplier {

    /*
    PLT (platelet count) — количество тромбоцитов в крови.
    Измеряется в тысячах на микролитр крови:
     1K/µL = 1000 тромбоцитов на микролитр.
    Норма: 150K–450K/µL
    <100K/µL → риск кровотечения, нужно избегать некоторых препаратов
    */

    // Пример формата: "<100K/µL - avoid"
    private static final Pattern PLT_PATTERN = Pattern.compile("([<>]=?)\\s*(\\d+)\\s*[Kk]?/?µ?[lL]");

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        // если препарат пустой или уже "отвергнут" ранее — не трогаем
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        Double patientPlt = patient.getEmr().getLast().getPlt(); // например, 92 или 120
        String rule = tp.getPlt();                               // например, "<100K/µL - avoid"

        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) {
            log.debug("PLT rule empty or NA for protocol {}", tp.getId());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        if (patientPlt == null) {
            log.warn("Patient PLT is null — cannot apply {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        Matcher m = PLT_PATTERN.matcher(rule);
        if (!m.find()) {
            log.warn("PLT rule '{}' could not be parsed for protocol {}", rule, tp.getId());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        String operator = m.group(1); // "<" или ">"
        double limit = Double.parseDouble(m.group(2));

        boolean below = operator.contains("<") && patientPlt < limit;
        boolean above = operator.contains(">") && patientPlt > limit;

        // Если правило содержит "avoid" — обнуляем препарат и добавляем комментарий
        if ((below || above) && rule.toLowerCase().contains("avoid")) {

            if (recommendation.getComments() == null)
                recommendation.setComments(new ArrayList<>());

            String reasonText = String.format(
                    "System: avoid for PLT %s %.0fK/µL (patient value: %.0fK/µL)",
                    operator, limit, patientPlt
            );

            // Добавляем в комментарии и в общий список причин
            recommendation.getComments().add(reasonText);
            rejectionReasons.add(String.format(
                    "[%s] Avoid triggered by PLT rule '%s' (limit=%.0f, patient=%.0f)",
                    getClass().getSimpleName(),
                    rule,
                    limit,
                    patientPlt
            ));

            for (DrugRecommendation d : recommendation.getDrugs()) {
                DrugUtils.clearDrug(d);
            }

            log.warn("Avoid triggered by PLT rule: patient={}, value={}, rule={}", patient.getId(), patientPlt, rule);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }
}