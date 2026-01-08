# Quick Test: Pain Escalation Tracking

## 🔧 Исправление применено
**Файл:** `NurseServiceImpl.java` (строки 326-337)
- ✅ Теперь можно создавать множественные VAS для одного пациента
- ✅ Блокировка только при статусе PENDING (ожидает одобрения)
- ✅ APPROVED/REJECTED/EXECUTED рекомендации не блокируют новые VAS

---

## 🚀 Быстрый тест (5 минут)

### Шаг 1: Создать пациента (если нет)
```http
POST /api/nurse/patients
{
  "firstName": "Test",_
  "lastName": "Patient",
  "dateOfBirth": "1980-01-01",
  "gender": "MALE",
  "phoneNumber": "+1234567890",
  "email": "test@example.com"
}
```
**Запомнить MRN пациента** (например: `000069`)

---

### Шаг 2: Создать EMR
```http
POST /api/nurse/patients/000069/emr
{
  "weight": 70,
  "height": 175,
  "gfr": "90",
  "childPughScore": "A",
  "diagnoses": [
    {
      "icdCode": "M54.5",
      "description": "Low back pain"
    }
  ]
}
```

---

### Шаг 3: Создать VAS #1 (боль = 6)
```http
POST /api/nurse/patients/000069/vas
{
  "painLevel": 6,
  "painPlace": "Lower back",
  "recordedBy": "nurse_001"
}
```
**Результат:** Автоматически создана рекомендация (статус PENDING)

---

### Шаг 4: Доктор одобряет рекомендацию
```http
GET /api/nurse/patients/000069/recommendations/last
```
**Запомнить ID рекомендации** (например: `14`)

```http
PUT /api/doctor/recommendations/14/approve
{
  "comment": "Approved for testing"
}
```
**Результат:** Статус изменен на APPROVED

---

### Шаг 5: Создать VAS #2 (боль = 8) ⚡ ЭСКАЛАЦИЯ
```http
POST /api/nurse/patients/000069/vas
{
  "painLevel": 8,
  "painPlace": "Lower back",
  "recordedBy": "nurse_001"
}
```
**Результат:** 
- ✅ VAS создан успешно (без rollback!)
- ✅ Создана эскалация (ухудшение на 2 балла)
- ✅ Автоматически создана новая рекомендация

---

### Шаг 6: Проверить эскалацию
```http
GET /api/escalations/patient/000069
```

**Ожидаемый ответ:**
```json
[
  {
    "id": 1,
    "escalationType": "PAIN_WORSENING",
    "priority": "HIGH",
    "status": "OPEN",
    "reason": "Pain worsened by 2 points (from 6 to 8)",
    "previousVasLevel": 6,
    "currentVasLevel": 8,
    "createdAt": "2025-11-01T16:14:27"
  }
]
```

---

### Шаг 7: Повторить для VAS #3 (боль = 9)

1. Доктор одобряет рекомендацию #2
2. Создать VAS с уровнем 9
3. Проверить создание второй эскалации

**Результат:** Обе эскалации успешно созданы!

---

## 🎯 Тест через External VAS

### Альтернативный способ (через внешнее устройство)

```http
POST /api/external/vas/record
{
  "patientMrn": "000069",
  "vasLevel": 6,
  "deviceId": "VAS_MONITOR_001",
  "source": "VAS_MONITOR",
  "location": "Ward A",
  "painPlace": "Lower back",
  "timestamp": "2025-11-01T16:00:00"
}
```

Затем одобрить рекомендацию и отправить VAS с уровнем 8:

```http
POST /api/external/vas/record
{
  "patientMrn": "000069",
  "vasLevel": 8,
  "deviceId": "VAS_MONITOR_001",
  "source": "VAS_MONITOR",
  "location": "Ward A",
  "painPlace": "Lower back",
  "timestamp": "2025-11-01T16:05:00"
}
```

---

## ✅ Критерии успеха

- [ ] Можно создать несколько VAS подряд (после одобрения рекомендаций)
- [ ] Нет ошибки "Transaction silently rolled back"
- [ ] Эскалации создаются при ухудшении боли на ≥2 балла
- [ ] Эскалации создаются при критической боли (VAS ≥ 8)
- [ ] Все VAS записи сохранены в БД

---

## ❌ Что НЕ должно работать

**Попытка создать VAS когда рекомендация PENDING:**
```http
POST /api/nurse/patients/000069/vas
{
  "painLevel": 7,
  "painPlace": "Lower back"
}
```
**Ожидаемая ошибка:**
```json
{
  "error": "Previous recommendation is still pending approval"
}
```

**Решение:** Доктор должен одобрить/отклонить рекомендацию

---

## 📊 Проверка результатов

### SQL запросы для проверки

**Все VAS пациента:**
```sql
SELECT id, pain_level, recorded_at, resolved 
FROM vas 
WHERE patient_id = (SELECT id FROM patient WHERE mrn = '000069')
ORDER BY recorded_at;
```

**Все эскалации:**
```sql
SELECT id, escalation_type, priority, status, reason, current_vas_level
FROM pain_escalation
WHERE patient_id = (SELECT id FROM patient WHERE mrn = '000069')
ORDER BY created_at;
```

**Все рекомендации:**
```sql
SELECT id, status, created_at 
FROM recommendation
WHERE patient_id = (SELECT id FROM patient WHERE mrn = '000069')
ORDER BY created_at;
```

---

## 🐛 Troubleshooting

### Проблема: "Previous recommendation is still pending approval"
**Решение:** Одобрить рекомендацию через `/api/doctor/recommendations/{id}/approve`

### Проблема: Эскалация не создается
**Проверить:**
1. Разница VAS ≥ 2 балла?
2. Логи `PainEscalationService`
3. Настройки в `application.properties`

### Проблема: Rollback транзакции
**Проверить:**
1. Логи `PainTrendRuleApplier`
2. Ошибки в `TreatmentProtocolService`
3. Валидация данных пациента (EMR, диагнозы)

---

## 📝 Полный тестовый сценарий

**Postman Collection:** Импортировать `pain_escalation_tests.json`

**Последовательность:**
1. Create Patient → MRN
2. Create EMR
3. Create VAS (level 6) → Recommendation #1 (PENDING)
4. Approve Recommendation #1 → Status: APPROVED
5. Create VAS (level 8) → ✅ Escalation #1 + Recommendation #2
6. Approve Recommendation #2
7. Create VAS (level 9) → ✅ Escalation #2 + Recommendation #3
8. Check Escalations → 2 escalations found
9. Check Stats → Verify counts

**Время выполнения:** ~3-5 минут

---

## 🎉 Готово!

Теперь вы можете создавать множественные VAS записи для тестирования Pain Escalation Tracking!

**Документация:** См. `PAIN_ESCALATION_TESTING_GUIDE.md` для детальных сценариев
