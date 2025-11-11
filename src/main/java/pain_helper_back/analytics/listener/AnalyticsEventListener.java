
package pain_helper_back.analytics.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import pain_helper_back.analytics.entity.AnalyticsEvent;
import pain_helper_back.analytics.event.*;
import pain_helper_back.analytics.publisher.AnalyticsPublisher;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/*
 * Слушатель бизнес-событий
 * Асинхронно публикует события в Kafka для обработки микросервисом Analytics-Reporting
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsEventListener {
    private final AnalyticsPublisher analyticsPublisher;

    /*
     * Обработка события: Создан новый сотрудник
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handlePersonCreated(PersonCreatedEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("firstName", event.getFirstName());
            metadata.put("lastName", event.getLastName());
            metadata.put("createdAt", event.getCreatedAt().toString());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("PERSON_CREATED")
                    .userId(event.getCreatedBy())
                    .userRole("ADMIN")
                    .metadata(metadata)
                    .build();

            // Добавляем в metadata информацию о созданном пользователе
            analyticsEvent.getMetadata().put("newPersonId", event.getPersonId());
            analyticsEvent.getMetadata().put("newPersonRole", event.getRole());

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: PERSON_CREATED, personId={}, role={}, createdBy={}",
                    event.getPersonId(), event.getRole(), event.getCreatedBy());

        } catch (Exception e) {
            log.error("Failed to save analytics event for person creation: {}", e.getMessage());
        }
    }
    /*
     * Обработка события: Удален сотрудник
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handlePersonDeleted(PersonDeletedEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("firstName", event.getFirstName());
            metadata.put("lastName", event.getLastName());
            metadata.put("deletedAt", event.getDeletedAt().toString());
            metadata.put("reason", event.getReason());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("PERSON_DELETED")
                    .userId(event.getDeletedBy())
                    .userRole("ADMIN")
                    .metadata(metadata)
                    .build();

            analyticsEvent.getMetadata().put("deletedPersonId", event.getPersonId());
            analyticsEvent.getMetadata().put("deletedPersonRole", event.getRole());

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: PERSON_DELETED, personId={}, role={}, deletedBy={}",
                    event.getPersonId(), event.getRole(), event.getDeletedBy());

        } catch (Exception e) {
            log.error("Failed to save analytics event for person deletion: {}", e.getMessage());
        }
    }
    /*
     * Обработка события: Обновлены данные сотрудника
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handlePersonUpdated(PersonUpdatedEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("updatedAt", event.getUpdatedAt().toString());
            metadata.put("changedFields", event.getChangedFields());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("PERSON_UPDATED")
                    .userId(event.getUpdatedBy())
                    .userRole("ADMIN") // Или определить динамически
                    .metadata(metadata)
                    .build();

            analyticsEvent.getMetadata().put("updatedPersonId", event.getPersonId());

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: PERSON_UPDATED, personId={}, updatedBy={}, fields={}",
                    event.getPersonId(), event.getUpdatedBy(), event.getChangedFields().keySet());

        } catch (Exception e) {
            log.error("Failed to save analytics event for person update: {}", e.getMessage());
        }
    }
    /*
     * Обработка события: Вход пользователя в систему
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handleUserLogin(UserLoginEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("loginAt", event.getLoginAt().toString());
            metadata.put("success", event.getSuccess());
            metadata.put("ipAddress", event.getIpAddress());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType(event.getSuccess() ? "USER_LOGIN_SUCCESS" : "USER_LOGIN_FAILED")
                    .userId(event.getPersonId())
                    .userRole(event.getRole())
                    .status(event.getSuccess() ? "SUCCESS" : "FAILED")
                    .metadata(metadata)
                    .build();

            analyticsPublisher.publish(analyticsEvent);
            if (event.getSuccess()) {
                log.debug("Analytics event published: USER_LOGIN_SUCCESS, personId={}, role={}",
                        event.getPersonId(), event.getRole());
            } else {
                log.debug("Analytics event published: USER_LOGIN_FAILED, personId={}", event.getPersonId());
            }

        } catch (Exception e) {
            log.error("Failed to save analytics event for user login: {}", e.getMessage());
        }
    }
    /*
     * Обработка события: Рекомендация одобрена
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handleRecommendationApproved(RecommendationApprovedEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("comment", event.getComment());
            metadata.put("approvedAt", event.getApprovedAt().toString());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("RECOMMENDATION_APPROVED")
                    .recommendationId(event.getRecommendationId())
                    .patientMrn(event.getPatientMrn())
                    .userId(event.getApprovedBy())
                    .userRole("DOCTOR")
                    .status("APPROVED")
                    .processingTimeMs(event.getProcessingTimeMs())
                    .metadata(metadata)
                    .build();

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: RECOMMENDATION_APPROVED, recommendationId={}, doctorId={}",
                    event.getRecommendationId(), event.getApprovedBy());

        } catch (Exception e) {
            log.error("Failed to save analytics event for recommendation approval: {}", e.getMessage());
        }
    }
    /*
     * Обработка события: Рекомендация отклонена
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handleRecommendationRejected(RecommendationRejectedEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("rejectionReason", event.getRejectionReason());
            metadata.put("comment", event.getComment());
            metadata.put("rejectedAt", event.getRejectedAt().toString());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("RECOMMENDATION_REJECTED")
                    .recommendationId(event.getRecommendationId())
                    .patientMrn(event.getPatientMrn())
                    .userId(event.getRejectedBy())
                    .userRole("DOCTOR")
                    .status("REJECTED")
                    .rejectionReason(event.getRejectionReason())
                    .metadata(metadata)
                    .build();

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: RECOMMENDATION_REJECTED, recommendationId={}, doctorId={}, reason={}",
                    event.getRecommendationId(), event.getRejectedBy(), event.getRejectionReason());

        } catch (Exception e) {
            log.error("Failed to save analytics event for recommendation rejection: {}", e.getMessage());
        }
    }

    /*
     * Обработка события: Пациент зарегистрирован
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handlePatientRegistered(PatientRegisteredEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("age", event.getAge());
            metadata.put("gender", event.getGender());
            metadata.put("registeredAt", event.getRegisteredAt().toString());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("PATIENT_REGISTERED")
                    .patientId(event.getPatientId())
                    .patientMrn(event.getPatientMrn())
                    .userId(event.getRegisteredBy())
                    .userRole(event.getRegisteredByRole()) // NURSE or DOCTOR
                    .metadata(metadata)
                    .build();

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: PATIENT_REGISTERED, patientMrn={}, registeredBy={}, role={}",
                    event.getPatientMrn(), event.getRegisteredBy(), event.getRegisteredByRole());

        } catch (Exception e) {
            log.error("Failed to save analytics event for patient registration: {}", e.getMessage());
        }
    }
    /*
     * Обработка события: Создана запись EMR
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handleEmrCreated(EmrCreatedEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("gfr", event.getGfr() != null ? event.getGfr() : "N/A");
            metadata.put("childPughScore", event.getChildPughScore() != null ? event.getChildPughScore() : "N/A");
            metadata.put("weight", event.getWeight() != null ? event.getWeight() : 0.0);
            metadata.put("height", event.getHeight() != null ? event.getHeight() : 0.0);
            metadata.put("createdAt", event.getCreatedAt().toString());
            
            // Добавляем информацию о диагнозах
            if (event.getDiagnosisCodes() != null && !event.getDiagnosisCodes().isEmpty()) {
                metadata.put("diagnosisCount", event.getDiagnosisCodes().size());
                metadata.put("diagnosisList", String.join(", ", event.getDiagnosisCodes()));
            }

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("EMR_CREATED")
                    .userId(event.getCreatedBy())
                    .userRole(event.getCreatedByRole())
                    .patientMrn(event.getPatientMrn())
                    .diagnosisCodes(event.getDiagnosisCodes()) // Сохраняем ICD коды
                    .diagnosisDescriptions(event.getDiagnosisDescriptions()) // Сохраняем описания
                    .metadata(metadata)
                    .build();

            analyticsPublisher.publish(analyticsEvent);
            log.debug("EMR_CREATED event published: patient={}, createdBy={}, diagnoses={}",
                    event.getPatientMrn(), event.getCreatedBy(),
                    event.getDiagnosisCodes() != null ? event.getDiagnosisCodes().size() : 0);
        } catch (Exception e) {
            log.error("Failed to save EMR_CREATED event: {}", e.getMessage(), e);
        }
    }
    /*
     * Обработка события: VAS записан
     * Различает внутренний (медсестра) и внешний (устройство) источник
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handleVasRecorded(VasRecordedEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("painLocation", event.getPainLocation());
            metadata.put("recordedAt", event.getRecordedAt().toString());
            metadata.put("isCritical", event.getIsCritical());
            metadata.put("vasSource", event.getVasSource()); // INTERNAL или EXTERNAL
            
            // Для внешних источников добавляем deviceId
            if ("EXTERNAL".equals(event.getVasSource()) && event.getDeviceId() != null) {
                metadata.put("deviceId", event.getDeviceId());
            }

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("VAS_RECORDED")
                    .patientMrn(event.getPatientMrn())
                    .userId(event.getRecordedBy())
                    .userRole("EXTERNAL".equals(event.getVasSource()) ? "EXTERNAL_SYSTEM" : "NURSE")
                    .vasLevel(event.getVasLevel())
                    .priority(event.getIsCritical() ? "HIGH" : "NORMAL")
                    .metadata(metadata)
                    .build();

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: VAS_RECORDED, patientMrn={}, vasLevel={}, critical={}, source={}, device={}",
                    event.getPatientMrn(), event.getVasLevel(), event.getIsCritical(), 
                    event.getVasSource(), event.getDeviceId());

        } catch (Exception e) {
            log.error("Failed to save analytics event for VAS recording: {}", e.getMessage());
        }
    }
    /*
     * Обработка события: Создана рекомендация
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handleRecommendationCreated(RecommendationCreatedEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("drugName", event.getDrugName());
            metadata.put("dosage", event.getDosage());
            metadata.put("route", event.getRoute());
            metadata.put("createdAt", event.getCreatedAt().toString());

            // Добавляем информацию о диагнозах
            if (event.getDiagnosisCodes() != null && !event.getDiagnosisCodes().isEmpty()) {
                metadata.put("diagnosisCount", event.getDiagnosisCodes().size());
                metadata.put("diagnosisList", String.join(", ", event.getDiagnosisCodes()));
            }

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("RECOMMENDATION_CREATED")
                    .recommendationId(event.getRecommendationId())
                    .patientMrn(event.getPatientMrn())
                    .userId(event.getCreatedBy())
                    .userRole("NURSE") // Обычно медсестра генерирует рекомендацию
                    .vasLevel(event.getVasLevel())
                    .processingTimeMs(event.getProcessingTimeMs())
                    .diagnosisCodes(event.getDiagnosisCodes()) // Сохраняем ICD коды
                    .metadata(metadata)
                    .build();

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: RECOMMENDATION_CREATED, recommendationId={}, patientMrn={}, drug={}, vasLevel={}, diagnoses={}",
                    event.getRecommendationId(), event.getPatientMrn(), event.getDrugName(), event.getVasLevel(),
                    event.getDiagnosisCodes() != null ? event.getDiagnosisCodes().size() : 0);

        } catch (Exception e) {
            log.error("Failed to save analytics event for recommendation creation: {}", e.getMessage());
        }
    }

    /*
     * Обработка события: Введена доза препарата
     */
    @EventListener
    @Async("analyticsTaskExecutor")
    public void handleDoseAdministered(DoseAdministeredEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("drugName", event.getDrugName());
            metadata.put("dosage", event.getDosage());
            metadata.put("unit", event.getUnit());
            metadata.put("administeredBy", event.getAdministeredBy());
            metadata.put("timestamp", event.getTimestamp());

            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("DOSE_ADMINISTERED")
                    .patientMrn(event.getPatientMrn())
                    .userId(event.getAdministeredBy())
                    .userRole("NURSE") // Обычно медсестра вводит дозу
                    .metadata(metadata)
                    .build();

            analyticsPublisher.publish(analyticsEvent);
            log.debug("Analytics event published: DOSE_ADMINISTERED, patientMrn={}, drug={} {}{}, administeredBy={}",
                    event.getPatientMrn(), event.getDosage(), event.getUnit(), event.getDrugName(), event.getAdministeredBy());

        } catch (Exception e) {
            log.error("Failed to save analytics event for dose administration: {}", e.getMessage());
        }
    }
}

