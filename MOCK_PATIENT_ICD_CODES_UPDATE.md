# Mock Patient ICD Codes Update

**Дата:** 19.10.2025  
**Автор:** AI Assistant  
**Версия:** 1.0

---

## 📋 ПРОБЛЕМА

Раньше моковые пациенты создавались с **любыми случайными диагнозами** из полного списка ICD-9-CM кодов (14,000+ кодов).

**Проблемы:**
- Большинство диагнозов не влияли на выбор лечения
- Тестирование было непредсказуемым
- Невозможно было проверить работу contraindications rules
- Моковые данные не соответствовали реальным сценариям

---

## ✅ РЕШЕНИЕ

Теперь моковые пациенты создаются **ТОЛЬКО с диагнозами из Treatment Protocol** (колонка `contraindications`).

**Преимущества:**
- ✅ Все диагнозы влияют на выбор лечения
- ✅ Тестирование стало предсказуемым
- ✅ Можно проверить работу contraindications rules
- ✅ Моковые данные соответствуют реальным сценариям

---

## 🔧 ТЕХНИЧЕСКИЕ ИЗМЕНЕНИЯ

### 1. Новый сервис: `TreatmentProtocolIcdExtractor`

**Файл:** `src/main/java/pain_helper_back/external_emr_integration_service/service/TreatmentProtocolIcdExtractor.java`

**Функции:**
- Извлекает все ICD коды из `treatment_protocol.contraindications`
- Использует регулярное выражение для парсинга кодов
- Предоставляет методы для получения случайных диагнозов
- Кэширует коды в памяти для быстрого доступа

**Методы:**
```java
// Получить случайный диагноз из протокола
public IcdCode getRandomProtocolDiagnosis()

// Получить N случайных диагнозов из протокола
public List<IcdCode> getRandomProtocolDiagnoses(int count)

// Получить количество доступных кодов
public int getProtocolIcdCodesCount()
```

### 2. Обновлен: `MockEmrDataGenerator`

**Изменения:**
```java
// БЫЛО:
private final IcdCodeLoaderService icdCodeLoaderService;
return icdCodeLoaderService.getRandomDiagnoses(diagnosisCount);

// СТАЛО:
private final TreatmentProtocolIcdExtractor treatmentProtocolIcdExtractor;
return treatmentProtocolIcdExtractor.getRandomProtocolDiagnoses(diagnosisCount);
```

### 3. Новый endpoint: `GET /api/emr/protocol-icd-codes`

**Использование:**
```bash
curl http://localhost:8080/api/emr/protocol-icd-codes
```

**Ответ:**
```json
{
  "count": 15,
  "message": "Mock patients are generated with 15 ICD codes from Treatment Protocol",
  "info": "These are the contraindication codes that affect treatment selection"
}
```

---

## 📊 ПРИМЕРЫ ICD КОДОВ ИЗ TREATMENT PROTOCOL

Типичные коды, которые извлекаются из `contraindications`:

| ICD Code | Description |
|----------|-------------|
| 571.2 | Alcoholic cirrhosis of liver |
| 571.5 | Cirrhosis of liver without mention of alcohol |
| 571.9 | Unspecified chronic liver disease |
| V45.11 | Renal dialysis status |
| E11.9 | Type 2 diabetes mellitus without complications |
| I50.9 | Heart failure, unspecified |
| K70.3 | Alcoholic cirrhosis of liver |
| K74.6 | Other and unspecified cirrhosis of liver |
| N18.6 | End stage renal disease |
| Z99.2 | Dependence on renal dialysis |

---

## 🧪 ТЕСТИРОВАНИЕ

### 1. Проверить количество ICD кодов

```bash
curl http://localhost:8080/api/emr/protocol-icd-codes
```

### 2. Создать мокового пациента

```bash
curl -X POST "http://localhost:8080/api/emr/mock/generate?createdBy=test"
```

### 3. Проверить диагнозы в БД

```sql
SELECT d.icd_code, d.description, p.mrn
FROM diagnosis d
JOIN emr e ON d.emr_id = e.id
JOIN nurse_patients p ON e.patient_id = p.id
WHERE p.mrn LIKE 'EMR-%'
ORDER BY p.id DESC
LIMIT 10;
```

### 4. Проверить, что contraindications работают

```bash
# Создать 10 моковых пациентов
curl -X POST "http://localhost:8080/api/emr/mock/generate-batch?count=10&createdBy=test"

# Создать рекомендацию для пациента с противопоказанием
# Должны сработать contraindications rules
```

---

## 📝 ЛОГИ

При старте приложения в логах будет:

```
INFO  - Loading ICD codes from CSV...
INFO  - Successfully loaded 14567 ICD codes
INFO  - Extracting ICD codes from Treatment Protocol contraindications...
INFO  - Found 15 unique ICD codes in Treatment Protocol
INFO  - Successfully extracted 15 ICD codes from Treatment Protocol:
DEBUG   - 571.2 : Alcoholic cirrhosis of liver
DEBUG   - 571.5 : Cirrhosis of liver without mention of alcohol
DEBUG   - 571.9 : Unspecified chronic liver disease
...
```

---

## 🔍 КАК ЭТО РАБОТАЕТ

### Workflow:

1. **При старте приложения:**
   - `TreatmentProtocolLoader` загружает `treatment_protocol.xlsx`
   - `TreatmentProtocolIcdExtractor` извлекает ICD коды из `contraindications`
   - Коды кэшируются в памяти

2. **При генерации мокового пациента:**
   - `MockEmrDataGenerator.generateDiagnosesForPatient()` вызывается
   - Определяется количество диагнозов (1-5)
   - `TreatmentProtocolIcdExtractor.getRandomProtocolDiagnoses(count)` возвращает случайные коды
   - Диагнозы сохраняются в БД

3. **При создании рекомендации:**
   - `ContraindicationsRuleApplier` проверяет диагнозы пациента
   - Если найдено совпадение с `contraindications` - препарат исключается
   - Теперь это работает корректно, т.к. моковые пациенты имеют релевантные диагнозы

---

## 🎯 РЕЗУЛЬТАТ

**До изменений:**
- Моковые пациенты: случайные 14,000+ ICD кодов
- Contraindications срабатывали редко
- Тестирование было непредсказуемым

**После изменений:**
- Моковые пациенты: только ~15-20 ICD кодов из Treatment Protocol
- Contraindications срабатывают часто
- Тестирование стало предсказуемым и реалистичным

---

## 📚 СВЯЗАННЫЕ ФАЙЛЫ

- `TreatmentProtocolIcdExtractor.java` - новый сервис
- `MockEmrDataGenerator.java` - обновлен
- `EmrIntegrationController.java` - добавлен endpoint
- `ContraindicationsRuleApplier.java` - использует эти коды
- `treatment_protocol.xlsx` - источник данных

---

## 🚀 СЛЕДУЮЩИЕ ШАГИ

1. ✅ Протестировать генерацию моковых пациентов
2. ✅ Проверить работу contraindications rules
3. ✅ Убедиться, что все диагнозы из протокола извлечены корректно
4. 🔄 Добавить unit-тесты для `TreatmentProtocolIcdExtractor`
5. 🔄 Добавить интеграционные тесты

---

**Вопросы?** Проверьте логи приложения или используйте endpoint `/api/emr/protocol-icd-codes`
