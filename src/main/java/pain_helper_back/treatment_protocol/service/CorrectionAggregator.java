package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;

import java.util.*;

/**
 * Агрегатор корректировок доз и интервалов, проходящий через все RuleApplier.
 * Сохраняет все изменения от фильтров и в конце вычисляет:
 *  - минимальную дозу
 *  - максимальный интервал
 */
@Slf4j
@Component
public class CorrectionAggregator {

    private final Map<String, List<Integer>> doseCorrections = new HashMap<>();
    private final Map<String, List<Integer>> intervalCorrections = new HashMap<>();

    /* Добавляем корректировку дозировки от любого фильтра */
    public void addDoseCorrection(DrugRecommendation drug, int dose) {
        String key = drug.getActiveMoiety();
        doseCorrections.computeIfAbsent(key, k -> new ArrayList<>()).add(dose);
        log.debug("Added dose correction for drug {} -> {}", key, dose);
    }

    /* Добавляем корректировку интервала от любого фильтра */
    public void addIntervalCorrection(DrugRecommendation drug, int interval) {
        String key = drug.getActiveMoiety();
        intervalCorrections.computeIfAbsent(key, k -> new ArrayList<>()).add(interval);
        log.debug("Added interval correction for drug {} -> {}", key, interval);
    }

    /* Применяем итоговые корректировки: минимальная доза, максимальный интервал */
    public void applyFinalAdjustments(DrugRecommendation drug) {
        if (drug.getActiveMoiety() == null || drug.getActiveMoiety().isBlank()) {
            log.debug("Skipping final adjustments for cleared drug (no active moiety)");
            return;
        }

        String key = drug.getActiveMoiety();

        List<Integer> doses = doseCorrections.get(key);
        if (doses != null && !doses.isEmpty()) {
            int finalDose = Collections.min(doses);
            drug.setDosing(finalDose + " mg");
            log.info("Final dose for drug {} set to {} mg", key, finalDose);
        }

        List<Integer> intervals = intervalCorrections.get(key);
        if (intervals != null && !intervals.isEmpty()) {
            int finalInterval = Collections.max(intervals);
            drug.setInterval(finalInterval + "h");
            log.info("Final interval for drug {} set to {}h", key, finalInterval);
        }
    }

    /* Очищаем агрегатор после завершения генерации рекомендации */
    public void clear() {
        doseCorrections.clear();
        intervalCorrections.clear();
        log.debug("CorrectionAggregator cleared");
    }
}
