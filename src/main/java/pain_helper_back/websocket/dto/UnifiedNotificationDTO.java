package pain_helper_back.websocket.dto;

import lombok.*;
import pain_helper_back.enums.EscalationPriority;

import java.time.LocalDateTime;
import java.util.List;

/*
 * УНИФИЦИРОВАННЫЙ DTO для всех типов real-time уведомлений через WebSocket.
 * 
 * ЗАЧЕМ НУЖЕН:
 * - Единый формат для всех уведомлений (EMR алерты, эскалации боли, рекомендации)
 * - Упрощает обработку на фронтенде
 * - Позволяет фильтровать по типу и приоритету
 * 
 * ТИПЫ УВЕДОМЛЕНИЙ:
 * - EMR_ALERT: Критические изменения в лабораторных показателях
 * - PAIN_ESCALATION: Эскалация боли пациента
 * - RECOMMENDATION_UPDATE: Обновление рекомендации
 * - DOSE_REMINDER: Напоминание о введении дозы
 * - SYSTEM_MESSAGE: Системные сообщения
 * 
 * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
 * // EMR Alert
 * UnifiedNotificationDTO.builder()
 *     .type(NotificationType.EMR_ALERT)
 *     .priority(NotificationPriority.CRITICAL)
 *     .patientMrn("EMR-12345")
 *     .patientName("Иван Иванов")
 *     .title("Критическое падение GFR")
 *     .message("GFR упал с 45 до 25 - требуется коррекция дозировок")
 *     .build();
 * 
 * // Pain Escalation
 * UnifiedNotificationDTO.builder()
 *     .type(NotificationType.PAIN_ESCALATION)
 *     .priority(NotificationPriority.HIGH)
 *     .patientMrn("EMR-12345")
 *     .title("Эскалация боли")
 *     .message("VAS увеличился с 5 до 9")
 *     .build();
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedNotificationDTO {

    /*
     * Тип уведомления
     */
    private NotificationType type;

    /*
     * Приоритет уведомления
     */
    private NotificationPriority priority;

    /*
     * MRN пациента
     */
    private String patientMrn;

    /*
     * Имя пациента (для отображения)
     */
    private String patientName;

    /*
     * Заголовок уведомления
     */
    private String title;

    /*
     * Основное сообщение
     */
    private String message;

    /*
     * Детальное описание (опционально)
     */
    private String details;

    /*
     * Рекомендации для врача
     */
    private String recommendations;

    /*
     * Время создания уведомления
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /*
     * ID связанной сущности (escalation_id, recommendation_id и т.д.)
     */
    private Long relatedEntityId;

    /*
     * Тип связанной сущности
     */
    private String relatedEntityType;

    /*
     * Дополнительные данные (для EMR алертов, эскалаций и т.д.)
     */
    private Object additionalData;

    /*
     * Требуется ли действие от врача
     */
    @Builder.Default
    private boolean requiresAction = false;

    /*
     * URL для перехода к деталям (опционально)
     */
    private String actionUrl;

    /*
     * Диагнозы пациента (для контекста)
     */
    private List<String> diagnoses;

    /*
     * Кому адресовано уведомление (DOCTOR, ANESTHESIOLOGIST, NURSE, ALL)
     */
    private String targetRole;

    /*
     * ID конкретного получателя (опционально, для персональных уведомлений)
     */
    private String targetUserId;

    /*
     * Тип уведомления
     */
    public enum NotificationType {
        EMR_ALERT,              // Критические изменения в EMR
        PAIN_ESCALATION,        // Эскалация боли
        RECOMMENDATION_UPDATE,  // Обновление рекомендации
        DOSE_REMINDER,          // Напоминание о дозе
        SYSTEM_MESSAGE,         // Системное сообщение
        PROTOCOL_APPROVAL,      // Требуется одобрение протокола
        PATIENT_ADMISSION,      // Поступление нового пациента
        CRITICAL_VAS            // Критический уровень боли
    }

    /*
     * Приоритет уведомления
     */
    public enum NotificationPriority {
        LOW,        // Низкий (информационное)
        MEDIUM,     // Средний (требует внимания)
        HIGH,       // Высокий (требует скорого действия)
        CRITICAL    // Критический (требует немедленного действия)
    }

    /*
     * Конвертировать EscalationPriority в NotificationPriority
     */
    public static NotificationPriority fromEscalationPriority(EscalationPriority escalationPriority) {
        return switch (escalationPriority) {
            case CRITICAL -> NotificationPriority.CRITICAL;
            case HIGH -> NotificationPriority.HIGH;
            case MEDIUM -> NotificationPriority.MEDIUM;
            case LOW -> NotificationPriority.LOW;
        };
    }
}
