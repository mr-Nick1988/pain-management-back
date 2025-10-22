package pain_helper_back.pain_escalation_tracking.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.pain_escalation_tracking.dto.PainEscalationNotificationDTO;
import pain_helper_back.websocket.dto.UnifiedNotificationDTO;
import pain_helper_back.websocket.service.UnifiedNotificationService;

import java.util.List;
import java.util.stream.Collectors;

/*
 * Сервис отправки push-уведомлений об эскалациях боли через WebSocket.
 * 
 * ОБНОВЛЕНО: Теперь использует UnifiedNotificationService для отправки уведомлений
 * в едином формате на все топики.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PainEscalationNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UnifiedNotificationService unifiedNotificationService;

    /*
     * Отправить уведомление о новой эскалации боли
     *
     * @param escalation созданная эскалация
     * @param patient пациент
     * @param currentVas текущий уровень VAS
     * @param previousVas предыдущий уровень VAS
     * @param recommendations рекомендации
     */
    public void sendEscalationNotification(Escalation escalation, Patient patient,
                                           Integer currentVas, Integer previousVas,
                                           String recommendations) {
        log.info("Sending escalation notification for patient: {}, escalation id: {}",
                patient.getMrn(), escalation.getId());

        // Извлекаем последние диагнозы
        List<String> latestDiagnoses = patient.getEmr() != null && !patient.getEmr().isEmpty()
                ? patient.getEmr().getLast().getDiagnoses().stream()
                .map(d -> d.getIcdCode() + " - " + d.getDescription())
                .collect(Collectors.toList())
                : List.of();

        // Формируем DTO для уведомления (legacy формат)
        PainEscalationNotificationDTO notification = PainEscalationNotificationDTO.builder()
                .escalationId(escalation.getId())
                .recommendationId(escalation.getRecommendation().getId())
                .patientMrn(patient.getMrn())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .currentVas(currentVas)
                .previousVas(previousVas)
                .vasChange(currentVas - previousVas)
                .escalationReason(escalation.getEscalationReason())
                .priority(escalation.getPriority())
                .recommendations(recommendations)
                .createdAt(escalation.getCreatedAt())
                .latestDiagnoses(latestDiagnoses)
                .build();

        // Отправляем уведомления на разные топики (legacy)
        sendToAllChannels(notification);
        
        // НОВОЕ: Отправляем через унифицированный сервис
        sendUnifiedNotification(escalation, patient, currentVas, previousVas, recommendations, latestDiagnoses);
    }

    /*
     * Отправить уведомление на все каналы
     */
    private void sendToAllChannels(PainEscalationNotificationDTO notification) {
        try {
            // Топик для всех врачей
            messagingTemplate.convertAndSend("/topic/escalations/doctors", notification);
            log.debug("Sent notification to /topic/escalations/doctors");

            // Топик для анестезиологов
            messagingTemplate.convertAndSend("/topic/escalations/anesthesiologists", notification);
            log.debug("Sent notification to /topic/escalations/anesthesiologists");

            // Топик для dashboard (мониторинг)
            messagingTemplate.convertAndSend("/topic/escalations/dashboard", notification);
            log.debug("Sent notification to /topic/escalations/dashboard");

            // Если критический приоритет - отправляем на специальный канал
            if ("CRITICAL".equals(notification.getPriority().name())) {
                messagingTemplate.convertAndSend("/topic/escalations/critical", notification);
                log.warn("CRITICAL escalation notification sent for patient: {}", notification.getPatientMrn());
            }

        } catch (Exception e) {
            log.error("Failed to send escalation notification: {}", e.getMessage(), e);
        }
    }

    /*
     * Отправить уведомление конкретному врачу
     *
     * @param doctorId ID врача
     * @param notification уведомление
     */
    public void sendToDoctor(String doctorId, PainEscalationNotificationDTO notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    doctorId,
                    "/queue/escalations",
                    notification
            );
            log.info("Sent personal notification to doctor: {}", doctorId);
        } catch (Exception e) {
            log.error("Failed to send notification to doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    /*
     * Отправить уведомление об обновлении статуса эскалации
     *
     * @param escalationId ID эскалации
     * @param newStatus новый статус
     * @param resolvedBy кто разрешил
     */
    public void sendEscalationStatusUpdate(Long escalationId, String newStatus, String resolvedBy) {
        try {
            var statusUpdate = new EscalationStatusUpdateDTO(
                    escalationId,
                    newStatus,
                    resolvedBy,
                    java.time.LocalDateTime.now()
            );

            messagingTemplate.convertAndSend("/topic/escalations/status-updates", statusUpdate);
            log.info("Sent status update for escalation {}: {}", escalationId, newStatus);
        } catch (Exception e) {
            log.error("Failed to send status update: {}", e.getMessage(), e);
        }
    }

    /*
     * НОВЫЙ МЕТОД: Отправить унифицированное уведомление об эскалации боли
     */
    private void sendUnifiedNotification(Escalation escalation, Patient patient,
                                        Integer currentVas, Integer previousVas,
                                        String recommendations, List<String> diagnoses) {
        try {
            // Определяем приоритет уведомления
            UnifiedNotificationDTO.NotificationPriority priority = 
                    UnifiedNotificationDTO.fromEscalationPriority(escalation.getPriority());

            // Формируем заголовок
            String title = String.format("Эскалация боли: %s", escalation.getPriority().name());
            
            // Формируем сообщение
            String message = String.format("VAS изменился с %d на %d (+%d). %s",
                    previousVas != null ? previousVas : 0,
                    currentVas,
                    currentVas - (previousVas != null ? previousVas : 0),
                    escalation.getEscalationReason());

            // Создаем унифицированное уведомление
            UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                    .type(UnifiedNotificationDTO.NotificationType.PAIN_ESCALATION)
                    .priority(priority)
                    .patientMrn(patient.getMrn())
                    .patientName(patient.getFirstName() + " " + patient.getLastName())
                    .title(title)
                    .message(message)
                    .details(escalation.getEscalationReason())
                    .recommendations(recommendations)
                    .relatedEntityId(escalation.getId())
                    .relatedEntityType("ESCALATION")
                    .diagnoses(diagnoses)
                    .targetRole("DOCTOR")
                    .requiresAction(true)
                    .actionUrl("/escalations/" + escalation.getId())
                    .build();

            // Отправляем через унифицированный сервис
            unifiedNotificationService.sendNotification(notification);
            
            log.info("Unified notification sent for escalation {}", escalation.getId());
        } catch (Exception e) {
            log.error("Failed to send unified notification for escalation {}: {}", 
                    escalation.getId(), e.getMessage(), e);
        }
    }

    /*
     * DTO для обновления статуса эскалации
     */
    public record EscalationStatusUpdateDTO(
            Long escalationId,
            String newStatus,
            String resolvedBy,
            java.time.LocalDateTime updatedAt
    ) {}

}
