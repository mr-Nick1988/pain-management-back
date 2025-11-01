package pain_helper_back.analytics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Событие: Создана рекомендация
 * Публикуется в NurseServiceImpl.createRecommendation()
 */
@Getter
public class RecommendationCreatedEvent extends ApplicationEvent {
    private final Long recommendationId;
    private final String patientMrn;
    private final List<String> drugName;
    private final List<String> dosage;
    private final String route;
    private final Integer vasLevel;
    private final String createdBy; // nurseId or anesthesiologistId
    private final LocalDateTime createdAt;
    private final Long processingTimeMs;  // TODO это поле - метрика производительности (имеется в LoggingAspect), а не часть бизнес-аудита.
    private final List<String> diagnosisCodes; // ICD коды диагнозов пациента


    public RecommendationCreatedEvent(Object source, Long recommendationId, String patientMrn,
                                      List<String> drugName, List<String> dosage, String route,
                                      Integer vasLevel, String createdBy, LocalDateTime createdAt,
                                      Long processingTimeMs, List<String> diagnosisCodes) {
        super(source);
        this.recommendationId = recommendationId;
        this.patientMrn = patientMrn;
        this.drugName = drugName;
        this.dosage = dosage;
        this.route = route;
        this.vasLevel = vasLevel;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.processingTimeMs = processingTimeMs;
        this.diagnosisCodes = diagnosisCodes;
    }
}