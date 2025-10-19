package pain_helper_back.external_emr_integration_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для алертов о критических изменениях в EMR данных пациента.
 * 
 * КОГДА ИСПОЛЬЗУЕТСЯ:
 * - При обнаружении критических изменений лабораторных показателей
 * - Для уведомления врачей о необходимости пересмотра рекомендаций
 * 
 * ПРИМЕРЫ КРИТИЧЕСКИХ ИЗМЕНЕНИЙ:
 * - GFR упал ниже 30 (почечная недостаточность)
 * - Тромбоциты (PLT) < 50 (риск кровотечения)
 * - Лейкоциты (WBC) < 1.0 (иммунодефицит)
 * - Натрий > 145 или < 135 (электролитный дисбаланс)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmrChangeAlertDTO {
    
    // Идентификация пациента
    private String patientMrn;              // MRN пациента
    private String patientName;             // Имя пациента для удобства
    
    // Информация об изменении
    private String parameterName;           // Название показателя (например, "GFR")
    private String oldValue;                // Старое значение
    private String newValue;                // Новое значение
    private String changeDescription;       // Описание изменения
    
    // Критичность
    private AlertSeverity severity;         // Уровень критичности (LOW, MEDIUM, HIGH, CRITICAL)
    private String recommendation;          // Рекомендация врачу
    
    // Временные метки
    private LocalDateTime detectedAt;       // Когда обнаружено изменение
    private LocalDateTime lastEmrUpdate;    // Когда был последний апдейт EMR
    
    // Дополнительная информация
    private boolean requiresRecommendationReview;  // Требуется ли пересмотр рекомендаций
    private Long affectedRecommendationId;         // ID затронутой рекомендации (если есть)
    
    /**
     * Уровни критичности алерта
     */
    public enum AlertSeverity {
        LOW,        // Незначительное изменение, информационный характер
        MEDIUM,     // Умеренное изменение, требует внимания
        HIGH,       // Значительное изменение, требует действий
        CRITICAL    // Критическое изменение, требует немедленных действий
    }
}
