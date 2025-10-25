package pain_helper_back.pain_escalation_tracking.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * DTO статистики по эскалациям боли.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PainEscalationStatisticsDTO {

    private Long totalEscalations;
    private Long pendingEscalations;
    private Long resolvedEscalations;
    private Long criticalEscalations;
    private Long highEscalations;
    private Long mediumEscalations;
    private Double averageResolutionTimeHours;
    private Long escalationsLast24Hours;
    private Long escalationsLast7Days;
}
