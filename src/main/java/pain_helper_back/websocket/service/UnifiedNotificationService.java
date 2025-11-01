package pain_helper_back.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pain_helper_back.websocket.dto.UnifiedNotificationDTO;

/*
 * УНИФИЦИРОВАННЫЙ СЕРВИС для отправки всех типов real-time уведомлений через WebSocket.
 * 
 * ОСНОВНЫЕ ФУНКЦИИ:
 * - Отправка уведомлений на разные топики в зависимости от типа и роли
 * - Персональные уведомления конкретным пользователям
 * - Broadcast уведомления всем подключенным клиентам
 * - Фильтрация по приоритету
 * 
 * ТОПИКИ:
 * 1. /topic/notifications/all - все уведомления (для dashboard)
 * 2. /topic/notifications/doctors - уведомления для врачей
 * 3. /topic/notifications/anesthesiologists - уведомления для анестезиологов
 * 4. /topic/notifications/nurses - уведомления для медсестер
 * 5. /topic/notifications/critical - только критические уведомления
 * 6. /topic/notifications/emr-alerts - EMR алерты
 * 7. /topic/notifications/pain-escalations - эскалации боли
 * 8. /queue/notifications/{userId} - персональные уведомления
 * 
 * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
 * // Отправить EMR алерт всем врачам
 * UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
 *     .type(NotificationType.EMR_ALERT)
 *     .priority(NotificationPriority.CRITICAL)
 *     .patientMrn("EMR-12345")
 *     .title("Критическое падение GFR")
 *     .message("GFR упал с 45 до 25")
 *     .targetRole("DOCTOR")
 *     .build();
 * 
 * unifiedNotificationService.sendNotification(notification);
 * 
 * ИНТЕГРАЦИЯ:
 * - Используется в PainEscalationNotificationService
 * - Используется в WebSocketNotificationService (EMR)
 * - Используется в новых сервисах уведомлений
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /*
     * Отправить уведомление на соответствующие топики.
     * Автоматически определяет топики на основе типа, роли и приоритета.
     * 
     * @param notification Уведомление для отправки
     */
    public void sendNotification(UnifiedNotificationDTO notification) {
        try {
            // Всегда отправляем на общий топик (для dashboard)
            messagingTemplate.convertAndSend("/topic/escalations/dashboard", notification);
            log.debug("Sent notification to /topic/escalations/dashboard: {}", notification.getTitle());

            // Отправляем на топик по роли
            if (notification.getTargetRole() != null) {
                String roleTopic = getRoleTopicPath(notification.getTargetRole());
                messagingTemplate.convertAndSend(roleTopic, notification);
                log.info("📨 Sent notification to {}: {}", roleTopic, notification.getTitle());
            }

            // Отправляем на топик по типу
            String typeTopic = getTypeTopicPath(notification.getType());
            if (typeTopic != null) {
                messagingTemplate.convertAndSend(typeTopic, notification);
                log.info("📨 Sent notification to {}: {}", typeTopic, notification.getTitle());
            }

            // Если критический приоритет - отправляем на специальный топик
            if (notification.getPriority() == UnifiedNotificationDTO.NotificationPriority.CRITICAL) {
                messagingTemplate.convertAndSend("/topic/escalations/critical", notification);
                log.warn("🚨 CRITICAL notification sent: {} - {}", notification.getTitle(), notification.getMessage());
            }

            // Если указан конкретный получатель - отправляем персонально
            if (notification.getTargetUserId() != null) {
                sendPersonalNotification(notification, notification.getTargetUserId());
            }

            log.info("Notification sent successfully: type={}, priority={}, patient={}", 
                    notification.getType(), notification.getPriority(), notification.getPatientMrn());

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }

    /*
     * Отправить персональное уведомление конкретному пользователю.
     * 
     * @param notification Уведомление
     * @param userId ID пользователя
     */
    public void sendPersonalNotification(UnifiedNotificationDTO notification, String userId) {
        try {
            String destination = String.format("/queue/notifications/%s", userId);
            messagingTemplate.convertAndSend(destination, notification);
            log.info("Personal notification sent to user {}: {}", userId, notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to send personal notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /*
     * Отправить уведомление всем пользователям определенной роли.
     * 
     * @param notification Уведомление
     * @param role Роль (DOCTOR, ANESTHESIOLOGIST, NURSE)
     */
    public void sendToRole(UnifiedNotificationDTO notification, String role) {
        try {
            String roleTopic = getRoleTopicPath(role);
            messagingTemplate.convertAndSend(roleTopic, notification);
            log.info("Notification sent to role {}: {}", role, notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to send notification to role {}: {}", role, e.getMessage(), e);
        }
    }

    /*
     * Отправить broadcast уведомление всем подключенным клиентам.
     * 
     * @param notification Уведомление
     */
    public void broadcastNotification(UnifiedNotificationDTO notification) {
        try {
            messagingTemplate.convertAndSend("/topic/escalations/dashboard", notification);
            log.info("📢 Broadcast notification sent: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to broadcast notification: {}", e.getMessage(), e);
        }
    }

    /*
     * Отправить критическое уведомление (на все критические топики).
     * 
     * @param notification Уведомление
     */
    public void sendCriticalNotification(UnifiedNotificationDTO notification) {
        notification.setPriority(UnifiedNotificationDTO.NotificationPriority.CRITICAL);
        
        try {
            // Отправляем на все критические топики
            messagingTemplate.convertAndSend("/topic/escalations/critical", notification);
            messagingTemplate.convertAndSend("/topic/escalations/dashboard", notification);
            
            // Если есть роль - отправляем и туда
            if (notification.getTargetRole() != null) {
                String roleTopic = getRoleTopicPath(notification.getTargetRole());
                messagingTemplate.convertAndSend(roleTopic, notification);
            }

            log.warn("🚨 CRITICAL notification sent to all channels: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to send critical notification: {}", e.getMessage(), e);
        }
    }

    /*
     * Получить путь топика по роли
     * 
     * ВАЖНО: Топики должны совпадать с теми, на которые подписан фронтенд!
     * Frontend подписан на: /topic/escalations/anesthesiologists
     */
    private String getRoleTopicPath(String role) {
        return switch (role.toUpperCase()) {
            case "DOCTOR" -> "/topic/escalations/doctors";
            case "ANESTHESIOLOGIST" -> "/topic/escalations/anesthesiologists";
            case "NURSE" -> "/topic/escalations/nurses";
            case "ADMIN" -> "/topic/escalations/admins";
            default -> "/topic/escalations/all";
        };
    }

    /*
     * Получить путь топика по типу уведомления
     * 
     * ВАЖНО: Используем /topic/escalations/* вместо /topic/notifications/*
     */
    private String getTypeTopicPath(UnifiedNotificationDTO.NotificationType type) {
        return switch (type) {
            case EMR_ALERT -> "/topic/emr-alerts";
            case PAIN_ESCALATION -> "/topic/escalations/anesthesiologists"; // Эскалации боли идут анестезиологам
            case RECOMMENDATION_UPDATE -> "/topic/escalations/doctors";
            case DOSE_REMINDER -> "/topic/escalations/nurses";
            case PROTOCOL_APPROVAL -> "/topic/escalations/doctors";
            case CRITICAL_VAS -> "/topic/escalations/critical";
            default -> null;
        };
    }

    /*
     * Отправить тестовое уведомление (для проверки WebSocket соединения)
     */
    public void sendTestNotification() {
        UnifiedNotificationDTO testNotification = UnifiedNotificationDTO.builder()
                .type(UnifiedNotificationDTO.NotificationType.SYSTEM_MESSAGE)
                .priority(UnifiedNotificationDTO.NotificationPriority.LOW)
                .title("Тестовое уведомление")
                .message("WebSocket соединение работает корректно")
                .timestamp(java.time.LocalDateTime.now())
                .build();

        broadcastNotification(testNotification);
        log.info("Test notification sent");
    }
}
