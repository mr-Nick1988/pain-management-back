package pain_helper_back.pain_escalation_tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pain_helper_back.enums.EscalationPriority;

/**
 * DTO: Результат проверки роста боли (Pain Escalation Check)
 * Используется внутри сервиса PainEscalationServiceImpl,
 * чтобы определить, нужно ли уведомить анестезиолога.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PainEscalationCheckResultDTO {

    /** Уникальный медицинский номер пациента (MRN) */
    private String patientMrn;

    /** Флаг: требуется ли эскалация (уведомление) */
    private boolean escalationRequired;

    /** Краткая причина, например: "VAS increased by 3 points" */
    private String escalationReason;

    /**
     * Приоритет уведомления (не бизнес-приоритет):
     LOW,
     MEDIUM,
     HIGH,
     CRITICAL
     */
    private EscalationPriority escalationPriority;

    /** Текущий уровень боли (VAS) */
    private Integer currentVas;

    /** Предыдущий уровень боли (VAS) */
    private Integer previousVas;

    /** Изменение боли между последними записями */
    private Integer vasChange;

    /** Текст рекомендаций / действий для анестезиолога */
    private String recommendations;

    /** Вложенный объект анализа тренда боли */
    private PainTrendAnalysisDTO painTrendAnalysisDTO;
}