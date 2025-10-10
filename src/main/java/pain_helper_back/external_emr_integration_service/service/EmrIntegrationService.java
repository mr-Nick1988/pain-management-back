package pain_helper_back.external_emr_integration_service.service;


import pain_helper_back.common.patients.dto.EmrDTO;
import pain_helper_back.external_emr_integration_service.dto.EmrImportResultDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirObservationDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirPatientDTO;

import java.util.List;

/*
 * Сервис для интеграции с внешними EMR системами через FHIR стандарт.
 *
 * ОСНОВНЫЕ ФУНКЦИИ:
 * 1. Импорт пациентов из внешних FHIR систем (другие больницы)
 * 2. Генерация моковых пациентов для разработки/тестирования
 * 3. Patient Reconciliation (сопоставление с существующими пациентами)
 * 4. Конвертация FHIR данных в формат для nurse/doctor модулей
 * 5. Присвоение внутренних EMR номеров
 */
public interface EmrIntegrationService {

    /**
     * Импортировать пациента из FHIR системы по ID.
     */
    EmrImportResultDTO importPatientFromFhir(String fhirPatientId, String importedBy);

    /**
     * Сгенерировать и импортировать моковый пациент.
     */
    EmrImportResultDTO generateAndImportMockPatient(String createdBy);

    /**
     * Сгенерировать и импортировать batch (пакет) моковых пациентов.
     */
    List<EmrImportResultDTO> generateAndImportMockBatch(int count, String createdBy);

    /**
     * Поиск пациентов в FHIR системе по имени и дате рождения.
     */
    List<FhirPatientDTO> searchPatientsInFhir(String firstName, String lastName, String birthDate);

    /**
     * Получить лабораторные показатели для пациента из FHIR системы.
     */
    List<FhirObservationDTO> getObservationsForPatient(String fhirPatientId);

    /**
     * Конвертировать FHIR Observations в EmrDTO (формат nurse модуля).
     */
    EmrDTO convertObservationsToEmr(List<FhirObservationDTO> observations, String createdBy);

    /**
     * Проверить, был ли пациент уже импортирован ранее.
     */
    boolean isPatientAlreadyImported(String fhirPatientId);

    /**
     * Получить внутренний EMR номер по внешнему FHIR ID.
     */
    String getInternalEmrNumber(String fhirPatientId);
}
