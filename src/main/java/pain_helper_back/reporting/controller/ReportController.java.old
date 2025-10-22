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
import pain_helper_back.reporting.service.DataAggregationService;
import pain_helper_back.reporting.service.EmailReportService;
import pain_helper_back.reporting.service.ExcelExportService;
import pain_helper_back.reporting.service.PdfExportService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/*
 * REST API контроллер для модуля отчетности
 *
 * НАЗНАЧЕНИЕ:
 * - Получение агрегированных отчетов за период
 * - Ручная генерация отчетов (для тестирования)
 * - Статистика по использованию системы
 *
 * ДОСТУП: Только для ADMIN роли (после внедрения Spring Security)
 *
 * ЭНДПОИНТЫ:
 * - GET /api/reports/daily - получить ежедневные отчеты
 * - GET /api/reports/daily/{date} - получить отчет за конкретную дату
 * - POST /api/reports/daily/generate - ручная генерация отчета
 * - GET /api/reports/summary - сводка за период
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // TODO: настроить CORS после деплоя фронтенда
public class ReportController {
    private final DailyReportRepository dailyReportRepository;
    private final DataAggregationService aggregationService;

    // ============================================
    // ПОЛУЧЕНИЕ ОТЧЕТОВ
    // ============================================

    /*
     * Получить все ежедневные отчеты за период
     *
     * @param startDate Начальная дата (формат: 2025-10-01)
     * @param endDate Конечная дата (формат: 2025-10-19)
     * @return Список ежедневных отчетов
     *
     * Пример: GET /api/reports/daily?startDate=2025-10-01&endDate=2025-10-19
     */
    @GetMapping("/daily")
    public ResponseEntity<List<DailyReportAggregate>> getDailyReports(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /api/reports/daily - startDate: {}, endDate: {}", startDate, endDate);

        try {
            List<DailyReportAggregate> reports;

            if (startDate != null && endDate != null) {
                // Получить отчеты за период
                reports = dailyReportRepository.findByReportDateBetween(startDate, endDate);
                log.info("Found {} reports between {} and {}", reports.size(), startDate, endDate);
            } else if (startDate != null) {
                // Получить отчеты начиная с даты
                reports = dailyReportRepository.findReportsForLastDays(startDate);
                log.info("Found {} reports since {}", reports.size(), startDate);
            } else {
                // Получить последние 30 дней
                LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
                reports = dailyReportRepository.findReportsForLastDays(thirtyDaysAgo);
                log.info("Found {} reports for last 30 days", reports.size());
            }
            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            log.error("Error fetching daily reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Получить отчет за конкретную дату
     *
     * @param date Дата отчета (формат: 2025-10-19)
     * @return Ежедневный отчет или 404 если не найден
     *
     * Пример: GET /api/reports/daily/2025-10-19
     */
    @GetMapping("/daily/{date}")
    public ResponseEntity<DailyReportAggregate> getDailyReportByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /api/reports/daily/{}", date);

        try {
            return dailyReportRepository.findByReportDate(date)
                    .map(report -> {
                        log.info("Found report for {}", date);
                        return ResponseEntity.ok(report);
                    })
                    .orElseGet(() -> {
                        log.warn("Report not found for {}", date);
                        return ResponseEntity.notFound().build();
                    });

        } catch (Exception e) {
            log.error("Error fetching report for {}: {}", date, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Получить последние N отчетов
     *
     * @param limit Количество отчетов (по умолчанию 7)
     * @return Список последних отчетов
     *
     * Пример: GET /api/reports/daily/recent?limit=7
     */
    @GetMapping("/daily/recent")
    public ResponseEntity<List<DailyReportAggregate>> getRecentReports(
            @RequestParam(defaultValue = "7") int limit
    ) {
        log.info("GET /api/reports/daily/recent - limit: {}", limit);

        try {
            LocalDate startDate = LocalDate.now().minusDays(limit);
            List<DailyReportAggregate> reports = dailyReportRepository.findReportsForLastDays(startDate);

            log.info("Found {} recent reports", reports.size());
            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            log.error("Error fetching recent reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // ============================================
    // РУЧНАЯ ГЕНЕРАЦИЯ ОТЧЕТОВ
    // ============================================

    /*
     * Ручная генерация отчета за конкретную дату
     * Используется для тестирования или восстановления данных
     *
     * @param date Дата для генерации отчета
     * @param regenerate Перегенерировать если уже существует (по умолчанию false)
     * @return Сгенерированный отчет
     *
     * Пример: POST /api/reports/daily/generate?date=2025-10-18&regenerate=true
     */
    @PostMapping("/daily/generate")
    public ResponseEntity<?> generateDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean regenerate
    ) {
        log.info("POST /api/reports/daily/generate - date: {}, regenerate: {}", date, regenerate);

        try {
            // Проверить, существует ли уже отчет
            if (!regenerate && dailyReportRepository.existsByReportDate(date)) {
                log.warn("Report for {} already exists. Use regenerate=true to override.", date);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "error", "Report already exists",
                                "message", "Report for " + date + " already exists. Use regenerate=true to override.",
                                "date", date
                        ));
            }

            // Если regenerate=true, удалить существующий отчет
            if (regenerate) {
                dailyReportRepository.findByReportDate(date).ifPresent(report -> {
                    log.info("Deleting existing report for {}", date);
                    dailyReportRepository.delete(report);
                });
            }

            // Генерация отчета
            DailyReportAggregate report = aggregationService.aggregateForDate(date);

            log.info("Successfully generated report for {}", date);
            return ResponseEntity.status(HttpStatus.CREATED).body(report);

        } catch (Exception e) {
            log.error("Error generating report for {}: {}", date, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Generation failed",
                            "message", e.getMessage(),
                            "date", date
                    ));
        }
    }
    // ============================================
    // СВОДНАЯ СТАТИСТИКА
    // ============================================

    /*
     * Получить сводную статистику за период
     * Агрегирует данные из ежедневных отчетов
     *
     * @param startDate Начальная дата
     * @param endDate Конечная дата
     * @return Сводная статистика
     *
     * Пример: GET /api/reports/summary?startDate=2025-10-01&endDate=2025-10-19
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /api/reports/summary - startDate: {}, endDate: {}", startDate, endDate);

        try {
            List<DailyReportAggregate> reports = dailyReportRepository
                    .findByReportDateBetween(startDate, endDate);

            if (reports.isEmpty()) {
                log.warn("No reports found for period {} to {}", startDate, endDate);
                return ResponseEntity.ok(Map.of(
                        "message", "No data available for the specified period",
                        "startDate", startDate,
                        "endDate", endDate
                ));
            }

            // Агрегация данных
            long totalPatients = reports.stream()
                    .mapToLong(r -> r.getTotalPatientsRegistered() != null ? r.getTotalPatientsRegistered() : 0L)
                    .sum();

            long totalVasRecords = reports.stream()
                    .mapToLong(r -> r.getTotalVasRecords() != null ? r.getTotalVasRecords() : 0L)
                    .sum();

            double avgVasLevel = reports.stream()
                    .filter(r -> r.getAverageVasLevel() != null)
                    .mapToDouble(DailyReportAggregate::getAverageVasLevel)
                    .average()
                    .orElse(0.0);

            long totalRecommendations = reports.stream()
                    .mapToLong(r -> r.getTotalRecommendations() != null ? r.getTotalRecommendations() : 0L)
                    .sum();

            long approvedRecommendations = reports.stream()
                    .mapToLong(r -> r.getApprovedRecommendations() != null ? r.getApprovedRecommendations() : 0L)
                    .sum();

            long rejectedRecommendations = reports.stream()
                    .mapToLong(r -> r.getRejectedRecommendations() != null ? r.getRejectedRecommendations() : 0L)
                    .sum();

            double overallApprovalRate = totalRecommendations > 0
                    ? (approvedRecommendations * 100.0 / totalRecommendations)
                    : 0.0;

            long totalEscalations = reports.stream()
                    .mapToLong(r -> r.getTotalEscalations() != null ? r.getTotalEscalations() : 0L)
                    .sum();

            long resolvedEscalations = reports.stream()
                    .mapToLong(r -> r.getResolvedEscalations() != null ? r.getResolvedEscalations() : 0L)
                    .sum();

            long totalLogins = reports.stream()
                    .mapToLong(r -> r.getTotalLogins() != null ? r.getTotalLogins() : 0L)
                    .sum();

            long uniqueUsers = reports.stream()
                    .mapToLong(r -> r.getUniqueActiveUsers() != null ? r.getUniqueActiveUsers() : 0L)
                    .max()
                    .orElse(0L);

            // Формирование ответа
            Map<String, Object> summary = Map.of(
                    "period", Map.of(
                            "startDate", startDate,
                            "endDate", endDate,
                            "daysCount", reports.size()
                    ),
                    "patients", Map.of(
                            "totalRegistered", totalPatients,
                            "totalVasRecords", totalVasRecords,
                            "averageVasLevel", Math.round(avgVasLevel * 100.0) / 100.0
                    ),
                    "recommendations", Map.of(
                            "total", totalRecommendations,
                            "approved", approvedRecommendations,
                            "rejected", rejectedRecommendations,
                            "approvalRate", Math.round(overallApprovalRate * 100.0) / 100.0
                    ),
                    "escalations", Map.of(
                            "total", totalEscalations,
                            "resolved", resolvedEscalations,
                            "pending", totalEscalations - resolvedEscalations
                    ),
                    "users", Map.of(
                            "totalLogins", totalLogins,
                            "uniqueActiveUsers", uniqueUsers
                    )
            );

            log.info("Generated summary for {} days", reports.size());
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error generating summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // ============================================
    // HEALTH CHECK
    // ============================================

    /*
     * Проверка состояния модуля отчетности
     *
     * @return Статус модуля
     *
     * Пример: GET /api/reports/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            long totalReports = dailyReportRepository.count();
            LocalDate latestReportDate = dailyReportRepository.findReportsForLastDays(LocalDate.now().minusDays(1))
                    .stream()
                    .map(DailyReportAggregate::getReportDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "module", "Reporting",
                    "totalReports", totalReports,
                    "latestReportDate", latestReportDate != null ? latestReportDate : "No reports yet",
                    "timestamp", LocalDate.now()
            ));

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "status", "DOWN",
                            "error", e.getMessage()
                    ));
        }
    }
    // ============================================
    // ЭКСПОРТ В EXCEL И PDF
    // ============================================

    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    /*
     * Экспорт ежедневного отчета в Excel
     *
     * @param date Дата отчета
     * @return Excel файл
     *
     * Пример: GET /api/reports/daily/2025-10-19/export/excel
     */
    @GetMapping("/daily/{date}/export/excel")
    public ResponseEntity<byte[]> exportDailyReportToExcel(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /api/reports/daily/{}/export/excel", date);

        try {
            // Получить отчет
            DailyReportAggregate report = dailyReportRepository.findByReportDate(date)
                    .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));

            // Экспортировать в Excel
            byte[] excelBytes = excelExportService.exportDailyReportToExcel(report);

            // Настроить заголовки для скачивания
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "daily_report_" + date + ".xlsx");

            log.info("Successfully exported report for {} to Excel", date);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            log.error("Error exporting report to Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Экспорт ежедневного отчета в PDF
     *
     * @param date Дата отчета
     * @return PDF файл
     *
     * Пример: GET /api/reports/daily/2025-10-19/export/pdf
     */
    @GetMapping("/daily/{date}/export/pdf")
    public ResponseEntity<byte[]> exportDailyReportToPdf(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /api/reports/daily/{}/export/pdf", date);

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

        } catch (Exception e) {
            log.error("Error exporting report to PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Экспорт нескольких отчетов за период в Excel
     *
     * @param startDate Начальная дата
     * @param endDate Конечная дата
     * @return Excel файл со сводкой
     *
     * Пример: GET /api/reports/export/excel?startDate=2025-10-01&endDate=2025-10-19
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportMultipleReportsToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /api/reports/export/excel - startDate: {}, endDate: {}", startDate, endDate);

        try {
            // Получить отчеты за период
            List<DailyReportAggregate> reports = dailyReportRepository
                    .findByReportDateBetween(startDate, endDate);

            if (reports.isEmpty()) {
                log.warn("No reports found for period {} to {}", startDate, endDate);
                return ResponseEntity.noContent().build();
            }

            // Экспортировать в Excel
            byte[] excelBytes = excelExportService.exportMultipleDailyReportsToExcel(reports, startDate, endDate);

            // Настроить заголовки для скачивания
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "reports_summary_" + startDate + "_to_" + endDate + ".xlsx");

            log.info("Successfully exported {} reports to Excel", reports.size());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            log.error("Error exporting multiple reports to Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Экспорт нескольких отчетов за период в PDF
     *
     * @param startDate Начальная дата
     * @param endDate Конечная дата
     * @return PDF файл со сводкой
     *
     * Пример: GET /api/reports/export/pdf?startDate=2025-10-01&endDate=2025-10-19
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportMultipleReportsToPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /api/reports/export/pdf - startDate: {}, endDate: {}", startDate, endDate);

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

    private final EmailReportService emailReportService;

    /*
     * Отправить ежедневный отчет по email
     *
     * @param date Дата отчета
     * @param email Email получателя
     * @param attachPdf Прикрепить PDF (по умолчанию true)
     * @param attachExcel Прикрепить Excel (по умолчанию true)
     * @return Статус отправки
     *
     * Пример: POST /api/reports/daily/2025-10-19/email?email=admin@example.com
     */
    @PostMapping("/daily/{date}/email")
    public ResponseEntity<Map<String, String>> emailDailyReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String email,
            @RequestParam(defaultValue = "true") boolean attachPdf,
            @RequestParam(defaultValue = "true") boolean attachExcel
    ) {
        log.info("POST /api/reports/daily/{}/email - recipient: {}", date, email);

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

        } catch (Exception e) {
            log.error("Error initiating email send: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }

    /*
     * Отправить сводный отчет за период по email
     *
     * @param startDate Начальная дата
     * @param endDate Конечная дата
     * @param email Email получателя
     * @param attachPdf Прикрепить PDF (по умолчанию true)
     * @param attachExcel Прикрепить Excel (по умолчанию true)
     * @return Статус отправки
     *
     * Пример: POST /api/reports/email/summary?startDate=2025-10-01&endDate=2025-10-19&email=admin@example.com
     */
    @PostMapping("/email/summary")
    public ResponseEntity<Map<String, String>> emailSummaryReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String email,
            @RequestParam(defaultValue = "true") boolean attachPdf,
            @RequestParam(defaultValue = "true") boolean attachExcel
    ) {
        log.info("POST /api/reports/email/summary - startDate: {}, endDate: {}, recipient: {}",
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
