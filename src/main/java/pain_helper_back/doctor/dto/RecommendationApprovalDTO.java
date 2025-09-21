package pain_helper_back.doctor.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecommendationApprovalDTO {
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    @Size(max = 500, message = "Rejected reason must not exceed 500 characters")
    private String rejectedReason;
}
