package pain_helper_back.external_emr_integration_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pain_helper_back.common.patients.dto.EmrDTO;
import pain_helper_back.common.patients.entity.Diagnosis;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.repository.DiagnosisRepository;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.external_emr_integration_service.client.HapiFhirClient;
import pain_helper_back.external_emr_integration_service.dto.EmrImportResultDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirObservationDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirPatientDTO;
import pain_helper_back.external_emr_integration_service.entity.EmrMapping;
import pain_helper_back.external_emr_integration_service.repository.EmrMappingRepository;
import pain_helper_back.enums.EmrSourceType;
import pain_helper_back.enums.MatchConfidence;
import pain_helper_back.enums.PatientsGenders;

import java.time.LocalDateTime;
import java.util.*;
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
 * 6. СОЗДАЕТ ЗАПИСИ В ОБЩЕЙ ТАБЛИЦЕ common.patients (Patient и Emr)
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

    // Репозитории для работы с общей логикой пациентов (common/patients)
    private final PatientRepository patientRepository;
    private final EmrRepository emrRepository;
    private final DiagnosisRepository diagnosisRepository;

    /*
     * МЕТОД 1: Импорт пациента из FHIR системы другой больницы.
     *
     * ЧТО ПРОИСХОДИТ:
     * 1. Проверяем, не импортировали ли мы этого пациента раньше (по FHIR ID)
     * 2. Если уже импортирован - возвращаем существующий EMR номер
     * 3. Если новый - получаем данные из FHIR сервера
     * 4. Получаем лабораторные анализы (креатинин, тромбоциты и т.д.)
     * 5. Присваиваем внутренний EMR номер (EMR-XXXXXXXX)
     * 6. Сохраняем связь в БД (EmrMapping)
     * 7. СОЗДАЕМ ПАЦИЕНТА в общей таблице (common.patients.entity.Patient)
     * 8. СОЗДАЕМ МЕДИЦИНСКУЮ КАРТУ (common.patients.entity.Emr) с лабораторными данными
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
                // Пациент был импортирован ранее - проверяем, существует ли он в системе
                String internalEmrNumber = existingMapping.get().getInternalEmrNumber();
                Optional<Patient> existingPatient = patientRepository.findByMrn(internalEmrNumber);
                
                if (existingPatient.isPresent() && existingPatient.get().getIsActive()) {
                    // Пациент существует И активен - блокируем повторный импорт
                    log.warn("Patient already exists and is active: fhirPatientId={}, mrn={}",
                            fhirPatientId, internalEmrNumber);
                    
                    EmrImportResultDTO result = EmrImportResultDTO.success("Patient already exists in system");
                    result.setExternalPatientIdInFhirResource(fhirPatientId);
                    result.setInternalPatientId(existingPatient.get().getId());
                    result.setMatchConfidence(MatchConfidence.EXACT);  // 100% совпадение
                    result.setNewPatientCreated(false);  // Новый пациент НЕ создан
                    result.setSourceType(existingMapping.get().getSourceType());
                    result.addWarning("Patient already exists with MRN: " + internalEmrNumber);
                    return result;
                }
                
                // Пациент был удален или не существует - НЕ удаляем маппинг, просто пропускаем проверку
                log.info("Patient was deleted or not found, allowing re-import: fhirPatientId={}, mrn={}",
                        fhirPatientId, internalEmrNumber);
                log.info("Existing mapping will be reused or updated");
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

            // ШАГ 3.5: Получаем диагнозы (Conditions) из FHIR
            // ВАЖНО: Диагнозы нужны для выбора правильного протокола лечения и аналитики
            log.debug("Fetching conditions (diagnoses) for patient: {}", fhirPatientId);
            List<IcdCodeLoaderService.IcdCode> diagnoses = hapiFhirClient.getConditionsForPatient(fhirPatientId);

            // ШАГ 4: Присваиваем внутренний EMR номер
            // Формат: EMR-A1B2C3D4 (уникальный для нашей системы)
            String internalEmrNumber;
            
            // ШАГ 5: Сохраняем связь между внешним FHIR ID и внутренним EMR номером
            // ВАЖНО: Если маппинг уже существует (пациент был удален) - используем его
            if (existingMapping.isPresent()) {
                internalEmrNumber = existingMapping.get().getInternalEmrNumber();
                log.info("Reusing existing EMR number: {} for FHIR patient: {}", internalEmrNumber, fhirPatientId);
            } else {
                internalEmrNumber = generateInternalEmrNumber();
                log.info("Assigned new internal EMR number: {} for FHIR patient: {}", internalEmrNumber, fhirPatientId);
                
                EmrMapping mapping = new EmrMapping();
                mapping.setExternalFhirId(fhirPatientId);  // ID в FHIR системе другой больницы
                mapping.setInternalEmrNumber(internalEmrNumber);  // Наш внутренний номер
                mapping.setSourceType(EmrSourceType.FHIR_SERVER);  // Источник: FHIR сервер
                mapping.setSourceSystemUrl(fhirPatient.getSourceSystemUrl());  // URL больницы
                mapping.setImportedBy(importedBy);  // Кто импортировал (для аудита)
                emrMappingRepository.save(mapping);
                
                log.info("Successfully saved EMR mapping: externalId={}, internalEmr={}",
                        fhirPatientId, internalEmrNumber);
            }

            // ШАГ 6-7: Создаем Patient и Emr в общей таблице (ОБЩИЙ МЕТОД)
            // ВАЖНО: Используем общий метод для устранения дублирования кода
            Long patientId = createPatientAndEmrFromFhir(fhirPatient, observations, diagnoses, null, internalEmrNumber, importedBy);

            // ШАГ 8: Формируем результат импорта
            EmrImportResultDTO result = EmrImportResultDTO.success("Patient imported successfully from FHIR server");
            result.setExternalPatientIdInFhirResource(fhirPatientId);
            result.setInternalPatientId(patientId);  // ID пациента в общей таблице
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
     * 5. СОЗДАЕТСЯ Patient в common.patients.entity.Patient
     * 6. СОЗДАЕТСЯ Emr в common.patients.entity.Emr
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
            // МЕДИЦИНСКИЕ ПОКАЗАТЕЛИ (ОБНОВЛЕНО: реалистичные значения из Clinical_Norms_and_Units.csv):
            // - GFR (функция почек): A(≥90), B(60-89), C(45-59), D(30-44), E(15-29), F(<15)
            // - Тромбоциты (PLT): 150-450 (норма), возможный диапазон 0-1000
            // - Лейкоциты (WBC): 4.0-10.0 (норма), возможный диапазон 2-40
            // - Натрий: 135-145 (норма), возможный диапазон 120-160
            // - Сатурация (SpO2): 95-100% (норма), возможный диапазон 85-100%
            // - Вес: >50 кг
            List<FhirObservationDTO> observations = mockEmrDataGenerator
                    .generateObservationForPatient(mockPatient.getPatientIdInFhirResource());
            
            // Генерируем диагнозы для мокового пациента из ICD кодов
            List<IcdCodeLoaderService.IcdCode> diagnoses = mockEmrDataGenerator.generateDiagnosesForPatient();
            
            // Генерируем аллергии (sensitivities) для мокового пациента
            List<String> sensitivities = mockEmrDataGenerator.generateSensitivitiesForPatient();

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

            // ШАГ 5-6: Создаем Patient и Emr в общей таблице (ОБЩИЙ МЕТОД)
            // ВАЖНО: Используем общий метод для устранения дублирования кода
            Long patientId = createPatientAndEmrFromFhir(mockPatient, observations, diagnoses, sensitivities, internalEmrNumber, createdBy);

            // ШАГ 7: Формируем результат
            EmrImportResultDTO result = EmrImportResultDTO.success("Mock patient generated and imported successfully");
            result.setExternalPatientIdInFhirResource(mockPatient.getPatientIdInFhirResource());
            result.setInternalPatientId(patientId);  // ID пациента в общей таблице
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
     * ВАЖНО: Проверяем не только маппинг, но и существование активного пациента!
     * - Если пациент был удален (isActive = false) → возвращаем false (можно импортировать заново)
     * - Если пациент существует и активен → возвращаем true (блокируем повторный импорт)
     *
     * READ-ONLY метод - только проверяет существование в БД.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isPatientAlreadyImported(String fhirPatientId) {
        Optional<EmrMapping> mapping = emrMappingRepository.findByExternalFhirId(fhirPatientId);
        
        if (mapping.isEmpty()) {
            log.debug("Patient import check: fhirPatientId={}, result=NOT_IMPORTED", fhirPatientId);
            return false;
        }
        
        // Проверяем, существует ли пациент в системе и активен ли он
        String internalEmrNumber = mapping.get().getInternalEmrNumber();
        Optional<Patient> patient = patientRepository.findByMrn(internalEmrNumber);
        
        boolean isActivePatient = patient.isPresent() && patient.get().getIsActive();
        log.debug("Patient import check: fhirPatientId={}, mrn={}, exists={}, active={}, result={}",
                fhirPatientId, internalEmrNumber, patient.isPresent(), 
                patient.map(Patient::getIsActive).orElse(false), isActivePatient);
        
        return isActivePatient;
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

    /*
     * Импорт одного мокового пациента (вспомогательный метод для batch).
     *
     * ПОЛНЫЙ ЦИКЛ с использованием общего метода createPatientAndEmrFromFhir().
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

            // Генерируем лабораторные данные
            List<FhirObservationDTO> observations = mockEmrDataGenerator
                    .generateObservationForPatient(mockPatient.getPatientIdInFhirResource());

            
            // Генерируем диагнозы
            List<IcdCodeLoaderService.IcdCode> diagnoses = mockEmrDataGenerator.generateDiagnosesForPatient();
            
            // Генерируем аллергии
            List<String> sensitivities = mockEmrDataGenerator.generateSensitivitiesForPatient();

            // Создаем Patient и Emr в общей таблице (ОБЩИЙ МЕТОД)
            Long patientId = createPatientAndEmrFromFhir(mockPatient, observations, diagnoses, sensitivities, internalEmrNumber, createdBy);

            EmrImportResultDTO result = EmrImportResultDTO.success("Mock patient imported");
            result.setExternalPatientIdInFhirResource(mockPatient.getPatientIdInFhirResource());
            result.setInternalPatientId(patientId);
            result.setNewPatientCreated(true);
            result.setSourceType(EmrSourceType.MOCK_GENERATOR);
            result.setObservationsImported(observations.size());
            return result;

        } catch (Exception e) {
            log.error("Failed to import mock patient: {}", e.getMessage());
            return EmrImportResultDTO.failure("Import failed: " + e.getMessage());
        }
    }

    /*
     * ОБЩИЙ МЕТОД: Создать Patient и Emr в общей таблице из FHIR данных.
     *
     * ИСПОЛЬЗУЕТСЯ В:
     * - importPatientFromFhir() - импорт из реального FHIR сервера
     * - generateAndImportMockPatient() - генерация моковых пациентов
     * - importMockPatient() - batch импорт моковых пациентов
     *
     * УСТРАНЯЕТ ДУБЛИРОВАНИЕ КОДА.
     *
     * ЧТО ДЕЛАЕТ:
     * 1. Создает запись в common.patients.entity.Patient
     * 2. Создает запись в common.patients.entity.Emr с лабораторными данными
     * 3. Связывает Emr с Patient через foreign key
     * 4. Возвращает ID созданного пациента
     *
     * @param fhirPatient данные пациента из FHIR
     * @param observations лабораторные показатели из FHIR
     * @param diagnoses список диагнозов (ICD коды)
     * @param internalEmrNumber внутренний EMR номер (используется как MRN)
     * @param createdBy кто создал запись
     * @return ID созданного пациента в таблице Patient
     */
    private Long createPatientAndEmrFromFhir(
            FhirPatientDTO fhirPatient,
            List<FhirObservationDTO> observations,
            List<IcdCodeLoaderService.IcdCode> diagnoses,
            List<String> sensitivities,
            String internalEmrNumber,
            String createdBy) {

        // Создаем пациента в общей таблице
        Patient patient = new Patient();
        patient.setMrn(internalEmrNumber);
        patient.setFirstName(fhirPatient.getFirstName());
        patient.setLastName(fhirPatient.getLastName());
        patient.setDateOfBirth(fhirPatient.getDateOfBirth());
        patient.setGender(convertGender(fhirPatient.getGender()));
        patient.setPhoneNumber(fhirPatient.getPhoneNumber());
        patient.setEmail(fhirPatient.getEmail());
        patient.setAddress(fhirPatient.getAddress());
        
        // Извлекаем страховой полис из identifiers
        if (fhirPatient.getIdentifiers() != null) {
            fhirPatient.getIdentifiers().stream()
                    .filter(id -> "INS".equals(id.getType()))
                    .findFirst()
                    .ifPresent(insurance -> patient.setInsurancePolicyNumber(insurance.getValue()));
        }
        
        patient.setIsActive(true);
        patient.setCreatedBy(createdBy);
        Patient savedPatient = patientRepository.save(patient);

        log.info("Created Patient: mrn={}, name={} {}",
                internalEmrNumber, patient.getFirstName(), patient.getLastName());

        // Создаем медицинскую карту с лабораторными показателями
        Emr emr = new Emr();
        emr.setPatient(savedPatient);
        emr.setCreatedBy(createdBy);

        // Извлекаем показатели из FHIR Observations
        for (FhirObservationDTO obs : observations) {
            String loincCode = obs.getLoincCode();
            Double value = obs.getValue();
            if (value == null) continue;

            switch (loincCode) {
                case "2160-0": emr.setGfr(calculateGfrCategory(value)); break;  // Креатинин → GFR
                case "777-3": emr.setPlt(value); break;  // Тромбоциты
                case "6690-2": emr.setWbc(value); break;  // Лейкоциты
                case "2951-2": emr.setSodium(value); break;  // Натрий
                case "59408-5": emr.setSat(value); break;  // Сатурация
                case "8302-2": emr.setHeight(value); break;  // Рост
                case "29463-7": emr.setWeight(value); break;  // Вес
                case "1975-2": emr.setChildPughScore(calculateChildPughFromBilirubin(value)); break;  // Билирубин → Child-Pugh
            }
        }

        // Дефолтные значения
        if (emr.getGfr() == null) emr.setGfr("Unknown");
        if (emr.getPlt() == null) emr.setPlt(200.0);
        if (emr.getWbc() == null) emr.setWbc(7.0);
        if (emr.getSodium() == null) emr.setSodium(140.0);
        if (emr.getSat() == null) emr.setSat(98.0);
        if (emr.getChildPughScore() == null) emr.setChildPughScore("A");  // Дефолт: нормальная печень

        // Устанавливаем sensitivities (аллергии)
        if (sensitivities != null && !sensitivities.isEmpty()) {
            emr.setSensitivities(sensitivities);
            log.info("Set {} sensitivities for patient: {}", sensitivities.size(), 
                     String.join(", ", sensitivities));
        }

        Emr savedEmr = emrRepository.save(emr);

        log.info("Created Emr: gfr={}, plt={}, wbc={}",
                emr.getGfr(), emr.getPlt(), emr.getWbc());

        // Создаем и сохраняем диагнозы
        if (diagnoses != null && !diagnoses.isEmpty()) {
            Set<Diagnosis> diagnosisEntities = new HashSet<>();
            for (IcdCodeLoaderService.IcdCode icdCode : diagnoses) {
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setEmr(savedEmr);
                diagnosis.setIcdCode(icdCode.getCode());
                diagnosis.setDescription(icdCode.getDescription());
                diagnosisEntities.add(diagnosis);
            }
            diagnosisRepository.saveAll(diagnosisEntities);
            log.info("Created {} diagnoses for patient: {}", diagnosisEntities.size(),
                    diagnosisEntities.stream()
                            .map(d -> d.getIcdCode() + " - " + d.getDescription())
                            .collect(Collectors.joining(", ")));
        }

        return savedPatient.getId();
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

        // Ограничиваем значение диапазоном 0-120
        if (estimatedGfr > 120) estimatedGfr = 120;
        if (estimatedGfr < 0) estimatedGfr = 0;

        // РАНДОМНО выбираем: либо букву (A/B/C/D), либо точное число (0-120)
        boolean useLetter = Math.random() < 0.5;  // 50% вероятность буквы
        
        if (useLetter) {
            // Возвращаем букву в зависимости от диапазона
            if (estimatedGfr >= 90) return "A";  // Норма (≥90)
            if (estimatedGfr >= 60) return "B";  // Легкое снижение (60-89)
            if (estimatedGfr >= 30) return "C";  // Умеренное снижение (30-59)
            return "D";  // Тяжелое снижение (<30)
        } else {
            // Возвращаем точное число 0-120
            return String.valueOf((int) Math.round(estimatedGfr));
        }
    }

    /*
     * Рассчитать упрощенный Child-Pugh Score по билирубину.
     *
     * Child-Pugh Score = Оценка функции печени (5-15 баллов)
     * Используется для корректировки дозы препаратов при печеночной недостаточности.
     *
     * ПОЛНЫЙ Child-Pugh требует 5 параметров:
     * 1. Билирубин (общий)
     * 2. Альбумин (белок крови)
     * 3. Протромбиновое время (свертываемость)
     * 4. Асцит (жидкость в животе) - клиническая оценка
     * 5. Энцефалопатия (нарушение сознания) - клиническая оценка
     *
     * УПРОЩЕННЫЙ РАСЧЕТ (только по билирубину):
     * - Билирубин < 2.0 mg/dL → Class A (5-6 баллов) = Нормальная печень
     * - Билирубин 2.0-3.0 mg/dL → Class B (7-9 баллов) = Умеренная дисфункция (снижение дозы на 25-50%)
     * - Билирубин > 3.0 mg/dL → Class C (10-15 баллов) = Тяжелая дисфункция (многие препараты противопоказаны)
     *
     * ЗАЧЕМ: Печень метаболизирует большинство обезболивающих.
     * При печеночной недостаточности препараты накапливаются → передозировка!
     *
     * ПРИМЕЧАНИЕ: Это упрощенная версия для моковых данных.
     * В реальной системе нужны все 5 параметров для точного расчета.
     *
     * @param bilirubin билирубин в mg/dL (норма: 0.3-1.2)
     * @return Child-Pugh класс (A, B, C)
     */
    private String calculateChildPughFromBilirubin(double bilirubin) {
        // Упрощенный расчет только по билирубину
        if (bilirubin < 2.0) {
            return "A";  // Нормальная печень (5-6 баллов)
        } else if (bilirubin < 3.0) {
            return "B";  // Умеренная дисфункция (7-9 баллов)
        } else {
            return "C";  // Тяжелая дисфункция (10-15 баллов)
        }
    }

    /*
     * Конвертирует gender из String в PatientsGenders enum.
     *
     * ЗАЧЕМ: В FHIR gender приходит как String ("male", "female"),
     * а в нашей БД хранится как enum PatientsGenders.
     *
     * @param gender строка из FHIR ("male", "female", "other", "unknown")
     * @return PatientsGenders enum (MALE, FEMALE) или null если не распознано
     */
    private PatientsGenders convertGender(String gender) {
        if (gender == null) return null;

        String genderUpper = gender.toUpperCase();
        if (genderUpper.contains("MALE") && !genderUpper.contains("FEMALE")) {
            return PatientsGenders.MALE;
        } else if (genderUpper.contains("FEMALE")) {
            return PatientsGenders.FEMALE;
        }
        return null;  // Unknown gender
    }
}
