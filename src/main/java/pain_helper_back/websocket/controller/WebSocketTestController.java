package pain_helper_back.websocket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.websocket.dto.UnifiedNotificationDTO;
import pain_helper_back.websocket.service.UnifiedNotificationService;

import java.time.LocalDateTime;
import java.util.Map;

/*
 * REST контроллер для тестирования WebSocket уведомлений.
 * 
 * ЗАЧЕМ НУЖЕН:
 * - Позволяет вручную отправить тестовые уведомления
 * - Проверка работоспособности WebSocket соединения
 * - Отладка подписок на топики
 * - Демонстрация различных типов уведомлений
 * 
 * ENDPOINTS:
 * 1. POST /api/websocket/test - отправить тестовое уведомление
 * 2. POST /api/websocket/test/emr-alert - тестовый EMR алерт
 * 3. POST /api/websocket/test/pain-escalation - тестовая эскалация боли
 * 4. POST /api/websocket/test/critical - тестовое критическое уведомление
 * 5. GET /api/websocket/status - проверка статуса WebSocket
 * 
 * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
 * curl -X POST http://localhost:8080/api/websocket/test
 * curl -X POST http://localhost:8080/api/websocket/test/emr-alert
 * 
 * ВАЖНО:
 * - Этот контроллер только для тестирования
 * - В продакшене можно отключить или защитить
 * - Реальные уведомления отправляются автоматически из сервисов
 */
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebSocketTestController {

    private final UnifiedNotificationService notificationService;

    /*
     * Отправить простое тестовое уведомление
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestNotification() {
        notificationService.sendTestNotification();
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test notification sent to all subscribers",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /*
     * Отправить тестовый EMR алерт
     */
    @PostMapping("/test/emr-alert")
    public ResponseEntity<Map<String, String>> sendTestEmrAlert() {
        UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                .type(UnifiedNotificationDTO.NotificationType.EMR_ALERT)
                .priority(UnifiedNotificationDTO.NotificationPriority.CRITICAL)
                .patientMrn("TEST-12345")
                .patientName("Тестовый Пациент")
                .title("Критическое падение GFR")
                .message("GFR упал с 45 до 25 - требуется коррекция дозировок")
                .details("Тяжелая почечная недостаточность. Рекомендуется немедленный пересмотр всех назначений.")
                .recommendations("СРОЧНО: Скорректировать дозировки всех препаратов с учетом GFR < 30")
                .targetRole("DOCTOR")
                .requiresAction(true)
                .build();

        notificationService.sendNotification(notification);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test EMR alert sent",
                "type", "EMR_ALERT",
                "priority", "CRITICAL"
        ));
    }

    /*
     * Отправить тестовую эскалацию боли
     */
    @PostMapping("/test/pain-escalation")
    public ResponseEntity<Map<String, String>> sendTestPainEscalation() {
        UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                .type(UnifiedNotificationDTO.NotificationType.PAIN_ESCALATION)
                .priority(UnifiedNotificationDTO.NotificationPriority.HIGH)
                .patientMrn("TEST-12345")
                .patientName("Тестовый")
                .title("Эскалация боли")
                .message("VAS увеличился с 5 до 9 через 2 часа после введения дозы")
                .details("Значительный рост боли слишком рано после последней дозы. Текущий протокол может быть недостаточным.")
                .recommendations("Рассмотреть увеличение дозировки или смену препарата")
                .targetRole("DOCTOR")
                .requiresAction(true)
                .build();

        notificationService.sendNotification(notification);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test pain escalation sent",
                "type", "PAIN_ESCALATION",
                "priority", "HIGH"
        ));
    }

    /*
     * Отправить тестовое критическое уведомление на все каналы
     */
    @PostMapping("/test/critical")
    public ResponseEntity<Map<String, String>> sendTestCriticalNotification() {
        UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                .type(UnifiedNotificationDTO.NotificationType.CRITICAL_VAS)
                .priority(UnifiedNotificationDTO.NotificationPriority.CRITICAL)
                .patientMrn("TEST-12345")
                .patientName("Тестовый Пациент")
                .title("КРИТИЧЕСКИЙ уровень боли")
                .message("VAS = 10 - максимальный уровень боли")
                .details("Пациент испытывает невыносимую боль. Требуется немедленное вмешательство.")
                .recommendations("ЭКСТРЕННО: Вызвать анестезиолога, рассмотреть IV анальгетики")
                .targetRole("DOCTOR")
                .requiresAction(true)
                .build();

        notificationService.sendCriticalNotification(notification);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test critical notification sent to all channels",
                "type", "CRITICAL_VAS",
                "priority", "CRITICAL"
        ));
    }

    /*
     * Отправить персональное уведомление конкретному пользователю
     */
    @PostMapping("/test/personal/{userId}")
    public ResponseEntity<Map<String, String>> sendTestPersonalNotification(@PathVariable String userId) {
        UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                .type(UnifiedNotificationDTO.NotificationType.SYSTEM_MESSAGE)
                .priority(UnifiedNotificationDTO.NotificationPriority.MEDIUM)
                .title("Персональное уведомление")
                .message("Это тестовое персональное уведомление для пользователя " + userId)
                .targetUserId(userId)
                .build();

        notificationService.sendPersonalNotification(notification, userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Personal notification sent to user: " + userId,
                "userId", userId
        ));
    }

    /*
     * Отправить уведомление для конкретной роли
     */
    @PostMapping("/test/role/{role}")
    public ResponseEntity<Map<String, String>> sendTestRoleNotification(@PathVariable String role) {
        UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
                .type(UnifiedNotificationDTO.NotificationType.SYSTEM_MESSAGE)
                .priority(UnifiedNotificationDTO.NotificationPriority.LOW)
                .title("Уведомление для роли " + role)
                .message("Это тестовое уведомление для всех пользователей с ролью " + role)
                .targetRole(role)
                .build();

        notificationService.sendToRole(notification, role);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Notification sent to role: " + role,
                "role", role
        ));
    }

    /*
     * Проверить статус WebSocket
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "endpoint", "ws://localhost:8080/ws",
                "sockjs_endpoint", "http://localhost:8080/ws",
                "topics", Map.of(
                        "anesthesiologists", "/topic/escalations/anesthesiologists",
                        "doctors", "/topic/escalations/doctors",
                        "critical", "/topic/escalations/critical",
                        "dashboard", "/topic/escalations/dashboard",
                        "emr_alerts", "/topic/emr-alerts"
                ),
                "frontend_example", "const socket = new SockJS('http://localhost:8080/ws'); const stompClient = Stomp.over(socket);",
                "timestamp", LocalDateTime.now()
        ));
    }
}
