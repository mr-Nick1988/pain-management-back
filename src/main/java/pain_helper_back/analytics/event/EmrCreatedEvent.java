package pain_helper_back.analytics.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 * Событие: Создана запись EMR (Electronic Medical Record)
 * Публикуется в NurseServiceImpl.createEmr() ИЛИ DoctorServiceImpl.createEmr()
 */
@Getter
public class EmrCreatedEvent extends ApplicationEvent {
    private final Long emrId;
    private final String patientMrn;
    private final String createdBy; // nurseId or doctorId
    private final String createdByRole; // "NURSE" or "DOCTOR"
    private final LocalDateTime createdAt;
    private final String gfr;
    private final String childPughScore;
    private final Double weight;
    private final Double height;

    public EmrCreatedEvent(Object source, Long emrId, String patientMrn,
                           String createdBy, String createdByRole, LocalDateTime createdAt,
                           String gfr, String childPughScore, Double weight, Double height) {
        super(source);
        this.emrId = emrId;
        this.patientMrn = patientMrn;
        this.createdBy = createdBy;
        this.createdByRole = createdByRole;
        this.createdAt = createdAt;
        this.gfr = gfr;
        this.childPughScore = childPughScore;
        this.weight = weight;
        this.height = height;
    }
}