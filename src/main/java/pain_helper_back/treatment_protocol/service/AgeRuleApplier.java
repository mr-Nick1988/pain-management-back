package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.PatternUtils;

@Slf4j
@Component
@Order(1)
public class AgeRuleApplier implements TreatmentRuleApplier {


    /**
     * Применяет возрастное правило к конкретной прописке препарата.
     * Если препарат разрешён — заполняет данные из TP. Иначе добавляет заметку в recommendation.comments.
     */
    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        int patientAge = patient.getAge();
        log.info("=== [START] {} for Patient ID={} Age={} ===",
                AgeRuleApplier.class.getSimpleName(), patient.getId(), patientAge);
        String ageAdjustment = (drug.getRole() == DrugRole.MAIN) ? tp.getFirstAgeAdjustments() : tp.getSecondAgeAdjustments();

        // Если правило пустое или "NA" — возраст не ограничивает
        if (ageAdjustment == null || ageAdjustment.trim().isEmpty() || ageAdjustment.toUpperCase().contains("NA")) {
            // разрешаем препарат — заполняем поля
            fillDrugFromProtocol(drug, tp);
            return;
        }
        // Вспомогательная: извлекает первое целое число из строки (например из ">75 years - avoid" даст 75)
        Integer limit = PatternUtils.extractFirstInt(ageAdjustment); // берём первое число (например 75 или 18)
        if (limit == null) {
            // не смогли распарсить число
            throw new IllegalArgumentException("Invalid protocol config: " + ageAdjustment);
        }
        // Логика  протокола:
        // - Для первого препарата: если patientAge > limit => avoid
        // - Для второго препарата: если patientAge < limit => avoid
        if (drug.getRole() == DrugRole.MAIN) {
            if (patientAge <= limit) {
                fillDrugFromProtocol(drug, tp);
                log.info("Filter {} accepted the main Drug {} ", AgeRuleApplier.class.getSimpleName(), drug.getDrugName());
            } else {
                log.info("Filter {} rejected the main Drug {} ", AgeRuleApplier.class.getSimpleName(), drug.getDrugName());
                recommendation.getComments().add("System: " + "First drug " + drug.getActiveMoiety() + " avoid: patient age (" + patientAge + ") > " + limit);
            }
        } else { // DrugRole.ALTERNATIVE
            if (patientAge >= limit) {
                fillDrugFromProtocol(drug, tp);
                log.info("Filter {} accepted the alternative Drug {} ", AgeRuleApplier.class.getSimpleName(), drug.getDrugName());

            } else {
                log.info("Filter {} rejected the alternative Drug {} ", AgeRuleApplier.class.getSimpleName(), drug.getDrugName());
                recommendation.getComments().add("System: " + "Second drug avoid: " + drug.getActiveMoiety() + " patient age (" + patientAge + ") < " + limit);
            }
        }
        log.info("=== [END] {} for Patient ID={} ===",
                AgeRuleApplier.class.getSimpleName(), patient.getId());
    }

    /**
     * Заполняет поля DrugRecommendation из строки протокола (по индексу лекарства).
     * Не выполняет проверок — просто копирует данные.
     */
    private void fillDrugFromProtocol(DrugRecommendation drug, TreatmentProtocol tp) {
        if (drug.getRole() == DrugRole.MAIN) {
            drug.setDrugName(tp.getFirstDrug());
            drug.setActiveMoiety(tp.getFirstDrugActiveMoiety());
            drug.setDosing(tp.getFirstDosingMg());
            drug.setInterval(tp.getFirstIntervalHrs());
            drug.setAgeAdjustment(tp.getFirstAgeAdjustments());
            drug.setWeightAdjustment(tp.getWeightKg());
            drug.setChildPugh(tp.getFirstChildPugh());
            // route  установили ранее
        } else {
            // второе "вещество" — имя препарата может отсутствовать, заполняем доступное
            drug.setDrugName(null); // если у тебя нет имени для second drug
            drug.setActiveMoiety(tp.getSecondDrugActiveMoiety());
            drug.setDosing(tp.getSecondDosingMg());
            drug.setInterval(tp.getSecondIntervalHrs());
            drug.setAgeAdjustment(tp.getSecondAgeAdjustments());
            drug.setWeightAdjustment(tp.getSecondWeightKg());
            drug.setChildPugh(tp.getSecondChildPugh());
        }
    }


}
// Тест пройден для 3 пациентов с разными возрастами при уровне боли для всех = 4
/*№ пациента	Возраст	Ожидаемое поведение	Реальный результат	Комментарий
1	85 лет	Tramal 🚫 (avoid), Paracetamol ✅	Tramal rejected, Paracetamol accepted	System: First drug avoid: patient age (85) > 75
2	45 лет	Tramal ✅, Paracetamol ✅	Оба accepted	Без комментариев — корректно
3	12 лет	Tramal ✅, Paracetamol 🚫 (avoid)	Paracetamol rejected	System: Second drug avoid: patient age (12) < 18*/
