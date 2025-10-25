package pain_helper_back.performance_SLA_monitoring.dto;


import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO для статистики производительности
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStatisticDTO {
    // Общая статистика
    private Long totalOperations;
    private Long successfulOperations;
    private Long failedOperations;
    private Long slaViolations;
    private Double slaViolationRate;           // Процент нарушений
    // Времена выполнения
    private Double averageExecutionTimeMs;
    private Long minExecutionTimeMs;
    private Long maxExecutionTimeMs;
    private Long medianExecutionTimeMs;
    private Long p95ExecutionTimeMs;           // 95-й перцентиль
    private Long p99ExecutionTimeMs;           // 99-й перцентиль
    // По операциям
    private Map<String, OperationStatistics> operationStats;

    // Топ медленных операций
    private List<PerformanceMetricDTO> slowestOperations;

    // Топ нарушений SLA
    private List<SlaViolationDTO> recentViolations;

    // Тренды
    private Map<String, Long> hourlyOperationCount;
    private Map<String, Double> hourlyAverageTime;

    /**
     * Статистика по конкретной операции
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationStatistics {
        private String operationName;
        private Long count;
        private Double averageTimeMs;
        private Long slaThresholdMs;
        private Long violations;
        private Double violationRate;
        private Long minTimeMs;
        private Long maxTimeMs;
    }
}
