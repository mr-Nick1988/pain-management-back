package pain_helper_back.common.patients.dto;

import lombok.Data;
/**
 * Универсальный DTO for действий Doctor / Anesthesiologist при requestе Recommendations.
 */

@Data
public class RecommendationWithVasDTO {
    private RecommendationDTO recommendation;
    private VasDTO vas;
    private String patientMrn;
}
