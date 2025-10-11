package pain_helper_back.analytics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 * Событие: Эскалация разрешена анестезиологом
 * Публикуется в AnesthesiologistServiceImpl.approveEscalation()
 */
@Getter
public class EscalationResolvedEvent extends ApplicationEvent {

    private final Long escalationId;
    private final Long recommendationId;
    private final String resolvedBy; // anesthesiologistId
    private final String patientMrn;
    private final LocalDateTime resolvedAt;
    private final Boolean approved; // true = approve, false = reject
    private final String resolution;
    private final Long resolutionTimeMs; //time from escalation creation to resolution

    public EscalationResolvedEvent(Object source, Long escalationId, Long recommendationId,
                                   String resolvedBy, String patientMrn, LocalDateTime resolvedAt,
                                   Boolean approved, String resolution, Long resolutionTimeMs) {
        super(source);
        this.escalationId = escalationId;
        this.recommendationId = recommendationId;
        this.resolvedBy = resolvedBy;
        this.patientMrn = patientMrn;
        this.resolvedAt = resolvedAt;
        this.approved = approved;
        this.resolution = resolution;
        this.resolutionTimeMs = resolutionTimeMs;
    }
}
