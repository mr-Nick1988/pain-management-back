package pain_helper_back.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pain_helper_back.analytics.dto.EventStatsDTO;
import pain_helper_back.analytics.dto.PatientStatsDTO;
import pain_helper_back.analytics.dto.PerformanceStatsDTO;
import pain_helper_back.analytics.dto.UserActivityDTO;
import pain_helper_back.analytics.entity.AnalyticsEvent;

import pain_helper_back.analytics.repository.AnalyticsEventRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final AnalyticsEventRepository analyticsEventRepository;


    /*
     * Получить общую статистику по событиям
     */
    public EventStatsDTO getEventStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<AnalyticsEvent> events;
        if (startDate != null && endDate != null) {
            events = analyticsEventRepository.findByTimestampBetween(startDate, endDate);
        } else {
            events = analyticsEventRepository.findAll();
        }
        Long totalEvents = (long) events.size();
        // Группировка по типу события
        Map<String, Long> eventsByType = events.stream()
                .collect(Collectors.groupingBy(
                        AnalyticsEvent::getEventType,
                        Collectors.counting()
                ));
        // Группировка по роли пользователя
        Map<String, Long> eventsByRole = events.stream()
                .filter(event -> event.getUserRole() != null)
                .collect(Collectors.groupingBy(
                        AnalyticsEvent::getUserRole,
                        Collectors.counting()
                ));
        // Группировка по статусу
        Map<String, Long> eventsByStatus = events.stream()
                .filter(e -> e.getStatus() != null)
                .collect(Collectors.groupingBy(
                        AnalyticsEvent::getStatus,
                        Collectors.counting()
                ));
        log.info("Event stats calculated: totalEvents={}, types={}, roles={}",
                totalEvents, eventsByType.size(), eventsByRole.size());

        return new EventStatsDTO(totalEvents, eventsByType, eventsByRole, eventsByStatus);
    }

    /*
     * Получить активность пользователя
     */
    public UserActivityDTO getUserActivity(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<AnalyticsEvent> userEvents;
        if (startDate != null && endDate != null) {
            userEvents = analyticsEventRepository.findByUserIdAndTimestampBetween(userId, startDate, endDate);
        } else {
            userEvents = analyticsEventRepository.findByUserId(userId);
        }
        if (userEvents.isEmpty()) {
            return new UserActivityDTO(userId, "UNKNOWN", 0L, null, 0L, 0L);
        }
        Long totalActions = (long) userEvents.size();
        String userRole = userEvents.get(0).getUserRole();

        LocalDateTime lastActivity = userEvents.stream()
                .map(AnalyticsEvent::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        Long loginCount = userEvents.stream()
                .filter(event -> "USER_LOGIN_SUCCESS".equals(event.getEventType()))
                .count();

        Long failedLoginCount = userEvents.stream()
                .filter(event -> "USER_LOGIN_FAILED".equals(event.getEventType()))
                .count();

        log.info("User activity calculated: userId={}, totalActions={}, loginCount={}",
                userId, totalActions, loginCount);

        return new UserActivityDTO(userId, userRole, totalActions, lastActivity, loginCount, failedLoginCount);

    }

    /*
     * Получить статистику производительности
     */
    public PerformanceStatsDTO getPerformanceStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<AnalyticsEvent> events;

        if (startDate != null && endDate != null) {
            events = analyticsEventRepository.findByTimestampBetween(startDate, endDate);
        } else {
            events = analyticsEventRepository.findAll();
        }
        //Рекомендации
        List<AnalyticsEvent> recommendationEvents = events.stream()
                .filter(event -> event.getRecommendationId() != null)
                .toList();

        Long totalRecommendations = recommendationEvents.stream()
                .filter(e -> "RECOMMENDATION_APPROVED".equals(e.getEventType()) ||
                        "RECOMMENDATION_REJECTED".equals(e.getEventType()))
                .count();

        Long approvedRecommendations = recommendationEvents.stream()
                .filter(e -> "RECOMMENDATION_APPROVED".equals(e.getEventType()))
                .count();

        Long rejectedRecommendations = recommendationEvents.stream()
                .filter(e -> "RECOMMENDATION_REJECTED".equals(e.getEventType()))
                .count();

        Double averageProcessingTimeMs = recommendationEvents.stream()
                .filter(e -> e.getProcessingTimeMs() != null)
                .mapToLong(AnalyticsEvent::getProcessingTimeMs)
                .average()
                .orElse(0.0);

        // Эскалации
        List<AnalyticsEvent> escalationEvents = events.stream()
                .filter(e -> e.getEscalationId() != null)
                .toList();

        Long totalEscalations = escalationEvents.stream()
                .filter(e -> "ESCALATION_CREATED".equals(e.getEventType()))
                .count();

        Long resolvedEscalations = escalationEvents.stream()
                .filter(e -> "ESCALATION_RESOLVED".equals(e.getEventType()))
                .count();

        Double averageEscalationResolutionTimeMs = escalationEvents.stream()
                .filter(e -> "ESCALATION_RESOLVED".equals(e.getEventType()) &&
                        e.getProcessingTimeMs() != null)
                .mapToLong(AnalyticsEvent::getProcessingTimeMs)
                .average()
                .orElse(0.0);

        log.info("Performance stats calculated: totalRecs={}, approved={}, totalEsc={}",
                totalRecommendations, approvedRecommendations, totalEscalations);

        return new PerformanceStatsDTO(
                averageProcessingTimeMs,
                totalRecommendations,
                approvedRecommendations,
                rejectedRecommendations,
                totalEscalations,
                resolvedEscalations,
                averageEscalationResolutionTimeMs
        );
    }

    /*
     * Получить статистику по пациентам
     */
    public PatientStatsDTO getPatientsStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<AnalyticsEvent> events;
        if (startDate != null && endDate != null) {
            events = analyticsEventRepository.findByTimestampBetween(startDate, endDate);
        } else {
            events = analyticsEventRepository.findAll();
        }
        // Регистрация пациентов
        List<AnalyticsEvent> patientEvents = events.stream()
                .filter(e -> "PATIENT_REGISTERED".equals(e.getEventType()))
                .toList();
        Long totalPatients = (long) patientEvents.size();

        // Группировка по полу
        Map<String, Long> patientsByGender = patientEvents.stream()
                .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("gender"))
                .collect(Collectors.groupingBy(
                        e -> String.valueOf(e.getMetadata().get("gender")),
                        Collectors.counting()
                ));
        // Группировка по возрастным группам
        Map<String, Long> patientsByAgeGroup = patientEvents.stream()
                .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("age"))
                .collect(Collectors.groupingBy(
                        e -> getAgeGroup((Integer) e.getMetadata().get("age")),
                        Collectors.counting()
                ));
        // VAS записи
        List<AnalyticsEvent> vasEvents = events.stream()
                .filter(e -> "VAS_RECORDED".equals(e.getEventType()))
                .toList();

        Long totalVasRecords = (long) vasEvents.size();

        Long criticalVasRecords = vasEvents.stream()
                .filter(e -> "HIGH".equals(e.getPriority()))
                .count();

        Double averageVasLevel = vasEvents.stream()
                .filter(e -> e.getVasLevel() != null)
                .mapToInt(AnalyticsEvent::getVasLevel)
                .average()
                .orElse(0.0);

        log.info("Patient stats calculated: totalPatients={}, totalVas={}, criticalVas={}",
                totalPatients, totalVasRecords, criticalVasRecords);

        return new PatientStatsDTO(
                totalPatients,
                patientsByGender,
                patientsByAgeGroup,
                totalVasRecords,
                criticalVasRecords,
                averageVasLevel
        );
    }

    /*
     * Получить последние события
     */
    public List<AnalyticsEvent> getRecentEvents(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return analyticsEventRepository.findAll(pageRequest).getContent();
    }
    /*
     * Получить события по типу
     */
    public List<AnalyticsEvent> getEventsByType(String eventType, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return analyticsEventRepository.findByEventTypeAndTimestampBetween(eventType, startDate, endDate);
        }
        return analyticsEventRepository.findByEventType(eventType);
    }


    private String getAgeGroup(Integer age) {
        if (age == null) return "UNKNOWN";
        if (age < 18) return "0-17";
        if (age < 30) return "18-29";
        if (age < 45) return "30-44";
        if (age < 60) return "45-59";
        if (age < 75) return "60-74";
        return "75+";
    }

}


