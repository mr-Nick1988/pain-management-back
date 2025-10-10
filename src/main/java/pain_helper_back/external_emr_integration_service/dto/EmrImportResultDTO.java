package pain_helper_back.external_emr_integration_service.dto;


import lombok.Data;
import pain_helper_back.enums.EmrSourceType;
import pain_helper_back.enums.MatchConfidence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * DTO для результата импорта EMR данных из внешней системы.
 * Содержит информацию о успешности операции, найденных совпадениях и возможных проблемах.
 */
@Data
public class EmrImportResultDTO {
    //Успешность импорта.
    private boolean success;
    //Сообщение о результате импорта.
    private String message;
    //ID пациента в FHIR системе.
    private String externalPatientIdInFhirResource;

    //ID пациента в нашей системе (doctor.entity.Patients или nurse.entity.Patient//TODO).
    //Заполняется, если пациент был создан или найден.
    private Long internalPatientId;
    //Уровень уверенности при сопоставлении с существующим пациентом.
    private MatchConfidence matchConfidence;
    //Был ли создан новый пациент или найден существующий.
    private boolean newPatientCreated;
    //Источник данных.
    private EmrSourceType sourceType;
    //Время импорта.
    private LocalDateTime importedAt;
    //Количество импортированных лабораторных показателей.
    private int observationsImported;
    //Список предупреждений (например, отсутствующие данные, неполные показатели).
    private List<String> warnings = new ArrayList<>();
    // Список ошибок, если импорт завершился с проблемами.
    private List<String> errors = new ArrayList<>();
    //Требуется ли ручная проверка (например, при низкой уверенности совпадения).
    private boolean requiresManualReview;
    //Дополнительная информация для ручной проверки.
    private String reviewNotes;

    // Вспомогательные методы для удобства

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public static EmrImportResultDTO success(String message) {
        EmrImportResultDTO result = new EmrImportResultDTO();
        result.setSuccess(true);
        result.setMessage(message);
        result.setImportedAt(LocalDateTime.now());
        return result;
    }

    public static EmrImportResultDTO failure(String message) {
        EmrImportResultDTO result = new EmrImportResultDTO();
        result.setSuccess(false);
        result.setMessage(message);
        result.setImportedAt(LocalDateTime.now());
        return result;
    }
}
