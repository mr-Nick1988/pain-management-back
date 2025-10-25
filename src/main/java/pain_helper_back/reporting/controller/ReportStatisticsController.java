package pain_helper_back.reporting.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.reporting.entity.DailyReportAggregate;
import pain_helper_back.reporting.service.ReportStatisticsService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API контроллер для статистики и получения отчетов
 * 
 * НАЗНАЧЕНИЕ:
 * - Получение агрегированных отчетов за период
 * - Ручная генерация отчетов (для тестирования)
 * - Статистика по использованию системы
 * - Health check модуля
 * 
 * ДОСТУП: Только для ADMIN роли (после внедрения Spring Security)
 * 
 * ЭНДПОИНТЫ:
 * - GET /api/reports/daily - получить ежедневные отчеты
 * - GET /api/reports/daily/{date} - получить отчет за конкретную дату
 * - GET /api/reports/daily/recent - получить последние N отчетов
 * - POST /api/reports/daily/generate - ручная генерация отчета
 * - GET /api/reports/summary - сводка за период
 * - GET /api/reports/health - проверка состояния модуля
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportStatisticsController {
    
    private final ReportStatisticsService statisticsService;

    // ============================================
    // ПОЛУЧЕНИЕ ОТЧЕТОВ
    // ============================================

    /**
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
            List<DailyReportAggregate> reports = statisticsService.getReportsForPeriod(startDate, endDate);
            log.info("Found {} reports", reports.size());
            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            log.error("Error fetching daily reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
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
            return statisticsService.getReportByDate(date)
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

    /**
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
            List<DailyReportAggregate> reports = statisticsService.getRecentReports(limit);
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

    /**
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
            DailyReportAggregate report = statisticsService.generateReportForDate(date, regenerate);
            log.info("Successfully generated report for {}", date);
            return ResponseEntity.status(HttpStatus.CREATED).body(report);

        } catch (IllegalStateException e) {
            log.warn("Report generation conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Report already exists",
                            "message", e.getMessage(),
                            "date", date
                    ));
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

    /**
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
            Map<String, Object> summary = statisticsService.calculateSummaryStatistics(startDate, endDate);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error generating summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================
    // HEALTH CHECK
    // ============================================

    /**
     * Проверка состояния модуля отчетности
     * 
     * @return Статус модуля
     * 
     * Пример: GET /api/reports/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("GET /api/reports/health");
        
        Map<String, Object> health = statisticsService.getHealthStatus();
        
        if ("DOWN".equals(health.get("status"))) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
        
        return ResponseEntity.ok(health);
    }
}
