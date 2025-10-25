package pain_helper_back.treatment_protocol.service.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.*;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.service.TreatmentRuleApplier;
import pain_helper_back.treatment_protocol.service.exception.StopRecommendationGenerationException;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Order(0)
public class PainTrendRuleApplier implements TreatmentRuleApplier {

    private static final int MIN_HISTORY = 2;

    @Override
    public void apply(DrugRecommendation drug,
                      Recommendation recommendation,
                      TreatmentProtocol tp,
                      Patient patient,
                      List<String> rejectionReasons) {

        log.info("=== [START] {} for Patient ID={} ===", getClass().getSimpleName(), patient.getId());

        List<Integer> vasHistory = patient.getVas().stream()
                .map(Vas::getPainLevel)
                .toList();

        if (vasHistory.size() < MIN_HISTORY) {
            log.info("Not enough VAS history ({} entries). Continue processing.", vasHistory.size());
            return;
        }

        int last = vasHistory.getLast();
        int prev = vasHistory.get(vasHistory.size() - 2);
        int diff = last - prev;

        // ========== Сценарий 1: ухудшение на 1 ==========
        if (diff == 1) {
            addSystemComment(recommendation, vasHistory,
                    " Pain slightly worsened by 1 point. Continue treatment but monitor closely.");
            return; // продолжаем
        }

        // ========== Сценарий 2: ухудшение ≥2 ==========
        if (diff >= 2) {
            clearRecommendation(recommendation, rejectionReasons, vasHistory,
                    " Pain worsened by " + diff + " points. Recommendation generation stopped.");
            throw new StopRecommendationGenerationException(
                    "Pain worsened by " + diff + " points. Recommendation generation stopped."
            );
        }

        // ========== Сценарий 3: инверсия ==========
        boolean inversionDetected = isInversion(vasHistory);
        if (inversionDetected) {
            int amplitude = getLastInversionAmplitude(vasHistory);
            if (amplitude >= 2) {
                clearRecommendation(recommendation, rejectionReasons, vasHistory,
                        " Pain trend inversion detected with amplitude " + amplitude + ". Recommendation stopped.");
                return;
            } else {
                addSystemComment(recommendation, vasHistory,
                        " Pain trend shows mild inversion (amplitude " + amplitude + "). Monitor patient dynamics.");
                return;
            }
        }

        log.info("No regression or inversion detected (VAS={})", vasHistory);

    }

    // ========= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =========

    private void clearRecommendation(Recommendation recommendation,
                                     List<String> rejectionReasons,
                                     List<Integer> vasHistory,
                                     String message) {
        recommendation.getDrugs().forEach(DrugUtils::clearDrug);
        addSystemComment(recommendation, vasHistory, message);
        rejectionReasons.add(String.format("[%s] %s (VAS=%s)",
                getClass().getSimpleName(), message, vasHistory));
        log.warn(message);
    }

    private void addSystemComment(Recommendation recommendation, List<Integer> vasHistory, String message) {
        if (recommendation.getComments() == null)
            recommendation.setComments(new ArrayList<>());
        recommendation.getComments().add(String.format("[SYSTEM] %s. VAS history: %s", message, vasHistory));
    }

    // [7,6,7] или [5,6,5]
    private boolean isInversion(List<Integer> vasHistory) {
        if (vasHistory.size() < 3) return false;
        for (int i = 0; i < vasHistory.size() - 2; i++) {
            int a = vasHistory.get(i);
            int b = vasHistory.get(i + 1);
            int c = vasHistory.get(i + 2);
            if ((a > b && b < c) || (a < b && b > c)) return true;
        }
        return false;
    }

    // метод проверки критичности колебаний последних показателей боли (1- не критично, 2 и более - критично)
    private int getLastInversionAmplitude(List<Integer> vas) {
        if (vas.size() < 3) return 0;
        for (int i = vas.size() - 3; i >= 0; i--) {
            int a = vas.get(i);
            int b = vas.get(i + 1);
            int c = vas.get(i + 2);
            if ((a > b && b < c) || (a < b && b > c)) {
                // локальная амплитуда вокруг перелома
                int left = Math.abs(a - b);
                int right = Math.abs(c - b);
                return Math.max(left, right);
            }
        }
        return 0;
    }
}
