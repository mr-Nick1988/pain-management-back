package pain_helper_back.analytics.entity;


import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * MongoDB документ для хранения бизнес-событий
 * Используется для аналитики и отчетности
 */
@Document(collection = "analytics_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {
    @Id
    private String id;
    @Indexed
    private LocalDateTime timestamp;
    //type of event
    @Indexed
    private String eventType; //RECOMMENDATION_APPROVED, ESCALATION_CREATED, PATIENT_REGISTERED, etc.
    //related entities
    private Long recommendationId;
    private Long escalationId;
    private Long patientId;
    private String patientMrn;
    //user triggering event
    @Indexed
    private String userId;
    private String userRole;//ANESTHESIOLOGIST, ADMIN, DOCTOR etc.
    //additional data
    private Map<String, Object> metadata;
    // business metrics
    private Integer vasLevel;//pain level
    private String priority;//  HIGH, MEDIUM, LOW (for escalations)
    private String status;//APPROVED, REJECTED, PENDING
    private String rejectionReason;
    //KPI calculation
    private Long processingTimeMs;
    // Диагнозы пациента (ICD коды)
    @Indexed
    private List<String> diagnosisCodes; // Для аналитики по диагнозам
    private List<String> diagnosisDescriptions; // Описания для читаемости
}
