# Инструкции по интеграции диагнозов в аналитику

## ✅ УЖЕ ВЫПОЛНЕНО:

### 1. Events (События) - ГОТОВО ✅
- **EmrCreatedEvent** - добавлены поля `diagnosisCodes` и `diagnosisDescriptions`
- **EscalationCreatedEvent** - добавлено поле `patientDiagnosisCodes`
- **EscalationResolvedEvent** - добавлено поле `patientDiagnosisCodes`

### 2. AnalyticsEvent Entity - ГОТОВО ✅
- Добавлены поля `diagnosisCodes` (индексированное) и `diagnosisDescriptions`
- Поля сохраняются в MongoDB для аналитики

### 3. AnalyticsEventListener - ГОТОВО ✅
- `handleEmrCreated()` - сохраняет диагнозы в аналитику
- `handleEscalationCreated()` - сохраняет диагнозы пациента
- `handleEscalationResolved()` - сохраняет диагнозы пациента

### 4. DoctorServiceImpl - В ПРОЦЕССЕ 🔧
- Обновлен вызов `EmrCreatedEvent` с диагнозами
- Обновлен вызов `EscalationCreatedEvent` с диагнозами пациента

### 5. AnesthesiologistServiceImpl - В ПРОЦЕССЕ 🔧
- Обновлены вызовы `EscalationResolvedEvent` с пустым списком (TODO: извлечь реальные диагнозы)

---

## ⚠️ ТРЕБУЕТСЯ ВЫПОЛНИТЬ В NURSESERVICEIMPL:

### Файл: `pain_helper_back/nurse/service/NurseServiceImpl.java`

### Что нужно сделать:

#### 1. Найти метод, где публикуется `EmrCreatedEvent` (примерно строка 167)

**БЫЛО:**
```java
eventPublisher.publishEvent(new EmrCreatedEvent(
        this,
        emr.getId(),
        mrn,
        "nurse_id", // TODO: заменить на реальный ID
        "NURSE",
        LocalDateTime.now(),
        emr.getGfr(),
        emr.getChildPughScore(),
        emr.getWeight(),
        emr.getHeight()
));
```

**ДОЛЖНО СТАТЬ:**
```java
// Извлекаем диагнозы для аналитики
List<String> diagnosisCodes = emr.getDiagnoses() != null ? 
        emr.getDiagnoses().stream().map(d -> d.getIcdCode()).toList() : new ArrayList<>();
List<String> diagnosisDescriptions = emr.getDiagnoses() != null ? 
        emr.getDiagnoses().stream().map(d -> d.getDescription()).toList() : new ArrayList<>();

eventPublisher.publishEvent(new EmrCreatedEvent(
        this,
        emr.getId(),
        mrn,
        "nurse_id", // TODO: заменить на реальный ID
        "NURSE",
        LocalDateTime.now(),
        emr.getGfr(),
        emr.getChildPughScore(),
        emr.getWeight(),
        emr.getHeight(),
        diagnosisCodes,
        diagnosisDescriptions
));
```

#### 2. Проверить импорты

Убедитесь, что в начале файла есть:
```java
import java.util.ArrayList;
import java.util.List;
```

---

## 📊 ВОЗМОЖНОСТИ АНАЛИТИКИ ПОСЛЕ ИНТЕГРАЦИИ:

### 1. Аналитика по диагнозам:
- Какие диагнозы чаще всего встречаются у пациентов с эскалациями
- Время разрешения эскалаций в зависимости от диагноза
- Корреляция между диагнозами и уровнем боли (VAS)

### 2. Запросы в MongoDB:
```javascript
// Найти все эскалации для пациентов с диабетом
db.analytics_events.find({
  eventType: "ESCALATION_CREATED",
  diagnosisCodes: { $regex: "^250" }  // ICD-9 код диабета
})

// Статистика по диагнозам в эскалациях
db.analytics_events.aggregate([
  { $match: { eventType: "ESCALATION_CREATED" } },
  { $unwind: "$diagnosisCodes" },
  { $group: { _id: "$diagnosisCodes", count: { $sum: 1 } } },
  { $sort: { count: -1 } }
])

// Среднее время разрешения эскалаций по диагнозам
db.analytics_events.aggregate([
  { $match: { eventType: "ESCALATION_RESOLVED" } },
  { $unwind: "$diagnosisCodes" },
  { $group: { 
      _id: "$diagnosisCodes", 
      avgResolutionTime: { $avg: "$processingTimeMs" },
      count: { $sum: 1 }
  } },
  { $sort: { avgResolutionTime: -1 } }
])
```

### 3. Отчеты для менеджмента:
- **Top 10 диагнозов** с наибольшим количеством эскалаций
- **Проблемные диагнозы** с самым долгим временем разрешения
- **Тренды** - как меняется количество эскалаций по диагнозам во времени

---

## 🔍 ДОПОЛНИТЕЛЬНЫЕ УЛУЧШЕНИЯ (ОПЦИОНАЛЬНО):

### 1. В AnesthesiologistServiceImpl:
Сейчас передается пустой список `new ArrayList<>()`. Можно улучшить, извлекая реальные диагнозы:

```java
// В методах approveEscalation() и rejectEscalation()
List<String> patientDiagnosisCodes = new ArrayList<>();
if (recommendation.getPatient() != null && recommendation.getPatient().getEmr() != null) {
    for (Emr emr : recommendation.getPatient().getEmr()) {
        if (emr.getDiagnoses() != null) {
            patientDiagnosisCodes.addAll(
                emr.getDiagnoses().stream()
                    .map(d -> d.getIcdCode())
                    .toList()
            );
        }
    }
}
```

### 2. Добавить диагнозы в другие события:
- **RecommendationApprovedEvent** - добавить диагнозы для анализа одобренных рекомендаций
- **RecommendationRejectedEvent** - добавить диагнозы для анализа отклоненных рекомендаций
- **VasRecordedEvent** - добавить диагнозы для корреляции боли с заболеваниями

---

## ✅ ПРОВЕРКА РАБОТОСПОСОБНОСТИ:

После внесения изменений в NurseServiceImpl:

1. Скомпилировать проект: `./mvnw clean compile`
2. Создать мокового пациента через EMR Integration Service
3. Проверить в MongoDB, что диагнозы сохранились:
```javascript
db.analytics_events.findOne({ eventType: "EMR_CREATED" })
```

Ожидаемый результат:
```json
{
  "_id": "...",
  "eventType": "EMR_CREATED",
  "diagnosisCodes": ["250.00", "401.9"],
  "diagnosisDescriptions": ["Diabetes mellitus...", "Essential hypertension..."],
  "metadata": {
    "diagnosisCount": 2,
    "diagnosisList": "250.00, 401.9"
  }
}
```

---

## 📝 ИТОГО:

**Интеграция диагнозов в аналитику позволяет:**
- Отслеживать какие заболевания чаще приводят к эскалациям
- Анализировать эффективность лечения по диагнозам
- Выявлять паттерны и улучшать протоколы лечения
- Генерировать отчеты для медицинского менеджмента
