package pain_helper_back.reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.reporting.dto.ReportsSummaryDTO;
import pain_helper_back.reporting.entity.DailyReportAggregate;
import pain_helper_back.reporting.repository.DailyReportRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportStatisticsService {

    private final DailyReportRepository dailyReportRepository;
    private final SQLAggregationService sqlAggregationService;

    public List<DailyReportAggregate> getReportsForPeriod(LocalDate startDate, LocalDate endDate) {
        return sqlAggregationService.getReportsForPeriod(startDate, endDate);
    }

    public Optional<DailyReportAggregate> getReportByDate(LocalDate date) {
        return sqlAggregationService.getReportByDate(date);
    }

    public List<DailyReportAggregate> getRecentReports(int limit) {
        return sqlAggregationService.getRecentReports(limit);
    }

    @Transactional
    public DailyReportAggregate generateReportForDate(LocalDate date, boolean regenerate) {
        log.info("Generating report for {}, regenerate: {}", date, regenerate);
        return sqlAggregationService.generateReportForDate(date, regenerate);
    }

    public ReportsSummaryDTO calculateSummaryStatistics(LocalDate startDate, LocalDate endDate) {
        return sqlAggregationService.calculateSummaryStatistics(startDate, endDate);
    }

    public Map<String, Object> getHealthStatus() {
        try {
            long totalReports = dailyReportRepository.count();
            LocalDate latestReportDate = dailyReportRepository.findAll().stream()
                    .map(DailyReportAggregate::getReportDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            return Map.of(
                    "status", "UP",
                    "module", "Reporting",
                    "totalReports", totalReports,
                    "latestReportDate", latestReportDate != null ? latestReportDate : "No reports yet",
                    "timestamp", LocalDate.now()
            );
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            return Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            );
        }
    }
}
