package pain_helper_back.performance_SLA_monitoring.service;


import pain_helper_back.performance_SLA_monitoring.dto.PerformanceMetricDTO;
import pain_helper_back.performance_SLA_monitoring.dto.PerformanceStatisticDTO;
import pain_helper_back.performance_SLA_monitoring.dto.SlaViolationDTO;
import pain_helper_back.performance_SLA_monitoring.entity.PerformanceMetric;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Сервис для мониторинга производительности и SLA
 */
public interface PerformanceMonitoringService {
    /*
     * Сохранить метрику производительности
     */
    void recordMetric(PerformanceMetric metric);
    /**
     * Получить все метрики за период
     */
    List<PerformanceMetricDTO> getMetrics(LocalDateTime start, LocalDateTime end);
    /*
     * Получить статистику производительности
     */
    PerformanceStatisticDTO getStatistics(LocalDateTime start, LocalDateTime end);

    List<SlaViolationDTO> getSlaViolations(LocalDateTime start, LocalDateTime end);

    PerformanceStatisticDTO.OperationStatistics getOperationStatistics(String operationName, LocalDateTime start, LocalDateTime end);

    List<PerformanceMetricDTO> getSlowestOperations(int limit, LocalDateTime start, LocalDateTime end);

    List<PerformanceMetricDTO> getMetricsByPatient(String patientMrn);

    List<PerformanceMetricDTO> getMetricsByUser(String userId);

    void cleanupOldMetrics(int daysToKeep);

}
