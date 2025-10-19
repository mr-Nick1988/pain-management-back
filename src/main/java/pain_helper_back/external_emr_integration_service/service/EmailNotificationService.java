package pain_helper_back.external_emr_integration_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import pain_helper_back.external_emr_integration_service.dto.EmrChangeAlertDTO;

import java.util.List;

/**
 * Сервис для отправки email уведомлений о критических изменениях в EMR.
 * 
 * ОСНОВНЫЕ ФУНКЦИИ:
 * - Отправка алертов врачам при критических изменениях лабораторных показателей
 * - Отправка сводки по результатам синхронизации администраторам
 * - Форматирование email сообщений с медицинской информацией
 * 
 * КОГДА ОТПРАВЛЯЮТСЯ УВЕДОМЛЕНИЯ:
 * - При обнаружении критических изменений (GFR < 30, PLT < 50, WBC < 2.0, SAT < 90, Натрий < 125 или > 155)
 * - После завершения автоматической синхронизации (если есть критические алерты)
 * - При ошибках синхронизации (для администраторов)
 * 
 * НАСТРОЙКА:
 * В application.properties нужно указать:
 * spring.mail.host=smtp.gmail.com
 * spring.mail.port=587
 * spring.mail.username=your-email@gmail.com
 * spring.mail.password=your-app-password
 * spring.mail.properties.mail.smtp.auth=true
 * spring.mail.properties.mail.smtp.starttls.enable=true
 * 
 * ВАЖНО: Этот сервис создается только если настроен Spring Mail (spring.mail.host)
 */
/**
 * ВАЖНО: IntelliJ IDEA показывает warning "Could not autowire. No beans of 'JavaMailSender' type found"
 * Это НОРМАЛЬНО! Бин создается только если spring.mail.host настроен в application.properties.
 * Если spring.mail.host нет - этот сервис вообще не создается (@ConditionalOnProperty).
 * Приложение работает корректно, warning можно игнорировать.
 */
@Service
@ConditionalOnProperty(name = "spring.mail.host")
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final JavaMailSender mailSender;

    /**
     * Отправить email уведомление о критическом изменении в EMR.
     * 
     * @param alert Алерт с информацией об изменении
     * @param doctorEmail Email врача, ответственного за пациента
     */
    public void sendCriticalChangeAlert(EmrChangeAlertDTO alert, String doctorEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(doctorEmail);
            message.setSubject(String.format("[CRITICAL] EMR Change for Patient %s", alert.getPatientMrn()));
            message.setText(formatCriticalAlertEmail(alert));
            
            mailSender.send(message);
            log.info("Email alert sent to doctor {} for patient {}", doctorEmail, alert.getPatientMrn());
        } catch (Exception e) {
            log.error("Failed to send email alert for patient {}: {}", alert.getPatientMrn(), e.getMessage(), e);
        }
    }

    /**
     * Отправить сводку по критическим алертам нескольким врачам.
     * 
     * @param alerts Список критических алертов
     * @param doctorEmails Список email адресов врачей
     */
    public void sendCriticalAlertsSummary(List<EmrChangeAlertDTO> alerts, List<String> doctorEmails) {
        if (alerts.isEmpty() || doctorEmails.isEmpty()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(doctorEmails.toArray(new String[0]));
            message.setSubject(String.format("[EMR SYNC] %d Critical Changes Detected", alerts.size()));
            message.setText(formatAlertsSummaryEmail(alerts));
            
            mailSender.send(message);
            log.info("Summary of {} critical alerts sent to {} doctors", alerts.size(), doctorEmails.size());
        } catch (Exception e) {
            log.error("Failed to send alerts summary: {}", e.getMessage(), e);
        }
    }

    /**
     * Форматирование email сообщения для критического алерта.
     */
    private String formatCriticalAlertEmail(EmrChangeAlertDTO alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("    CRITICAL CHANGE IN MEDICAL DATA\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        
        sb.append("PATIENT:\n");
        sb.append(String.format("  • MRN: %s\n", alert.getPatientMrn()));
        sb.append(String.format("  • Name: %s\n\n", alert.getPatientName()));
        
        sb.append("CHANGE:\n");
        sb.append(String.format("  • Parameter: %s\n", alert.getParameterName()));
        sb.append(String.format("  • Old: %s\n", alert.getOldValue()));
        sb.append(String.format("  • New: %s\n", alert.getNewValue()));
        sb.append(String.format("  • Severity: %s\n\n", alert.getSeverity()));
        
        sb.append("DESCRIPTION:\n");
        sb.append(String.format("  %s\n\n", alert.getChangeDescription()));
        
        sb.append("RECOMMENDATION:\n");
        sb.append(String.format("  %s\n\n", alert.getRecommendation()));
        
        if (alert.isRequiresRecommendationReview()) {
            sb.append("TREATMENT REVIEW REQUIRED!\n\n");
        }
        
        sb.append(String.format("Detected: %s\n", alert.getDetectedAt()));
        sb.append("\n═══════════════════════════════════════════════════\n");
        sb.append("This is an automated notification from Pain Management Assistant\n");
        
        return sb.toString();
    }

    /**
     * Форматирование email сводки по нескольким алертам.
     */
    private String formatAlertsSummaryEmail(List<EmrChangeAlertDTO> alerts) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("       EMR CRITICAL CHANGES SUMMARY\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        
        sb.append(String.format("Critical changes detected: %d\n\n", alerts.size()));
        
        // Группировка по пациентам
        sb.append("PATIENT DETAILS:\n\n");
        for (int i = 0; i < alerts.size(); i++) {
            EmrChangeAlertDTO alert = alerts.get(i);
            sb.append(String.format("%d. Patient: %s (%s)\n", i + 1, alert.getPatientName(), alert.getPatientMrn()));
            sb.append(String.format("   Parameter: %s (%s → %s)\n", 
                    alert.getParameterName(), alert.getOldValue(), alert.getNewValue()));
            sb.append(String.format("   Severity: %s\n", alert.getSeverity()));
            sb.append(String.format("   Description: %s\n\n", alert.getChangeDescription()));
        }
        
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("Please review treatment plans for the listed patients.\n");
        sb.append("This is an automated notification from Pain Management Assistant\n");
        
        return sb.toString();
    }
}
