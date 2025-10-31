package pain_helper_back.pain_escalation_tracking.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO: Push-уведомление об эскалации боли
 * Используется WebSocket-слоем для real-time уведомлений:
 * - при скачке боли ≥ 2 пунктов,
 * - при критических значениях VAS (например ≥ 7).
 */
@Value
@Builder
public class PainEscalationNotificationDTO {

    /** Идентификатор записи эскалации (PainEscalation entity) */
    Long escalationId;

    /** Медицинский номер пациента (MRN) */
    String patientMrn;

    /** Полное имя пациента */
    String patientName;

    /** Текущий уровень боли (VAS) */
    Integer currentVas;

    /** Предыдущий уровень боли (VAS) */
    Integer previousVas;

    /** Разница между текущим и предыдущим VAS */
    Integer vasChange;

    /** Приоритет для UI (INFO / ALERT / CRITICAL) */
    String priority;


    /** Время создания записи PainEscalation */
    LocalDateTime createdAt;

    /** Последние диагнозы пациента (для контекста уведомления) */
    List<String> latestDiagnoses;
}