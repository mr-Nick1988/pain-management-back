package pain_helper_back.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import pain_helper_back.kafka.dto.AnalyticsEventDTO;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing analytics events
 * Topic: analytics-events
 * Consumer: Logging Service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventProducer {

    private final KafkaTemplate<String, AnalyticsEventDTO> analyticsEventKafkaTemplate;

    @Value("${kafka.topic.analytics-events:analytics-events}")
    private String analyticsEventsTopic;

    /**
     * Publish analytics event to Kafka
     *
     * @param eventType type of event (e.g., "RECOMMENDATION_GENERATED")
     * @param userId user who triggered the event
     * @param metadata event-specific data
     */
    public void publishEvent(String eventType, String userId, Map<String, Object> metadata) {
        publishEvent(eventType, userId, null, metadata, null, null);
    }

    /**
     * Publish analytics event with full parameters
     *
     * @param eventType event type
     * @param userId user ID
     * @param sessionId session ID
     * @param metadata event metadata
     * @param module module name
     * @param category event category
     */
    public void publishEvent(String eventType, String userId, String sessionId, 
                            Map<String, Object> metadata, String module, String category) {
        
        AnalyticsEventDTO event = AnalyticsEventDTO.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .correlationId(UUID.randomUUID().toString())
                .userId(userId)
                .sessionId(sessionId)
                .metadata(metadata)
                .module(module)
                .level("INFO")
                .category(category)
                .build();

        publishEventToKafka(event);
    }

    /**
     * Publish recommendation generated event
     */
    public void publishRecommendationGenerated(String userId, Long patientId, Long recommendationId, 
                                               int painLevel, String primaryDrug) {
        Map<String, Object> metadata = Map.of(
                "patientId", patientId,
                "recommendationId", recommendationId,
                "painLevel", painLevel,
                "primaryDrug", primaryDrug
        );

        publishEvent("RECOMMENDATION_GENERATED", userId, null, metadata, "nurse", "BUSINESS");
    }

    /**
     * Publish recommendation approved event
     */
    public void publishRecommendationApproved(String userId, Long patientId, Long recommendationId, String comment) {
        Map<String, Object> metadata = Map.of(
                "patientId", patientId,
                "recommendationId", recommendationId,
                "approvalComment", comment != null ? comment : ""
        );

        publishEvent("RECOMMENDATION_APPROVED", userId, null, metadata, "doctor", "BUSINESS");
    }

    /**
     * Publish recommendation rejected event
     */
    public void publishRecommendationRejected(String userId, Long patientId, Long recommendationId, String reason) {
        Map<String, Object> metadata = Map.of(
                "patientId", patientId,
                "recommendationId", recommendationId,
                "rejectionReason", reason != null ? reason : ""
        );

        publishEvent("RECOMMENDATION_REJECTED", userId, null, metadata, "doctor", "BUSINESS");
    }

    /**
     * Publish escalation created event
     */
    public void publishEscalationCreated(String userId, Long patientId, Long escalationId, 
                                        String priority, String reason) {
        Map<String, Object> metadata = Map.of(
                "patientId", patientId,
                "escalationId", escalationId,
                "priority", priority,
                "reason", reason
        );

        publishEvent("ESCALATION_CREATED", userId, null, metadata, "doctor", "BUSINESS");
    }

    /**
     * Publish patient registered event
     */
    public void publishPatientRegistered(String userId, Long patientId, String firstName, String lastName) {
        Map<String, Object> metadata = Map.of(
                "patientId", patientId,
                "firstName", firstName,
                "lastName", lastName
        );

        publishEvent("PATIENT_REGISTERED", userId, null, metadata, "nurse", "BUSINESS");
    }

    /**
     * Publish VAS recorded event
     */
    public void publishVasRecorded(String userId, Long patientId, int painLevel, String location) {
        Map<String, Object> metadata = Map.of(
                "patientId", patientId,
                "painLevel", painLevel,
                "location", location != null ? location : ""
        );

        publishEvent("VAS_RECORDED", userId, null, metadata, "nurse", "BUSINESS");
    }

    /**
     * Publish user login event
     */
    public void publishUserLogin(String userId, String role, boolean success) {
        Map<String, Object> metadata = Map.of(
                "role", role,
                "success", success
        );

        publishEvent("USER_LOGIN", userId, null, metadata, "auth", "SECURITY");
    }

    /**
     * Internal method to publish event to Kafka
     */
    private void publishEventToKafka(AnalyticsEventDTO event) {
        try {
            log.debug("Publishing analytics event: type={}, eventId={}", 
                    event.getEventType(), event.getEventId());

            CompletableFuture<SendResult<String, AnalyticsEventDTO>> future = 
                    analyticsEventKafkaTemplate.send(analyticsEventsTopic, event.getEventId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Successfully published event {} to topic {} at offset {}", 
                            event.getEventId(), 
                            analyticsEventsTopic,
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event {}: {}", 
                            event.getEventId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing analytics event: {}", e.getMessage(), e);
            // Don't throw exception - analytics should not break business logic
        }
    }
}
