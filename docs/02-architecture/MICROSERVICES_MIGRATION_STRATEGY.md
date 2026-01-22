# 📋 СТРАТЕГИЯ РАЗДЕЛЕНИЯ МОНОЛИТА НА МИКРОСЕРВИСЫ

**Дата создания:** 08.01.2026  
**Версия:** 1.0  
**Статус:** Утверждено к реализации

---

## 🎯 Исполнительное резюме

После глубокого анализа проекта выявлена **критическая проблема архитектурного смешивания**: в монолите находятся как минимум **18 модулей**, из которых лишь **6 модулей относятся к core-бизнес-логике** (workflow + treatment protocol). Остальные 12 модулей — это инфраструктурные, интеграционные и аналитические компоненты, которые должны быть микросервисами.

**Ключевая проблема**: монолит перегружен ответственностями, которые нарушают принципы Single Responsibility и создают tight coupling между бизнес-логикой и инфраструктурой.

---

## 📊 ТЕКУЩЕЕ СОСТОЯНИЕ МОНОЛИТА

### Модули в монолите (18 компонентов):

#### 🟢 **CORE (должны остаться в монолите):**
1. **`admin/`** — управление пользователями (Admin role)
2. **`doctor/`** — работа доктора с пациентами и рекомендациями
3. **`nurse/`** — работа медсестры с пациентами, VAS, EMR
4. **`anesthesiologist/`** — работа анестезиолога с эскалациями и протоколами
5. **`treatment_protocol/`** — алгоритм генерации рекомендаций (75k ICD кодов, фильтры)
6. **`common/`** — общие сущности (Patient, VAS, EMR, Recommendation, Persons)

#### 🔴 **ДОЛЖНЫ БЫТЬ МИКРОСЕРВИСАМИ (12 модулей):**
7. **`external_emr_integration_service/`** — интеграция с FHIR серверами
8. **`emr_recalculation/`** — обработка изменений EMR через Kafka
9. **`analytics/`** — сбор аналитических событий в MongoDB
10. **`reporting/`** — генерация отчетов (Excel, PDF, email)
11. **`performance_SLA_monitoring/`** — мониторинг производительности
12. **`pain_escalation_tracking/`** — отслеживание эскалаций боли и дозировок
13. **`VAS_external_integration/`** — внешняя интеграция VAS данных
14. **`websocket/`** — WebSocket уведомления
15. **`common/persons/`** — аутентификация/авторизация (дублирует authentication-service)
16. **Kafka consumer** — обработка emr.changes
17. **MongoDB** — аналитика и логи
18. **Scheduled tasks** — FHIR sync, отчеты

---

## ⚠️ ВЫЯВЛЕННЫЕ АНТИ-ПАТТЕРНЫ

### 1. **Дублирование функциональности**
- В монолите есть модуль аутентификации (`common/persons/`), но уже существует **отдельный authentication-service**
- В монолите есть модуль reporting, но уже существует **отдельный reporting-service**
- В монолите есть аналитика в MongoDB, но уже существует **отдельный logging-service**

### 2. **Нарушение Bounded Context**
- Treatment Protocol (core бизнес-логика) смешан с External EMR Integration (инфраструктура)
- Workflow ролей смешан с Performance Monitoring (observability)
- Domain Events (аналитика) обрабатываются внутри монолита вместо event-driven архитектуры

### 3. **Tight Coupling с инфраструктурой**
- Монолит напрямую работает с FHIR клиентом (HAPI)
- Монолит напрямую пишет в MongoDB для аналитики
- Монолит содержит Kafka consumer для EMR событий
- Монолит содержит WebSocket сервер для уведомлений

### 4. **Множественные ответственности**
Монолит выполняет:
- Бизнес-логику workflow
- Интеграцию с внешними системами
- Мониторинг производительности
- Генерацию отчетов
- Обработку событий
- Real-time уведомления

---

## 🎯 ЦЕЛЕВАЯ АРХИТЕКТУРА

### **Монолит (Core Domain Service)** — только workflow + treatment protocol

#### Что остается:
```
pain_management_core/
├── admin/           # Управление пользователями
├── doctor/          # Работа доктора
├── nurse/           # Работа медсестры
├── anesthesiologist/# Работа анестезиолога
├── treatment_protocol/ # Алгоритм рекомендаций
└── common/
    └── patients/    # Общие сущности: Patient, VAS, EMR, Recommendation
```

#### Ответственности монолита:
- ✅ CRUD операции с пациентами
- ✅ Регистрация VAS (жалобы на боль)
- ✅ Управление EMR данными
- ✅ Генерация рекомендаций через Treatment Protocol
- ✅ Workflow одобрения/отклонения рекомендаций
- ✅ Эскалации к анестезиологу
- ✅ Управление пользователями (Admin)

#### Что удаляется из монолита:
- ❌ `external_emr_integration_service/`
- ❌ `emr_recalculation/`
- ❌ `analytics/`
- ❌ `reporting/`
- ❌ `performance_SLA_monitoring/`
- ❌ `pain_escalation_tracking/`
- ❌ `VAS_external_integration/`
- ❌ `websocket/`
- ❌ `common/persons/` (аутентификация)
- ❌ Kafka consumer/producer код
- ❌ MongoDB зависимости
- ❌ HAPI FHIR клиент
- ❌ Scheduled tasks (кроме treatment protocol)
- ❌ WebSocket dependencies
- ❌ Mail dependencies
- ❌ PDF generation
- ❌ Resilience4j

---

## 🏗️ МИКРОСЕРВИСЫ (целевая архитектура)

### ✅ **Уже существуют (готовы к использованию):**

#### 1. **Authentication Service** (порт 8082)
- **Статус**: Готов, документирован
- **Функции**: JWT токены, login/logout, регистрация, валидация
- **Что делать**: Удалить `common/persons/` из монолита, переключить на этот сервис

#### 2. **Reporting Service** (порт 8091)
- **Статус**: Готов, документирован
- **Функции**: Daily KPI, Excel/PDF экспорт, email отчеты, Kafka consumer
- **Что делать**: Удалить `reporting/` из монолита, интегрировать через Kafka/REST

#### 3. **Logging Service** (порт 8081/8083)
- **Статус**: Готов, документирован
- **Функции**: Централизованный прием логов через Kafka в MongoDB
- **Что делать**: Удалить `analytics/` из монолита, публиковать события в Kafka

#### 4. **Backup & Restore Service** (порт 8085)
- **Статус**: Готов, документирован
- **Функции**: Бэкап/рестор Postgres и MongoDB
- **Что делать**: Использовать как есть для ops задач

### 🆕 **Нужно создать новые микросервисы:**

#### 5. **EMR Integration Service** (порт 8086)
- **Текущий статус**: Каркас создан (26.12.2025), но не завершен
- **Переносим из монолита**:
  - `external_emr_integration_service/` (FHIR клиент, HAPI, ICD loader)
  - `emr_recalculation/` (обработка изменений EMR)
- **Ответственности**:
  - Интеграция с FHIR серверами (HAPI FHIR)
  - Синхронизация EMR данных по расписанию
  - Детекция критических изменений EMR
  - Публикация событий в Kafka: `emr.changes`, `emr.critical.alerts`
  - Загрузка ICD справочника (75k записей)
- **БД**: Postgres (EMR снапшоты, история изменений)
- **Kafka Producer**: `emr.changes`, `emr.upserted`

#### 6. **Notification Service** (порт 8087)
- **Переносим из монолита**:
  - `websocket/` (WebSocket сервер)
  - `external_emr_integration_service/service/EmailNotificationService`
  - `pain_escalation_tracking/service/PainEscalationNotificationService`
- **Ответственности**:
  - WebSocket уведомления для UI
  - Email уведомления
  - Push-нотификации (будущее)
  - Управление подписками на события
- **Kafka Consumer**: `notification.requests`
- **Технологии**: Spring WebSocket, STOMP, Spring Mail

#### 7. **Analytics & Monitoring Service** (порт 8088)
- **Переносим из монолита**:
  - `analytics/` (события, domain events, AOP аспекты)
  - `performance_SLA_monitoring/` (мониторинг производительности)
- **Ответственности**:
  - Сбор domain events (UserLoginEvent, RecommendationGeneratedEvent и т.д.)
  - Мониторинг SLA и производительности
  - Метрики бизнес-процессов
  - Dashboards для аналитики
- **БД**: MongoDB (аналитические события)
- **Kafka Consumer**: `analytics-events`, `performance-metrics`

#### 8. **Pain Escalation Tracking Service** (порт 8089)
- **Переносим из монолита**:
  - `pain_escalation_tracking/` (DoseAdministration, PainEscalation)
- **Ответственности**:
  - Отслеживание введенных доз препаратов
  - Мониторинг эскалаций боли
  - Алерты при превышении дозировок
  - История администрирования препаратов
- **БД**: Postgres (дозы, эскалации)
- **Kafka Producer**: `dose.administered`, `pain.escalated`

#### 9. **External VAS Integration Service** (порт 8090)
- **Переносим из монолита**:
  - `VAS_external_integration/` (внешняя интеграция VAS данных)
- **Ответственности**:
  - API для внешних систем для отправки VAS данных
  - Парсинг различных форматов VAS (JSON, XML, HL7)
  - Валидация и нормализация VAS данных
  - API Key управление для внешних систем
- **БД**: Postgres (API keys, история интеграций)
- **Kafka Producer**: `vas.external.received`

---

## 📋 ДЕТАЛЬНЫЙ ПЛАН МИГРАЦИИ

### **ЭТАП 0: Подготовка (1-2 дня)**

#### Действия:
1. ✅ Создать документацию со стратегией
2. ✅ Заморозить новые фичи в монолите
3. ✅ Создать git ветку `refactor/microservices-split`
4. ✅ Сделать полный бэкап БД и кода

#### Результат:
- Понимание стратегии
- Защита от потери данных
- Изолированная ветка для работы

---

### **ЭТАП 1: Чистка монолита от дублирования (3-5 дней)**

#### 1.1. Удалить аутентификацию из монолита
**Что удалять:**
- `common/persons/` (кроме базовой сущности Person для аудита)
- Методы login/logout в контроллерах
- Password hashing логику

**Что делать:**
- Интегрировать с `authentication-service` (8082)
- Добавить JWT validation middleware в монолит
- Использовать `personId` из JWT для аудита

**Зависимости:**
- ✅ Authentication-service уже готов
- Нужно: REST клиент для `/api/auth/validate`, `/api/auth/me`

#### 1.2. Удалить reporting из монолита
**Что удалять:**
- Весь пакет `reporting/`
- Excel/PDF generation код
- Email reporting код

**Что делать:**
- Публиковать события в Kafka `reporting-commands`
- Для UI: проксировать запросы к `reporting-service` (8091)

**Зависимости:**
- ✅ Reporting-service уже готов
- Нужно: Kafka producer, REST proxy

#### 1.3. Удалить analytics из монолита
**Что удалять:**
- Пакет `analytics/`
- MongoDB зависимости (если используется только для аналитики)
- Domain event listeners

**Что делать:**
- Публиковать domain events в Kafka `analytics-events`
- Создать DTOs для событий

**Зависимости:**
- ✅ Logging-service уже готов для технических логов
- Нужно: создать Analytics-service для бизнес-событий

#### Результат ЭТАПА 1:
- Монолит освобожден от инфраструктурных модулей
- Налажена коммуникация с существующими микросервисами
- Размер монолита уменьшен на ~40%

---

### **ЭТАП 2: Создание новых микросервисов (по одному, 5-7 дней каждый)**

#### Приоритет 1: **EMR Integration Service**
**Почему первый:**
- Критичен для бизнес-логики
- Уже есть каркас
- Четкая граница ответственности

**План:**
1. Скопировать `external_emr_integration_service/` + `emr_recalculation/` в новый проект
2. Создать `pom.xml` с зависимостями: HAPI FHIR, Kafka, Postgres
3. Реализовать FHIR sync scheduler
4. Реализовать Kafka producer для `emr.changes`
5. Создать REST API для ручной синхронизации
6. Создать Dockerfile
7. Тестирование интеграции с монолитом

**Интерфейс с монолитом:**
- Kafka: монолит слушает `emr.changes` (уже есть)
- REST (опционально): `/api/emr/sync/{patientId}`

#### Приоритет 2: **Notification Service**
**Почему второй:**
- Нужен для user experience
- Простой в реализации
- Независим от других микросервисов

**План:**
1. Скопировать `websocket/` в новый проект
2. Добавить email notification функционал
3. Создать Kafka consumer для `notification.requests`
4. Создать WebSocket endpoints
5. Dockerfile

**Интерфейс с монолитом:**
- Kafka: монолит публикует `notification.requests`
- WebSocket: фронтенд подключается напрямую к Notification Service

#### Приоритет 3: **Analytics & Monitoring Service**
**План:**
1. Скопировать `analytics/` + `performance_SLA_monitoring/`
2. MongoDB зависимости
3. Kafka consumer для `analytics-events`, `performance-metrics`
4. REST API для дашбордов
5. Dockerfile

#### Приоритет 4: **Pain Escalation Tracking Service**
#### Приоритет 5: **External VAS Integration Service**

---

### **ЭТАП 3: Финальная очистка монолита (2-3 дня)**

**Удалить зависимости из `pom.xml`:**
```xml
<!-- УДАЛИТЬ: -->
- spring-boot-starter-data-mongodb (если используется только для аналитики)
- hapi-fhir-* (FHIR клиент)
- spring-boot-starter-websocket
- spring-boot-starter-mail
- apache.pdfbox (PDF generation)
- resilience4j (если не используется в core)
- javafaker (моковые данные)
```

**Оставить только:**
```xml
<!-- ОСТАВИТЬ: -->
- spring-boot-starter-data-jpa (Postgres)
- spring-boot-starter-web (REST API)
- spring-boot-starter-validation
- spring-boot-starter-aop (для аудита)
- postgresql (JDBC)
- lombok
- modelmapper
- apache-poi (для treatment protocol Excel)
- liquibase-core (миграции БД)
- spring-kafka (event publishing)
```

**Итоговая структура монолита:**
```
src/main/java/pain_management_core/
├── PainManagementCoreApplication.java
├── admin/
│   ├── controller/AdminController
│   ├── service/AdminService
│   └── dto/
├── doctor/
│   ├── controller/DoctorController
│   ├── service/DoctorService
│   └── dto/
├── nurse/
│   ├── controller/NurseController
│   ├── service/NurseService
│   └── dto/
├── anesthesiologist/
│   ├── controller/AnesthesiologistController
│   ├── service/AnesthesiologistService
│   └── dto/
├── treatment_protocol/
│   ├── service/TreatmentProtocolService (алгоритм)
│   ├── entity/TreatmentProtocol
│   ├── repository/TreatmentProtocolRepository
│   └── excel_loader/ (загрузка таблицы)
├── common/
│   ├── patients/
│   │   ├── entity/Patient, VAS, EMR, Recommendation
│   │   └── repository/
│   ├── audit/ (аудит действий)
│   └── dto/ (общие DTO)
├── config/
│   ├── JwtValidationConfig (для authentication-service)
│   ├── KafkaProducerConfig (event publishing)
│   └── ModelMapperConfig
└── enums/ (Roles, Statuses, Priorities)
```

---

## 🔄 КОММУНИКАЦИЯ МЕЖДУ СЕРВИСАМИ

### **Синхронная коммуникация (REST):**

```
Frontend → Authentication Service (login/validate)
Frontend → Monolith (business operations)
Frontend → Notification Service (WebSocket)
Frontend → Reporting Service (reports/exports)

Monolith → Authentication Service (validate JWT)
Monolith → EMR Integration Service (manual sync - optional)
```

### **Асинхронная коммуникация (Kafka):**

```
Monolith (Producer):
  → analytics-events (domain events)
  → reporting-commands (generate reports)
  → notification.requests (send notifications)
  
Monolith (Consumer):
  ← emr.changes (EMR updates)
  
EMR Integration Service (Producer):
  → emr.changes
  → emr.critical.alerts
  
Pain Escalation Service (Producer):
  → dose.administered
  → pain.escalated
  
External VAS Service (Producer):
  → vas.external.received
```

### **Event Schema Examples:**

```json
// analytics-events
{
  "eventType": "RECOMMENDATION_GENERATED",
  "timestamp": "2025-01-08T16:00:00Z",
  "patientId": "MRN-123",
  "userId": "DOC-456",
  "metadata": {...}
}

// notification.requests
{
  "type": "EMAIL",
  "recipient": "doctor@hospital.com",
  "subject": "Critical Pain Escalation",
  "body": "Patient MRN-123 requires attention"
}

// emr.changes
{
  "mrn": "MRN-123",
  "changes": [
    {"field": "gfr", "oldValue": "90", "newValue": "45"}
  ],
  "severity": "CRITICAL",
  "timestamp": "2025-01-08T16:00:00Z"
}
```

---

## 📦 СТРУКТУРА ПРОЕКТА (после миграции)

```
C:\backend_projects\
├── pain_management_core/           # Монолит (core domain)
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── pom.xml
│   └── Dockerfile
│
├── microservices/
│   ├── authentication-service/     ✅ Готов
│   ├── reporting-service/          ✅ Готов
│   ├── logging-service/            ✅ Готов
│   ├── backup-restore-service/     ✅ Готов
│   ├── emr-integration-service/    🆕 Создать
│   ├── notification-service/       🆕 Создать
│   ├── analytics-monitoring-service/ 🆕 Создать
│   ├── pain-escalation-service/    🆕 Создать
│   └── external-vas-service/       🆕 Создать
│
└── docs/
    ├── architecture/
    │   └── MICROSERVICES_MIGRATION_STRATEGY.md
    └── api/
        └── event-schemas/
```

---

## ⚡ ПРЕИМУЩЕСТВА НОВОЙ АРХИТЕКТУРЫ

### 1. **Разделение ответственностей**
- ✅ Монолит отвечает ТОЛЬКО за бизнес-логику workflow
- ✅ Каждый микросервис имеет единую ответственность
- ✅ Чистая архитектура: domain отделен от инфраструктуры

### 2. **Независимое развертывание**
- ✅ Можно обновить Reporting без влияния на монолит
- ✅ Можно масштабировать EMR Integration отдельно
- ✅ Проще откатывать изменения

### 3. **Технологическая свобода**
- ✅ Notification Service может использовать Node.js для WebSocket
- ✅ Analytics Service может использовать специализированные БД
- ✅ Каждый сервис выбирает оптимальный стек

### 4. **Упрощение монолита**
- ✅ Размер кодовой базы уменьшится на ~60%
- ✅ Время сборки сократится
- ✅ Проще тестировать и поддерживать
- ✅ Новые разработчики быстрее вникают

### 5. **Fault Isolation**
- ✅ Падение Notification Service не убьет весь монолит
- ✅ Проблемы с FHIR не влияют на core workflow
- ✅ Circuit breakers на границах микросервисов

---

## 🚨 РИСКИ И МИТИГАЦИЯ

### Риск 1: **Распределенные транзакции**
**Проблема:** Один бизнес-процесс может затрагивать несколько микросервисов

**Решение:**
- Использовать Saga Pattern (choreography-based)
- Event Sourcing для критичных операций
- Eventual Consistency вместо ACID
- Компенсирующие транзакции

### Риск 2: **Network latency**
**Проблема:** REST вызовы между сервисами добавляют задержку

**Решение:**
- Минимизировать синхронные вызовы
- Использовать Kafka для асинхронной коммуникации
- Кэширование на уровне API Gateway
- Response caching в монолите

### Риск 3: **Дублирование данных**
**Проблема:** Patient, VAS, EMR могут быть в нескольких БД

**Решение:**
- Монолит — single source of truth для Patient/VAS/EMR
- Микросервисы хранят только ссылки (MRN, patientId)
- Event-driven репликация при необходимости
- CQRS: монолит = write model, микросервисы = read models

### Риск 4: **Сложность отладки**
**Проблема:** Трассировка запросов через несколько сервисов

**Решение:**
- Distributed tracing (Spring Cloud Sleuth + Zipkin)
- Correlation ID в каждом запросе
- Централизованное логирование (Logging Service)
- ELK stack для поиска по логам

### Риск 5: **Миграция существующих данных**
**Проблема:** Аналитика уже в MongoDB монолита

**Решение:**
- Написать миграционный скрипт MongoDB → Analytics Service
- Dual-write период (монолит + Analytics Service)
- Верификация данных
- Откат при проблемах

---

## 📝 РЕКОМЕНДАЦИИ ПО РЕАЛИЗАЦИИ

### 1. **Начните с малого**
- ✅ Не пытайтесь мигрировать все сразу
- ✅ Первый микросервис: Authentication (проще всего)
- ✅ Второй: Reporting (уже готов, только интеграция)
- ✅ Третий: EMR Integration (самый сложный, но критичный)

### 2. **Strangler Fig Pattern**
- ✅ Не переписывайте монолит полностью
- ✅ Постепенно выносите модули один за другим
- ✅ Монолит и микросервисы работают параллельно
- ✅ Feature flags для переключения между old/new

### 3. **Database per Service**
- ✅ Каждый микросервис имеет свою БД
- ✅ Монолит: Postgres (patients, vas, emr, recommendations)
- ✅ EMR Integration: Postgres (fhir sync history)
- ✅ Analytics: MongoDB (events, metrics)
- ✅ Reporting: Postgres (daily aggregates)

### 4. **API Gateway (будущее)**
После создания микросервисов:
- ✅ Единая точка входа для фронтенда
- ✅ Маршрутизация запросов
- ✅ JWT validation
- ✅ Rate limiting
- ✅ CORS handling

### 5. **Тестирование**
- ✅ Unit тесты для каждого микросервиса
- ✅ Integration тесты с Testcontainers (Postgres, Kafka)
- ✅ Contract tests (Pact) между сервисами
- ✅ End-to-end тесты для критичных сценариев

---

## 🎬 ПЕРВЫЕ ШАГИ (ЧТО ДЕЛАТЬ СЕЙЧАС)

### Шаг 1: **Решение по стратегии** ✅
1. Прочитать этот документ полностью
2. Обсудить с командой (если есть)
3. Внести корректировки в стратегию
4. Утвердить приоритеты микросервисов

### Шаг 2: **Подготовка репозитория** (1 день)
1. Создать структуру папок для микросервисов
2. Создать шаблон микросервиса (pom.xml, Dockerfile, структура)
3. Настроить git branching strategy
4. Сделать полный бэкап

### Шаг 3: **Интеграция с Authentication Service** (2-3 дня)
1. Удалить логин/пароль из монолита
2. Добавить JWT validation в монолит
3. Интегрировать фронтенд с authentication-service
4. Тестирование

### Шаг 4: **Создание первого микросервиса** (5-7 дней)
Выбрать один из:
- **Вариант A (легкий)**: Notification Service
- **Вариант B (важный)**: EMR Integration Service

### Шаг 5: **Kafka Infrastructure** (2-3 дня)
1. Определить все топики
2. Создать схемы событий (JSON Schema или Avro)
3. Настроить Kafka в docker-compose
4. Тестирование pub/sub

---

## 📊 МЕТРИКИ УСПЕХА

### Технические метрики:
- ✅ Размер монолита уменьшен на 60%
- ✅ Время сборки монолита сократилось на 50%
- ✅ Покрытие тестами > 70% для каждого микросервиса
- ✅ Latency < 200ms для критичных операций
- ✅ Uptime > 99.5% для каждого сервиса

### Бизнес-метрики:
- ✅ Время добавления новой фичи сократилось на 40%
- ✅ Время onboarding новых разработчиков сократилось на 50%
- ✅ Количество багов в production сократилось на 30%
- ✅ Время восстановления после инцидента < 30 минут

---

## 📚 ДОПОЛНИТЕЛЬНЫЕ РЕСУРСЫ

### Паттерны микросервисов:
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)
- [Database per Service](https://microservices.io/patterns/data/database-per-service.html)
- [Event-Driven Architecture](https://martinfowler.com/articles/201701-event-driven.html)

### Domain-Driven Design:
- Bounded Context
- Ubiquitous Language
- Core Domain vs Supporting Subdomains

---

## ✅ ЗАКЛЮЧЕНИЕ

Проект находится в **переходном состоянии** между монолитом и микросервисами. У вас уже есть 4 готовых микросервиса, но монолит не интегрирован с ними и дублирует их функциональность.

**Стратегия:**
1. Сначала интегрируйтесь с готовыми микросервисами (Authentication, Reporting, Logging)
2. Затем создайте новые микросервисы для инфраструктурных задач (EMR, Notifications, Analytics)
3. В конце очистите монолит до core domain

**Результат:**
- Монолит: 6 модулей, ~40% текущего размера, только workflow + treatment protocol
- Микросервисы: 9 сервисов, каждый с единой ответственностью
- Архитектура: event-driven, scalable, maintainable

**Временные затраты:**
- Подготовка и анализ: 2-3 дня ✅
- Интеграция с готовыми сервисами: 5-7 дней
- Создание новых микросервисов: 25-30 дней (по 5-7 дней каждый)
- Финальная очистка монолита: 3-5 дней
- **Итого: ~40-45 дней чистой работы**

---

**Документ готов к использованию. Переходим к реализации.**
