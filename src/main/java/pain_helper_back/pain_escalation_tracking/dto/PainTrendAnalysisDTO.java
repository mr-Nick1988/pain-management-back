package pain_helper_back.pain_escalation_tracking.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Результат анализа тренда боли пациента
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PainTrendAnalysisDTO {
    private String patientMrn;

    private Integer currentVas;

    private Integer previousVas;

    private Integer vasChange;

    private LocalDateTime lastVasRecordedAt;

    private LocalDateTime previousVasRecordedAt;

    private Long hoursSinceLastVas;

    /**
     * Тренд боли: INCREASING, DECREASING, STABLE
     */
    private String painTrend;

    /**
     * Список VAS за последние 24 часа
     */
    private List<Integer> vasHistory;

    /**
     * Средний VAS за период
     */
    private Double averageVas;

    /**
     * Максимальный VAS за период
     */
    private Integer maxVas;

    /**
     * Минимальный VAS за период
     */
    private Integer minVas;

    /**
     * Количество записей VAS за период
     */
    private Integer vasRecordCount;
}
