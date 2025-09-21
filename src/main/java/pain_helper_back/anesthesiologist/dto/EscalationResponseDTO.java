package pain_helper_back.anesthesiologist.dto;

import lombok.Data;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.time.LocalDateTime;

@Data
public class EscalationResponseDTO {
    private Long id;
    private String patientId;
    private String patientName;
    private String doctorName;
    private String rejectedReason;
    private EscalationPriority escalationPriority;
    private EscalationStatus escalationStatus;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
