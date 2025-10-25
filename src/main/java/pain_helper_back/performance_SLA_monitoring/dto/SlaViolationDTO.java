package pain_helper_back.performance_SLA_monitoring.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для нарушения SLA
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaViolationDTO {
    private String operationName;
    private Long executionTimeMs;
    private Long slaThresholdMs;
    private Long excessTimeMs;              // Насколько превышен порог
    private Double slaPercentage;
    private String methodName;
    private String userId;
    private String patientMrn;
    private LocalDateTime timestamp;
    private String errorMessage;
}
