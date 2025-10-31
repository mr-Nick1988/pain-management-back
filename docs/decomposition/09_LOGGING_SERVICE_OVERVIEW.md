# 09 - Logging Service - Обзор ⭐ ПРИОРИТЕТ #1

**Предыдущий:** [08_KAFKA_SETUP.md](08_KAFKA_SETUP.md)  
**Следующий:** [10_LOGGING_SERVICE_IMPLEMENTATION.md](10_LOGGING_SERVICE_IMPLEMENTATION.md)

---

## 🎯 Зачем выделять Logging Service первым

### Проблемы текущей реализации

❌ **Нагрузка на основную систему**
- Логирование выполняется синхронно в монолите
- Каждый запрос пишет в MongoDB
- MongoDB используется совместно с аналитикой

❌ **Невозможность масштабирования**
- Нельзя масштабировать логирование отдельно
- При росте нагрузки страдает вся система

❌ **Риск потери данных**
- Если MongoDB недоступна, логи теряются
- Нет буферизации

### Преимущества микросервиса

✅ **Изоляция нагрузки**
- Логирование не влияет на основную систему
- Dedicated MongoDB instance
- Асинхронная обработка через Kafka

✅ **Масштабируемость**
- Независимое масштабирование
- Можно запустить несколько инстансов
- Kafka обеспечивает балансировку нагрузки

✅ **Надежность**
- Kafka буферизует сообщения
- Retry механизмы
- Не влияет на бизнес-логику при сбоях

✅ **Производительность**
- Снижение нагрузки на монолит на **30-40%**
- Асинхронная обработка
- Batch processing возможен

---

## 📐 Архитектура

```
┌─────────────────────────────────────────────────────────┐
│              Monolith (Core Business)                    │
│  Admin │ Doctor │ Nurse │ Anesthesiologist │ etc.      │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ @LogOperation (AOP Aspect)
                     │ Асинхронная отправка
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Kafka Producer (в монолите)                 │
│  Topic: "logging-events"                                 │
│  Serialization: JSON                                     │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Kafka
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   Apache Kafka                           │
│  Topic: logging-events                                   │
│  Partitions: 3                                           │
│  Retention: 7 days                                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Consumer Group
                     ▼
┌─────────────────────────────────────────────────────────┐
│           Logging Service (Microservice)                 │
│  ┌─────────────────────────────────────────────┐        │
│  │  Kafka Consumer (3 instances)               │        │
│  │  - Manual acknowledgment                    │        │
│  │  - Error handling                           │        │
│  │  - Batch processing                         │        │
│  └──────────────────┬──────────────────────────┘        │
│                     │                                    │
│                     ▼                                    │
│  ┌─────────────────────────────────────────────┐        │
│  │  Logging Service                            │        │
│  │  - Validation                               │        │
│  │  - Enrichment (metadata)                    │        │
│  │  - Persistence                              │        │
│  └──────────────────┬──────────────────────────┘        │
│                     │                                    │
│                     ▼                                    │
│  ┌─────────────────────────────────────────────┐        │
│  │  REST API                                   │        │
│  │  - Search logs                              │        │
│  │  - Get by user/service                      │        │
│  │  - Error logs                               │        │
│  └─────────────────────────────────────────────┘        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│        MongoDB (Dedicated Instance)                      │
│  Database: logging_db                                    │
│  Collections:                                            │
│    - log_entries (indexed)                              │
│    - audit_trail (indexed)                              │
└─────────────────────────────────────────────────────────┘
```

---

## 📊 Структура данных

### LogEvent (Kafka Message)

```java
{
  "id": "uuid",
  "timestamp": "2025-10-31T16:00:00",
  "serviceName": "monolith",
  "operation": "createPatient",
  "userId": "doctor-123",
  "userName": "Dr. Smith",
  "logLevel": "INFO",
  "methodName": "createPatient",
  "className": "DoctorController",
  "parameters": {
    "firstName": "John",
    "lastName": "Doe"
  },
  "result": {
    "patientId": "patient-456"
  },
  "executionTimeMs": 150,
  "traceId": "trace-789",
  "spanId": "span-012",
  "metadata": {
    "ipAddress": "192.168.1.1",
    "userAgent": "Mozilla/5.0..."
  }
}
```

### LogEntry (MongoDB Document)

```javascript
{
  "_id": ObjectId("..."),
  "timestamp": ISODate("2025-10-31T16:00:00Z"),
  "serviceName": "monolith",
  "operation": "createPatient",
  "userId": "doctor-123",
  "userName": "Dr. Smith",
  "logLevel": "INFO",
  "methodName": "createPatient",
  "className": "DoctorController",
  "parameters": { ... },
  "result": { ... },
  "errorMessage": null,
  "stackTrace": null,
  "executionTimeMs": 150,
  "traceId": "trace-789",
  "spanId": "span-012",
  "metadata": { ... }
}
```

---

## 🔄 Поток данных

### 1. Генерация события (Монолит)

```java
@LogOperation
public Patient createPatient(PatientCreationDTO dto) {
    // Бизнес-логика
    Patient patient = patientService.create(dto);
    
    // AOP автоматически создает LogEvent и отправляет в Kafka
    // Это происходит АСИНХРОННО
    
    return patient;
}
```

### 2. Отправка в Kafka (Монолит)

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Around("@annotation(LogOperation)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        LogEvent event = createLogEvent(joinPoint);
        
        try {
            Object result = joinPoint.proceed();
            event.setResult(result);
            event.setLogLevel("INFO");
            
            // Асинхронная отправка в Kafka
            kafkaTemplate.send("logging-events", event);
            
            return result;
        } catch (Exception e) {
            event.setLogLevel("ERROR");
            event.setErrorMessage(e.getMessage());
            kafkaTemplate.send("logging-events", event);
            throw e;
        }
    }
}
```

### 3. Обработка в Logging Service

```java
@KafkaListener(topics = "logging-events")
public void consumeLogEvent(LogEvent event, Acknowledgment ack) {
    try {
        // Валидация
        validate(event);
        
        // Обогащение метаданными
        enrich(event);
        
        // Сохранение в MongoDB
        LogEntry logEntry = mapToLogEntry(event);
        logEntryRepository.save(logEntry);
        
        // Подтверждение обработки
        ack.acknowledge();
        
    } catch (Exception e) {
        log.error("Error processing log event", e);
        // Не acknowledge - сообщение будет обработано повторно
    }
}
```

---

## 📈 Ожидаемые результаты

### Производительность

| Метрика | До миграции | После миграции | Улучшение |
|---------|-------------|----------------|-----------|
| **Latency монолита (p95)** | 250ms | 180ms | ✅ -28% |
| **CPU монолита** | 75% | 50% | ✅ -33% |
| **MongoDB нагрузка** | 100% | 20% (только бизнес) | ✅ -80% |
| **Throughput логирования** | 1000 msg/s | 5000 msg/s | ✅ +400% |

### Надежность

- ✅ Логирование не блокирует бизнес-логику
- ✅ Kafka буферизует сообщения (retention 7 days)
- ✅ Retry механизмы при сбоях
- ✅ Circuit Breaker защищает от каскадных сбоев

---

## 🔧 Технологии

### Logging Service

- **Framework:** Spring Boot 3.5.5
- **Java:** 21
- **Database:** MongoDB 7.0 (dedicated)
- **Message Broker:** Apache Kafka 3.6+
- **Service Discovery:** Eureka Client
- **Config:** Spring Cloud Config Client
- **Monitoring:** Micrometer + Prometheus
- **Tracing:** Zipkin

### Зависимости

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>
```

---

## 📋 API Endpoints

### Query Logs

```
GET /api/logs/search
POST /api/logs/search
GET /api/logs/user/{userId}
GET /api/logs/service/{serviceName}
GET /api/logs/errors
GET /api/logs/{id}
```

### Примеры запросов

```bash
# Поиск логов по пользователю
curl "http://localhost:8081/api/logs/user/doctor-123?from=2025-10-31T00:00:00&to=2025-10-31T23:59:59"

# Поиск ошибок
curl "http://localhost:8081/api/logs/errors?from=2025-10-31T00:00:00&to=2025-10-31T23:59:59"

# Поиск по сервису
curl "http://localhost:8081/api/logs/service/monolith?from=2025-10-31T00:00:00&to=2025-10-31T23:59:59"
```

---

## ⏱️ План миграции

### Week 1: Подготовка
- [ ] Создать проект Logging Service
- [ ] Настроить MongoDB (dedicated)
- [ ] Настроить Kafka consumer
- [ ] Реализовать базовую логику

### Week 2: Интеграция
- [ ] Добавить Kafka producer в монолит
- [ ] Обновить LoggingAspect
- [ ] Протестировать на dev окружении

### Week 3: Тестирование и деплой
- [ ] Load testing
- [ ] Canary deployment (10% трафика)
- [ ] Мониторинг метрик
- [ ] Постепенное увеличение до 100%

---

## ✅ Критерии успеха

- ✅ Logging Service обрабатывает все события
- ✅ Latency монолита снизилась
- ✅ CPU монолита снизилось
- ✅ Нет потери логов
- ✅ Consumer lag < 100 сообщений
- ✅ Error rate < 0.1%

---

**Следующий шаг:** [10_LOGGING_SERVICE_IMPLEMENTATION.md](10_LOGGING_SERVICE_IMPLEMENTATION.md)
