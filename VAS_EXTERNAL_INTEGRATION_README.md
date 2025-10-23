# Модуль внешней интеграции VAS (VAS External Integration Module)

## Обзор

**VAS External Integration Module** - это универсальная система приема данных об уровне боли (VAS - Visual Analog Scale) из внешних медицинских систем. Модуль поддерживает множественные форматы данных, обеспечивает безопасность через API ключи и автоматически генерирует рекомендации по лечению.

## Ключевые возможности

- ✅ **5 поддерживаемых форматов**: JSON, XML, HL7 v2, FHIR, CSV
- ✅ **Автоматическое определение формата** по Content-Type и структуре данных
- ✅ **Система API ключей** для аутентификации внешних систем
- ✅ **IP Whitelist** для ограничения доступа по IP адресам
- ✅ **Rate Limiting** - ограничение запросов в минуту
- ✅ **Автоматическая генерация рекомендаций** при VAS >= 4
- ✅ **Batch импорт** из CSV файлов
- ✅ **Полное логирование** всех операций

## Архитектура модуля

```
VAS_external_integration/
├── entity/
│   └── ApiKey.java                    # API ключи внешних систем
├── dto/
│   └── ExternalVasRecordRequest.java  # Унифицированный DTO для VAS
├── parser/
│   ├── VasFormatParser.java           # Интерфейс парсера
│   ├── JsonVasParser.java             # Парсер JSON
│   ├── XmlVasParser.java              # Парсер XML
│   ├── Hl7VasParser.java              # Парсер HL7 v2
│   ├── FhirVasParser.java             # Парсер FHIR
│   └── CsvVasParser.java              # Парсер CSV
├── service/
│   ├── ApiKeyService.java             # Управление API ключами
│   ├── VasParserFactory.java          # Фабрика парсеров
│   └── ExternalVasIntegrationService.java  # Обработка VAS данных
├── controller/
│   ├── ExternalVasIntegrationController.java  # Прием VAS данных
│   └── ApiKeyManagementController.java        # Управление API ключами
└── repository/
    └── ApiKeyRepository.java          # JPA репозиторий для API ключей
```

## Поддерживаемые форматы данных

### 1. JSON (application/json)

**Приоритет**: 10 (самый высокий)

```json
{
  "patientMrn": "EMR-12345678",
  "vasLevel": 7,
  "deviceId": "MONITOR-001",
  "location": "Ward A, Bed 12",
  "timestamp": "2025-10-20T14:30:00",
  "notes": "Patient complains of severe pain",
  "source": "VAS_MONITOR"
}
```

### 2. XML (application/xml)

**Приоритет**: 9

```xml
<vasRecord>
  <patientMrn>EMR-12345678</patientMrn>
  <vasLevel>7</vasLevel>
  <deviceId>MONITOR-001</deviceId>
  <location>Ward A, Bed 12</location>
  <timestamp>2025-10-20T14:30:00</timestamp>
  <notes>Patient complains of severe pain</notes>
  <source>VAS_MONITOR</source>
</vasRecord>
```

### 3. HL7 v2 (application/hl7-v2)

**Приоритет**: 8

```
MSH|^~\&|VAS_SYSTEM|HOSPITAL|PAIN_MGMT|HOSPITAL|20251020143000||ORU^R01|MSG001|P|2.5
PID|||EMR-12345678||Doe^John||19800101|M
OBX|1|NM|38208-5^Pain severity^LN||7|{score}|0-10||||F|||20251020143000
```

**Описание сегментов**:
- **MSH** - заголовок сообщения (Message Header)
- **PID** - информация о пациенте (Patient Identification)
- **OBX** - наблюдения (Observation/Result)
  - LOINC код `38208-5` = Pain severity (уровень боли)

### 4. FHIR (application/fhir+json)

**Приоритет**: 7

```json
{
  "resourceType": "Observation",
  "status": "final",
  "code": {
    "coding": [{
      "system": "http://loinc.org",
      "code": "38208-5",
      "display": "Pain severity"
    }]
  },
  "subject": {
    "reference": "Patient/EMR-12345678"
  },
  "effectiveDateTime": "2025-10-20T14:30:00Z",
  "valueInteger": 7,
  "device": {
    "display": "MONITOR-001"
  }
}
```

### 5. CSV (text/csv) - для batch импорта

**Приоритет**: 6

```csv
patientMrn,vasLevel,deviceId,location,timestamp,notes,source
EMR-12345678,7,MONITOR-001,Ward A Bed 12,2025-10-20T14:30:00,Severe pain,VAS_MONITOR
EMR-87654321,4,TABLET-002,ICU-3,2025-10-20T14:35:00,Moderate pain,MANUAL_ENTRY
EMR-11223344,9,MONITOR-002,ER-Room-5,2025-10-20T14:40:00,Critical pain,VAS_MONITOR
```

## REST API Endpoints

### Прием VAS данных (ExternalVasIntegrationController)

#### 1. POST /api/external/vas/record - Запись одной VAS

**Описание**: Прием одной VAS записи в любом поддерживаемом формате.

**Заголовки**:
- `X-API-Key` (обязательно) - API ключ внешней системы
- `Content-Type` (опционально) - тип данных (application/json, application/xml, и т.д.)

**Body**: Сырые данные в любом поддерживаемом формате

**Пример запроса (JSON)**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: abc123def456ghi789jkl012mno345pq" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "EMR-12345678",
    "vasLevel": 7,
    "deviceId": "MONITOR-001",
    "location": "Ward A, Bed 12",
    "source": "VAS_MONITOR"
  }'
```

**Ответ (успех)**:
```json
{
  "status": "success",
  "vasId": 123,
  "patientMrn": "EMR-12345678",
  "vasLevel": 7,
  "format": "JSON"
}
```

**Ответ (ошибка - невалидный API ключ)**:
```json
{
  "error": "Invalid API key or IP not whitelisted"
}
```
HTTP Status: 401 Unauthorized

#### 2. POST /api/external/vas/batch - Batch импорт из CSV

**Описание**: Импорт множественных VAS записей из CSV файла.

**Заголовки**:
- `X-API-Key` (обязательно)
- `Content-Type: text/csv`

**Body**: CSV данные

**Пример запроса**:
```bash
curl -X POST http://localhost:8080/api/external/vas/batch \
  -H "X-API-Key: abc123def456ghi789jkl012mno345pq" \
  -H "Content-Type: text/csv" \
  --data-binary @vas_records.csv
```

**Ответ (успех)**:
```json
{
  "status": "success",
  "total": 10,
  "success": 9,
  "failed": 1,
  "createdVasIds": [101, 102, 103, 104, 105, 106, 107, 108, 109],
  "errors": [
    "Line 5: Patient not found with MRN: EMR-99999999 (MRN: EMR-99999999)"
  ]
}
```

#### 3. GET /api/external/vas/health - Health Check

**Описание**: Проверка работоспособности модуля.

**Пример запроса**:
```bash
curl -X GET http://localhost:8080/api/external/vas/health
```

**Ответ**:
```json
{
  "status": "UP",
  "module": "External VAS Integration",
  "timestamp": "2025-10-20T14:30:00"
}
```

### Управление API ключами (ApiKeyManagementController)

#### 1. POST /api/admin/api-keys/generate - Генерация нового API ключа

**Описание**: Создание нового API ключа для внешней системы.

**Параметры**:
- `systemName` (обязательно) - название системы
- `description` (опционально) - описание системы
- `expiresInDays` (опционально) - срок действия в днях (null = бессрочный)
- `createdBy` (обязательно) - кто создал ключ

**Пример запроса**:
```bash
curl -X POST "http://localhost:8080/api/admin/api-keys/generate?systemName=VAS%20Monitor%20Ward%20A&description=VAS%20monitoring%20system%20in%20Ward%20A&expiresInDays=365&createdBy=admin"
```

**Ответ**:
```json
{
  "status": "success",
  "message": "API key generated successfully",
  "apiKey": "abc123def456ghi789jkl012mno345pq",
  "systemName": "VAS Monitor Ward A",
  "expiresAt": "2026-10-20T14:30:00",
  "ipWhitelist": "*",
  "rateLimitPerMinute": 100
}
```

#### 2. GET /api/admin/api-keys - Получение всех активных ключей

**Пример запроса**:
```bash
curl -X GET http://localhost:8080/api/admin/api-keys
```

**Ответ**:
```json
{
  "status": "success",
  "total": 3,
  "keys": [
    {
      "apiKey": "abc123def456ghi789jkl012mno345pq",
      "systemName": "VAS Monitor Ward A",
      "description": "VAS monitoring system in Ward A",
      "active": true,
      "createdAt": "2025-10-20T14:30:00",
      "expiresAt": "2026-10-20T14:30:00",
      "ipWhitelist": "192.168.1.100,192.168.1.101",
      "rateLimitPerMinute": 100,
      "usageCount": 1523
    }
  ]
}
```

#### 3. DELETE /api/admin/api-keys/{apiKey} - Деактивация ключа

**Пример запроса**:
```bash
curl -X DELETE http://localhost:8080/api/admin/api-keys/abc123def456ghi789jkl012mno345pq
```

**Ответ**:
```json
{
  "status": "success",
  "message": "API key deactivated successfully"
}
```

#### 4. PUT /api/admin/api-keys/{apiKey}/whitelist - Обновление IP whitelist

**Параметры**:
- `ipWhitelist` - список IP адресов через запятую или "*" для любого IP

**Пример запроса**:
```bash
curl -X PUT "http://localhost:8080/api/admin/api-keys/abc123def456ghi789jkl012mno345pq/whitelist?ipWhitelist=192.168.1.100,192.168.1.101"
```

**Ответ**:
```json
{
  "status": "success",
  "message": "IP whitelist updated successfully"
}
```

#### 5. PUT /api/admin/api-keys/{apiKey}/rate-limit - Обновление rate limit

**Параметры**:
- `rateLimitPerMinute` - количество запросов в минуту

**Пример запроса**:
```bash
curl -X PUT "http://localhost:8080/api/admin/api-keys/abc123def456ghi789jkl012mno345pq/rate-limit?rateLimitPerMinute=200"
```

**Ответ**:
```json
{
  "status": "success",
  "message": "Rate limit updated successfully"
}
```

## Функциональность

### 1. Автоматическая генерация рекомендаций

При получении VAS >= 4 модуль автоматически вызывает `nurseService.createRecommendation(patientMrn)` для генерации рекомендации по лечению на основе Treatment Protocol.

**Логика**:
```java
if (externalVas.getVasLevel() >= 4) {
    try {
        nurseService.createRecommendation(patientMrn);
        log.info("Recommendation generated automatically for patient: {}", patientMrn);
    } catch (Exception e) {
        log.error("Failed to generate recommendation: {}", e.getMessage());
        // VAS уже сохранен, не бросаем исключение
    }
}
```

### 2. Безопасность

**API ключи**:
- Генерация уникальных ключей (UUID без дефисов, 32 символа)
- Срок действия (опционально)
- Статус активности (active/inactive)
- Статистика использования (lastUsedAt, usageCount)

**IP Whitelist**:
- Список разрешенных IP адресов через запятую
- Wildcard "*" для разрешения любого IP
- Автоматическое извлечение IP клиента с поддержкой X-Forwarded-For

**Rate Limiting**:
- Ограничение запросов в минуту (настраивается для каждого ключа)
- По умолчанию: 100 запросов/минуту

**Маскировка ключей в логах**:
- В логах показываются только первые 8 символов ключа
- Пример: `abc12345****` вместо `abc123def456ghi789jkl012mno345pq`

### 3. Мониторинг и логирование

**Логирование операций**:
```
INFO: POST /api/external/vas/record - apiKey: abc12345****, contentType: application/json
INFO: Selected parser: JsonVasParser
INFO: Successfully parsed VAS data: patientMrn=EMR-12345678, vasLevel=7, format=JSON
INFO: VAS record saved: vasId=123, patientMrn=EMR-12345678, vasLevel=7
INFO: Recommendation generated automatically for patient: EMR-12345678
```

**Логирование ошибок**:
```
WARN: API key not found or inactive: abc12345****
WARN: IP not whitelisted: 192.168.1.200 for API key: abc12345****
ERROR: No suitable parser found for contentType: application/unknown
ERROR: Patient not found with MRN: EMR-99999999
```

### 4. Обработка ошибок

**Типы ошибок**:
- **401 Unauthorized** - невалидный API ключ или IP не в whitelist
- **400 Bad Request** - ошибка парсинга данных (неподдерживаемый формат)
- **404 Not Found** - пациент не найден по MRN
- **500 Internal Server Error** - внутренняя ошибка сервера

**Формат ответа при ошибке**:
```json
{
  "error": "Parse error",
  "message": "Unsupported data format. Supported formats: JSON, XML, HL7 v2, FHIR, CSV"
}
```

## Технические решения

### 1. Паттерн Strategy для парсеров

**Интерфейс VasFormatParser**:
```java
public interface VasFormatParser {
    boolean canParse(String contentType, String rawData);
    ExternalVasRecordRequest parse(String rawData) throws ParseException;
    int getPriority();
}
```

**Реализации**:
- JsonVasParser (приоритет 10)
- XmlVasParser (приоритет 9)
- Hl7VasParser (приоритет 8)
- FhirVasParser (приоритет 7)
- CsvVasParser (приоритет 6)

**Фабрика VasParserFactory**:
```java
public ExternalVasRecordRequest parse(String contentType, String rawData) {
    List<VasFormatParser> suitableParsers = parsers.stream()
        .filter(parser -> parser.canParse(contentType, rawData))
        .sorted(Comparator.comparingInt(VasFormatParser::getPriority))
        .toList();
    
    VasFormatParser selectedParser = suitableParsers.get(0);
    return selectedParser.parse(rawData);
}
```

### 2. Транзакционность

Все методы сервисов помечены `@Transactional` для обеспечения атомарности операций:
```java
@Transactional
public Long processExternalVasRecord(ExternalVasRecordRequest externalVas) {
    // 1. Найти пациента
    // 2. Создать VAS запись
    // 3. Генерация рекомендации (опционально)
}
```

### 3. Интеграция с другими модулями

**Common/Patients**:
```java
Patient patient = patientRepository.findByMrn(externalVas.getPatientMrn())
    .orElseThrow(() -> new RuntimeException("Patient not found"));
    
Vas vas = new Vas();
vas.setPatient(patient);
vas.setVasLevel(externalVas.getVasLevel());
vasRepository.save(vas);
```

**Nurse Service**:
```java
if (externalVas.getVasLevel() >= 4) {
    nurseService.createRecommendation(patient.getMrn());
}
```

**Analytics Module**:
- Все операции автоматически логируются через LoggingAspect
- События сохраняются в MongoDB для аналитики

## Примеры использования

### Сценарий 1: VAS монитор в палате

**Система**: VAS Monitor Ward A  
**Формат**: JSON  
**Частота**: каждые 15 минут

```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: abc123def456ghi789jkl012mno345pq" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "EMR-12345678",
    "vasLevel": 7,
    "deviceId": "MONITOR-WARD-A-001",
    "location": "Ward A, Bed 12",
    "source": "VAS_MONITOR"
  }'
```

### Сценарий 2: Планшет медсестры

**Система**: Nurse Tablet Station  
**Формат**: JSON  
**Частота**: по требованию

```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: xyz789abc123def456ghi012jkl345mn" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "EMR-87654321",
    "vasLevel": 4,
    "deviceId": "TABLET-NURSE-002",
    "location": "ICU-3",
    "notes": "Patient reports moderate pain after medication",
    "source": "MANUAL_ENTRY"
  }'
```

### Сценарий 3: Интеграция с больничной EMR системой

**Система**: Hospital EMR System  
**Формат**: HL7 v2  
**Частота**: real-time

```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: emr456def789ghi012jkl345mno678pq" \
  -H "Content-Type: application/hl7-v2" \
  -d 'MSH|^~\&|EMR_SYSTEM|HOSPITAL|PAIN_MGMT|HOSPITAL|20251020143000||ORU^R01|MSG001|P|2.5
PID|||EMR-11223344||Smith^Jane||19750515|F
OBX|1|NM|38208-5^Pain severity^LN||9|{score}|0-10||||F|||20251020143000'
```

### Сценарий 4: Batch импорт из файла

**Система**: Data Import Service  
**Формат**: CSV  
**Частота**: ежедневно

```bash
curl -X POST http://localhost:8080/api/external/vas/batch \
  -H "X-API-Key: batch789ghi012jkl345mno678pqr901st" \
  -H "Content-Type: text/csv" \
  --data-binary @daily_vas_records.csv
```

## Быстрый старт

### Шаг 1: Генерация API ключа

```bash
curl -X POST "http://localhost:8080/api/admin/api-keys/generate?systemName=Test%20System&description=Testing&expiresInDays=30&createdBy=admin"
```

Сохраните полученный `apiKey`.

### Шаг 2: Настройка IP whitelist (опционально)

```bash
curl -X PUT "http://localhost:8080/api/admin/api-keys/{apiKey}/whitelist?ipWhitelist=192.168.1.100"
```

### Шаг 3: Отправка тестовой VAS записи

```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "EMR-12345678",
    "vasLevel": 7,
    "deviceId": "TEST-DEVICE",
    "location": "Test Ward",
    "source": "TEST"
  }'
```

### Шаг 4: Проверка health check

```bash
curl -X GET http://localhost:8080/api/external/vas/health
```

## Troubleshooting

### Проблема: 401 Unauthorized

**Причина**: Невалидный API ключ или IP не в whitelist

**Решение**:
1. Проверьте правильность API ключа
2. Убедитесь, что ключ активен: `GET /api/admin/api-keys`
3. Проверьте IP whitelist: должен содержать ваш IP или "*"
4. Проверьте срок действия ключа (expiresAt)

### Проблема: 400 Bad Request - Parse error

**Причина**: Неподдерживаемый формат данных

**Решение**:
1. Проверьте Content-Type заголовок
2. Убедитесь, что данные соответствуют формату
3. Проверьте структуру JSON/XML/HL7/FHIR
4. Для CSV проверьте наличие заголовков и правильность разделителей

### Проблема: 404 Not Found - Patient not found

**Причина**: Пациент с указанным MRN не существует в системе

**Решение**:
1. Проверьте правильность MRN (формат: EMR-XXXXXXXX)
2. Убедитесь, что пациент зарегистрирован в системе
3. Используйте `GET /api/nurse/patients/search?mrn={mrn}` для проверки

### Проблема: Рекомендация не генерируется автоматически

**Причина**: VAS < 4 или ошибка в Treatment Protocol Service

**Решение**:
1. Проверьте уровень VAS (должен быть >= 4)
2. Проверьте логи: `Failed to generate recommendation`
3. Убедитесь, что у пациента есть EMR с необходимыми данными
4. Проверьте работу NurseService.createRecommendation()

## Заключение

Модуль внешней интеграции VAS обеспечивает универсальный и безопасный способ приема данных об уровне боли из различных внешних систем. Поддержка множественных форматов, система API ключей и автоматическая генерация рекомендаций делают модуль готовым к интеграции с реальными медицинскими системами.
