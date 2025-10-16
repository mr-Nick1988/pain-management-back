package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.List;

/**
 * 🔍 PainTrendRuleApplier
 *
 * Анализирует динамику боли (VAS) за последние визиты пациента.
 * Если боль регрессирует (ухудшается) или ведёт себя нестабильно (скачет вверх-вниз),
 * система не генерирует новую рекомендацию и очищает список препаратов.
 */
@Component
@Slf4j
@Order(1)
public class PainTrendRuleApplier implements TreatmentRuleApplier {

    private static final int MIN_HISTORY = 3; // минимальное количество записей для анализа

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation,
                      TreatmentProtocol tp, Patient patient) {

        // Извлекаем историю болевых шкал пациента
        List<Integer> vasHistory = patient.getVas().stream()
                .map(Vas::getPainLevel)
                .toList();

        if (vasHistory.size() < MIN_HISTORY) return;

        if (isRegressing(vasHistory) || isUnstable(vasHistory)) {
            // очищаем препараты
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);

            // добавляем системный комментарий
            recommendation.getComments().add(
                    "[SYSTEM] Recommendation stopped: pain trend worsening or unstable. VAS history=" + vasHistory
            );

            log.warn("PainTrendRuleApplier triggered for patient {}. VAS history = {}", patient.getMrn(), vasHistory);
        }
    }

    /**
     * Регрессия — боль увеличивается по сравнению с предыдущим измерением.
     * Например: [5, 6, 7] или [4, 5, 5, 6].
     */
    private boolean isRegressing(List<Integer> vasHistory) {
        for (int i = 0; i < vasHistory.size() - 1; i++) {
            if (vasHistory.get(i) < vasHistory.get(i + 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Нестабильность — скачки боли вверх и вниз без устойчивого тренда.
     * Например: [7, 6, 7] или [5, 7, 6, 7].
     */
    private boolean isUnstable(List<Integer> vasHistory) {
        if (vasHistory.size() < 3) return false;

        for (int i = 0; i < vasHistory.size() - 2; i++) {
            int a = vasHistory.get(i);
            int b = vasHistory.get(i + 1);
            int c = vasHistory.get(i + 2);

            // если сначала боль падает, потом снова растёт (или наоборот)
            if ((a > b && b < c) || (a < b && b > c)) {
                return true;
            }
        }
        return false;
    }
}