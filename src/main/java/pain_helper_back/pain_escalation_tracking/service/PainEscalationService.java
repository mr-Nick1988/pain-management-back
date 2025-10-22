package pain_helper_back.pain_escalation_tracking.service;

import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.pain_escalation_tracking.controller.PainEscalationController;
import pain_helper_back.pain_escalation_tracking.dto.DoseAdministrationRequestDTO;
import pain_helper_back.pain_escalation_tracking.dto.DoseAdministrationResponseDTO;
import pain_helper_back.pain_escalation_tracking.dto.PainEscalationCheckResult;
import pain_helper_back.pain_escalation_tracking.dto.PainTrendAnalysis;
import pain_helper_back.pain_escalation_tracking.entity.DoseAdministration;

import java.util.List;

/*
 * Сервис для управления эскалацией боли
 * Отслеживает рост боли, управляет дозами и создает эскалации
 */
public interface PainEscalationService {

    /*
     * Проверить, требуется ли эскалация боли для пациента
     *
     * @param mrn MRN пациента
     * @return результат проверки с рекомендациями
     */
    PainEscalationCheckResult checkPainEscalation(String mrn);

    /*
     * Проверить эскалацию с возможностью override VAS
     *
     * @param mrn MRN пациента
     * @param command команда с vasLevelOverride
     * @return результат проверки
     */
    PainEscalationCheckResult checkPainEscalation(String mrn, PainEscalationController.PainEscalationCheckCommand command);

    /*
     * Проверить, можно ли ввести следующую дозу
     *
     * @param mrn MRN пациента
     * @return true если прошло достаточно времени с последней дозы
     */
    boolean canAdministerNextDose(String mrn);

    /*
     * Построить DTO с информацией о доступности следующей дозы
     *
     * @param mrn MRN пациента
     * @return информация о доступности дозы
     */
    PainEscalationController.DoseEligibilityDTO buildDoseEligibility(String mrn);

    /*
     * Зарегистрировать введение дозы (внутренний метод)
     *
     * @param doseAdministration информация о введенной дозе
     * @return сохраненная запись
     */
    DoseAdministration registerDoseAdministration(DoseAdministration doseAdministration);

    /*
     * Зарегистрировать введение дозы через REST API
     *
     * @param mrn MRN пациента
     * @param request DTO с данными о дозе
     * @return DTO ответа
     */
    DoseAdministrationResponseDTO registerDoseAdministration(String mrn, DoseAdministrationRequestDTO request);

    /*
     * Проанализировать тренд боли пациента за последние 24 часа
     *
     * @param mrn MRN пациента
     * @return анализ тренда боли
     */
    PainTrendAnalysis analyzePainTrend(String mrn);

    /*
     * Автоматически обработать новую запись VAS
     * Вызывается при создании нового VAS
     *
     * @param mrn MRN пациента
     * @param newVasLevel новый уровень VAS
     */
    void handleNewVasRecord(String mrn, Integer newVasLevel);

    /*
     * Получить последние эскалации
     *
     * @param limit количество эскалаций
     * @return список эскалаций
     */
    List<Escalation> findRecentEscalations(int limit);

    /*
     * Получить эскалацию по ID
     *
     * @param id ID эскалации
     * @return эскалация
     */
    Escalation findEscalationById(Long id);
}