# 🔄 ПОЛНЫЙ WORKFLOW СИСТЕМЫ УПРАВЛЕНИЯ БОЛЬЮ

## 📋 Обзор архитектуры

Система использует **ЕДИНУЮ таблицу рекомендаций** для всех модулей (Nurse, Doctor, Anesthesiologist).

### Ключевые сущности (Entity):

1. **`common/patients/entity/Patient`** - пациент (единая таблица)
    - Поля: `id`, `mrn`, `firstName`, `lastName`, `dateOfBirth`, `gender`, `phoneNumber`, `email`,
      `insurancePolicyNumber`
    - Таблица БД: `nurse_patients`

2. **`common/patients/entity/Vas`** - жалоба на боль
    - Поля: `id`, `patient`, `painLevel` (0-10), `location`, `description`, `recordedAt`
    - Таблица БД: `vas`

3. **`common/patients/entity/Emr`** - медицинская карта
    - Поля: `id`, `patient`, `gfr`, `childPughScore`, `plt`, `wbc`, `sodium`, `sat`, `height`, `weight`
    - Таблица БД: `emr`

4. **`common/patients/entity/Recommendation`** - центральная сущность рекомендации (110 строк)
    - Поля workflow:
        - `status` (RecommendationStatus enum)
        - `doctorId`, `doctorActionAt`, `doctorComment`
        - `anesthesiologistId`, `anesthesiologistActionAt`, `anesthesiologistComment`
        - `finalApprovedBy`, `finalApprovalAt`
        - `escalation` (OneToOne связь с Escalation)
    - Таблица БД: `recommendation`

5. **`anesthesiologist/entity/Escalation`** - эскалация
    - Поля: `id`, `recommendation`, `escalatedBy`, `escalatedAt`, `escalationReason`, `priority`, `status`,
      `resolvedBy`, `resolvedAt`, `resolution`
    - Таблица БД: `escalation`

---

## 🏥 ШАГ 1: NURSE → Создание рекомендации

### 1.1. Регистрация пациента (если новый)

**Endpoint**: `POST /api/nurse/patients`

**Входные данные:**

- **DTO**: `PatientDTO`
  ```java

{
"firstName": "Иван",
"lastName": "Петров",
"dateOfBirth": "1985-05-15",
"gender": "MALE",
"phoneNumber": "+7 999 123-45-67",
"insurancePolicyNumber": "1234567890"
}

  ```

**Процесс в NurseService:**
```java
// 1. Создать Patient
Patient patient = new Patient();
patient.setFirstName("Иван");
patient.setLastName("Петров");
// ... остальные поля

// 2. Сохранить в БД
Patient savedPatient = patientRepository.save(patient);

// 3. Сгенерировать MRN (Medical Record Number)
String mrn = "MRN-" + savedPatient.getId();
savedPatient.setMrn(mrn);
patientRepository.save(savedPatient);
```

**Результат в БД:**

```sql
-- Таблица: nurse_patients
INSERT INTO nurse_patients (id, mrn, first_name, last_name, date_of_birth, gender,
                            phone_number, insurance_policy_number, is_active, created_at)
VALUES (1, 'MRN-1', 'Иван', 'Петров', '1985-05-15', 'MALE',
        '+7 999 123-45-67', '1234567890', true, '2025-10-08 10:00:00');
```

---

### 1.2. Ввод медицинской карты (EMR)

**Endpoint**: `POST /api/nurse/patients/{patientId}/emr`

**Входные данные:**

- **DTO**: `EmrDTO`
  ```java
  {
    "gfr": "Normal (>90)",
    "childPughScore": "A",
    "plt": 250.0,
    "wbc": 7.5,
    "sodium": 140.0,
    "sat": 98.0,
    "height": 175.0,
    "weight": 80.0
  }
  ```

**Процесс в NurseService:**

```java
// 1. Найти пациента
Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found"));

// 2. Создать EMR
Emr emr = new Emr();
emr.

setPatient(patient);
emr.

setGfr("Normal (>90)");
emr.

setChildPughScore("A");
// ... остальные поля

// 3. Сохранить
emrRepository.

save(emr);
```

**Результат в БД:**

```sql
-- Таблица: emr
INSERT INTO emr (id, patient_id, gfr, child_pugh_score, plt, wbc,
                 sodium, sat, height, weight, created_at)
VALUES (1, 1, 'Normal (>90)', 'A', 250.0, 7.5,
        140.0, 98.0, 175.0, 80.0, '2025-10-08 10:05:00');
```

---

### 1.3. Ввод жалобы на боль (VAS)

**Endpoint**: `POST /api/nurse/patients/{patientId}/vas`

**Входные данные:**

- **DTO**: `VasInputDTO`
  ```java
  {
    "painLevel": 8,
    "location": "Lower back",
    "description": "Sharp pain, worse when moving"
  }
  ```

**Процесс в NurseService:**

```java
// 1. Найти пациента
Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found"));

// 2. Создать VAS запись
Vas vas = new Vas();
vas.

setPatient(patient);
vas.

setPainLevel(8);
vas.

setLocation("Lower back");
vas.

setDescription("Sharp pain, worse when moving");
vas.

setRecordedAt(LocalDateTime.now());

// 3. Сохранить
Vas savedVas = vasRepository.save(vas);
```

**Результат в БД:**

```sql
-- Таблица: vas
INSERT INTO vas (id, patient_id, pain_level, location, description, recorded_at)
VALUES (1, 1, 8, 'Lower back', 'Sharp pain, worse when moving', '2025-10-08 10:10:00');
```

---

### 1.4. Генерация рекомендации (автоматически)

**Процесс в TreatmentProtocolService:**

```java
// 1. Получить медицинские данные пациента
Emr emr = emrRepository.findByPatientId(patientId);
Vas vas = vasRepository.findLatestByPatientId(patientId);

// 2. Отфильтровать протокол лечения по уровню боли
List<TreatmentProtocol> protocols = treatmentProtocolRepository
        .findByPainLevelRange(vas.getPainLevel());

// 3. Применить фильтры:
//    - Возраст пациента
//    - Вес пациента
//    - GFR (функция почек)
//    - Child-Pugh Score (функция печени)
//    - Противопоказания

// 4. Выбрать оптимальный протокол
TreatmentProtocol selectedProtocol = applyFilters(protocols, patient, emr);

// 5. Создать рекомендацию
Recommendation recommendation = new Recommendation();
recommendation.

setPatient(patient);
recommendation.

setRegimenHierarchy(selectedProtocol.getRegimenHierarchy());
        recommendation.

setStatus(RecommendationStatus.PENDING);

// 6. Добавить препараты из протокола
List<DrugRecommendation> drugs = new ArrayList<>();
DrugRecommendation drug = new DrugRecommendation();
drug.

setDrugName(selectedProtocol.getDrugName());
        drug.

setDosage(selectedProtocol.getDosage());
        drug.

setRoute(selectedProtocol.getRoute());
        drug.

setFrequency(selectedProtocol.getFrequency());
        drug.

setRecommendation(recommendation);
drugs.

add(drug);

recommendation.

setDrugs(drugs);

// 7. Добавить противопоказания
if(selectedProtocol.

getContraindications() !=null){
        recommendation.

setContraindications(
        Arrays.asList(selectedProtocol.getContraindications().

split(","))
        );
        }

// 8. Сохранить
Recommendation savedRecommendation = recommendationRepository.save(recommendation);
```

**Результат в БД:**

```sql
-- Таблица: recommendation
INSERT INTO recommendation (recommendation_id, patient_id, regimen_hierarchy, status,
                            created_at, created_by)
VALUES (1, 1, 1, 'PENDING', '2025-10-08 10:15:00', 'system');

-- Таблица: drug_recommendation
INSERT INTO drug_recommendation (id, recommendation_id, drug_name, dosage, route, frequency)
VALUES (1, 1, 'Paracetamol', '500mg', 'PO', 'q6h');

-- Таблица: recommendation_contraindications
INSERT INTO recommendation_contraindications (recommendation_id, element)
VALUES (1, 'Liver disease');
```

**Связанные DTO:**

- **Вход**: `VasInputDTO`
- **Выход**: `RecommendationDTO`

---

## 👨‍⚕️ ШАГ 2: DOCTOR → Одобрение или отклонение

### 2.1. Просмотр рекомендаций

**Endpoint**: `GET /api/doctor/recommendations/pending`

**Процесс в DoctorService:**

```java
// Получить все рекомендации со статусом PENDING
List<Recommendation> recommendations = recommendationRepository
                .findByStatus(RecommendationStatus.PENDING);

// Конвертировать в DTO
List<RecommendationWithVasDTO> dtos = recommendations.stream()
        .map(rec -> {
            RecommendationWithVasDTO dto = modelMapper.map(rec, RecommendationWithVasDTO.class);
            // Добавить данные VAS
            Vas vas = vasRepository.findLatestByPatientId(rec.getPatient().getId());
            dto.setVasLevel(vas.getPainLevel());
            dto.setVasLocation(vas.getLocation());
            return dto;
        })
        .toList();
```

**Ответ (DTO):**

```json
[
  {
    "id": 1,
    "patientName": "Иван Петров",
    "patientMrn": "MRN-1",
    "status": "PENDING",
    "regimenHierarchy": 1,
    "drugs": [
      {
        "drugName": "Paracetamol",
        "dosage": "500mg",
        "route": "PO",
        "frequency": "q6h"
      }
    ],
    "vasLevel": 8,
    "vasLocation": "Lower back",
    "createdAt": "2025-10-08T10:15:00"
  }
]
```

---

### 2.2. СЦЕНАРИЙ А: Врач одобряет рекомендацию

**Endpoint**: `PUT /api/doctor/recommendations/{id}/approve`

**Входные данные:**

- **DTO**: `RecommendationApprovalRejectionDTO`
  ```java
  {
    "doctorId": "DOC123",
    "comment": "Approved, patient stable, no contraindications"
  }
  ```

**Процесс в DoctorServiceImpl.approveRecommendation():**

```java
// 1. Найти рекомендацию
Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

// 2. Проверить статус
if(recommendation.

getStatus() !=RecommendationStatus.PENDING){
        throw new

IllegalStateException("Recommendation is not pending");
}

// 3. Обновить поля - ОДОБРЕНИЕ ВРАЧА
        recommendation.

setStatus(RecommendationStatus.APPROVED_BY_DOCTOR);
recommendation.

setDoctorId(dto.getDoctorId());
        recommendation.

setDoctorActionAt(LocalDateTime.now());
        recommendation.

setDoctorComment(dto.getComment());

// 4. ФИНАЛЬНОЕ ОДОБРЕНИЕ (т.к. нет эскалации)
        recommendation.

setStatus(RecommendationStatus.FINAL_APPROVED);
recommendation.

setFinalApprovedBy(dto.getDoctorId());
        recommendation.

setFinalApprovalAt(LocalDateTime.now());

// 5. Добавить комментарий в список
        if(dto.

getComment() !=null&&!dto.

getComment().

isBlank()){
        recommendation.

getComments().

add("Doctor: "+dto.getComment());
        }

// 6. Сохранить
Recommendation savedRecommendation = recommendationRepository.save(recommendation);

// 7. Логирование
log.

info("Recommendation approved: id={}, doctorId={}, status={}",
     savedRecommendation.getId(),dto.

getDoctorId(),savedRecommendation.

getStatus());
```

**Результат в БД:**

```sql
-- Таблица: recommendation (UPDATE)
UPDATE recommendation
SET status            = 'FINAL_APPROVED',
    doctor_id         = 'DOC123',
    doctor_action_at  = '2025-10-08 14:30:00',
    doctor_comment    = 'Approved, patient stable, no contraindications',
    final_approved_by = 'DOC123',
    final_approval_at = '2025-10-08 14:30:00',
    updated_at        = '2025-10-08 14:30:00',
    updated_by        = 'DOC123'
WHERE recommendation_id = 1;

-- Таблица: recommendation_comments (INSERT)
INSERT INTO recommendation_comments (recommendation_id, element)
VALUES (1, 'Doctor: Approved, patient stable, no contraindications');
```

**Связанные файлы:**

- **Entity**: `common/patients/entity/Recommendation.java` (строки 49-56: doctor level поля)
- **DTO**: `doctor/dto/RecommendationApprovalRejectionDTO.java`
- **Service**: `doctor/service/DoctorServiceImpl.java` (метод `approveRecommendation()`)
- **Repository**: `common/patients/repository/RecommendationRepository.java`

**Workflow завершен ✅** - рекомендация одобрена врачом и финально утверждена.

---

### 2.3. СЦЕНАРИЙ Б: Врач отклоняет → АВТОМАТИЧЕСКАЯ ЭСКАЛАЦИЯ

**Endpoint**: `PUT /api/doctor/recommendations/{id}/reject`

**Входные данные:**

- **DTO**: `RecommendationApprovalRejectionDTO`
  ```java
  {
    "doctorId": "DOC123",
    "comment": "Patient has contraindications",
    "rejectedReason": "Allergy to NSAIDs, liver dysfunction"
  }
  ```

**Процесс в DoctorServiceImpl.rejectRecommendation():**

```java
// 1. Найти рекомендацию
Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

// 2. Проверить статус
if(recommendation.

getStatus() !=RecommendationStatus.PENDING){
        throw new

IllegalStateException("Recommendation is not pending");
}

// 3. Получить VAS для определения приоритета эскалации
Vas vas = vasRepository.findLatestByPatientId(recommendation.getPatient().getId());

// 4. Обновить рекомендацию - ОТКЛОНЕНИЕ
recommendation.

setStatus(RecommendationStatus.REJECTED_BY_DOCTOR);
recommendation.

setDoctorId(dto.getDoctorId());
        recommendation.

setDoctorActionAt(LocalDateTime.now());
        recommendation.

setDoctorComment(dto.getComment());
        recommendation.

setRejectedReason(dto.getRejectedReason());

// 5. СОЗДАТЬ ЭСКАЛАЦИЮ (автоматически)
Escalation escalation = new Escalation();
escalation.

setRecommendation(recommendation);
escalation.

setEscalatedBy(dto.getDoctorId());
        escalation.

setEscalatedAt(LocalDateTime.now());
        escalation.

setEscalationReason(dto.getRejectedReason());
        escalation.

setDescription("Doctor rejected recommendation due to: "+dto.getRejectedReason());

// 6. ОПРЕДЕЛИТЬ ПРИОРИТЕТ по уровню боли VAS
EscalationPriority priority;
if(vas.

getPainLevel() >=8){
priority =EscalationPriority.HIGH;    // Критическая боль
}else if(vas.

getPainLevel() >=5){
priority =EscalationPriority.MEDIUM;  // Умеренная боль
}else{
priority =EscalationPriority.LOW;     // Легкая боль
}

        escalation.

setPriority(priority);
escalation.

setStatus(EscalationStatus.PENDING);

// 7. Связать эскалацию с рекомендацией
recommendation.

setEscalation(escalation);
recommendation.

setStatus(RecommendationStatus.ESCALATED_TO_ANESTHESIOLOGIST);

// 8. Добавить комментарий
if(dto.

getComment() !=null&&!dto.

getComment().

isBlank()){
        recommendation.

getComments().

add("Doctor (REJECTED): "+dto.getComment());
        }

// 9. Сохранить (cascade сохранит Escalation)
Recommendation savedRecommendation = recommendationRepository.save(recommendation);

// 10. Логирование
log.

info("Recommendation rejected and escalated: recommendationId={}, escalationId={}, priority={}, vasLevel={}",
     savedRecommendation.getId(), 
    savedRecommendation.

getEscalation().

getId(),

priority,
        vas.

getPainLevel());
```

**Результат в БД:**

```sql
-- Таблица: recommendation (UPDATE)
UPDATE recommendation
SET status           = 'ESCALATED_TO_ANESTHESIOLOGIST',
    doctor_id        = 'DOC123',
    doctor_action_at = '2025-10-08 14:30:00',
    doctor_comment   = 'Patient has contraindications',
    rejected_reason  = 'Allergy to NSAIDs, liver dysfunction',
    updated_at       = '2025-10-08 14:30:00',
    updated_by       = 'DOC123'
WHERE recommendation_id = 1;

-- Таблица: escalation (INSERT - НОВАЯ ЗАПИСЬ)
INSERT INTO escalation (escalation_id, recommendation_id, escalated_by, escalated_at,
                        escalation_reason, description, priority, status,
                        created_at, created_by)
VALUES (1, 1, 'DOC123', '2025-10-08 14:30:00',
        'Allergy to NSAIDs, liver dysfunction',
        'Doctor rejected recommendation due to: Allergy to NSAIDs, liver dysfunction',
        'HIGH', -- т.к. VAS = 8
        'PENDING',
        '2025-10-08 14:30:00', 'DOC123');

-- Таблица: recommendation_comments (INSERT)
INSERT INTO recommendation_comments (recommendation_id, element)
VALUES (1, 'Doctor (REJECTED): Patient has contraindications');
```

**Связанные файлы:**

- **Entity**:
    - `common/patients/entity/Recommendation.java` (строки 49-56: doctor level, строка 58: escalation)
    - `anesthesiologist/entity/Escalation.java` (вся сущность)
- **DTO**: `doctor/dto/RecommendationApprovalRejectionDTO.java`
- **Service**: `doctor/service/DoctorServiceImpl.java` (метод `rejectRecommendation()`)
- **Repository**:
    - `common/patients/repository/RecommendationRepository.java`
    - `anesthesiologist/repository/TreatmentEscalationRepository.java` (создается через cascade)

**Workflow продолжается** → переход к анестезиологу

---

## 💉 ШАГ 3: ANESTHESIOLOGIST → Разрешение эскалации

### 3.1. Просмотр эскалаций

**Endpoint**: `GET /api/anesthesiologist/escalations/active`

**Процесс в AnesthesiologistServiceImpl:**

```java
// Получить активные эскалации, отсортированные по приоритету
List<Escalation> escalations = escalationRepository
                .findActiveEscalationsOrderedByPriorityAndDate();

// Конвертировать в DTO
List<EscalationResponseDTO> dtos = escalations.stream()
        .map(escalation -> modelMapper.map(escalation, EscalationResponseDTO.class))
        .toList();
```

**SQL запрос в TreatmentEscalationRepository:**

```sql
SELECT e
FROM Escalation e
WHERE e.status IN ('PENDING', 'IN_PROGRESS')
ORDER BY CASE e.priority
             WHEN 'HIGH' THEN 1
             WHEN 'MEDIUM' THEN 2
             WHEN 'LOW' THEN 3
             END,
         e.createdAt ASC
```

**Ответ (DTO):**

```json
[
  {
    "id": 1,
    "recommendationId": 1,
    "patientName": "Иван Петров",
    "patientMrn": "MRN-1",
    "escalatedBy": "DOC123",
    "escalatedAt": "2025-10-08T14:30:00",
    "escalationReason": "Allergy to NSAIDs, liver dysfunction",
    "priority": "HIGH",
    "status": "PENDING",
    "vasLevel": 8
  }
]
```

---

### 3.2. СЦЕНАРИЙ А: Анестезиолог одобряет эскалацию

**Endpoint**: `PUT /api/anesthesiologist/escalations/{id}/approve`

**Входные данные:**

- **DTO**: `EscalationResolutionDTO`
  ```java
  {
    "resolvedBy": "ANESTH456",
    "comment": "Approved alternative protocol with Tramadol",
    "resolution": "Changed to opioid-based protocol due to NSAID allergy"
  }
  ```

**Процесс в AnesthesiologistServiceImpl.approveEscalation():**

```java
// 1. Найти эскалацию
Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new NotFoundException("Escalation not found with id: " + escalationId));

// 2. Проверить статус
if(escalation.

getStatus() ==EscalationStatus.RESOLVED ||
        escalation.

getStatus() ==EscalationStatus.CANCELLED){
        throw new

IllegalStateException("Escalation is already resolved or cancelled");
}

// 3. Обновить эскалацию
        escalation.

setStatus(EscalationStatus.RESOLVED);
escalation.

setResolvedBy(dto.getResolvedBy());
        escalation.

setResolvedAt(LocalDateTime.now());
        escalation.

setResolution(dto.getResolution());

// 4. Получить связанную рекомендацию
Recommendation recommendation = escalation.getRecommendation();

// 5. Обновить рекомендацию - ОДОБРЕНИЕ АНЕСТЕЗИОЛОГА
recommendation.

setStatus(RecommendationStatus.APPROVED_BY_ANESTHESIOLOGIST);
recommendation.

setAnesthesiologistId(dto.getResolvedBy());
        recommendation.

setAnesthesiologistActionAt(LocalDateTime.now());
        recommendation.

setAnesthesiologistComment(dto.getComment());

// 6. ФИНАЛЬНОЕ ОДОБРЕНИЕ
        recommendation.

setStatus(RecommendationStatus.FINAL_APPROVED);
recommendation.

setFinalApprovedBy(dto.getResolvedBy());
        recommendation.

setFinalApprovalAt(LocalDateTime.now());

// 7. Добавить комментарий в список
        if(dto.

getComment() !=null&&!dto.

getComment().

isBlank()){
        recommendation.

getComments().

add("Anesthesiologist: "+dto.getComment());
        }

// 8. Сохранить
        recommendationRepository.

save(recommendation);

Escalation savedEscalation = escalationRepository.save(escalation);

// 9. Логирование
log.

info("Escalation approved: id={}, recommendationId={}, status={}",
     savedEscalation.getId(), 
    recommendation.

getId(), 
    recommendation.

getStatus());
```

**Результат в БД:**

```sql
-- Таблица: escalation (UPDATE)
UPDATE escalation
SET status      = 'RESOLVED',
    resolved_by = 'ANESTH456',
    resolved_at = '2025-10-08 15:00:00',
    resolution  = 'Changed to opioid-based protocol due to NSAID allergy',
    updated_at  = '2025-10-08 15:00:00',
    updated_by  = 'ANESTH456'
WHERE escalation_id = 1;

-- Таблица: recommendation (UPDATE)
UPDATE recommendation
SET status                     = 'FINAL_APPROVED',
    anesthesiologist_id        = 'ANESTH456',
    anesthesiologist_action_at = '2025-10-08 15:00:00',
    anesthesiologist_comment   = 'Approved alternative protocol with Tramadol',
    final_approved_by          = 'ANESTH456',
    final_approval_at          = '2025-10-08 15:00:00',
    updated_at                 = '2025-10-08 15:00:00',
    updated_by                 = 'ANESTH456'
WHERE recommendation_id = 1;

-- Таблица: recommendation_comments (INSERT)
INSERT INTO recommendation_comments (recommendation_id, element)
VALUES (1, 'Anesthesiologist: Approved alternative protocol with Tramadol');
```

**Связанные файлы:**

- **Entity**:
    - `anesthesiologist/entity/Escalation.java` (строки 51-56: resolved fields)
    - `common/patients/entity/Recommendation.java` (строки 61-73: anesthesiologist + final approval)
- **DTO**: `anesthesiologist/dto/EscalationResolutionDTO.java`
- **Service**: `anesthesiologist/service/AnesthesiologistServiceImpl.java` (метод `approveEscalation()`)
- **Repository**:
    - `anesthesiologist/repository/TreatmentEscalationRepository.java`
    - `common/patients/repository/RecommendationRepository.java`

**Workflow завершен ✅** - эскалация разрешена, рекомендация финально одобрена.

---

### 3.3. СЦЕНАРИЙ Б: Анестезиолог отклоняет эскалацию

**Endpoint**: `PUT /api/anesthesiologist/escalations/{id}/reject`

**Входные данные:**

- **DTO**: `EscalationResolutionDTO`
  ```java
  {
    "resolvedBy": "ANESTH456",
    "comment": "Patient requires different approach, consult pain specialist",
    "resolution": "Rejected, needs multidisciplinary consultation"
  }
  ```

**Процесс в AnesthesiologistServiceImpl.rejectEscalation():**

```java
// 1. Найти эскалацию
Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new NotFoundException("Escalation not found with id: " + escalationId));

// 2. Проверить статус
if(escalation.

getStatus() ==EscalationStatus.RESOLVED ||
        escalation.

getStatus() ==EscalationStatus.CANCELLED){
        throw new

IllegalStateException("Escalation is already resolved or cancelled");
}

// 3. Обновить эскалацию
        escalation.

setStatus(EscalationStatus.RESOLVED);
escalation.

setResolvedBy(dto.getResolvedBy());
        escalation.

setResolvedAt(LocalDateTime.now());
        escalation.

setResolution(dto.getResolution());

// 4. Получить связанную рекомендацию
Recommendation recommendation = escalation.getRecommendation();

// 5. Обновить рекомендацию - ОТКЛОНЕНИЕ
recommendation.

setStatus(RecommendationStatus.REJECTED_BY_ANESTHESIOLOGIST);
recommendation.

setAnesthesiologistId(dto.getResolvedBy());
        recommendation.

setAnesthesiologistActionAt(LocalDateTime.now());
        recommendation.

setAnesthesiologistComment(dto.getComment());

// 6. Добавить комментарий
        if(dto.

getComment() !=null&&!dto.

getComment().

isBlank()){
        recommendation.

getComments().

add("Anesthesiologist (REJECTED): "+dto.getComment());
        }

// 7. Сохранить
        recommendationRepository.

save(recommendation);

Escalation savedEscalation = escalationRepository.save(escalation);

// 8. Логирование
log.

info("Escalation rejected: id={}, recommendationId={}, status={}",
     savedEscalation.getId(), 
    recommendation.

getId(), 
    recommendation.

getStatus());
```

**Результат в БД:**

```sql
-- Таблица: escalation (UPDATE)
UPDATE escalation
SET status      = 'RESOLVED',
    resolved_by = 'ANESTH456',
    resolved_at = '2025-10-08 15:00:00',
    resolution  = 'Rejected, needs multidisciplinary consultation',
    updated_at  = '2025-10-08 15:00:00',
    updated_by  = 'ANESTH456'
WHERE escalation_id = 1;

-- Таблица: recommendation (UPDATE)
UPDATE recommendation
SET status                     = 'REJECTED_BY_ANESTHESIOLOGIST',
    anesthesiologist_id        = 'ANESTH456',
    anesthesiologist_action_at = '2025-10-08 15:00:00',
    anesthesiologist_comment   = 'Patient requires different approach, consult pain specialist',
    updated_at                 = '2025-10-08 15:00:00',
    updated_by                 = 'ANESTH456'
WHERE recommendation_id = 1;

-- Таблица: recommendation_comments (INSERT)
INSERT INTO recommendation_comments (recommendation_id, element)
VALUES (1, 'Anesthesiologist (REJECTED): Patient requires different approach, consult pain specialist');
```

**Связанные файлы:**

- **Entity**:
    - `anesthesiologist/entity/Escalation.java` (строки 51-56: resolved fields)
    - `common/patients/entity/Recommendation.java` (строки 61-67: anesthesiologist level)
- **DTO**: `anesthesiologist/dto/EscalationResolutionDTO.java`
- **Service**: `anesthesiologist/service/AnesthesiologistServiceImpl.java` (метод `rejectEscalation()`)
- **Repository**:
    - `anesthesiologist/repository/TreatmentEscalationRepository.java`
    - `common/patients/repository/RecommendationRepository.java`

**Workflow завершен ❌** - эскалация разрешена, рекомендация отклонена.

---

## 📊 Диаграмма статусов Recommendation (RecommendationStatus enum)

```
PENDING (создано медсестрой)
    ↓
    ├─→ APPROVED_BY_DOCTOR → FINAL_APPROVED ✅
    │   (врач одобрил)         (финальное одобрение)
    │
    └─→ REJECTED_BY_DOCTOR → ESCALATED_TO_ANESTHESIOLOGIST
        (врач отклонил)        (автоматическая эскалация)
                ↓
                ├─→ APPROVED_BY_ANESTHESIOLOGIST → FINAL_APPROVED ✅
                │   (анестезиолог одобрил)          (финальное одобрение)
                │
                └─→ REJECTED_BY_ANESTHESIOLOGIST ❌
                    (анестезиолог отклонил)
```

**Файл enum**: `enums/RecommendationStatus.java`

---

## 📊 Диаграмма статусов Escalation (EscalationStatus enum)

```
PENDING (создано при отклонении врача)
    ↓
    ├─→ IN_PROGRESS (анестезиолог взял в работу)
    │       ↓
    │       └─→ RESOLVED (разрешено: одобрено или отклонено)
    │
    ├─→ RESOLVED (разрешено напрямую)
    │
    └─→ CANCELLED (отменено)
```

**Файл enum**: `enums/EscalationStatus.java`

---

## 🗂️ Структура таблиц БД

### Таблица: `nurse_patients`

```sql
CREATE TABLE nurse_patients
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    mrn                     VARCHAR(50) UNIQUE,
    first_name              VARCHAR(50),
    last_name               VARCHAR(50),
    date_of_birth           DATE,
    gender                  VARCHAR(10),
    phone_number            VARCHAR(20),
    email                   VARCHAR(100),
    insurance_policy_number VARCHAR(50),
    is_active               BOOLEAN DEFAULT true,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50)
);
```

### Таблица: `emr`

```sql
CREATE TABLE emr
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id       BIGINT,
    gfr              VARCHAR(50),
    child_pugh_score VARCHAR(10),
    plt DOUBLE,
    wbc DOUBLE,
    sodium DOUBLE,
    sat DOUBLE,
    height DOUBLE,
    weight DOUBLE,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES nurse_patients (id)
);
```

### Таблица: `vas`

```sql
CREATE TABLE vas
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id  BIGINT,
    pain_level  INT,
    location    VARCHAR(255),
    description TEXT,
    recorded_at TIMESTAMP,
    created_at  TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES nurse_patients (id)
);
```

### Таблица: `recommendation`

```sql
CREATE TABLE recommendation
(
    recommendation_id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id                 BIGINT,
    regimen_hierarchy          INT,
    status                     VARCHAR(50),
    rejected_reason            VARCHAR(500),

    -- Doctor level
    doctor_id                  VARCHAR(50),
    doctor_action_at           TIMESTAMP,
    doctor_comment             VARCHAR(1000),

    -- Anesthesiologist level
    anesthesiologist_id        VARCHAR(50),
    anesthesiologist_action_at TIMESTAMP,
    anesthesiologist_comment   VARCHAR(1000),

    -- Final approval
    final_approved_by          VARCHAR(50),
    final_approval_at          TIMESTAMP,

    -- Audit
    created_at                 TIMESTAMP,
    updated_at                 TIMESTAMP,
    created_by                 VARCHAR(50),
    updated_by                 VARCHAR(50),

    FOREIGN KEY (patient_id) REFERENCES nurse_patients (id)
);
```

### Таблица: `escalation`

```sql
CREATE TABLE escalation
(
    escalation_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    recommendation_id BIGINT UNIQUE,
    escalated_by      VARCHAR(50),
    escalated_at      TIMESTAMP,
    escalation_reason VARCHAR(1000),
    description       VARCHAR(2000),
    priority          VARCHAR(20),
    status            VARCHAR(20),
    resolved_by       VARCHAR(50),
    resolved_at       TIMESTAMP,
    resolution        VARCHAR(2000),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50),

    FOREIGN KEY (recommendation_id) REFERENCES recommendation (recommendation_id)
);
```

### Таблица: `drug_recommendation`

```sql
CREATE TABLE drug_recommendation
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    recommendation_id BIGINT,
    drug_name         VARCHAR(255),
    dosage            VARCHAR(100),
    route             VARCHAR(50),
    frequency         VARCHAR(100),
    is_alternative    BOOLEAN DEFAULT false,

    FOREIGN KEY (recommendation_id) REFERENCES recommendation (recommendation_id)
);
```

### Таблица: `recommendation_comments`

```sql
CREATE TABLE recommendation_comments
(
    recommendation_id BIGINT,
    element           VARCHAR(2000),

    FOREIGN KEY (recommendation_id) REFERENCES recommendation (recommendation_id)
);
```

---

## 📦 Все используемые DTO

### Nurse модуль:

- **`PatientDTO`** - данные пациента
- **`EmrDTO`** - медицинская карта
- **`VasInputDTO`** - ввод жалобы на боль
- **`RecommendationDTO`** - ответ с рекомендацией

### Doctor модуль:

- **`RecommendationApprovalRejectionDTO`** - одобрение/отклонение рекомендации
  ```java
  {
    "doctorId": String,
    "comment": String,
    "rejectedReason": String  // только для reject
  }
  ```
- **`RecommendationWithVasDTO`** - рекомендация с данными VAS

### Anesthesiologist модуль:

- **`EscalationResponseDTO`** - данные эскалации
  ```java
  {
    "id": Long,
    "recommendationId": Long,
    "patientName": String,
    "patientMrn": String,
    "escalatedBy": String,
    "escalatedAt": LocalDateTime,
    "escalationReason": String,
    "priority": EscalationPriority,
    "status": EscalationStatus,
    "vasLevel": Integer
  }
  ```
- **`EscalationResolutionDTO`** - разрешение эскалации
  ```java
  {
    "resolvedBy": String,
    "comment": String,
    "resolution": String
  }
  ```
- **`EscalationStatsDTO`** - статистика эскалаций
  ```java
  {
    "total": Long,
    "pending": Long,
    "inProgress": Long,
    "resolved": Long,
    "high": Long,
    "medium": Long,
    "low": Long
  }
  ```

---

## 🗄️ Используемые репозитории

### Common репозитории (используются всеми модулями):

- **`PatientRepository`** (`common/patients/repository`)
    - `findById(Long id)`
    - `findByMrn(String mrn)`
    - `save(Patient patient)`

- **`RecommendationRepository`** (`common/patients/repository`)
    - `findById(Long id)`
    - `findByStatus(RecommendationStatus status)`
    - `findByPatientId(Long patientId)`
    - `save(Recommendation recommendation)`

- **`VasRepository`** (`common/patients/repository`)
    - `findLatestByPatientId(Long patientId)`
    - `save(Vas vas)`

- **`EmrRepository`** (`common/patients/repository`)
    - `findByPatientId(Long patientId)`
    - `save(Emr emr)`

### Anesthesiologist репозитории:

- **`TreatmentEscalationRepository`** (`anesthesiologist/repository`)
    - `findById(Long id)`
    - `findByStatus(EscalationStatus status)`
    - `findByPriority(EscalationPriority priority)`
    - `findActiveEscalationsOrderedByPriorityAndDate()`
    - `findCriticalActiveEscalations()`
    - `findByEscalatedBy(String escalatedBy)`
    - `findByResolvedBy(String resolvedBy)`
    - `findByRecommendationId(Long recommendationId)`
    - `countByStatus(EscalationStatus status)`
    - `countByPriority(EscalationPriority priority)`
    - `save(Escalation escalation)`

---

## ❌ УСТАРЕВШИЕ СУЩНОСТИ (МОЖНО УДАЛИТЬ)

### 1. `anesthesiologist/entity/Recommendation.java` (22 строки)

**Причина удаления:**

- Примитивная заглушка с 6 полями
- Не используется нигде в коде
- Дублирует `common/patients/entity/Recommendation.java` (110 строк)
- Создает отдельную таблицу `anesthesiologist_recommendation` в БД
- Вся работа с рекомендациями идет через `common/patients/entity/Recommendation`

**Проверка использования:**

```bash
# Поиск импортов
grep -r "import.*anesthesiologist.entity.Recommendation" src/
# Результат: НЕТ ИСПОЛЬЗОВАНИЙ
```

**Решение:** ✅ УДАЛИТЬ

---

### 2. `anesthesiologist/entity/Approval.java` (20 строк)

**Причина удаления:**

- Не используется нигде в коде
- Нет репозитория (`ApprovalRepository` не существует)
- Нет сервиса, использующего эту Entity
- Нет контроллера
- Упоминается только в названии DTO `RecommendationApprovalRejectionDTO`, но это просто название (не импортируется)

**Проверка использования:**

```bash
# Поиск импортов Approval
grep -r "import.*Approval" src/
# Результат: 
# - doctor/dto/RecommendationApprovalRejectionDTO.java (только в названии)
# - НЕТ РЕАЛЬНЫХ ИСПОЛЬЗОВАНИЙ Entity
```

**Решение:** ✅ УДАЛИТЬ

---

## ✅ ИТОГОВАЯ АРХИТЕКТУРА

### Единая таблица рекомендаций:

- **`common/patients/entity/Recommendation`** - используется всеми модулями (Nurse, Doctor, Anesthesiologist)
- **`anesthesiologist/entity/Escalation`** - связана с Recommendation через @OneToOne

### Преимущества:

1. **Нет дублирования данных** - одна рекомендация = одна запись в БД
2. **Единый источник правды** (Single Source of Truth)
3. **Простота поддержки** - изменения в одном месте
4. **Консистентность данных** - невозможны расхождения между модулями
5. **Полный audit trail** - все действия записываются в одну Entity

### Связи между таблицами:

```
nurse_patients (1) ─────→ (N) recommendation
                │
                ├─────→ (N) vas
                │
                └─────→ (1) emr

recommendation (1) ─────→ (1) escalation
                │
                └─────→ (N) drug_recommendation
                │
                └─────→ (N) recommendation_comments
```

---

## 🔍 Как найти данные в БД

### Найти рекомендацию по ID:

```sql
SELECT *
FROM recommendation
WHERE recommendation_id = 1;
```

### Найти все рекомендации пациента:

```sql
SELECT r.*, p.first_name, p.last_name, p.mrn
FROM recommendation r
         JOIN nurse_patients p ON r.patient_id = p.id
WHERE p.mrn = 'MRN-1';
```

### Найти эскалацию по рекомендации:

```sql
SELECT e.*, r.status as recommendation_status
FROM escalation e
         JOIN recommendation r ON e.recommendation_id = r.recommendation_id
WHERE r.recommendation_id = 1;
```

### Найти все активные эскалации с HIGH priority:

```sql
SELECT e.*, p.first_name, p.last_name, p.mrn, r.status
FROM escalation e
         JOIN recommendation r ON e.recommendation_id = r.recommendation_id
         JOIN nurse_patients p ON r.patient_id = p.id
WHERE e.status = 'PENDING'
  AND e.priority = 'HIGH'
ORDER BY e.created_at ASC;
```

### Найти историю рекомендации (все комментарии):

```sql
SELECT rc.element as comment, r.status
FROM recommendation_comments rc
         JOIN recommendation r ON rc.recommendation_id = r.recommendation_id
WHERE r.recommendation_id = 1
ORDER BY r.updated_at ASC;
```

---

## 📝 Примеры полного workflow в БД

### Пример 1: Успешное одобрение врачом

**Начальное состояние:**

```sql
-- recommendation
id
=1, status='PENDING', doctor_id=NULL, anesthesiologist_id=NULL

-- escalation
(нет записи)
```

**После одобрения врача:**

```sql
-- recommendation
id
=1, status='FINAL_APPROVED', 
doctor_id='DOC123', doctor_action_at='2025-10-08 14:30:00',
final_approved_by='DOC123', final_approval_at='2025-10-08 14:30:00'

-- escalation
(нет записи)
```

---

### Пример 2: Отклонение врача → Одобрение анестезиолога

**Начальное состояние:**

```sql
-- recommendation
id
=1, status='PENDING', doctor_id=NULL, anesthesiologist_id=NULL

-- escalation
(нет записи)
```

**После отклонения врача:**

```sql
-- recommendation
id
=1, status='ESCALATED_TO_ANESTHESIOLOGIST',
doctor_id='DOC123', doctor_action_at='2025-10-08 14:30:00',
rejected_reason='Allergy to NSAIDs'

-- escalation
id=1, recommendation_id=1, escalated_by='DOC123',
priority='HIGH', status='PENDING'
```

**После одобрения анестезиолога:**

```sql
-- recommendation
id
=1, status='FINAL_APPROVED',
doctor_id='DOC123',
anesthesiologist_id='ANESTH456', anesthesiologist_action_at='2025-10-08 15:00:00',
final_approved_by='ANESTH456', final_approval_at='2025-10-08 15:00:00'

-- escalation
id=1, status='RESOLVED', resolved_by='ANESTH456', resolved_at='2025-10-08 15:00:00'
```

---

## 🎯 Ключевые точки интеграции

### 1. Nurse → Doctor

- **Связь**: `Recommendation.status = PENDING`
- **Endpoint**: `GET /api/doctor/recommendations/pending`
- **DTO**: `RecommendationWithVasDTO`

### 2. Doctor → Anesthesiologist (при отклонении)

- **Связь**: `Escalation.recommendation` (OneToOne)
- **Триггер**: `DoctorService.rejectRecommendation()` создает `Escalation`
- **Endpoint**: `GET /api/anesthesiologist/escalations/active`
- **DTO**: `EscalationResponseDTO`

### 3. Anesthesiologist → Recommendation (при разрешении)

- **Связь**: `Escalation.recommendation` (обратная связь)
- **Обновление**: `AnesthesiologistService.approveEscalation()` обновляет `Recommendation.status`
- **Финальный статус**: `FINAL_APPROVED` или `REJECTED_BY_ANESTHESIOLOGIST`

---

## 📚 Документация файлов

### Entity файлы:

- `common/patients/entity/Patient.java` - пациент
- `common/patients/entity/Vas.java` - жалоба на боль
- `common/patients/entity/Emr.java` - медицинская карта
- `common/patients/entity/Recommendation.java` - **ЦЕНТРАЛЬНАЯ СУЩНОСТЬ** (110 строк)
- `anesthesiologist/entity/Escalation.java` - эскалация (94 строки)

### Service файлы:

- `nurse/service/NurseServiceImpl.java` - создание пациента, VAS, EMR
- `treatment_protocol/service/TreatmentProtocolService.java` - генерация рекомендаций
- `doctor/service/DoctorServiceImpl.java` - одобрение/отклонение рекомендаций
- `anesthesiologist/service/AnesthesiologistServiceImpl.java` - разрешение эскалаций

### Repository файлы:

- `common/patients/repository/PatientRepository.java`
- `common/patients/repository/VasRepository.java`
- `common/patients/repository/EmrRepository.java`
- `common/patients/repository/RecommendationRepository.java`
- `anesthesiologist/repository/TreatmentEscalationRepository.java`

### Controller файлы:

- `nurse/controller/NurseController.java`
- `doctor/controller/DoctorController.java`
- `anesthesiologist/controller/AnesthesiologistController.java`

---

## 🔧 Технические детали

### Транзакционность:

- Все методы изменения данных помечены `@Transactional`
- Read-only методы помечены `@Transactional(readOnly = true)`
- Cascade операции: `Recommendation` → `Escalation` (при сохранении рекомендации сохраняется эскалация)

### Маппинг Entity ↔ DTO:

- Используется `ModelMapper` для автоматического маппинга
- Пример: `modelMapper.map(escalation, EscalationResponseDTO.class)`

### Логирование:

- Все критические операции логируются через `@Slf4j`
- Формат: `log.info("Operation: details={}", details)`

### Валидация:

- Входные DTO валидируются через `@Valid` в контроллерах
- Используются аннотации: `@NotBlank`, `@NotNull`, `@Size`, `@Past`, `@Positive`

---

**Конец документации**
