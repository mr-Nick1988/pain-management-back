package pain_helper_back.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStatsDTO {
    private Double averageProcessingTimeMs;
    private Long totalRecommendations;
    private Long approvedRecommendations;
    private Long rejectedRecommendations;
    private Long totalEscalations;
    private Long resolvedEscalations;
    private Double averageEscalationResolutionTimeMs;
}
