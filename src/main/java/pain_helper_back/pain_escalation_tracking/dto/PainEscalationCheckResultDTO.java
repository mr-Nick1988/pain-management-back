package pain_helper_back.pain_escalation_tracking.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Результат проверки необходимости эскалации боли
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PainEscalationCheckResultDTO {
    private String patientMrn;

    /**
     * Требуется ли эскалация
     */
    private boolean escalationRequired;

    /**
     * Причина эскалации
     */
    private String escalationReason;

    /**
     * Приоритет эскалации: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String escalationPriority;

    /**
     * Текущий VAS
     */
    private Integer currentVas;

    /**
     * Предыдущий VAS
     */
    private Integer previousVas;

    /**
     * Изменение VAS
     */
    private Integer vasChange;

    /**
     * Можно ли ввести следующую дозу
     */
    private boolean canAdministerNextDose;

    /**
     * Время последней дозы
     */
    private LocalDateTime lastDoseTime;

    /**
     * Часов прошло с последней дозы
     */
    private Long hoursSinceLastDose;

    /**
     * Минимальный требуемый интервал (часы)
     */
    private Integer requiredIntervalHours;

    /**
     * Рекомендации для медперсонала
     */
    private String recommendations;

    /**
     * Анализ тренда боли
     */
    private PainTrendAnalysisDTO painTrendAnalysisDTO;
}
