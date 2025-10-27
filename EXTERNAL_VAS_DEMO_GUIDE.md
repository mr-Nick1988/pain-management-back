#   🎯 EXTERNAL VAS INTEGRATION - ПРЕЗЕНТАЦИОННЫЙ ГАЙД

## 📋 ЧТО УЖЕ РЕАЛИЗОВАНО (Backend)

### ✅ Готовые Endpoints
1. **POST /api/external/vas/record** - прием VAS с внешнего устройства
2. **POST /api/external/vas/batch** - batch импорт CSV
3. **GET /api/external/vas/health** - health check
4. **POST /api/admin/api-keys/generate** - генерация API ключа
5. **GET /api/admin/api-keys** - список всех ключей
6. **DELETE /api/admin/api-keys/{apiKey}** - деактивация ключа
7. **PUT /api/admin/api-keys/{apiKey}/whitelist** - обновление IP whitelist
8. **PUT /api/admin/api-keys/{apiKey}/rate-limit** - обновление rate limit


---

## 🔧 ЧТО НУЖНО ДОБАВИТЬ В BACKEND

### 1. DTO для мониторинга

**Создать файл:** `VAS_external_integration/dto/ExternalVasRecordResponse.java`

```java
package pain_helper_back.VAS_external_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для отображения VAS записи с внешнего устройства в мониторе
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVasRecordResponse {
    private Long id;
    private String patientMrn;
    private String patientFirstName;
    private String patientLastName;
    private Integer vasLevel;
    private String deviceId;
    private String location;
    private LocalDateTime timestamp;
    private String notes;
    private String source;
    private LocalDateTime createdAt;
}
```

**Создать файл:** `VAS_external_integration/dto/VasMonitorStats.java`

```java
package pain_helper_back.VAS_external_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Статистика для VAS Monitor Dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VasMonitorStats {
    private Integer totalRecordsToday;
    private Double averageVas;
    private Integer highPainAlerts; // COUNT WHERE vasLevel >= 7
    private Integer activeDevices;  // COUNT DISTINCT deviceId (сегодня)
}
```

### 2. Методы в Service

**Добавить в:** `VAS_external_integration/service/ExternalVasIntegrationService.java`

```java
/**
 * Получить список VAS записей с фильтрами
 */
@Transactional(readOnly = true)
public List<ExternalVasRecordResponse> getVasRecords(
        String deviceId,
        String location,
        String timeRange,
        Integer vasLevelMin,
        Integer vasLevelMax) {
    
    log.info("Fetching VAS records: deviceId={}, location={}, timeRange={}, vasRange={}-{}",
            deviceId, location, timeRange, vasLevelMin, vasLevelMax);
    
    // Определяем временной диапазон
    LocalDateTime startTime = calculateStartTime(timeRange);
    
    // Получаем VAS записи с JOIN к Patient
    List<Vas> vasRecords = vasRepository.findAll().stream()
            .filter(v -> v.getRecordedBy() != null && v.getRecordedBy().startsWith("EXTERNAL_"))
            .filter(v -> startTime == null || v.getCreatedAt().isAfter(startTime))
            .filter(v -> deviceId == null || extractDeviceId(v.getRecordedBy()).contains(deviceId))
            .filter(v -> location == null || (v.getLocation() != null && v.getLocation().contains(location)))
            .filter(v -> vasLevelMin == null || v.getVasLevel() >= vasLevelMin)
            .filter(v -> vasLevelMax == null || v.getVasLevel() <= vasLevelMax)
            .sorted((v1, v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt())) // DESC
            .toList();
    
    // Конвертируем в DTO
    return vasRecords.stream()
            .map(this::convertToResponse)
            .toList();
}

/**
 * Получить статистику по VAS записям за сегодня
 */
@Transactional(readOnly = true)
public VasMonitorStats getVasStatistics() {
    log.info("Calculating VAS statistics for today");
    
    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
    
    List<Vas> todayRecords = vasRepository.findAll().stream()
            .filter(v -> v.getRecordedBy() != null && v.getRecordedBy().startsWith("EXTERNAL_"))
            .filter(v -> v.getCreatedAt().isAfter(startOfDay))
            .toList();
    
    int totalRecords = todayRecords.size();
    
    double averageVas = todayRecords.isEmpty() ? 0.0 :
            todayRecords.stream()
                    .mapToInt(Vas::getVasLevel)
                    .average()
                    .orElse(0.0);
    
    int highPainAlerts = (int) todayRecords.stream()
            .filter(v -> v.getVasLevel() >= 7)
            .count();
    
    long activeDevices = todayRecords.stream()
            .map(v -> extractDeviceId(v.getRecordedBy()))
            .distinct()
            .count();
    
    return VasMonitorStats.builder()
            .totalRecordsToday(totalRecords)
            .averageVas(Math.round(averageVas * 10.0) / 10.0) // Округление до 1 знака
            .highPainAlerts(highPainAlerts)
            .activeDevices((int) activeDevices)
            .build();
}

// Вспомогательные методы
private LocalDateTime calculateStartTime(String timeRange) {
    if (timeRange == null) return null;
    
    return switch (timeRange) {
        case "1h" -> LocalDateTime.now().minusHours(1);
        case "6h" -> LocalDateTime.now().minusHours(6);
        case "24h" -> LocalDateTime.now().minusHours(24);
        case "7d" -> LocalDateTime.now().minusDays(7);
        default -> null;
    };
}

private String extractDeviceId(String recordedBy) {
    // recordedBy format: "EXTERNAL_VAS_MONITOR" или "EXTERNAL_DEVICE_ID"
    if (recordedBy == null || !recordedBy.startsWith("EXTERNAL_")) {
        return "UNKNOWN";
    }
    return recordedBy.substring("EXTERNAL_".length());
}

private ExternalVasRecordResponse convertToResponse(Vas vas) {
    Patient patient = vas.getPatient();
    
    return ExternalVasRecordResponse.builder()
            .id(vas.getId())
            .patientMrn(patient.getMrn())
            .patientFirstName(patient.getFirstName())
            .patientLastName(patient.getLastName())
            .vasLevel(vas.getVasLevel())
            .deviceId(extractDeviceId(vas.getRecordedBy()))
            .location(vas.getLocation())
            .timestamp(vas.getRecordedAt())
            .notes(vas.getNotes())
            .source(extractDeviceId(vas.getRecordedBy()))
            .createdAt(vas.getCreatedAt())
            .build();
}
```

### 3. Endpoints в Controller

**Добавить в:** `VAS_external_integration/controller/ExternalVasIntegrationController.java`

```java
/**
 * Получить список VAS записей с фильтрами (для мониторинга)
 */
@GetMapping("/records")
public ResponseEntity<List<ExternalVasRecordResponse>> getRecords(
        @RequestParam(required = false) String deviceId,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String timeRange,
        @RequestParam(required = false) Integer vasLevelMin,
        @RequestParam(required = false) Integer vasLevelMax) {
    
    log.info("GET /api/external/vas/records - deviceId={}, location={}, timeRange={}, vasRange={}-{}",
            deviceId, location, timeRange, vasLevelMin, vasLevelMax);
    
    List<ExternalVasRecordResponse> records = integrationService.getVasRecords(
            deviceId, location, timeRange, vasLevelMin, vasLevelMax);
    
    return ResponseEntity.ok(records);
}

/**
 * Получить статистику по VAS записям за сегодня
 */
@GetMapping("/stats")
public ResponseEntity<VasMonitorStats> getStats() {
    log.info("GET /api/external/vas/stats");
    
    VasMonitorStats stats = integrationService.getVasStatistics();
    return ResponseEntity.ok(stats);
}
```

---

## 🎬 СЦЕНАРИЙ ПРЕЗЕНТАЦИИ (Step-by-Step)

### ЭТАП 1: Подготовка (5 минут)

#### 1.1 Создать тестовых пациентов
```bash
# Через Mock Generator или FHIR Import
POST /api/emr/mock/generate-batch?count=5

# Запомнить MRN пациентов (например: MRN-42, MRN-43, MRN-44)
```

#### 1.2 Сгенерировать API ключ
```bash
# Через UI: Admin → API Key Management → Generate New Key
System Name: "Demo VAS Monitors"
Description: "Presentation demo devices"
IP Whitelist: "*"
Rate Limit: 120

# Скопировать сгенерированный ключ:
# pma_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
```

---

### ЭТАП 2: Демонстрация Single VAS Record (10 минут)

#### 2.1 Открыть VAS Device Simulator
```
Frontend: Nurse Dashboard → External VAS Monitor → Device Simulator Tab
```

#### 2.2 Отправить VAS запись
```json
Patient MRN: MRN-42
VAS Level: 8 (красный индикатор)
Device ID: MONITOR-001
Location: Ward A, Bed 12
Notes: Patient reports severe pain in lower back
```

**Кнопка:** "Send VAS Record"

#### 2.3 Показать результат
```
✅ Success Response:
{
  "status": "success",
  "vasId": 123,
  "patientMrn": "MRN-42",
  "vasLevel": 8,
  "format": "JSON"
}
```

#### 2.4 Переключиться на Monitor Tab
```
Frontend: External VAS Monitor → Monitor Tab

Показать:
- ✅ Запись появилась в real-time таблице
- 🔴 VAS Level = 8 (красный индикатор)
- 📍 Location: Ward A, Bed 12
- 🖥️ Device: MONITOR-001
- ⏰ Timestamp: "2 seconds ago"
```

#### 2.5 Показать автоматическую эскалацию
```
Frontend: Escalation Dashboard

Показать:
- 🚨 Новая эскалация создана автоматически
- Priority: HIGH (VAS >= 7)
- Reason: "High pain level detected from external device"
- Patient: MRN-42
```

---

### ЭТАП 3: Демонстрация Batch Import (10 минут)

#### 3.1 Подготовить CSV файл
```csv
patientMrn,vasLevel,deviceId,location,timestamp
MRN-42,7,MONITOR-001,Ward A,2025-10-26T12:00:00
MRN-43,5,MONITOR-002,Ward B,2025-10-26T12:01:00
MRN-44,9,MONITOR-003,ICU-1,2025-10-26T12:02:00
MRN-42,6,MONITOR-001,Ward A,2025-10-26T12:03:00
MRN-43,8,MONITOR-002,Ward B,2025-10-26T12:04:00
```

#### 3.2 Отправить через Postman/curl
```bash
curl -X POST http://localhost:8080/api/external/vas/batch \
  -H "X-API-Key: pma_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6" \
  -H "Content-Type: text/csv" \
  --data-binary @vas_batch.csv
```

#### 3.3 Показать результат
```json
{
  "status": "success",
  "total": 5,
  "success": 5,
  "failed": 0,
  "createdVasIds": [124, 125, 126, 127, 128]
}
```

#### 3.4 Обновить Monitor
```
Frontend: External VAS Monitor → Refresh

Показать:
- ✅ Все 5 записей появились в таблице
- 📊 Статистика обновилась:
  - Total Records Today: 6 (1 single + 5 batch)
  - Average VAS: 7.2
  - High Pain Alerts: 3 (VAS >= 7)
  - Active Devices: 3
```

---

### ЭТАП 4: Демонстрация Фильтрации (5 минут)

#### 4.1 Фильтр по Device
```
Frontend: Device Filter → Select "MONITOR-001"

Показать:
- ✅ Только записи с MONITOR-001
- 📊 Статистика пересчитана для этого устройства
```

#### 4.2 Фильтр по Location
```
Frontend: Location Filter → Select "Ward A"

Показать:
- ✅ Только записи из Ward A
```

#### 4.3 Фильтр по VAS Level
```
Frontend: VAS Level Range → 7-10 (High Pain)

Показать:
- 🔴 Только записи с VAS >= 7
- 📊 High Pain Alerts count
```

#### 4.4 Фильтр по Time Range
```
Frontend: Time Range → "Last 1 hour"

Показать:
- ⏰ Только записи за последний час
```

---

### ЭТАП 5: Демонстрация API Key Management (5 минут)

#### 5.1 Открыть API Key Management
```
Frontend: Admin → API Key Management
```

#### 5.2 Показать список ключей
```
Таблица:
- System Name: Demo VAS Monitors
- API Key: pma_live_a1b2c3d4**** (masked)
- Expires: Never
- IP Whitelist: *
- Rate Limit: 120/min
- Status: ✅ Active
- Usage Count: 6 (1 single + 5 batch)
- Last Used: 2 minutes ago
```

#### 5.3 Обновить IP Whitelist
```
Actions → Edit → IP Whitelist
New Value: 192.168.1.0/24

Показать:
- ✅ Updated successfully
- 🔒 Теперь только IP из этой подсети могут использовать ключ
```

#### 5.4 Обновить Rate Limit
```
Actions → Edit → Rate Limit
New Value: 60/min

Показать:
- ✅ Updated successfully
- ⏱️ Теперь максимум 60 запросов в минуту
```

#### 5.5 Деактивировать ключ
```
Actions → Deactivate

Показать:
- ❌ Status: Inactive
- 🚫 Попытка использовать ключ → 401 Unauthorized
```

---

### ЭТАП 6: Демонстрация Real-time Updates (5 минут)

#### 6.1 Включить Auto-refresh
```
Frontend: External VAS Monitor → ☑️ Auto-refresh every 30s
```

#### 6.2 Отправить VAS через Simulator
```
Каждые 10 секунд отправлять новую запись с разными VAS levels
```

#### 6.3 Показать обновления
```
Показать:
- 🔄 Таблица обновляется автоматически
- 📊 Статистика пересчитывается
- 🔔 Toast notifications при новых записях
- 🚨 Alert при VAS >= 7
```

---

### ЭТАП 7: Демонстрация Integration с Pain Escalation (5 минут)

#### 7.1 Отправить критический VAS
```
Patient MRN: MRN-42
VAS Level: 9
Device ID: MONITOR-001
```

#### 7.2 Показать автоматическую цепочку
```
1. ✅ VAS Record создан
2. 🚨 Pain Escalation проверка запущена автоматически
3. 🔴 Escalation создана (Priority: CRITICAL)
4. 📧 Notification отправлена анестезиологу
5. 💊 Recommendation сгенерирована автоматически (если VAS >= 4)
```

#### 7.3 Открыть Escalation Dashboard
```
Frontend: Escalation Dashboard

Показать:
- 🚨 Новая CRITICAL эскалация
- Patient: MRN-42
- Reason: "VAS level 9 detected from external device MONITOR-001"
- Created: "Just now"
- Status: PENDING
```

---

## 🧪 ТЕСТОВЫЕ ДАННЫЕ ДЛЯ ПРЕЗЕНТАЦИИ

### Пациенты (создать заранее)
```
MRN-42: John Doe, Ward A, Bed 12
MRN-43: Jane Smith, Ward B, Bed 5
MRN-44: Bob Wilson, ICU-1, Bed 3
MRN-45: Alice Johnson, Ward A, Bed 8
MRN-46: Charlie Brown, Ward C, Bed 15
```

### API Ключи
```
1. Demo VAS Monitors (для презентации)
   - IP: *
   - Rate: 120/min
   
2. Production Monitors (деактивирован)
   - IP: 192.168.1.0/24
   - Rate: 60/min
```

### VAS Записи (отправить через batch)
```csv
patientMrn,vasLevel,deviceId,location,timestamp
MRN-42,3,MONITOR-001,Ward A,2025-10-26T08:00:00
MRN-42,5,MONITOR-001,Ward A,2025-10-26T09:00:00
MRN-42,7,MONITOR-001,Ward A,2025-10-26T10:00:00
MRN-42,8,MONITOR-001,Ward A,2025-10-26T11:00:00
MRN-43,4,MONITOR-002,Ward B,2025-10-26T08:30:00
MRN-43,6,MONITOR-002,Ward B,2025-10-26T09:30:00
MRN-43,5,MONITOR-002,Ward B,2025-10-26T10:30:00
MRN-44,9,MONITOR-003,ICU-1,2025-10-26T08:15:00
MRN-44,7,MONITOR-003,ICU-1,2025-10-26T09:15:00
MRN-44,6,MONITOR-003,ICU-1,2025-10-26T10:15:00
```

---

## 📊 КЛЮЧЕВЫЕ МЕТРИКИ ДЛЯ ПОКАЗА

### Dashboard Statistics
```
Total Records Today: 15
Average VAS: 6.2
High Pain Alerts: 5 (VAS >= 7)
Active Devices: 3
```

### Device Breakdown
```
MONITOR-001: 4 records (Ward A)
MONITOR-002: 3 records (Ward B)
MONITOR-003: 3 records (ICU-1)
TABLET-001: 2 records (Ward A)
MOBILE-APP: 3 records (Various)
```

### Time Distribution
```
Last 1 hour: 5 records
Last 6 hours: 10 records
Last 24 hours: 15 records
```

---

## 🎯 ПРЕЗЕНТАЦИОННЫЕ ФИШКИ

### 1. Real-time Updates
- Включить auto-refresh
- Отправлять VAS каждые 10 секунд
- Показать как таблица обновляется

### 2. Color Coding
- 🟢 VAS 0-3: зеленый (низкая боль)
- 🟡 VAS 4-6: желтый (средняя боль)
- 🔴 VAS 7-10: красный (высокая боль)

### 3. Badges
- 🔵 VAS_MONITOR (синий)
- 🟣 EMR_SYSTEM (фиолетовый)
- ⚪ MANUAL_ENTRY (серый)

### 4. Notifications
- Toast при новой записи
- Alert при VAS >= 7
- Sound notification (опционально)

### 5. Charts
- Line chart: VAS trend за 24 часа
- Bar chart: VAS distribution по devices
- Pie chart: Source breakdown

---

## 🔧 TROUBLESHOOTING

### Проблема: API Key не работает
```
Решение:
1. Проверить что ключ активен (active = true)
2. Проверить IP whitelist
3. Проверить expiration date
4. Проверить rate limit
```

### Проблема: VAS не появляется в мониторе
```
Решение:
1. Проверить что пациент существует (MRN)
2. Проверить что VAS сохранился в БД
3. Обновить страницу (F5)
4. Проверить фильтры (сбросить все)
```

### Проблема: Статистика не обновляется
```
Решение:
1. Проверить что записи созданы сегодня
2. Проверить что recordedBy начинается с "EXTERNAL_"
3. Обновить страницу
```

---

## ✅ CHECKLIST ПЕРЕД ПРЕЗЕНТАЦИЕЙ

- [ ] Backend запущен (port 8080)
- [ ] Frontend запущен (port 3000)
- [ ] База данных доступна
- [ ] Созданы тестовые пациенты (5 шт)
- [ ] Сгенерирован API ключ
- [ ] Подготовлен CSV файл для batch
- [ ] Postman/curl готов для batch импорта
- [ ] Открыты вкладки:
  - [ ] External VAS Monitor
  - [ ] API Key Management
  - [ ] Escalation Dashboard
  - [ ] Device Simulator
- [ ] Проверена работа всех endpoints
- [ ] Очищена БД от старых тестовых данных

---

## 🎬 ФИНАЛЬНЫЙ СЦЕНАРИЙ (30 минут)

**0:00-0:05** - Введение и обзор архитектуры
**0:05-0:10** - Генерация API ключа
**0:10-0:15** - Single VAS record через Simulator
**0:15-0:20** - Batch import через Postman
**0:20-0:25** - Фильтрация и статистика
**0:25-0:30** - Real-time updates и эскалация

**ИТОГО:** Полная демонстрация External VAS Integration с акцентом на:
- ✅ Простота интеграции (один API ключ)
- ✅ Безопасность (IP whitelist, rate limiting)
- ✅ Real-time мониторинг
- ✅ Автоматическая эскалация
- ✅ Batch обработка
