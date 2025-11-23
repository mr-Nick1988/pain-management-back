package pain_helper_back.reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pain_helper_back.reporting.dto.ReportsSummaryDTO;
import pain_helper_back.reporting.dto.DailyReportAggregateDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportStatisticsService {

    private final SQLAggregationService sqlAggregationService;
    @Value("${analytics.reporting.base-url:http://localhost:8091}")
    private String reportingBaseUrl;

    public List<DailyReportAggregateDTO> getReportsForPeriod(LocalDate startDate, LocalDate endDate) {
        return sqlAggregationService.getReportsForPeriod(startDate, endDate);
    }

    public Optional<DailyReportAggregateDTO> getReportByDate(LocalDate date) {
        return sqlAggregationService.getReportByDate(date);
    }

    public List<DailyReportAggregateDTO> getRecentReports(int limit) {
        return sqlAggregationService.getRecentReports(limit);
    }

    @Transactional
    public DailyReportAggregateDTO generateReportForDate(LocalDate date, boolean regenerate) {
        log.info("Generating report for {}, regenerate: {}", date, regenerate);
        return sqlAggregationService.generateReportForDate(date, regenerate);
    }

    public ReportsSummaryDTO calculateSummaryStatistics(LocalDate startDate, LocalDate endDate) {
        return sqlAggregationService.calculateSummaryStatistics(startDate, endDate);
    }

    public Map<String, Object> getHealthStatus() {
        try {
            String base = normalizeBase(reportingBaseUrl);
            RestTemplate rest = new RestTemplate();

            // latest report
            UriComponentsBuilder recent = UriComponentsBuilder.fromHttpUrl(base)
                    .path("/api/reports/recent")
                    .queryParam("limit", 1);
            ResponseEntity<List<DailyReportAggregateDTO>> recentResp = rest.exchange(
                    recent.toUriString(), HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    new ParameterizedTypeReference<List<DailyReportAggregateDTO>>() {}
            );
            LocalDate latestReportDate = null;
            if (recentResp.getBody() != null && !recentResp.getBody().isEmpty()) {
                latestReportDate = recentResp.getBody().get(0).getReportDate();
            }

            // naive total count (bounded) — request last 3650 days
            UriComponentsBuilder listAll = UriComponentsBuilder.fromHttpUrl(base)
                    .path("/api/reports/daily")
                    .queryParam("startDate", LocalDate.now().minusYears(10))
                    .queryParam("endDate", LocalDate.now());
            ResponseEntity<List<DailyReportAggregateDTO>> allResp = rest.exchange(
                    listAll.toUriString(), HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    new ParameterizedTypeReference<List<DailyReportAggregateDTO>>() {}
            );
            long totalReports = allResp.getBody() != null ? allResp.getBody().size() : 0L;

            return Map.of(
                    "status", "UP",
                    "module", "Reporting",
                    "totalReports", totalReports,
                    "latestReportDate", latestReportDate != null ? latestReportDate : "No reports yet",
                    "timestamp", LocalDate.now()
            );
        } catch (RestClientException e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            return Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            );
        }
    }

    private String normalizeBase(String base) {
        if (base == null || base.isBlank()) return "http://localhost:8091";
        if (base.endsWith("/")) return base.substring(0, base.length() - 1);
        return base;
    }
}
