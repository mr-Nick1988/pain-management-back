package pain_helper_back.common.patients.dto;

import lombok.*;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    private Long id;
    private String mrn;
    private Integer painLevel;
    private RecommendationStatus status;
    private List<DrugRecommendationDTO> drugs;
    private String contraindications;
    private String rejectionComment;
    private String approvalComment;
    private String doctorId;
    private String anesthesiologistId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String reviewReason;
    private LocalDateTime reviewRequestedAt;
    private String reviewedBy;
    private String patientMrn;
    private Boolean generationFailed;
    private List<String> rejectionReasonsSummary;
}
