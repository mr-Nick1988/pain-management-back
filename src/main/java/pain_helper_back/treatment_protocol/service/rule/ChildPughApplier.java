package pain_helper_back.treatment_protocol.service.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.service.CorrectionAggregator;
import pain_helper_back.treatment_protocol.service.TreatmentRuleApplier;
import pain_helper_back.treatment_protocol.utils.DrugUtils;
import pain_helper_back.treatment_protocol.utils.SafeValueUtils;
import pain_helper_back.treatment_protocol.utils.SanitizeUtils;

import java.util.*;
import java.util.regex.*;

@Component
@Order(8)
@Slf4j
public class ChildPughApplier implements TreatmentRuleApplier {

    private static final Pattern CHILD_PUGH_PATTERN = Pattern.compile("([ABC])\\s*-\\s*([^ABC]+)(?=$|[ABC])");

    private final CorrectionAggregator correctionAggregator;

    public ChildPughApplier(CorrectionAggregator correctionAggregator) {
        this.correctionAggregator = correctionAggregator;
    }

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        if (!DrugUtils.hasInfo(drug)) return;

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        String patientChildPugh = patient.getEmr().getLast().getChildPughScore();
        String childPughRule = (drug.getRole() == DrugRole.MAIN)
                ? tp.getFirstChildPugh()
                : tp.getSecondChildPugh();

        if (childPughRule == null || childPughRule.isBlank() || childPughRule.equalsIgnoreCase("NA")) {
            log.debug("ChildPugh rule empty or NA for protocol {}", tp.getId());
            return;
        }

        childPughRule = SanitizeUtils.clean(childPughRule);

        Matcher m = CHILD_PUGH_PATTERN.matcher(childPughRule);
        Map<String, String> liverRules = new HashMap<>();
        while (m.find()) {
            liverRules.put(m.group(1), m.group(2).trim());
        }

        String patientRule = liverRules.get(patientChildPugh);
        if (patientRule == null || patientRule.isBlank()) {
            log.debug("No ChildPugh rule found for category {} in protocol {}", patientChildPugh, tp.getId());
            return;
        }

        applyRuleToDrug(drug, recommendation, patientRule.toLowerCase(), patientChildPugh, rejectionReasons);

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }

    private void applyRuleToDrug(DrugRecommendation drug,
                                 Recommendation recommendation,
                                 String patientRule,
                                 String patientChildPugh,
                                 List<String> rejectionReasons) {

        String drugName = SafeValueUtils.safeValue(drug);

        // 1. avoid → очищаем препарат
        if (patientRule.contains("avoid")) {
            recommendation.getComments().add("System: avoid " + drugName + " for patient with Child-Pugh = " + patientChildPugh);
            rejectionReasons.add(String.format(
                    "[%s] Avoid recommendation with drug %s for Child-Pugh category %s (rule='%s')",
                    getClass().getSimpleName(),
                    drugName,
                    patientChildPugh,
                    patientRule
            ));
            DrugUtils.clearDrug(drug);
            return;
        }

        // 2. Корректировка дозировки
        Matcher mg = Pattern.compile("(\\d+)\\s*mg").matcher(patientRule);
        if (mg.find()) {
            String oldDosing = drug.getDosing();
            String newDosing = mg.group(1) + " mg";
            drug.setDosing(newDosing);

            //  записываем в CorrectionAggregator
            try {
                int numericDose = Integer.parseInt(mg.group(1));
                correctionAggregator.addDoseCorrection(drug, numericDose);
            } catch (NumberFormatException ignored) {}

            recommendation.getComments().add(
                    String.format("System: corrected dosing of %s from %s to %s for Child-Pugh=%s",
                            drugName, oldDosing, newDosing, patientChildPugh)
            );
            log.info("Dose adjusted: {} ({} → {}) [ChildPugh {}]",
                    drug.getDrugName(), oldDosing, newDosing, patientChildPugh);
        }

        // 3. Корректировка интервала
        Matcher h = Pattern.compile("(\\d+)\\s*h").matcher(patientRule);
        if (h.find()) {
            String oldInterval = drug.getInterval();
            String newInterval = h.group(1) + "h";
            drug.setInterval(newInterval);

            //  записываем в CorrectionAggregator
            try {
                int numericInterval = Integer.parseInt(h.group(1));
                correctionAggregator.addIntervalCorrection(drug, numericInterval);
            } catch (NumberFormatException ignored) {}

            recommendation.getComments().add(
                    String.format("System: corrected interval of %s from %s to %s for Child-Pugh=%s",
                            drugName, oldInterval, newInterval, patientChildPugh)
            );
            log.info("Interval adjusted: {} ({} → {}) [ChildPugh {}]",
                    drug.getDrugName(), oldInterval, newInterval, patientChildPugh);
        }

        log.info("Applied ChildPugh rule '{}' for {} category (protocol {})",
                patientRule, patientChildPugh, drug.getId());
    }
}