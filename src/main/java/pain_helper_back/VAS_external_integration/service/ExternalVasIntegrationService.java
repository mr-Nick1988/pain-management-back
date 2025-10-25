package pain_helper_back.VAS_external_integration.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequest;
import pain_helper_back.VAS_external_integration.parser.CsvVasParser;
import pain_helper_back.VAS_external_integration.parser.VasFormatParser;
import pain_helper_back.analytics.event.VasRecordedEvent;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.VasRepository;
import pain_helper_back.nurse.service.NurseService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Сервис для обработки VAS данных из внешних систем.
 *
 * ФУНКЦИИ:
 * - Конвертация внешних данных во внутренний формат
 * - Сохранение VAS в БД
 * - Автоматическая генерация рекомендаций
 * - Batch обработка CSV
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExternalVasIntegrationService {
    private final PatientRepository patientRepository;
    private final VasRepository vasRepository;
    private final NurseService nurseService;
    private final CsvVasParser csvParser;
    private final ApplicationEventPublisher eventPublisher;
    private final pain_helper_back.pain_escalation_tracking.service.PainEscalationService painEscalationService;

    /*
     * Обработка одной VAS записи из внешней системы
     *
     * @param externalVas Внешняя VAS запись
     * @return ID созданной VAS записи
     */
    public Long processExternalVasRecord(ExternalVasRecordRequest externalVas) {
        log.info("Processing external VAS record: patientMrn={}, vasLevel={}, source={}",
                externalVas.getPatientMrn(), externalVas.getVasLevel(), externalVas.getSource());

        // 1. Найти пациента по MRN
        Patient patient = patientRepository.findByMrn(externalVas.getPatientMrn())
                .orElseThrow(() -> new RuntimeException(
                        "Patient not found with MRN: " + externalVas.getPatientMrn()));
        // 2. Создать VAS запись
        Vas vas = new Vas();
        vas.setPatient(patient);
        vas.setVasLevel(externalVas.getVasLevel());
        vas.setRecordedAt(externalVas.getTimestamp());
        vas.setLocation(externalVas.getLocation());
        vas.setNotes(externalVas.getNotes());
        vas.setRecordedBy("EXTERNAL_" + externalVas.getSource()); // Помечаем как внешний источник

        Vas savedVas = vasRepository.save(vas);

        log.info("VAS record saved: vasId={}, patientMrn={}, vasLevel={}",
                savedVas.getId(), externalVas.getPatientMrn(), externalVas.getVasLevel());

        // 2.1. Публикация события VAS (EXTERNAL источник - внешнее устройство)
        eventPublisher.publishEvent(new VasRecordedEvent(
                this,
                savedVas.getId(),
                externalVas.getPatientMrn(),
                "EXTERNAL_" + externalVas.getSource(), // recordedBy - помечаем как внешний источник
                externalVas.getTimestamp() != null ? externalVas.getTimestamp() : java.time.LocalDateTime.now(),
                externalVas.getVasLevel(),
                externalVas.getLocation(), // painLocation
                externalVas.getVasLevel() >= 8, // isCritical если боль >= 8
                "EXTERNAL", // vasSource - внешний источник
                externalVas.getDeviceId() // deviceId устройства
        ));

        log.info("VAS_RECORDED event published: source=EXTERNAL, device={}, vasLevel={}",
                externalVas.getDeviceId(), externalVas.getVasLevel());

        // 2.2. 🔥 АВТОМАТИЧЕСКАЯ ПРОВЕРКА ЭСКАЛАЦИИ БОЛИ
        painEscalationService.handleNewVasRecord(patient.getMrn(), externalVas.getVasLevel());

        // 3. Автоматическая генерация рекомендации (если VAS >= 4)
        if (externalVas.getVasLevel() >= 4) {
            try {
                nurseService.createRecommendation(patient.getMrn());
                log.info("Recommendation generated automatically for patient: {}", patient.getMrn());
            } catch (Exception e) {
                log.error("Failed to generate recommendation for patient {}: {}",
                        patient.getMrn(), e.getMessage());
                // Не бросаем исключение - VAS уже сохранен
            }
        }
        return savedVas.getId();
    }

    /*
     * Batch обработка VAS записей из CSV
     *
     * @param csvData CSV данные
     * @return Статистика обработки
     */
    public Map<String, Object> processBatchVasRecords(String csvData) throws VasFormatParser.ParseException {
        log.info("Processing batch VAS import from CSV");

        // Парсинг CSV
        List<ExternalVasRecordRequest> records = csvParser.parseMultiple(csvData);
        int total = records.size();
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        List<Long> createdVasIds = new ArrayList<>();

        // Обработка каждой записи
        for (int i = 0; i < records.size(); i++) {
            ExternalVasRecordRequest record = records.get(i);

            try {
                Long vasId = processExternalVasRecord(record);
                createdVasIds.add(vasId);
                success++;
            } catch (Exception e) {
                failed++;
                String error = String.format("Line %d: %s (MRN: %s)",
                        i + 2, e.getMessage(), record.getPatientMrn());
                errors.add(error);
                log.warn("Failed to process CSV line {}: {}", i + 2, e.getMessage());
            }
        }
        log.info("Batch import completed: total={}, success={}, failed={}", total, success, failed);
        // Формирование результата
        Map<String, Object> result = new HashMap<>();
        result.put("status", failed == 0 ? "success" : "partial_success");
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("createdVasIds", createdVasIds);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        return result;
    }
}
