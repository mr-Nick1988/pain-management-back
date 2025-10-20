# 📘 ПОЛНАЯ ДОКУМЕНТАЦИЯ: EMR Integration Module

## Дата создания: 02.10.2025
## Автор: Backend Team (Pain Management System)

---

# 📑 СОДЕРЖАНИЕ

1. [Обзор модуля](#обзор-модуля)
2. [Архитектура системы](#архитектура-системы)
3. [Структура файлов](#структура-файлов)
4. [Взаимодействие с common/patients](#взаимодействие-с-commonpatients)
5. [Детальный Workflow](#детальный-workflow)
6. [Компоненты модуля](#компоненты-модуля)
7. [REST API Endpoints](#rest-api-endpoints)
8. [Примеры использования](#примеры-использования)
9. [Технические решения](#технические-решения)
10. [Диаграммы](#диаграммы)

---

# 🎯 ОБЗОР МОДУЛЯ

## Что такое EMR Integration?

**EMR Integration** — это модуль для интеграции с внешними системами электронных медицинских карт (EMR - Electronic Medical Records) через стандарт **FHIR** (Fast Healthcare Interoperability Resources).

## Зачем нужен этот модуль?

### Проблема:
Пациент приходит в нашу больницу из другой больницы. У него уже есть медицинская история (анализы, диагнозы, лекарства), но она хранится в **другой системе**.

### Решение:
EMR Integration модуль **автоматически импортирует** данные пациента из внешней системы через FHIR API и создает записи в нашей базе данных.

## Основные функции:

1. ✅ **Импорт пациентов из FHIR серверов** других больниц
2. ✅ **Генерация моковых (тестовых) пациентов** для разработки и тестирования
3. ✅ **Присвоение внутренних EMR номеров** (уникальные идентификаторы в нашей системе)
4. ✅ **Конвертация медицинских данных** из FHIR формата в наш формат
5. ✅ **Избежание дубликатов** (Patient Reconciliation)
6. ✅ **Создание записей в общей таблице** `common.patients` (Patient и Emr)

---

# 🏗️ АРХИТЕКТУРА СИСТЕМЫ

## Общая схема

```
┌─────────────────────────────────────────────────────────────────┐
│                    FRONTEND (React/Vue/Angular)                  │
│  - Медсестра вводит FHIR ID пациента из другой больницы         │
│  - Фронтенд-разработчик генерирует 50 моковых пациентов         │
└─────────────────────────────────────────────────────────────────┘
                              ↓ HTTP REST API
┌─────────────────────────────────────────────────────────────────┐
│              EmrIntegrationController (REST Layer)               │
│  POST /api/emr/import/{fhirPatientId}                           │
│  POST /api/emr/mock/generate                                    │
│  POST /api/emr/mock/generate-batch                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│         EmrIntegrationServiceImpl (Business Logic Layer)         │
│                                                                  │
│  Зависимости:                                                   │
│  1. HapiFhirClient → получение данных из FHIR сервера          │
│  2. MockEmrDataGenerator → генерация тестовых данных           │
│  3. EmrMappingRepository → связь FHIR ID ↔ EMR номер           │
│  4. PatientRepository → создание Patient (common/patients)      │
│  5. EmrRepository → создание Emr (common/patients)              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DATABASE (H2 / PostgreSQL)                    │
│                                                                  │
│  emr_integration:                                               │
│  └── emr_mappings (связь FHIR ID ↔ EMR номер)                 │
│                                                                  │
│  common/patients: ✅ ОБЩАЯ ЛОГИКА ДЛЯ ВСЕХ МОДУЛЕЙ             │
│  ├── nurse_patients (Patient) ← СОЗДАЕТСЯ ИЗ EMR INTEGRATION   │
│  ├── emr (Emr) ← СОЗДАЕТСЯ ИЗ EMR INTEGRATION                  │
│  ├── vas (VAS шкала боли)                                      │
│  └── recommendations (рекомендации врачей)                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              ДОСТУПНОСТЬ ДЛЯ ВСЕХ МОДУЛЕЙ                       │
│                                                                  │
│  ✅ Doctor модуль → PatientRepository.findByMrn()              │
│  ✅ Nurse модуль → EmrRepository.findByPatientMrn()            │
│  ✅ Anesthesiologist → TreatmentProtocolService                │
└─────────────────────────────────────────────────────────────────┘
```

---

# 📁 СТРУКТУРА ФАЙЛОВ

## 1. common/patients/ (Общая логика для всех модулей)

```
common/patients/
├── entity/
│   ├── Patient.java          ✅ Единая таблица пациентов
│   │   @Entity
│   │   @Table(name = "nurse_patients")
│   │   Поля:
│   │   - id (Long, PK, auto-increment)
│   │   - mrn (String, UNIQUE) ← EMR номер используется как MRN
│   │   - firstName (String)
│   │   - lastName (String)
│   │   - dateOfBirth (LocalDate)
│   │   - gender (PatientsGenders enum: MALE, FEMALE)
│   │   - phoneNumber (String)
│   │   - email (String)
│   │   - address (String)
│   │   - insurancePolicyNumber (String)
│   │   - isActive (Boolean)
│   │   - createdAt, updatedAt (LocalDateTime)
│   │   - createdBy, updatedBy (String)
│   │   Связи:
│   │   - @OneToMany → List<Emr> emr
│   │   - @OneToMany → List<Vas> vas
│   │   - @OneToMany → List<Recommendation> recommendations
│   │
│   ├── Emr.java              ✅ Медицинские карты (лабораторные данные)
│   │   @Entity
│   │   @Table(name = "emr")
│   │   Поля:
│   │   - id (Long, PK)
│   │   - gfr (String) ← Функция почек (GFR категория)
│   │   - childPughScore (String) ← Функция печени
│   │   - plt (Double) ← Тромбоциты (Platelets)
│   │   - wbc (Double) ← Лейкоциты (White Blood Cells)
│   │   - sat (Double) ← Сатурация кислорода (SpO2)
│   │   - sodium (Double) ← Натрий
│   │   - height, weight (Double)
│   │   - createdAt, updatedAt (LocalDateTime)
│   │   - createdBy, updatedBy (String)
│   │   Связи:
│   │   - @ManyToOne → Patient patient
│   │
│   ├── Vas.java              ✅ VAS шкала боли
│   ├── Recommendation.java   ✅ Рекомендации врачей
│   └── DrugRecommendation.java
│
├── repository/
│   ├── PatientRepository.java
│   │   extends JpaRepository<Patient, Long>
│   │   Методы:
│   │   - findByMrn(String mrn): Optional<Patient>
│   │   - existsByMrn(String mrn): boolean
│   │   - findByIsActive(Boolean isActive): List<Patient>
│   │   - findByEmail(String email): Optional<Patient>
│   │   - findByPhoneNumber(String phoneNumber): Optional<Patient>
│   │
│   └── EmrRepository.java
│       extends JpaRepository<Emr, Long>
│       Методы:
│       - findByPatientMrn(String mrn): List<Emr>
│       - findByPatientMrnOrderByCreatedAtDesc(String mrn): List<Emr>
│       - findByGfrLessThan(String threshold): List<Emr>
│       - findByChildPughScore(String score): List<Emr>
│
└── dto/
    ├── PatientDTO.java
    ├── EmrDTO.java
    ├── VasDTO.java
    └── RecommendationDTO.java
```

## 2. emr_integration/ (Импорт из FHIR)

```
emr_integration/
├── entity/
│   └── EmrMapping.java       ✅ Связь FHIR ID ↔ EMR номер
│       @Entity
│       @Table(name = "emr_mappings")
│       Поля:
│       - id (Long, PK)
│       - externalFhirId (String, UNIQUE) ← ID в FHIR системе ("Patient/12345")
│       - internalEmrNumber (String, UNIQUE) ← Наш EMR номер ("EMR-A1B2C3D4")
│       - sourceType (EmrSourceType enum: FHIR_SERVER, MOCK_GENERATOR)
│       - sourceSystemUrl (String) ← URL FHIR сервера
│       - importedAt (LocalDateTime)
│       - importedBy (String)
│
├── repository/
│   └── EmrMappingRepository.java
│       extends JpaRepository<EmrMapping, Long>
│       Методы:
│       - findByExternalFhirId(String fhirId): Optional<EmrMapping>
│       - existsByExternalFhirId(String fhirId): boolean
│       - findBySourceType(EmrSourceType type): List<EmrMapping>
│       - findBySourceSystemUrl(String url): List<EmrMapping>
│
├── service/
│   ├── EmrIntegrationService.java (interface)
│   │   Методы:
│   │   - importPatientFromFhir(String fhirPatientId, String importedBy)
│   │   - generateAndImportMockPatient(String createdBy)
│   │   - generateAndImportMockBatch(int count, String createdBy)
│   │   - searchPatientsInFhir(String firstName, String lastName, String birthDate)
│   │   - getObservationsForPatient(String fhirPatientId)
│   │   - convertObservationsToEmr(List<FhirObservationDTO> observations, String createdBy)
│   │   - isPatientAlreadyImported(String fhirPatientId)
│   │   - getInternalEmrNumber(String fhirPatientId)
│   │
│   ├── EmrIntegrationServiceImpl.java ✅ ОСНОВНАЯ ЛОГИКА
│   │   @Service
│   │   @RequiredArgsConstructor
│   │   @Transactional
│   │   
│   │   Зависимости:
│   │   - HapiFhirClient hapiFhirClient
│   │   - EmrMappingRepository emrMappingRepository
│   │   - MockEmrDataGenerator mockEmrDataGenerator
│   │   - PatientRepository patientRepository ← ИЗ common/patients
│   │   - EmrRepository emrRepository ← ИЗ common/patients
│   │   
│   │   Методы:
│   │   1. importPatientFromFhir() - импорт из реального FHIR сервера
│   │   2. generateAndImportMockPatient() - генерация 1 мокового пациента
│   │   3. generateAndImportMockBatch() - генерация N моковых пациентов
│   │   4. searchPatientsInFhir() - поиск в FHIR системе
│   │   5. getObservationsForPatient() - получение лабораторных данных
│   │   6. convertObservationsToEmr() - конвертация FHIR → EmrDTO
│   │   7. isPatientAlreadyImported() - проверка дубликата
│   │   8. getInternalEmrNumber() - получение EMR номера
│   │   
│   │   Вспомогательные методы:
│   │   - generateInternalEmrNumber() - генерация EMR-XXXXXXXX
│   │   - importMockPatient() - импорт 1 мокового пациента (для batch)
│   │   - createPatientAndEmrFromFhir() ← ОБЩИЙ МЕТОД (устраняет дублирование)
│   │   - calculateGfrCategory() - расчет GFR из креатинина
│   │   - convertGender() - конвертация String → PatientsGenders enum
│   │
│   └── MockEmrDataGenerator.java
│       Генерация тестовых данных с JavaFaker
│
├── controller/
│   └── EmrIntegrationController.java
│       @RestController
│       @RequestMapping("/api/emr")
│       @CrossOrigin(origins = "*")
│       
│       Endpoints:
│       - POST /api/emr/import/{fhirPatientId}
│       - POST /api/emr/mock/generate
│       - POST /api/emr/mock/generate-batch
│       - GET /api/emr/search
│       - GET /api/emr/observations/{fhirPatientId}
│       - POST /api/emr/convert-observations
│       - GET /api/emr/check-import/{fhirPatientId}
│
├── client/
│   └── HapiFhirClient.java   ✅ Клиент для FHIR сервера
│       Методы:
│       - getPatientById(String fhirPatientId): FhirPatientDTO
│       - getObservationsForPatient(String fhirPatientId): List<FhirObservationDTO>
│       - searchPatients(String firstName, String lastName, String birthDate): List<FhirPatientDTO>
│
├── dto/
│   ├── FhirPatientDTO.java
│   │   Поля из FHIR Patient resource:
│   │   - patientIdInFhirResource (String)
│   │   - firstName, lastName (String)
│   │   - dateOfBirth (LocalDate)
│   │   - gender (String)
│   │   - phoneNumber, email, address (String)
│   │   - identifiers (List<FhirIdentifierDTO>)
│   │   - sourceSystemUrl (String)
│   │
│   ├── FhirObservationDTO.java
│   │   Поля из FHIR Observation resource:
│   │   - observationId (String)
│   │   - loincCode (String) ← Стандартный медицинский код
│   │   - value (Double)
│   │   - unit (String)
│   │   - effectiveDateTime (LocalDateTime)
│   │   
│   │   LOINC коды:
│   │   - "2160-0" → Креатинин (для расчета GFR)
│   │   - "777-3" → Тромбоциты (PLT)
│   │   - "6690-2" → Лейкоциты (WBC)
│   │   - "2951-2" → Натрий
│   │   - "59408-5" → Сатурация (SpO2)
│   │
│   └── EmrImportResultDTO.java
│       Результат импорта:
│       - success (boolean)
│       - message (String)
│       - externalPatientIdInFhirResource (String)
│       - internalPatientId (Long) ← ID в common.patients.Patient
│       - matchConfidence (MatchConfidence enum)
│       - newPatientCreated (boolean)
│       - sourceType (EmrSourceType enum)
│       - observationsImported (int)
│       - warnings (List<String>)
│       - errors (List<String>)
│
└── FhirConfig.java
    Конфигурация FHIR клиента
```

---

# 🔗 ВЗАИМОДЕЙСТВИЕ С common/patients

## Ключевое решение архитектуры

**ПРОБЛЕМА:** Раньше каждый модуль (doctor, nurse, anesthesiologist) имел свою таблицу пациентов. Это приводило к дублированию данных и несогласованности.

**РЕШЕНИЕ:** Создана **общая таблица пациентов** `common.patients.entity.Patient`, которую используют **ВСЕ модули**.

## Как EMR Integration создает записи в common/patients

### Метод: `createPatientAndEmrFromFhir()`

Это **ОБЩИЙ МЕТОД**, который используется в:
1. `importPatientFromFhir()` - импорт из реального FHIR сервера
2. `generateAndImportMockPatient()` - генерация моковых пациентов
3. `importMockPatient()` - batch импорт моковых пациентов

### Что делает метод:

```java
private Long createPatientAndEmrFromFhir(
        FhirPatientDTO fhirPatient,
        List<FhirObservationDTO> observations,
        String internalEmrNumber,
        String createdBy) {
    
    // ШАГ 1: Создаем пациента в common.patients.entity.Patient
    Patient patient = new Patient();
    patient.setMrn(internalEmrNumber);  // EMR номер = MRN
    patient.setFirstName(fhirPatient.getFirstName());
    patient.setLastName(fhirPatient.getLastName());
    patient.setDateOfBirth(fhirPatient.getDateOfBirth());
    patient.setGender(convertGender(fhirPatient.getGender()));
    patient.setPhoneNumber(fhirPatient.getPhoneNumber());
    patient.setEmail(fhirPatient.getEmail());
    patient.setAddress(fhirPatient.getAddress());
    patient.setIsActive(true);
    patient.setCreatedBy(createdBy);
    Patient savedPatient = patientRepository.save(patient);
    
    // ШАГ 2: Создаем медицинскую карту в common.patients.entity.Emr
    Emr emr = new Emr();
    emr.setPatient(savedPatient);  // Связь с Patient
    emr.setCreatedBy(createdBy);
    
    // Извлекаем лабораторные показатели из FHIR Observations
    for (FhirObservationDTO obs : observations) {
        String loincCode = obs.getLoincCode();
        Double value = obs.getValue();
        if (value == null) continue;
        
        switch (loincCode) {
            case "2160-0": emr.setGfr(calculateGfrCategory(value)); break;
            case "777-3": emr.setPlt(value); break;
            case "6690-2": emr.setWbc(value); break;
            case "2951-2": emr.setSodium(value); break;
            case "59408-5": emr.setSat(value); break;
        }
    }
    
    // Дефолтные значения
    if (emr.getGfr() == null) emr.setGfr("Unknown");
    if (emr.getPlt() == null) emr.setPlt(200.0);
    if (emr.getWbc() == null) emr.setWbc(7.0);
    if (emr.getSodium() == null) emr.setSodium(140.0);
    if (emr.getSat() == null) emr.setSat(98.0);
    emr.setChildPughScore("N/A");
    
    emrRepository.save(emr);
    
    return savedPatient.getId();
}
```

### Результат:

После вызова этого метода в базе данных создаются **2 записи**:

1. **`nurse_patients` (Patient)**
   - id = 42
   - mrn = "EMR-A1B2C3D4"
   - firstName = "John"
   - lastName = "Smith"
   - dateOfBirth = 1980-01-15
   - gender = MALE
   - isActive = true

2. **`emr` (Emr)**
   - id = 123
   - patient_id = 42 (foreign key → Patient)
   - gfr = "≥90 (Normal)"
   - plt = 200.0
   - wbc = 7.0
   - sodium = 140.0
   - sat = 98.0

### Доступность для других модулей:

Теперь **ВСЕ модули** могут получить доступ к этому пациенту:

```java
// Doctor модуль
Optional<Patient> patient = patientRepository.findByMrn("EMR-A1B2C3D4");

// Nurse модуль
List<Emr> emrRecords = emrRepository.findByPatientMrn("EMR-A1B2C3D4");

// Anesthesiologist модуль
// Может использовать Patient для Treatment Protocol Service
```

---

# 🔄 ДЕТАЛЬНЫЙ WORKFLOW

## Сценарий 1: Импорт пациента из FHIR сервера

### Шаг за шагом:

```
1. FRONTEND
   Медсестра вводит FHIR ID пациента: "Patient/12345"
   POST /api/emr/import/Patient/12345?importedBy=nurse_maria
   
2. EmrIntegrationController.importPatientFromFhir()
   @PathVariable fhirPatientId = "Patient/12345"
   @RequestParam importedBy = "nurse_maria"
   Вызывает: emrIntegrationService.importPatientFromFhir(...)
   
3. EmrIntegrationServiceImpl.importPatientFromFhir()
   
   ШАГ 1: Проверка дубликата
   Optional<EmrMapping> existing = emrMappingRepository.findByExternalFhirId("Patient/12345");
   if (existing.isPresent()) {
       // Пациент уже импортирован
       return EmrImportResultDTO.success("Already imported");
   }
   
   ШАГ 2: Получение данных из FHIR сервера
   FhirPatientDTO fhirPatient = hapiFhirClient.getPatientById("Patient/12345");
   // Результат:
   // {
   //   firstName: "John",
   //   lastName: "Smith",
   //   dateOfBirth: "1980-01-15",
   //   gender: "male",
   //   phoneNumber: "+1234567890",
   //   email: "john.smith@example.com"
   // }
   
   ШАГ 3: Получение лабораторных анализов
   List<FhirObservationDTO> observations = hapiFhirClient.getObservationsForPatient("Patient/12345");
   // Результат:
   // [
   //   { loincCode: "2160-0", value: 1.2 },  // Креатинин
   //   { loincCode: "777-3", value: 200.0 }, // Тромбоциты
   //   { loincCode: "6690-2", value: 7.0 }   // Лейкоциты
   // ]
   
   ШАГ 4: Генерация внутреннего EMR номера
   String internalEmrNumber = generateInternalEmrNumber();
   // Результат: "EMR-A1B2C3D4"
   
   ШАГ 5: Сохранение маппинга
   EmrMapping mapping = new EmrMapping();
   mapping.setExternalFhirId("Patient/12345");
   mapping.setInternalEmrNumber("EMR-A1B2C3D4");
   mapping.setSourceType(EmrSourceType.FHIR_SERVER);
   mapping.setSourceSystemUrl("https://hospital-b.com/fhir");
   mapping.setImportedBy("nurse_maria");
   emrMappingRepository.save(mapping);
   
   ШАГ 6-7: Создание Patient и Emr в common/patients
   Long patientId = createPatientAndEmrFromFhir(
       fhirPatient,
       observations,
       internalEmrNumber,
       "nurse_maria"
   );
   // Результат: patientId = 42
   
   ШАГ 8: Формирование результата
   EmrImportResultDTO result = EmrImportResultDTO.success("Patient imported successfully");
   result.setExternalPatientIdInFhirResource("Patient/12345");
   result.setInternalPatientId(42);
   result.setNewPatientCreated(true);
   result.setObservationsImported(6);
   return result;

4. RESPONSE
   HTTP 200 OK
   {
     "success": true,
     "message": "Patient imported successfully from FHIR server",
     "externalPatientIdInFhirResource": "Patient/12345",
     "internalPatientId": 42,
     "matchConfidence": "NO_MATCH",
     "newPatientCreated": true,
     "sourceType": "FHIR_SERVER",
     "observationsImported": 6,
     "warnings": [],
     "errors": []
   }

5. БАЗА ДАННЫХ
   Созданы 3 записи:
   
   emr_mappings:
   | id | external_fhir_id | internal_emr_number | source_type  | imported_by  |
   |----|------------------|---------------------|--------------|--------------|
   | 1  | Patient/12345    | EMR-A1B2C3D4        | FHIR_SERVER  | nurse_maria  |
   
   nurse_patients:
   | id | mrn          | first_name | last_name | date_of_birth | gender | is_active |
   |----|--------------|------------|-----------|---------------|--------|-----------|
   | 42 | EMR-A1B2C3D4 | John       | Smith     | 1980-01-15    | MALE   | true      |
   
   emr:
   | id  | patient_id | gfr            | plt   | wbc  | sodium | sat  |
   |-----|------------|----------------|-------|------|--------|------|
   | 123 | 42         | ≥90 (Normal)   | 200.0 | 7.0  | 140.0  | 98.0 |

6. ДОСТУПНОСТЬ
   Теперь пациент доступен для всех модулей:
   - Doctor модуль: patientRepository.findByMrn("EMR-A1B2C3D4")
   - Nurse модуль: emrRepository.findByPatientMrn("EMR-A1B2C3D4")
   - Anesthesiologist: может использовать для Treatment Protocol
```

## Сценарий 2: Генерация моковых пациентов

### Шаг за шагом:

```
1. FRONTEND
   Фронтенд-разработчик хочет 50 пациентов для тестирования
   POST /api/emr/mock/generate-batch?count=50&createdBy=developer
   
2. EmrIntegrationController.generateMockBatch()
   @RequestParam count = 50
   @RequestParam createdBy = "developer"
   Вызывает: emrIntegrationService.generateAndImportMockBatch(50, "developer")
   
3. EmrIntegrationServiceImpl.generateAndImportMockBatch()
   
   ШАГ 1: Генерация 50 моковых пациентов
   List<FhirPatientDTO> mockPatients = mockEmrDataGenerator.generateBatch(50);
   // MockEmrDataGenerator использует JavaFaker для генерации:
   // - Имена: John Smith, Jane Doe, Michael Johnson, ...
   // - Даты рождения: случайный возраст 18-90 лет
   // - Контакты: телефон, email, адрес
   
   ШАГ 2: Импорт каждого пациента
   List<EmrImportResultDTO> results = mockPatients.stream()
       .map(patient -> importMockPatient(patient, "developer"))
       .collect(Collectors.toList());
   
   Для каждого пациента:
   - Генерируется EMR номер (EMR-XXXXXXXX)
   - Создается EmrMapping
   - Генерируются лабораторные данные (Observations)
   - Вызывается createPatientAndEmrFromFhir()
   - Создаются Patient и Emr в common/patients

4. RESPONSE
   HTTP 201 CREATED
   [
     {
       "success": true,
       "message": "Mock patient imported",
       "externalPatientIdInFhirResource": "mock-patient-1",
       "internalPatientId": 43,
       "newPatientCreated": true,
       "sourceType": "MOCK_GENERATOR",
       "observationsImported": 6
     },
     ... (еще 49 пациентов)
   ]

5. БАЗА ДАННЫХ
   Созданы 150 записей:
   - 50 записей в emr_mappings
   - 50 записей в nurse_patients
   - 50 записей в emr

6. ИСПОЛЬЗОВАНИЕ
   Фронтенд-разработчик теперь может:
   - Тестировать список пациентов
   - Тестировать фильтрацию
   - Тестировать поиск
   - Тестировать UI компоненты
```

---

# 🧩 КОМПОНЕНТЫ МОДУЛЯ

## 1. EmrIntegrationServiceImpl (Основная логика)

### Зависимости:

```java
@Service
@RequiredArgsConstructor
@Transactional
public class EmrIntegrationServiceImpl implements EmrIntegrationService {
    
    // Клиент для работы с FHIR сервером
    private final HapiFhirClient hapiFhirClient;
    
    // Репозиторий для маппинга FHIR ID ↔ EMR номер
    private final EmrMappingRepository emrMappingRepository;
    
    // Генератор моковых пациентов
    private final MockEmrDataGenerator mockEmrDataGenerator;
    
    // Репозитории из common/patients
    private final PatientRepository patientRepository;
    private final EmrRepository emrRepository;
}
```

### Методы:

#### 1. `importPatientFromFhir()`
- **Назначение:** Импорт пациента из реального FHIR сервера
- **Входные данные:** 
  - `fhirPatientId` (String) - ID пациента в FHIR системе
  - `importedBy` (String) - кто импортировал
- **Возвращает:** `EmrImportResultDTO`
- **Что делает:**
  1. Проверяет дубликат
  2. Получает данные из FHIR
  3. Получает лабораторные анализы
  4. Генерирует EMR номер
  5. Сохраняет EmrMapping
  6. Создает Patient и Emr
  7. Возвращает результат

#### 2. `generateAndImportMockPatient()`
- **Назначение:** Генерация 1 мокового пациента
- **Входные данные:** 
  - `createdBy` (String) - кто создал
- **Возвращает:** `EmrImportResultDTO`
- **Что делает:**
  1. Генерирует FhirPatientDTO с JavaFaker
  2. Вызывает `importMockPatient()`

#### 3. `generateAndImportMockBatch()`
- **Назначение:** Генерация N моковых пациентов
- **Входные данные:** 
  - `count` (int) - количество пациентов
  - `createdBy` (String) - кто создал
- **Возвращает:** `List<EmrImportResultDTO>`
- **Что делает:**
  1. Генерирует batch пациентов
  2. Для каждого вызывает `importMockPatient()`

#### 4. `searchPatientsInFhir()`
- **Назначение:** Поиск пациентов в FHIR системе
- **Входные данные:** 
  - `firstName`, `lastName`, `birthDate` (String)
- **Возвращает:** `List<FhirPatientDTO>`
- **@Transactional(readOnly = true)** - только чтение

#### 5. `getObservationsForPatient()`
- **Назначение:** Получение лабораторных анализов
- **Входные данные:** 
  - `fhirPatientId` (String)
- **Возвращает:** `List<FhirObservationDTO>`
- **@Transactional(readOnly = true)** - только чтение

#### 6. `convertObservationsToEmr()`
- **Назначение:** Конвертация FHIR Observations → EmrDTO
- **Входные данные:** 
  - `observations` (List<FhirObservationDTO>)
  - `createdBy` (String)
- **Возвращает:** `EmrDTO`
- **@Transactional(readOnly = true)** - только чтение

#### 7. `isPatientAlreadyImported()`
- **Назначение:** Проверка, импортирован ли пациент
- **Входные данные:** 
  - `fhirPatientId` (String)
- **Возвращает:** `boolean`

#### 8. `getInternalEmrNumber()`
- **Назначение:** Получение внутреннего EMR номера
- **Входные данные:** 
  - `fhirPatientId` (String)
- **Возвращает:** `String` (EMR номер или null)

### Вспомогательные методы:

#### `generateInternalEmrNumber()`
```java
private String generateInternalEmrNumber() {
    return "EMR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}
// Результат: "EMR-A1B2C3D4"
```

#### `importMockPatient()`
```java
private EmrImportResultDTO importMockPatient(FhirPatientDTO mockPatient, String createdBy) {
    // 1. Генерация EMR номера
    // 2. Создание EmrMapping
    // 3. Генерация Observations
    // 4. Вызов createPatientAndEmrFromFhir()
    // 5. Возврат результата
}
```

#### `createPatientAndEmrFromFhir()` ✅ КЛЮЧЕВОЙ МЕТОД
```java
private Long createPatientAndEmrFromFhir(
        FhirPatientDTO fhirPatient,
        List<FhirObservationDTO> observations,
        String internalEmrNumber,
        String createdBy) {
    
    // 1. Создание Patient в common.patients
    // 2. Создание Emr в common.patients
    // 3. Связывание Emr с Patient
    // 4. Возврат ID пациента
}
```

#### `calculateGfrCategory()`
```java
private String calculateGfrCategory(double creatinine) {
    // Упрощенный расчет: GFR ≈ 100 / креатинин
    double estimatedGfr = 100.0 / creatinine;
    
    if (estimatedGfr >= 90) return "≥90 (Normal)";
    if (estimatedGfr >= 60) return "60-89 (Mild decrease)";
    if (estimatedGfr >= 30) return "30-59 (Moderate decrease)";
    if (estimatedGfr >= 15) return "15-29 (Severe decrease)";
    return "<15 (Kidney failure)";
}
```

#### `convertGender()`
```java
private PatientsGenders convertGender(String gender) {
    if (gender == null) return null;
    
    String genderUpper = gender.toUpperCase();
    if (genderUpper.contains("MALE") && !genderUpper.contains("FEMALE")) {
        return PatientsGenders.MALE;
    } else if (genderUpper.contains("FEMALE")) {
        return PatientsGenders.FEMALE;
    }
    return null;
}
```

---

# 🌐 REST API ENDPOINTS

## 1. POST /api/emr/import/{fhirPatientId}

**Назначение:** Импорт пациента из FHIR системы другой больницы

**Параметры:**
- `fhirPatientId` (path) - ID пациента в FHIR системе (например, "Patient/12345")
- `importedBy` (query, optional, default="system") - кто импортировал

**Request:**
```http
POST /api/emr/import/Patient/12345?importedBy=nurse_maria
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Patient imported successfully from FHIR server",
  "externalPatientIdInFhirResource": "Patient/12345",
  "internalPatientId": 42,
  "matchConfidence": "NO_MATCH",
  "newPatientCreated": true,
  "sourceType": "FHIR_SERVER",
  "observationsImported": 6,
  "warnings": [],
  "errors": []
}
```

**Response (Already Imported):**
```json
{
  "success": true,
  "message": "Patient already imported",
  "externalPatientIdInFhirResource": "Patient/12345",
  "internalPatientId": 42,
  "matchConfidence": "EXACT",
  "newPatientCreated": false,
  "sourceType": "FHIR_SERVER",
  "warnings": ["Patient was already imported previously"],
  "errors": []
}
```

---

## 2. POST /api/emr/mock/generate

**Назначение:** Генерация 1 мокового (тестового) пациента

**Параметры:**
- `createdBy` (query, optional, default="system") - кто создал

**Request:**
```http
POST /api/emr/mock/generate?createdBy=developer
```

**Response:**
```json
{
  "success": true,
  "message": "Mock patient generated and imported successfully",
  "externalPatientIdInFhirResource": "mock-patient-abc123",
  "internalPatientId": 43,
  "matchConfidence": "NO_MATCH",
  "newPatientCreated": true,
  "sourceType": "MOCK_GENERATOR",
  "observationsImported": 6,
  "warnings": [],
  "errors": []
}
```

---

## 3. POST /api/emr/mock/generate-batch

**Назначение:** Генерация N моковых пациентов (максимум 100)

**Параметры:**
- `count` (query, optional, default=10) - количество пациентов
- `createdBy` (query, optional, default="system") - кто создал

**Request:**
```http
POST /api/emr/mock/generate-batch?count=50&createdBy=developer
```

**Response:**
```json
[
  {
    "success": true,
    "message": "Mock patient imported",
    "externalPatientIdInFhirResource": "mock-patient-1",
    "internalPatientId": 43,
    "newPatientCreated": true,
    "sourceType": "MOCK_GENERATOR",
    "observationsImported": 6
  },
  ... (еще 49 пациентов)
]
```

---

## 4. GET /api/emr/search

**Назначение:** Поиск пациентов в FHIR системе

**Параметры:**
- `firstName` (query, optional) - имя
- `lastName` (query, optional) - фамилия
- `birthDate` (query, optional) - дата рождения (YYYY-MM-DD)

**Request:**
```http
GET /api/emr/search?firstName=John&lastName=Smith&birthDate=1980-01-15
```

**Response:**
```json
[
  {
    "patientIdInFhirResource": "Patient/12345",
    "firstName": "John",
    "lastName": "Smith",
    "dateOfBirth": "1980-01-15",
    "gender": "male",
    "phoneNumber": "+1234567890",
    "email": "john.smith@example.com",
    "sourceSystemUrl": "https://hospital-b.com/fhir"
  }
]
```

---

## 5. GET /api/emr/observations/{fhirPatientId}

**Назначение:** Получение лабораторных анализов для пациента

**Параметры:**
- `fhirPatientId` (path) - ID пациента в FHIR системе

**Request:**
```http
GET /api/emr/observations/Patient/12345
```

**Response:**
```json
[
  {
    "observationId": "Observation/1",
    "loincCode": "2160-0",
    "value": 1.2,
    "unit": "mg/dL",
    "effectiveDateTime": "2025-10-01T10:00:00"
  },
  {
    "observationId": "Observation/2",
    "loincCode": "777-3",
    "value": 200.0,
    "unit": "10*3/uL",
    "effectiveDateTime": "2025-10-01T10:00:00"
  }
]
```

---

## 6. POST /api/emr/convert-observations

**Назначение:** Конвертация FHIR Observations в EmrDTO

**Параметры:**
- `createdBy` (query, optional, default="system") - кто создал

**Request:**
```http
POST /api/emr/convert-observations?createdBy=nurse_maria
Content-Type: application/json

[
  {
    "loincCode": "2160-0",
    "value": 1.2
  },
  {
    "loincCode": "777-3",
    "value": 200.0
  }
]
```

**Response:**
```json
{
  "gfr": "≥90 (Normal)",
  "plt": 200.0,
  "wbc": 7.0,
  "sodium": 140.0,
  "sat": 98.0,
  "childPughScore": "N/A",
  "createdBy": "nurse_maria",
  "createdAt": "2025-10-02T18:00:00"
}
```

---

## 7. GET /api/emr/check-import/{fhirPatientId}

**Назначение:** Проверка, импортирован ли пациент

**Параметры:**
- `fhirPatientId` (path) - ID пациента в FHIR системе

**Request:**
```http
GET /api/emr/check-import/Patient/12345
```

**Response:**
```json
{
  "alreadyImported": true,
  "internalEmrNumber": "EMR-A1B2C3D4"
}
```

---

# 💡 ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ

## Пример 1: Импорт пациента из другой больницы

### Сценарий:
Пациент John Smith приходит в нашу больницу из больницы Б. У него есть FHIR ID: "Patient/12345".

### Код (Frontend):
```javascript
async function importPatient(fhirPatientId) {
  const response = await fetch(
    `/api/emr/import/${fhirPatientId}?importedBy=nurse_maria`,
    { method: 'POST' }
  );
  
  const result = await response.json();
  
  if (result.success) {
    console.log('Пациент импортирован!');
    console.log('Внутренний ID:', result.internalPatientId);
    console.log('EMR номер:', result.internalEmrNumber);
  }
}

importPatient('Patient/12345');
```

### Что происходит в backend:
1. EmrIntegrationController получает запрос
2. EmrIntegrationServiceImpl:
   - Проверяет дубликат
   - Получает данные из FHIR сервера больницы Б
   - Получает лабораторные анализы
   - Генерирует EMR номер: "EMR-A1B2C3D4"
   - Создает EmrMapping
   - Создает Patient в `nurse_patients`
   - Создает Emr в `emr`
3. Возвращает результат

### Результат в БД:
```sql
-- emr_mappings
INSERT INTO emr_mappings (external_fhir_id, internal_emr_number, source_type, imported_by)
VALUES ('Patient/12345', 'EMR-A1B2C3D4', 'FHIR_SERVER', 'nurse_maria');

-- nurse_patients
INSERT INTO nurse_patients (mrn, first_name, last_name, date_of_birth, gender, is_active)
VALUES ('EMR-A1B2C3D4', 'John', 'Smith', '1980-01-15', 'MALE', true);

-- emr
INSERT INTO emr (patient_id, gfr, plt, wbc, sodium, sat, child_pugh_score)
VALUES (42, '≥90 (Normal)', 200.0, 7.0, 140.0, 98.0, 'N/A');
```

---

## Пример 2: Генерация 50 моковых пациентов для тестирования

### Сценарий:
Фронтенд-разработчик хочет протестировать список пациентов, но в БД пусто.

### Код (Frontend):
```javascript
async function generateTestPatients() {
  const response = await fetch(
    '/api/emr/mock/generate-batch?count=50&createdBy=developer',
    { method: 'POST' }
  );
  
  const results = await response.json();
  
  console.log(`Создано ${results.length} пациентов`);
  
  // Теперь можно тестировать список
  const patients = await fetch('/api/patients').then(r => r.json());
  console.log('Пациенты:', patients);
}

generateTestPatients();
```

### Что происходит в backend:
1. MockEmrDataGenerator генерирует 50 пациентов с JavaFaker
2. Для каждого пациента:
   - Генерируется EMR номер
   - Создается EmrMapping
   - Генерируются лабораторные данные
   - Создается Patient
   - Создается Emr
3. Возвращается массив из 50 результатов

### Результат в БД:
```sql
-- 50 записей в emr_mappings
-- 50 записей в nurse_patients
-- 50 записей в emr
```

---

## Пример 3: Проверка, импортирован ли пациент

### Сценарий:
Перед импортом нужно проверить, не импортировали ли мы этого пациента раньше.

### Код (Frontend):
```javascript
async function checkAndImport(fhirPatientId) {
  // Проверяем
  const checkResponse = await fetch(`/api/emr/check-import/${fhirPatientId}`);
  const checkResult = await checkResponse.json();
  
  if (checkResult.alreadyImported) {
    console.log('Пациент уже импортирован!');
    console.log('EMR номер:', checkResult.internalEmrNumber);
    return;
  }
  
  // Импортируем
  const importResponse = await fetch(
    `/api/emr/import/${fhirPatientId}?importedBy=nurse_maria`,
    { method: 'POST' }
  );
  
  const importResult = await importResponse.json();
  console.log('Пациент импортирован:', importResult);
}

checkAndImport('Patient/12345');
```

---

# 🛠️ ТЕХНИЧЕСКИЕ РЕШЕНИЯ

## 1. Устранение дублирования кода

### Проблема:
Методы `generateAndImportMockPatient()` и `importMockPatient()` дублировали код создания Patient и Emr.

### Решение:
Создан **общий метод** `createPatientAndEmrFromFhir()`, который используется в:
- `importPatientFromFhir()`
- `generateAndImportMockPatient()`
- `importMockPatient()`

### Преимущества:
- ✅ Код в одном месте
- ✅ Легче поддерживать
- ✅ Меньше ошибок
- ✅ DRY принцип (Don't Repeat Yourself)

---

## 2. Транзакционность

### @Transactional на уровне класса:
```java
@Service
@Transactional  // Все методы изменения данных в одной транзакции
public class EmrIntegrationServiceImpl implements EmrIntegrationService {
    // ...
}
```

### @Transactional(readOnly = true) для методов чтения:
```java
@Override
@Transactional(readOnly = true)  // Оптимизация: только чтение
public List<FhirPatientDTO> searchPatientsInFhir(...) {
    // ...
}
```

### Преимущества:
- ✅ Атомарность операций
- ✅ Откат при ошибке
- ✅ Оптимизация для read-only методов

---

## 3. Lombok для уменьшения boilerplate кода

### Entity:
```java
@Entity
@Table(name = "emr_mappings")
@Data  // Автогенерация геттеров, сеттеров, toString(), equals(), hashCode()
public class EmrMapping {
    // ...
}
```

### Service:
```java
@Service
@RequiredArgsConstructor  // Автогенерация конструктора для final полей
@Slf4j  // Автогенерация логгера
@Transactional
public class EmrIntegrationServiceImpl implements EmrIntegrationService {
    // ...
}
```

---

## 4. Enum для типов источников

```java
public enum EmrSourceType {
    FHIR_SERVER,        // Реальный FHIR сервер другой больницы
    MOCK_GENERATOR,     // Генератор моковых данных
    EXTERNAL_HOSPITAL,  // Внешняя больница (не FHIR)
    MANUAL_ENTRY        // Ручной ввод
}
```

---

## 5. LOINC коды для лабораторных анализов

LOINC (Logical Observation Identifiers Names and Codes) - стандартные медицинские коды.

### Используемые коды:
- **"2160-0"** → Креатинин (для расчета GFR)
- **"777-3"** → Тромбоциты (PLT)
- **"6690-2"** → Лейкоциты (WBC)
- **"2951-2"** → Натрий
- **"59408-5"** → Сатурация кислорода (SpO2)

---

## 6. Расчет GFR из креатинина

### Медицинское объяснение:
**GFR (Glomerular Filtration Rate)** = Скорость клубочковой фильтрации

Показывает, насколько хорошо почки фильтруют кровь от токсинов.

### Категории GFR:
- **≥90 ml/min:** Нормальная функция почек
- **60-89:** Умеренное снижение (начальная стадия)
- **30-59:** Значительное снижение (НУЖНА КОРРЕКТИРОВКА ДОЗЫ ПРЕПАРАТОВ!)
- **15-29:** Тяжелое снижение (снижение дозы на 50-75%)
- **<15:** Почечная недостаточность (многие препараты противопоказаны)

### Упрощенная формула:
```
GFR ≈ 100 / креатинин
```

### Реальная формула (Cockcroft-Gault):
```
GFR = ((140 - возраст) × вес × (0.85 если женщина)) / (72 × креатинин)
```

### Зачем:
Если GFR низкий, многие обезболивающие накапливаются в организме и могут вызвать передозировку. Нужно снижать дозу!

---

## 7. Конвертация gender: String → Enum

### Проблема:
В FHIR gender приходит как String ("male", "female", "other", "unknown"), а в нашей БД хранится как enum `PatientsGenders`.

### Решение:
```java
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
```

---

# 📊 ДИАГРАММЫ

## Диаграмма классов

```
┌─────────────────────────────────────────────────────────────────┐
│                    EmrIntegrationController                      │
│  - emrIntegrationService: EmrIntegrationService                 │
│  + importPatientFromFhir()                                      │
│  + generateMockPatient()                                        │
│  + generateMockBatch()                                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  EmrIntegrationServiceImpl                       │
│  - hapiFhirClient: HapiFhirClient                               │
│  - emrMappingRepository: EmrMappingRepository                   │
│  - mockEmrDataGenerator: MockEmrDataGenerator                   │
│  - patientRepository: PatientRepository                         │
│  - emrRepository: EmrRepository                                 │
│  + importPatientFromFhir()                                      │
│  + generateAndImportMockPatient()                               │
│  + generateAndImportMockBatch()                                 │
│  - createPatientAndEmrFromFhir() ← ОБЩИЙ МЕТОД                 │
│  - calculateGfrCategory()                                       │
│  - convertGender()                                              │
└─────────────────────────────────────────────────────────────────┘
                    ↓                         ↓
┌──────────────────────────────┐  ┌──────────────────────────────┐
│   EmrMappingRepository       │  │   PatientRepository          │
│   (emr_integration)          │  │   (common/patients)          │
│   - findByExternalFhirId()   │  │   - findByMrn()              │
│   - existsByExternalFhirId() │  │   - save()                   │
└──────────────────────────────┘  └──────────────────────────────┘
                                              ↓
                                  ┌──────────────────────────────┐
                                  │   EmrRepository              │
                                  │   (common/patients)          │
                                  │   - findByPatientMrn()       │
                                  │   - save()                   │
                                  └──────────────────────────────┘
```

## Диаграмма последовательности (Импорт пациента)

```
Frontend          Controller         Service            FHIR Client      Database
   │                  │                 │                    │              │
   │ POST /import     │                 │                    │              │
   │─────────────────>│                 │                    │              │
   │                  │ importPatient() │                    │              │
   │                  │────────────────>│                    │              │
   │                  │                 │ getPatientById()   │              │
   │                  │                 │───────────────────>│              │
   │                  │                 │<───────────────────│              │
   │                  │                 │ getObservations()  │              │
   │                  │                 │───────────────────>│              │
   │                  │                 │<───────────────────│              │
   │                  │                 │ save(EmrMapping)   │              │
   │                  │                 │───────────────────────────────────>│
   │                  │                 │ save(Patient)      │              │
   │                  │                 │───────────────────────────────────>│
   │                  │                 │ save(Emr)          │              │
   │                  │                 │───────────────────────────────────>│
   │                  │<────────────────│                    │              │
   │<─────────────────│                 │                    │              │
   │  200 OK          │                 │                    │              │
```

---

# 🎓 ЗАКЛЮЧЕНИЕ

## Что мы реализовали:

1. ✅ **Полный модуль EMR Integration** для импорта пациентов из FHIR систем
2. ✅ **Интеграция с common/patients** - все импортированные пациенты доступны для всех модулей
3. ✅ **Генерация моковых пациентов** для разработки и тестирования
4. ✅ **Устранение дублирования кода** через общий метод `createPatientAndEmrFromFhir()`
5. ✅ **REST API** с 7 endpoints
6. ✅ **Транзакционность** для атомарности операций
7. ✅ **Конвертация медицинских данных** из FHIR формата в наш формат
8. ✅ **Расчет GFR** из креатинина для корректировки доз препаратов

## Ключевые преимущества:

- 🚀 **Быстрый импорт** пациентов из других больниц
- 🧪 **Моковые данные** для тестирования
- 🔗 **Единая таблица пациентов** для всех модулей
- 🛡️ **Избежание дубликатов** через EmrMapping
- 📊 **Лабораторные данные** для Treatment Protocol Service
- 🏥 **Стандарт FHIR** для совместимости с другими системами

## Следующие шаги:

1. 🔐 Добавить аутентификацию (заменить `@RequestParam(defaultValue = "system")`)
2. 📝 Добавить валидацию DTO с `@Valid`, `@NotBlank`, `@Size`
3. 🧪 Написать unit-тесты для `EmrIntegrationServiceImpl`
4. 📊 Добавить мониторинг и метрики (сколько пациентов импортировано)
5. 🔄 Добавить синхронизацию данных (обновление существующих пациентов)

---

**Документ создан:** 02.10.2025  
**Автор:** Backend Team (Pain Management System)  
**Версия:** 1.0
