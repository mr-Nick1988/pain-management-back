package pain_helper_back.performance_SLA_monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для метрики производительности
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetricDTO {

    private String id;
    private String operationName;
    private Long executionTimeMs;
    private Long slaThresholdMs;
    private Boolean slaViolated;
    private Double slaPercentage;
    private String methodName;
    private String userId;
    private String userRole;
    private String patientMrn;
    private String status;
    private String errorMessage;
    private LocalDateTime timestamp;
}

