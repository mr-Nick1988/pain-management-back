package pain_helper_back.pain_escalation_tracking.service;
import pain_helper_back.pain_escalation_tracking.dto.PainTrendAnalysisDTO;

/**
 * Сервис анализа и отслеживания роста боли (Pain Escalation Tracking).
 * <p>
 * Отвечает за:
 * - анализ тренда боли пациента по VAS (для UI-графиков и аналитики);
 * - обработку новой жалобы пациента и определение необходимости уведомления.
 */

public interface PainEscalationService {

    /**
     * Анализирует историю VAS за последние 24 часа для визуализации и аналитики.
     *
     * @param mrn медицинский номер пациента
     * @return DTO с данными по тренду боли
     */
    PainTrendAnalysisDTO analyzePainTrend(String mrn);

    /**
     * Обрабатывает новую жалобу пациента (новое значение VAS).
     * Если боль выросла на ≥ 2 балла — создаёт PainEscalation и отправляет уведомление анестезиологу.
     *
     * @param mrn         медицинский номер пациента
     * @param newVasLevel новый уровень боли (VAS)
     */
    void handleNewVasRecord(String mrn, Integer newVasLevel);
}