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

import java.util.ArrayList;
import java.util.List;

/**
 * PainTrendRuleApplier
 * Анализирует динамику боли (VAS) за последние визиты пациента.
 * Если боль регрессирует (ухудшается) или ведёт себя нестабильно (скачет вверх-вниз),
 * система не генерирует новую рекомендацию и очищает список препаратов.
 */
//@Component
@Slf4j
@Order(0) // выполняется самым первым, до AgeRuleApplier
public class PainTrendRuleApplier implements TreatmentRuleApplier {

    private static final int MIN_HISTORY = 3; // минимальное количество записей для анализа

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        // Извлекаем историю болевых шкал пациента
        List<Integer> vasHistory = patient.getVas().stream()
                .map(Vas::getPainLevel)
                .toList();

        // Если данных слишком мало, фильтр не применяется
        if (vasHistory.size() < MIN_HISTORY) {
            log.info("Not enough VAS history to apply {} (size={})", getClass().getSimpleName(), vasHistory.size());
            log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
            return;
        }

        boolean regressing = isRegressing(vasHistory);
        boolean unstable = isUnstable(vasHistory);

        if (regressing || unstable) {

            // очищаем препараты
            recommendation.getDrugs().forEach(DrugUtils::clearDrug);

            if (recommendation.getComments() == null)
                recommendation.setComments(new ArrayList<>());

            String reason = regressing
                    ? "pain trend worsening (VAS increasing)"
                    : "pain trend unstable (VAS fluctuating)";

            // добавляем системные комментарии
            recommendation.getComments().add(String.format(
                    "System: recommendation stopped due to %s. VAS history=%s",
                    reason,
                    vasHistory
            ));

            rejectionReasons.add(String.format(
                    "[%s] Recommendation stopped due to %s. VAS history=%s",
                    getClass().getSimpleName(),
                    reason,
                    vasHistory
            ));

            log.warn("{} triggered for patient {} (VAS history={})", getClass().getSimpleName(), patient.getMrn(), vasHistory);
        }

        log.info("=== [END] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());
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

            if ((a > b && b < c) || (a < b && b > c)) {
                return true;
            }
        }
        return false;
    }
}