package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.Diagnosis;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;
import pain_helper_back.treatment_protocol.utils.SafeValueUtils;
import pain_helper_back.treatment_protocol.utils.SanitizeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@Order(2)
public class ContraindicationsRuleApplier implements TreatmentRuleApplier {

    /**
     * Регулярное выражение для извлечения ICD-кодов (например: 571.201, V45.1103, E11.9 и т.п.)
     */
    private static final Pattern ICD_PATTERN =
            Pattern.compile("[A-Z]?[0-9]{2,3}(?:\\.[0-9A-Z]{1,4})?");

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===",
                getClass().getSimpleName(), patient.getId());

        //  Проверяем: есть ли смысл обрабатывать
        if (!DrugUtils.hasInfo(drug)
                || patient.getEmr().isEmpty()
                || patient.getEmr().getLast().getDiagnoses().isEmpty()
                || tp.getContraindications() == null
                || tp.getContraindications().isBlank()
                || tp.getContraindications().equalsIgnoreCase("NA")) {
            log.debug("No contraindication data or drug empty — skipping {}", getClass().getSimpleName());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        // Получаем диагнозы пациента и очищаем строку противопоказаний
        Set<Diagnosis> patientDiagnoses = patient.getEmr().getLast().getDiagnoses();
        String raw = tp.getContraindications();
        String contraindications = SanitizeUtils.clean(raw);

        //  Извлекаем ICD-коды из строки
        Set<String> contraindicationsSet = extractICDCodes(contraindications);

        log.info("Patient ICDs: {}", patientDiagnoses.stream().map(Diagnosis::getIcdCode).toList());
        log.info("Contra raw: {}", raw);
        log.info("Contra parsed: {}", contraindicationsSet);

        //  Безопасно извлекаем имена препаратов (избегаем NPE)
        String mainDrugName = SafeValueUtils.safeValue(recommendation.getDrugs().getFirst());
        String altMoiety = SafeValueUtils.safeValue(recommendation.getDrugs().get(1));

        //  Проверяем каждый диагноз пациента
        for (Diagnosis diagnosis : patientDiagnoses) {
            String code = normalizeCode(diagnosis.getIcdCode());
            if (code.isEmpty()) continue;

            boolean matchFound = contraindicationsSet.stream()
                    .map(this::normalizeCode)
                    .anyMatch(contra -> contra.startsWith(code) || code.startsWith(contra));

            if (matchFound) {
                // Добавляем только причину отказа (comments не трогаем, т.к. рекомендация будет исключена)
                String reasonText = String.format(
                        "[%s] Avoid recommendation with drugs (%s and %s) triggered by contraindications (ICD match): %s (%s)",
                        getClass().getSimpleName(),
                        mainDrugName,
                        altMoiety,
                        diagnosis.getDescription(),
                        diagnosis.getIcdCode()
                );

                rejectionReasons.add(reasonText);

                //  Обнуляем все препараты — рекомендация исключается полностью
                recommendation.getDrugs().forEach(DrugUtils::clearDrug);

                log.warn("Avoid triggered by contraindications: patient={}, code={}, desc={}",
                        patient.getId(), diagnosis.getIcdCode(), diagnosis.getDescription());
                return; // дальнейшие проверки не нужны
            }
        }

        log.info("=== [END] {} for Patient ID={} ===",
                getClass().getSimpleName(), patient.getId());
    }

    /**
     * Нормализует код диагноза (удаляет пробелы, делает верхний регистр).
     */
    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().replace("\u00A0", "").toUpperCase();
    }

    /**
     * Извлекает все ICD-коды из длинной строки (например: "571.201 OR 571.901").
     */
    private Set<String> extractICDCodes(String contraindications) {
        Set<String> codes = new HashSet<>();
        Matcher matcher = ICD_PATTERN.matcher(contraindications);
        while (matcher.find()) {
            codes.add(matcher.group().toUpperCase());
        }
        return codes;
    }
}