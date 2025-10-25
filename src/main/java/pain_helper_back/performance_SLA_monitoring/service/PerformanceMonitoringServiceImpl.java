package pain_helper_back.performance_SLA_monitoring.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.performance_SLA_monitoring.config.PerformanceSlaConfig;
import pain_helper_back.performance_SLA_monitoring.dto.PerformanceMetricDTO;
import pain_helper_back.performance_SLA_monitoring.dto.PerformanceStatisticDTO;
import pain_helper_back.performance_SLA_monitoring.dto.SlaViolationDTO;
import pain_helper_back.performance_SLA_monitoring.entity.PerformanceMetric;
import pain_helper_back.performance_SLA_monitoring.repository.PerformanceMetricRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/*
 * Реализация сервиса мониторинга производительности
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringServiceImpl implements PerformanceMonitoringService {

    private final PerformanceMetricRepository metricRepository;
    private final PerformanceSlaConfig slaConfig;
    private final ModelMapper modelMapper;


    @Override
    @Transactional
    public void recordMetric(PerformanceMetric metric) {
        try {
            metricRepository.save(metric);

            if (metric.getSlaViolated()) {
                log.warn("SLA VIOLATION: {} took {}ms (threshold: {}ms, {}% of SLA)",
                        metric.getOperationName(),
                        metric.getExecutionTimeMs(),
                        metric.getSlaThresholdMs(),
                        String.format("%.1f", metric.getSlaPercentage()));
            }
        } catch (Exception e) {
            log.error("Failed to record performance metric: {}", e.getMessage());
        }
    }

    @Override
    public List<PerformanceMetricDTO> getMetrics(LocalDateTime start, LocalDateTime end) {
        List<PerformanceMetric> metrics = metricRepository.findByTimestampBetween(start, end);
        return metrics.stream().map(metric -> modelMapper.map(metric, PerformanceMetricDTO.class)).toList();
    }

    @Override
    public List<SlaViolationDTO> getSlaViolations(LocalDateTime start, LocalDateTime end) {
        List<PerformanceMetric> metrics = metricRepository.findByTimestampBetween(start, end);
        return metrics.stream().map(metric -> modelMapper.map(metric, SlaViolationDTO.class)).toList();

    }


    @Override
    public PerformanceStatisticDTO getStatistics(LocalDateTime start, LocalDateTime end) {
        List<PerformanceMetric> metrics = metricRepository.findByTimestampBetween(start, end);

        if (metrics.isEmpty()) {
            return PerformanceStatisticDTO.builder()
                    .totalOperations(0L)
                    .successfulOperations(0L)
                    .failedOperations(0L)
                    .slaViolations(0L)
                    .slaViolationRate(0.0)
                    .build();
        }

        // Общая статистика
        long total = metrics.size();
        long successful = metrics.stream().filter(m -> "SUCCESS".equals(m.getStatus())).count();
        long failed = total - successful;
        long violations = metrics.stream().filter(PerformanceMetric::getSlaViolated).count();
        double violationRate = (violations * 100.0) / total;

        // Времена выполнения
        List<Long> times = metrics.stream()
                .map(PerformanceMetric::getExecutionTimeMs)
                .sorted()
                .toList();

        double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long minTime = times.get(0);
        long maxTime = times.get(times.size() - 1);
        long medianTime = times.get(times.size() / 2);
        long p95Time = times.get((int) (times.size() * 0.95));
        long p99Time = times.get((int) (times.size() * 0.99));

        // Статистика по операциям
        Map<String, PerformanceStatisticDTO.OperationStatistics> operationStats = calculateOperationStatistics(metrics);

        // Топ медленных операций
        List<PerformanceMetricDTO> slowestOperations = metrics.stream()
                .sorted(Comparator.comparingLong(PerformanceMetric::getExecutionTimeMs).reversed())
                .limit(10)
                .map(metric -> modelMapper.map(metric, PerformanceMetricDTO.class))
                .toList();
        // Последние нарушения SLA
        List<SlaViolationDTO> recentViolations = metrics.stream()
                .filter(PerformanceMetric::getSlaViolated)
                .sorted(Comparator.comparing(PerformanceMetric::getTimestamp).reversed())
                .limit(20)
                .map(metric -> modelMapper.map(metric, SlaViolationDTO.class))
                .toList();
        // Тренды по часам
        Map<String, Long> hourlyCount = calculateHourlyOperationCount(metrics);
        Map<String, Double> hourlyAvgTime = calculateHourlyAverageTime(metrics);
        return PerformanceStatisticDTO.builder()
                .totalOperations(total)
                .successfulOperations(successful)
                .failedOperations(failed)
                .slaViolations(violations)
                .slaViolationRate(violationRate)
                .averageExecutionTimeMs(avgTime)
                .minExecutionTimeMs(minTime)
                .maxExecutionTimeMs(maxTime)
                .medianExecutionTimeMs(medianTime)
                .p95ExecutionTimeMs(p95Time)
                .p99ExecutionTimeMs(p99Time)
                .operationStats(operationStats)
                .slowestOperations(slowestOperations)
                .recentViolations(recentViolations)
                .hourlyOperationCount(hourlyCount)
                .hourlyAverageTime(hourlyAvgTime)
                .build();
    }

    @Override
    public PerformanceStatisticDTO.OperationStatistics getOperationStatistics(
            String operationName, LocalDateTime start, LocalDateTime end) {
        List<PerformanceMetric> metrics = metricRepository.findByOperationNameAndTimestampBetween(operationName, start, end);

        if (metrics.isEmpty()) {
            return null;
        }
        long count = metrics.size();

        double avgTime = metrics.stream()
                .mapToLong(PerformanceMetric::getExecutionTimeMs)
                .average()
                .orElse(0.0);

        long violations = metrics.stream()
                .filter(PerformanceMetric::getSlaViolated)
                .count();
        double violationRate = (violations * 100.0) / count;

        long minTime = metrics.stream()
                .mapToLong(PerformanceMetric::getExecutionTimeMs)
                .min()
                .orElse(0L);

        long maxTime = metrics.stream()
                .mapToLong(PerformanceMetric::getExecutionTimeMs)
                .max()
                .orElse(0L);

        Long slaThreshold = slaConfig.getThreshold(operationName);

        return PerformanceStatisticDTO.OperationStatistics.builder()
                .operationName(operationName)
                .count(count)
                .averageTimeMs(avgTime)
                .slaThresholdMs(slaThreshold)
                .violations(violations)
                .violationRate(violationRate)
                .minTimeMs(minTime)
                .maxTimeMs(maxTime)
                .build();
    }
    @Override
    public List<PerformanceMetricDTO> getSlowestOperations(int limit, LocalDateTime start, LocalDateTime end) {
        List<PerformanceMetric> metrics = metricRepository
                .findTop10ByTimestampBetweenOrderByExecutionTimeMsDesc(start, end);
        return metrics.stream().map(metric -> modelMapper.map(metric, PerformanceMetricDTO.class)).toList();
    }
    @Override
    public List<PerformanceMetricDTO> getMetricsByPatient(String patientMrn) {
        List<PerformanceMetric> metrics = metricRepository.findByPatientMrn(patientMrn);
        return metrics.stream().map(metric -> modelMapper.map(metric, PerformanceMetricDTO.class)).toList();
    }
    @Override
    public List<PerformanceMetricDTO> getMetricsByUser(String userId) {
        List<PerformanceMetric> metrics = metricRepository.findByUserId(userId);
        return metrics.stream().map(metric -> modelMapper.map(metric, PerformanceMetricDTO.class)).toList();
    }
    @Override
    @Transactional
    public void cleanupOldMetrics(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        List<PerformanceMetric> oldMetrics = metricRepository.findByTimestampBetween(LocalDateTime.MIN,cutoff);

        if(!oldMetrics.isEmpty()){
            metricRepository.deleteAll(oldMetrics);
            log.info("Cleaned up {} old performance metrics (older than {} days)",
                    oldMetrics.size(), daysToKeep);
        }

    }
// ========== HELPER METHODS ==========

    private PerformanceMetricDTO toDTO(PerformanceMetric metric) {
        return PerformanceMetricDTO.builder()
                .id(metric.getId())
                .operationName(metric.getOperationName())
                .executionTimeMs(metric.getExecutionTimeMs())
                .slaThresholdMs(metric.getSlaThresholdMs())
                .slaViolated(metric.getSlaViolated())
                .slaPercentage(metric.getSlaPercentage())
                .methodName(metric.getMethodName())
                .userId(metric.getUserId())
                .userRole(metric.getUserRole())
                .patientMrn(metric.getPatientMrn())
                .status(metric.getStatus())
                .errorMessage(metric.getErrorMessage())
                .timestamp(metric.getTimestamp())
                .build();
    }

    private SlaViolationDTO toViolationDTO(PerformanceMetric metric) {
        long excessTime = metric.getExecutionTimeMs() - metric.getSlaThresholdMs();

        return SlaViolationDTO.builder()
                .operationName(metric.getOperationName())
                .executionTimeMs(metric.getExecutionTimeMs())
                .slaThresholdMs(metric.getSlaThresholdMs())
                .excessTimeMs(excessTime)
                .slaPercentage(metric.getSlaPercentage())
                .methodName(metric.getMethodName())
                .userId(metric.getUserId())
                .patientMrn(metric.getPatientMrn())
                .timestamp(metric.getTimestamp())
                .errorMessage(metric.getErrorMessage())
                .build();
    }

    private Map<String, PerformanceStatisticDTO.OperationStatistics> calculateOperationStatistics(
            List<PerformanceMetric> metrics) {

        Map<String, List<PerformanceMetric>> byOperation = metrics.stream()
                .collect(Collectors.groupingBy(PerformanceMetric::getOperationName));

        Map<String, PerformanceStatisticDTO.OperationStatistics> stats = new HashMap<>();

        byOperation.forEach((opName, opMetrics) -> {
            long count = opMetrics.size();
            double avgTime = opMetrics.stream()
                    .mapToLong(PerformanceMetric::getExecutionTimeMs)
                    .average()
                    .orElse(0.0);

            long violations = opMetrics.stream()
                    .filter(PerformanceMetric::getSlaViolated)
                    .count();

            double violationRate = (violations * 100.0) / count;

            long minTime = opMetrics.stream()
                    .mapToLong(PerformanceMetric::getExecutionTimeMs)
                    .min()
                    .orElse(0L);

            long maxTime = opMetrics.stream()
                    .mapToLong(PerformanceMetric::getExecutionTimeMs)
                    .max()
                    .orElse(0L);

            Long slaThreshold = slaConfig.getThreshold(opName);

            stats.put(opName, PerformanceStatisticDTO.OperationStatistics.builder()
                    .operationName(opName)
                    .count(count)
                    .averageTimeMs(avgTime)
                    .slaThresholdMs(slaThreshold)
                    .violations(violations)
                    .violationRate(violationRate)
                    .minTimeMs(minTime)
                    .maxTimeMs(maxTime)
                    .build());
        });

        return stats;
    }

    private Map<String, Long> calculateHourlyOperationCount(List<PerformanceMetric> metrics) {
        return metrics.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getTimestamp().getHour() + ":00",
                        Collectors.counting()
                ));
    }

    private Map<String, Double> calculateHourlyAverageTime(List<PerformanceMetric> metrics) {
        return metrics.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getTimestamp().getHour() + ":00",
                        Collectors.averagingLong(PerformanceMetric::getExecutionTimeMs)
                ));
    }
}

