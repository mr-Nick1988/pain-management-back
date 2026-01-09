package pain_helper_back.websocket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/*
 * REST Controller for тестирования WebSocket уведомлений.
 * 
 * ЗАЧЕМ НУЖЕН:
 * - Позволяет вручную отправить тестовые уведомления
 * - Проверка работоспособности WebSocket соединения
 * - Отладка подписок на топики
 * - Демонстрация различных typeов уведомлений
 * 
 * ENDPOINTS:
 * 1. POST /api/websocket/test - отправить тестовое уведомление
 * 2. POST /api/websocket/test/emr-alert - тестовый EMR алерт
 * 3. POST /api/websocket/test/pain-escalation - тестовая эскалация боли
 * 4. POST /api/websocket/test/critical - тестовое критическое уведомление
 * 5. GET /api/websocket/status - проверка statusа WebSocket
 * 
 * Example ИСПОЛЬЗОВАНИЯ:
 * curl -X POST http://localhost:8080/api/websocket/test
 * curl -X POST http://localhost:8080/api/websocket/test/emr-alert
 * 
 * Important:
 * - Этот Controller только for тестирования
 * - В продакшене можно отключить or защитить
 * - Реальные уведомления отправляются автоматически from Serviceов
 */
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebSocketTestController {


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
                .patientMrn("TEST-12345")
                .patientName("Тестовый patient")
                .title("Критическое падение GFR")
                .message("GFR упал с 45 до 25 - требуется коррекция дозировок")
                .details("Тяжелая почечная недостаточность. Рекомендуется немедленный пересмотр allх назначений.")
                .recommendations("СРОЧНО: Скорректировать дозировки allх drugов considering GFR < 30")
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
                .patientMrn("TEST-12345")
                .patientName("Тестовый")
                .title("Эскалация боли")
                .message("VAS увеличился с 5 до 9 via 2 часа after введения дозы")
                .details("Значительный рост боли слишком рано after afterдней дозы. current protocol может быть недостаточным.")
                .recommendations("Рассмотреть увеличение дозировки or смену drugа")
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
     * Отправить тестовое критическое уведомление на all каналы
     */
    @PostMapping("/test/critical")
    public ResponseEntity<Map<String, String>> sendTestCriticalNotification() {
                .patientMrn("TEST-12345")
                .patientName("Тестовый patient")
                .title("КРИТИЧЕСКИЙ уровень боли")
                .message("VAS = 10 - максимальный уровень боли")
                .details("patient испытывает невыносимую боль. Требуется немедленное вмешательство.")
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
                .title("Персональное уведомление")
                .message("Это тестовое персональное уведомление for пользователя " + userId)
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
     * Отправить уведомление for конкретной роли
     */
    @PostMapping("/test/role/{role}")
    public ResponseEntity<Map<String, String>> sendTestRoleNotification(@PathVariable String role) {
                .title("Уведомление for роли " + role)
                .message("Это тестовое уведомление for allх пользователей с ролью " + role)
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
     * Проверить status WebSocket
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
