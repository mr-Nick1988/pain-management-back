# 🧪 ПОЛНЫЙ ГАЙД ПО ТЕСТИРОВАНИЮ EMR INTEGRATION MODULE

## Дата: 19.10.2025 (обновлено)
## Автор: Игорь Негода

---

# 📑 СОДЕРЖАНИЕ

1. [Подготовка к тестированию](#подготовка-к-тестированию)
2. [Тестирование через Postman](#тестирование-через-postman)
3. [Тестирование синхронизации EMR](#тестирование-синхронизации-emr)
4. [Тестирование WebSocket уведомлений](#тестирование-websocket-уведомлений)
5. [Тестирование моковых пациентов с Protocol ICD кодами](#тестирование-моковых-пациентов-с-protocol-icd-кодами)
6. [Тестирование через curl](#тестирование-через-curl)
7. [Проверка базы данных](#проверка-базы-данных)
8. [Типичные ошибки и решения](#типичные-ошибки-и-решения)

---

# 🚀 ПОДГОТОВКА К ТЕСТИРОВАНИЮ

## Шаг 1: Запустить приложение

```bash
# В корне проекта
mvn spring-boot:run

# Или через IDE (IntelliJ IDEA / Eclipse)
# Run -> PainHelperBackApplication
```

## Шаг 2: Проверить, что приложение запустилось

```bash
# Должно быть доступно на порту 8080
curl http://localhost:8080/actuator/health
```

**Ожидаемый ответ:**
```json
{
  "status": "UP"
}
```

## Шаг 3: Установить Postman (если еще не установлен)

- Скачать: https://www.postman.com/downloads/
- Или использовать curl из командной строки

---

# 📬 ТЕСТИРОВАНИЕ ЧЕРЕЗ POSTMAN

## BASE URL
```
http://localhost:8080/api/emr
```

---

## ✅ ENDPOINT 1: Генерация 1 мокового пациента

### Зачем тестировать:
Это **САМЫЙ ПРОСТОЙ** endpoint для начала. Не требует FHIR сервера, генерирует тестовые данные.

### Request:
```
POST http://localhost:8080/api/emr/mock/generate?createdBy=igor_test
```

### Headers:
```
Content-Type: application/json
```

### Body:
```
Нет (пустой)
```

### Ожидаемый Response (200 OK):
```json
{
  "success": true,
  "message": "Mock patient generated and imported successfully",
  "externalPatientIdInFhirResource": "mock-patient-abc123",
  "internalPatientId": 1,
  "matchConfidence": "NO_MATCH",
  "newPatientCreated": true,
  "sourceType": "MOCK_GENERATOR",
  "observationsImported": 6,
  "warnings": [],
  "errors": [],
  "requiresManualReview": false,
  "reviewNotes": null
}
```
 
### ✅ Что проверить:
- [x] `success: true`
- [x] `internalPatientId` - должен быть числом (1, 2, 3...)
- [x] `newPatientCreated: true`
- [x] `sourceType: "MOCK_GENERATOR"`
- [x] `observationsImported: 6` - импортировано 6 лабораторных показателей

### 🔍 Проверка в БД:
```sql
-- Проверяем, что создался пациент
SELECT * FROM nurse_patients ORDER BY id DESC LIMIT 1;

-- Проверяем, что создалась медицинская карта
SELECT * FROM emr ORDER BY id DESC LIMIT 1;

-- Проверяем, что создался маппинг
SELECT * FROM emr_mappings ORDER BY id DESC LIMIT 1;
```

---

## ✅ ENDPOINT 2: Генерация batch моковых пациентов

### Зачем тестировать:
Проверяет **производительность** и создание **нескольких пациентов** за раз.

### Request:
```
POST http://localhost:8080/api/emr/mock/generate-batch?count=10&createdBy=igor_test
```

### Headers:
```
Content-Type: application/json
```

### Body:
```
Нет (пустой)
```

### Ожидаемый Response (201 CREATED):
```json
[
  {
    "success": true,
    "message": "Mock patient imported",
    "externalPatientIdInFhirResource": "mock-patient-1",
    "internalPatientId": 2,
    "newPatientCreated": true,
    "sourceType": "MOCK_GENERATOR",
    "observationsImported": 6
  },
  {
    "success": true,
    "message": "Mock patient imported",
    "externalPatientIdInFhirResource": "mock-patient-2",
    "internalPatientId": 3,
    "newPatientCreated": true,
    "sourceType": "MOCK_GENERATOR",
    "observationsImported": 6
  }
  // ... еще 8 пациентов
]
```

### ✅ Что проверить:
- [x] Массив из 10 элементов
- [x] Все `success: true`
- [x] `internalPatientId` увеличивается (2, 3, 4, 5...)
- [x] Все `sourceType: "MOCK_GENERATOR"`

### 🔍 Проверка в БД:
```sql
-- Должно быть 10 новых пациентов
SELECT COUNT(*) FROM nurse_patients;

-- Должно быть 10 новых медицинских карт
SELECT COUNT(*) FROM emr;

-- Должно быть 10 новых маппингов
SELECT COUNT(*) FROM emr_mappings WHERE source_type = 'MOCK_GENERATOR';
```

### 🎯 Тест производительности:
```
POST http://localhost:8080/api/emr/mock/generate-batch?count=50&createdBy=performance_test
```

**Ожидаемое время:** 2-5 секунд для 50 пациентов

---

## ✅ ENDPOINT 3: Проверка, импортирован ли пациент

### Зачем тестировать:
Проверяет, что система **не создает дубликаты**.

### Request:
```
GET http://localhost:8080/api/emr/check-import/mock-patient-abc123
```

### Headers:
```
Нет
```

### Ожидаемый Response (200 OK):
```json
{
  "alreadyImported": true,
  "internalEmrNumber": "EMR-A1B2C3D4"
}
```

### ✅ Что проверить:
- [x] `alreadyImported: true` - если пациент уже импортирован
- [x] `internalEmrNumber` - должен быть формата "EMR-XXXXXXXX"

### Тест с несуществующим пациентом:
```
GET http://localhost:8080/api/emr/check-import/Patient/99999999
```

**Ожидаемый Response:**
```json
{
  "alreadyImported": false,
  "internalEmrNumber": null
}
```

---

## ✅ ENDPOINT 4: Конвертация FHIR Observations в EmrDTO

### Зачем тестировать:
Проверяет **расчет GFR** из креатинина и конвертацию медицинских данных.

### Request:
```
POST http://localhost:8080/api/emr/convert-observations?createdBy=igor_test
```

### Headers:
```
Content-Type: application/json
```

### Body:
```json
[
  {
    "observationId": "Observation/1",
    "loincCode": "2160-0",
    "value": 1.2,
    "unit": "mg/dL",
    "effectiveDateTime": "2025-10-04T10:00:00"
  },
  {
    "observationId": "Observation/2",
    "loincCode": "777-3",
    "value": 180.0,
    "unit": "10*3/uL",
    "effectiveDateTime": "2025-10-04T10:00:00"
  },
  {
    "observationId": "Observation/3",
    "loincCode": "6690-2",
    "value": 8.5,
    "unit": "10*3/uL",
    "effectiveDateTime": "2025-10-04T10:00:00"
  },
  {
    "observationId": "Observation/4",
    "loincCode": "2951-2",
    "value": 138.0,
    "unit": "mmol/L",
    "effectiveDateTime": "2025-10-04T10:00:00"
  },
  {
    "observationId": "Observation/5",
    "loincCode": "59408-5",
    "value": 97.0,
    "unit": "%",
    "effectiveDateTime": "2025-10-04T10:00:00"
  }
]
```

### Ожидаемый Response (200 OK):
```json
{
  "gfr": "≥90 (Normal)",
  "plt": 180.0,
  "wbc": 8.5,
  "sodium": 138.0,
  "sat": 97.0,
  "childPughScore": "N/A",
  "height": null,
  "weight": null,
  "createdBy": "igor_test",
  "createdAt": "2025-10-04T17:00:00"
}
```

### ✅ Что проверить:
- [x] `gfr` - должен быть рассчитан из креатинина (1.2 → GFR ≈ 83 → "60-89 (Mild decrease)")
  - **ВАЖНО:** GFR = 100 / 1.2 ≈ 83.3 → категория "60-89 (Mild decrease)"
- [x] `plt: 180.0` - тромбоциты
- [x] `wbc: 8.5` - лейкоциты
- [x] `sodium: 138.0` - натрий
- [x] `sat: 97.0` - сатурация

### 🧪 Тест с разными значениями креатинина:

#### Тест 1: Нормальная функция почек (креатинин = 0.9)
```json
[
  {
    "loincCode": "2160-0",
    "value": 0.9
  }
]
```
**Ожидаемый GFR:** "≥90 (Normal)" (100 / 0.9 ≈ 111)

#### Тест 2: Умеренное снижение (креатинин = 1.5)
```json
[
  {
    "loincCode": "2160-0",
    "value": 1.5
  }
]
```
**Ожидаемый GFR:** "60-89 (Mild decrease)" (100 / 1.5 ≈ 67)

#### Тест 3: Значительное снижение (креатинин = 2.5)
```json
[
  {
    "loincCode": "2160-0",
    "value": 2.5
  }
]
```
**Ожидаемый GFR:** "30-59 (Moderate decrease)" (100 / 2.5 = 40)

#### Тест 4: Почечная недостаточность (креатинин = 8.0)
```json
[
  {
    "loincCode": "2160-0",
    "value": 8.0
  }
]
```
**Ожидаемый GFR:** "<15 (Kidney failure)" (100 / 8.0 = 12.5)

---

## ✅ ENDPOINT 5: Импорт пациента из FHIR сервера

### ⚠️ ВАЖНО:
Этот endpoint требует **реальный FHIR сервер**. Если FHIR сервер не настроен, endpoint вернет ошибку.

### Вариант 1: Тестирование с реальным FHIR сервером

#### Request:
```
POST http://localhost:8080/api/emr/import/Patient/12345?importedBy=igor_test
```

#### Ожидаемый Response (200 OK):
```json
{
  "success": true,
  "message": "Patient imported successfully from FHIR server",
  "externalPatientIdInFhirResource": "Patient/12345",
  "internalPatientId": 51,
  "matchConfidence": "NO_MATCH",
  "newPatientCreated": true,
  "sourceType": "FHIR_SERVER",
  "observationsImported": 6,
  "warnings": [],
  "errors": []
}
```

### Вариант 2: Тестирование с HAPI FHIR Test Server

#### Настройка в application.properties:
```properties
fhir.server.url=http://hapi.fhir.org/baseR4
fhir.server.connection-timeout=30000
fhir.server.socket-timeout=30000
```

#### Request:
```
POST http://localhost:8080/api/emr/import/Patient/1234567?importedBy=igor_test
```

### Вариант 3: Если FHIR сервер недоступен

**Ожидаемый Response (500 INTERNAL_SERVER_ERROR):**
```json
{
  "success": false,
  "message": "Failed to import patient from FHIR server",
  "externalPatientIdInFhirResource": "Patient/12345",
  "internalPatientId": null,
  "errors": [
    "FHIR server error: Connection refused"
  ],
  "requiresManualReview": true,
  "reviewNotes": "Check FHIR server connectivity and patient ID validity"
}
```

### ✅ Что проверить:
- [x] Если FHIR сервер доступен: `success: true`, `sourceType: "FHIR_SERVER"`
- [x] Если FHIR сервер недоступен: `success: false`, `errors` содержит описание ошибки
- [x] `requiresManualReview: true` - требуется ручная проверка

---

## ✅ ENDPOINT 6: Поиск пациентов в FHIR системе

### ⚠️ ВАЖНО:
Требует **реальный FHIR сервер**.

### Request:
```
GET http://localhost:8080/api/emr/search?firstName=John&lastName=Smith&birthDate=1980-01-15
```

### Ожидаемый Response (200 OK):
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
    "address": "123 Main St, New York, NY",
    "sourceSystemUrl": "http://hapi.fhir.org/baseR4",
    "identifiers": [
      {
        "type": "MRN",
        "system": "http://hospital.org/mrn",
        "value": "MRN-12345",
        "use": "official"
      }
    ]
  }
]
```

### ✅ Что проверить:
- [x] Массив пациентов (может быть пустым, если не найдено)
- [x] Каждый пациент имеет `patientIdInFhirResource`
- [x] `sourceSystemUrl` - URL FHIR сервера

---

## ✅ ENDPOINT 7: Получение лабораторных анализов

### ⚠️ ВАЖНО:
Требует **реальный FHIR сервер**.

### Request:
```
GET http://localhost:8080/api/emr/observations/Patient/12345
```

### Ожидаемый Response (200 OK):
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

### ✅ Что проверить:
- [x] Массив лабораторных показателей
- [x] Каждый показатель имеет `loincCode` (стандартный медицинский код)
- [x] `value` - числовое значение
- [x] `unit` - единица измерения

---

# 🔄 ТЕСТИРОВАНИЕ СИНХРОНИЗАЦИИ EMR

## Обзор функционала синхронизации

**НОВЫЙ ФУНКЦИОНАЛ (18.10.2025):**
- Автоматическая синхронизация EMR данных из FHIR серверов каждые 6 часов
- Ручная синхронизация всех пациентов
- Ручная синхронизация конкретного пациента
- Обнаружение критических изменений (GFR, PLT, WBC, SAT)
- Real-time уведомления через WebSocket
- Email уведомления (опционально)

---

## ✅ ENDPOINT 8: Ручная синхронизация всех FHIR пациентов

### Зачем тестировать:
Проверяет **массовую синхронизацию** всех пациентов, импортированных из FHIR серверов. Обновляет лабораторные показатели и генерирует алерты при критических изменениях.

### Request:
```
POST http://localhost:8080/api/emr/sync/all
```

### Headers:
```
Content-Type: application/json
```

### Body:
```
Нет (пустой)
```

### Ожидаемый Response (200 OK):
```json
{
  "syncStartTime": "2025-10-18T15:30:00",
  "syncEndTime": "2025-10-18T15:30:05",
  "durationMs": 5234,
  "totalPatientsProcessed": 10,
  "successfulSyncs": 8,
  "failedSyncs": 0,
  "patientsWithChanges": 3,
  "criticalAlertsGenerated": 2,
  "syncedPatientMrns": [
    "EMR-A1B2C3D4",
    "EMR-E5F6G7H8"
  ],
  "failedPatientMrns": [],
  "alerts": [
    {
      "patientMrn": "EMR-A1B2C3D4",
      "parameterName": "GFR",
      "oldValue": "45",
      "newValue": "28",
      "severity": "CRITICAL",
      "changeDescription": "Функция почек критически снизилась",
      "detectedAt": "2025-10-18T15:30:02",
      "requiresRecommendationReview": true
    },
    {
      "patientMrn": "EMR-E5F6G7H8",
      "parameterName": "PLT",
      "oldValue": "80.0",
      "newValue": "45.0",
      "severity": "CRITICAL",
      "changeDescription": "Критически низкие тромбоциты - риск кровотечения",
      "detectedAt": "2025-10-18T15:30:03",
      "requiresRecommendationReview": true
    }
  ],
  "errorMessages": [],
  "status": "SUCCESS",
  "message": "All patients synced successfully"
}
```

### ✅ Что проверить:
- [x] `status: "SUCCESS"` - синхронизация прошла успешно
- [x] `totalPatientsProcessed` - количество обработанных пациентов
- [x] `successfulSyncs` - количество успешных синхронизаций
- [x] `patientsWithChanges` - у скольких пациентов обнаружены изменения
- [x] `criticalAlertsGenerated` - количество критических алертов
- [x] `alerts` - массив критических изменений с деталями
- [x] `durationMs` - время выполнения синхронизации

### 🔍 Проверка в БД после синхронизации:
```sql
-- Проверить обновленные EMR записи
SELECT 
    e.patient_id,
    p.mrn,
    e.gfr,
    e.plt,
    e.wbc,
    e.sat,
    e.updated_at,
    e.updated_by
FROM emr e
JOIN nurse_patients p ON e.patient_id = p.id
WHERE e.updated_by = 'EMR_SYNC_SCHEDULER'
ORDER BY e.updated_at DESC;
```

### 📊 Варианты статусов синхронизации:

#### Статус: SUCCESS
```json
{
  "status": "SUCCESS",
  "message": "All patients synced successfully",
  "failedSyncs": 0
}
```

#### Статус: PARTIAL_SUCCESS
```json
{
  "status": "PARTIAL_SUCCESS",
  "message": "Partial success: 8 succeeded, 2 failed",
  "successfulSyncs": 8,
  "failedSyncs": 2,
  "errorMessages": [
    "Failed to sync patient: fhirId=Patient/123, error=Connection timeout"
  ]
}
```

#### Статус: FAILED
```json
{
  "status": "FAILED",
  "message": "All sync attempts failed",
  "successfulSyncs": 0,
  "failedSyncs": 10
}
```

---

## ✅ ENDPOINT 9: Синхронизация конкретного пациента

### Зачем тестировать:
Проверяет **точечную синхронизацию** одного пациента. Используется перед генерацией новой рекомендации или после получения информации об изменении состояния.

### Request:
```
POST http://localhost:8080/api/emr/sync/patient/EMR-A1B2C3D4
```

### Headers:
```
Content-Type: application/json
```

### Body:
```
Нет (пустой)
```

### Ожидаемый Response (200 OK):

#### Вариант 1: Обнаружены изменения
```json
{
  "message": "Patient synced successfully. Changes detected.",
  "hasChanges": true
}
```

#### Вариант 2: Изменений нет
```json
{
  "message": "Patient synced successfully. No changes detected.",
  "hasChanges": false
}
```

### ✅ Что проверить:
- [x] `hasChanges: true` - если данные пациента изменились
- [x] `hasChanges: false` - если данные остались прежними
- [x] HTTP статус 200 - синхронизация прошла успешно

### 🔍 Проверка в БД:
```sql
-- Проверить, обновился ли конкретный пациент
SELECT 
    p.mrn,
    p.first_name,
    p.last_name,
    e.gfr,
    e.plt,
    e.wbc,
    e.sat,
    e.updated_at,
    e.updated_by
FROM emr e
JOIN nurse_patients p ON e.patient_id = p.id
WHERE p.mrn = 'EMR-A1B2C3D4';
```

### ⚠️ Возможные ошибки:

#### Ошибка 1: Пациент не найден
```json
{
  "timestamp": "2025-10-18T15:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Patient not found or not imported from FHIR: EMR-XXXXXXXX"
}
```
**Решение:** Проверьте, что пациент был импортирован из FHIR сервера (не мок).

#### Ошибка 2: FHIR сервер недоступен
```json
{
  "timestamp": "2025-10-18T15:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to fetch FHIR observations: Connection refused"
}
```
**Решение:** Проверьте доступность FHIR сервера.

---

## 🧪 ТЕСТИРОВАНИЕ ОБНАРУЖЕНИЯ КРИТИЧЕСКИХ ИЗМЕНЕНИЙ

### Сценарий 1: Критическое падение GFR

**Шаг 1:** Импортируйте пациента с нормальным GFR
```bash
POST http://localhost:8080/api/emr/mock/generate?createdBy=test_user
```

**Шаг 2:** Вручную измените GFR в БД на критическое значение
```sql
-- Симуляция критического падения GFR
UPDATE emr 
SET gfr = '25'  -- Было 60, стало 25 (критично!)
WHERE patient_id = (SELECT id FROM nurse_patients WHERE mrn = 'EMR-A1B2C3D4');
```

**Шаг 3:** Запустите синхронизацию
```bash
POST http://localhost:8080/api/emr/sync/patient/EMR-A1B2C3D4
```

**Ожидаемый результат:** 
- Должен быть сгенерирован алерт с `severity: "CRITICAL"`
- `parameterName: "GFR"`
- `changeDescription: "Функция почек критически снизилась"`

---

### Сценарий 2: Критически низкие тромбоциты (PLT < 50)

**Шаг 1:** Создайте пациента
```bash
POST http://localhost:8080/api/emr/mock/generate?createdBy=test_user
```

**Шаг 2:** Симулируйте падение тромбоцитов
```sql
UPDATE emr 
SET plt = 40.0  -- Критически низко!
WHERE patient_id = (SELECT id FROM nurse_patients WHERE mrn = 'EMR-A1B2C3D4');
```

**Шаг 3:** Синхронизация
```bash
POST http://localhost:8080/api/emr/sync/all
```

**Ожидаемый алерт:**
```json
{
  "parameterName": "PLT",
  "oldValue": "200.0",
  "newValue": "40.0",
  "severity": "CRITICAL",
  "changeDescription": "Критически низкие тромбоциты - риск кровотечения",
  "requiresRecommendationReview": true
}
```

---

### Сценарий 3: Множественные критические изменения

**Симуляция:** Пациент с одновременным ухудшением нескольких показателей

```sql
UPDATE emr 
SET 
    gfr = '20',      -- Критично!
    plt = 45.0,      -- Критично!
    wbc = 1.5,       -- Критично!
    sat = 85.0       -- Критично!
WHERE patient_id = (SELECT id FROM nurse_patients WHERE mrn = 'EMR-A1B2C3D4');
```

**Запуск синхронизации:**
```bash
POST http://localhost:8080/api/emr/sync/patient/EMR-A1B2C3D4
```

**Ожидаемый результат:** 4 критических алерта (GFR, PLT, WBC, SAT)

---

## 📊 КРИТИЧЕСКИЕ ПОРОГИ

| Параметр | Критический порог | Описание |
|----------|------------------|----------|
| **GFR** | < 30 или падение > 20 единиц | Тяжелая почечная недостаточность |
| **PLT** | < 50 × 10³/µL | Критическая тромбоцитопения, риск кровотечения |
| **WBC** | < 2.0 × 10³/µL | Тяжелая лейкопения, иммунодефицит |
| **SAT** | < 90% | Критическая гипоксия |
| **Натрий** | < 125 или > 155 mmol/L | Опасный электролитный дисбаланс |

---

# 🔔 ТЕСТИРОВАНИЕ WEBSOCKET УВЕДОМЛЕНИЙ

## Обзор WebSocket функционала

**ЗАЧЕМ:**
- Real-time уведомления врачей о критических изменениях
- Мгновенная доставка алертов без обновления страницы
- Поддержка множественных подписчиков

**АРХИТЕКТУРА:**
- **Endpoint:** `ws://localhost:8080/ws-emr-alerts`
- **Topic:** `/topic/emr-critical-alerts`
- **Protocol:** STOMP over WebSocket

---

## 🧪 ТЕСТИРОВАНИЕ ЧЕРЕЗ БРАУЗЕР (JavaScript)

### Вариант 1: Тестирование через консоль браузера

**Шаг 1:** Откройте консоль браузера (F12) на любой странице

**Шаг 2:** Подключитесь к WebSocket:

```javascript
// Подключение к WebSocket
const socket = new WebSocket('ws://localhost:8080/ws-emr-alerts');

socket.onopen = function(event) {
    console.log('✅ WebSocket подключен!');
    
    // Подписка на топик критических алертов
    const subscribeMessage = {
        command: 'SUBSCRIBE',
        destination: '/topic/emr-critical-alerts',
        id: 'sub-0'
    };
    
    socket.send(JSON.stringify(subscribeMessage));
};

socket.onmessage = function(event) {
    console.log('📨 Получено сообщение:', event.data);
    const alert = JSON.parse(event.data);
    console.log('🚨 КРИТИЧЕСКИЙ АЛЕРТ:', alert);
};

socket.onerror = function(error) {
    console.error('❌ Ошибка WebSocket:', error);
};

socket.onclose = function(event) {
    console.log('🔌 WebSocket отключен');
};
```

**Шаг 3:** Запустите синхронизацию с критическими изменениями:

```bash
# В другой вкладке или через Postman
POST http://localhost:8080/api/emr/sync/all
```

**Ожидаемый результат в консоли:**
```
✅ WebSocket подключен!
📨 Получено сообщение: {"patientMrn":"EMR-A1B2C3D4","parameterName":"GFR",...}
🚨 КРИТИЧЕСКИЙ АЛЕРТ: {
  patientMrn: "EMR-A1B2C3D4",
  parameterName: "GFR",
  oldValue: "45",
  newValue: "28",
  severity: "CRITICAL",
  changeDescription: "Функция почек критически снизилась",
  detectedAt: "2025-10-18T15:30:02",
  requiresRecommendationReview: true
}
```

---

### Вариант 2: Тестирование через HTML страницу

Создайте файл `websocket-test.html`:

```html
<!DOCTYPE html>
<html>
<head>
    <title>EMR WebSocket Test</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
    <style>
        body { font-family: Arial; padding: 20px; }
        .alert { 
            padding: 15px; 
            margin: 10px 0; 
            border-radius: 5px;
            background: #ff4444;
            color: white;
        }
        .status { 
            padding: 10px; 
            background: #f0f0f0; 
            border-radius: 5px;
            margin-bottom: 20px;
        }
        button { 
            padding: 10px 20px; 
            font-size: 16px; 
            cursor: pointer;
            margin: 5px;
        }
    </style>
</head>
<body>
    <h1>🔔 EMR Critical Alerts - WebSocket Test</h1>
    
    <div class="status" id="status">
        <strong>Статус:</strong> <span id="connectionStatus">Не подключено</span>
    </div>
    
    <button onclick="connect()">🔌 Подключиться</button>
    <button onclick="disconnect()">❌ Отключиться</button>
    <button onclick="clearAlerts()">🗑️ Очистить алерты</button>
    
    <h2>Полученные алерты:</h2>
    <div id="alerts"></div>

    <script>
        let stompClient = null;
        
        function connect() {
            const socket = new SockJS('http://localhost:8080/ws-emr-alerts');
            stompClient = Stomp.over(socket);
            
            stompClient.connect({}, function(frame) {
                document.getElementById('connectionStatus').textContent = '✅ Подключено';
                document.getElementById('connectionStatus').style.color = 'green';
                console.log('Connected: ' + frame);
                
                // Подписка на критические алерты
                stompClient.subscribe('/topic/emr-critical-alerts', function(message) {
                    showAlert(JSON.parse(message.body));
                });
            }, function(error) {
                document.getElementById('connectionStatus').textContent = '❌ Ошибка подключения';
                document.getElementById('connectionStatus').style.color = 'red';
                console.error('Error: ' + error);
            });
        }
        
        function disconnect() {
            if (stompClient !== null) {
                stompClient.disconnect();
            }
            document.getElementById('connectionStatus').textContent = '🔌 Отключено';
            document.getElementById('connectionStatus').style.color = 'gray';
        }
        
        function showAlert(alert) {
            const alertsDiv = document.getElementById('alerts');
            const alertElement = document.createElement('div');
            alertElement.className = 'alert';
            alertElement.innerHTML = `
                <strong>🚨 ${alert.severity} ALERT</strong><br>
                <strong>Пациент:</strong> ${alert.patientMrn}<br>
                <strong>Параметр:</strong> ${alert.parameterName}<br>
                <strong>Изменение:</strong> ${alert.oldValue} → ${alert.newValue}<br>
                <strong>Описание:</strong> ${alert.changeDescription}<br>
                <strong>Время:</strong> ${alert.detectedAt}<br>
                <strong>Требуется пересмотр рекомендаций:</strong> ${alert.requiresRecommendationReview ? 'Да' : 'Нет'}
            `;
            alertsDiv.insertBefore(alertElement, alertsDiv.firstChild);
        }
        
        function clearAlerts() {
            document.getElementById('alerts').innerHTML = '';
        }
    </script>
</body>
</html>
```

**Как использовать:**
1. Сохраните файл как `websocket-test.html`
2. Откройте в браузере
3. Нажмите "Подключиться"
4. Запустите синхронизацию через Postman
5. Наблюдайте real-time алерты на странице

---

## 🧪 ТЕСТИРОВАНИЕ ЧЕРЕЗ POSTMAN (WebSocket)

### Шаг 1: Создайте WebSocket Request в Postman

1. Откройте Postman
2. New → WebSocket Request
3. URL: `ws://localhost:8080/ws-emr-alerts`
4. Нажмите "Connect"

### Шаг 2: Отправьте STOMP команду подписки

```json
CONNECT
accept-version:1.1,1.0
heart-beat:10000,10000

^@
SUBSCRIBE
id:sub-0
destination:/topic/emr-critical-alerts

^@
```

### Шаг 3: Запустите синхронизацию

В другой вкладке Postman:
```
POST http://localhost:8080/api/emr/sync/all
```

### Шаг 4: Наблюдайте сообщения в WebSocket вкладке

Вы должны увидеть:
```
MESSAGE
destination:/topic/emr-critical-alerts
content-type:application/json
subscription:sub-0
message-id:1

{"patientMrn":"EMR-A1B2C3D4","parameterName":"GFR",...}
```

---

# 🧬 ТЕСТИРОВАНИЕ МОКОВЫХ ПАЦИЕНТОВ С PROTOCOL ICD КОДАМИ

## 📋 Что изменилось (19.10.2025)

**ВАЖНО:** Теперь моковые пациенты создаются **ТОЛЬКО с диагнозами из Treatment Protocol** (колонка `contraindications`).

### До изменений:
- Моковые пациенты: случайные 14,000+ ICD кодов
- Contraindications срабатывали редко
- Тестирование было непредсказуемым

### После изменений:
- Моковые пациенты: только **47 уникальных ICD кодов** из Treatment Protocol
- Contraindications срабатывают часто
- Тестирование стало предсказуемым и реалистичным

---

## ✅ ENDPOINT: Проверить Protocol ICD коды

### Request:
```
GET http://localhost:8080/api/emr/protocol-icd-codes
```

### Ожидаемый Response (200 OK):
```json
{
  "count": 15,
  "message": "Mock patients are generated with 15 ICD codes from Treatment Protocol",
  "info": "These are the contraindication codes that affect treatment selection"
}
```

### ✅ Что проверить:
- `count` = 47 (извлечены все ICD коды из протокола)
- В логах: `Successfully extracted 47 ICD codes from Treatment Protocol`

---

## 🧪 ТЕСТИРОВАНИЕ: Создать пациента с противопоказанием

### Шаг 1: Создать мокового пациента
```
POST http://localhost:8080/api/emr/mock/generate?createdBy=test
```

### Шаг 2: Проверить диагнозы в H2 Console

```sql
SELECT 
    p.mrn,
    p.first_name,
    p.last_name,
    d.icd_code,
    d.description
FROM nurse_patients p
JOIN emr e ON e.patient_id = p.id
JOIN diagnosis d ON d.emr_id = e.id
WHERE p.mrn LIKE 'EMR-%'
ORDER BY p.id DESC
LIMIT 10;
```

**Ожидаемый результат:**
- Диагнозы типа: `571.2` (Alcoholic cirrhosis), `571.5` (Cirrhosis), `V45.11` (Renal dialysis)
- Все диагнозы из Treatment Protocol contraindications

### Шаг 3: Создать рекомендацию для пациента

Используйте UI или API для создания рекомендации.

### Шаг 4: Проверить, что contraindications сработали

```sql
SELECT 
    r.id,
    r.patient_id,
    r.comments
FROM recommendations r
WHERE r.patient_id = <PATIENT_ID>
ORDER BY r.id DESC
LIMIT 1;
```

**Ожидаемый результат в comments:**
```
System: avoid for contraindications (match by base ICD): Alcoholic cirrhosis of liver (571.2)
```

---

## 📊 ТЕСТИРОВАНИЕ: Статистика по диагнозам

### Создать batch пациентов:
```
POST http://localhost:8080/api/emr/mock/generate-batch?count=20&createdBy=batch_test
```

### Проверить распределение диагнозов:

```sql
SELECT 
    d.icd_code,
    d.description,
    COUNT(*) as usage_count
FROM diagnosis d
JOIN emr e ON d.emr_id = e.id
JOIN nurse_patients p ON e.patient_id = p.id
WHERE p.mrn LIKE 'EMR-%'
GROUP BY d.icd_code, d.description
ORDER BY usage_count DESC;
```

**Ожидаемый результат:**
- Только коды из Treatment Protocol (571.2, 571.5, 571.9, V45.11, E11.9, и т.д.)
- Равномерное распределение (каждый код используется примерно одинаково)

---

## 🔍 ПРОВЕРКА В ЛОГАХ

При старте приложения:

```
INFO  - Loading ICD codes from CSV...
INFO  - Successfully loaded 14567 ICD codes
INFO  - Treatment protocol table successfully loaded and sanitized.
INFO  - Extracting ICD codes from Treatment Protocol contraindications...
INFO  - Found 47 unique ICD codes in Treatment Protocol
INFO  - Successfully extracted 47 ICD codes from Treatment Protocol:
INFO    - 571.201 : Alcoholic cirrhosis of liver (variant: 571.201)
INFO    - 571.501 : Cirrhosis of liver without mention of alcohol (variant: 571.501)
INFO    - 571.901 : Unspecified chronic liver disease (variant: 571.901)
INFO    - 287.4901 : Contraindication condition (ICD: 287.4901)
INFO    - 345.0001 : Contraindication condition (ICD: 345.0001)
...
```

При создании мокового пациента:

```
INFO  - Generating mock patient: createdBy=test
INFO  - Created 2 diagnoses for patient: 571.2 - Alcoholic cirrhosis of liver, E11.9 - Type 2 diabetes mellitus
INFO  - Mock patient created successfully: name=John Smith, internalEmr=EMR-ABC123
```

---

## 📋 ПРИМЕРЫ ICD КОДОВ ИЗ TREATMENT PROTOCOL

| ICD Code | Description |
|----------|-------------|
| 571.2 | Alcoholic cirrhosis of liver |
| 571.5 | Cirrhosis of liver without mention of alcohol |
| 571.9 | Unspecified chronic liver disease |
| V45.11 | Renal dialysis status |
| E11.9 | Type 2 diabetes mellitus without complications |
| I50.9 | Heart failure, unspecified |
| K70.3 | Alcoholic cirrhosis of liver |
| K74.6 | Other and unspecified cirrhosis of liver |
| N18.6 | End stage renal disease |
| Z99.2 | Dependence on renal dialysis |

---

## 📧 ТЕСТИРОВАНИЕ EMAIL УВЕДОМЛЕНИЙ (опционально)

### Настройка Spring Mail (application.properties):

```properties
# Email уведомления (опционально)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Email получателей для критических алертов
emr.alerts.email.recipients=doctor1@hospital.com,doctor2@hospital.com
```

### Проверка отправки email:

После синхронизации с критическими алертами проверьте логи:
```
INFO  - Email уведомления отправлены (если настроен Spring Mail)
```

Или:
```
WARN  - Email уведомления не отправлены (Spring Mail не настроен или ошибка)
```

---

# 💻 ТЕСТИРОВАНИЕ ЧЕРЕЗ CURL

## Endpoint 1: Генерация 1 мокового пациента

```bash
curl -X POST "http://localhost:8080/api/emr/mock/generate?createdBy=igor_test" \
  -H "Content-Type: application/json"
```

## Endpoint 2: Генерация 10 моковых пациентов

```bash
curl -X POST "http://localhost:8080/api/emr/mock/generate-batch?count=10&createdBy=igor_test" \
  -H "Content-Type: application/json"
```

## Endpoint 3: Проверка импорта

```bash
curl -X GET "http://localhost:8080/api/emr/check-import/mock-patient-abc123"
```

## Endpoint 4: Конвертация Observations

```bash
curl -X POST "http://localhost:8080/api/emr/convert-observations?createdBy=igor_test" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "loincCode": "2160-0",
      "value": 1.2
    },
    {
      "loincCode": "777-3",
      "value": 180.0
    }
  ]'
```

## Endpoint 5: Импорт из FHIR

```bash
curl -X POST "http://localhost:8080/api/emr/import/Patient/12345?importedBy=igor_test" \
  -H "Content-Type: application/json"
```

## Endpoint 6: Поиск в FHIR

```bash
curl -X GET "http://localhost:8080/api/emr/search?firstName=John&lastName=Smith"
```

## Endpoint 7: Получение Observations

```bash
curl -X GET "http://localhost:8080/api/emr/observations/Patient/12345"
```

## Endpoint 8: Синхронизация всех пациентов

```bash
curl -X POST "http://localhost:8080/api/emr/sync/all" \
  -H "Content-Type: application/json"
```

## Endpoint 9: Синхронизация конкретного пациента

```bash
curl -X POST "http://localhost:8080/api/emr/sync/patient/EMR-A1B2C3D4" \
  -H "Content-Type: application/json"
```

---

# 🗄️ ПРОВЕРКА БАЗЫ ДАННЫХ

## Подключение к H2 Console

1. Открыть браузер: http://localhost:8080/h2-console
2. JDBC URL: `jdbc:h2:mem:testdb`
3. Username: `sa`
4. Password: (пустой)

## SQL запросы для проверки

### 1. Проверить созданных пациентов

```sql
-- Все пациенты
SELECT * FROM nurse_patients ORDER BY created_at DESC;

-- Количество пациентов
SELECT COUNT(*) as total_patients FROM nurse_patients;

-- Последний созданный пациент
SELECT * FROM nurse_patients ORDER BY id DESC LIMIT 1;
```

### 2. Проверить медицинские карты (EMR)

```sql
-- Все EMR
SELECT * FROM emr ORDER BY created_at DESC;

-- EMR с пациентами (JOIN)
SELECT 
    p.mrn,
    p.first_name,
    p.last_name,
    e.gfr,
    e.plt,
    e.wbc,
    e.sodium,
    e.sat,
    e.created_at
FROM emr e
JOIN nurse_patients p ON e.patient_id = p.id
ORDER BY e.created_at DESC;
```

### 3. Проверить маппинги (EMR Mappings)

```sql
-- Все маппинги
SELECT * FROM emr_mappings ORDER BY imported_at DESC;

-- Маппинги по типу источника
SELECT 
    source_type,
    COUNT(*) as count
FROM emr_mappings
GROUP BY source_type;

-- Маппинги с пациентами
SELECT 
    em.external_fhir_id,
    em.internal_emr_number,
    em.source_type,
    p.first_name,
    p.last_name,
    em.imported_at
FROM emr_mappings em
JOIN nurse_patients p ON p.mrn = em.internal_emr_number
ORDER BY em.imported_at DESC;
```

### 4. Проверить дубликаты

```sql
-- Проверить, есть ли дубликаты EMR номеров
SELECT 
    internal_emr_number,
    COUNT(*) as count
FROM emr_mappings
GROUP BY internal_emr_number
HAVING COUNT(*) > 1;

-- Проверить, есть ли дубликаты FHIR ID
SELECT 
    external_fhir_id,
    COUNT(*) as count
FROM emr_mappings
GROUP BY external_fhir_id
HAVING COUNT(*) > 1;
```

### 5. Статистика

```sql
-- Общая статистика
SELECT 
    (SELECT COUNT(*) FROM nurse_patients) as total_patients,
    (SELECT COUNT(*) FROM emr) as total_emr_records,
    (SELECT COUNT(*) FROM emr_mappings) as total_mappings,
    (SELECT COUNT(*) FROM emr_mappings WHERE source_type = 'MOCK_GENERATOR') as mock_patients,
    (SELECT COUNT(*) FROM emr_mappings WHERE source_type = 'FHIR_SERVER') as fhir_patients;
```

---

# 🐛 ТИПИЧНЫЕ ОШИБКИ И РЕШЕНИЯ

## Ошибка 1: "Connection refused" при импорте из FHIR

**Причина:** FHIR сервер недоступен или неправильно настроен.

**Решение:**
1. Проверить `application.properties`:
```properties
fhir.server.url=http://hapi.fhir.org/baseR4
```
2. Проверить доступность FHIR сервера:
```bash
curl http://hapi.fhir.org/baseR4/metadata
```

## Ошибка 2: "Patient already imported"

**Причина:** Пациент с таким FHIR ID уже импортирован.

**Решение:**
- Это **нормальное поведение** (защита от дубликатов)
- Используйте другой FHIR ID
- Или удалите запись из `emr_mappings`:
```sql
DELETE FROM emr_mappings WHERE external_fhir_id = 'Patient/12345';
```

## Ошибка 3: "Internal Server Error" при генерации моков

**Причина:** Ошибка в базе данных или логике.

**Решение:**
1. Проверить логи приложения
2. Проверить, что таблицы созданы:
```sql
SHOW TABLES;
```
3. Проверить constraints:
```sql
SELECT * FROM INFORMATION_SCHEMA.CONSTRAINTS 
WHERE TABLE_NAME IN ('nurse_patients', 'emr', 'emr_mappings');
```

## Ошибка 4: GFR рассчитывается неправильно

**Причина:** Неправильное значение креатинина.

**Решение:**
- Проверить формулу: GFR ≈ 100 / креатинин
- Примеры:
  - Креатинин 0.9 → GFR ≈ 111 → "≥90 (Normal)"
  - Креатинин 1.5 → GFR ≈ 67 → "60-89 (Mild decrease)"
  - Креатинин 2.5 → GFR = 40 → "30-59 (Moderate decrease)"
  - Креатинин 8.0 → GFR = 12.5 → "<15 (Kidney failure)"

## Ошибка 5: "Foreign key constraint violation"

**Причина:** Пытаетесь создать EMR для несуществующего пациента.

**Решение:**
1. Сначала создайте пациента
2. Затем создайте EMR
3. Проверить, что `patient_id` в таблице `emr` соответствует `id` в таблице `nurse_patients`

---

# ✅ ЧЕКЛИСТ ПОЛНОГО ТЕСТИРОВАНИЯ

## Базовые тесты (обязательно)

- [ ] 1. Генерация 1 мокового пациента
- [ ] 2. Генерация 10 моковых пациентов
- [ ] 3. Проверка импорта существующего пациента
- [ ] 4. Проверка импорта несуществующего пациента
- [ ] 5. Конвертация Observations с нормальным креатинином (0.9)
- [ ] 6. Конвертация Observations с высоким креатинином (2.5)

## Проверка БД (обязательно)

- [ ] 7. Проверить, что пациенты создались в `nurse_patients`
- [ ] 8. Проверить, что EMR создались в `emr`
- [ ] 9. Проверить, что маппинги создались в `emr_mappings`
- [ ] 10. Проверить, что нет дубликатов EMR номеров
- [ ] 11. Проверить, что нет дубликатов FHIR ID

## Тесты синхронизации (НОВОЕ - 18.10.2025)

- [ ] 12. Синхронизация всех FHIR пациентов
- [ ] 13. Синхронизация конкретного пациента по MRN
- [ ] 14. Проверка обнаружения изменений в EMR данных
- [ ] 15. Генерация критических алертов при GFR < 30
- [ ] 16. Генерация критических алертов при PLT < 50
- [ ] 17. Генерация критических алертов при WBC < 2.0
- [ ] 18. Генерация критических алертов при SAT < 90
- [ ] 19. Проверка множественных критических изменений
- [ ] 20. Проверка обновления `updated_by = 'EMR_SYNC_SCHEDULER'` в БД

## Тесты WebSocket уведомлений (НОВОЕ - 18.10.2025)

- [ ] 21. Подключение к WebSocket через браузер
- [ ] 22. Подписка на топик `/topic/emr-critical-alerts`
- [ ] 23. Получение real-time уведомлений при синхронизации
- [ ] 24. Проверка формата алертов в WebSocket
- [ ] 25. Тестирование через HTML страницу `websocket-test.html`
- [ ] 26. Проверка отключения WebSocket

## Расширенные тесты (опционально)

- [ ] 27. Генерация 50 моковых пациентов (производительность)
- [ ] 28. Импорт из реального FHIR сервера (если доступен)
- [ ] 29. Поиск пациентов в FHIR (если доступен)
- [ ] 30. Получение Observations из FHIR (если доступен)
- [ ] 31. Автоматическая синхронизация по расписанию (каждые 6 часов)
- [ ] 32. Email уведомления (если настроен Spring Mail)

## Тесты на ошибки (опционально)

- [ ] 33. Попытка импорта с неправильным FHIR ID
- [ ] 34. Попытка импорта при недоступном FHIR сервере
- [ ] 35. Конвертация Observations с пустым массивом
- [ ] 36. Конвертация Observations с неправильными LOINC кодами
- [ ] 37. Синхронизация несуществующего пациента
- [ ] 38. Синхронизация мокового пациента (должна вернуть ошибку)

---

# 🎯 БЫСТРЫЙ СТАРТ (5 МИНУТ)

Если у тебя мало времени, выполни **только эти 5 тестов**:

## 1. Генерация 1 мокового пациента
```bash
curl -X POST "http://localhost:8080/api/emr/mock/generate?createdBy=quick_test"
```

## 2. Проверка в БД
```sql
SELECT * FROM nurse_patients ORDER BY id DESC LIMIT 1;
SELECT * FROM emr ORDER BY id DESC LIMIT 1;
SELECT * FROM emr_mappings ORDER BY id DESC LIMIT 1;
```

## 3. Генерация 10 моковых пациентов
```bash
curl -X POST "http://localhost:8080/api/emr/mock/generate-batch?count=10&createdBy=quick_test"
```

## 4. Проверка количества
```sql
SELECT COUNT(*) FROM nurse_patients;
SELECT COUNT(*) FROM emr;
SELECT COUNT(*) FROM emr_mappings;
```

## 5. Конвертация Observations
```bash
curl -X POST "http://localhost:8080/api/emr/convert-observations?createdBy=quick_test" \
  -H "Content-Type: application/json" \
  -d '[{"loincCode":"2160-0","value":1.2}]'
```

**Ожидаемый результат:** `"gfr": "60-89 (Mild decrease)"`

---

# 🚀 БЫСТРЫЙ СТАРТ ДЛЯ СИНХРОНИЗАЦИИ (НОВОЕ - 3 МИНУТЫ)

Если хочешь быстро протестировать **новый функционал синхронизации**:

## 1. Создай тестового пациента
```bash
curl -X POST "http://localhost:8080/api/emr/mock/generate?createdBy=sync_test"
```

## 2. Симулируй критическое изменение в БД
```sql
-- Получи MRN последнего созданного пациента
SELECT mrn FROM nurse_patients ORDER BY id DESC LIMIT 1;

-- Установи критические значения (замени EMR-XXXXXXXX на реальный MRN)
UPDATE emr 
SET gfr = '25', plt = 45.0, wbc = 1.8, sat = 88.0
WHERE patient_id = (SELECT id FROM nurse_patients WHERE mrn = 'EMR-XXXXXXXX');
```

## 3. Запусти синхронизацию
```bash
curl -X POST "http://localhost:8080/api/emr/sync/all"
```

## 4. Проверь результат
Должны быть сгенерированы **4 критических алерта** (GFR, PLT, WBC, SAT)

## 5. Тест WebSocket (опционально)
Открой консоль браузера (F12) и выполни:
```javascript
const socket = new WebSocket('ws://localhost:8080/ws-emr-alerts');
socket.onmessage = (e) => console.log('ALERT:', JSON.parse(e.data));
```
Затем повтори шаг 3 - увидишь real-time алерты!

---

# 📊 РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ

## Шаблон для записи результатов

```
Дата тестирования: _______________
Тестировщик: _______________

| Endpoint | Метод | Статус | Время ответа | Примечания |
|----------|-------|--------|--------------|------------|
| /mock/generate | POST | ✅ / ❌ | ___ ms | |
| /mock/generate-batch | POST | ✅ / ❌ | ___ ms | |
| /check-import/{id} | GET | ✅ / ❌ | ___ ms | |
| /convert-observations | POST | ✅ / ❌ | ___ ms | |
| /import/{id} | POST | ✅ / ❌ | ___ ms | |
| /search | GET | ✅ / ❌ | ___ ms | |
| /observations/{id} | GET | ✅ / ❌ | ___ ms | |
| **/sync/all** | **POST** | ✅ / ❌ | ___ ms | **НОВОЕ** |
| **/sync/patient/{mrn}** | **POST** | ✅ / ❌ | ___ ms | **НОВОЕ** |

БД проверки:
- Пациенты созданы: ✅ / ❌
- EMR созданы: ✅ / ❌
- Маппинги созданы: ✅ / ❌
- Дубликаты отсутствуют: ✅ / ❌
- EMR обновлены синхронизацией: ✅ / ❌ **НОВОЕ**
- updated_by = 'EMR_SYNC_SCHEDULER': ✅ / ❌ **НОВОЕ**

Синхронизация и алерты:
- Обнаружение изменений работает: ✅ / ❌ **НОВОЕ**
- Критические алерты генерируются: ✅ / ❌ **НОВОЕ**
- WebSocket уведомления работают: ✅ / ❌ **НОВОЕ**

Общий вердикт: ✅ ВСЕ РАБОТАЕТ / ❌ ЕСТЬ ПРОБЛЕМЫ
```

---

# 🚀 СЛЕДУЮЩИЕ ШАГИ

После успешного тестирования:

1. ✅ Добавить unit-тесты для `EmrIntegrationServiceImpl`
2. ✅ Добавить integration-тесты для контроллера
3. ✅ Настроить CI/CD pipeline с автоматическими тестами
4. ✅ Добавить мониторинг и метрики (сколько пациентов импортировано)
5. ✅ Документировать API через Swagger/OpenAPI
6. ✅ **НОВОЕ:** Добавить unit-тесты для `EmrSyncScheduler`
7. ✅ **НОВОЕ:** Добавить unit-тесты для `EmrChangeDetectionService`
8. ✅ **НОВОЕ:** Настроить мониторинг WebSocket соединений
9. ✅ **НОВОЕ:** Настроить Email уведомления (Spring Mail)
10. ✅ **НОВОЕ:** Добавить дашборд для мониторинга критических алертов

---

# 📝 CHANGELOG

## Версия 2.1 (19.10.2025) - ОБНОВЛЕНИЕ

### ✨ Новый функционал:
- **Protocol ICD коды для моковых пациентов** - только релевантные диагнозы
- **TreatmentProtocolIcdExtractor** - извлечение ICD кодов из contraindications
- **Новый endpoint** - `/api/emr/protocol-icd-codes` для проверки

### 📚 Новые разделы документации:
- Тестирование моковых пациентов с Protocol ICD кодами
- Проверка contraindications на реалистичных данных
- Статистика по распределению диагнозов

### 🔧 Технические улучшения:
- `TreatmentProtocolIcdExtractor` - новый сервис
- `MockEmrDataGenerator` - обновлен для использования Protocol ICD
- Моковые пациенты теперь имеют только релевантные диагнозы (**47 уникальных кодов**)
- `EmailNotificationService` - исправлена инъекция зависимостей (constructor injection)

---

## Версия 2.0 (18.10.2025) - ОБНОВЛЕНИЕ

### ✨ Новый функционал:
- **Автоматическая синхронизация EMR** - каждые 6 часов
- **Ручная синхронизация** - всех пациентов или конкретного
- **Обнаружение критических изменений** - GFR, PLT, WBC, SAT
- **Real-time уведомления** - через WebSocket
- **Email уведомления** - опционально через Spring Mail

### 📚 Новые разделы документации:
- Тестирование синхронизации EMR (2 новых endpoint)
- Тестирование WebSocket уведомлений
- Тестирование обнаружения критических изменений
- Таблица критических порогов
- HTML страница для тестирования WebSocket
- Быстрый старт для синхронизации (3 минуты)
- Обновленный чеклист (38 пунктов)

### 🔧 Технические улучшения:
- Добавлены зависимости: `spring-boot-starter-websocket`, `spring-boot-starter-mail`
- Новые сервисы: `EmrSyncScheduler`, `EmrChangeDetectionService`, `WebSocketNotificationService`
- Новые DTO: `EmrSyncResultDTO`, `EmrChangeAlertDTO`
- Scheduled задача: `@Scheduled(cron = "0 0 */6 * * *")`

---

## Версия 1.0 (04.10.2025) - ПЕРВАЯ ВЕРСИЯ

### Базовый функционал:
- Импорт пациентов из FHIR серверов
- Генерация моковых пациентов
- Конвертация FHIR Observations в EMR
- Поиск пациентов в FHIR
- Проверка дубликатов

---

**Документ создан:** 04.10.2025  
**Последнее обновление:** 19.10.2025  
**Автор:** Игорь Негода  
**Версия:** 2.1

**Удачи в тестировании! 🚀**

---

# 📞 КОНТАКТЫ И ПОДДЕРЖКА

Если возникли вопросы или проблемы при тестировании:

1. **Проверьте логи приложения** - большинство ошибок описаны там
2. **Проверьте H2 Console** - состояние базы данных
3. **Проверьте FHIR сервер** - доступность внешней системы
4. **Проверьте WebSocket** - подключение в консоли браузера

**Основные логи для отладки:**
```
INFO  - EMR sync completed: success=8, unchanged=2, failed=0, duration=5234ms
WARN  - Обнаружено 2 критических алертов, отправка уведомлений...
ERROR - Failed to sync patient: fhirId=Patient/123, error=Connection timeout
```

**Полезные команды:**
```bash
# Проверить статус приложения
curl http://localhost:8080/actuator/health

# Проверить логи в реальном времени (если запущено через mvn)
# Логи будут в консоли

# Очистить H2 базу данных (перезапустить приложение)
# Все данные в памяти будут удалены
```
