package pain_helper_back.analytics.publisher;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pain_helper_back.analytics.entity.AnalyticsEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Публикует события аналитики напрямую в Kafka.
 * Заменяет связку Spring Events + AnalyticsEventListener.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.analytics-events:analytics-events}")
    private String topic;

    // ==================== PATIENT EVENTS ====================

    public void publishPatientRegistered(Long patientId, String patientMrn, String registeredBy,
                                         String registeredByRole, Integer age, String gender) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("age", age);
        metadata.put("gender", gender);
        metadata.put("registeredAt", LocalDateTime.now().toString());

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("PATIENT_REGISTERED")
                .patientId(patientId)
                .patientMrn(patientMrn)
                .userId(registeredBy)
                .userRole(registeredByRole)
                .metadata(metadata)
                .build();

        publish(event);
    }

    // ==================== EMR EVENTS ====================

    public void publishEmrCreated(String patientMrn, String createdBy, String createdByRole,
                                  String gfr, String childPughScore, Double weight, Double height,
                                  List<String> diagnosisCodes, List<String> diagnosisDescriptions) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("gfr", gfr != null ? gfr : "N/A");
        metadata.put("childPughScore", childPughScore != null ? childPughScore : "N/A");
        metadata.put("weight", weight != null ? weight : 0.0);
        metadata.put("height", height != null ? height : 0.0);
        metadata.put("createdAt", LocalDateTime.now().toString());

        if (diagnosisCodes != null && !diagnosisCodes.isEmpty()) {
            metadata.put("diagnosisCount", diagnosisCodes.size());
            metadata.put("diagnosisList", String.join(", ", diagnosisCodes));
        }

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("EMR_CREATED")
                .patientMrn(patientMrn)
                .userId(createdBy)
                .userRole(createdByRole)
                .diagnosisCodes(diagnosisCodes)
                .diagnosisDescriptions(diagnosisDescriptions)
                .metadata(metadata)
                .build();

        publish(event);
    }

    // ==================== VAS EVENTS ====================

    public void publishVasRecorded(String patientMrn, Integer vasLevel, String painLocation,
                                   String recordedBy, Boolean isCritical, String vasSource, String deviceId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("painLocation", painLocation);
        metadata.put("recordedAt", LocalDateTime.now().toString());
        metadata.put("isCritical", isCritical);
        metadata.put("vasSource", vasSource);

        if ("EXTERNAL".equals(vasSource) && deviceId != null) {
            metadata.put("deviceId", deviceId);
        }

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("VAS_RECORDED")
                .patientMrn(patientMrn)
                .userId(recordedBy)
                .userRole("EXTERNAL".equals(vasSource) ? "EXTERNAL_SYSTEM" : "NURSE")
                .vasLevel(vasLevel)
                .priority(isCritical ? "HIGH" : "NORMAL")
                .metadata(metadata)
                .build();

        publish(event);
    }

    // ==================== RECOMMENDATION EVENTS ====================

    public void publishRecommendationCreated(Long recommendationId, String patientMrn, String createdBy,
                                             Integer vasLevel, String drugName, String dosage, String route,
                                             Long processingTimeMs, List<String> diagnosisCodes) {
        // Backward-compatible wrapper defaults role to NURSE
        publishRecommendationCreated(recommendationId, patientMrn, createdBy,
                "NURSE", vasLevel, drugName, dosage, route, processingTimeMs, diagnosisCodes);
    }

    public void publishRecommendationCreated(Long recommendationId, String patientMrn, String createdBy,
                                             String userRole, Integer vasLevel, String drugName, String dosage, String route,
                                             Long processingTimeMs, List<String> diagnosisCodes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("drugName", drugName);
        metadata.put("dosage", dosage);
        metadata.put("route", route);
        metadata.put("createdAt", LocalDateTime.now().toString());

        if (diagnosisCodes != null && !diagnosisCodes.isEmpty()) {
            metadata.put("diagnosisCount", diagnosisCodes.size());
            metadata.put("diagnosisList", String.join(", ", diagnosisCodes));
        }

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("RECOMMENDATION_CREATED")
                .recommendationId(recommendationId)
                .patientMrn(patientMrn)
                .userId(createdBy)
                .userRole(userRole)
                .vasLevel(vasLevel)
                .processingTimeMs(processingTimeMs)
                .diagnosisCodes(diagnosisCodes)
                .metadata(metadata)
                .build();

        publish(event);
    }

    public void publishRecommendationApproved(Long recommendationId, String patientMrn, String approvedBy,
                                              String comment, Long processingTimeMs) {
        // Backward-compatible wrapper defaults role to DOCTOR
        publishRecommendationApproved(recommendationId, patientMrn, approvedBy, "DOCTOR", comment, processingTimeMs);
    }

    public void publishRecommendationApproved(Long recommendationId, String patientMrn, String approvedBy,
                                              String userRole, String comment, Long processingTimeMs) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("comment", comment);
        metadata.put("approvedAt", LocalDateTime.now().toString());

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("RECOMMENDATION_APPROVED")
                .recommendationId(recommendationId)
                .patientMrn(patientMrn)
                .userId(approvedBy)
                .userRole(userRole)
                .status("APPROVED")
                .processingTimeMs(processingTimeMs)
                .metadata(metadata)
                .build();

        publish(event);
    }

    public void publishRecommendationRejected(Long recommendationId, String patientMrn, String rejectedBy,
                                              String rejectionReason, String comment) {
        // Backward-compatible wrapper defaults role to DOCTOR
        publishRecommendationRejected(recommendationId, patientMrn, rejectedBy, "DOCTOR", rejectionReason, comment);
    }

    public void publishRecommendationRejected(Long recommendationId, String patientMrn, String rejectedBy,
                                              String userRole, String rejectionReason, String comment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rejectionReason", rejectionReason);
        metadata.put("comment", comment);
        metadata.put("rejectedAt", LocalDateTime.now().toString());

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("RECOMMENDATION_REJECTED")
                .recommendationId(recommendationId)
                .patientMrn(patientMrn)
                .userId(rejectedBy)
                .userRole(userRole)
                .status("REJECTED")
                .rejectionReason(rejectionReason)
                .metadata(metadata)
                .build();

        publish(event);
    }

    // ==================== DOSE EVENTS ====================

    public void publishDoseAdministered(String patientMrn, String drugName, Double dosage,
                                        String unit, String administeredBy) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("drugName", drugName);
        metadata.put("dosage", dosage);
        metadata.put("unit", unit);
        metadata.put("administeredBy", administeredBy);
        metadata.put("timestamp", LocalDateTime.now().toString());

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("DOSE_ADMINISTERED")
                .patientMrn(patientMrn)
                .userId(administeredBy)
                .userRole("NURSE")
                .metadata(metadata)
                .build();

        publish(event);
    }

    // ==================== USER EVENTS ====================

    public void publishUserLogin(String personId, String role, Boolean success, String ipAddress) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("loginAt", LocalDateTime.now().toString());
        metadata.put("success", success);
        metadata.put("ipAddress", ipAddress);

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType(success ? "USER_LOGIN_SUCCESS" : "USER_LOGIN_FAILED")
                .userId(personId)
                .userRole(role)
                .status(success ? "SUCCESS" : "FAILED")
                .metadata(metadata)
                .build();

        publish(event);
    }

    public void publishPersonCreated(String personId, String role, String firstName, String lastName, String createdBy) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("firstName", firstName);
        metadata.put("lastName", lastName);
        metadata.put("createdAt", LocalDateTime.now().toString());
        metadata.put("newPersonId", personId);
        metadata.put("newPersonRole", role);

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("PERSON_CREATED")
                .userId(createdBy)
                .userRole("ADMIN")
                .metadata(metadata)
                .build();

        publish(event);
    }

    public void publishPersonDeleted(String personId, String role, String firstName, String lastName,
                                     String deletedBy, String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("firstName", firstName);
        metadata.put("lastName", lastName);
        metadata.put("deletedAt", LocalDateTime.now().toString());
        metadata.put("reason", reason);
        metadata.put("deletedPersonId", personId);
        metadata.put("deletedPersonRole", role);

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("PERSON_DELETED")
                .userId(deletedBy)
                .userRole("ADMIN")
                .metadata(metadata)
                .build();

        publish(event);
    }

    public void publishPersonUpdated(String personId, String updatedBy, Map<String, Object> changedFields) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("updatedAt", LocalDateTime.now().toString());
        metadata.put("changedFields", changedFields);
        metadata.put("updatedPersonId", personId);

        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("PERSON_UPDATED")
                .userId(updatedBy)
                .userRole("ADMIN")
                .metadata(metadata)
                .build();

        publish(event);
    }

    // ==================== ESCALATION EVENTS ====================

    public void publishEscalationCreated(Long escalationId, String priority, Integer vasLevel) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("ESCALATION_CREATED")
                .escalationId(escalationId)
                .priority(priority)
                .vasLevel(vasLevel)
                .build();

        publish(event);
    }

    public void publishEscalationResolved(Long escalationId, Long processingTimeMs) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .timestamp(LocalDateTime.now())
                .eventType("ESCALATION_RESOLVED")
                .escalationId(escalationId)
                .processingTimeMs(processingTimeMs)
                .build();

        publish(event);
    }

    // ==================== CORE PUBLISH METHOD ====================

    private void publish(AnalyticsEvent event) {
        try {
            if (event.getTimestamp() == null) {
                event.setTimestamp(LocalDateTime.now());
            }
            kafkaTemplate.send(new ProducerRecord<>(topic, event.getEventType(), event));
            log.debug("Analytics event published to Kafka: {}", event.getEventType());
        } catch (Exception ex) {
            log.error("Failed to publish analytics event to Kafka: {}", ex.getMessage(), ex);
            // Можно добавить REST fallback если нужно
        }
    }
}

