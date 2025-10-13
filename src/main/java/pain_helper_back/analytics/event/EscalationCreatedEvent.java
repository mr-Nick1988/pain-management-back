package pain_helper_back.analytics.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import pain_helper_back.enums.EscalationPriority;

import java.time.LocalDateTime;

/*
 * Событие: Создана эскалация (автоматически при отклонении врачом)
 * Публикуется в DoctorServiceImpl.rejectRecommendation()
 */
@Getter
public class EscalationCreatedEvent extends ApplicationEvent {
    private final Long escalationId;
    private final Long recommendationId;
    private final String escalatedBy; // doctorId
    private final String patientMrn;
    private final LocalDateTime escalatedAt;
    private final EscalationPriority priority; // HIGH, MEDIUM, LOW
    private final String escalationReason;
    private final Integer vasLevel;

    public EscalationCreatedEvent(Object source, Long escalationId, Long recommendationId,
                                  String escalatedBy, String patientMrn, LocalDateTime escalatedAt,
                                  EscalationPriority priority, String escalationReason, Integer vasLevel) {
        super(source);
        this.escalationId = escalationId;
        this.recommendationId = recommendationId;
        this.escalatedBy = escalatedBy;
        this.patientMrn = patientMrn;
        this.escalatedAt = escalatedAt;
        this.priority = priority;
        this.escalationReason = escalationReason;
        this.vasLevel = vasLevel;
    }
}
