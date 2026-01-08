package pain_helper_back.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pain_helper_back.client.dto.DailyReportDTO;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST client for Reporting Service
 * Provides proxy methods for frontend to access reports
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${reporting.service.url:http://localhost:8091}")
    private String reportingServiceUrl;

    @Value("${reporting.service.timeout:10000}")
    private int timeoutMs;

    /**
     * Get daily reports for period
     */
    @CircuitBreaker(name = "reportingService", fallbackMethod = "getDailyReportsFallback")
    public List<DailyReportDTO> getDailyReports(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching daily reports from Reporting Service: {} to {}", startDate, endDate);

        WebClient webClient = webClientBuilder
                .baseUrl(reportingServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reports/daily")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DailyReportDTO>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch daily reports from Reporting Service: {}", e.getMessage());
            throw new RuntimeException("Reporting Service unavailable", e);
        }
    }

    /**
     * Get daily report by specific date
     */
    @CircuitBreaker(name = "reportingService", fallbackMethod = "getDailyReportByDateFallback")
    public DailyReportDTO getDailyReportByDate(LocalDate date) {
        log.debug("Fetching daily report for date: {}", date);

        WebClient webClient = webClientBuilder
                .baseUrl(reportingServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return webClient.get()
                    .uri("/api/reports/daily/{date}", date)
                    .retrieve()
                    .bodyToMono(DailyReportDTO.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch daily report for {}: {}", date, e.getMessage());
            throw new RuntimeException("Reporting Service unavailable", e);
        }
    }

    /**
     * Get recent reports
     */
    @CircuitBreaker(name = "reportingService", fallbackMethod = "getRecentReportsFallback")
    public List<DailyReportDTO> getRecentReports(int limit) {
        log.debug("Fetching {} recent reports from Reporting Service", limit);

        WebClient webClient = webClientBuilder
                .baseUrl(reportingServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reports/daily/recent")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<DailyReportDTO>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch recent reports: {}", e.getMessage());
            throw new RuntimeException("Reporting Service unavailable", e);
        }
    }

    /**
     * Get summary statistics
     */
    @CircuitBreaker(name = "reportingService", fallbackMethod = "getSummaryFallback")
    public Map<String, Object> getSummary(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching summary from Reporting Service: {} to {}", startDate, endDate);

        WebClient webClient = webClientBuilder
                .baseUrl(reportingServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reports/summary")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch summary: {}", e.getMessage());
            throw new RuntimeException("Reporting Service unavailable", e);
        }
    }

    /**
     * Download Excel report
     */
    @CircuitBreaker(name = "reportingService", fallbackMethod = "downloadExcelFallback")
    public byte[] downloadExcel(LocalDate startDate, LocalDate endDate) {
        log.debug("Downloading Excel report from Reporting Service");

        WebClient webClient = webClientBuilder
                .baseUrl(reportingServiceUrl)
                .build();

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reports/export/excel")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofMillis(timeoutMs * 3)) // Longer timeout for file generation
                    .block();
        } catch (Exception e) {
            log.error("Failed to download Excel report: {}", e.getMessage());
            throw new RuntimeException("Reporting Service unavailable", e);
        }
    }

    /**
     * Download PDF report
     */
    @CircuitBreaker(name = "reportingService", fallbackMethod = "downloadPdfFallback")
    public byte[] downloadPdf(LocalDate startDate, LocalDate endDate) {
        log.debug("Downloading PDF report from Reporting Service");

        WebClient webClient = webClientBuilder
                .baseUrl(reportingServiceUrl)
                .build();

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/reports/export/pdf")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofMillis(timeoutMs * 3))
                    .block();
        } catch (Exception e) {
            log.error("Failed to download PDF report: {}", e.getMessage());
            throw new RuntimeException("Reporting Service unavailable", e);
        }
    }

    // ============= FALLBACK METHODS =============

    public List<DailyReportDTO> getDailyReportsFallback(LocalDate startDate, LocalDate endDate, Exception e) {
        log.error("Circuit breaker activated for getDailyReports: {}", e.getMessage());
        return Collections.emptyList();
    }

    public DailyReportDTO getDailyReportByDateFallback(LocalDate date, Exception e) {
        log.error("Circuit breaker activated for getDailyReportByDate: {}", e.getMessage());
        return null;
    }

    public List<DailyReportDTO> getRecentReportsFallback(int limit, Exception e) {
        log.error("Circuit breaker activated for getRecentReports: {}", e.getMessage());
        return Collections.emptyList();
    }

    public Map<String, Object> getSummaryFallback(LocalDate startDate, LocalDate endDate, Exception e) {
        log.error("Circuit breaker activated for getSummary: {}", e.getMessage());
        return Map.of(
                "error", "Reporting Service is temporarily unavailable",
                "message", e.getMessage()
        );
    }

    public byte[] downloadExcelFallback(LocalDate startDate, LocalDate endDate, Exception e) {
        log.error("Circuit breaker activated for downloadExcel: {}", e.getMessage());
        return new byte[0];
    }

    public byte[] downloadPdfFallback(LocalDate startDate, LocalDate endDate, Exception e) {
        log.error("Circuit breaker activated for downloadPdf: {}", e.getMessage());
        return new byte[0];
    }
}
