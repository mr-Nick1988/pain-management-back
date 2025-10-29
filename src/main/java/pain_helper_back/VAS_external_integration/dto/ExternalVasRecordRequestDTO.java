package pain_helper_back.VAS_external_integration.dto;


import lombok.*;

import java.time.LocalDateTime;

/*
 * Унифицированный DTO для VAS записей из внешних систем.
 *
 * ПОДДЕРЖИВАЕМЫЕ ФОРМАТЫ:
 * - JSON (REST API)
 * - XML (legacy systems)
 * - HL7 v2 (hospital systems)
 * - FHIR (modern healthcare)
 * - CSV (batch import)
 *
 * Все парсеры конвертируют свои форматы в этот DTO.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVasRecordRequestDTO {
    /**
     * MRN пациента (обязательно)
     * Примеры: "EMR-12345678", "MRN-987654"
     */
    private String patientMrn;

    /**
     * Уровень боли по VAS (0-10, обязательно)
     */
    private Integer vasLevel;

    /**
     * ID устройства/системы, отправившей данные
     * Примеры: "MONITOR-001", "TABLET-WARD-A", "VAS-DEVICE-123"
     */
    private String deviceId;

    /**
     * Локация пациента
     * Примеры: "Ward A, Bed 12", "ICU-3", "ER-Room-5"
     */
    private String location;

    /**
     * Локация боли на теле пациента
     * Примеры: "Shoulder", "Leg", "Abdomen", "Chest", "Back"
     */
    private String painPlace;

    /**
     * Временная метка записи VAS
     * Если null - используется текущее время сервера
     */
    private LocalDateTime timestamp;

    /**
     * Дополнительные заметки (опционально)
     */
    private String notes;

    /**
     * Источник данных (для аудита)
     * Примеры: "VAS_MONITOR", "MANUAL_ENTRY", "EMR_SYSTEM"
     */
    private String source;

    /**
     * Формат исходных данных (автоматически определяется)
     */
    private DataFormat format;

    public enum DataFormat {
        JSON,
        XML,
        HL7_V2,
        FHIR,
        CSV
    }
}
