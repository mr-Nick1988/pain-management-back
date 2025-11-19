package pain_helper_back.reporting.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.reporting.entity.DailyReportAggregate;
import pain_helper_back.reporting.repository.DailyReportRepository;
import pain_helper_back.reporting.service.PdfExportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PdfExportController {

    private final PdfExportService pdfExportService;
    private final DailyReportRepository dailyReportRepository;

    @GetMapping("/daily/{date}/export/pdf")
    public ResponseEntity<byte[]> exportDailyReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /api/reports/daily/{}/export/pdf", date);

        try {
            DailyReportAggregate report = dailyReportRepository.findByReportDate(date)
                    .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));

            byte[] pdfBytes = pdfExportService.exportDailyReportToPdf(report);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "daily_report_" + date + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (RuntimeException e) {
            log.error("Report not found for {}: {}", date, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error exporting report to PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPeriodReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /api/reports/export/pdf - startDate: {}, endDate: {}", startDate, endDate);

        try {
            List<DailyReportAggregate> reports = dailyReportRepository
                    .findAllByReportDateBetween(startDate, endDate);

            if (reports.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            byte[] pdfBytes = pdfExportService.exportMultipleDailyReportsToPdf(reports, startDate, endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "reports_summary_" + startDate + "_to_" + endDate + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error exporting multiple reports to PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}