package pain_helper_back.common.patients.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
/**
 * Универсальный DTO для действий Doctor / Anesthesiologist при работе с Recommendation.
 */
@Data
public class RecommendationApprovalRejectionDTO {
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    @Size(max = 500, message = "Rejected reason must not exceed 500 characters")
    private String rejectedReason;
}
