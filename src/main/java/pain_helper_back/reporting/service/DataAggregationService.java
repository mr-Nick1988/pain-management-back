package pain_helper_back.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.analytics.entity.AnalyticsEvent;
import pain_helper_back.analytics.repository.AnalyticsEventRepository;
import pain_helper_back.reporting.entity.DailyReportAggregate;
import pain_helper_back.reporting.repository.DailyReportRepository;
import pain_helper_back.reporting.repository.MonthlyReportRepository;
import pain_helper_back.reporting.repository.WeeklyReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Сервис для агрегации данных из MongoDB в PostgreSQL
 *
 * НАЗНАЧЕНИЕ:
 * - Извлекает события из MongoDB (AnalyticsEvent)
 * - Агрегирует данные (подсчет, средние значения, группировки)
 * - Сохраняет агрегаты в PostgreSQL для долгосрочного хранения
 * - Очищает старые события из MongoDB (retention policy)
 *
 * РАСПИСАНИЕ:
 * - Ежедневная агрегация: каждый день в 00:30
 * - Еженедельная агрегация: каждый понедельник в 01:00
 * - Ежемесячная агрегация: 1-го числа каждого месяца в 02:00
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataAggregationService {

    private final AnalyticsEventRepository mongoRepository;
    private final DailyReportRepository dailyReportRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final MonthlyReportRepository monthlyReportRepository;
    private final ObjectMapper objectMapper;

    // ============================================
    // ЕЖЕДНЕВНАЯ АГРЕГАЦИЯ (каждый день в 00:30)
    // ============================================

    /**
     * Агрегация данных за вчерашний день
     * Запускается автоматически каждый день в 00:30
     */
    @Scheduled(cron = "0 30 0 * * *") // 00:30 каждый день
    @Transactional
    public void aggregateDailyData() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting daily aggregation for date: {}", yesterday);

        try {
            // Проверить, не создан ли уже отчет за этот день
            if (dailyReportRepository.existsByReportDate(yesterday)) {
                log.warn("Daily report for {} already exists. Skipping.", yesterday);
                return;
            }

            // 1. Извлечь события из MongoDB за вчерашний день
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

            List<AnalyticsEvent> events = mongoRepository.findByTimestampBetween(startOfDay, endOfDay);
            log.info("Found {} events for {}", events.size(), yesterday);

            if (events.isEmpty()) {
                log.warn("No events found for {}. Creating empty report.", yesterday);
            }

            // 2. Агрегировать данные
            DailyReportAggregate aggregate = aggregateDailyEvents(events, yesterday);

            // 3. Сохранить в PostgreSQL
            dailyReportRepository.save(aggregate);
            log.info("Daily aggregation completed successfully for {}", yesterday);

            // 4. Опционально: очистить старые события из MongoDB (старше 30 дней)
            cleanupOldEvents(30);

        } catch (Exception e) {
            log.error("Error during daily aggregation for {}: {}", yesterday, e.getMessage(), e);
        }
    }

    /**
     * Агрегация событий за день
     * Извлекает метрики из списка событий и создает агрегат
     */
    private DailyReportAggregate aggregateDailyEvents(List<AnalyticsEvent> events, LocalDate reportDate) {

        // Группировка событий по типам
        Map<String, Long> eventsByType = events.stream()
                .collect(Collectors.groupingBy(
                        AnalyticsEvent::getEventType,
                        Collectors.counting()
                ));

        // ============================================
        // СТАТИСТИКА ПАЦИЕНТОВ
        // ============================================
        Long patientsRegistered = eventsByType.getOrDefault("PATIENT_REGISTERED", 0L);
        Long vasRecords = eventsByType.getOrDefault("VAS_RECORDED", 0L);

        // Средний VAS (уровень боли)
        Double avgVas = events.stream()
                .filter(e -> "VAS_RECORDED".equals(e.getEventType()))
                .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("vasLevel"))
                .mapToDouble(e -> {
                    try {
                        return Double.parseDouble(e.getMetadata().get("vasLevel").toString());
                    } catch (Exception ex) {
                        return 0.0;
                    }
                })
                .average()
                .orElse(0.0);

        // Критические VAS (>= 7)
        Long criticalVas = events.stream()
                .filter(e -> "VAS_RECORDED".equals(e.getEventType()))
                .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("vasLevel"))
                .filter(e -> {
                    try {
                        return Double.parseDouble(e.getMetadata().get("vasLevel").toString()) >= 7.0;
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .count();

        // ============================================
        // СТАТИСТИКА РЕКОМЕНДАЦИЙ
        // ============================================
        Long totalRecs = eventsByType.getOrDefault("RECOMMENDATION_CREATED", 0L);
        Long approvedRecs = eventsByType.getOrDefault("RECOMMENDATION_APPROVED", 0L);
        Long rejectedRecs = eventsByType.getOrDefault("RECOMMENDATION_REJECTED", 0L);

        // Процент одобрения
        Double approvalRate = totalRecs > 0
                ? (approvedRecs.doubleValue() / totalRecs.doubleValue() * 100.0)
                : 0.0;

        // ============================================
        // СТАТИСТИКА ЭСКАЛАЦИЙ
        // ============================================
        Long totalEsc = eventsByType.getOrDefault("ESCALATION_CREATED", 0L);
        Long resolvedEsc = eventsByType.getOrDefault("ESCALATION_RESOLVED", 0L);
        Long pendingEsc = totalEsc - resolvedEsc;

        // Среднее время разрешения эскалаций (в часах)
        Double avgResolutionTime = events.stream()
                .filter(e -> "ESCALATION_RESOLVED".equals(e.getEventType()))
                .filter(e -> e.getProcessingTimeMs() != null)
                .mapToLong(AnalyticsEvent::getProcessingTimeMs)
                .average()
                .orElse(0.0) / 3600000.0; // Конвертация мс в часы

        // ============================================
        // ПРОИЗВОДИТЕЛЬНОСТЬ СИСТЕМЫ
        // ============================================
        Double avgProcessingTime = events.stream()
                .filter(e -> e.getProcessingTimeMs() != null)
                .mapToLong(AnalyticsEvent::getProcessingTimeMs)
                .average()
                .orElse(0.0);

        Long totalOperations = (long) events.size();

        // Подсчет неудачных операций (если в метаданных есть status=ERROR)
        Long failedOperations = events.stream()
                .filter(e -> e.getMetadata() != null &&
                        "ERROR".equals(e.getMetadata().get("status")))
                .count();

        // ============================================
        // АКТИВНОСТЬ ПОЛЬЗОВАТЕЛЕЙ
        // ============================================
        Long totalLogins = eventsByType.getOrDefault("USER_LOGIN_SUCCESS", 0L);
        Long failedLogins = eventsByType.getOrDefault("USER_LOGIN_FAILED", 0L);

        // Уникальные пользователи
        Long uniqueUsers = events.stream()
                .map(AnalyticsEvent::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // ============================================
        // ИСПОЛЬЗОВАНИЕ ПРЕПАРАТОВ (JSON)
        // ============================================
        Map<String, Long> topDrugs = extractTopDrugs(events);
        String topDrugsJson = convertToJson(topDrugs);

        Map<String, Long> doseAdjustments = extractDoseAdjustments(events);
        String doseAdjustmentsJson = convertToJson(doseAdjustments);

        // ============================================
        // СОЗДАНИЕ АГРЕГАТА
        // ============================================
        return DailyReportAggregate.builder()
                .reportDate(reportDate)
                .totalPatientsRegistered(patientsRegistered)
                .totalVasRecords(vasRecords)
                .averageVasLevel(avgVas)
                .criticalVasCount(criticalVas)
                .totalRecommendations(totalRecs)
                .approvedRecommendations(approvedRecs)
                .rejectedRecommendations(rejectedRecs)
                .approvalRate(approvalRate)
                .totalEscalations(totalEsc)
                .resolvedEscalations(resolvedEsc)
                .pendingEscalations(pendingEsc)
                .averageResolutionTimeHours(avgResolutionTime)
                .averageProcessingTimeMs(avgProcessingTime)
                .totalOperations(totalOperations)
                .failedOperations(failedOperations)
                .totalLogins(totalLogins)
                .uniqueActiveUsers(uniqueUsers)
                .failedLoginAttempts(failedLogins)
                .topDrugsJson(topDrugsJson)
                .doseAdjustmentsJson(doseAdjustmentsJson)
                .sourceEventsCount((long) events.size())
                .build();
    }

    /*
     * Извлечь топ-10 наиболее назначаемых препаратов
     * Ищет в метаданных событий поле "drugName"
     */
    private Map<String, Long> extractTopDrugs(List<AnalyticsEvent> events) {
        return events.stream()
                .filter(e -> "RECOMMENDATION_APPROVED".equals(e.getEventType()) ||
                        "RECOMMENDATION_CREATED".equals(e.getEventType()))
                .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("drugName"))
                .map(e -> e.getMetadata().get("drugName").toString())
                .collect(Collectors.groupingBy(drug -> drug, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
    /*
     * Извлечь статистику корректировок доз
     * Ищет в метаданных поле "adjustmentReason"
     */
    private Map<String,Long> extractDoseAdjustments(List<AnalyticsEvent>events){
        return events.stream()
                .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("adjustmentReason"))
                .map(e -> e.getMetadata().get("adjustmentReason").toString())
                .collect(Collectors.groupingBy(reason -> reason, Collectors.counting()));
    }
    /*
     * Конвертация Map в JSON строку
     */
    private String convertToJson(Map<String, Long> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Error converting map to JSON: {}", e.getMessage());
            return "{}";
        }
    }
    /*
     * Очистка старых событий из MongoDB
     * Удаляет события старше указанного количества дней
     *
     * @param retentionDays Количество дней хранения (обычно 30)
     */
    private void cleanupOldEvents(int retentionDays){
        try{
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            // Найти старые события
            List<AnalyticsEvent> oldEvents = mongoRepository.findByTimestampBefore(cutoffDate);

            if(!oldEvents.isEmpty()){
                log.info("Cleaning up {} old events (older than {} days)", oldEvents.size(), retentionDays);
                mongoRepository.deleteAll(oldEvents);
                log.info("Cleanup completed successfully");
            }else {
                log.debug("No old events to cleanup");
            }
        }catch (Exception e){
            log.error("Error during cleanup of old events: {}", e.getMessage(), e);
        }
    }
    /*
     * Ручная агрегация за конкретную дату (для тестирования или восстановления данных)
     */
    @Transactional
    public DailyReportAggregate aggregateForDate(LocalDate date) {
        log.info("Manual aggregation requested for date: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<AnalyticsEvent> events = mongoRepository.findByTimestampBetween(startOfDay, endOfDay);
        DailyReportAggregate aggregate = aggregateDailyEvents(events, date);

        return dailyReportRepository.save(aggregate);
    }
}