package pain_helper_back.analytics.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 * Событие: Рекомендация отклонена врачом
 * Публикуется в DoctorServiceImpl.rejectRecommendation()
 */
@Getter
public class RecommendationRejectedEvent extends ApplicationEvent {

    private final Long recommendationId;
    private final String rejectedBy; // может быть id как доктора, так и анестезиолога
    private final String role;
    private final String patientMrn;
    private final LocalDateTime rejectedAt;
    private final String rejectionReason;
    private final String comment;
    private final Long lifeCycleMs; // может быть null

    public RecommendationRejectedEvent(Object source, Long recommendationId, String doctorId, String role,
                                       String patientMrn, LocalDateTime rejectedAt,
                                       String rejectionReason, String comment, Long lifeCycleMs) {
        super(source);
        this.recommendationId = recommendationId;
        this.rejectedBy = doctorId;
        this.role = role;
        this.patientMrn = patientMrn;
        this.rejectedAt = rejectedAt;
        this.rejectionReason = rejectionReason;
        this.comment = comment;
        this.lifeCycleMs = lifeCycleMs;
    }
}
