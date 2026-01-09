package pain_helper_back.websocket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/*
 * REST Controller for testing WebSocket notifications.
 * 
 * Purpose:
 * - Allows manual sending of test notifications
 * - Verify WebSocket connection functionality
 * - Debug topic subscriptions
 * - Demonstrate various notification types
 * 
 * ENDPOINTS:
 * 1. POST /api/websocket/test - send test notification
 * 2. POST /api/websocket/test/emr-alert - test EMR alert
 * 3. POST /api/websocket/test/pain-escalation - test pain escalation
 * 4. POST /api/websocket/test/critical - test critical notification
 * 5. GET /api/websocket/status - check WebSocket status
 * 
 * Example USAGE:
 * curl -X POST http://localhost:8080/api/websocket/test
 * curl -X POST http://localhost:8080/api/websocket/test/emr-alert
 * 
 * Important:
 * - This Controller is for testing only
 * - In production can be disabled or secured
 * - Real notifications are sent automatically from Services
 */
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebSocketTestController {


    /*
     * Send simple test notification
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
     * Send test EMR alert
     */
    @PostMapping("/test/emr-alert")
    public ResponseEntity<Map<String, String>> sendTestEmrAlert() {
                .patientMrn("TEST-12345")
                .patientName("Test patient")
                .title("Critical GFR drop")
                .message("GFR dropped from 45 to 25 - dosage correction required")
                .details("Severe renal failure. Immediate review of all prescriptions recommended.")
                .recommendations("URGENT: Adjust dosages of all drugs considering GFR < 30")
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
     * Send test pain escalation
     */
    @PostMapping("/test/pain-escalation")
    public ResponseEntity<Map<String, String>> sendTestPainEscalation() {
                .patientMrn("TEST-12345")
                .patientName("Test patient")
                .title("Pain escalation")
                .message("VAS increased from 5 to 9 within 2 hours after dose administration")
                .details("Significant pain increase too soon after last dose. Current protocol may be insufficient.")
                .recommendations("Consider increasing dosage or changing drug")
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
     * Send test critical notification to all channels
     */
    @PostMapping("/test/critical")
    public ResponseEntity<Map<String, String>> sendTestCriticalNotification() {
                .patientMrn("TEST-99999")
                .patientName("Critical Test Patient")
                .title("CRITICAL: System Test Alert")
                .message("This is a test of critical notification system")
                .details("Testing emergency notification broadcast to all roles")
                .recommendations("No action required - this is a test")
                .targetRole("ALL")
                .requiresAction(false)
                .build();

        notificationService.sendNotification(notification);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test critical notification sent to all channels",
                "type", "CRITICAL",
                "priority", "CRITICAL"
        ));
    }

    /*
     * Check WebSocket connection status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "service", "WebSocket Notification Service",
                "endpoints", Map.of(
                        "connect", "/ws",
                        "subscribe", "/topic/notifications/{role}"
                ),
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
