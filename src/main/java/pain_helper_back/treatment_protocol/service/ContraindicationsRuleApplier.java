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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@Order(2)
public class ContraindicationsRuleApplier implements TreatmentRuleApplier {

    // Регулярка: извлекает коды вроде 571.201, 287.4901, V45.1103 и т.п.
    private static final Pattern ICD_PATTERN = Pattern.compile("[A-Z]?[0-9]{3}(?:\\.[0-9A-Z]+)?");

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        if (!DrugUtils.hasInfo(drug)) return;

        Set<Diagnosis> patientDiagnoses = patient.getEmr().getLast().getDiagnoses();
        if (patientDiagnoses == null || patientDiagnoses.isEmpty()) return;

        String contraindications = tp.getContraindications();
        if (contraindications == null || contraindications.trim().isEmpty() || contraindications.equalsIgnoreCase("NA"))
            return;

        Set<String> contraindicationsSet = extractICDCodes(contraindications);

        // 🔍 Проверяем каждую болезнь пациента
        for (Diagnosis diagnosis : patientDiagnoses) {
            String code = diagnosis.getIcdCode();
            if (code == null) continue;

            // Извлекаем "основную часть" ICD — до точки и одну цифру после
            String baseCode = getBaseCode(code);

            // Проверяем совпадение с любым противопоказанием
            boolean matchFound = contraindicationsSet.stream()
                    .map(this::getBaseCode)
                    .anyMatch(c -> c.equals(baseCode));

            if (matchFound) {
                recommendation.getDrugs().forEach(DrugUtils::clearDrug);
                recommendation.getComments().add(
                        "System: avoid for contraindications (match by base ICD): " +
                                diagnosis.getDescription() + " (" + diagnosis.getIcdCode() + ")"
                );
                log.info("Avoid triggered by contraindications (base match): patient={}, code={}, desc={}",
                        patient.getId(), diagnosis.getIcdCode(), diagnosis.getDescription());
                return;
            }
        }
    }

    /** Извлекает все ICD-коды из длинной строки с 'OR' и т.п. */
    private Set<String> extractICDCodes(String contraindications) {
        Set<String> codes = new HashSet<>();
        Matcher matcher = ICD_PATTERN.matcher(contraindications);
        while (matcher.find()) {
            codes.add(matcher.group());
        }
        return codes;
    }

    /** Возвращает "основу" кода: до точки и одну цифру после (если есть) */
    private String getBaseCode(String code) {
        if (code == null) return "";
        // Убираем всё после первой цифры после точки
        int dotIndex = code.indexOf('.');
        if (dotIndex != -1 && dotIndex + 2 <= code.length()) {
            return code.substring(0, Math.min(dotIndex + 2, code.length()));
        }
        // Если точки нет — возвращаем сам код
        return code;
    }
}