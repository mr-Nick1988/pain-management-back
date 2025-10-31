package pain_helper_back.pain_escalation_tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO: Анализ тренда боли пациента
 * Используется для:
 *  - визуализации графиков VAS на UI (frontend),
 *  - анализа динамики боли в аналитике,
 *  - передачи краткой статистики о боли.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PainTrendAnalysisDTO {

    /** Уникальный медицинский номер пациента (MRN) */
    private String patientMrn;

    /** Текущий уровень боли (VAS) */
    private Integer currentVas;

    /** Предыдущий зарегистрированный уровень боли (VAS) */
    private Integer previousVas;

    /** Изменение боли между последними двумя записями */
    private Integer vasChange;

    /** Дата и время последней жалобы (VAS) */
    private LocalDateTime lastVasRecordedAt;

    /** Дата и время предыдущей жалобы */
    private LocalDateTime previousVasRecordedAt;

    /** Кол-во часов между последними двумя жалобами */
    private int daysBetweenVasRecords;

    /**
     * Тренд боли:
     *  - "INCREASING" — боль усиливается
     *  - "DECREASING" — боль снижается
     *  - "STABLE" — без изменений
     */
    private String painTrend;


    /** Средний уровень боли за период */
    private Double averageVas;

    /** Максимальный уровень боли за период */
    private Integer maxVas;

    /** Минимальный уровень боли за период */
    private Integer minVas;

    /** История уровней боли за весь период */
    private List<Integer> vasHistory;

    /** Количество зарегистрированных записей VAS за период */
    private Integer vasRecordCount;
}