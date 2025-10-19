package pain_helper_back.external_emr_integration_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для результатов синхронизации EMR данных.
 * 
 * КОГДА ИСПОЛЬЗУЕТСЯ:
 * - После выполнения запланированной синхронизации
 * - После ручной синхронизации через API
 * - Для логирования и мониторинга процесса синхронизации
 * 
 * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
 * EmrSyncResultDTO result = emrSyncScheduler.syncAllPatients();
 * if (result.hasErrors()) {
 *     log.error("Синхронизация завершилась с ошибками: {}", result.getErrorMessages());
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmrSyncResultDTO {
    
    // Общая информация о синхронизации
    private LocalDateTime syncStartTime;        // Время начала синхронизации
    private LocalDateTime syncEndTime;          // Время окончания синхронизации
    private long durationMs;                    // Длительность в миллисекундах
    
    // Статистика
    private int totalPatientsProcessed;         // Всего пациентов обработано
    private int successfulSyncs;                // Успешных синхронизаций
    private int failedSyncs;                    // Неудачных синхронизаций
    private int patientsWithChanges;            // Пациентов с изменениями
    private int criticalAlertsGenerated;        // Сгенерировано критических алертов
    
    // Детали
    private List<String> syncedPatientMrns = new ArrayList<>();     // MRN синхронизированных пациентов
    private List<String> failedPatientMrns = new ArrayList<>();     // MRN пациентов с ошибками
    private List<EmrChangeAlertDTO> alerts = new ArrayList<>();     // Сгенерированные алерты
    private List<String> errorMessages = new ArrayList<>();         // Сообщения об ошибках
    
    // Статус
    private SyncStatus status;                  // Общий статус синхронизации
    private String message;                     // Общее сообщение
    
    /**
     * Статусы синхронизации
     */
    public enum SyncStatus {
        SUCCESS,            // Все успешно
        PARTIAL_SUCCESS,    // Частичный успех (есть ошибки, но большинство синхронизировано)
        FAILED,             // Полный провал
        IN_PROGRESS         // В процессе выполнения
    }
    
    /**
     * Проверка наличия ошибок
     */
    public boolean hasErrors() {
        return !errorMessages.isEmpty() || failedSyncs > 0;
    }
    
    /**
     * Проверка наличия критических алертов
     */
    public boolean hasCriticalAlerts() {
        return alerts.stream()
                .anyMatch(alert -> alert.getSeverity() == EmrChangeAlertDTO.AlertSeverity.CRITICAL);
    }
    
    /**
     * Получить процент успешных синхронизаций
     */
    public double getSuccessRate() {
        if (totalPatientsProcessed == 0) return 0.0;
        return (double) successfulSyncs / totalPatientsProcessed * 100.0;
    }
}
