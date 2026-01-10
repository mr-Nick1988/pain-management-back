package pain_helper_back.treatment_protocol.service.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.service.TreatmentRuleApplier;
import pain_helper_back.treatment_protocol.utils.DrugUtils;
import pain_helper_back.treatment_protocol.utils.SafeValueUtils;
import pain_helper_back.treatment_protocol.utils.SanitizeUtils;

import java.util.List;
import java.util.stream.Stream;

/*
 * AVOID if sensitivity — правило Exceptions drugов при индивидуальной чувствительности (аллергии).
 * patient может иметь list чувствительных веществ (наExample, ["PARACETAMOL", "TRAMADOL"]).
 * if в Treatment Protocol указано "PARACETAMOL OR TRAMADOL",
 * и одно from веществ совпадает с patientскими, drugы from recommendation исключаются (avoid).
 */

@Component
@Slf4j
@Order(3)
public class SensitivityRuleApplier implements TreatmentRuleApplier {

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        //  Пропускаем, if drug уже отклонён or пустой
        if (!DrugUtils.hasInfo(drug)) {
            log.debug("Skipping {} — drug already rejected or empty", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // Extract data patient и protocolа
        Emr emr = patient.getEmr().getLast();
        List<String> sensitivities = emr.getSensitivities();
        String rule = tp.getAvoidIfSensitivity();

        //  Check входные data (ранний выход)
        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")
                || sensitivities == null || sensitivities.isEmpty()) {
            log.debug("No sensitivity data or rule NA for {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // Нормалfromуем data: приводим всё к верхнему регистру,
        // игнорирует регистр, разделяет по любым typeам разделителей: OR, запятая, слеш, точка с запятой, вертикальная черта, не боится лишних пробелов.
        List<String> ruleSensitivities = Stream.of(
                        rule.split("(?i)(?:(?<=\\s)OR(?=\\s)|AND|[,;/|\\\\]+)") // OR — only if окружён пробелами
                )
                .map(SanitizeUtils::normalize)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> normalizedPatientSens = sensitivities.stream()
                .flatMap(s -> Stream.of(s.split("\\s*,\\s*")))
                .map(SanitizeUtils::normalize)
                .filter(s -> !s.isEmpty())
                .toList();
        //  Check совпадения между protocolом и данными patient
        boolean hasMatch = ruleSensitivities.stream().anyMatch(normalizedPatientSens::contains);

        if (hasMatch) {
            //  withoutопасно Extract имена drugов (fromбегаем NPE)
            String mainDrugName = SafeValueUtils.safeValue(recommendation.getDrugs().getFirst());
            String altMoiety = SafeValueUtils.safeValue(recommendation.getDrugs().get(1));

            //  Format причину Exceptions recommendation (system reason)
            String reasonText = String.format(
                    "[%s] Avoid recommendation with drugs (%s and %s) triggered by sensitivity match. Rule=%s, Patient=%s",
                    getClass().getSimpleName(),
                    mainDrugName,
                    altMoiety,
                    ruleSensitivities,
                    normalizedPatientSens
            );

            rejectionReasons.add(reasonText);

            // Полностью очищаем drugы (avoid)
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);

            log.warn("Avoid triggered by sensitivity rule: patient={}, sensitivities={}, rule={}",
                    patient.getId(), normalizedPatientSens, ruleSensitivities);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
    }


}