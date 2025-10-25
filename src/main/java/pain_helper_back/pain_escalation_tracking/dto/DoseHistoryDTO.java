package pain_helper_back.pain_escalation_tracking.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для истории доз пациента.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoseHistoryDTO {

    private Long id;
    private String drugName;
    private Double dosage;
    private String unit;
    private String route;
    private LocalDateTime administeredAt;
    private String administeredBy;
    private String notes;
    private LocalDateTime nextDoseAllowedAt;
}
