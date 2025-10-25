package pain_helper_back.performance_SLA_monitoring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.performance_SLA_monitoring.dto.PerformanceMetricDTO;
import pain_helper_back.performance_SLA_monitoring.dto.PerformanceStatisticDTO;
import pain_helper_back.performance_SLA_monitoring.dto.SlaViolationDTO;
import pain_helper_back.performance_SLA_monitoring.service.PerformanceMonitoringService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API для мониторинга производительности и SLA
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PerformanceController {
    private final PerformanceMonitoringService performanceMonitoringService;

    /**
     * GET /api/performance/statistics?start=...&end=...
     * Получить полную статистику производительности
     */
    @GetMapping("/statistics")
    public ResponseEntity<PerformanceStatisticDTO> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("Calculating performance statistics from {} to {}", start, end);
        PerformanceStatisticDTO stats = performanceMonitoringService.getStatistics(start, end);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/performance/statistics/recent?hours=24
     * Статистика за последние N часов
     */
    @GetMapping("/statistics/recent")
    public ResponseEntity<PerformanceStatisticDTO> getRecentStatistics(
            @RequestParam(defaultValue = "24") int hours) {

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);

        log.info("Calculating statistics for last {} hours", hours);
        PerformanceStatisticDTO stats = performanceMonitoringService.getStatistics(start, end);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/performance/metrics?start=...&end=...
     * Получить все метрики за период
     */
    @GetMapping("/metrics")
    public ResponseEntity<List<PerformanceMetricDTO>> getMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("Fetching performance metrics from {} to {}", start, end);
        List<PerformanceMetricDTO> metrics = performanceMonitoringService.getMetrics(start, end);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/performance/sla-violations?start=...&end=...
     * Получить нарушения SLA
     */
    @GetMapping("/sla-violations")
    public ResponseEntity<List<SlaViolationDTO>> getSlaViolations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("Fetching SLA violations from {} to {}", start, end);
        List<SlaViolationDTO> violations = performanceMonitoringService.getSlaViolations(start, end);
        return ResponseEntity.ok(violations);
    }

    /**
     * GET /api/performance/sla-violations/recent?hours=24
     * Нарушения SLA за последние N часов
     */
    @GetMapping("/sla-violations/recent")
    public ResponseEntity<List<SlaViolationDTO>> getRecentViolations(
            @RequestParam(defaultValue = "24") int hours) {

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);

        log.info("Fetching SLA violations for last {} hours", hours);
        return ResponseEntity.ok(performanceMonitoringService.getSlaViolations(start, end));
    }

    /**
     * GET /api/performance/operations/{operationName}/statistics?start=...&end=...
     * Статистика по конкретной операции
     */
    @GetMapping("/operations/{operationName}/statistics")
    public ResponseEntity<PerformanceStatisticDTO.OperationStatistics> getOperationStats(
            @PathVariable String operationName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("Fetching statistics for operation: {}", operationName);
        PerformanceStatisticDTO.OperationStatistics stats =
                performanceMonitoringService.getOperationStatistics(operationName, start, end);

        if (stats == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/performance/slowest?limit=10&hours=24
     * Топ самых медленных операций
     */
    @GetMapping("/slowest")
    public ResponseEntity<List<PerformanceMetricDTO>> getSlowestOperations(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24") int hours) {

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);

        log.info("Fetching top {} slowest operations for last {} hours", limit, hours);
        List<PerformanceMetricDTO> slowest = performanceMonitoringService.getSlowestOperations(limit, start, end);
        return ResponseEntity.ok(slowest);
    }

    /**
     * GET /api/performance/patients/{mrn}/metrics
     * Метрики по конкретному пациенту
     */
    @GetMapping("/patients/{mrn}/metrics")
    public ResponseEntity<List<PerformanceMetricDTO>> getPatientMetrics(@PathVariable String mrn) {
        log.info("Fetching metrics for patient: {}", mrn);
        List<PerformanceMetricDTO> metrics = performanceMonitoringService.getMetricsByPatient(mrn);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/performance/users/{userId}/metrics
     * Метрики по конкретному пользователю
     */
    @GetMapping("/users/{userId}/metrics")
    public ResponseEntity<List<PerformanceMetricDTO>> getUserMetrics(@PathVariable String userId) {
        log.info("Fetching metrics for user: {}", userId);
        List<PerformanceMetricDTO> metrics = performanceMonitoringService.getMetricsByUser(userId);
        return ResponseEntity.ok(metrics);
    }

    /**
     * DELETE /api/performance/cleanup?daysToKeep=30
     * Очистить старые метрики
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanupOldMetrics(
            @RequestParam(defaultValue = "30") int daysToKeep) {

        log.info("Cleaning up metrics older than {} days", daysToKeep);
        performanceMonitoringService.cleanupOldMetrics(daysToKeep);
        return ResponseEntity.ok("Old metrics cleaned up successfully");
    }
}
