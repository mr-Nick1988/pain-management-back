# 02 - Стратегия декомпозиции

**Предыдущий:** [01_OVERVIEW.md](01_OVERVIEW.md)  
**Следующий:** [03_INFRASTRUCTURE_OVERVIEW.md](03_INFRASTRUCTURE_OVERVIEW.md)

---

## 🎯 Подход: Strangler Fig Pattern

Используем паттерн **Strangler Fig** - постепенное "обвитие" монолита новыми сервисами.

### Принцип работы

```
Шаг 1: Монолит работает как обычно
┌─────────────────────┐
│      Monolith       │
│  All functionality  │
└─────────────────────┘

Шаг 2: Создаем новый микросервис параллельно
┌─────────────────────┐     ┌──────────────┐
│      Monolith       │     │ Microservice │
│  All functionality  │     │  New logic   │
└─────────────────────┘     └──────────────┘

Шаг 3: API Gateway перенаправляет часть трафика
                ┌─────────────┐
                │ API Gateway │
                └──────┬──────┘
                   ┌───┴───┐
         ┌─────────▼─┐   ┌─▼────────────┐
         │ Monolith  │   │ Microservice │
         │ (90%)     │   │ (10%)        │
         └───────────┘   └──────────────┘

Шаг 4: Постепенно переключаем весь трафик
                ┌─────────────┐
                │ API Gateway │
                └──────┬──────┘
                       │
                   ┌───▼──────────┐
                   │ Microservice │
                   │ (100%)       │
                   └──────────────┘

Шаг 5: Удаляем старый код из монолита
┌─────────────────────┐     ┌──────────────┐
│      Monolith       │     │ Microservice │
│  Without old logic  │     │  All logic   │
└─────────────────────┘     └──────────────┘
```

---

## 📋 Принципы декомпозиции

### 1. Bounded Context (DDD)

Каждый микросервис = отдельный bounded context

```
Logging Service → Контекст логирования
Analytics Service → Контекст аналитики
EMR Integration → Контекст интеграции с EMR
```

### 2. Database per Service

Каждый сервис имеет свою БД

```
Logging Service → MongoDB (dedicated)
Analytics Service → MongoDB (dedicated)
EMR Integration → MongoDB (cache)
Pain Escalation → PostgreSQL
```

### 3. API First

Сначала проектируем API, затем реализацию

```
1. Определить API контракт (OpenAPI/Swagger)
2. Согласовать с командой
3. Реализовать сервис
4. Протестировать
5. Задеплоить
```

### 4. Event-Driven Communication

Асинхронная коммуникация через Kafka

```
Monolith → Kafka → Logging Service
Logging Service → Kafka → Analytics Service
```

### 5. Backward Compatibility

Поддержка старых API во время миграции

```
API Gateway:
  /api/v1/logs → Monolith (deprecated)
  /api/v2/logs → Logging Service (new)
```

---

## 🔄 Порядок миграции

### Критерии выбора порядка

1. **Изоляция нагрузки** - насколько сервис нагружает систему
2. **Зависимости** - количество зависимостей от других модулей
3. **Сложность** - сложность миграции
4. **Бизнес-ценность** - влияние на бизнес

### Приоритизация

| Сервис | Нагрузка | Зависимости | Сложность | Приоритет |
|--------|----------|-------------|-----------|-----------|
| **Logging** | 🔴 Высокая | 🟢 Низкие | 🟢 Низкая | ⭐ #1 |
| **Analytics** | 🟡 Средняя | 🟡 Средние | 🟡 Средняя | #2 |
| **EMR Integration** | 🟡 Средняя | 🟢 Низкие | 🔴 Высокая | #3 |
| **Pain Escalation** | 🟢 Низкая | 🟡 Средние | 🟡 Средняя | #4 |
| **Performance** | 🟢 Низкая | 🟢 Низкие | 🟢 Низкая | #5 |
| **Reporting** | 🟡 Средняя | 🟡 Средние | 🟡 Средняя | #6 |
| **Backup** | 🟢 Низкая | 🟢 Низкие | 🟡 Средняя | #7 |

---

## 🛡️ Стратегия минимизации рисков

### 1. Canary Deployment

Постепенное переключение трафика

```
Week 1: 10% трафика → новый сервис
Week 2: 25% трафика → новый сервис
Week 3: 50% трафика → новый сервис
Week 4: 100% трафика → новый сервис
```

### 2. Feature Flags

Возможность быстрого отката

```java
@Configuration
public class FeatureFlags {
    @Value("${feature.logging-service.enabled:false}")
    private boolean loggingServiceEnabled;
    
    public boolean useNewLoggingService() {
        return loggingServiceEnabled;
    }
}
```

### 3. Полные бэкапы

Перед каждой миграцией

```bash
# Бэкап H2
./backup-h2.sh

# Бэкап MongoDB
./backup-mongodb.sh

# Бэкап конфигурации
./backup-config.sh
```

### 4. Rollback Plan

План отката для каждого сервиса

```
1. Переключить API Gateway обратно на монолит
2. Отключить feature flag
3. Остановить новый сервис
4. Восстановить данные из бэкапа (если нужно)
5. Провести анализ причин
```

---

## 📊 Мониторинг миграции

### Ключевые метрики

| Метрика | Целевое значение | Критическое значение |
|---------|------------------|---------------------|
| **Latency (p95)** | < 200ms | > 500ms |
| **Error Rate** | < 0.1% | > 1% |
| **Throughput** | Без изменений | -20% |
| **CPU Usage** | < 70% | > 90% |
| **Memory Usage** | < 80% | > 95% |

### Алерты

```yaml
alerts:
  - name: HighErrorRate
    condition: error_rate > 1%
    action: Rollback
    
  - name: HighLatency
    condition: p95_latency > 500ms
    action: Investigate
    
  - name: LowThroughput
    condition: throughput < baseline * 0.8
    action: Rollback
```

---

## 🔧 Технические решения

### Circuit Breaker

Защита от каскадных сбоев

```java
@CircuitBreaker(name = "loggingService", fallbackMethod = "fallbackLog")
public void sendLog(LogEvent event) {
    kafkaTemplate.send("logging-events", event);
}

public void fallbackLog(LogEvent event, Exception e) {
    // Сохранить локально или пропустить
    log.warn("Failed to send log to Kafka: {}", e.getMessage());
}
```

### Retry Pattern

Повторные попытки при временных сбоях

```java
@Retry(name = "loggingService", fallbackMethod = "fallbackLog")
public void sendLog(LogEvent event) {
    kafkaTemplate.send("logging-events", event);
}
```

### Timeout

Ограничение времени ожидания

```java
@TimeLimiter(name = "loggingService")
public CompletableFuture<Void> sendLogAsync(LogEvent event) {
    return CompletableFuture.runAsync(() -> 
        kafkaTemplate.send("logging-events", event)
    );
}
```

---

## ✅ Чеклист перед миграцией каждого сервиса

- [ ] API контракт определен и согласован
- [ ] Новый сервис реализован и протестирован
- [ ] Unit тесты написаны и проходят
- [ ] Integration тесты написаны и проходят
- [ ] Load тесты проведены
- [ ] Мониторинг и алерты настроены
- [ ] Полный бэкап создан
- [ ] Rollback план подготовлен
- [ ] Документация обновлена
- [ ] Команда проинформирована

---

**Следующий шаг:** [03_INFRASTRUCTURE_OVERVIEW.md](03_INFRASTRUCTURE_OVERVIEW.md)
