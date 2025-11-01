package pain_helper_back.pain_escalation_tracking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.pain_escalation_tracking.dto.PainEscalationNotificationDTO;
import pain_helper_back.pain_escalation_tracking.entity.PainEscalation;
import pain_helper_back.websocket.dto.UnifiedNotificationDTO;
import pain_helper_back.websocket.service.UnifiedNotificationService;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Сервис отправки push-уведомлений об эскалациях боли через WebSocket.
 * Он создаёт PainEscalationNotificationDTO на основании PainEscalation entity для отправки
 * Это “мост” между бизнес-логикой и коммуникацией.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PainEscalationNotificationService {
    // достаточно встроенного SimpMessagingTemplate для отправлений уведомлений по небольшим ролям
    private final SimpMessagingTemplate messagingTemplate;
    // это некий “Notification Hub”, предназначенный для масштабных систем, где уведомлений много (разные типы, роли, приоритеты);
    private final UnifiedNotificationService unifiedNotificationService;

    /**
     * Отправить уведомление о сильном скачке боли у пациента (>=2)
     *
     * @param painEscalation - содержит всю информацию о сильном скачке боли
     */
    public void sendEscalationNotification(PainEscalation painEscalation) {
        Patient patient = painEscalation.getPatient();
        log.info("Sending escalation notification for patient: {}, recommendation id: {}",
                patient.getMrn(), painEscalation.getLastRecommendation().getId());

        // Извлекаем диагнозы
        List<String> latestDiagnoses = patient.getEmr() != null && !patient.getEmr().isEmpty()
                ? patient.getEmr().getLast().getDiagnoses().stream()
                .map(d -> d.getIcdCode() + " - " + d.getDescription())
                .collect(Collectors.toList())
                : List.of();

        // Формируем DTO для уведомления
        PainEscalationNotificationDTO notification = PainEscalationNotificationDTO.builder()
                .escalationId(painEscalation.getId())
                .patientMrn(patient.getMrn())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .currentVas(painEscalation.getCurrentVas())
                .previousVas(painEscalation.getPreviousVas())
                .vasChange(painEscalation.getVasChange())
                .priority(painEscalation.getPriority().name())
                .createdAt(painEscalation.getCreatedAt())
                .latestDiagnoses(latestDiagnoses)
                .build();

        // Отправляем уведомления на разные каналы
        sendToAllChannels(notification);

        // Отправляем унифицированное уведомление
        sendUnifiedNotification(painEscalation.getLastRecommendation(), patient, painEscalation.getCurrentVas(),
                painEscalation.getPreviousVas(), latestDiagnoses);
    }

    private void sendToAllChannels(PainEscalationNotificationDTO notification) {
        //real-time уведомления через WebSocket на Topic (doctors,anesthesiologists,dashboard)
        try {
//            messagingTemplate.convertAndSend("/topic/escalations/doctors", notification); не требуется так как фильтр при боле >=2 поставит статус ESCALATED
            messagingTemplate.convertAndSend("/topic/escalations/anesthesiologists", notification);
            // “CRITICAL” - если боль критическая на Topic notification
            if ("CRITICAL".equalsIgnoreCase(notification.getPriority())) {
                messagingTemplate.convertAndSend("/topic/escalations/critical", notification);
                log.warn("CRITICAL escalation notification sent for patient: {}", notification.getPatientMrn());
            }
        } catch (Exception e) {
            log.error("Failed to send escalation notification: {}", e.getMessage(), e);
        }
    }


    /**
     * Отправить унифицированное уведомление об эскалации боли
     * шлёт в общий notification pipeline, который собирает всё (включая боль, EMR, рекомендации, протоколы).
     * Архитектурно: это нужно для централизованного мониторинга и UI-интеграции.
     * В едином “notification-center” на фронтенде ты можешь показывать все события в одном списке.
     * центральный журнал событий-уведомлений для хранения в БД
     * Оставить её стоит только если у тебя:
     * Есть единый компонент на фронтенде “Уведомления” (Notification Center),
     * где показываются все типы событий — и эскалации, и EMR, и системные алерты.
     * Или ты хочешь, чтобы администратор видел “все события системы” в одном месте.
     * TODO можно удалить
     */
    private void sendUnifiedNotification(Recommendation recommendation, Patient patient,
                                         Integer currentVas, Integer previousVas,
                                         List<String> diagnoses) {
        try {
            String title = "Pain escalation detected";
            String message = String.format(
                    "VAS changed from %d to %d (+%d). Recommendation #%d marked as ESCALATED.",
                    previousVas != null ? previousVas : 0,
                    currentVas != null ? currentVas : 0,
                    (currentVas != null && previousVas != null) ? currentVas - previousVas : 0,
                    recommendation.getId()
            );

            UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                    .type(UnifiedNotificationDTO.NotificationType.PAIN_ESCALATION)
                    .priority(UnifiedNotificationDTO.NotificationPriority.MEDIUM)
                    .patientMrn(patient.getMrn())
                    .patientName(patient.getFirstName() + " " + patient.getLastName())
                    .title(title)
                    .message(message)
                    .details("Recommendation escalated due to pain increase.")
                    .relatedEntityId(recommendation.getId())
                    .relatedEntityType("RECOMMENDATION")
                    .diagnoses(diagnoses)
                    .targetRole("ANESTHESIOLOGIST")
                    .requiresAction(true)
                    .actionUrl("/recommendations/" + recommendation.getId())
                    .build();

            unifiedNotificationService.sendNotification(notification);
            log.info("Unified notification sent for escalated recommendation {}", recommendation.getId());

        } catch (Exception e) {
            log.error("Failed to send unified notification for recommendation {}: {}",
                    recommendation.getId(), e.getMessage(), e);
        }
    }

}