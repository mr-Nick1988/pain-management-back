package pain_helper_back.VAS_external_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для отображения VAS записи с внешнего устройства в мониторе.
 * 
 * ИСПОЛЬЗУЕТСЯ В:
 * - External VAS Monitor (Frontend)
 * - GET /api/external/vas/records
 * 
 * СОДЕРЖИТ:
 * - Данные VAS записи
 * - Информацию о пациенте (JOIN с Patient)
 * - Метаданные устройства
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVasRecordResponseDTO {
    
    /**
     * ID VAS записи в БД
     */
    private Long id;
    
    /**
     * MRN пациента
     */
    private String patientMrn;
    
    /**
     * Имя пациента (JOIN с Patient)
     */
    private String patientFirstName;
    
    /**
     * Фамилия пациента (JOIN с Patient)
     */
    private String patientLastName;
    
    /**
     * Уровень боли по VAS (0-10)
     */
    private Integer vasLevel;
    
    /**
     * ID устройства, отправившего данные
     * Примеры: "MONITOR-001", "TABLET-WARD-A"
     */
    private String deviceId;
    
    /**
     * Локация пациента
     * Примеры: "Ward A, Bed 12", "ICU-3"
     */
    private String location;

    /**
     * Локация боли на теле пациента
     * Примеры: "Shoulder", "Leg", "Abdomen"
     */
    private String painPlace;
    /**
     * Временная метка записи VAS
     */
    private LocalDateTime timestamp;
    
    /**
     * Дополнительные заметки
     */
    private String notes;
    
    /**
     * Источник данных (для badge на фронтенде)
     * Примеры: "VAS_MONITOR", "EMR_SYSTEM", "MOBILE_APP"
     */
    private String source;
    
    /**
     * Время создания записи в системе
     */
    private LocalDateTime createdAt;
}
