package pain_helper_back.anesthesiologist.dto;


import lombok.Data;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;

@Data
public class RecommendationWorkFlowDTO {
    // ========== BASIC INFO ========== //
    private Long id;
    private RecommendationStatus status;
    private Integer regimenHierarchy;// иерархия режима лечения для отслеживания зменений в обезболивающих

    // ========== DOCTOR LEVEL ========== //
    private String doctorId;
    private LocalDateTime doctorActionAt;
    private String doctorComment;

    // ========== ESCALATION ========== //
    private EscalationResponseDTO escalation;

    // ========== ANESTHESIOLOGIST LEVEL ========== //
    private String anesthesiologistId;
    private LocalDateTime anesthesiologistActionAt;
    private String anesthesiologistComment;

    // ========== FINAL APPROVAL ========== //
    private String finalApprovedBy;
    private LocalDateTime finalApprovedAt;

    // ========== AUDIT ========== //
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
