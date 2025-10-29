# 🧪 EXTERNAL VAS INTEGRATION - ТЕСТОВЫЕ ДАННЫЕ

## 📋 ПОДГОТОВКА К ПРЕЗЕНТАЦИИ

### 1. Создать тестовых пациентов

**Через Mock Generator:**
```bash
POST http://localhost:8080/api/emr/mock/generate-batch?count=5&createdBy=demo
```

**Или вручную через Postman:**
```json
POST http://localhost:8080/api/nurse/patients

{
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1985-05-15",
  "gender": "MALE",
  "weight": 75.0,
  "height": 180.0
}
```

**Запомнить MRN пациентов:**
- MRN-42: John Doe
- MRN-43: Jane Smith
- MRN-44: Bob Wilson
- MRN-45: Alice Johnson
- MRN-46: Charlie Brown

---

### 2. Сгенерировать API ключ

**Через Frontend:**
```
Admin → API Key Management → Generate New Key

Form:
- System Name: "Demo VAS Monitors"
- Description: "Presentation demo devices for External VAS Integration"
- Expires In Days: (оставить пустым - Never)
- IP Whitelist: * (любые IP)
- Rate Limit: 120

Кнопка: Generate
```

**Скопировать сгенерированный ключ:**
```
pma_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
```

⚠️ **ВАЖНО:** Сохранить ключ сразу! Он показывается только один раз.

---

## 🎬 СЦЕНАРИЙ 1: Single VAS Record (Device Simulator)

### Шаг 1: Отправить VAS через Simulator

**Frontend:** Nurse Dashboard → External VAS Monitor → Device Simulator Tab

```
Patient MRN: MRN-42
VAS Level: 8 (переместить slider)
Device ID: MONITOR-001
Location: Ward A, Bed 12
Notes: Patient reports severe pain in lower back after surgery
```

**Кнопка:** Send VAS Record

### Ожидаемый результат:

```json
✅ Success Response:
{
  "status": "success",
  "vasId": 123,
  "patientMrn": "MRN-42",
  "vasLevel": 8,
  "format": "JSON"
}
```

### Шаг 2: Проверить в Monitor

**Frontend:** External VAS Monitor → Monitor Tab

**Таблица должна показать:**
```
Time       | MRN    | Name     | VAS | Device      | Location
Just now   | MRN-42 | John Doe | 🔴8 | MONITOR-001 | Ward A, Bed 12
```

**Статистика обновилась:**
```
Total Records Today: 1
Average VAS: 8.0
High Pain Alerts: 1
Active Devices: 1
```

---

## 🎬 СЦЕНАРИЙ 2: Batch Import (Postman/curl)

### Подготовить CSV файл: `vas_batch_demo.csv`

```csv
patientMrn,vasLevel,deviceId,location,timestamp
MRN-42,7,MONITOR-001,Ward A,2025-10-26T08:00:00
MRN-42,6,MONITOR-001,Ward A,2025-10-26T09:00:00
MRN-42,8,MONITOR-001,Ward A,2025-10-26T10:00:00
MRN-43,5,MONITOR-002,Ward B,2025-10-26T08:30:00
MRN-43,6,MONITOR-002,Ward B,2025-10-26T09:30:00
MRN-43,8,MONITOR-002,Ward B,2025-10-26T10:30:00
MRN-44,9,MONITOR-003,ICU-1,2025-10-26T08:15:00
MRN-44,7,MONITOR-003,ICU-1,2025-10-26T09:15:00
MRN-44,6,MONITOR-003,ICU-1,2025-10-26T10:15:00
MRN-45,4,TABLET-001,Ward A,2025-10-26T08:45:00
MRN-45,5,TABLET-001,Ward A,2025-10-26T09:45:00
MRN-46,3,TABLET-002,Ward C,2025-10-26T08:20:00
MRN-46,4,TABLET-002,Ward C,2025-10-26T09:20:00
MRN-46,7,TABLET-002,Ward C,2025-10-26T10:20:00
```

### Отправить через Postman

**Request:**
```
POST http://localhost:8080/api/external/vas/batch
Headers:
  X-API-Key: pma_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
  Content-Type: text/csv

Body (raw):
[вставить содержимое CSV файла]
```

**Или через curl:**
```bash
curl -X POST http://localhost:8080/api/external/vas/batch \
  -H "X-API-Key: pma_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6" \
  -H "Content-Type: text/csv" \
  --data-binary @vas_batch_demo.csv
```

### Ожидаемый результат:

```json
{
  "status": "success",
  "total": 14,
  "success": 14,
  "failed": 0,
  "createdVasIds": [124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137]
}
```

### Проверить в Monitor

**Frontend:** External VAS Monitor → Refresh (или auto-refresh)

**Статистика обновилась:**
```
Total Records Today: 15 (1 single + 14 batch)
Average VAS: 6.3
High Pain Alerts: 6 (VAS >= 7)
Active Devices: 5 (MONITOR-001, MONITOR-002, MONITOR-003, TABLET-001, TABLET-002)
```

---

## 🎬 СЦЕНАРИЙ 3: Фильтрация данных

### Фильтр 1: По Device ID

**Frontend:** Device Filter → Select "MONITOR-001"

**Ожидаемый результат:**
```
Показаны только записи:
- MRN-42 | VAS 7 | MONITOR-001 | Ward A | 08:00
- MRN-42 | VAS 6 | MONITOR-001 | Ward A | 09:00
- MRN-42 | VAS 8 | MONITOR-001 | Ward A | 10:00

Total: 3 records
```

### Фильтр 2: По Location

**Frontend:** Location Filter → Type "Ward A"

**Ожидаемый результат:**
```
Показаны записи из Ward A:
- MONITOR-001 (3 записи)
- TABLET-001 (2 записи)

Total: 5 records
```

### Фильтр 3: По VAS Level (High Pain)

**Frontend:** VAS Level Range → Min: 7, Max: 10

**Ожидаемый результат:**
```
Показаны только записи с VAS >= 7:
- MRN-42 | VAS 8 | MONITOR-001
- MRN-42 | VAS 7 | MONITOR-001
- MRN-43 | VAS 8 | MONITOR-002
- MRN-44 | VAS 9 | MONITOR-003
- MRN-44 | VAS 7 | MONITOR-003
- MRN-46 | VAS 7 | TABLET-002

Total: 6 records (High Pain Alerts)
```

### Фильтр 4: По Time Range

**Frontend:** Time Range → "Last 1 hour"

**Ожидаемый результат:**
```
Показаны только записи за последний час
(зависит от текущего времени)
```

### Сброс фильтров

**Кнопка:** Clear Filters

**Ожидаемый результат:**
```
Показаны все 15 записей
```

---

## 🎬 СЦЕНАРИЙ 4: Real-time Updates

### Включить Auto-refresh

**Frontend:** ☑️ Auto-refresh every 30 seconds

### Отправить новые VAS записи

**Каждые 10 секунд через Device Simulator:**

```
Запись 1:
- Patient MRN: MRN-42
- VAS Level: 9
- Device ID: MONITOR-001
- Location: Ward A, Bed 12

Запись 2 (через 10 сек):
- Patient MRN: MRN-43
- VAS Level: 7
- Device ID: MONITOR-002
- Location: Ward B, Bed 5

Запись 3 (через 10 сек):
- Patient MRN: MRN-44
- VAS Level: 8
- Device ID: MONITOR-003
- Location: ICU-1, Bed 3
```

### Ожидаемый результат:

```
✅ Таблица обновляется автоматически каждые 30 секунд
✅ Новые записи появляются вверху (DESC по времени)
✅ Статистика пересчитывается
✅ Toast notifications при новых записях
🚨 Alert при VAS >= 7
```

---

## 🎬 СЦЕНАРИЙ 5: Pain Escalation Integration

### Отправить критический VAS

**Device Simulator:**
```
Patient MRN: MRN-42
VAS Level: 9
Device ID: MONITOR-001
Location: Ward A, Bed 12
Notes: CRITICAL - Patient in severe pain, requesting immediate assistance
```

### Проверить автоматическую цепочку

**1. VAS Record создан:**
```
✅ POST /api/external/vas/record
Response: vasId = 138
```

**2. Pain Escalation проверка:**
```
✅ Автоматически вызван: painEscalationService.handleNewVasRecord(MRN-42, 9)
```

**3. Escalation создана:**
```
Frontend: Escalation Dashboard

Новая эскалация:
- Patient: MRN-42 (John Doe)
- Priority: 🔴 CRITICAL
- Reason: "VAS level 9 detected from external device MONITOR-001"
- Status: PENDING
- Created: Just now
```

**4. Recommendation сгенерирована:**
```
✅ Автоматически создана рекомендация (VAS >= 4)
Frontend: Recommendations → Pending

New Recommendation:
- Patient: MRN-42
- Status: PENDING_APPROVAL
- Generated: Just now
```

**5. Notification отправлена:**
```
✅ WebSocket notification отправлена анестезиологу
```

---

## 🎬 СЦЕНАРИЙ 6: API Key Management

### Просмотр ключей

**Frontend:** Admin → API Key Management

**Таблица:**
```
System Name       | API Key          | Expires | IP Whitelist | Rate | Status | Usage
Demo VAS Monitors | pma_live_a1b2*** | Never   | *            | 120  | ✅     | 15
```

### Обновить IP Whitelist

**Actions → Edit → IP Whitelist:**
```
New Value: 192.168.1.0/24,10.0.0.0/8

Кнопка: Update
```

**Ожидаемый результат:**
```
✅ IP whitelist updated successfully
Новое значение: 192.168.1.0/24,10.0.0.0/8
```

### Обновить Rate Limit

**Actions → Edit → Rate Limit:**
```
New Value: 60

Кнопка: Update
```

**Ожидаемый результат:**
```
✅ Rate limit updated successfully
Новое значение: 60/min
```

### Тест Rate Limiting

**Отправить 61 запрос за минуту через Postman:**
```bash
for i in {1..61}; do
  curl -X POST http://localhost:8080/api/external/vas/record \
    -H "X-API-Key: pma_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6" \
    -H "Content-Type: application/json" \
    -d '{"patientMrn":"MRN-42","vasLevel":5,"deviceId":"TEST","location":"Test"}' &
done
```

**Ожидаемый результат:**
```
Первые 60 запросов: ✅ 201 Created
61-й запрос: ❌ 429 Too Many Requests
{
  "error": "Rate limit exceeded"
}
```

### Деактивировать ключ

**Actions → Deactivate:**
```
Confirm: Yes
```

**Ожидаемый результат:**
```
✅ API key deactivated successfully
Status: ❌ Inactive
```

**Попытка использовать деактивированный ключ:**
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: pma_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6" \
  -H "Content-Type: application/json" \
  -d '{"patientMrn":"MRN-42","vasLevel":5,"deviceId":"TEST","location":"Test"}'
```

**Ожидаемый результат:**
```
❌ 401 Unauthorized
{
  "error": "Invalid API key or IP not whitelisted"
}
```

---

## 📊 ФИНАЛЬНАЯ СТАТИСТИКА (после всех тестов)

### GET /api/external/vas/stats

```json
{
  "totalRecordsToday": 18,
  "averageVas": 6.5,
  "highPainAlerts": 8,
  "activeDevices": 5
}
```

### GET /api/external/vas/records

```json
[
  {
    "id": 138,
    "patientMrn": "MRN-42",
    "patientFirstName": "John",
    "patientLastName": "Doe",
    "vasLevel": 9,
    "deviceId": "MONITOR-001",
    "location": "Ward A, Bed 12",
    "timestamp": "2025-10-26T12:00:00",
    "notes": "CRITICAL - Patient in severe pain",
    "source": "MONITOR-001",
    "createdAt": "2025-10-26T12:00:05"
  },
  // ... еще 17 записей
]
```

---

## ✅ CHECKLIST ПЕРЕД ПРЕЗЕНТАЦИЕЙ

### Backend
- [ ] Сервер запущен (port 8080)
- [ ] База данных доступна
- [ ] Созданы тестовые пациенты (5 шт)
- [ ] Проверены все endpoints:
  - [ ] POST /api/external/vas/record
  - [ ] POST /api/external/vas/batch
  - [ ] GET /api/external/vas/records
  - [ ] GET /api/external/vas/stats
  - [ ] GET /api/external/vas/health
  - [ ] POST /api/admin/api-keys/generate
  - [ ] GET /api/admin/api-keys

### Frontend
- [ ] Приложение запущено (port 3000)
- [ ] Открыты вкладки:
  - [ ] External VAS Monitor
  - [ ] API Key Management
  - [ ] Escalation Dashboard
  - [ ] Device Simulator
- [ ] Проверена компиляция (no errors)

### Данные
- [ ] API ключ сгенерирован и сохранен
- [ ] CSV файл подготовлен
- [ ] Postman collection готов
- [ ] Тестовые пациенты созданы

### Демо
- [ ] Проверен single VAS record
- [ ] Проверен batch import
- [ ] Проверены фильтры
- [ ] Проверен auto-refresh
- [ ] Проверена эскалация
- [ ] Проверен API key management

---

## 🎯 ПРЕЗЕНТАЦИОННЫЕ ФИШКИ

### 1. Color Coding (покажи визуально)
```
🟢 VAS 0-3: зеленый (низкая боль)
🟡 VAS 4-6: желтый (средняя боль)
🔴 VAS 7-10: красный (высокая боль)
```

### 2. Source Badges
```
🔵 VAS_MONITOR (синий)
🟣 EMR_SYSTEM (фиолетовый)
⚪ MANUAL_ENTRY (серый)
🟢 TABLET (зеленый)
```

### 3. Real-time Indicators
```
⏰ "Just now" → "2 minutes ago" → "5 minutes ago"
🔄 Auto-refresh countdown: "Next refresh in 25s"
🔔 Toast notification при новой записи
```

### 4. Statistics Cards
```
┌─────────────────┐
│ 📊 18           │
│ Total Records   │
└─────────────────┘

┌─────────────────┐
│ 📈 6.5          │
│ Average VAS     │
└─────────────────┘

┌─────────────────┐
│ 🚨 8            │
│ High Pain       │
└─────────────────┘

┌─────────────────┐
│ 🖥️ 5            │
│ Active Devices  │
└─────────────────┘
```

---

## 🔧 TROUBLESHOOTING

### Проблема: API Key не работает
```
Решение:
1. Проверить что ключ активен (Status: Active)
2. Проверить IP whitelist (должен быть "*" для теста)
3. Проверить expiration date
4. Проверить rate limit
5. Проверить что ключ скопирован полностью
```

### Проблема: VAS не появляется в мониторе
```
Решение:
1. Проверить что пациент существует (MRN)
2. Проверить логи backend (VAS saved?)
3. Обновить страницу (F5)
4. Проверить фильтры (Clear Filters)
5. Проверить что recordedBy начинается с "EXTERNAL_"
```

### Проблема: Статистика = 0
```
Решение:
1. Проверить что записи созданы сегодня
2. Проверить что recordedBy = "EXTERNAL_*"
3. Проверить timezone (LocalDateTime.now())
4. Обновить страницу
```

### Проблема: Batch import failed
```
Решение:
1. Проверить формат CSV (правильные заголовки)
2. Проверить что все MRN существуют
3. Проверить timestamp format (ISO 8601)
4. Проверить VAS level (0-10)
5. Посмотреть errors в response
```

---

## 🎬 ФИНАЛЬНЫЙ СЦЕНАРИЙ (30 минут)

**00:00-00:05** - Введение
- Показать архитектуру External VAS Integration
- Объяснить зачем нужно (автоматизация, real-time)

**00:05-00:10** - API Key Management
- Сгенерировать ключ
- Показать security features (IP whitelist, rate limit)
- Показать usage statistics

**00:10-00:15** - Single VAS Record
- Отправить через Device Simulator
- Показать в Monitor
- Показать автоматическую эскалацию

**00:15-00:20** - Batch Import
- Отправить CSV через Postman
- Показать результаты
- Показать обновленную статистику

**00:20-00:25** - Фильтрация и Real-time
- Показать все фильтры
- Включить auto-refresh
- Отправить несколько VAS в реальном времени

**00:25-00:30** - Integration с Pain Escalation
- Отправить критический VAS (9)
- Показать цепочку: VAS → Escalation → Recommendation
- Показать Escalation Dashboard

**ИТОГО:** Полная демонстрация всех возможностей External VAS Integration!
