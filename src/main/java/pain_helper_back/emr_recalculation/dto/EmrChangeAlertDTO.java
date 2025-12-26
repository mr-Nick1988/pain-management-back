package pain_helper_back.emr_recalculation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmrChangeAlertDTO {
    
    private String patientMrn;
    private String patientName;
    
    private String parameterName;
    private String oldValue;
    private String newValue;
    private String changeDescription;
    
    private AlertSeverity severity;
    private String recommendation;
    
    private LocalDateTime detectedAt;
    private LocalDateTime lastEmrUpdate;
    
    private boolean requiresRecommendationReview;
    private Long affectedRecommendationId;
    
    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
