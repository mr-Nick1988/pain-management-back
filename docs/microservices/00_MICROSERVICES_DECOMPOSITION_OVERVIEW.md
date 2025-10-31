# 🏗️ Pain Management System - Microservices Decomposition Overview

**Дата создания:** 31.10.2025  
**Версия:** 1.0  
**Статус:** Архитектурный план

---

## 📋 Содержание

1. [Введение](#введение)
2. [Текущее состояние монолита](#текущее-состояние-монолита)
3. [Предлагаемая микросервисная архитектура](#предлагаемая-микросервисная-архитектура)
4. [Стратегия декомпозиции](#стратегия-декомпозиции)
5. [Технологический стек](#технологический-стек)
6. [Порядок миграции](#порядок-миграции)
7. [Риски и митигация](#риски-и-митигация)

---

## 🎯 Введение

Данный документ описывает стратегию безопасной декомпозиции монолитного приложения **Pain Management System** в микросервисную архитектуру. Цель миграции:

- ✅ **Масштабируемость** - независимое масштабирование компонентов
- ✅ **Изоляция нагрузки** - разделение ресурсов между сервисами
- ✅ **Технологическая гибкость** - использование оптимальных технологий для каждого сервиса
- ✅ **Отказоустойчивость** - изоляция сбоев
- ✅ **Независимое развертывание** - CI/CD для каждого сервиса

---

## 🏢 Текущее состояние монолита

### Архитектура монолита

```
pain_helper_back (Monolith)
├── Core Business Modules
│   ├── admin/                    # Управление пользователями
│   ├── doctor/                   # Модуль врача
│   ├── nurse/                    # Модуль медсестры
│   ├── anesthesiologist/         # Модуль анестезиолога
│   └── treatment_protocol/       # Протоколы лечения
│
├── Infrastructure Modules (для декомпозиции)
│   ├── analytics/                # ⚠️ Аналитика + Логирование
│   ├── backup_restore/           # ⚠️ Бэкап и восстановление
│   ├── external_emr_integration_service/  # ⚠️ Интеграция с EMR
│   ├── pain_escalation_tracking/ # ⚠️ Отслеживание эскалаций
│   ├── performance_SLA_monitoring/ # ⚠️ Мониторинг производительности
│   ├── reporting/                # ⚠️ Отчетность
│   └── VAS_external_integration/ # ⚠️ Внешняя интеграция VAS
│
└── Shared Components
    ├── common/                   # Общие компоненты
    ├── config/                   # Конфигурация
    ├── enums/                    # Перечисления
    └── websocket/                # WebSocket
```

### Технологический стек монолита

- **Backend:** Spring Boot 3.5.5, Java 21
- **Databases:** H2 (реляционная), MongoDB (логи/аналитика)
- **Integration:** HAPI FHIR R4, Resilience4j
- **Communication:** REST API, WebSocket
- **Build:** Maven

### Проблемы монолита

| Проблема | Описание | Влияние |
|----------|----------|---------|
| **Единая база нагрузки** | Логирование и аналитика нагружают основную систему | 🔴 Высокое |
| **Масштабирование** | Невозможно масштабировать отдельные модули | 🔴 Высокое |
| **Связанность** | Тесная связь между модулями | 🟡 Среднее |
| **Развертывание** | Изменение одного модуля требует перезапуска всего | 🟡 Среднее |
| **Технологические ограничения** | Все модули используют одни технологии | 🟢 Низкое |

---

## 🎨 Предлагаемая микросервисная архитектура

### Карта микросервисов

```
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (Spring Cloud Gateway)            │
│                    + Service Discovery (Eureka)                  │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼────────┐   ┌────────▼────────┐   ┌───────▼────────┐
│  Core Business │   │  Infrastructure │   │  Integration   │
│   Services     │   │    Services     │   │    Services    │
└────────────────┘   └─────────────────┘   └────────────────┘
        │                     │                     │
        │                     │                     │
┌───────▼────────────────────▼─────────────────────▼────────┐
│                                                             │
│  1. Core Business Service (Monolith Core)                  │
│     - admin, doctor, nurse, anesthesiologist               │
│     - treatment_protocol                                   │
│     Database: H2 (PostgreSQL в будущем)                    │
│                                                             │
│  2. Logging & Audit Service ⭐ ПРИОРИТЕТ #1                │
│     - Централизованное логирование всех сервисов           │
│     - Audit trail                                          │
│     Database: MongoDB (dedicated)                          │
│     Message Broker: Apache Kafka                           │
│                                                             │
│  3. Analytics Service                                      │
│     - Статистика и аналитика                               │
│     - Агрегация данных                                     │
│     Database: MongoDB (dedicated)                          │
│     Depends on: Logging Service (Kafka consumer)           │
│                                                             │
│  4. EMR Integration Service                                │
│     - Интеграция с внешними EMR системами                  │
│     - FHIR R4 протокол                                     │
│     Database: MongoDB (кэш и синхронизация)                │
│                                                             │
│  5. Pain Escalation Service                                │
│     - Отслеживание эскалаций боли                          │
│     - Уведомления и алерты                                 │
│     Database: PostgreSQL                                   │
│                                                             │
│  6. Performance & SLA Monitoring Service                   │
│     - Мониторинг производительности                        │
│     - SLA метрики                                          │
│     Database: InfluxDB / TimescaleDB                       │
│                                                             │
│  7. Reporting Service                                      │
│     - Генерация отчетов (PDF, Excel)                       │
│     - Scheduled reports                                    │
│     Database: PostgreSQL (read replicas)                   │
│                                                             │
│  8. Backup & Restore Service                               │
│     - Автоматические бэкапы                                │
│     - Восстановление данных                                │
│     Storage: S3-compatible (MinIO)                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼────────┐   ┌────────▼────────┐   ┌───────▼────────┐
│  Apache Kafka  │   │  Redis Cache    │   │  Config Server │
│  (Event Bus)   │   │  (Distributed)  │   │  (Spring Cloud)│
└────────────────┘   └─────────────────┘   └────────────────┘
```

### Описание микросервисов

#### 🔵 1. Core Business Service (Монолит Core)
**Статус:** Остается монолитом на первом этапе

- **Ответственность:** Основная бизнес-логика
- **Модули:** admin, doctor, nurse, anesthesiologist, treatment_protocol
- **Database:** H2 → PostgreSQL (миграция)
- **Причина:** Тесно связанные бизнес-процессы, сложная доменная логика

#### ⭐ 2. Logging & Audit Service (ПРИОРИТЕТ #1)
**Статус:** Первый для выделения

- **Ответственность:** Централизованное логирование всех операций
- **Источник:** `analytics/aspect/LoggingAspect.java`, `analytics/entity/LogEntry.java`
- **Database:** MongoDB (dedicated instance)
- **Message Broker:** Apache Kafka
- **Преимущества:**
  - Изоляция нагрузки от основной системы
  - Независимое масштабирование
  - Не влияет на бизнес-логику при сбоях
  - Централизованный audit trail

#### 🟢 3. Analytics Service
**Статус:** Второй для выделения

- **Ответственность:** Аналитика, статистика, агрегация
- **Источник:** `analytics/` (без логирования)
- **Database:** MongoDB (dedicated)
- **Зависимости:** Kafka consumer (события от Logging Service)
- **API:** REST для получения аналитики

#### 🟡 4. EMR Integration Service
**Статус:** Третий для выделения

- **Ответственность:** Интеграция с внешними EMR системами
- **Источник:** `external_emr_integration_service/`
- **Database:** MongoDB (кэш FHIR ресурсов)
- **Технологии:** HAPI FHIR R4, Resilience4j
- **Паттерны:** Circuit Breaker, Retry, Fallback

#### 🟠 5. Pain Escalation Service
**Статус:** Четвертый для выделения

- **Ответственность:** Отслеживание эскалаций боли, алерты
- **Источник:** `pain_escalation_tracking/`
- **Database:** PostgreSQL
- **Интеграция:** WebSocket для real-time уведомлений
- **Scheduler:** Периодический анализ трендов

#### 🔴 6. Performance & SLA Monitoring Service
**Статус:** Пятый для выделения

- **Ответственность:** Мониторинг производительности, SLA метрики
- **Источник:** `performance_SLA_monitoring/`
- **Database:** InfluxDB или TimescaleDB (time-series)
- **Интеграция:** Prometheus, Grafana

#### 🟣 7. Reporting Service
**Статус:** Шестой для выделения

- **Ответственность:** Генерация отчетов
- **Источник:** `reporting/`
- **Database:** PostgreSQL (read replicas)
- **Технологии:** Apache POI, PDFBox
- **Паттерны:** CQRS (read-only)

#### ⚫ 8. Backup & Restore Service
**Статус:** Седьмой для выделения

- **Ответственность:** Автоматические бэкапы и восстановление
- **Источник:** `backup_restore/`
- **Storage:** MinIO (S3-compatible)
- **Scheduler:** Cron jobs для автоматических бэкапов

---

## 🎯 Стратегия декомпозиции

### Подход: Strangler Fig Pattern

Используем паттерн **Strangler Fig** - постепенное "обвитие" монолита новыми сервисами:

1. **Создание нового микросервиса** параллельно монолиту
2. **Перенаправление трафика** через API Gateway
3. **Постепенное переключение** функциональности
4. **Удаление старого кода** из монолита

### Принципы декомпозиции

| Принцип | Описание |
|---------|----------|
| **Bounded Context** | Каждый сервис - отдельный bounded context из DDD |
| **Database per Service** | Каждый сервис имеет свою БД |
| **API First** | Сначала проектируем API, затем реализацию |
| **Event-Driven** | Асинхронная коммуникация через Kafka |
| **Backward Compatibility** | Поддержка старых API во время миграции |

---

## 🛠️ Технологический стек

### Микросервисы

| Компонент | Технология | Версия | Назначение |
|-----------|-----------|--------|------------|
| **Framework** | Spring Boot | 3.5.5 | Основной фреймворк |
| **Language** | Java | 21 | Язык программирования |
| **Build Tool** | Maven | 3.9+ | Сборка проектов |

### Service Mesh & Communication

| Компонент | Технология | Версия | Назначение |
|-----------|-----------|--------|------------|
| **API Gateway** | Spring Cloud Gateway | 4.1.x | Единая точка входа |
| **Service Discovery** | Netflix Eureka | 4.1.x | Обнаружение сервисов |
| **Config Server** | Spring Cloud Config | 4.1.x | Централизованная конфигурация |
| **Message Broker** | Apache Kafka | 3.6+ | Event streaming |
| **Circuit Breaker** | Resilience4j | 2.1.0 | Отказоустойчивость |

### Databases

| Сервис | Database | Назначение |
|--------|----------|------------|
| **Core Business** | PostgreSQL | Основная БД (миграция с H2) |
| **Logging & Audit** | MongoDB | Логи и audit trail |
| **Analytics** | MongoDB | Аналитические данные |
| **EMR Integration** | MongoDB | Кэш FHIR ресурсов |
| **Pain Escalation** | PostgreSQL | Эскалации |
| **Performance Monitoring** | InfluxDB/TimescaleDB | Time-series метрики |
| **Reporting** | PostgreSQL (read replica) | Отчеты |

### Infrastructure

| Компонент | Технология | Назначение |
|-----------|-----------|------------|
| **Containerization** | Docker | Контейнеризация |
| **Orchestration** | Docker Compose / Kubernetes | Оркестрация |
| **Cache** | Redis | Распределенный кэш |
| **Object Storage** | MinIO | S3-compatible хранилище |
| **Monitoring** | Prometheus + Grafana | Мониторинг |
| **Logging** | ELK Stack (optional) | Визуализация логов |
| **Tracing** | Zipkin / Jaeger | Distributed tracing |

---

## 📅 Порядок миграции

### Phase 0: Подготовка (2 недели)

- [ ] Настройка инфраструктуры (Docker, Kafka, Eureka)
- [ ] Создание API Gateway
- [ ] Настройка Config Server
- [ ] Подготовка CI/CD пайплайнов

### Phase 1: Logging & Audit Service ⭐ (2-3 недели)

**Приоритет:** КРИТИЧЕСКИЙ

**Причина:** Максимальная изоляция нагрузки, минимальные зависимости

**Шаги:**
1. Создание нового микросервиса
2. Настройка Kafka topics
3. Миграция LoggingAspect на Kafka producer
4. Тестирование асинхронного логирования
5. Переключение трафика

**Результат:** Логирование не нагружает основную систему

### Phase 2: Analytics Service (2 недели)

**Зависимости:** Logging Service

**Шаги:**
1. Создание микросервиса
2. Kafka consumer для событий
3. Миграция аналитических API
4. Тестирование
5. Переключение трафика

### Phase 3: EMR Integration Service (3 недели)

**Сложность:** Высокая (внешние интеграции)

**Шаги:**
1. Создание микросервиса
2. Миграция FHIR клиента
3. Настройка Circuit Breaker
4. Тестирование с внешними EMR
5. Переключение трафика

### Phase 4: Pain Escalation Service (2 недели)

**Шаги:**
1. Создание микросервиса
2. Миграция логики эскалаций
3. WebSocket интеграция
4. Тестирование алертов
5. Переключение трафика

### Phase 5: Performance & SLA Monitoring (2 недели)

**Шаги:**
1. Создание микросервиса
2. Настройка InfluxDB/TimescaleDB
3. Миграция метрик
4. Интеграция с Prometheus
5. Переключение трафика

### Phase 6: Reporting Service (2 недели)

**Шаги:**
1. Создание микросервиса
2. Настройка read replicas
3. Миграция генерации отчетов
4. Тестирование
5. Переключение трафика

### Phase 7: Backup & Restore Service (2 недели)

**Шаги:**
1. Создание микросервиса
2. Настройка MinIO
3. Миграция логики бэкапов
4. Тестирование восстановления
5. Переключение трафика

### Phase 8: Core Business Migration (4-6 недель)

**Опционально:** Миграция H2 → PostgreSQL

---

## ⚠️ Риски и митигация

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| **Потеря данных при миграции** | Средняя | Критическое | Полные бэкапы, тестирование на копиях |
| **Проблемы производительности** | Средняя | Высокое | Load testing, постепенное переключение |
| **Несовместимость API** | Низкая | Среднее | Версионирование API, backward compatibility |
| **Сложность отладки** | Высокая | Среднее | Distributed tracing (Zipkin/Jaeger) |
| **Увеличение latency** | Средняя | Среднее | Кэширование (Redis), оптимизация запросов |
| **Каскадные сбои** | Средняя | Высокое | Circuit Breaker, Retry, Timeout |

---

## 📚 Дополнительные документы

1. **[01_INFRASTRUCTURE_SETUP.md](./01_INFRASTRUCTURE_SETUP.md)** - Настройка инфраструктуры
2. **[02_LOGGING_SERVICE_MIGRATION.md](./02_LOGGING_SERVICE_MIGRATION.md)** - Миграция Logging Service
3. **[03_ANALYTICS_SERVICE_MIGRATION.md](./03_ANALYTICS_SERVICE_MIGRATION.md)** - Миграция Analytics Service
4. **[04_EMR_INTEGRATION_SERVICE_MIGRATION.md](./04_EMR_INTEGRATION_SERVICE_MIGRATION.md)** - Миграция EMR Integration
5. **[05_PAIN_ESCALATION_SERVICE_MIGRATION.md](./05_PAIN_ESCALATION_SERVICE_MIGRATION.md)** - Миграция Pain Escalation
6. **[06_PERFORMANCE_MONITORING_SERVICE_MIGRATION.md](./06_PERFORMANCE_MONITORING_SERVICE_MIGRATION.md)** - Миграция Performance Monitoring
7. **[07_REPORTING_SERVICE_MIGRATION.md](./07_REPORTING_SERVICE_MIGRATION.md)** - Миграция Reporting Service
8. **[08_BACKUP_RESTORE_SERVICE_MIGRATION.md](./08_BACKUP_RESTORE_SERVICE_MIGRATION.md)** - Миграция Backup & Restore
9. **[09_API_GATEWAY_CONFIGURATION.md](./09_API_GATEWAY_CONFIGURATION.md)** - Настройка API Gateway
10. **[10_KAFKA_EVENT_DRIVEN_ARCHITECTURE.md](./10_KAFKA_EVENT_DRIVEN_ARCHITECTURE.md)** - Event-Driven архитектура
11. **[11_DATABASE_MIGRATION_STRATEGY.md](./11_DATABASE_MIGRATION_STRATEGY.md)** - Стратегия миграции БД
12. **[12_TESTING_STRATEGY.md](./12_TESTING_STRATEGY.md)** - Стратегия тестирования
13. **[13_DEPLOYMENT_STRATEGY.md](./13_DEPLOYMENT_STRATEGY.md)** - Стратегия развертывания
14. **[14_MONITORING_OBSERVABILITY.md](./14_MONITORING_OBSERVABILITY.md)** - Мониторинг и observability

---

## ✅ Критерии успеха

- ✅ Все микросервисы работают независимо
- ✅ Логирование не нагружает основную систему
- ✅ Latency не увеличилась более чем на 10%
- ✅ Все тесты проходят успешно
- ✅ Нет потери данных
- ✅ Backward compatibility сохранена
- ✅ Мониторинг и алертинг настроены

---

**Следующий шаг:** [01_INFRASTRUCTURE_SETUP.md](./01_INFRASTRUCTURE_SETUP.md)
