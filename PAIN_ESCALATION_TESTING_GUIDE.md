# Pain Escalation Tracking - Testing Guide

## Проблема и Решение

### Исходная Проблема
Система блокировала создание новых VAS записей для пациента, если у него была **любая неразрешенная рекомендация** (статус != EXECUTED). Это делало невозможным тестирование Pain Escalation Tracking.

### Исправление
**Файл:** `NurseServiceImpl.java` (строки 326-337)

**Было:**
```java
if (last.getStatus() != RecommendationStatus.EXECUTED) {
    throw new EntityExistsException("Previous recommendation is still unresolved");
}
```

**Стало:**
```java
if (last.getStatus() == RecommendationStatus.PENDING) {
    log.warn("Cannot create new recommendation - previous recommendation {} is still PENDING", 
            last.getId());
    throw new EntityExistsException("Previous recommendation is still pending approval");
}
```

**Логика:**
- ✅ **APPROVED** рекомендация → можно создавать новые VAS
- ✅ **REJECTED** рекомендация → можно создавать новые VAS  
- ✅ **EXECUTED** рекомендация → можно создавать новые VAS
- ❌ **PENDING** рекомендация → блокировка (ждем одобрения доктором)

---

## Тестирование Pain Escalation Tracking

### Сценарий 1: Эскалация боли (Pain Worsening)

**Цель:** Проверить создание эскалации при ухудшении боли на ≥2 балла

**Шаги:**
1. Создать VAS с уровнем боли **6**
   ```http
   POST /api/nurse/patients/{mrn}/vas
   {
     "painLevel": 6,
     "painPlace": "Lower back",
     "recordedBy": "nurse_001"
   }
   ```

2. Доктор одобряет рекомендацию
   ```http
   PUT /api/doctor/recommendations/{id}/approve
   {
     "comment": "Approved for testing"
   }
   ```

3. Создать VAS с уровнем боли **8** (ухудшение на 2 балла)
   ```http
   POST /api/nurse/patients/{mrn}/vas
   {
     "painLevel": 8,
     "painPlace": "Lower back",
     "recordedBy": "nurse_001"
   }
   ```

4. Проверить создание эскалации
   ```http
   GET /api/escalations/patient/{mrn}
   ```

**Ожидаемый результат:**
- Создана эскалация с типом `PAIN_WORSENING`
- Статус: `OPEN`
- Приоритет: `HIGH` (т.к. VAS=8)
- Reason: "Pain worsened by 2 points"

---

### Сценарий 2: Критическая боль (Critical Pain)

**Цель:** Проверить создание эскалации при VAS ≥ 8

**Шаги:**
1. Создать VAS с уровнем боли **9**
   ```http
   POST /api/nurse/patients/{mrn}/vas
   {
     "painLevel": 9,
     "painPlace": "Chest",
     "recordedBy": "nurse_002"
   }
   ```

2. Проверить эскалацию
   ```http
   GET /api/escalations/patient/{mrn}
   ```

**Ожидаемый результат:**
- Создана эскалация с типом `CRITICAL_PAIN`
- Статус: `OPEN`
- Приоритет: `CRITICAL`
- Reason: "Critical pain level detected (VAS=9)"

---

### Сценарий 3: Множественные VAS для одного пациента

**Цель:** Проверить возможность создания нескольких VAS записей подряд

**Шаги:**
1. Создать VAS #1 (уровень 5)
2. Доктор одобряет рекомендацию
3. Создать VAS #2 (уровень 7) → должна создаться эскалация
4. Доктор одобряет рекомендацию
5. Создать VAS #3 (уровень 9) → должна создаться еще одна эскалация
6. Доктор одобряет рекомендацию
7. Создать VAS #4 (уровень 4) → улучшение, эскалация не создается

**Ожидаемый результат:**
- Все VAS записи успешно созданы
- Созданы 2 эскалации (для VAS #2 и #3)
- Нет ошибок транзакций

---

### Сценарий 4: External VAS Integration

**Цель:** Проверить создание VAS через внешнее устройство

**Шаги:**
1. Отправить VAS через External API
   ```http
   POST /api/external/vas/record
   {
     "patientMrn": "EMR-97E0CC48",
     "vasLevel": 8,
     "deviceId": "VAS_MONITOR_001",
     "source": "VAS_MONITOR",
     "location": "Ward A",
     "painPlace": "Abdomen",
     "timestamp": "2025-11-01T16:00:00"
   }
   ```

2. Доктор одобряет автоматически созданную рекомендацию

3. Отправить еще один VAS с уровнем 9
   ```http
   POST /api/external/vas/record
   {
     "patientMrn": "EMR-97E0CC48",
     "vasLevel": 9,
     ...
   }
   ```

**Ожидаемый результат:**
- Обе VAS записи созданы
- Автоматически созданы рекомендации
- Создана эскалация при ухудшении боли

---

### Сценарий 5: Проверка статистики эскалаций

**Шаги:**
1. Создать несколько эскалаций (см. сценарии выше)
2. Получить статистику
   ```http
   GET /api/escalations/stats
   ```

**Ожидаемый результат:**
```json
{
  "totalEscalations": 5,
  "openEscalations": 3,
  "resolvedEscalations": 2,
  "criticalPriority": 2,
  "highPriority": 3,
  "averageResolutionTimeMinutes": 45.5
}
```

---

## Проверка Dose Administration Tracking

### Сценарий 6: Отслеживание введения препаратов

**Шаги:**
1. Медсестра выполняет рекомендацию
   ```http
   POST /api/nurse/patients/{mrn}/recommendations/execute
   ```

2. Проверить создание DoseAdministration
   ```http
   GET /api/escalations/patient/{mrn}/doses
   ```

**Ожидаемый результат:**
- Создана запись DoseAdministration
- Связана с рекомендацией и пациентом
- Содержит информацию о препаратах и дозировках

---

## Проверка Notification System

### Сценарий 7: Уведомления при эскалации

**Шаги:**
1. Создать критическую эскалацию (VAS ≥ 8)
2. Проверить логи на наличие уведомлений
3. Проверить WebSocket события (если подключен фронтенд)

**Ожидаемые логи:**
```
INFO  p.p.s.PainEscalationNotificationService : Sending CRITICAL escalation notification...
INFO  p.p.s.PainEscalationNotificationService : Notification sent to doctor_id for escalation #123
```

---

## Troubleshooting

### Ошибка: "Previous recommendation is still pending approval"

**Причина:** Пытаетесь создать новую VAS, когда предыдущая рекомендация в статусе PENDING

**Решение:** Доктор должен одобрить/отклонить рекомендацию:
```http
PUT /api/doctor/recommendations/{id}/approve
```

### Ошибка: "Transaction silently rolled back"

**Причина:** Исключение в вложенной транзакции (например, в TreatmentProtocolService)

**Решение:** Проверить логи на наличие ошибок в PainTrendRuleApplier или других правилах

### Эскалация не создается

**Проверить:**
1. VAS записи сохранены в БД
2. Разница между последними VAS ≥ 2 балла
3. Логи PainEscalationService на наличие ошибок
4. Настройки эскалации в application.properties

---

## Полезные SQL запросы

### Проверить все VAS пациента
```sql
SELECT id, pain_level, recorded_at, recorded_by 
FROM vas 
WHERE patient_id = (SELECT id FROM patient WHERE mrn = 'EMR-97E0CC48')
ORDER BY recorded_at DESC;
```

### Проверить эскалации пациента
```sql
SELECT id, escalation_type, priority, status, reason, created_at
FROM pain_escalation
WHERE patient_id = (SELECT id FROM patient WHERE mrn = 'EMR-97E0CC48')
ORDER BY created_at DESC;
```

### Проверить рекомендации пациента
```sql
SELECT id, status, created_at, updated_at
FROM recommendation
WHERE patient_id = (SELECT id FROM patient WHERE mrn = 'EMR-97E0CC48')
ORDER BY created_at DESC;
```

---

## Конфигурация

### application.properties
```properties
# Pain Escalation Settings
pain.escalation.threshold=2
pain.escalation.critical-level=8
pain.escalation.notification.enabled=true
pain.escalation.auto-resolve.enabled=false
```

---

## Контакты для вопросов

- **Backend:** NurseServiceImpl, PainEscalationService
- **External Integration:** ExternalVasIntegrationService
- **Notifications:** PainEscalationNotificationService
