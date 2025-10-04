# 🧪 ПОЛНЫЙ ГАЙД ПО ТЕСТИРОВАНИЮ EMR INTEGRATION MODULE

## Дата: 04.10.2025
## Автор: Игорь Негода

---

# 📑 СОДЕРЖАНИЕ

1. [Подготовка к тестированию](#подготовка-к-тестированию)
2. [Тестирование через Postman](#тестирование-через-postman)
3. [Тестирование через curl](#тестирование-через-curl)
4. [Проверка базы данных](#проверка-базы-данных)
5. [Типичные ошибки и решения](#типичные-ошибки-и-решения)

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

## Расширенные тесты (опционально)

- [ ] 12. Генерация 50 моковых пациентов (производительность)
- [ ] 13. Импорт из реального FHIR сервера (если доступен)
- [ ] 14. Поиск пациентов в FHIR (если доступен)
- [ ] 15. Получение Observations из FHIR (если доступен)

## Тесты на ошибки (опционально)

- [ ] 16. Попытка импорта с неправильным FHIR ID
- [ ] 17. Попытка импорта при недоступном FHIR сервере
- [ ] 18. Конвертация Observations с пустым массивом
- [ ] 19. Конвертация Observations с неправильными LOINC кодами

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

БД проверки:
- Пациенты созданы: ✅ / ❌
- EMR созданы: ✅ / ❌
- Маппинги созданы: ✅ / ❌
- Дубликаты отсутствуют: ✅ / ❌

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

---

**Документ создан:** 04.10.2025  
**Автор:** Игорь Негода  
**Версия:** 1.0

**Удачи в тестировании! 🚀**
