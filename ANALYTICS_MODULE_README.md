# 📊 Analytics Module - Система аналитики и мониторинга

**Дата реализации:** 10.10.2025  
**Версия:** 1.0.0  
**Статус:** ✅ Готов к использованию

---

## 🎯 Назначение модуля

**Analytics Module** — это система **мониторинга, логирования и аналитики** для приложения Pain Management System. Модуль автоматически собирает данные о всех операциях в системе и предоставляет REST API для получения статистики.

### Основные функции:
1. **Бизнес-аналитика** — статистика по пациентам, рекомендациям, эскалациям
2. **Техническое логирование** — автоматическое логирование всех методов сервисов
3. **Мониторинг производительности** — отслеживание времени выполнения операций
4. **Аудит действий пользователей** — кто, когда и что делал в системе

---

## 🏗️ Архитектура модуля

```
┌─────────────────────────────────────────────────────────────────┐
│                    PAIN MANAGEMENT SYSTEM                       │
│  (DoctorService, NurseService, AnesthesiologistService, etc.)   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────────────────┐
                    │  Spring Events      │ ← Публикация событий
                    │  (Event Bus)        │
                    └─────────────────────┘
                              ↓
        ┌─────────────────────────────────────────────┐
        │     AnalyticsEventListener (@Async)         │
        │  - handleRecommendationApproved()           │
        │  - handleEscalationResolved()               │
        │  - handlePatientRegistered()                │
        │  - handleUserLogin()                        │
        └─────────────────────────────────────────────┘
                              ↓
                    ┌─────────────────────┐
                    │   MongoDB           │
                    │  - AnalyticsEvent   │
                    │  - LogEntry         │
                    └─────────────────────┘
                              ↑
        ┌─────────────────────────────────────────────┐
        │     LoggingAspect (AOP)                     │
        │  - Автоматическое логирование всех методов  │
        │  - Перехват через @Around                   │
        └─────────────────────────────────────────────┘
                              ↓
                    ┌─────────────────────┐
                    │  AnalyticsService   │
                    │  - Агрегация данных │
                    │  - Вычисление метрик│
                    └─────────────────────┘
                              ↓
                    ┌─────────────────────┐
                    │ AnalyticsController │
                    │  REST API для Admin │
                    └─────────────────────┘
```

---

## 📁 Структура модуля

```
analytics/
├── config/
│   └── AsyncConfig.java              # Конфигурация асинхронной обработки
├── aspect/
│   └── LoggingAspect.java            # AOP для автоматического логирования
├── entity/
│   ├── AnalyticsEvent.java           # MongoDB документ для бизнес-событий
│   ├── LogEntry.java                 # MongoDB документ для технических логов
│   └── PerformanceMetric.java        # MongoDB документ для метрик производительности
├── repository/
│   ├── AnalyticsEventRepository.java # Репозиторий для событий
│   ├── LogEntryRepository.java       # Репозиторий для логов
│   └── PerformanceMetricRepository.java
├── event/
│   ├── PersonCreatedEvent.java       # Событие: создан сотрудник
│   ├── PersonDeletedEvent.java       # Событие: удален сотрудник
│   ├── PersonUpdatedEvent.java       # Событие: обновлен сотрудник
│   ├── UserLoginEvent.java           # Событие: вход пользователя
│   ├── RecommendationApprovedEvent.java  # Событие: одобрена рекомендация
│   ├── EscalationCreatedEvent.java   # Событие: создана эскалация
│   ├── EscalationResolvedEvent.java  # Событие: разрешена эскалация
│   ├── PatientRegisteredEvent.java   # Событие: зарегистрирован пациент
│   └── VasRecordedEvent.java         # Событие: записан VAS
├── listener/
│   └── AnalyticsEventListener.java   # Слушатель событий (асинхронный)
├── dto/
│   ├── EventStatsDTO.java            # DTO для статистики событий
│   ├── UserActivityDTO.java          # DTO для активности пользователя
│   ├── PerformanceStatsDTO.java      # DTO для метрик производительности
│   └── PatientStatsDTO.java          # DTO для статистики пациентов
├── service/
│   └── AnalyticsService.java         # Сервис для агрегации и вычисления метрик
└── controller/
    └── AnalyticsController.java      # REST API для получения аналитики
```

---

## 🔄 Workflow: Как работает модуль

### **1. Бизнес-событие (Spring Events)**

```java
// DoctorService одобряет рекомендацию
public void approveRecommendation(Long id, RecommendationApprovalDTO dto) {
    // ... бизнес-логика ...
    
    // Публикуем событие (НЕ блокирует основной поток)
    eventPublisher.publishEvent(new RecommendationApprovedEvent(
        this, recommendationId, doctorId, patientMrn, ...
    ));
    
    return result; // Продолжаем работу СРАЗУ
}
```

### **2. Асинхронная обработка события**

```java
// AnalyticsEventListener получает событие в отдельном потоке
@EventListener
@Async("analyticsTaskExecutor") // ← Выполняется асинхронно!
public void handleRecommendationApproved(RecommendationApprovedEvent event) {
    // Создаем документ для MongoDB
    AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
        .timestamp(LocalDateTime.now())
        .eventType("RECOMMENDATION_APPROVED")
        .userId(event.getDoctorId())
        .patientMrn(event.getPatientMrn())
        .processingTimeMs(event.getProcessingTimeMs())
        .build();
    
    // Сохраняем в MongoDB
    analyticsEventRepository.save(analyticsEvent);
}
```

### **3. Техническое логирование (AOP)**

```java
// LoggingAspect автоматически перехватывает ВСЕ методы сервисов
@Around("execution(* pain_helper_back..service..*.*(..))")
public Object logServiceMethod(ProceedingJoinPoint joinPoint) {
    long startTime = System.currentTimeMillis();
    
    try {
        Object result = joinPoint.proceed(); // Выполняем метод
        return result;
    } catch (Exception e) {
        // Логируем ошибку
    } finally {
        long duration = System.currentTimeMillis() - startTime;
        
        // Сохраняем лог в MongoDB
        LogEntry logEntry = LogEntry.builder()
            .className(className)
            .methodName(methodName)
            .durationMs(duration)
            .success(exception == null)
            .build();
        
        logEntryRepository.save(logEntry);
    }
}
```

### **4. Получение аналитики через REST API**

```java
// Admin запрашивает статистику
GET /api/analytics/performance?startDate=2025-10-01T00:00:00&endDate=2025-10-10T23:59:59

// AnalyticsService агрегирует данные из MongoDB
public PerformanceStatsDTO getPerformanceStats(startDate, endDate) {
    List<AnalyticsEvent> events = repository.findByTimestampBetween(startDate, endDate);
    
    // Вычисляем метрики
    Long totalRecommendations = events.stream()
        .filter(e -> "RECOMMENDATION_APPROVED".equals(e.getEventType()))
        .count();
    
    Double avgTime = events.stream()
        .mapToLong(AnalyticsEvent::getProcessingTimeMs)
        .average()
        .orElse(0.0);
    
    return new PerformanceStatsDTO(avgTime, totalRecommendations, ...);
}
```

---

## 🎭 Роли и доступ

### **👨‍⚕️ ADMIN (Администратор больницы)**

**Доступ:** ✅ Полный доступ ко всем эндпоинтам Analytics API

**Функции:**
- Просмотр общей статистики системы
- Мониторинг активности пользователей
- Анализ производительности
- Просмотр технических логов
- Аудит действий персонала

**Dashboard компоненты:**
1. **Общая статистика** — количество событий по типам
2. **Активность пользователей** — кто работает в системе
3. **Производительность** — среднее время обработки рекомендаций
4. **Статистика пациентов** — регистрации, VAS записи
5. **Технические логи** — ошибки, медленные операции

### **👨‍⚕️ DOCTOR, NURSE, ANESTHESIOLOGIST**

**Доступ:** ❌ Нет доступа к Analytics API (только Admin)

**Причина:** Аналитика предназначена для управления и мониторинга системы, не для клинического персонала.

---

## 📡 REST API Endpoints

### **1. Статистика событий**

```http
GET /api/analytics/events/stats
Query params:
  - startDate (optional): 2025-10-01T00:00:00
  - endDate (optional): 2025-10-10T23:59:59

Response:
{
  "totalEvents": 1523,
  "eventsByType": {
    "RECOMMENDATION_APPROVED": 342,
    "ESCALATION_RESOLVED": 89,
    "PATIENT_REGISTERED": 156,
    "USER_LOGIN_SUCCESS": 523
  },
  "eventsByRole": {
    "DOCTOR": 450,
    "NURSE": 680,
    "ANESTHESIOLOGIST": 120,
    "ADMIN": 273
  },
  "eventsByStatus": {
    "APPROVED": 342,
    "RESOLVED": 89,
    "SUCCESS": 523
  }
}
```

### **2. Активность пользователя**

```http
GET /api/analytics/users/{userId}/activity
Path params:
  - userId: "doctor123"
Query params:
  - startDate (optional)
  - endDate (optional)

Response:
{
  "userId": "doctor123",
  "userRole": "DOCTOR",
  "totalActions": 145,
  "lastActivity": "2025-10-10T18:30:00",
  "loginCount": 23,
  "failedLoginCount": 2
}
```

### **3. Производительность системы**

```http
GET /api/analytics/performance
Query params:
  - startDate (optional)
  - endDate (optional)

Response:
{
  "averageProcessingTimeMs": 1250.5,
  "totalRecommendations": 342,
  "approvedRecommendations": 298,
  "rejectedRecommendations": 44,
  "totalEscalations": 89,
  "resolvedEscalations": 76,
  "averageEscalationResolutionTimeMs": 3600000
}
```

### **4. Статистика пациентов**

```http
GET /api/analytics/patients/stats
Query params:
  - startDate (optional)
  - endDate (optional)

Response:
{
  "totalPatients": 156,
  "patientsByGender": {
    "MALE": 78,
    "FEMALE": 78
  },
  "patientsByAgeGroup": {
    "18-29": 23,
    "30-44": 45,
    "45-59": 56,
    "60-74": 28,
    "75+": 4
  },
  "totalVasRecords": 523,
  "criticalVasRecords": 89,
  "averageVasLevel": 5.2
}
```

### **5. Последние события**

```http
GET /api/analytics/events/recent?limit=50

Response: [
  {
    "id": "67890abc",
    "timestamp": "2025-10-10T18:30:00",
    "eventType": "RECOMMENDATION_APPROVED",
    "userId": "doctor123",
    "userRole": "DOCTOR",
    "patientMrn": "000123",
    "processingTimeMs": 1200
  },
  ...
]
```

### **6. События по типу**

```http
GET /api/analytics/events/type/RECOMMENDATION_APPROVED
Query params:
  - startDate (optional)
  - endDate (optional)

Response: [ ... список событий ... ]
```

### **7. Технические логи**

```http
GET /api/analytics/logs/recent?limit=100

Response: [
  {
    "id": "log123",
    "timestamp": "2025-10-10T18:30:00",
    "className": "DoctorServiceImpl",
    "methodName": "approveRecommendation",
    "durationMs": 1250,
    "success": true,
    "module": "doctor"
  },
  ...
]
```

### **8. Логи по уровню**

```http
GET /api/analytics/logs/level/ERROR
Query params:
  - startDate (optional)
  - endDate (optional)

Response: [ ... список логов с ошибками ... ]
```

---

## 🧪 Как тестировать модуль

### **Предварительные требования:**

1. ✅ MongoDB запущен и доступен
2. ✅ Приложение запущено
3. ✅ В `application.properties` настроен MongoDB:
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/pain_management_analytics
```

---

### **Тест 1: Проверка автоматического логирования**

**Цель:** Убедиться, что LoggingAspect логирует все методы сервисов

**Шаги:**
1. Выполните любую операцию через API (например, создайте пациента)
```http
POST /api/nurse/patients
```

2. Проверьте логи в MongoDB:
```http
GET /api/analytics/logs/recent?limit=10
```

3. **Ожидаемый результат:**
```json
[
  {
    "className": "NurseServiceImpl",
    "methodName": "createPatient",
    "durationMs": 250,
    "success": true,
    "module": "nurse"
  }
]
```

---

### **Тест 2: Проверка бизнес-событий**

**Цель:** Убедиться, что события публикуются и сохраняются

**Шаги:**
1. Одобрите рекомендацию:
```http
PUT /api/doctor/recommendations/1/approve
{
  "comment": "Test approval"
}
```

2. Проверьте события:
```http
GET /api/analytics/events/type/RECOMMENDATION_APPROVED
```

3. **Ожидаемый результат:**
```json
[
  {
    "eventType": "RECOMMENDATION_APPROVED",
    "recommendationId": 1,
    "userId": "doctor123",
    "userRole": "DOCTOR",
    "processingTimeMs": 1200
  }
]
```

---

### **Тест 3: Проверка статистики**

**Цель:** Убедиться, что AnalyticsService корректно агрегирует данные

**Шаги:**
1. Выполните несколько операций:
   - Зарегистрируйте 3 пациентов
   - Одобрите 2 рекомендации
   - Создайте 1 эскалацию

2. Запросите статистику:
```http
GET /api/analytics/events/stats
```

3. **Ожидаемый результат:**
```json
{
  "totalEvents": 6,
  "eventsByType": {
    "PATIENT_REGISTERED": 3,
    "RECOMMENDATION_APPROVED": 2,
    "ESCALATION_CREATED": 1
  }
}
```

---

### **Тест 4: Проверка производительности**

**Цель:** Убедиться, что метрики производительности вычисляются корректно

**Шаги:**
1. Выполните несколько операций с рекомендациями

2. Запросите метрики:
```http
GET /api/analytics/performance
```

3. **Ожидаемый результат:**
```json
{
  "averageProcessingTimeMs": 1250.5,
  "totalRecommendations": 5,
  "approvedRecommendations": 4,
  "rejectedRecommendations": 1
}
```

---

### **Тест 5: Проверка фильтрации по датам**

**Цель:** Убедиться, что фильтрация по временному диапазону работает

**Шаги:**
1. Запросите статистику за определенный период:
```http
GET /api/analytics/events/stats?startDate=2025-10-01T00:00:00&endDate=2025-10-10T23:59:59
```

2. **Ожидаемый результат:** Только события в указанном диапазоне

---

### **Тест 6: Проверка активности пользователя**

**Цель:** Убедиться, что активность пользователя отслеживается

**Шаги:**
1. Выполните вход пользователя:
```http
POST /api/person/login
{
  "login": "doctor123",
  "password": "password"
}
```

2. Проверьте активность:
```http
GET /api/analytics/users/doctor123/activity
```

3. **Ожидаемый результат:**
```json
{
  "userId": "doctor123",
  "loginCount": 1,
  "failedLoginCount": 0,
  "totalActions": 5
}
```

---

### **Тест 7: Проверка обработки ошибок**

**Цель:** Убедиться, что ошибки логируются корректно

**Шаги:**
1. Выполните операцию, которая вызовет ошибку (например, несуществующий пациент)

2. Проверьте логи ошибок:
```http
GET /api/analytics/logs/level/ERROR
```

3. **Ожидаемый результат:**
```json
[
  {
    "level": "ERROR",
    "success": false,
    "errorMessage": "Patient not found"
  }
]
```

---

## 🔧 Конфигурация

### **AsyncConfig.java**

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "analyticsTaskExecutor")
    public Executor analyticsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // 5 потоков
        executor.setMaxPoolSize(10);        // Максимум 10
        executor.setQueueCapacity(100);     // Очередь на 100 задач
        executor.setThreadNamePrefix("analytics-");
        executor.initialize();
        return executor;
    }
}
```

**Параметры:**
- `corePoolSize=5` — постоянно работают 5 потоков
- `maxPoolSize=10` — при нагрузке до 10 потоков
- `queueCapacity=100` — очередь на 100 задач

---

## 📊 Типы событий

| Событие | Описание | Публикуется в |
|---------|----------|---------------|
| `PERSON_CREATED` | Создан сотрудник | AdminServiceImpl.createPerson() |
| `PERSON_DELETED` | Удален сотрудник | AdminServiceImpl.deletePerson() |
| `PERSON_UPDATED` | Обновлен сотрудник | AdminServiceImpl.updatePerson() |
| `USER_LOGIN_SUCCESS` | Успешный вход | PersonService.login() |
| `USER_LOGIN_FAILED` | Неудачный вход | PersonService.login() |
| `RECOMMENDATION_APPROVED` | Одобрена рекомендация | DoctorServiceImpl.approveRecommendation() |
| `RECOMMENDATION_REJECTED` | Отклонена рекомендация | DoctorServiceImpl.rejectRecommendation() |
| `ESCALATION_CREATED` | Создана эскалация | DoctorServiceImpl.rejectRecommendation() |
| `ESCALATION_RESOLVED` | Разрешена эскалация | AnesthesiologistServiceImpl.approveEscalation() |
| `PATIENT_REGISTERED` | Зарегистрирован пациент | NurseServiceImpl.createPatient() |
| `VAS_RECORDED` | Записан VAS | NurseServiceImpl.createVAS() |

---

## ⚡ Производительность

### **Асинхронная обработка**
- ✅ События обрабатываются в отдельных потоках
- ✅ Основной поток НЕ блокируется
- ✅ Если MongoDB недоступен, основная логика продолжает работать

### **Оптимизация запросов**
- ✅ Индексы на `timestamp`, `eventType`, `userId`, `level`
- ✅ Пагинация для больших результатов
- ✅ Фильтрация по датам для уменьшения объема данных

---

## 🎯 Отчетность о проделанной работе

### **Реализовано:**

✅ **MongoDB интеграция**
- Добавлены зависимости в pom.xml
- Настроен MongoDB в application.properties
- Созданы 3 коллекции: AnalyticsEvent, LogEntry, PerformanceMetric

✅ **Асинхронная обработка**
- AsyncConfig с ThreadPoolTaskExecutor
- @Async на всех event listeners
- Не блокирует основной поток

✅ **AOP логирование**
- LoggingAspect перехватывает все методы сервисов
- Автоматическое логирование времени выполнения
- Логирование ошибок и stack traces

✅ **Spring Events**
- 9 типов бизнес-событий
- Event-driven архитектура
- Слабая связанность модулей

✅ **Event Listeners**
- AnalyticsEventListener обрабатывает все события
- Асинхронное сохранение в MongoDB
- Обработка ошибок без падения основного потока

✅ **Интеграция в сервисы**
- DoctorServiceImpl: 3 события
- AnesthesiologistServiceImpl: 2 события
- AdminServiceImpl: 3 события
- PersonService: 2 события
- NurseServiceImpl: 2 события

✅ **Analytics Service**
- Агрегация данных из MongoDB
- Вычисление метрик производительности
- Статистика по пациентам, пользователям, событиям

✅ **REST API**
- 8 эндпоинтов для получения аналитики
- Фильтрация по датам
- Пагинация для больших результатов

✅ **DTO классы**
- EventStatsDTO
- UserActivityDTO
- PerformanceStatsDTO
- PatientStatsDTO

---

## 📝 Следующие шаги (опционально)

1. **Security** — добавить проверку роли ADMIN для Analytics API
2. **Grafana Dashboard** — визуализация метрик
3. **Alerts** — уведомления при критических событиях
4. **Export** — экспорт отчетов в PDF/Excel
5. **Real-time updates** — WebSocket для live dashboard

---

## 👨‍💻 Автор

**Разработано:** 10.10.2025  
**Модуль:** Analytics & Monitoring  
**Технологии:** Spring Boot, MongoDB, AOP, Spring Events, Async Processing
