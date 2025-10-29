# ⚡ EXTERNAL VAS - БЫСТРЫЙ ТЕСТ (5 минут)

## 🚀 ШАГ 1: Подготовка (1 мин)

### Создать тестового пациента
```bash
POST http://localhost:8080/api/emr/mock/generate

Запомнить MRN из response (например: MRN-42)
```

### Сгенерировать API ключ
```
Frontend: Admin → API Key Management → Generate New Key

System Name: Test
IP Whitelist: *
Rate Limit: 120

Скопировать ключ: pma_live_XXXXXXXXXXXXXXXX
```

---

## 🧪 ШАГ 2: Тест Single VAS (2 мин)

### Через Device Simulator (Frontend)
```
Nurse Dashboard → External VAS Monitor → Device Simulator

Patient MRN: MRN-42
VAS Level: 8
Device ID: MONITOR-001
Location: Ward A

Кнопка: Send VAS Record
```

### Проверка
```
✅ Success response
✅ Запись появилась в Monitor Tab
✅ Статистика: Total=1, Avg=8.0, HighPain=1, Devices=1
```

---

## 📦 ШАГ 3: Тест Batch Import (2 мин)

### Создать файл test.csv
```csv
patientMrn,vasLevel,deviceId,location,timestamp
MRN-42,7,MONITOR-001,Ward A,2025-10-26T08:00:00
MRN-42,6,MONITOR-001,Ward A,2025-10-26T09:00:00
MRN-42,9,MONITOR-001,Ward A,2025-10-26T10:00:00
```

### Отправить через Postman
```
POST http://localhost:8080/api/external/vas/batch
Headers:
  X-API-Key: pma_live_XXXXXXXXXXXXXXXX
  Content-Type: text/csv

Body: [вставить CSV]
```

### Проверка
```
✅ Response: total=3, success=3, failed=0
✅ Monitor показывает 4 записи (1+3)
✅ Статистика обновилась
```

---

## ✅ ГОТОВО!

**Все работает если:**
- ✅ Single VAS создается
- ✅ Batch import успешен
- ✅ Monitor показывает записи
- ✅ Статистика корректна
- ✅ Фильтры работают

---

## 🎯 ДЛЯ ПРЕЗЕНТАЦИИ

### Открыть вкладки:
1. External VAS Monitor
2. API Key Management
3. Escalation Dashboard
4. Device Simulator

### Подготовить:
- 5 тестовых пациентов
- 1 API ключ
- CSV файл с 10-15 записями
- Postman collection

### Показать:
1. Генерация API ключа (1 раз!)
2. Single VAS через Simulator
3. Batch import через Postman
4. Фильтрация (device, location, VAS level)
5. Real-time updates (auto-refresh)
6. Автоматическая эскалация (VAS=9)

**Время:** 10-15 минут

---

## 📝 POSTMAN COLLECTION

### 1. Generate API Key
```
POST http://localhost:8080/api/admin/api-keys/generate?systemName=Test&ipWhitelist=*&rateLimitPerMinute=120
```

### 2. Single VAS
```
POST http://localhost:8080/api/external/vas/record
Headers:
  X-API-Key: pma_live_XXXXXXXXXXXXXXXX
  Content-Type: application/json

Body:
{
  "patientMrn": "MRN-42",
  "vasLevel": 8,
  "deviceId": "MONITOR-001",
  "location": "Ward A",
  "notes": "Test"
}
```

### 3. Batch VAS
```
POST http://localhost:8080/api/external/vas/batch
Headers:
  X-API-Key: pma_live_XXXXXXXXXXXXXXXX
  Content-Type: text/csv

Body: [CSV data]
```

### 4. Get Records
```
GET http://localhost:8080/api/external/vas/records?timeRange=24h
```

### 5. Get Stats
```
GET http://localhost:8080/api/external/vas/stats
```

### 6. Health Check
```
GET http://localhost:8080/api/external/vas/health
```

---

## 🔥 БЫСТРЫЕ КОМАНДЫ

### Создать 5 пациентов
```bash
curl -X POST "http://localhost:8080/api/emr/mock/generate-batch?count=5"
```

### Отправить VAS
```bash
curl -X POST http://localhost:8080/api/external/vas/record \
  -H "X-API-Key: pma_live_XXXXXXXXXXXXXXXX" \
  -H "Content-Type: application/json" \
  -d '{"patientMrn":"MRN-42","vasLevel":8,"deviceId":"MONITOR-001","location":"Ward A"}'
```

### Получить статистику
```bash
curl http://localhost:8080/api/external/vas/stats
```

---

## ✅ ИТОГОВЫЙ CHECKLIST

**Backend:**
- [ ] Сервер запущен
- [ ] Endpoints работают
- [ ] База данных доступна

**Frontend:**
- [ ] Приложение запущено
- [ ] Monitor отображает данные
- [ ] Simulator работает
- [ ] API Key Management работает

**Данные:**
- [ ] Пациенты созданы
- [ ] API ключ сгенерирован
- [ ] VAS записи созданы
- [ ] Статистика корректна

**Готово к презентации!** 🎉
