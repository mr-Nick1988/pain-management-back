package pain_helper_back.common.patients.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Универсальный DTO for действий Doctor / Anesthesiologist при requestе Recommendations.
 */

@Getter
@Setter
public class RecommendationWithVasDTO {
    private RecommendationDTO recommendation;
    private VasDTO vas;
    private String patientMrn;
}
