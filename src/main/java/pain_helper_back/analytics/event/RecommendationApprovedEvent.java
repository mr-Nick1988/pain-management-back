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
    private final String approvedBy; //  может быть doctorId или anesthesiologistId
    private final String role;       // "DOCTOR" или "ANESTHESIOLOGIST"
    private final String patientMrn;
    private final LocalDateTime approvedAt;
    private final String comment;
    private final Long processingTimeMs;// time from recommendation creation to approval

    public RecommendationApprovedEvent(Object source, Long recommendationId, String doctorId, String role, String patientMrn, LocalDateTime approvedAt, String comment, Long processingTimeMs) {
        super(source);
        this.recommendationId = recommendationId;
        this.approvedBy = doctorId;
        this.role = role;
        this.patientMrn = patientMrn;
        this.approvedAt = approvedAt;
        this.comment = comment;
        this.processingTimeMs = processingTimeMs;
    }
}
