package pain_helper_back.analytics.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pain_helper_back.analytics.entity.AnalyticsEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * Repository для работы с бизнес-событиями в MongoDB
 */
@Repository
public interface AnalyticsEventRepository extends MongoRepository<AnalyticsEvent, String> {
    // find by type of events
    List<AnalyticsEvent> findByEventType(String eventType);
    //find by user
    List<AnalyticsEvent> findByUserId(String userId);
    //find by user and timestamp between
    List<AnalyticsEvent>findByUserIdAndTimestampBetween(String userId,LocalDateTime start, LocalDateTime end);


    List<AnalyticsEvent>findByEventTypeAndTimestampBetween(String eventType,LocalDateTime startDate, LocalDateTime endDate);
    //find by user role
    List<AnalyticsEvent> findByUserRole(String userRole);
    //find by timestamp between
    List<AnalyticsEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    //find by recommendation
    List<AnalyticsEvent> findByRecommendationId(Long recommendationId);
    //find by escalation
    List<AnalyticsEvent> findByEscalationId(Long escalationId);
    //find by patient
    List<AnalyticsEvent> findByPatientMrn(String patientMrn);
    //find by priority
    List<AnalyticsEvent> findByPriority(String priority);
    //calculate events by type
    Long countByEventType(String eventType);
    //calculate events by type and timestamps between
    Long countByEventTypeAndTimestampBetween(String eventType,LocalDateTime start, LocalDateTime end);
    //find events by high VAS(critical pain)
    @Query("{ 'vasLevel': { $gte: ?0 } }")
    List<AnalyticsEvent> findByVasLevelGreaterThanEqual(Integer vasLevel);
    //stats statistic
    @Query(value = "{ 'eventType': ?0 }", count = true)
    Long countByEventTypeCustom(String eventType);
    /**
     * Найти события старше указанной даты (для cleanup старых данных)
     */
    List<AnalyticsEvent> findByTimestampBefore(LocalDateTime cutoffDate);
}
