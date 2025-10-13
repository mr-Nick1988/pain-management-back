package pain_helper_back.analytics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 * Событие: Записан уровень боли (VAS) пациента
 * Публикуется в NurseServiceImpl.recordVas()
 */
@Getter
public class VasRecordedEvent extends ApplicationEvent {

    private final Long vasId;
    private final String patientMrn;
    private final String recordedBy; // nurseId
    private final LocalDateTime recordedAt;
    private final Integer vasLevel; // 0-10
    private final String painLocation;
    private final Boolean isCritical; // true if VAS >= 8

    public VasRecordedEvent(Object source, Long vasId, String patientMrn,
                            String recordedBy, LocalDateTime recordedAt,
                            Integer vasLevel, String painLocation, Boolean isCritical) {
        super(source);
        this.vasId = vasId;
        this.patientMrn = patientMrn;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
        this.vasLevel = vasLevel;
        this.painLocation = painLocation;
        this.isCritical = isCritical;
    }
}