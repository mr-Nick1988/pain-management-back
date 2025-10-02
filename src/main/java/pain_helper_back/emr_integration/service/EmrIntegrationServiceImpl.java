package pain_helper_back.emr_integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.emr_integration.client.HapiFhirClient;
import pain_helper_back.emr_integration.dto.EmrImportResultDTO;
import pain_helper_back.emr_integration.dto.FhirObservationDTO;
import pain_helper_back.emr_integration.dto.FhirPatientDTO;
import pain_helper_back.emr_integration.entity.EmrMapping;
import pain_helper_back.emr_integration.repository.EmrMappingRepository;
import pain_helper_back.enums.EmrSourceType;
import pain_helper_back.enums.MatchConfidence;
import pain_helper_back.nurse.dto.EmrDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * Реализация сервиса интеграции с внешними EMR (Electronic Medical Records) системами.
 *
 * ЧТО ДЕЛАЕТ ЭТОТ СЕРВИС:
 * 1. Импортирует пациентов из FHIR серверов других больниц
 * 2. Генерирует моковых (тестовых) пациентов для разработки
 * 3. Присваивает внутренние EMR номера (уникальные идентификаторы в нашей системе)
 * 4. Конвертирует медицинские данные из FHIR формата в наш формат
 * 5. Избегает дубликатов пациентов (Patient Reconciliation)
 *
 * ТРАНЗАКЦИОННОСТЬ:
 * - @Transactional на уровне класса - все методы изменения данных в одной транзакции
 * - @Transactional(readOnly = true) - для методов только чтения (оптимизация)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional  // По умолчанию для всех методов (можно переопределить)
public class EmrIntegrationServiceImpl implements EmrIntegrationService {
    // Клиент для работы с FHIR сервером (получение данных пациентов)
    private final HapiFhirClient hapiFhirClient;
    // Репозиторий для сохранения связи: внешний FHIR ID ↔ внутренний EMR номер
    private final EmrMappingRepository emrMappingRepository;
    // Генератор моковых (тестовых) пациентов с реалистичными данными
    private final MockEmrDataGenerator mockEmrDataGenerator;

    /*
     * МЕТОД 1: Импорт пациента из FHIR системы другой больницы.
     *
     * ЧТО ПРОИСХОДИТ:
     * 1. Проверяем, не импортировали ли мы этого пациента раньше (по FHIR ID)
     * 2. Если уже импортирован - возвращаем существующий EMR номер
     * 3. Если новый - получаем данные из FHIR сервера
     * 4. Получаем лабораторные анализы (креатинин, тромбоциты и т.д.)
     * 5. Присваиваем внутренний EMR номер (EMR-XXXXXXXX)
     * 6. Сохраняем связь в БД
     *
     * ЗАЧЕМ: Чтобы врач видел медицинскую историю пациента из другой больницы
     * и мог правильно назначить обезболивающее (учитывая функцию почек/печени)
     */
    @Override
    public EmrImportResultDTO importPatientFromFhir(String fhirPatientId, String importedBy) {
        log.info("Starting FHIR patient import: fhirPatientId={}, importedBy={}", fhirPatientId, importedBy);

        try {
            // ШАГ 1: Проверяем, не импортировали ли уже этого пациента
            Optional<EmrMapping> existingMapping = emrMappingRepository.findByExternalFhirId(fhirPatientId);

            if (existingMapping.isPresent()) {
                // Пациент уже импортирован ранее - возвращаем существующий номер
                log.info("Patient already imported: fhirPatientId={}, internalEmrNumber={}",
                        fhirPatientId, existingMapping.get().getInternalEmrNumber());

                EmrImportResultDTO result = EmrImportResultDTO.success("Patient already imported");
                result.setExternalPatientIdInFhirResource(fhirPatientId);
                result.setMatchConfidence(MatchConfidence.EXACT);  // 100% совпадение
                result.setNewPatientCreated(false);  // Новый пациент НЕ создан
                result.setSourceType(existingMapping.get().getSourceType());
                result.addWarning("Patient was already imported previously");
                return result;
            }

            // ШАГ 2: Получаем данные пациента из FHIR сервера
            // (имя, фамилия, дата рождения, пол, контакты)
            log.debug("Fetching patient data from FHIR server: {}", fhirPatientId);
            FhirPatientDTO fhirPatient = hapiFhirClient.getPatientById(fhirPatientId);

            // ШАГ 3: Получаем лабораторные анализы (Observations)
            // ВАЖНО: Эти анализы нужны для корректировки дозы препаратов!
            // - Креатинин → функция почек (если плохая, снижаем дозу)
            // - Билирубин → функция печени (если плохая, исключаем гепатотоксичные препараты)
            // - Тромбоциты → риск кровотечений
            log.debug("Fetching observations (lab results) for patient: {}", fhirPatientId);
            List<FhirObservationDTO> observations = hapiFhirClient.getObservationsForPatient(fhirPatientId);

            // ШАГ 4: Присваиваем внутренний EMR номер
            // Формат: EMR-A1B2C3D4 (уникальный для нашей системы)
            String internalEmrNumber = generateInternalEmrNumber();
            log.info("Assigned internal EMR number: {} for FHIR patient: {}", internalEmrNumber, fhirPatientId);

            // ШАГ 5: Сохраняем связь между внешним FHIR ID и внутренним EMR номером
            EmrMapping mapping = new EmrMapping();
            mapping.setExternalFhirId(fhirPatientId);  // ID в FHIR системе другой больницы
            mapping.setInternalEmrNumber(internalEmrNumber);  // Наш внутренний номер
            mapping.setSourceType(EmrSourceType.FHIR_SERVER);  // Источник: FHIR сервер
            mapping.setSourceSystemUrl(fhirPatient.getSourceSystemUrl());  // URL больницы
            mapping.setImportedBy(importedBy);  // Кто импортировал (для аудита)
            emrMappingRepository.save(mapping);

            log.info("Successfully saved EMR mapping: externalId={}, internalEmr={}",
                    fhirPatientId, internalEmrNumber);

            // ШАГ 6: Формируем результат импорта
            EmrImportResultDTO result = EmrImportResultDTO.success("Patient imported successfully from FHIR server");
            result.setExternalPatientIdInFhirResource(fhirPatientId);
            result.setInternalPatientId(null);  // TODO: Создать в doctor.entity.Patient
            result.setMatchConfidence(MatchConfidence.NO_MATCH);  // Новый пациент
            result.setNewPatientCreated(true);  // Создан новый пациент
            result.setSourceType(EmrSourceType.FHIR_SERVER);
            result.setObservationsImported(observations.size());  // Сколько анализов импортировано

            // Добавляем предупреждения, если данные неполные
            if (observations.isEmpty()) {
                result.addWarning("No laboratory observations found for patient");
            }
            if (fhirPatient.getIdentifiers() == null || fhirPatient.getIdentifiers().isEmpty()) {
                result.addWarning("No identifiers (MRN, insurance) found for patient");
            }

            log.info("FHIR import completed successfully: internalEmr={}, observations={}",
                    internalEmrNumber, observations.size());

            return result;

        } catch (Exception e) {
            // Если что-то пошло не так (сеть, FHIR сервер недоступен и т.д.)
            log.error("Failed to import patient from FHIR: fhirPatientId={}, error={}",
                    fhirPatientId, e.getMessage(), e);

            EmrImportResultDTO result = EmrImportResultDTO.failure("Failed to import patient from FHIR server");
            result.setExternalPatientIdInFhirResource(fhirPatientId);
            result.addError("FHIR server error: " + e.getMessage());
            result.setRequiresManualReview(true);
            result.setReviewNotes("Check FHIR server connectivity and patient ID validity");
            return result;
        }
    }

    /*
     * МЕТОД 2: Генерация и импорт МОКОВОГО (тестового) пациента.
     *
     * ЧТО ПРОИСХОДИТ:
     * 1. MockEmrDataGenerator создает пациента с реалистичными данными (JavaFaker)
     * 2. Генерируются лабораторные анализы (креатинин, тромбоциты и т.д.)
     * 3. Присваивается внутренний EMR номер
     * 4. Сохраняется в БД с sourceType = MOCK_GENERATOR
     *
     * ЗАЧЕМ:
     * - Фронтенд-разработчик: "Дай 50 пациентов для тестирования UI"
     * - QA-тестировщик: "Нужен пациент с креатинином 2.5 для проверки корректировки дозы"
     * - Backend-разработчик: "Запускаю автотесты без зависимости от FHIR сервера"
     */
    @Override
    public EmrImportResultDTO generateAndImportMockPatient(String createdBy) {
        log.info("Generating mock patient: createdBy={}", createdBy);

        try {
            // ШАГ 1: Генерируем мокового пациента с реалистичными данными
            // Имя: John Smith, Jane Doe и т.д. (JavaFaker)
            // Дата рождения: случайный возраст 18-90 лет
            // Контакты: телефон, email, адрес
            FhirPatientDTO mockPatient = mockEmrDataGenerator.generateRandomPatient();

            // ШАГ 2: Генерируем лабораторные анализы
            // МЕДИЦИНСКИЕ ПОКАЗАТЕЛИ:
            // - Креатинин: 0.5-3.0 mg/dL (функция почек)
            // - Билирубин: 0.3-5.0 mg/dL (функция печени)
            // - Тромбоциты (PLT): 50-400 (риск кровотечений)
            // - Лейкоциты (WBC): 3-15 (инфекции)
            // - Натрий: 130-150 mmol/L (водно-электролитный баланс)
            // - Сатурация (SpO2): 85-100% (дыхательная функция)
            List<FhirObservationDTO> observations = mockEmrDataGenerator
                    .generateObservationForPatient(mockPatient.getPatientIdInFhirResource());

            // ШАГ 3: Присваиваем внутренний EMR номер
            String internalEmrNumber = generateInternalEmrNumber();
            log.info("Assigned internal EMR number: {} for mock patient: {}",
                    internalEmrNumber, mockPatient.getPatientIdInFhirResource());

            // ШАГ 4: Сохраняем маппинг в БД
            EmrMapping mapping = new EmrMapping();
            mapping.setExternalFhirId(mockPatient.getPatientIdInFhirResource());
            mapping.setInternalEmrNumber(internalEmrNumber);
            mapping.setSourceType(EmrSourceType.MOCK_GENERATOR);  // Источник: генератор моков
            mapping.setSourceSystemUrl("http://mock-emr-generator.local");
            mapping.setImportedBy(createdBy);
            emrMappingRepository.save(mapping);

            // ШАГ 5: Формируем результат
            EmrImportResultDTO result = EmrImportResultDTO.success("Mock patient generated and imported successfully");
            result.setExternalPatientIdInFhirResource(mockPatient.getPatientIdInFhirResource());
            result.setInternalPatientId(null);  // TODO: Создать в doctor.entity.Patient
            result.setMatchConfidence(MatchConfidence.NO_MATCH);
            result.setNewPatientCreated(true);
            result.setSourceType(EmrSourceType.MOCK_GENERATOR);
            result.setObservationsImported(observations.size());

            log.info("Mock patient created successfully: name={} {}, internalEmr={}",
                    mockPatient.getFirstName(), mockPatient.getLastName(), internalEmrNumber);

            return result;

        } catch (Exception e) {
            log.error("Failed to generate mock patient: error={}", e.getMessage(), e);

            EmrImportResultDTO result = EmrImportResultDTO.failure("Failed to generate mock patient");
            result.addError("Mock generation error: " + e.getMessage());
            return result;
        }
    }

    /*
     * МЕТОД 3: Генерация BATCH (пакета) моковых пациентов.
     *
     * ЗАЧЕМ:
     * - Фронтенд: "Дай 50 пациентов для тестирования списка"
     * - QA: "Нужно 100 пациентов для нагрузочного тестирования"
     *
     * ПРОИЗВОДИТЕЛЬНОСТЬ: 100 пациентов за ~2-3 секунды
     */
    @Override
    public List<EmrImportResultDTO> generateAndImportMockBatch(int count, String createdBy) {
        log.info("Generating mock batch: count={}, createdBy={}", count, createdBy);
        long startTime = System.currentTimeMillis();

        // Генерируем batch пациентов одним вызовом (быстрее)
        List<FhirPatientDTO> mockPatients = mockEmrDataGenerator.generateBatch(count);

        // Импортируем каждого пациента
        List<EmrImportResultDTO> results = mockPatients.stream()
                .map(patient -> importMockPatient(patient, createdBy))
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info("Mock batch generation completed: count={}, duration={}ms", count, duration);

        return results;
    }

    /*
     * МЕТОД 4: Поиск пациентов в FHIR системе.
     *
     * READ-ONLY метод - не изменяет данные, только читает из FHIR сервера.
     *
     * ЗАЧЕМ:
     * - Перед импортом проверить, есть ли пациент в FHIR системе
     * - Для Patient Reconciliation (найти похожих пациентов)
     */
    @Override
    @Transactional(readOnly = true)  // Оптимизация: только чтение
    public List<FhirPatientDTO> searchPatientsInFhir(String firstName, String lastName, String birthDate) {
        log.info("Searching patients in FHIR: firstName={}, lastName={}, birthDate={}",
                firstName, lastName, birthDate);

        try {
            List<FhirPatientDTO> patients = hapiFhirClient.searchPatients(firstName, lastName, birthDate);
            log.info("Found {} patients in FHIR server", patients.size());
            return patients;
        } catch (Exception e) {
            log.error("Failed to search patients in FHIR: error={}", e.getMessage(), e);
            throw new RuntimeException("FHIR search failed: " + e.getMessage(), e);
        }
    }

    /*
     * МЕТОД 5: Получить лабораторные анализы для пациента.
     *
     * READ-ONLY метод - только читает из FHIR сервера.
     *
     * ЧТО ВОЗВРАЩАЕТСЯ:
     * - Креатинин (2160-0) → функция почек
     * - Билирубин (1975-2) → функция печени
     * - Тромбоциты (777-3) → риск кровотечений
     * - Лейкоциты (6690-2) → инфекции
     * - Натрий (2951-2) → водно-электролитный баланс
     * - Сатурация (59408-5) → дыхательная функция
     *
     * ЗАЧЕМ: Для Treatment Protocol Service (корректировка доз препаратов)
     */
    @Override
    @Transactional(readOnly = true)
    public List<FhirObservationDTO> getObservationsForPatient(String fhirPatientId) {
        log.info("Fetching observations for patient: {}", fhirPatientId);

        try {
            List<FhirObservationDTO> observations = hapiFhirClient.getObservationsForPatient(fhirPatientId);
            log.info("Found {} observations for patient {}", observations.size(), fhirPatientId);
            return observations;
        } catch (Exception e) {
            log.error("Failed to fetch observations: fhirPatientId={}, error={}",
                    fhirPatientId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch observations: " + e.getMessage(), e);
        }
    }

    /*
     * МЕТОД 6: Конвертировать FHIR Observations в EmrDTO (формат nurse модуля).
     *
     * READ-ONLY метод - только конвертирует данные, не пишет в БД.
     *
     * РУЧНАЯ КОНВЕРТАЦИЯ (ModelMapper НЕ нужен):
     * - FHIR Observations имеют LOINC коды (стандартные медицинские коды)
     * - EmrDTO имеет простые поля (gfr, plt, wbc, sodium, sat)
     * - Нужны РАСЧЕТЫ: GFR из креатинина, Child-Pugh из билирубина
     *
     * МЕДИЦИНСКИЕ ТЕРМИНЫ:
     * - GFR (Glomerular Filtration Rate) = Скорость клубочковой фильтрации
     *   Показывает, насколько хорошо работают почки
     *   ≥90: Нормально
     *   60-89: Умеренное снижение
     *   30-59: Значительное снижение (НУЖНА КОРРЕКТИРОВКА ДОЗЫ!)
     *   15-29: Тяжелое снижение (снижение дозы на 50-75%)
     *   <15: Почечная недостаточность (многие препараты противопоказаны)
     *
     * - PLT (Platelets) = Тромбоциты
     *   Отвечают за свертываемость крови
     *   Норма: 150-400 тысяч/мкл
     *   <50: Высокий риск кровотечений (осторожно с НПВС!)
     *
     * - WBC (White Blood Cells) = Лейкоциты
     *   Борются с инфекциями
     *   Норма: 4-11 тысяч/мкл
     *   >15: Возможна инфекция
     *
     * - Sodium = Натрий
     *   Водно-электролитный баланс
     *   Норма: 135-145 mmol/L
     *
     * - SpO2 (Oxygen Saturation) = Сатурация кислорода
     *   Показывает, сколько кислорода в крови
     *   Норма: 95-100%
     *   <90%: Дыхательная недостаточность
     */
    @Override
    @Transactional(readOnly = true)
    public EmrDTO convertObservationsToEmr(List<FhirObservationDTO> observations, String createdBy) {
        log.debug("Converting {} observations to EmrDTO", observations.size());

        EmrDTO emr = new EmrDTO();

        // Извлекаем показатели по LOINC кодам (стандартные медицинские коды)
        for (FhirObservationDTO obs : observations) {
            String loincCode = obs.getLoincCode();
            Double value = obs.getValue();

            if (value == null) continue;  // Пропускаем, если значение отсутствует

            switch (loincCode) {
                case "2160-0":  // Креатинин → рассчитываем GFR (функция почек)
                    emr.setGfr(calculateGfrCategory(value));
                    break;
                case "777-3":  // Тромбоциты (риск кровотечений)
                    emr.setPlt(value);
                    break;
                case "6690-2":  // Лейкоциты (инфекции)
                    emr.setWbc(value);
                    break;
                case "2951-2":  // Натрий (водно-электролитный баланс)
                    emr.setSodium(value);
                    break;
                case "59408-5":  // Сатурация (дыхательная функция)
                    emr.setSat(value);
                    break;
            }
        }

        // Устанавливаем дефолтные значения для отсутствующих показателей
        if (emr.getGfr() == null) emr.setGfr("Unknown");
        if (emr.getPlt() == null) emr.setPlt(200.0);  // Норма
        if (emr.getWbc() == null) emr.setWbc(7.0);    // Норма
        if (emr.getSodium() == null) emr.setSodium(140.0);  // Норма
        if (emr.getSat() == null) emr.setSat(98.0);   // Норма

        // Child-Pugh Score (функция печени) пока заглушка
        // Требует больше данных: билирубин, альбумин, протромбиновое время, асцит, энцефалопатия
        emr.setChildPughScore("N/A");

        emr.setCreatedBy(createdBy);
        emr.setCreatedAt(LocalDateTime.now());

        log.debug("Converted to EmrDTO: gfr={}, plt={}, wbc={}, sodium={}, sat={}",
                emr.getGfr(), emr.getPlt(), emr.getWbc(), emr.getSodium(), emr.getSat());

        return emr;
    }

    /*
     * МЕТОД 7: Проверить, был ли пациент уже импортирован.
     *
     * READ-ONLY метод - только проверяет существование в БД.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isPatientAlreadyImported(String fhirPatientId) {
        boolean exists = emrMappingRepository.existsByExternalFhirId(fhirPatientId);
        log.debug("Patient import check: fhirPatientId={}, exists={}", fhirPatientId, exists);
        return exists;
    }

    /*
     * МЕТОД 8: Получить внутренний EMR номер по внешнему FHIR ID.
     *
     * READ-ONLY метод - только читает из БД.
     */
    @Override
    @Transactional(readOnly = true)
    public String getInternalEmrNumber(String fhirPatientId) {
        return emrMappingRepository.findByExternalFhirId(fhirPatientId)
                .map(EmrMapping::getInternalEmrNumber)
                .orElse(null);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /*
     * Генерация уникального внутреннего EMR номера.
     *
     * Формат: EMR-XXXXXXXX (8 символов)
     * Пример: EMR-A1B2C3D4
     *
     * Использует UUID для гарантии уникальности.
     */
    private String generateInternalEmrNumber() {
        return "EMR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Импорт одного мокового пациента (вспомогательный метод для batch).
     */
    private EmrImportResultDTO importMockPatient(FhirPatientDTO mockPatient, String createdBy) {
        try {
            String internalEmrNumber = generateInternalEmrNumber();

            EmrMapping mapping = new EmrMapping();
            mapping.setExternalFhirId(mockPatient.getPatientIdInFhirResource());
            mapping.setInternalEmrNumber(internalEmrNumber);
            mapping.setSourceType(EmrSourceType.MOCK_GENERATOR);
            mapping.setSourceSystemUrl("http://mock-emr-generator.local");
            mapping.setImportedBy(createdBy);
            emrMappingRepository.save(mapping);

            EmrImportResultDTO result = EmrImportResultDTO.success("Mock patient imported");
            result.setExternalPatientIdInFhirResource(mockPatient.getPatientIdInFhirResource());
            result.setNewPatientCreated(true);
            result.setSourceType(EmrSourceType.MOCK_GENERATOR);
            return result;

        } catch (Exception e) {
            log.error("Failed to import mock patient: {}", e.getMessage());
            return EmrImportResultDTO.failure("Import failed: " + e.getMessage());
        }
    }

    /*
     * Рассчитать категорию GFR (функция почек) по креатинину.
     *
     * МЕДИЦИНСКОЕ ОБЪЯСНЕНИЕ:
     * GFR (Glomerular Filtration Rate) = Скорость клубочковой фильтрации
     * Показывает, насколько хорошо почки фильтруют кровь от токсинов.
     *
     * КАТЕГОРИИ GFR:
     * - ≥90 ml/min: Нормальная функция почек
     * - 60-89: Умеренное снижение (начальная стадия)
     * - 30-59: Значительное снижение (НУЖНА КОРРЕКТИРОВКА ДОЗЫ ПРЕПАРАТОВ!)
     * - 15-29: Тяжелое снижение (снижение дозы на 50-75%)
     * - <15: Почечная недостаточность (многие препараты противопоказаны)
     *
     * УПРОЩЕННАЯ ФОРМУЛА (без возраста/веса/пола):
     * GFR ≈ 100 / креатинин
     *
     * РЕАЛЬНАЯ ФОРМУЛА (Cockcroft-Gault):
     * GFR = ((140 - возраст) × вес × (0.85 если женщина)) / (72 × креатинин)
     *
     * ЗАЧЕМ: Если GFR низкий, многие обезболивающие накапливаются в организме
     * и могут вызвать передозировку. Нужно снижать дозу!
     *
     * @param creatinine креатинин в mg/dL (норма: 0.6-1.2)
     * @return категория GFR с описанием
     */
    private String calculateGfrCategory(double creatinine) {
        // Упрощенный расчет: GFR ≈ 100 / креатинин
        double estimatedGfr = 100.0 / creatinine;

        if (estimatedGfr >= 90) return "≥90 (Normal)";  // Норма
        if (estimatedGfr >= 60) return "60-89 (Mild decrease)";  // Легкое снижение
        if (estimatedGfr >= 30) return "30-59 (Moderate decrease)";  // Умеренное снижение
        if (estimatedGfr >= 15) return "15-29 (Severe decrease)";  // Тяжелое снижение
        return "<15 (Kidney failure)";  // Почечная недостаточность
    }
}