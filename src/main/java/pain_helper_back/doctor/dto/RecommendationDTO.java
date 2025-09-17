package pain_helper_back.doctor.dto;

import lombok.Data;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;

@Data
public class RecommendationDTO {
    private Long id;
    private Long patientId;
    private PatientResponseDTO patient;
    private String description;
    private String justification;
    private RecommendationStatus status;
    private String rejectionReason;
    private String doctorComment;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
