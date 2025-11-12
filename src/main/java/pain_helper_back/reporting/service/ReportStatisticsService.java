//package pain_helper_back.reporting.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import pain_helper_back.reporting.entity.DailyReportAggregate;
//import pain_helper_back.reporting.repository.DailyReportRepository;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * Сервис для работы со статистикой и отчетами
// * Содержит всю бизнес-логику по агрегации и расчету статистики
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//@Transactional(readOnly = true)
//public class ReportStatisticsService {
//
//    private final DailyReportRepository dailyReportRepository;
//    private final DataAggregationService aggregationService;
//
//    /*
//     * Получить отчеты за период
//     *
//     * @param startDate Начальная дата (опционально)
//     * @param endDate Конечная дата (опционально)
//     * @return Список отчетов
//     */
//    public List<DailyReportAggregate> getReportsForPeriod(LocalDate startDate, LocalDate endDate) {
//        if (startDate != null && endDate != null) {
//            log.info("Fetching reports between {} and {}", startDate, endDate);
//            return dailyReportRepository.findByReportDateBetween(startDate, endDate);
//        } else if (startDate != null) {
//            log.info("Fetching reports since {}", startDate);
//            return dailyReportRepository.findReportsForLastDays(startDate);
//        } else {
//            // По умолчанию последние 30 дней
//            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
//            log.info("Fetching reports for last 30 days");
//            return dailyReportRepository.findReportsForLastDays(thirtyDaysAgo);
//        }
//    }
//
//    /**
//     * Получить отчет за конкретную дату
//     *
//     * @param date Дата отчета
//     * @return Optional с отчетом
//     */
//    public Optional<DailyReportAggregate> getReportByDate(LocalDate date) {
//        log.info("Fetching report for {}", date);
//        return dailyReportRepository.findByReportDate(date);
//    }
//
//    /**
//     * Получить последние N отчетов
//     *
//     * @param limit Количество отчетов
//     * @return Список последних отчетов
//     */
//    public List<DailyReportAggregate> getRecentReports(int limit) {
//        LocalDate startDate = LocalDate.now().minusDays(limit);
//        log.info("Fetching {} recent reports", limit);
//        return dailyReportRepository.findReportsForLastDays(startDate);
//    }
//
//    /**
//     * Сгенерировать отчет за дату
//     *
//     * @param date Дата для генерации
//     * @param regenerate Перегенерировать если существует
//     * @return Сгенерированный отчет
//     * @throws IllegalStateException если отчет уже существует и regenerate=false
//     */
//    @Transactional
//    public DailyReportAggregate generateReportForDate(LocalDate date, boolean regenerate) {
//        log.info("Generating report for {}, regenerate: {}", date, regenerate);
//
//        // Проверка существования
//        if (!regenerate && dailyReportRepository.existsByReportDate(date)) {
//            throw new IllegalStateException("Report for " + date + " already exists. Use regenerate=true to override.");
//        }
//
//        // Удаление существующего при regenerate=true
//        if (regenerate) {
//            dailyReportRepository.findByReportDate(date).ifPresent(report -> {
//                log.info("Deleting existing report for {}", date);
//                dailyReportRepository.delete(report);
//            });
//        }
//
//        // Генерация нового отчета
//        DailyReportAggregate report = aggregationService.aggregateForDate(date);
//        log.info("Successfully generated report for {}", date);
//
//        return report;
//    }
//
//    /**
//     * Получить сводную статистику за период
//     * Агрегирует данные из ежедневных отчетов
//     *
//     * @param startDate Начальная дата
//     * @param endDate Конечная дата
//     * @return Map со статистикой
//     */
//    public Map<String, Object> calculateSummaryStatistics(LocalDate startDate, LocalDate endDate) {
//        log.info("Calculating summary statistics for {} to {}", startDate, endDate);
//
//        List<DailyReportAggregate> reports = dailyReportRepository
//                .findByReportDateBetween(startDate, endDate);
//
//        if (reports.isEmpty()) {
//            log.warn("No reports found for period {} to {}", startDate, endDate);
//            return Map.of(
//                    "message", "No data available for the specified period",
//                    "startDate", startDate,
//                    "endDate", endDate
//            );
//        }
//
//        // Агрегация данных
//        long totalPatients = reports.stream()
//                .mapToLong(r -> r.getTotalPatientsRegistered() != null ? r.getTotalPatientsRegistered() : 0L)
//                .sum();
//
//        long totalVasRecords = reports.stream()
//                .mapToLong(r -> r.getTotalVasRecords() != null ? r.getTotalVasRecords() : 0L)
//                .sum();
//
//        double avgVasLevel = reports.stream()
//                .filter(r -> r.getAverageVasLevel() != null)
//                .mapToDouble(DailyReportAggregate::getAverageVasLevel)
//                .average()
//                .orElse(0.0);
//
//        long totalRecommendations = reports.stream()
//                .mapToLong(r -> r.getTotalRecommendations() != null ? r.getTotalRecommendations() : 0L)
//                .sum();
//
//        long approvedRecommendations = reports.stream()
//                .mapToLong(r -> r.getApprovedRecommendations() != null ? r.getApprovedRecommendations() : 0L)
//                .sum();
//
//        long rejectedRecommendations = reports.stream()
//                .mapToLong(r -> r.getRejectedRecommendations() != null ? r.getRejectedRecommendations() : 0L)
//                .sum();
//
//        double overallApprovalRate = totalRecommendations > 0
//                ? (approvedRecommendations * 100.0 / totalRecommendations)
//                : 0.0;
//
//        long totalEscalations = reports.stream()
//                .mapToLong(r -> r.getTotalEscalations() != null ? r.getTotalEscalations() : 0L)
//                .sum();
//
//        long resolvedEscalations = reports.stream()
//                .mapToLong(r -> r.getResolvedEscalations() != null ? r.getResolvedEscalations() : 0L)
//                .sum();
//
//        long totalLogins = reports.stream()
//                .mapToLong(r -> r.getTotalLogins() != null ? r.getTotalLogins() : 0L)
//                .sum();
//
//        long uniqueUsers = reports.stream()
//                .mapToLong(r -> r.getUniqueActiveUsers() != null ? r.getUniqueActiveUsers() : 0L)
//                .max()
//                .orElse(0L);
//
//        // Формирование результата
//        Map<String, Object> summary = Map.of(
//                "period", Map.of(
//                        "startDate", startDate,
//                        "endDate", endDate,
//                        "daysCount", reports.size()
//                ),
//                "patients", Map.of(
//                        "totalRegistered", totalPatients,
//                        "totalVasRecords", totalVasRecords,
//                        "averageVasLevel", Math.round(avgVasLevel * 100.0) / 100.0
//                ),
//                "recommendations", Map.of(
//                        "total", totalRecommendations,
//                        "approved", approvedRecommendations,
//                        "rejected", rejectedRecommendations,
//                        "approvalRate", Math.round(overallApprovalRate * 100.0) / 100.0
//                ),
//                "escalations", Map.of(
//                        "total", totalEscalations,
//                        "resolved", resolvedEscalations,
//                        "pending", totalEscalations - resolvedEscalations
//                ),
//                "users", Map.of(
//                        "totalLogins", totalLogins,
//                        "uniqueActiveUsers", uniqueUsers
//                )
//        );
//
//        log.info("Generated summary for {} days", reports.size());
//        return summary;
//    }
//
//    /**
//     * Проверка состояния модуля отчетности
//     *
//     * @return Map со статусом
//     */
//    public Map<String, Object> getHealthStatus() {
//        try {
//            long totalReports = dailyReportRepository.count();
//            LocalDate latestReportDate = dailyReportRepository.findReportsForLastDays(LocalDate.now().minusDays(1))
//                    .stream()
//                    .map(DailyReportAggregate::getReportDate)
//                    .max(LocalDate::compareTo)
//                    .orElse(null);
//
//            return Map.of(
//                    "status", "UP",
//                    "module", "Reporting",
//                    "totalReports", totalReports,
//                    "latestReportDate", latestReportDate != null ? latestReportDate : "No reports yet",
//                    "timestamp", LocalDate.now()
//            );
//        } catch (Exception e) {
//            log.error("Health check failed: {}", e.getMessage(), e);
//            return Map.of(
//                    "status", "DOWN",
//                    "error", e.getMessage()
//            );
//        }
//    }
//}
