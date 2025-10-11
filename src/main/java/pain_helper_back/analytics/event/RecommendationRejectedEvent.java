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
    private final String doctorId;
    private final String patientMrn;
    private final LocalDateTime rejectedAt;
    private final String rejectionReason;
    private final String comment;

    public RecommendationRejectedEvent(Object source, Long recommendationId, String doctorId,
                                       String patientMrn, LocalDateTime rejectedAt,
                                       String rejectionReason, String comment) {
        super(source);
        this.recommendationId = recommendationId;
        this.doctorId = doctorId;
        this.patientMrn = patientMrn;
        this.rejectedAt = rejectedAt;
        this.rejectionReason = rejectionReason;
        this.comment = comment;
    }
}
