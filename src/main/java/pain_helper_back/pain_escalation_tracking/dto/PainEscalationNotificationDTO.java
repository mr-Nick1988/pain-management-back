package pain_helper_back.pain_escalation_tracking.dto;

import lombok.Builder;
import lombok.Value;
import pain_helper_back.enums.EscalationPriority;

import java.time.LocalDateTime;
import java.util.List;

/*
 * DTO для push-уведомлений об эскалации боли
 * Отправляется через WebSocket подписчикам (врач, анестезиолог, dashboard)
 */
@Value
@Builder
public class PainEscalationNotificationDTO {
    Long escalationId;
    Long recommendationId;
    String patientMrn;
    String patientName;
    Integer currentVas;
    Integer previousVas;
    Integer vasChange;
    String escalationReason;
    EscalationPriority priority;
    String recommendations;
    LocalDateTime createdAt;
    List<String> latestDiagnoses;
}
