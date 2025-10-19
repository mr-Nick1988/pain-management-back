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
     * [A-Z] допускаем любую опциональную в начале (характерно для ICD-9-CM);
     * [0-9]{3} — три цифры после неё;
     * (?:\\.[0-9])? — точка и ровно одна цифра после неё, опционально.
     */
    private static final Pattern ICD_PATTERN =
            Pattern.compile("[A-Z]?[0-9]{2,3}(?:\\.[0-9A-Z]{1,4})?");

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient, List<String> rejectionReasons) {
        // 1 Если у лекарства нет данных или у пациента нет диагнозов или в протоколе нет противопоказаний
        // ---> выходим (чтобы не обрабатывать пустые строки)
        if (!DrugUtils.hasInfo(drug)
                || patient.getEmr().isEmpty()
                || patient.getEmr().getLast().getDiagnoses().isEmpty()
                || tp.getContraindications() == null
                || tp.getContraindications().isBlank()
                || tp.getContraindications().equalsIgnoreCase("NA")) {
            return;
        }
        log.info("=== [START] {} for Patient ID={} ===",
                ContraindicationsRuleApplier.class.getSimpleName(), patient.getId());

        // 2 Получаем список диагнозов последнего EMR пациента
        Set<Diagnosis> patientDiagnoses = patient.getEmr().getLast().getDiagnoses();

        // 3 Извлекаем поле contraindications из протокола (может быть NA или пустым)
        String raw = tp.getContraindications();


        // 4 Санитизируем строку противопоказаний:
        // убираем неразрывные пробелы, длинные тире, лишние пробелы и т.п.
        String contraindications = SanitizeUtils.clean(raw);

        // 5 Извлекаем ICD-коды из очищенной строки
        Set<String> contraindicationsSet = extractICDCodes(contraindications);

        //  Временные диагностические логи (для отладки)
        log.info("Patient ICDs: {}", patientDiagnoses.stream().map(Diagnosis::getIcdCode).toList());
        log.info("Contra raw: {}", raw);
        log.info("Contra parsed: {}", contraindicationsSet);

        // 6 Проверяем каждый диагноз пациента на совпадение с противопоказаниями
        for (Diagnosis diagnosis : patientDiagnoses) {
            // Очищаем и нормализуем код болезни пациента (TRIM + UPPERCASE)
            String code = normalizeCode(diagnosis.getIcdCode());
            if (code.isEmpty()) continue;

            //  Проверяем, совпадает ли диагноз пациента с каким-либо кодом противопоказаний
            boolean matchFound = contraindicationsSet.stream()
                    // Приводим все коды противопоказаний к чистому и верхнему регистру,
                    // чтобы сравнение было нечувствительно к пробелам, разным символам и регистру.
                    .map(this::normalizeCode)
                    // Проверяем для каждого кода противопоказания (contra):
                    //  - если код противопоказания начинается с кода диагноза пациента
                    //    → пример: contra = "V45.1101", code = "V45.11" → true
                    //  - ИЛИ если код диагноза начинается с кода противопоказания
                    //    → пример: contra = "V45.11", code = "V45.1101" → true
                    // Таким образом, мы учитываем и полное совпадение, и иерархические подуровни ICD,
                    // когда, например, V45.11 и V45.1101 относятся к одной категории болезней.
                    .anyMatch(contra -> contra.startsWith(code) || code.startsWith(contra));

            // 7 Если нашли совпадение — очищаем все препараты и добавляем комментарий
            if (matchFound) {
                recommendation.getDrugs().forEach(DrugUtils::clearDrug);
                recommendation.getComments().add(
                        "System: avoid for contraindications (match by base ICD): " +
                                diagnosis.getDescription() + " (" + diagnosis.getIcdCode() + ")"
                );
                rejectionReasons.add(String.format(
                        "[%s] Avoid triggered by contraindications (ICD match): %s (%s)",
                        ContraindicationsRuleApplier.class.getSimpleName(),
                        diagnosis.getDescription(),
                        diagnosis.getIcdCode()
                ));                log.info("Avoid triggered by contraindications (base match): patient={}, code={}, desc={}",
                        patient.getId(), diagnosis.getIcdCode(), diagnosis.getDescription());
                return; //  сразу выходим, т.к. дальше проверять смысла нет
            }
        }
        log.info("=== [END] {} for Patient ID={} ===",
                ContraindicationsRuleApplier.class.getSimpleName(), patient.getId());
    }

    /**
     * Нормализует код диагноза пациента (удаляет пробелы и делает верхний регистр)
     * Например: " 571.201 " → "571.201"
     */
    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().replace("\u00A0", "").toUpperCase();
    }

    /**
     * Извлекает все ICD-коды из длинной строки (например: "571.201 OR 571.501 OR 571.901")
     * Возвращает множество строк (Set), чтобы избежать дубликатов.
     */
    private Set<String> extractICDCodes(String contraindications) {
        Set<String> codes = new HashSet<>();
        Matcher matcher = ICD_PATTERN.matcher(contraindications);
        while (matcher.find()) {
            // Добавляем каждый найденный код, предварительно очищая его
            codes.add(matcher.group().toUpperCase());
        }
        return codes;
    }
}