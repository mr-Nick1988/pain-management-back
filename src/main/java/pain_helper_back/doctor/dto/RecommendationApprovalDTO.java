package pain_helper_back.doctor.dto;


import lombok.Data;

@Data
public class RecommendationApprovalDTO {
    private String comment;
    private String rejectedReason;
}
