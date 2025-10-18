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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@Component
@Slf4j
@Order(2)
public class ContraindicationsRuleApplier implements TreatmentRuleApplier {

    /**
     * Регулярное выражение для извлечения ICD-кодов (например: 571.201, V45.1103, E11.9 и т.п.)
     * - Может начинаться с буквы (A-Z)
     * - За ней идут минимум 3 цифры
     * - После этого — необязательная часть с точкой и буквами/цифрами
     */
    private static final Pattern ICD_PATTERN = Pattern.compile("[A-Z]?[0-9]{3}(?:\\.[0-9A-Z]+)?");

    @Override    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        // 1 Если у лекарства нет данных, выходим (чтобы не обрабатывать пустые строки)
        if (!DrugUtils.hasInfo(drug)) return;

        // 2 Получаем список диагнозов последнего EMR пациента
        Set<Diagnosis> patientDiagnoses = patient.getEmr().getLast().getDiagnoses();
        if (patientDiagnoses == null || patientDiagnoses.isEmpty()) return;

        // 3 Извлекаем поле contraindications из протокола (может быть NA или пустым)
        String raw = tp.getContraindications();
        if (raw == null || raw.trim().isEmpty() || raw.equalsIgnoreCase("NA"))
            return;

        // 4 Санитизируем строку противопоказаний:
        // убираем неразрывные пробелы, длинные тире, лишние пробелы и т.п.
        String contraindications = SanitizeUtils.clean(raw);

        // 5 Извлекаем ICD-коды из очищенной строки
        Set<String> contraindicationsSet = extractICDCodes(contraindications);

        // 🔍 Временные диагностические логи (для отладки)
        log.info("Patient ICDs: {}", patientDiagnoses.stream().map(Diagnosis::getIcdCode).toList());
        log.info("Contra raw: {}", raw);
        log.info("Contra parsed: {}", contraindicationsSet);

        // 6 Проверяем каждый диагноз пациента на совпадение с противопоказаниями
        for (Diagnosis diagnosis : patientDiagnoses) {
            // Очищаем и нормализуем код болезни пациента (TRIM + UPPERCASE)
            String code = normalizeCode(diagnosis.getIcdCode());
            if (code.isEmpty()) continue;

            // Извлекаем "основную часть" кода — до точки и 1 цифру после (571.201 → 571.2)
            String baseCode = getBaseCode(code);

            // Проверяем, совпадает ли базовая часть с любой из противопоказаний
            boolean matchFound = contraindicationsSet.stream()
                    .map(this::getBaseCode)    // у всех противопоказаний берём базу (571.201 → 571.2)
                    .map(this::normalizeCode)  // на всякий случай нормализуем
                    .anyMatch(c -> c.equals(baseCode)); // сравниваем напрямую

            // 7 Если нашли совпадение — очищаем все препараты и добавляем комментарий
            if (matchFound) {
                recommendation.getDrugs().forEach(DrugUtils::clearDrug);
                recommendation.getComments().add(
                        "System: avoid for contraindications (match by base ICD): " +
                                diagnosis.getDescription() + " (" + diagnosis.getIcdCode() + ")"
                );
                log.info("Avoid triggered by contraindications (base match): patient={}, code={}, desc={}",
                        patient.getId(), diagnosis.getIcdCode(), diagnosis.getDescription());
                return; //  сразу выходим, т.к. дальше проверять смысла нет
            }
        }
    }

    /**
     * Нормализует код (удаляет мусор и делает верхний регистр)
     * Например: " 571.201 " → "571.201"
     */
    private String normalizeCode(String code) {
        return code == null ? "" : SanitizeUtils.clean(code).toUpperCase();
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
            codes.add(normalizeCode(matcher.group()));
        }
        return codes;
    }

    /**
     * Возвращает "базовую часть" ICD-кода — до точки и одной цифры после.
     * Пример:
     *  - "571.201" → "571.2"
     *  - "E11.9" → "E11.9"
     *  - "571" → "571"
     */
    private String getBaseCode(String code) {
        if (code == null) return "";
        int dotIndex = code.indexOf('.');
        if (dotIndex != -1 && dotIndex + 2 <= code.length()) {
            return code.substring(0, Math.min(dotIndex + 2, code.length()));
        }
        return code; // если точки нет — возвращаем весь код
    }
}