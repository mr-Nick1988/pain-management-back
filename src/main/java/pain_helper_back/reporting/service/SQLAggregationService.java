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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SQLAggregationService {

    

    @Value("${analytics.reporting.base-url:http://localhost:8091}")
    private String reportingBaseUrl;

    public List<DailyReportAggregateDTO> getReportsForPeriod(LocalDate startDate, LocalDate endDate) {
        try {
            String base = normalizeBase(reportingBaseUrl);
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(base)
                    .path("/api/reports/daily");
            if (startDate != null) b.queryParam("startDate", startDate);
            if (endDate != null) b.queryParam("endDate", endDate);

            RestTemplate rest = new RestTemplate();
            ResponseEntity<List<DailyReportAggregateDTO>> resp = rest.exchange(
                    b.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    new ParameterizedTypeReference<List<DailyReportAggregateDTO>>() {}
            );
            return resp.getBody() != null ? resp.getBody() : List.of();
        } catch (RestClientException e) {
            log.error("Failed to fetch reports for period from Reporting service: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public Optional<DailyReportAggregateDTO> getReportByDate(LocalDate date) {
        try {
            String base = normalizeBase(reportingBaseUrl);
            String url = base + "/api/reports/daily/" + date;
            RestTemplate rest = new RestTemplate();
            DailyReportAggregateDTO agg = rest.getForObject(url, DailyReportAggregateDTO.class);
            return Optional.ofNullable(agg);
        } catch (RestClientException e) {
            log.error("Failed to fetch report by date from Reporting service: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public List<DailyReportAggregateDTO> getRecentReports(int limit) {
        try {
            String base = normalizeBase(reportingBaseUrl);
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(base)
                    .path("/api/reports/recent")
                    .queryParam("limit", limit);
            RestTemplate rest = new RestTemplate();
            ResponseEntity<List<DailyReportAggregateDTO>> resp = rest.exchange(
                    b.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    new ParameterizedTypeReference<List<DailyReportAggregateDTO>>() {}
            );
            return resp.getBody() != null ? resp.getBody() : List.of();
        } catch (RestClientException e) {
            log.error("Failed to fetch recent reports from Reporting service: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public DailyReportAggregateDTO generateReportForDate(LocalDate date, boolean regenerate) {
        try {
            String base = normalizeBase(reportingBaseUrl);
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(base)
                    .path("/api/reports/daily/")
                    .path(date.toString())
                    .path("/generate")
                    .queryParam("regenerate", regenerate);
            RestTemplate rest = new RestTemplate();
            ResponseEntity<DailyReportAggregateDTO> resp = rest.exchange(
                    b.toUriString(),
                    HttpMethod.POST,
                    new HttpEntity<>(new HttpHeaders()),
                    DailyReportAggregateDTO.class
            );
            DailyReportAggregateDTO body = resp.getBody();
            if (body != null) {
                log.info("DailyReportAggregate generated via Reporting service for {}", date);
                return body;
            }
        } catch (RestClientException e) {
            log.error("Failed to generate report via Reporting service: {}", e.getMessage(), e);
        }
        throw new IllegalStateException("Failed to generate report for " + date + " via Reporting service");
    }

    @Transactional(readOnly = true)
    public ReportsSummaryDTO calculateSummaryStatistics(LocalDate startDate, LocalDate endDate) {
        try {
            String base = normalizeBase(reportingBaseUrl);
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(base)
                    .path("/api/reports/summary");
            if (startDate != null) b.queryParam("startDate", startDate);
            if (endDate != null) b.queryParam("endDate", endDate);
            RestTemplate rest = new RestTemplate();
            ResponseEntity<ReportsSummaryDTO> resp = rest.exchange(
                    b.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    ReportsSummaryDTO.class
            );
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("Failed to fetch summary from Reporting service: {}", e.getMessage(), e);
            return new ReportsSummaryDTO();
        }
    }

    private String normalizeBase(String base) {
        if (base == null || base.isBlank()) return "http://localhost:8091";
        if (base.endsWith("/")) return base.substring(0, base.length() - 1);
        return base;
    }
}
