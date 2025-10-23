package pain_helper_back.pain_escalation_tracking.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.time.LocalDateTime;

/*
 * DTO информации об эскалации.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationInfoDTO {

    private Long escalationId;
    private String patientMrn;
    private EscalationPriority priority;
    private EscalationStatus status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
}
