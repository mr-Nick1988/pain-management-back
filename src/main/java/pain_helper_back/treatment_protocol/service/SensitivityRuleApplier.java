package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.List;
import java.util.stream.Stream;

/*
AVOID if sensitivity — правило исключения препаратов при индивидуальной чувствительности (аллергии).
Пациент может иметь список чувствительных веществ (например, ["PARACETAMOL", "TRAMADOL"]).
Если в Treatment Protocol указано "PARACETAMOL OR TRAMADOL",
и одно из веществ совпадает с пациентскими, препараты из рекомендации исключаются (avoid).
*/

//@Component
@Slf4j
@Order(3)
public class SensitivityRuleApplier implements TreatmentRuleApplier {
    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        if (!DrugUtils.hasInfo(drug)) return;
        // 1 Получаем список чувствительных веществ пациента
        List<String> patientSensitivities = patient.getEmr().getLast().getSensitivities();
        if (patientSensitivities == null) return;
        // 2 Получаем список чувствительных веществ из правил протокола
        String rule = tp.getAvoidIfSensitivity();
        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) return;

        // 3 Разделяем строку протокола по "OR" и очищаем пробелы + нормализуем регистр
        List<String> ruleSensitivities = Stream.of(rule.split("OR"))
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        // 4 Приводим чувствительности пациента к верхнему регистру для надёжного сравнения
        List<String> normalizedPatientSens = patientSensitivities.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        boolean hasMatch = ruleSensitivities.stream().anyMatch(normalizedPatientSens::contains);
        if (hasMatch) {
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);
            recommendation.getComments().add(
                    "System: avoid due to sensitivity match. Rule=" + ruleSensitivities + ", patient=" + normalizedPatientSens
            );
            log.warn("Avoid triggered by sensitivity rule: patient={}, sensitivities={}, rule={}",
                    patient.getId(), normalizedPatientSens, ruleSensitivities);
        }
    }
}
