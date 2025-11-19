package pain_helper_back.reporting.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.reporting.entity.DailyReportAggregate;
import pain_helper_back.reporting.dto.ReportsSummaryDTO;
import pain_helper_back.reporting.service.ReportStatisticsService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportStatisticsController {

    private final ReportStatisticsService reportStatisticsService;

    @GetMapping("/daily")
    public ResponseEntity<List<DailyReportAggregate>> getReportsForPeriod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportStatisticsService.getReportsForPeriod(startDate, endDate));
    }

    @GetMapping("/daily/{date}")
    public ResponseEntity<DailyReportAggregate> getReportByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reportStatisticsService.getReportByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/daily/recent")
    public ResponseEntity<List<DailyReportAggregate>> getRecentReports(
            @RequestParam(defaultValue = "7") int limit
    ) {
        return ResponseEntity.ok(reportStatisticsService.getRecentReports(limit));
    }

    @PostMapping("/daily/generate")
    public ResponseEntity<DailyReportAggregate> generateReportForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean regenerate
    ) {
        return ResponseEntity.ok(reportStatisticsService.generateReportForDate(date, regenerate));
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportsSummaryDTO> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportStatisticsService.calculateSummaryStatistics(startDate, endDate));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(reportStatisticsService.getHealthStatus());
    }
}
