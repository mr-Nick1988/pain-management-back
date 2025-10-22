package pain_helper_back.emr_recalculation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.external_emr_integration_service.dto.EmrChangeAlertDTO;
import pain_helper_back.external_emr_integration_service.service.EmrChangeDetectionService;
import pain_helper_back.external_emr_integration_service.service.WebSocketNotificationService;
import pain_helper_back.treatment_protocol.service.TreatmentProtocolService;
import pain_helper_back.websocket.dto.UnifiedNotificationDTO;
import pain_helper_back.websocket.service.UnifiedNotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/*
 * СЕРВИС АВТОМАТИЧЕСКОГО ПЕРЕСЧЕТА РЕКОМЕНДАЦИЙ ПРИ ИЗМЕНЕНИИ EMR.
 * 
 * ОСНОВНЫЕ ФУНКЦИИ:
 * - Обнаружение критических изменений в EMR (GFR, PLT, WBC, SAT и т.д.)
 * - Автоматический пересчет рекомендаций при критических изменениях
 * - Отправка уведомлений врачам о необходимости пересмотра назначений
 * - Маркировка старых рекомендаций как требующих пересмотра
 * - Генерация новых рекомендаций с учетом обновленных данных EMR
 * 
 * КОГДА СРАБАТЫВАЕТ:
 * 1. При синхронизации EMR из внешней системы (EmrSyncScheduler)
 * 2. При ручном обновлении EMR через API
 * 3. При обнаружении критических изменений лабораторных показателей
 * 
 * КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ:
 * - GFR < 30 или падение > 20 единиц
 * - PLT < 50 (риск кровотечения)
 * - WBC < 2.0 (иммунодефицит)
 * - SAT < 90 (критическая гипоксия)
 * - Натрий < 125 или > 155
 * 
 * АЛГОРИТМ РАБОТЫ:
 * 1. Получить старый и новый EMR
 * 2. Обнаружить критические изменения
 * 3. Если есть критические изменения:
 *    a. Найти все активные рекомендации пациента
 *    b. Пометить их как REQUIRES_REVIEW
 *    c. Сгенерировать новые рекомендации с учетом новых данных
 *    d. Отправить уведомления врачам
 * 4. Отправить WebSocket уведомления
 * 
 * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
 * // В EmrSyncScheduler после обновления EMR:
 * emrRecalculationService.handleEmrChange(patient, oldEmr, newEmr);
 * 
 * // В NurseServiceImpl после ручного обновления EMR:
 * emrRecalculationService.handleEmrChange(patient, oldEmr, updatedEmr);
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmrRecalculationService {

    private final EmrChangeDetectionService changeDetectionService;
    private final TreatmentProtocolService treatmentProtocolService;
    private final RecommendationRepository recommendationRepository;
    private final PatientRepository patientRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final UnifiedNotificationService unifiedNotificationService;

    /*
     * Обработать изменение EMR и пересчитать рекомендации при необходимости.
     * 
     * @param patient Пациент
     * @param oldEmr Старые данные EMR
     * @param newEmr Новые данные EMR
     */
    @Transactional
    public void handleEmrChange(Patient patient, Emr oldEmr, Emr newEmr) {
        log.info("Handling EMR change for patient: {}", patient.getMrn());

        // Проверяем, есть ли изменения
        boolean hasChanges = changeDetectionService.detectChanges(oldEmr, newEmr);
        if (!hasChanges) {
            log.debug("No EMR changes detected for patient {}", patient.getMrn());
            return;
        }

        log.info("EMR changes detected for patient {}", patient.getMrn());

        // Проверяем критические изменения
        List<EmrChangeAlertDTO> criticalAlerts = changeDetectionService.checkCriticalChanges(
                oldEmr, newEmr, patient.getMrn());

        if (criticalAlerts.isEmpty()) {
            log.debug("No critical EMR changes for patient {}", patient.getMrn());
            return;
        }

        log.warn("CRITICAL EMR changes detected for patient {}: {} alerts", 
                patient.getMrn(), criticalAlerts.size());

        // Обрабатываем критические изменения
        handleCriticalEmrChanges(patient, criticalAlerts, newEmr);
    }

    /*
     * Обработать критические изменения EMR.
     * 
     * @param patient Пациент
     * @param alerts Список критических алертов
     * @param newEmr Новые данные EMR
     */
    @Transactional
    public void handleCriticalEmrChanges(Patient patient, List<EmrChangeAlertDTO> alerts, Emr newEmr) {
        log.info("Processing {} critical EMR changes for patient {}", alerts.size(), patient.getMrn());

        // 1. Найти все активные рекомендации пациента
        List<Recommendation> activeRecommendations = recommendationRepository
                .findByPatientMrnAndStatus(patient.getMrn(), RecommendationStatus.APPROVED);

        if (activeRecommendations.isEmpty()) {
            log.info("No active recommendations found for patient {}", patient.getMrn());
        } else {
            log.info("Found {} active recommendations for patient {}", 
                    activeRecommendations.size(), patient.getMrn());

            // 2. Пометить все активные рекомендации как требующие пересмотра
            markRecommendationsForReview(activeRecommendations, alerts);
        }

        // 3. Сгенерировать новые рекомендации с учетом обновленных данных EMR
        generateUpdatedRecommendations(patient, newEmr, alerts);

        // 4. Отправить WebSocket уведомления
        sendEmrChangeNotifications(patient, alerts);

        log.info("Critical EMR changes processed successfully for patient {}", patient.getMrn());
    }

    /*
     * Пометить рекомендации как требующие пересмотра.
     * 
     * @param recommendations Список рекомендаций
     * @param alerts Список алертов
     */
    @Transactional
    public void markRecommendationsForReview(List<Recommendation> recommendations, 
                                            List<EmrChangeAlertDTO> alerts) {
        String reviewReason = buildReviewReason(alerts);

        for (Recommendation recommendation : recommendations) {
            recommendation.setStatus(RecommendationStatus.REQUIRES_REVIEW);
            recommendation.setReviewReason(reviewReason);
            recommendation.setReviewRequestedAt(LocalDateTime.now());
            recommendationRepository.save(recommendation);

            log.info("Recommendation {} marked for review: {}", 
                    recommendation.getId(), reviewReason);
        }
    }

    /*
     * Сгенерировать обновленные рекомендации с учетом новых данных EMR.
     * 
     * @param patient Пациент
     * @param newEmr Новые данные EMR
     * @param alerts Список алертов
     */
    @Transactional
    public void generateUpdatedRecommendations(Patient patient, Emr newEmr,
                                               List<EmrChangeAlertDTO> alerts) {
        try {
            log.info("Generating updated recommendations for patient {} with new EMR data",
                    patient.getMrn());

            // Получаем последний VAS пациента
            Vas lastVas = patient.getVas() != null && !patient.getVas().isEmpty()
                    ? patient.getVas().getLast()
                    : null;

            if (lastVas == null) {
                log.warn("Cannot generate recommendations: no VAS data for patient {}",
                        patient.getMrn());
                return;
            }

            // Генерируем новую рекомендацию с учетом обновленных данных EMR
            Recommendation newRecommendation = treatmentProtocolService.generateRecommendation(
                    lastVas, patient);

            // Добавляем примечание о причине пересчета
            String recalculationNote = String.format(
                    "Рекомендация пересчитана автоматически из-за критических изменений EMR: %s",
                    buildReviewReason(alerts));

            newRecommendation.setJustification(
                    newRecommendation.getJustification() + "\n\n" + recalculationNote);
            newRecommendation.setStatus(RecommendationStatus.PENDING);
            newRecommendation.setCreatedAt(LocalDateTime.now());

            // Сохраняем новую рекомендацию
            Recommendation saved = recommendationRepository.save(newRecommendation);

            log.info("New recommendation {} generated for patient {} due to EMR changes",
                    saved.getId(), patient.getMrn());

            // Отправляем уведомление о новой рекомендации
            sendNewRecommendationNotification(patient, saved, alerts);

        } catch (Exception e) {
            log.error("Failed to generate updated recommendations for patient {}: {}",
                    patient.getMrn(), e.getMessage(), e);
        }
    }
    /*
     * Построить причину пересмотра из списка алертов.
     * 
     * @param alerts Список алертов
     * @return Строка с описанием причин
     */
    private String buildReviewReason(List<EmrChangeAlertDTO> alerts) {
        return alerts.stream()
                .map(alert -> String.format("%s: %s → %s (%s)",
                        alert.getParameterName(),
                        alert.getOldValue(),
                        alert.getNewValue(),
                        alert.getChangeDescription()))
                .collect(Collectors.joining("; "));
    }

    /*
     * Отправить WebSocket уведомления об изменениях EMR.
     * 
     * @param patient Пациент
     * @param alerts Список алертов
     */
    private void sendEmrChangeNotifications(Patient patient, List<EmrChangeAlertDTO> alerts) {
        try {
            // Отправляем legacy уведомления
            for (EmrChangeAlertDTO alert : alerts) {
                alert.setPatientMrn(patient.getMrn());
                alert.setPatientName(patient.getFirstName() + " " + patient.getLastName());
                webSocketNotificationService.sendCriticalAlert(alert);
            }

            // Отправляем унифицированные уведомления
            for (EmrChangeAlertDTO alert : alerts) {
                UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                        .type(UnifiedNotificationDTO.NotificationType.EMR_ALERT)
                        .priority(UnifiedNotificationDTO.NotificationPriority.CRITICAL)
                        .patientMrn(patient.getMrn())
                        .patientName(patient.getFirstName() + " " + patient.getLastName())
                        .title("Критическое изменение EMR: " + alert.getParameterName())
                        .message(String.format("%s изменился с %s на %s",
                                alert.getParameterName(),
                                alert.getOldValue(),
                                alert.getNewValue()))
                        .details(alert.getChangeDescription())
                        .recommendations("Требуется пересмотр всех активных назначений")
                        .targetRole("DOCTOR")
                        .requiresAction(true)
                        .build();

                unifiedNotificationService.sendCriticalNotification(notification);
            }

            log.info("EMR change notifications sent for patient {}", patient.getMrn());
        } catch (Exception e) {
            log.error("Failed to send EMR change notifications: {}", e.getMessage(), e);
        }
    }

    /*
     * Отправить уведомление о новой рекомендации.
     * 
     * @param patient Пациент
     * @param recommendation Новая рекомендация
     * @param alerts Список алертов
     */
    private void sendNewRecommendationNotification(Patient patient, Recommendation recommendation,
                                                   List<EmrChangeAlertDTO> alerts) {
        try {
            String alertsSummary = alerts.stream()
                    .map(a -> a.getParameterName() + ": " + a.getOldValue() + " → " + a.getNewValue())
                    .collect(Collectors.joining(", "));

            UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                    .type(UnifiedNotificationDTO.NotificationType.RECOMMENDATION_UPDATE)
                    .priority(UnifiedNotificationDTO.NotificationPriority.HIGH)
                    .patientMrn(patient.getMrn())
                    .patientName(patient.getFirstName() + " " + patient.getLastName())
                    .title("Новая рекомендация после изменения EMR")
                    .message(String.format("Сгенерирована новая рекомендация #%d из-за критических изменений EMR",
                            recommendation.getId()))
                    .details("Изменения EMR: " + alertsSummary)
                    .recommendations(recommendation.getDescription())
                    .relatedEntityId(recommendation.getId())
                    .relatedEntityType("RECOMMENDATION")
                    .targetRole("DOCTOR")
                    .requiresAction(true)
                    .actionUrl("/recommendations/" + recommendation.getId())
                    .build();

            unifiedNotificationService.sendNotification(notification);

            log.info("New recommendation notification sent for patient {}", patient.getMrn());
        } catch (Exception e) {
            log.error("Failed to send new recommendation notification: {}", e.getMessage(), e);
        }
    }
}
