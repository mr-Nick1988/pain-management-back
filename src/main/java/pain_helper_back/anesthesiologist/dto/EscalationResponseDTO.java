package pain_helper_back.anesthesiologist.dto;

import lombok.Data;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.time.LocalDateTime;

@Data
public class EscalationResponseDTO {
    // ========== BASIC INFO ========== //
    private Long id;
    private Long recommendationId;
    private EscalationStatus status;
    private EscalationPriority priority;

    // ========== ESCALATION INFO ========== //
    private String escalatedBy;
    private LocalDateTime escalatedAt;
    private String escalationReason;
    private String description;

    // ========== RESOLUTION INFO ========== //
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private String resolution;

    // ========== AUDIT ========== //
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

