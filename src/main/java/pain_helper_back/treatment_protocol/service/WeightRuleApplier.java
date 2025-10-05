package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(2)
@Slf4j
public class WeightRuleApplier implements TreatmentRuleApplier {
    // Паттерн для поиска последнего числа в строке (поддержка десятичных)
    private static final Pattern LAST_NUMBER_PATTERN = Pattern.compile(("(\\d+)(?=[^0-9]*$)"));


    /**
     * Применяет правило по весу. По протоколу корректировка применяется только для веса < 50kg.
     * Правило ожидается в формате вроде "<50kg - 8h" или "<50kg - 50mg".
     * Извлекает последнее число в строке и unit (mg/h) и применяет корректировку dose/interval.
     */
    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        // если препарат пустой или уже "отвергнут" ранее — не трогаем
        if (!DrugUtils.hasInfo(drug)) return;
        // Если веса нет или пациент >= 50kg — по протоколу ничего не делаем
        Double patientWeight = patient.getEmr().getLast().getWeight();
        if (patientWeight == null || patientWeight >= 50.0) return;

        String weightRule = (drug.getRole() == DrugRole.MAIN) ? tp.getWeightKg() : tp.getSecondWeightKg();
        if (weightRule == null || weightRule.trim().isEmpty() || weightRule.toUpperCase().contains("NA")) return;

        // ищем последнее число
        Matcher m = LAST_NUMBER_PATTERN.matcher(weightRule);
        if (!m.find()) return;
        String lastNumber = m.group(1);

        // определяем unit: берем хвост строки после найденного числа и оставляем только буквенные символы
        int afterIndex = m.end();
        String suffix = weightRule.substring(afterIndex).replaceAll("[^a-zA-Z%]", "").toLowerCase();

        if (suffix.contains("mg")) {
            // корректировка дозы
            String newDose = lastNumber + " mg";
            drug.setDosing(newDose);
            recommendation.getComments().add("System: " + "Dose adjusted for weight <50kg: set dosing to " + newDose
                    + " for " + (drug.getDrugName() != null ? drug.getDrugName() : drug.getActiveMoiety()));
        } else if (suffix.contains("h")) {
            // корректировка интервала
            String newInterval = lastNumber + "h";
            drug.setInterval(newInterval);
            recommendation.getComments().add("System: " + "Interval adjusted for weight <50kg: set interval to " + newInterval
                    + " for " + (drug.getDrugName() != null ? drug.getDrugName() : drug.getActiveMoiety()));
        } else {
            // неизвестный суффикс — просто логируем заметку, не ломая данные
            log.error("Unknown unit '{}' in weight rule '{}'", suffix, weightRule);
            throw new IllegalArgumentException("Invalid weight rule: " + weightRule);
        }
    }
}
