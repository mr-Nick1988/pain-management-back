package pain_helper_back.common.patients.dto;

import lombok.Data;
/**
 * Универсальный DTO для действий Doctor / Anesthesiologist при запросе Recommendations.
 */

@Data
public class RecommendationWithVasDTO {
    private RecommendationDTO recommendation;
    private VasDTO vas;
}
