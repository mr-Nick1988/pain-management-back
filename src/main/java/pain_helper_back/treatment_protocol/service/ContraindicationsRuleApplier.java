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
@Order(10) // 👉 этот фильтр выполняется последним в цепочке
public class ContraindicationsRuleApplier implements TreatmentRuleApplier {

    // 👉 регулярка для извлечения ICD-кодов, например: 571.201, E11, I10.9, V45.1103 и т.д.
    private static final Pattern ICD_PATTERN = Pattern.compile("[A-Z]?[0-9]{3}(?:\\.[0-9A-Z]+)?");

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        // 1 Проверяем, что препарат вообще существует и не был ранее "очищен"
        if (!DrugUtils.hasInfo(drug)) return;

        // 2 Получаем диагнозы пациента (Set<String>) из последней EMR-записи
        Set<Diagnosis> patientDiagnoses = patient.getEmr().getLast().getDiagnoses();
        if (patientDiagnoses == null || patientDiagnoses.isEmpty()) return;

        // 3 Извлекаем противопоказания из строки протокола (может быть длинная строка)
        String contraindications = tp.getContraindications();
        if (contraindications == null || contraindications.trim().isEmpty() || contraindications.equalsIgnoreCase("NA"))
            return;

        // 4 Парсим все коды из строки противопоказаний в Set<String>
        Set<String> contraindicationsSet = extractICDCodes(contraindications);

        // 5 Проверяем, есть ли пересечение диагнозов пациента и противопоказаний препарата
        for (Diagnosis diagnosis : patientDiagnoses) {
            if (contraindicationsSet.contains(diagnosis.getIcdCode())) {
                // 6 Если совпадение найдено — исключаем препарат
                recommendation.getDrugs().forEach(DrugUtils::clearDrug);
                // 7 Добавляем комментарий для аудита
                recommendation.getComments().add("System: avoid for contraindications: " + diagnosis.getDescription() + " (" + diagnosis.getIcdCode() + ")");
                // 8 Логируем для отладки
                log.info("Avoid triggered by contraindications: patient={}, diagnosisCode={}, diagnosisDescription={}", patient.getId(), diagnosis.getIcdCode(), diagnosis.getDescription());
                return;
            }
        }
    }

    //  Метод для извлечения всех ICD-кодов из строки
    private Set<String> extractICDCodes(String contraindications) {
        Set<String> codes = new HashSet<>();
        Matcher matcher = ICD_PATTERN.matcher(contraindications);
        while (matcher.find()) {
            codes.add(matcher.group());
        }
        return codes;
    }
}