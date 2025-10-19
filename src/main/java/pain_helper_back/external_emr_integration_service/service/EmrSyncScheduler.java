package pain_helper_back.external_emr_integration_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.enums.EmrSourceType;
import pain_helper_back.external_emr_integration_service.client.HapiFhirClient;
import pain_helper_back.external_emr_integration_service.dto.EmrSyncResultDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirObservationDTO;
import pain_helper_back.external_emr_integration_service.entity.EmrMapping;
import pain_helper_back.external_emr_integration_service.repository.EmrMappingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 *  EmrSyncScheduler - Автоматическая синхронизация EMR данных
 *
 * ЗАЧЕМ:
 * - Автоматически обновляет лабораторные показатели пациентов из FHIR серверов
 * - Отслеживает критические изменения (GFR, PLT, WBC и т.д.)
 * - Запускается по расписанию (каждые 6 часов)
 * - Можно запустить вручную через REST API
 *
 * WORKFLOW:
 * 1. Получает список всех пациентов, импортированных из FHIR
 * 2. Для каждого пациента запрашивает свежие Observations из FHIR
 * 3. Сравнивает с текущими значениями в БД
 * 4. Обновляет EMR если есть изменения
 * 5. Создает алерты при критических изменениях
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmrSyncScheduler {
    private final EmrMappingRepository emrMappingRepository;
    private final EmrRepository emrRepository;
    private final HapiFhirClient hapiFhirClient;
    private final EmrChangeDetectionService emrChangeDetectionService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Scheduled(cron = "0 0 */6 * * *")
    public EmrSyncResultDTO syncAllFhirPatients() {
        log.info("Starting scheduled EMR sync for all FHIR patients...");

        long startTime = System.currentTimeMillis();

        EmrSyncResultDTO result = new EmrSyncResultDTO();
        result.setSyncStartTime(LocalDateTime.now());

        // Получаем всех пациентов из FHIR серверов (не моковых)
        List<EmrMapping> fhirMappings = emrMappingRepository.findBySourceType(EmrSourceType.FHIR_SERVER);

        if (fhirMappings.isEmpty()) {
            log.info("️No FHIR patients found for sync");
            result.setTotalPatientsProcessed(0);
            result.setSuccessfulSyncs(0);
            result.setFailedSyncs(0);
            result.setPatientsWithChanges(0);
            result.setSyncEndTime(LocalDateTime.now());
            result.setDurationMs(System.currentTimeMillis() - startTime);
            result.setStatus(EmrSyncResultDTO.SyncStatus.SUCCESS);
            result.setMessage("No FHIR patients to sync");
            return result;
        }
        log.info("Found {} FHIR patients to sync", fhirMappings.size());
        result.setTotalPatientsProcessed(fhirMappings.size());

        int successCount = 0;
        int failureCount = 0;
        int unchangedCount = 0;

        List<String> errors = new ArrayList<>();
        for (EmrMapping mapping : fhirMappings) {
            try {
                boolean hasChanges = syncSinglePatient(mapping);
                if (hasChanges) {
                    successCount++;
                } else {
                    unchangedCount++;
                }
            } catch (Exception e) {
                failureCount++;
                String errorMsg = String.format("Failed to sync patient: fhirId=%s, error=%s",
                        mapping.getExternalFhirId(), e.getMessage());
                errors.add(errorMsg);
                log.error(" X " + errorMsg);
            }
        }
        long duration = System.currentTimeMillis() - startTime;
        result.setSuccessfulSyncs(successCount);
        result.setFailedSyncs(failureCount);
        result.setPatientsWithChanges(successCount);
        result.setErrorMessages(errors);
        result.setSyncEndTime(LocalDateTime.now());
        result.setDurationMs(duration);
        
        // Установка статуса
        if (failureCount == 0) {
            result.setStatus(EmrSyncResultDTO.SyncStatus.SUCCESS);
            result.setMessage("All patients synced successfully");
        } else if (successCount > 0) {
            result.setStatus(EmrSyncResultDTO.SyncStatus.PARTIAL_SUCCESS);
            result.setMessage(String.format("Partial success: %d succeeded, %d failed", successCount, failureCount));
        } else {
            result.setStatus(EmrSyncResultDTO.SyncStatus.FAILED);
            result.setMessage("All sync attempts failed");
        }
        
        result.setCriticalAlertsGenerated(result.getAlerts().size());

        log.info("EMR sync completed: success={}, unchanged={}, failed={}, duration={}ms",
                successCount, unchangedCount, failureCount, duration);

        // Отправка уведомлений о критических алертах
        if (!result.getAlerts().isEmpty()) {
            log.warn("Detected {} critical alerts, sending notifications...", result.getAlerts().size());
            
            // WebSocket уведомления (real-time)
            webSocketNotificationService.sendCriticalAlerts(result.getAlerts());
            
            // Email уведомления отключены (опционально, настраивается через Spring Mail)
        }

        return result;
    }

    /*
     * Синхронизация одного пациента.
     *
     * @param mapping маппинг между внешним FHIR ID и внутренним EMR
     * @return true если были изменения, false если данные не изменились
     */
    public boolean syncSinglePatient(EmrMapping mapping) {
        String fhirPatientId = mapping.getExternalFhirId();
        String internalEmrNumber = mapping.getInternalEmrNumber();

        log.debug("Syncing patient: fhirId={}, emrNumber={}", fhirPatientId, internalEmrNumber);

        // 1. Получаем свежие Observations из FHIR
        List<FhirObservationDTO> freshObservations;
        try {
            freshObservations = hapiFhirClient.getObservationsForPatient(fhirPatientId);
        } catch (Exception e) {
            log.error("Failed to fetch observations from FHIR: fhirId={}, error={}",
                    fhirPatientId, e.getMessage());
            throw new RuntimeException("Failed to fetch FHIR observations: " + e.getMessage(), e);
        }

        if (freshObservations.isEmpty()) {
            log.warn("No observations found in FHIR for patient: {}", fhirPatientId);
            return false;
        }

        // 2. Получаем текущий EMR из БД
        Emr currentEmr = emrRepository.findByPatientMrn(internalEmrNumber)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("EMR not found for MRN: " + internalEmrNumber));

        // 3. Создаем новый EMR объект с обновленными данными
        Emr updatedEmr = createUpdatedEmr(currentEmr, freshObservations);

        // 4. Проверяем, есть ли изменения
        boolean hasChanges = emrChangeDetectionService.detectChanges(currentEmr, updatedEmr);

        if (!hasChanges) {
            log.debug("No changes detected for patient: {}", internalEmrNumber);
            return false;
        }

        // 5. Сохраняем обновленный EMR
        updatedEmr.setUpdatedAt(LocalDateTime.now());
        updatedEmr.setUpdatedBy("EMR_SYNC_SCHEDULER");
        emrRepository.save(updatedEmr);

        log.info("EMR updated for patient: mrn={}, changes detected", internalEmrNumber);

        // 6. Проверяем критические изменения и создаем алерты
        emrChangeDetectionService.checkCriticalChanges(currentEmr, updatedEmr, internalEmrNumber);

        return true;
    }

    /*
     * Создает обновленный EMR объект из свежих FHIR Observations.
     */
    private Emr createUpdatedEmr(Emr currentEmr, List<FhirObservationDTO> freshObservations) {
        Emr updated = new Emr();
        updated.setId(currentEmr.getId());
        updated.setPatient(currentEmr.getPatient());
        updated.setCreatedAt(currentEmr.getCreatedAt());
        updated.setCreatedBy(currentEmr.getCreatedBy());

        // Копируем текущие значения (будут перезаписаны если есть новые)
        updated.setGfr(currentEmr.getGfr());
        updated.setPlt(currentEmr.getPlt());
        updated.setWbc(currentEmr.getWbc());
        updated.setSodium(currentEmr.getSodium());
        updated.setSat(currentEmr.getSat());
        updated.setHeight(currentEmr.getHeight());
        updated.setWeight(currentEmr.getWeight());
        updated.setChildPughScore(currentEmr.getChildPughScore());
        // Обновляем из свежих Observations
        for (FhirObservationDTO obs : freshObservations) {
            String loincCode = obs.getLoincCode();
            Double value = obs.getValue();
            if (value == null) continue;

            switch (loincCode) {
                case "2160-0":
                    updated.setGfr(calculateGfrCategory(value));
                    break;  // Креатинин → GFR
                case "777-3":
                    updated.setPlt(value);
                    break;  // Тромбоциты
                case "6690-2":
                    updated.setWbc(value);
                    break;  // Лейкоциты
                case "2951-2":
                    updated.setSodium(value);
                    break;  // Натрий
                case "59408-5":
                    updated.setSat(value);
                    break;  // Сатурация
                case "8302-2":
                    updated.setHeight(value);
                    break;  // Рост
                case "29463-7":
                    updated.setWeight(value);
                    break;  // Вес
                case "1975-2":
                    updated.setChildPughScore(calculateChildPughFromBilirubin(value));
                    break;  // Билирубин
            }
        }

        return updated;
    }
    /*
     * Рассчитать GFR категорию (копия из EmrIntegrationServiceImpl).
     */
    private String calculateGfrCategory(double creatinine) {
        double estimatedGfr = 100.0 / creatinine;
        if (estimatedGfr > 120) estimatedGfr = 120;
        if (estimatedGfr < 0) estimatedGfr = 0;

        boolean useLetter = Math.random() < 0.5;

        if (useLetter) {
            if (estimatedGfr >= 90) return "A";
            if (estimatedGfr >= 60) return "B";
            if (estimatedGfr >= 30) return "C";
            return "D";
        } else {
            return String.valueOf((int) Math.round(estimatedGfr));
        }
    }
    /*
     * Рассчитать Child-Pugh Score (копия из EmrIntegrationServiceImpl).
     */
    private String calculateChildPughFromBilirubin(double bilirubin) {
        if (bilirubin < 2.0) return "A";
        if (bilirubin < 3.0) return "B";
        return "C";
    }
    /**
     * Ручная синхронизация конкретного пациента.
     * Используется через REST API.
     */
    public boolean syncPatientByMrn(String mrn) {
        log.info("Manual sync requested for patient: mrn={}", mrn);

        EmrMapping mapping = emrMappingRepository.findByInternalEmrNumber(mrn)
                .orElseThrow(() -> new RuntimeException("Patient not found or not imported from FHIR: " + mrn));

        return syncSinglePatient(mapping);
    }

    /*
     * Ручная синхронизация всех пациентов (для тестирования).
     */
    public EmrSyncResultDTO manualSyncAll() {
        log.info("Manual sync ALL patients requested");
        return syncAllFhirPatients();
    }
}
