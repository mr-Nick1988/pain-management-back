package pain_helper_back.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;`nimport lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for Kafka analytics events
 * Published to topic: analytics-events
 * Consumed by: Logging Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEventDTO {

    /**
     * Unique event identifier
     */
    private String eventId;

    /**
     * Event type: RECOMMENDATION_GENERATED, RECOMMENDATION_APPROVED, etc.
     */
    private String eventType;

    /**
     * Timestamp when event occurred
     */
    private LocalDateTime timestamp;

    /**
     * Correlation ID for tracing
     */
    private String correlationId;

    /**
     * User who triggered the event
     */
    private String userId;

    /**
     * Session ID
     */
    private String sessionId;

    /**
     * Event metadata (flexible JSON-like structure)
     */
    private Map<String, Object> metadata;

    /**
     * Module that generated the event
     */
    private String module;

    /**
     * Optional: Event severity/level
     */
    private String level;

    /**
     * Optional: Event category
     */
    private String category;
}
