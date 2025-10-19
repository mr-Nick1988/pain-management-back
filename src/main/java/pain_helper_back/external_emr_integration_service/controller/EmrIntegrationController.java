package pain_helper_back.external_emr_integration_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.patients.dto.EmrDTO;
import pain_helper_back.external_emr_integration_service.dto.EmrImportResultDTO;
import pain_helper_back.external_emr_integration_service.dto.EmrSyncResultDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirObservationDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirPatientDTO;
import pain_helper_back.external_emr_integration_service.service.EmrIntegrationService;
import pain_helper_back.external_emr_integration_service.service.EmrSyncScheduler;
import pain_helper_back.external_emr_integration_service.service.TreatmentProtocolIcdExtractor;

import java.util.List;
import java.util.Map;

/*
 * Controller for EMR integration with external FHIR systems.
 *
 * ENDPOINTS:
 * - POST /api/emr/import/{fhirPatientId} - импорт пациента из FHIR
 * - POST /api/emr/mock/generate - создать 1 моковый пациент
 * - POST /api/emr/mock/generate-batch - создать N моковых пациентов
 * - GET /api/emr/search - поиск пациентов в FHIR
 * - GET /api/emr/observations/{fhirPatientId} - получить лабораторные анализы
 * - POST /api/emr/convert-observations - конвертировать FHIR → EmrDTO
 * - GET /api/emr/check-import/{fhirPatientId} - проверить, импортирован ли пациент
 */
@RestController
@RequestMapping("/api/emr")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmrIntegrationController {

    private final EmrIntegrationService emrIntegrationService;
    private final TreatmentProtocolIcdExtractor treatmentProtocolIcdExtractor;

    /**
     * Импорт пациента из FHIR системы другой больницы.
     * Получает данные пациента и лабораторные анализы из внешнего FHIR сервера.
     */
    @PostMapping("/import/{fhirPatientId}")
    public ResponseEntity<EmrImportResultDTO> importPatientFromFhir(
            @PathVariable String fhirPatientId,
            @RequestParam(defaultValue = "system") String importedBy) {

        EmrImportResultDTO result = emrIntegrationService.importPatientFromFhir(fhirPatientId, importedBy);
        HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Генерация одного мокового (тестового) пациента.
     * Используется для разработки и тестирования UI.
     */
    @PostMapping("/mock/generate")
    public ResponseEntity<EmrImportResultDTO> generateMockPatient(
            @RequestParam(defaultValue = "system") String createdBy) {

        EmrImportResultDTO result = emrIntegrationService.generateAndImportMockPatient(createdBy);
        HttpStatus status = result.isSuccess() ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Генерация batch (пакета) моковых пациентов.
     * Максимум 100 пациентов за раз.
     */
    @PostMapping("/mock/generate-batch")
    public ResponseEntity<List<EmrImportResultDTO>> generateMockBatch(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "system") String createdBy) {

        if (count > 100) count = 100;

        List<EmrImportResultDTO> results = emrIntegrationService.generateAndImportMockBatch(count, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /**
     * Поиск пациентов в FHIR системе по имени и дате рождения.
     */
    @GetMapping("/search")
    public List<FhirPatientDTO> searchPatientsInFhir(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String birthDate) {

        return emrIntegrationService.searchPatientsInFhir(firstName, lastName, birthDate);
    }

    /**
     * Получить лабораторные анализы для пациента из FHIR системы.
     * Возвращает креатинин, тромбоциты, лейкоциты и другие показатели.
     */
    @GetMapping("/observations/{fhirPatientId}")
    public List<FhirObservationDTO> getObservationsForPatient(@PathVariable String fhirPatientId) {
        return emrIntegrationService.getObservationsForPatient(fhirPatientId);
    }

    /**
     * Конвертировать FHIR Observations в EmrDTO (формат nurse модуля).
     * Рассчитывает GFR из креатинина и подготавливает данные для Treatment Protocol.
     */
    @PostMapping("/convert-observations")
    public EmrDTO convertObservationsToEmr(
            @RequestBody List<FhirObservationDTO> observations,
            @RequestParam(defaultValue = "system") String createdBy) {

        return emrIntegrationService.convertObservationsToEmr(observations, createdBy);
    }

    /**
     * Проверить, был ли пациент уже импортирован ранее.
     * Возвращает статус импорта и внутренний EMR номер.
     */
    @GetMapping("/check-import/{fhirPatientId}")
    public ImportCheckResponse checkIfPatientImported(@PathVariable String fhirPatientId) {
        boolean alreadyImported = emrIntegrationService.isPatientAlreadyImported(fhirPatientId);
        String internalEmrNumber = emrIntegrationService.getInternalEmrNumber(fhirPatientId);
        return new ImportCheckResponse(alreadyImported, internalEmrNumber);
    }

    public record ImportCheckResponse(boolean alreadyImported, String internalEmrNumber) {
    }
    
    // ============================================
    // EMR СИНХРОНИЗАЦИЯ
    // ============================================
    
    private final EmrSyncScheduler emrSyncScheduler;
    
    /*
     * Ручная синхронизация всех FHIR пациентов.
     * Обновляет лабораторные показатели из внешних FHIR систем.
     * 
     * КОГДА ИСПОЛЬЗОВАТЬ:
     * - Для тестирования синхронизации
     * - Для принудительного обновления всех данных
     * - После добавления новых пациентов из FHIR
     */
    @PostMapping("/sync/all")
    public ResponseEntity<EmrSyncResultDTO> syncAllPatients() {
        EmrSyncResultDTO result = emrSyncScheduler.manualSyncAll();
        return ResponseEntity.ok(result);
    }
    
    /*
     * Синхронизация конкретного пациента по MRN.
     * 
     * КОГДА ИСПОЛЬЗОВАТЬ:
     * - Когда нужно обновить данные конкретного пациента
     * - Перед генерацией новой рекомендации
     * - После получения информации о изменении состояния пациента
     */
    @PostMapping("/sync/patient/{mrn}")
    public ResponseEntity<String> syncPatientByMrn(@PathVariable String mrn) {
        boolean hasChanges = emrSyncScheduler.syncPatientByMrn(mrn);
        String message = hasChanges ? 
                "Синхронизация завершена успешно. Обнаружены изменения в EMR." :
                "Синхронизация завершена. Изменений не обнаружено.";
        return ResponseEntity.ok(message);
    }
    
    /**
     * Получить список ICD кодов из Treatment Protocol.
     * Используется для проверки, какие диагнозы используются для генерации моковых пациентов.
     */
    @GetMapping("/protocol-icd-codes")
    public ResponseEntity<Map<String, Object>> getProtocolIcdCodes() {
        int count = treatmentProtocolIcdExtractor.getProtocolIcdCodesCount();
        return ResponseEntity.ok(Map.of(
            "count", count,
            "message", "Mock patients are generated with " + count + " ICD codes from Treatment Protocol",
            "info", "These are the contraindication codes that affect treatment selection"
        ));
    }
}
