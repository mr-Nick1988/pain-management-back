package pain_helper_back.external_emr_integration_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pain_helper_back.external_emr_integration_service.dto.EmrChangeAlertDTO;

import java.util.List;

/*
 * Сервис для отправки real-time уведомлений через WebSocket.
 * 
 * ОСНОВНЫЕ ФУНКЦИИ:
 * - Отправка критических алертов всем подключенным клиентам
 * - Отправка уведомлений конкретному врачу (по user ID)
 * - Broadcast сообщений о завершении синхронизации
 * 
 * ТОПИКИ:
 * - /topic/emr-alerts - все критические алерты (для всех врачей)
 * - /topic/emr-alerts/{doctorId} - алерты для конкретного врача
 * - /topic/emr-sync-status - статус синхронизации (для администраторов)
 * 
 * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
 * // В EmrSyncScheduler после обнаружения критического изменения:
 * webSocketNotificationService.sendCriticalAlert(alert);
 * 
 * // Frontend получит JSON:
 * {
 *   "patientMrn": "EMR-12345678",
 *   "patientName": "Иван Иванов",
 *   "parameterName": "GFR",
 *   "oldValue": "45",
 *   "newValue": "25",
 *   "severity": "CRITICAL",
 *   "changeDescription": "GFR упал ниже 30 - тяжелая почечная недостаточность",
 *   "recommendation": "СРОЧНО: Требуется коррекция дозировок всех препаратов"
 * }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Отправить критический алерт всем подключенным клиентам.
     * 
     * @param alert Алерт о критическом изменении
     */
    public void sendCriticalAlert(EmrChangeAlertDTO alert) {
        try {
            messagingTemplate.convertAndSend("/topic/emr-alerts", alert);
            log.info("WebSocket alert sent for patient {}: {} {} → {}", 
                    alert.getPatientMrn(), alert.getParameterName(), 
                    alert.getOldValue(), alert.getNewValue());
        } catch (Exception e) {
            log.error("Failed to send WebSocket alert for patient {}: {}", 
                    alert.getPatientMrn(), e.getMessage(), e);
        }
    }

    /**
     * Отправить несколько критических алертов.
     * 
     * @param alerts Список алертов
     */
    public void sendCriticalAlerts(List<EmrChangeAlertDTO> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        for (EmrChangeAlertDTO alert : alerts) {
            sendCriticalAlert(alert);
        }
        
        log.info("Sent {} WebSocket alerts", alerts.size());
    }

    /**
     * Отправить алерт конкретному врачу.
     * 
     * @param alert Алерт
     * @param doctorId ID врача
     */
    public void sendAlertToDoctor(EmrChangeAlertDTO alert, String doctorId) {
        try {
            String destination = String.format("/topic/emr-alerts/%s", doctorId);
            messagingTemplate.convertAndSend(destination, alert);
            log.info("WebSocket alert sent to doctor {} for patient {}", 
                    doctorId, alert.getPatientMrn());
        } catch (Exception e) {
            log.error("Failed to send WebSocket alert to doctor {}: {}", 
                    doctorId, e.getMessage(), e);
        }
    }

    /**
     * Отправить уведомление о завершении синхронизации.
     * 
     * @param message Сообщение о статусе синхронизации
     */
    public void sendSyncStatusUpdate(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/emr-sync-status", message);
            log.debug("WebSocket sync status sent: {}", message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket sync status: {}", e.getMessage(), e);
        }
    }
}
