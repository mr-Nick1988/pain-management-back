package pain_helper_back.pain_escalation_tracking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.pain_escalation_tracking.dto.PainEscalationNotificationDTO;
import pain_helper_back.websocket.dto.UnifiedNotificationDTO;
import pain_helper_back.websocket.service.UnifiedNotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Сервис отправки push-уведомлений об эскалациях боли через WebSocket.
 * Теперь использует Recommendation вместо удалённой сущности Escalation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PainEscalationNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UnifiedNotificationService unifiedNotificationService;

    /**
     * Отправить уведомление о новой эскалации боли
     *
     * @param recommendation рекомендация, переведённая в статус ESCALATED
     * @param patient пациент
     * @param currentVas текущий уровень VAS
     * @param previousVas предыдущий уровень VAS
     * @param recommendations текст рекомендаций / комментариев
     */
    public void sendEscalationNotification(Recommendation recommendation, Patient patient,
                                           Integer currentVas, Integer previousVas,
                                           String recommendations) {
        log.info("Sending escalation notification for patient: {}, recommendation id: {}",
                patient.getMrn(), recommendation.getId());

        // Извлекаем диагнозы
        List<String> latestDiagnoses = patient.getEmr() != null && !patient.getEmr().isEmpty()
                ? patient.getEmr().getLast().getDiagnoses().stream()
                .map(d -> d.getIcdCode() + " - " + d.getDescription())
                .collect(Collectors.toList())
                : List.of();

        // Формируем DTO для уведомления
        PainEscalationNotificationDTO notification = PainEscalationNotificationDTO.builder()
                .escalationId(recommendation.getId())
                .recommendationId(recommendation.getId())
                .patientMrn(patient.getMrn())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .currentVas(currentVas)
                .previousVas(previousVas)
                .vasChange(currentVas != null && previousVas != null ? currentVas - previousVas : 0)
                .escalationReason("Pain escalation detected: " + recommendation.getComments().getLast())
                .priority("ESCALATED") // просто символическое поле для UI
                .recommendations(recommendations)
                .createdAt(LocalDateTime.now())
                .latestDiagnoses(latestDiagnoses)
                .build();

        // Отправляем уведомления на разные каналы
        sendToAllChannels(notification);

        // Отправляем унифицированное уведомление
        sendUnifiedNotification(recommendation, patient, currentVas, previousVas, recommendations, latestDiagnoses);
    }

    private void sendToAllChannels(PainEscalationNotificationDTO notification) {
        try {
            messagingTemplate.convertAndSend("/topic/escalations/doctors", notification);
            messagingTemplate.convertAndSend("/topic/escalations/anesthesiologists", notification);
            messagingTemplate.convertAndSend("/topic/escalations/dashboard", notification);

            // “CRITICAL” здесь просто как пример — в будущем можно добавить логику приоритета
            if ("CRITICAL".equalsIgnoreCase(notification.getPriority())) {
                messagingTemplate.convertAndSend("/topic/escalations/critical", notification);
                log.warn("CRITICAL escalation notification sent for patient: {}", notification.getPatientMrn());
            }
        } catch (Exception e) {
            log.error("Failed to send escalation notification: {}", e.getMessage(), e);
        }
    }

    public void sendToDoctor(String doctorId, PainEscalationNotificationDTO notification) {
        try {
            messagingTemplate.convertAndSendToUser(doctorId, "/queue/escalations", notification);
            log.info("Sent personal notification to doctor: {}", doctorId);
        } catch (Exception e) {
            log.error("Failed to send notification to doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    /**
     * Отправить уведомление об изменении статуса рекомендации (например, Escalated → Approved)
     */
    public void sendRecommendationStatusUpdate(Long recId, RecommendationStatus newStatus, String updatedBy) {
        try {
            var statusUpdate = new RecommendationStatusUpdateDTO(
                    recId,
                    newStatus.name(),
                    updatedBy,
                    LocalDateTime.now()
            );

            messagingTemplate.convertAndSend("/topic/escalations/status-updates", statusUpdate);
            log.info("Sent status update for recommendation {}: {}", recId, newStatus);
        } catch (Exception e) {
            log.error("Failed to send status update: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправить унифицированное уведомление об эскалации боли
     */
    private void sendUnifiedNotification(Recommendation recommendation, Patient patient,
                                         Integer currentVas, Integer previousVas,
                                         String recommendations, List<String> diagnoses) {
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
                    .recommendations(recommendations)
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

    public record RecommendationStatusUpdateDTO(
            Long recommendationId,
            String newStatus,
            String updatedBy,
            LocalDateTime updatedAt
    ) {}
}