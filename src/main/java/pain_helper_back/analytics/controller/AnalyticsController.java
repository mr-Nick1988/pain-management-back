package pain_helper_back.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.analytics.dto.EventStatsDTO;
import pain_helper_back.analytics.dto.PatientStatsDTO;
import pain_helper_back.analytics.dto.PerformanceStatsDTO;
import pain_helper_back.analytics.dto.UserActivityDTO;
import pain_helper_back.analytics.entity.AnalyticsEvent;

import pain_helper_back.analytics.service.AnalyticsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    /*
     * GET /api/analytics/events/stats
     * Получить общую статистику по событиям
     */
    @GetMapping("/events/stats")
    public ResponseEntity<EventStatsDTO> getEventStats(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("GET /api/analytics/events/stats - startDate={}, endDate={}", startDate, endDate);
        EventStatsDTO stats = analyticsService.getEventStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /*
     * GET /api/analytics/users/{userId}/activity
     * Получить активность пользователя
     */
    @GetMapping("/users/{userId}/activity")
    public ResponseEntity<UserActivityDTO> getUserActivity(@PathVariable String userId,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("GET /api/analytics/users/{userId}/activity - userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        UserActivityDTO activity = analyticsService.getUserActivity(userId, startDate, endDate);
        return ResponseEntity.ok(activity);
    }

    /*
     * GET /api/analytics/performance
     * Получить статистику производительности
     */
    @GetMapping("/performance")
    public ResponseEntity<PerformanceStatsDTO> getPerformanceStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        log.info("GET /api/analytics/performance - startDate={}, endDate={}", startDate, endDate);
        PerformanceStatsDTO stats = analyticsService.getPerformanceStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /*
     * GET /api/analytics/patients/stats
     * Получить статистику по пациентам
     */
    @GetMapping("/patients/stats")
    public ResponseEntity<PatientStatsDTO> getPatientStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        log.info("GET /api/analytics/patients/stats - startDate={}, endDate={}", startDate, endDate);
        PatientStatsDTO stats = analyticsService.getPatientsStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /*
     * GET /api/analytics/events/recent
     * Получить последние события
     */
    @GetMapping("/events/recent")
    public ResponseEntity<List<AnalyticsEvent>> getRecentEvents(
            @RequestParam(defaultValue = "50") int limit) {
        log.info("GET /api/analytics/events/recent - limit={}", limit);
        List<AnalyticsEvent> events = analyticsService.getRecentEvents(limit);
        return ResponseEntity.ok(events);
    }

    /*
     * GET /api/analytics/events/type/{eventType}
     * Получить события по типу
     */
    @GetMapping("/events/type/{eventType}")
    public ResponseEntity<List<AnalyticsEvent>> getEventsByType(
            @PathVariable String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        log.info("GET /api/analytics/events/type/{} - startDate={}, endDate={}", eventType, startDate, endDate);
        List<AnalyticsEvent> events = analyticsService.getEventsByType(eventType, startDate, endDate);
        return ResponseEntity.ok(events);
    }

}
