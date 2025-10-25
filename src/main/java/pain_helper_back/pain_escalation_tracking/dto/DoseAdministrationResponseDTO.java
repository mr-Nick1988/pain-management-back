package pain_helper_back.pain_escalation_tracking.dto;

/*
 * DTO-ответ после регистрации введённой дозы
 *
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoseAdministrationResponseDTO {

    private Boolean success;
    private String message;
    private Long doseId;
    private LocalDateTime administeredAt;
    private LocalDateTime nextDoseAllowedAt;
}
