package pain_helper_back.emr_recalculation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pain_helper_back.emr_recalculation.dto.EmrChangeAlertDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

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

    public void sendCriticalAlerts(List<EmrChangeAlertDTO> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        for (EmrChangeAlertDTO alert : alerts) {
            sendCriticalAlert(alert);
        }
        
        log.info("Sent {} WebSocket alerts", alerts.size());
    }

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

    public void sendSyncStatusUpdate(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/emr-sync-status", message);
            log.debug("WebSocket sync status sent: {}", message);
        } catch (Exception e) {
            log.error("Failed to send WebSocket sync status: {}", e.getMessage(), e);
        }
    }
}
