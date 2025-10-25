# 📊 Performance SLA Monitoring Module

**Дата создания:** 23.10.2025  
**Версия:** 1.0.0  
**Статус:** ✅ Полностью реализовано  
**Разработчик:** Nick

---

## 🎯 НАЗНАЧЕНИЕ

Модуль автоматического мониторинга производительности и контроля SLA (Service Level Agreement) для всех критических операций системы Pain Management Assistant.

### Основные задачи:
- ✅ Автоматический сбор метрик производительности через AOP
- ✅ Контроль соблюдения SLA для критических операций
- ✅ Детекция и логирование нарушений SLA в реальном времени
- ✅ Предоставление статистики и KPI через REST API
- ✅ Хранение метрик в MongoDB для долгосрочного анализа

---

## 🏗️ АРХИТЕКТУРА

```
performance_SLA_monitoring/
├── aspect/
│   └── PerformanceMonitoringAspect.java      # AOP для автоматического измерения
├── config/
│   └── PerformanceSlaConfig.java             # SLA пороги (17 операций)
├── controller/
│   └── PerformanceController.java            # REST API (10 эндпоинтов)
├── dto/
│   ├── PerformanceMetricDTO.java             # Метрика производительности
│   ├── SlaViolationDTO.java                  # Нарушение SLA
│   └── PerformanceStatisticDTO.java          # Статистика с перцентилями
├── entity/
│   └── PerformanceMetric.java                # MongoDB документ
├── repository/
│   └── PerformanceMetricRepository.java      # MongoDB repository
└── service/
    ├── PerformanceMonitoringService.java     # Interface
    └── PerformanceMonitoringServiceImpl.java # Реализация с расчетами
```

---

## 📋 SLA ПОРОГИ

| Операция | SLA Порог | Описание |
|----------|-----------|----------|
| **Рекомендации** | | |
| `recommendation.generate` | 2000ms | Генерация рекомендации по протоколу |
| `recommendation.approve` | 1000ms | Одобрение рекомендации врачом |
| `recommendation.reject` | 1000ms | Отклонение рекомендации |
| **Данные пациента** | | |
| `patient.load` | 3000ms | Загрузка данных пациента |
| `vas.create` | 1000ms | Создание VAS записи |
| `emr.create` | 2000ms | Создание EMR записи |
| `emr.sync` | 5000ms | Синхронизация с внешней EMR |
| **Эскалации** | | |
| `escalation.check` | 1500ms | Проверка необходимости эскалации |
| `escalation.create` | 1000ms | Создание эскалации |
| `escalation.resolve` | 1000ms | Разрешение эскалации |
| **Протоколы** | | |
| `protocol.load` | 2000ms | Загрузка протокола лечения |
| `protocol.apply` | 1500ms | Применение правил протокола |
| **Отчеты** | | |
| `report.generate` | 5000ms | Генерация отчета |
| `report.export` | 3000ms | Экспорт отчета (PDF/Excel) |
| **Аналитика** | | |
| `analytics.query` | 2000ms | Запрос аналитических данных |
| `kpi.calculate` | 3000ms | Расчет KPI метрик |

---

## 🚀 REST API ENDPOINTS

### 1. Статистика производительности

#### Получить полную статистику за период
```http
GET /api/performance/statistics?start=2025-10-23T00:00:00&end=2025-10-23T23:59:59

Response:
{
  "totalOperations": 1500,
  "successfulOperations": 1480,
  "failedOperations": 20,
  "slaViolations": 45,
  "slaViolationRate": 3.0,
  "averageExecutionTimeMs": 850.5,
  "minExecutionTimeMs": 120,
  "maxExecutionTimeMs": 4500,
  "medianExecutionTimeMs": 750,
  "p95ExecutionTimeMs": 1800,
  "p99ExecutionTimeMs": 2500,
  "operationStats": {
    "recommendation.generate": {
      "operationName": "recommendation.generate",
      "count": 250,
      "averageTimeMs": 1200.5,
      "slaThresholdMs": 2000,
      "violations": 8,
      "violationRate": 3.2,
      "minTimeMs": 800,
      "maxTimeMs": 2450
    }
  },
  "slowestOperations": [...],
  "recentViolations": [...],
  "hourlyOperationCount": {...},
  "hourlyAverageTime": {...}
}
```

#### Статистика за последние N часов
```http
GET /api/performance/statistics/recent?hours=24
```

---

### 2. Метрики производительности

#### Получить все метрики за период
```http
GET /api/performance/metrics?start=2025-10-23T00:00:00&end=2025-10-23T23:59:59

Response:
[
  {
    "id": "67890abc",
    "operationName": "recommendation.generate",
    "executionTimeMs": 1850,
    "slaThresholdMs": 2000,
    "slaViolated": false,
    "slaPercentage": 92.5,
    "methodName": "NurseServiceImpl.generateRecommendation",
    "userId": "nurse_123",
    "userRole": "NURSE",
    "patientMrn": "EMR-A1B2C3D4",
    "status": "SUCCESS",
    "errorMessage": null,
    "timestamp": "2025-10-23T14:30:00"
  }
]
```

---

### 3. SLA Нарушения

#### Получить нарушения SLA за период
```http
GET /api/performance/sla-violations?start=2025-10-23T00:00:00&end=2025-10-23T23:59:59

Response:
[
  {
    "operationName": "recommendation.generate",
    "executionTimeMs": 2450,
    "slaThresholdMs": 2000,
    "excessTimeMs": 450,
    "slaPercentage": 122.5,
    "methodName": "NurseServiceImpl.generateRecommendation",
    "userId": "nurse_456",
    "patientMrn": "EMR-X9Y8Z7",
    "timestamp": "2025-10-23T15:45:00",
    "errorMessage": null
  }
]
```

#### Нарушения за последние N часов
```http
GET /api/performance/sla-violations/recent?hours=24
```

---

### 4. Статистика по операции

```http
GET /api/performance/operations/recommendation.generate/statistics?start=2025-10-23T00:00:00&end=2025-10-23T23:59:59

Response:
{
  "operationName": "recommendation.generate",
  "count": 250,
  "averageTimeMs": 1200.5,
  "slaThresholdMs": 2000,
  "violations": 8,
  "violationRate": 3.2,
  "minTimeMs": 800,
  "maxTimeMs": 2450
}
```

---

### 5. Топ медленных операций

```http
GET /api/performance/slowest?limit=10&hours=24

Response:
[
  {
    "id": "abc123",
    "operationName": "emr.sync",
    "executionTimeMs": 4800,
    "slaThresholdMs": 5000,
    "slaViolated": false,
    "slaPercentage": 96.0,
    ...
  }
]
```

---

### 6. Метрики по пациенту

```http
GET /api/performance/patients/EMR-A1B2C3D4/metrics

Response: [список всех метрик для пациента]
```

---

### 7. Метрики по пользователю

```http
GET /api/performance/users/nurse_123/metrics

Response: [список всех метрик пользователя]
```

---

### 8. Очистка старых метрик

```http
DELETE /api/performance/cleanup?daysToKeep=30

Response: "Old metrics cleaned up successfully"
```

---

## 🔧 КАК РАБОТАЕТ

### Автоматический мониторинг через AOP

`PerformanceMonitoringAspect` использует Spring AOP для автоматического перехвата:

```java
@Around("execution(* pain_helper_back..service..*ServiceImpl.*(..))")
public Object monitorServiceMethods(ProceedingJoinPoint joinPoint)

@Around("execution(* pain_helper_back..controller..*Controller.*(..))")
public Object monitorControllerMethods(ProceedingJoinPoint joinPoint)
```

### Процесс мониторинга:

1. **Перехват вызова метода**
   - AOP Aspect перехватывает вызов
   - Засекает время начала

2. **Выполнение метода**
   - Метод выполняется как обычно
   - Отслеживается статус (SUCCESS/ERROR)

3. **Расчет метрик**
   - Вычисляется время выполнения
   - Проверяется SLA порог
   - Рассчитывается процент от SLA

4. **Сохранение в MongoDB**
   - Метрика сохраняется асинхронно
   - Не блокирует основной поток

5. **Логирование нарушений**
   ```
   WARN - SLA VIOLATION: recommendation.generate took 2450ms 
          (threshold: 2000ms, 122.5% of SLA)
   ```

---

## 📊 СТАТИСТИКА И АНАЛИТИКА

### PerformanceStatisticDTO включает:

#### Общая статистика:
- `totalOperations` - всего операций
- `successfulOperations` - успешных
- `failedOperations` - с ошибками
- `slaViolations` - нарушений SLA
- `slaViolationRate` - процент нарушений

#### Времена выполнения:
- `averageExecutionTimeMs` - среднее время
- `minExecutionTimeMs` - минимальное
- `maxExecutionTimeMs` - максимальное
- `medianExecutionTimeMs` - медиана
- `p95ExecutionTimeMs` - 95-й перцентиль
- `p99ExecutionTimeMs` - 99-й перцентиль

#### Детализация по операциям:
- Статистика каждой операции отдельно
- Сравнение с SLA порогом
- Процент нарушений

#### Топы и тренды:
- Топ-10 медленных операций
- Последние нарушения SLA
- Почасовое распределение операций
- Почасовое среднее время

---

## 💡 ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ

### Пример 1: Мониторинг за сутки

```bash
curl "http://localhost:8080/api/performance/statistics/recent?hours=24"
```

### Пример 2: Найти все нарушения SLA

```bash
curl "http://localhost:8080/api/performance/sla-violations/recent?hours=24"
```

### Пример 3: Топ-10 медленных операций

```bash
curl "http://localhost:8080/api/performance/slowest?limit=10&hours=24"
```

### Пример 4: Статистика конкретной операции

```bash
curl "http://localhost:8080/api/performance/operations/recommendation.generate/statistics?start=2025-10-23T00:00:00&end=2025-10-23T23:59:59"
```

### Пример 5: Метрики по пациенту

```bash
curl "http://localhost:8080/api/performance/patients/EMR-A1B2C3D4/metrics"
```

---

## ⚙️ КОНФИГУРАЦИЯ

### application.properties

```properties
# Performance SLA Monitoring Configuration
performance.sla.enabled=true
performance.sla.async-recording=true

# MongoDB для метрик
spring.data.mongodb.uri=mongodb://localhost:27017/pain_management_analytics
```

### Кастомные SLA пороги

Можно изменить в `PerformanceSlaConfig.java`:

```java
public PerformanceSlaConfig() {
    thresholds.put("custom.operation", 3000L);
    thresholds.put("recommendation.generate", 1500L); // Ужесточить порог
}
```

---

## 🧪 ТЕСТИРОВАНИЕ

### Ручное тестирование

1. **Запустить приложение**
   ```bash
   mvn spring-boot:run
   ```

2. **Выполнить несколько операций**
   - Создать пациента
   - Сгенерировать рекомендацию
   - Одобрить рекомендацию

3. **Проверить метрики**
   ```bash
   curl "http://localhost:8080/api/performance/statistics/recent?hours=1"
   ```

4. **Проверить MongoDB**
   ```javascript
   use pain_management_analytics
   db.performance_metrics.find().limit(10)
   ```

### Проверка нарушений SLA

1. **Создать медленную операцию** (для теста)
   - Добавить `Thread.sleep(3000)` в метод

2. **Проверить логи**
   ```
   WARN - SLA VIOLATION: recommendation.generate took 3200ms 
          (threshold: 2000ms, 160.0% of SLA)
   ```

3. **Проверить через API**
   ```bash
   curl "http://localhost:8080/api/performance/sla-violations/recent?hours=1"
   ```

---

## 🎯 СООТВЕТСТВИЕ ТРЕБОВАНИЯМ SRS

### Requirement 4.2: Performance
> "The system shall generate recommendations within [specific time frame]"

✅ **Реализовано:**
- SLA порог 2000ms для генерации рекомендаций
- Автоматический мониторинг всех операций
- Алерты при превышении порога

### Requirement 3.5: KPI Tracking
> "The system shall track and display Key Performance Indicators (KPIs) to measure effectiveness and time savings"

✅ **Реализовано:**
- Сбор метрик для всех операций
- Расчет KPI (avg, min, max, p95, p99)
- REST API для отображения KPI
- Статистика по операциям/пациентам/пользователям

### Дополнительно:
- ✅ Автоматический сбор метрик (без ручного вмешательства)
- ✅ Real-time алерты при нарушениях SLA
- ✅ Долгосрочное хранение в MongoDB
- ✅ Детальная аналитика с перцентилями

---

## 🔮 БУДУЩИЕ УЛУЧШЕНИЯ

### Фаза 2 (Опционально):

1. **Grafana Dashboard**
   - Визуализация метрик в реальном времени
   - Графики трендов производительности
   - Алерты на дашборде

2. **Prometheus Integration**
   - Экспорт метрик в формате Prometheus
   - Интеграция с Prometheus Alertmanager
   - Долгосрочное хранение временных рядов

3. **Email/SMS алерты**
   - Уведомления при критических нарушениях SLA
   - Еженедельные отчеты по производительности
   - Алерты для DevOps команды

4. **Автоматическое масштабирование**
   - Детекция деградации производительности
   - Триггеры для автоскейлинга
   - Интеграция с Kubernetes HPA

5. **ML предсказание узких мест**
   - Анализ паттернов производительности
   - Предсказание будущих проблем
   - Рекомендации по оптимизации

---

## 📈 МЕТРИКИ МОДУЛЯ

| Компонент | Количество |
|-----------|------------|
| **Классы** | 10 |
| **REST эндпоинты** | 10 |
| **SLA пороги** | 17 операций |
| **DTO** | 3 |
| **Автоматический мониторинг** | ✅ Все сервисы + контроллеры |
| **Хранилище** | MongoDB |
| **Retention** | 30 дней (настраиваемо) |
| **Перцентили** | p95, p99 |

---

## 📝 CHANGELOG

### Version 1.0.0 (23.10.2025)
- ✅ Создан модуль Performance SLA Monitoring
- ✅ Реализован AOP аспект для автоматического мониторинга
- ✅ Добавлено 17 SLA порогов для критических операций
- ✅ Создан REST API с 10 эндпоинтами
- ✅ Реализован расчет статистики с перцентилями
- ✅ Добавлено логирование нарушений SLA
- ✅ Интеграция с MongoDB для хранения метрик
- ✅ Документация модуля

---

## 👨‍💻 РАЗРАБОТЧИК

**Nick**  
**Дата:** 23.10.2025  
**Статус:** ✅ Production Ready

---

## 📚 СВЯЗАННАЯ ДОКУМЕНТАЦИЯ

- [Модуль Аналитики](ANALYTICS_MODULE_README.md)
- [Workflow README](../WORKFLOW_README.md)
- [Нереализованные функции](UNIMPLEMENTED_FEATURES.md)

---

**Модуль готов к использованию в production! 🚀**
