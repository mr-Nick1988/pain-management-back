package pain_helper_back.analytics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 * Событие: Рекомендация одобрена врачом
 * Публикуется в DoctorServiceImpl.approveRecommendation()
 */
@Getter
public class RecommendationApprovedEvent extends ApplicationEvent {

    private final Long recommendationId;
    private final String doctorId;
    private final String patientMrn;
    private final LocalDateTime approvedAt;
    private final String comment;
    private final Long processingTimeMs;// time from recommendation creation to approval

    public RecommendationApprovedEvent(Object source, Long recommendationId, String doctorId, String patientMrn, LocalDateTime approvedAt, String comment, Long processingTimeMs) {
        super(source);
        this.recommendationId = recommendationId;
        this.doctorId = doctorId;
        this.patientMrn = patientMrn;
        this.approvedAt = approvedAt;
        this.comment = comment;
        this.processingTimeMs = processingTimeMs;
    }
}
