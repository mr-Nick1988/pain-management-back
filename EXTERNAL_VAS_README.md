# 🏥 EXTERNAL VAS INTEGRATION - README

## 🎯 ЧТО ЭТО?

**External VAS Integration** - модуль для автоматического получения данных о боли (VAS) с внешних устройств:
- 🖥️ Больничные мониторы боли
- 📱 Планшеты в палатах
- 💉 Мобильные приложения для пациентов
- 🏥 Интеграция с EMR системами других больниц

---

## ✨ КЛЮЧЕВЫЕ ВОЗМОЖНОСТИ

### 🔐 Security
- **API Key Authentication** - безопасная аутентификация
- **IP Whitelist** - ограничение по IP адресам (CIDR notation)
- **Rate Limiting** - защита от DDoS (requests/minute)
- **Expiration** - автоматическое истечение ключей

### 📊 Monitoring
- **Real-time Dashboard** - мониторинг VAS в реальном времени
- **Filtering** - по устройству, локации, времени, уровню боли
- **Statistics** - total, average, high pain alerts, active devices
- **Auto-refresh** - автоматическое обновление каждые 30 секунд

### 🔄 Integration
- **Automatic Pain Escalation** - при VAS >= 7
- **Automatic Recommendation** - при VAS >= 4
- **Event Publishing** - для analytics и reporting
- **Multi-format Support** - JSON, XML, HL7, FHIR, CSV

---

## 🚀 БЫСТРЫЙ СТАРТ

### 1. Создать тестового пациента
```bash
POST http://localhost:8080/api/emr/mock/generate
```

### 2. Сгенерировать API ключ
```
Frontend: Admin → API Key Management → Generate New Key
Скопировать ключ: pma_live_XXXXXXXXXXXXXXXX
```

### 3. Отправить VAS
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: pma_live_XXXXXXXXXXXXXXXX" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "MRN-42",
    "vasLevel": 8,
    "deviceId": "MONITOR-001",
    "location": "Ward A"
  }'
```

### 4. Проверить в Monitor
```
Frontend: Nurse Dashboard → External VAS Monitor
```

**Готово!** ✅

---

## 📚 ДОКУМЕНТАЦИЯ

### Основные документы:
1. **EXTERNAL_VAS_DEMO_GUIDE.md** - полный презентационный гайд (30 мин)
2. **EXTERNAL_VAS_TEST_DATA.md** - тестовые данные и сценарии
3. **EXTERNAL_VAS_QUICK_TEST.md** - быстрый тест (5 мин)
4. **EXTERNAL_VAS_IMPLEMENTATION_STATUS.md** - статус реализации

### API Endpoints:

#### External VAS
- `POST /api/external/vas/record` - single VAS
- `POST /api/external/vas/batch` - batch CSV
- `GET /api/external/vas/records` - список с фильтрами
- `GET /api/external/vas/stats` - статистика
- `GET /api/external/vas/health` - health check

#### API Key Management
- `POST /api/admin/api-keys/generate` - генерация
- `GET /api/admin/api-keys` - список
- `DELETE /api/admin/api-keys/{key}` - деактивация
- `PUT /api/admin/api-keys/{key}/whitelist` - IP whitelist
- `PUT /api/admin/api-keys/{key}/rate-limit` - rate limit

---

## 🧪 ТЕСТИРОВАНИЕ

### Через Device Simulator (Frontend)
```
1. Открыть: Nurse Dashboard → External VAS Monitor → Device Simulator
2. Заполнить форму:
   - Patient MRN: MRN-42
   - VAS Level: 8
   - Device ID: MONITOR-001
   - Location: Ward A
3. Нажать: Send VAS Record
4. Проверить: Monitor Tab
```

### Через Postman
```
1. Import collection из документации
2. Generate API Key
3. Send Single VAS
4. Send Batch CSV
5. Get Records
6. Get Stats
```

### Через curl
```bash
# Single VAS
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{"patientMrn":"MRN-42","vasLevel":8,"deviceId":"MONITOR-001","location":"Ward A"}'

# Batch CSV
curl -X POST http://localhost:8080/api/external/vas/batch \
  -H "X-API-Key: YOUR_KEY" \
  -H "Content-Type: text/csv" \
  --data-binary @vas_batch.csv

# Get Stats
curl http://localhost:8080/api/external/vas/stats
```

---

## 🎬 ПРЕЗЕНТАЦИЯ (10 минут)

### Сценарий:
1. **API Key Management** (2 мин)
   - Сгенерировать ключ
   - Показать security features

2. **Single VAS** (2 мин)
   - Отправить через Simulator
   - Показать в Monitor

3. **Batch Import** (2 мин)
   - Отправить CSV через Postman
   - Показать результаты

4. **Filtering** (2 мин)
   - Фильтр по device
   - Фильтр по VAS level

5. **Pain Escalation** (2 мин)
   - Отправить VAS=9
   - Показать автоматическую эскалацию

---

## 🔧 АРХИТЕКТУРА

### Backend
```
ExternalVasIntegrationController
    ↓
ExternalVasIntegrationService
    ↓
VasRepository → Patient → Vas Entity
    ↓
PainEscalationService (auto-trigger)
    ↓
NurseService (auto-recommendation)
```

### Security Flow
```
Request → API Key Validation
    ↓
IP Whitelist Check
    ↓
Rate Limit Check
    ↓
Expiration Check
    ↓
Process VAS
```

### Data Flow
```
External Device → POST /api/external/vas/record
    ↓
Parse (JSON/XML/HL7/FHIR/CSV)
    ↓
Validate Patient (MRN)
    ↓
Save VAS Entity
    ↓
Publish VasRecordedEvent
    ↓
Auto-check Pain Escalation (if VAS >= 7)
    ↓
Auto-generate Recommendation (if VAS >= 4)
```

---

## 📊 ПРИМЕРЫ ДАННЫХ

### Single VAS (JSON)
```json
{
  "patientMrn": "MRN-42",
  "vasLevel": 8,
  "deviceId": "MONITOR-001",
  "location": "Ward A, Bed 12",
  "timestamp": "2025-10-26T12:00:00",
  "notes": "Patient reports severe pain",
  "source": "VAS_MONITOR"
}
```

### Batch VAS (CSV)
```csv
patientMrn,vasLevel,deviceId,location,timestamp
MRN-42,7,MONITOR-001,Ward A,2025-10-26T08:00:00
MRN-43,5,MONITOR-002,Ward B,2025-10-26T08:30:00
MRN-44,9,MONITOR-003,ICU-1,2025-10-26T08:15:00
```

### Statistics Response
```json
{
  "totalRecordsToday": 15,
  "averageVas": 6.5,
  "highPainAlerts": 6,
  "activeDevices": 5
}
```

---

## 🐛 TROUBLESHOOTING

### API Key не работает
```
✓ Проверить что ключ активен (Status: Active)
✓ Проверить IP whitelist (должен быть "*" для теста)
✓ Проверить expiration date
✓ Проверить что ключ скопирован полностью
```

### VAS не появляется в мониторе
```
✓ Проверить что пациент существует (MRN)
✓ Проверить логи backend (VAS saved?)
✓ Обновить страницу (F5)
✓ Проверить фильтры (Clear Filters)
```

### Статистика = 0
```
✓ Проверить что записи созданы сегодня
✓ Проверить что recordedBy начинается с "EXTERNAL_"
✓ Проверить timezone
```

---

## 📞 SUPPORT

**Документация:**
- `EXTERNAL_VAS_DEMO_GUIDE.md` - полный гайд
- `EXTERNAL_VAS_TEST_DATA.md` - тестовые данные
- `EXTERNAL_VAS_QUICK_TEST.md` - быстрый тест

**Логи:**
- Backend: console output
- Frontend: Network tab в браузере

**Проверка:**
- Health check: `GET /api/external/vas/health`
- Stats: `GET /api/external/vas/stats`

---

## ✅ CHECKLIST

**Готово к использованию если:**
- [x] Backend запущен (port 8080)
- [x] Frontend запущен (port 3000)
- [x] База данных доступна
- [x] Созданы тестовые пациенты
- [x] Сгенерирован API ключ
- [x] Single VAS работает
- [x] Batch import работает
- [x] Monitor отображает данные
- [x] Статистика корректна

**Все готово!** 🎉

---

## 🚀 NEXT STEPS

1. Прочитать `EXTERNAL_VAS_DEMO_GUIDE.md`
2. Запустить `EXTERNAL_VAS_QUICK_TEST.md`
3. Подготовить презентацию
4. Протестировать все сценарии
5. Показать заказчику!

**Good luck!** 💪
