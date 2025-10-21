# Руководство по тестированию модуля внешней интеграции VAS

## Содержание

1. [Подготовка к тестированию](#подготовка-к-тестированию)
2. [Тестирование управления API ключами](#тестирование-управления-api-ключами)
3. [Тестирование приема VAS данных](#тестирование-приема-vas-данных)
4. [Тестирование различных форматов](#тестирование-различных-форматов)
5. [Тестирование безопасности](#тестирование-безопасности)
6. [Тестирование batch импорта](#тестирование-batch-импорта)
7. [Тестирование автоматической генерации рекомендаций](#тестирование-автоматической-генерации-рекомендаций)
8. [Чеклист тестирования](#чеклист-тестирования)

## Подготовка к тестированию

### 1. Запуск приложения

```bash
cd C:\backend_projects\pain_managment_back
mvn spring-boot:run
```

Дождитесь сообщения: `Started PainHelperBackApplication`

### 2. Проверка health check

```bash
curl -X GET http://localhost:8080/api/external/vas/health
```

**Ожидаемый ответ**:
```json
{
  "status": "UP",
  "module": "External VAS Integration",
  "timestamp": "2025-10-20T19:45:00"
}
```

### 3. Создание тестового пациента

Если у вас нет пациента в системе, создайте его:

```bash
curl -X POST http://localhost:8080/api/emr/mock/generate
```

Сохраните полученный `mrn` (например, `EMR-A1B2C3D4`).

## Тестирование управления API ключами

### Тест 1: Генерация нового API ключа

**Цель**: Проверить создание API ключа для внешней системы.

**Запрос**:
```bash
curl -X POST "http://localhost:8080/api/admin/api-keys/generate?systemName=VAS%20Monitor%20Test&description=Test%20monitoring%20system&expiresInDays=30&createdBy=admin"
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "message": "API key generated successfully",
  "apiKey": "abc123def456ghi789jkl012mno345pq",
  "systemName": "VAS Monitor Test",
  "expiresAt": "2025-11-19T19:45:00",
  "ipWhitelist": "*",
  "rateLimitPerMinute": 100
}
```

**Проверка**:
- ✅ Ключ сгенерирован (32 символа)
- ✅ Срок действия = текущая дата + 30 дней
- ✅ IP whitelist по умолчанию = "*"
- ✅ Rate limit по умолчанию = 100

**Сохраните полученный `apiKey` для дальнейших тестов!**

### Тест 2: Получение всех активных ключей

**Запрос**:
```bash
curl -X GET http://localhost:8080/api/admin/api-keys
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "total": 1,
  "keys": [
    {
      "apiKey": "abc123def456ghi789jkl012mno345pq",
      "systemName": "VAS Monitor Test",
      "active": true,
      "usageCount": 0
    }
  ]
}
```

**Проверка**:
- ✅ Список содержит созданный ключ
- ✅ Ключ активен (active: true)
- ✅ Счетчик использования = 0

### Тест 3: Обновление IP whitelist

**Запрос**:
```bash
curl -X PUT "http://localhost:8080/api/admin/api-keys/{apiKey}/whitelist?ipWhitelist=127.0.0.1,192.168.1.100"
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "message": "IP whitelist updated successfully"
}

```

**Проверка**:
```bash
curl -X GET http://localhost:8080/api/admin/api-keys
```
Убедитесь, что `ipWhitelist` обновлен.

### Тест 4: Обновление rate limit

**Запрос**:
```bash
curl -X PUT "http://localhost:8080/api/admin/api-keys/{apiKey}/rate-limit?rateLimitPerMinute=200"
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "message": "Rate limit updated successfully"
}
```

### Тест 5: Деактивация ключа

**Запрос**:
```bash
curl -X DELETE http://localhost:8080/api/admin/api-keys/{apiKey}
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "message": "API key deactivated successfully"
}
```

**Проверка**:
```bash
curl -X GET http://localhost:8080/api/admin/api-keys
```
Список должен быть пустым (total: 0).

**⚠️ ВАЖНО**: Создайте новый ключ для дальнейших тестов!

## Тестирование приема VAS данных

### Тест 6: Запись VAS в формате JSON

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "EMR-A1B2C3D4",
    "vasLevel": 7,
    "deviceId": "MONITOR-001",
    "location": "Ward A, Bed 12",
    "notes": "Patient complains of severe pain",
    "source": "VAS_MONITOR"
  }'
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "vasId": 1,
  "patientMrn": "EMR-A1B2C3D4",
  "vasLevel": 7,
  "format": "JSON"
}
```

**Проверка**:
- ✅ VAS запись создана (vasId присвоен)
- ✅ Формат определен как JSON
- ✅ В логах: "Successfully parsed VAS data using JsonVasParser"
- ✅ В логах: "VAS record saved: vasId=1"
- ✅ В логах: "Recommendation generated automatically" (т.к. VAS >= 4)

### Тест 7: Проверка сохранения VAS в БД

**Запрос**:
```bash
curl -X GET "http://localhost:8080/api/nurse/patients/search?mrn=EMR-A1B2C3D4"
```

**Проверка**:
- ✅ У пациента есть VAS записи
- ✅ Последняя VAS запись имеет vasLevel = 7
- ✅ location = "Ward A, Bed 12"
- ✅ recordedBy = "EXTERNAL_VAS_MONITOR"

## Тестирование различных форматов

### Тест 8: XML формат

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/xml" \
  -d '<vasRecord>
  <patientMrn>EMR-A1B2C3D4</patientMrn>
  <vasLevel>5</vasLevel>
  <deviceId>MONITOR-002</deviceId>
  <location>ICU-3</location>
  <notes>Moderate pain</notes>
  <source>VAS_MONITOR</source>
</vasRecord>'
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "vasId": 2,
  "patientMrn": "EMR-A1B2C3D4",
  "vasLevel": 5,
  "format": "XML"
}
```

**Проверка**:
- ✅ Формат определен как XML
- ✅ В логах: "Selected parser: XmlVasParser"

### Тест 9: HL7 v2 формат

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/hl7-v2" \
  -d 'MSH|^~\&|VAS_SYSTEM|HOSPITAL|PAIN_MGMT|HOSPITAL|20251020143000||ORU^R01|MSG001|P|2.5
PID|||EMR-A1B2C3D4||Doe^John||19800101|M
OBX|1|NM|38208-5^Pain severity^LN||8|{score}|0-10||||F|||20251020143000'
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "vasId": 3,
  "patientMrn": "EMR-A1B2C3D4",
  "vasLevel": 8,
  "format": "HL7_V2"
}
```

**Проверка**:
- ✅ Формат определен как HL7_V2
- ✅ В логах: "Selected parser: Hl7VasParser"
- ✅ VAS извлечен из OBX сегмента

### Тест 10: FHIR формат

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/fhir+json" \
  -d '{
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
    "reference": "Patient/EMR-A1B2C3D4"
  },
  "effectiveDateTime": "2025-10-20T14:30:00Z",
  "valueInteger": 6,
  "device": {
    "display": "MONITOR-003"
  }
}'
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "vasId": 4,
  "patientMrn": "EMR-A1B2C3D4",
  "vasLevel": 6,
  "format": "FHIR"
}
```

**Проверка**:
- ✅ Формат определен как FHIR
- ✅ В логах: "Selected parser: FhirVasParser"

### Тест 11: Автоматическое определение формата (без Content-Type)

**Запрос** (JSON без Content-Type):
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -d '{
    "patientMrn": "EMR-A1B2C3D4",
    "vasLevel": 4
  }'
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "vasId": 5,
  "patientMrn": "EMR-A1B2C3D4",
  "vasLevel": 4,
  "format": "JSON"
}
```

**Проверка**:
- ✅ Формат автоматически определен как JSON по структуре данных

## Тестирование безопасности

### Тест 12: Невалидный API ключ

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: invalid_key_123" \
  -H "Content-Type: application/json" \
  -d '{"patientMrn": "EMR-A1B2C3D4", "vasLevel": 5}'
```

**Ожидаемый результат**:
```json
{
  "error": "Invalid API key or IP not whitelisted"
}
```
HTTP Status: 401 Unauthorized

**Проверка**:
- ✅ Запрос отклонен
- ✅ В логах: "API key not found or inactive: invalid_****"

### Тест 13: Отсутствие API ключа

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "Content-Type: application/json" \
  -d '{"patientMrn": "EMR-A1B2C3D4", "vasLevel": 5}'
```

**Ожидаемый результат**: Ошибка 400 или 401

### Тест 14: IP не в whitelist

**Шаг 1**: Обновите whitelist на конкретный IP (не ваш):
```bash
curl -X PUT "http://localhost:8080/api/admin/api-keys/{apiKey}/whitelist?ipWhitelist=192.168.1.200"
```

**Шаг 2**: Попробуйте отправить VAS:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/json" \
  -d '{"patientMrn": "EMR-A1B2C3D4", "vasLevel": 5}'
```

**Ожидаемый результат**:
```json
{
  "error": "Invalid API key or IP not whitelisted"
}
```

**Проверка**:
- ✅ В логах: "IP not whitelisted: 127.0.0.1 for API key: abc12345****"

**Шаг 3**: Верните whitelist на "*":
```bash
curl -X PUT "http://localhost:8080/api/admin/api-keys/{apiKey}/whitelist?ipWhitelist=*"
```

### Тест 15: Истекший API ключ

**Шаг 1**: Создайте ключ с истекшим сроком (expiresInDays=0):
```bash
curl -X POST "http://localhost:8080/api/admin/api-keys/generate?systemName=Expired%20Key&expiresInDays=0&createdBy=admin"
```

**Шаг 2**: Попробуйте использовать этот ключ:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {expired_apiKey}" \
  -H "Content-Type: application/json" \
  -d '{"patientMrn": "EMR-A1B2C3D4", "vasLevel": 5}'
```

**Ожидаемый результат**: 401 Unauthorized

**Проверка**:
- ✅ В логах: "API key expired: abc12345****, expiresAt: ..."

## Тестирование batch импорта

### Тест 16: Создание CSV файла

Создайте файл `vas_test_batch.csv`:
```csv
patientMrn,vasLevel,deviceId,location,timestamp,notes,source
EMR-A1B2C3D4,7,MONITOR-001,Ward A Bed 12,2025-10-20T14:30:00,Severe pain,VAS_MONITOR
EMR-A1B2C3D4,5,TABLET-002,ICU-3,2025-10-20T14:35:00,Moderate pain,MANUAL_ENTRY
EMR-A1B2C3D4,8,MONITOR-002,ER-Room-5,2025-10-20T14:40:00,Critical pain,VAS_MONITOR
```

### Тест 17: Batch импорт

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/batch \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: text/csv" \
  --data-binary @vas_test_batch.csv
```

**Ожидаемый результат**:
```json
{
  "status": "success",
  "total": 3,
  "success": 3,
  "failed": 0,
  "createdVasIds": [6, 7, 8]
}
```

**Проверка**:
- ✅ Все 3 записи импортированы
- ✅ Нет ошибок (failed: 0)
- ✅ В логах: "Batch import completed: total=3, success=3, failed=0"

### Тест 18: Batch импорт с ошибками

Создайте файл `vas_test_batch_errors.csv` с несуществующим MRN:
```csv
patientMrn,vasLevel,deviceId,location,timestamp,notes,source
EMR-A1B2C3D4,7,MONITOR-001,Ward A,2025-10-20T14:30:00,OK,VAS_MONITOR
EMR-INVALID,5,MONITOR-002,Ward B,2025-10-20T14:35:00,Error,VAS_MONITOR
EMR-A1B2C3D4,6,MONITOR-003,Ward C,2025-10-20T14:40:00,OK,VAS_MONITOR
```

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/batch \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: text/csv" \
  --data-binary @vas_test_batch_errors.csv
```

**Ожидаемый результат**:
```json
{
  "status": "partial_success",
  "total": 3,
  "success": 2,
  "failed": 1,
  "createdVasIds": [9, 10],
  "errors": [
    "Line 3: Patient not found with MRN: EMR-INVALID (MRN: EMR-INVALID)"
  ]
}
```

**Проверка**:
- ✅ 2 записи импортированы, 1 ошибка
- ✅ Массив errors содержит описание ошибки
- ✅ В логах: "Failed to process CSV line 3: ..."

## Тестирование автоматической генерации рекомендаций

### Тест 19: VAS >= 4 генерирует рекомендацию

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "EMR-A1B2C3D4",
    "vasLevel": 7
  }'
```

**Проверка**:
1. В логах: "Recommendation generated automatically for patient: EMR-A1B2C3D4"
2. Проверьте рекомендации пациента:
```bash
curl -X GET "http://localhost:8080/api/nurse/patients/search?mrn=EMR-A1B2C3D4"
```
3. У пациента должна быть новая рекомендация со статусом PENDING

### Тест 20: VAS < 4 не генерирует рекомендацию

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "EMR-A1B2C3D4",
    "vasLevel": 2
  }'
```

**Проверка**:
- ✅ VAS сохранен
- ✅ В логах НЕТ сообщения "Recommendation generated automatically"
- ✅ Количество рекомендаций не изменилось

## Тестирование обработки ошибок

### Тест 21: Несуществующий пациент

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "EMR-NOTEXIST",
    "vasLevel": 5
  }'
```

**Ожидаемый результат**:
```json
{
  "error": "Patient not found with MRN: EMR-NOTEXIST"
}
```
HTTP Status: 500

### Тест 22: Неподдерживаемый формат

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/unknown" \
  -d 'some random data'
```

**Ожидаемый результат**:
```json
{
  "error": "Parse error",
  "message": "Unsupported data format. Supported formats: JSON, XML, HL7 v2, FHIR, CSV"
}
```
HTTP Status: 400

### Тест 23: Некорректный JSON

**Запрос**:
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: {ваш_apiKey}" \
  -H "Content-Type: application/json" \
  -d '{invalid json'
```

**Ожидаемый результат**: 400 Bad Request с сообщением об ошибке парсинга

## Чеклист тестирования

### Управление API ключами
- [ ] Генерация нового API ключа
- [ ] Получение всех активных ключей
- [ ] Обновление IP whitelist
- [ ] Обновление rate limit
- [ ] Деактивация ключа

### Прием VAS данных
- [ ] Запись VAS в формате JSON
- [ ] Запись VAS в формате XML
- [ ] Запись VAS в формате HL7 v2
- [ ] Запись VAS в формате FHIR
- [ ] Автоматическое определение формата
- [ ] Проверка сохранения в БД

### Безопасность
- [ ] Невалидный API ключ (401)
- [ ] Отсутствие API ключа (401)
- [ ] IP не в whitelist (401)
- [ ] Истекший API ключ (401)
- [ ] Маскировка ключей в логах

### Batch импорт
- [ ] Успешный импорт всех записей
- [ ] Импорт с ошибками (partial_success)
- [ ] Статистика импорта (total, success, failed)

### Автоматическая генерация рекомендаций
- [ ] VAS >= 4 генерирует рекомендацию
- [ ] VAS < 4 не генерирует рекомендацию
- [ ] Ошибка генерации не блокирует сохранение VAS

### Обработка ошибок
- [ ] Несуществующий пациент (500)
- [ ] Неподдерживаемый формат (400)
- [ ] Некорректный JSON/XML (400)
- [ ] Детальные сообщения об ошибках

### Мониторинг
- [ ] Health check работает
- [ ] Логирование всех операций
- [ ] Маскировка API ключей в логах
- [ ] Статистика использования ключей (usageCount, lastUsedAt)

## Заключение

После прохождения всех тестов модуль внешней интеграции VAS должен:
- ✅ Принимать VAS данные в 5 форматах
- ✅ Автоматически определять формат данных
- ✅ Валидировать API ключи и IP whitelist
- ✅ Сохранять VAS в БД
- ✅ Автоматически генерировать рекомендации при VAS >= 4
- ✅ Обрабатывать batch импорт из CSV
- ✅ Корректно обрабатывать ошибки
- ✅ Логировать все операции

Модуль готов к интеграции с внешними системами!
