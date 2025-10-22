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
import pain_helper_back.reporting.service.EmailReportService;
import pain_helper_back.reporting.service.PdfExportService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API контроллер для экспорта отчетов в PDF и email рассылки
 * 
 * НАЗНАЧЕНИЕ:
 * - Экспорт ежедневного отчета в PDF
 * - Экспорт нескольких отчетов за период в PDF
 * - Отправка отчетов по email
 * 
 * ДОСТУП: Только для ADMIN роли (после внедрения Spring Security)
 * 
 * ЭНДПОИНТЫ:
 * - GET /api/reports/export/pdf/daily/{date} - экспорт отчета за дату
 * - GET /api/reports/export/pdf/period - экспорт отчетов за период
 * - POST /api/reports/export/pdf/daily/{date}/email - отправить отчет по email
 * - POST /api/reports/export/pdf/period/email - отправить сводку по email
 */
@RestController
@RequestMapping("/api/reports/export/pdf")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PdfExportController {
    
    private final PdfExportService pdfExportService;
    private final EmailReportService emailReportService;
    private final DailyReportRepository dailyReportRepository;

    /**
     * Экспорт ежедневного отчета в PDF
     * 
     * @param date Дата отчета
     * @return PDF файл
     * 
     * Пример: GET /api/reports/export/pdf/daily/2025-10-19
     */
    @GetMapping("/daily/{date}")
    public ResponseEntity<byte[]> exportDailyReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /api/reports/export/pdf/daily/{}", date);

        try {
            // Получить отчет
            DailyReportAggregate report = dailyReportRepository.findByReportDate(date)
                    .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));

            // Экспортировать в PDF
            byte[] pdfBytes = pdfExportService.exportDailyReportToPdf(report);

            // Настроить заголовки для скачивания
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "daily_report_" + date + ".pdf");

            log.info("Successfully exported report for {} to PDF", date);
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

    /**
     * Экспорт нескольких отчетов за период в PDF
     * 
     * @param startDate Начальная дата
     * @param endDate Конечная дата
     * @return PDF файл со сводкой
     * 
     * Пример: GET /api/reports/export/pdf/period?startDate=2025-10-01&endDate=2025-10-19
     */
    @GetMapping("/period")
    public ResponseEntity<byte[]> exportPeriodReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /api/reports/export/pdf/period - startDate: {}, endDate: {}", startDate, endDate);

        try {
            // Получить отчеты за период
            List<DailyReportAggregate> reports = dailyReportRepository
                    .findByReportDateBetween(startDate, endDate);

            if (reports.isEmpty()) {
                log.warn("No reports found for period {} to {}", startDate, endDate);
                return ResponseEntity.noContent().build();
            }

            // Экспортировать в PDF
            byte[] pdfBytes = pdfExportService.exportMultipleDailyReportsToPdf(reports, startDate, endDate);

            // Настроить заголовки для скачивания
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "reports_summary_" + startDate + "_to_" + endDate + ".pdf");

            log.info("Successfully exported {} reports to PDF", reports.size());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error exporting multiple reports to PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // EMAIL РАССЫЛКА
    // ============================================

    /**
     * Отправить ежедневный отчет по email
     * 
     * @param date Дата отчета
     * @param email Email получателя
     * @param attachPdf Прикрепить PDF (по умолчанию true)
     * @param attachExcel Прикрепить Excel (по умолчанию true)
     * @return Статус отправки
     * 
     * Пример: POST /api/reports/export/pdf/daily/2025-10-19/email?email=admin@example.com
     */
    @PostMapping("/daily/{date}/email")
    public ResponseEntity<Map<String, String>> emailDailyReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String email,
            @RequestParam(defaultValue = "true") boolean attachPdf,
            @RequestParam(defaultValue = "true") boolean attachExcel
    ) {
        log.info("POST /api/reports/export/pdf/daily/{}/email - recipient: {}", date, email);

        try {
            // Получить отчет
            DailyReportAggregate report = dailyReportRepository.findByReportDate(date)
                    .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));

            // Отправить email (асинхронно)
            emailReportService.sendDailyReport(report, email, attachPdf, attachExcel);

            log.info("Email sending initiated for report {}", date);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Email is being sent to " + email,
                    "date", date.toString()
            ));

        } catch (RuntimeException e) {
            log.error("Report not found for {}: {}", date, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "status", "error",
                            "message", "Report not found for date: " + date
                    ));
        } catch (Exception e) {
            log.error("Error initiating email send: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * Отправить сводный отчет за период по email
     * 
     * @param startDate Начальная дата
     * @param endDate Конечная дата
     * @param email Email получателя
     * @param attachPdf Прикрепить PDF (по умолчанию true)
     * @param attachExcel Прикрепить Excel (по умолчанию true)
     * @return Статус отправки
     * 
     * Пример: POST /api/reports/export/pdf/period/email?startDate=2025-10-01&endDate=2025-10-19&email=admin@example.com
     */
    @PostMapping("/period/email")
    public ResponseEntity<Map<String, String>> emailSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String email,
            @RequestParam(defaultValue = "true") boolean attachPdf,
            @RequestParam(defaultValue = "true") boolean attachExcel
    ) {
        log.info("POST /api/reports/export/pdf/period/email - startDate: {}, endDate: {}, recipient: {}",
                startDate, endDate, email);

        try {
            // Получить отчеты за период
            List<DailyReportAggregate> reports = dailyReportRepository
                    .findByReportDateBetween(startDate, endDate);

            if (reports.isEmpty()) {
                log.warn("No reports found for period {} to {}", startDate, endDate);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "error",
                                "message", "No reports found for the specified period"
                        ));
            }

            // Отправить email (асинхронно)
            emailReportService.sendSummaryReport(reports, startDate, endDate, email, attachPdf, attachExcel);

            log.info("Email sending initiated for summary report");
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Summary email is being sent to " + email,
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString(),
                    "reportsCount", String.valueOf(reports.size())
            ));

        } catch (Exception e) {
            log.error("Error initiating summary email send: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }
}
