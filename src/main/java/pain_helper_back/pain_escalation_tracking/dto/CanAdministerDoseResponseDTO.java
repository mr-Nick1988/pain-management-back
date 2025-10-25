package pain_helper_back.pain_escalation_tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * DTO ответа на проверку возможности введения дозы.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanAdministerDoseResponseDTO {

    private Boolean canAdminister;
    private String patientMrn;
    private String message;
    private LocalDateTime lastDoseAt;
    private LocalDateTime nextDoseAllowedAt;
    private Integer hoursUntilNextDose;
}
