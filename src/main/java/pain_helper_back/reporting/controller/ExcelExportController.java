//package pain_helper_back.reporting.controller;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import pain_helper_back.reporting.entity.DailyReportAggregate;
//import pain_helper_back.reporting.repository.DailyReportRepository;
//import pain_helper_back.reporting.service.ExcelExportService;
//
//import java.time.LocalDate;
//import java.util.List;
//
///**
// * REST API контроллер для экспорта отчетов в Excel
// *
// * НАЗНАЧЕНИЕ:
// * - Экспорт ежедневного отчета в Excel
// * - Экспорт нескольких отчетов за период в Excel
// *
// * ДОСТУП: Только для ADMIN роли (после внедрения Spring Security)
// *
// * ЭНДПОИНТЫ:
// * - GET /api/reports/export/excel/daily/{date} - экспорт отчета за дату
// * - GET /api/reports/export/excel/period - экспорт отчетов за период
// */
//@RestController
//@RequestMapping("/api/reports/export/excel")
//@RequiredArgsConstructor
//@Slf4j
//@CrossOrigin(origins = "*")
//public class ExcelExportController {
//
//    private final ExcelExportService excelExportService;
//    private final DailyReportRepository dailyReportRepository;
//
//    /**
//     * Экспорт ежедневного отчета в Excel
//     *
//     * @param date Дата отчета
//     * @return Excel файл
//     *
//     * Пример: GET /api/reports/export/excel/daily/2025-10-19
//     */
//    @GetMapping("/daily/{date}")
//    public ResponseEntity<byte[]> exportDailyReport(
//            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
//    ) {
//        log.info("GET /api/reports/export/excel/daily/{}", date);
//
//        try {
//            // Получить отчет
//            DailyReportAggregate report = dailyReportRepository.findByReportDate(date)
//                    .orElseThrow(() -> new RuntimeException("Report not found for date: " + date));
//
//            // Экспортировать в Excel
//            byte[] excelBytes = excelExportService.exportDailyReportToExcel(report);
//
//            // Настроить заголовки для скачивания
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.setContentDispositionFormData("attachment", "daily_report_" + date + ".xlsx");
//
//            log.info("Successfully exported report for {} to Excel", date);
//            return ResponseEntity.ok()
//                    .headers(headers)
//                    .body(excelBytes);
//
//        } catch (RuntimeException e) {
//            log.error("Report not found for {}: {}", date, e.getMessage());
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        } catch (Exception e) {
//            log.error("Error exporting report to Excel: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * Экспорт нескольких отчетов за период в Excel
//     *
//     * @param startDate Начальная дата
//     * @param endDate Конечная дата
//     * @return Excel файл со сводкой
//     *
//     * Пример: GET /api/reports/export/excel/period?startDate=2025-10-01&endDate=2025-10-19
//     */
//    @GetMapping("/period")
//    public ResponseEntity<byte[]> exportPeriodReports(
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        log.info("GET /api/reports/export/excel/period - startDate: {}, endDate: {}", startDate, endDate);
//
//        try {
//            // Получить отчеты за период
//            List<DailyReportAggregate> reports = dailyReportRepository
//                    .findByReportDateBetween(startDate, endDate);
//
//            if (reports.isEmpty()) {
//                log.warn("No reports found for period {} to {}", startDate, endDate);
//                return ResponseEntity.noContent().build();
//            }
//
//            // Экспортировать в Excel
//            byte[] excelBytes = excelExportService.exportMultipleDailyReportsToExcel(reports, startDate, endDate);
//
//            // Настроить заголовки для скачивания
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.setContentDispositionFormData("attachment",
//                    "reports_summary_" + startDate + "_to_" + endDate + ".xlsx");
//
//            log.info("Successfully exported {} reports to Excel", reports.size());
//            return ResponseEntity.ok()
//                    .headers(headers)
//                    .body(excelBytes);
//
//        } catch (Exception e) {
//            log.error("Error exporting multiple reports to Excel: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//}
