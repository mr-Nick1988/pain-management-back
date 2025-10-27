package pain_helper_back.VAS_external_integration.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequest;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordResponse;
import pain_helper_back.VAS_external_integration.dto.VasMonitorStats;
import pain_helper_back.VAS_external_integration.parser.CsvVasParser;
import pain_helper_back.VAS_external_integration.parser.VasFormatParser;
import pain_helper_back.analytics.event.VasRecordedEvent;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.VasRepository;
import pain_helper_back.nurse.service.NurseService;

import java.time.LocalDateTime;
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
    
    // Self-injection для вызова методов с @Transactional(propagation = REQUIRES_NEW)
    @Autowired
    @Lazy
    private ExternalVasIntegrationService self;

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
        // ВАЖНО: Используем отдельную транзакцию, чтобы ошибка рекомендации не откатила VAS
        if (externalVas.getVasLevel() >= 4) {
            // Проверяем есть ли уже PENDING рекомендация у пациента
            boolean hasPending = patient.getRecommendations().stream()
                    .anyMatch(r -> r.getStatus() == pain_helper_back.enums.RecommendationStatus.PENDING);
            
            if (hasPending) {
                log.info("Skipping recommendation generation - PENDING recommendation already exists for patient {}", 
                        patient.getMrn());
            } else {
                try {
                    // Вызываем через self-proxy для применения @Transactional(propagation = REQUIRES_NEW)
                    self.createRecommendationInNewTransaction(patient.getMrn());
                    log.info("Recommendation generated automatically for patient: {}", patient.getMrn());
                } catch (Exception e) {
                    log.error("Failed to generate recommendation for patient {}: {}",
                            patient.getMrn(), e.getMessage());
                    // Не бросаем исключение - VAS уже сохранен в основной транзакции
                }
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

    /*
     * Получить список VAS записей с фильтрами для мониторинга.
     *
     * @param deviceId Фильтр по ID устройства (optional)
     * @param location Фильтр по локации (optional)
     * @param timeRange Временной диапазон: "1h", "6h", "24h", "7d" (optional)
     * @param vasLevelMin Минимальный уровень VAS (optional)
     * @param vasLevelMax Максимальный уровень VAS (optional)
     * @return Список VAS записей с данными пациентов
     */
    @Transactional(readOnly = true)
    public List<ExternalVasRecordResponse> getVasRecords(
            String deviceId,
            String location,
            String timeRange,
            Integer vasLevelMin,
            Integer vasLevelMax) {

        log.info("Fetching VAS records: deviceId={}, location={}, timeRange={}, vasRange={}-{}",
                deviceId, location, timeRange, vasLevelMin, vasLevelMax);

        // Определяем временной диапазон
        LocalDateTime startTime = calculateStartTime(timeRange);

        // Получаем VAS записи с фильтрацией
        List<Vas> vasRecords = vasRepository.findAll().stream()
                .filter(v -> v.getRecordedBy() != null && v.getRecordedBy().startsWith("EXTERNAL_"))
                .filter(v -> startTime == null || v.getCreatedAt().isAfter(startTime))
                .filter(v -> deviceId == null || extractDeviceId(v.getRecordedBy()).contains(deviceId))
                .filter(v -> location == null || (v.getLocation() != null && v.getLocation().contains(location)))
                .filter(v -> vasLevelMin == null || v.getVasLevel() >= vasLevelMin)
                .filter(v -> vasLevelMax == null || v.getVasLevel() <= vasLevelMax)
                .sorted((v1, v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt())) // DESC по времени
                .toList();

        log.info("Found {} VAS records matching filters", vasRecords.size());

        // Конвертируем в DTO с данными пациента
        return vasRecords.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /*
     * Получить статистику по VAS записям за сегодня.
     *
     * @return Статистика: total, average, high pain alerts, active devices
     */
    @Transactional(readOnly = true)
    public VasMonitorStats getVasStatistics() {
        log.info("Calculating VAS statistics for today");

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();

        // Получаем все внешние VAS записи за сегодня
        List<Vas> todayRecords = vasRepository.findAll().stream()
                .filter(v -> v.getRecordedBy() != null && v.getRecordedBy().startsWith("EXTERNAL_"))
                .filter(v -> v.getCreatedAt().isAfter(startOfDay))
                .toList();

        int totalRecords = todayRecords.size();

        // Средний VAS
        double averageVas = todayRecords.isEmpty() ? 0.0 :
                todayRecords.stream()
                        .mapToInt(Vas::getVasLevel)
                        .average()
                        .orElse(0.0);

        // High pain alerts (VAS >= 7)
        int highPainAlerts = (int) todayRecords.stream()
                .filter(v -> v.getVasLevel() >= 7)
                .count();

        // Активные устройства (DISTINCT deviceId)
        long activeDevices = todayRecords.stream()
                .map(v -> extractDeviceId(v.getRecordedBy()))
                .distinct()
                .count();

        log.info("VAS Statistics: total={}, avg={}, highPain={}, devices={}",
                totalRecords, averageVas, highPainAlerts, activeDevices);

        return VasMonitorStats.builder()
                .totalRecordsToday(totalRecords)
                .averageVas(Math.round(averageVas * 10.0) / 10.0) // Округление до 1 знака
                .highPainAlerts(highPainAlerts)
                .activeDevices((int) activeDevices)
                .build();
    }

    /*
     * Вспомогательный метод: расчет начального времени по timeRange.
     */
    private LocalDateTime calculateStartTime(String timeRange) {
        if (timeRange == null) return null;

        return switch (timeRange) {
            case "1h" -> LocalDateTime.now().minusHours(1);
            case "6h" -> LocalDateTime.now().minusHours(6);
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d" -> LocalDateTime.now().minusDays(7);
            default -> null;
        };
    }

    /*
     * Вспомогательный метод: извлечение deviceId из recordedBy.
     * Format: "EXTERNAL_VAS_MONITOR" → "VAS_MONITOR"
     */
    private String extractDeviceId(String recordedBy) {
        if (recordedBy == null || !recordedBy.startsWith("EXTERNAL_")) {
            return "UNKNOWN";
        }
        return recordedBy.substring("EXTERNAL_".length());
    }

    /*
     * Вспомогательный метод: конвертация Vas entity в Response DTO.
     */
    private ExternalVasRecordResponse convertToResponse(Vas vas) {
        Patient patient = vas.getPatient();

        return ExternalVasRecordResponse.builder()
                .id(vas.getId())
                .patientMrn(patient.getMrn())
                .patientFirstName(patient.getFirstName())
                .patientLastName(patient.getLastName())
                .vasLevel(vas.getVasLevel())
                .deviceId(extractDeviceId(vas.getRecordedBy()))
                .location(vas.getLocation())
                .timestamp(vas.getRecordedAt())
                .notes(vas.getNotes())
                .source(extractDeviceId(vas.getRecordedBy()))
                .createdAt(vas.getCreatedAt())
                .build();
    }

    /**
     * Создание рекомендации в отдельной транзакции.
     * 
     * ВАЖНО: Использует REQUIRES_NEW propagation, чтобы ошибка создания рекомендации
     * не откатывала сохранение VAS записи.
     * 
     * ПРИЧИНА: При VAS >= 4 автоматически генерируется рекомендация, но если у пациента
     * уже есть неразрешенная рекомендация, то createRecommendation() бросает исключение.
     * Без REQUIRES_NEW это приводит к UnexpectedRollbackException и откату VAS.
     * 
     * @param mrn MRN пациента
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void createRecommendationInNewTransaction(String mrn) {
        nurseService.createRecommendation(mrn);
    }
}
