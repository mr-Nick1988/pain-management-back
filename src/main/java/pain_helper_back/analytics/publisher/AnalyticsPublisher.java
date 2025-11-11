package pain_helper_back.analytics.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pain_helper_back.analytics.entity.AnalyticsEvent;

/**
 * Publishes analytics events to Kafka. Falls back to REST if Kafka is unavailable.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${kafka.topic.analytics-events:analytics-events}")
    private String topic;

    @Value("${analytics.reporting.base-url:http://localhost:8091}")
    private String reportingBaseUrl;

    public void publish(AnalyticsEvent event) {
        try {
            if (event.getTimestamp() == null) {
                event.setTimestamp(java.time.LocalDateTime.now());
            }
            // Send as JSON via Spring Kafka JsonSerializer (value type Object -> JSON bytes)
            kafkaTemplate.send(new ProducerRecord<>(topic, event.getEventType(), event));
            log.debug("Analytics event published to Kafka: {}", event.getEventType());
        } catch (Exception ex) {
            log.error("Kafka publish failed, using REST fallback: {}", ex.getMessage());
            restFallback(event);
        }
    }

    private void restFallback(AnalyticsEvent event) {
        try {
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AnalyticsEvent> req = new HttpEntity<>(event, headers);
            rt.postForEntity(reportingBaseUrl + "/api/analytics/events", req, Void.class);
            log.warn("Analytics event sent via REST fallback: {}", event.getEventType());
        } catch (Exception e) {
            log.error("REST fallback failed: {}", e.getMessage(), e);
        }
    }
}
