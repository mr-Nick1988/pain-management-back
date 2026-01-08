# 🗺️ МИГРАЦИОННАЯ КАРТА (ROADMAP)

**Версия:** 1.0  
**Дата начала:** 08.01.2026  
**Статус:** В процессе

---

## 📊 ОБЩИЙ ПРОГРЕСС

```
ЭТАП 0: Подготовка                           ████████████ 100% ✅
ЭТАП 1: Интеграция с готовыми сервисами      ░░░░░░░░░░░░   0% 🔄
ЭТАП 2: Создание новых микросервисов         ░░░░░░░░░░░░   0% ⏳
ЭТАП 3: Финальная очистка монолита           ░░░░░░░░░░░░   0% ⏳
```

---

## ✅ ЭТАП 0: ПОДГОТОВКА (ЗАВЕРШЕН)

### Выполненные задачи:
- [x] Создана стратегическая документация (`MICROSERVICES_MIGRATION_STRATEGY.md`)
- [x] Создана git ветка `refactor/microservices-split`
- [x] Создана документация по Kafka Event Schemas (`KAFKA_EVENT_SCHEMAS.md`)
- [x] Проверена структура директории `C:\backend_projects\microservices\`

### Результат:
✅ Готовность к началу миграции

---

## 🔄 ЭТАП 1: ИНТЕГРАЦИЯ С ГОТОВЫМИ МИКРОСЕРВИСАМИ

**Цель:** Подключить монолит к существующим микросервисам и удалить дублирующуюся функциональность.

**Длительность:** 5-7 дней  
**Приоритет:** Критический  
**Статус:** Готов к началу

---

### 1.1. Интеграция с Authentication Service

**Директория микросервиса:** `C:\backend_projects\microservices\authentication-service\`

#### 📋 Задачи:

##### A. Анализ текущего состояния
- [ ] Изучить существующий код аутентификации в монолите (`common/persons/`)
- [ ] Проверить endpoints в `PersonController`
- [ ] Определить зависимости на `PersonService`
- [ ] Зафиксировать текущую схему БД для Person

##### B. Настройка JWT Validation в монолите
- [ ] Добавить зависимость `jjwt` в `pom.xml` монолита
- [ ] Создать `JwtValidationFilter` для проверки JWT из cookies
- [ ] Создать `JwtUtil` для парсинга и валидации токенов
- [ ] Добавить конфигурацию `SecurityConfig` с исключениями для публичных endpoints

##### C. REST клиент для Authentication Service
- [ ] Создать `AuthenticationServiceClient` с RestTemplate/WebClient
- [ ] Реализовать методы: `validateToken()`, `getUserInfo()`
- [ ] Добавить Resilience4j Circuit Breaker для устойчивости
- [ ] Конфигурация: `auth.service.url=http://localhost:8082`

##### D. Миграция данных
- [ ] **Решение:** Оставить таблицу `persons` в монолите для аудита
- [ ] Удалить `password` колонку из таблицы (больше не нужна)
- [ ] Создать Liquibase миграцию для изменения схемы
- [ ] Синхронизировать users между монолитом и Authentication Service

##### E. Удаление старого кода
- [ ] Удалить `PersonService.login()`, `PersonService.changeCredentials()`
- [ ] Удалить `PersonController.login()`, `PersonController.changeCredentials()`
- [ ] Оставить `PersonController` только для профиля пользователя
- [ ] Удалить BCrypt и password hashing логику

##### F. Обновление контроллеров
- [ ] Заменить `@RequestParam(defaultValue = "system")` на `@AuthenticationPrincipal`
- [ ] Извлекать `userId` и `role` из JWT токена
- [ ] Добавить аннотацию `@PreAuthorize` для role-based access control

##### G. Тестирование
- [ ] Unit тесты для `JwtValidationFilter`
- [ ] Integration тесты с Testcontainers + WireMock для Auth Service
- [ ] E2E тест: login через Auth Service → запрос к монолиту с JWT

##### H. Документация
- [ ] Обновить README с инструкциями по запуску Auth Service
- [ ] Документировать изменения в API endpoints
- [ ] Создать диаграмму последовательности (sequence diagram) для auth flow

**Критерии готовности:**
- ✅ Монолит валидирует JWT токены из Authentication Service
- ✅ Удален весь код аутентификации из монолита
- ✅ Все endpoints монолита защищены JWT
- ✅ E2E тесты проходят

**Файлы для изменения:**
```
src/main/java/pain_helper_back/
├── common/persons/
│   ├── controller/PersonController.java (упростить)
│   ├── service/PersonService.java (удалить login/password методы)
│   └── entity/Person.java (убрать password field)
├── config/
│   ├── SecurityConfig.java (создать)
│   ├── JwtValidationFilter.java (создать)
│   └── JwtUtil.java (создать)
└── client/
    └── AuthenticationServiceClient.java (создать)

src/main/resources/
└── db/changelog/
    └── remove-password-from-persons.xml (создать)

pom.xml (добавить jjwt)
```

---

### 1.2. Интеграция с Reporting Service

**Директория микросервиса:** `C:\backend_projects\microservices\reporting_service\`

#### 📋 Задачи:

##### A. Анализ текущего reporting в монолите
- [ ] Изучить пакет `reporting/` в монолите
- [ ] Определить используемые endpoints
- [ ] Проверить, кто вызывает reporting функции (UI или внутренние jobs)
- [ ] Зафиксировать схему БД для отчетов

##### B. Настройка Kafka Producer в монолите
- [ ] Создать `ReportingCommandProducer` для публикации команд
- [ ] Добавить конфигурацию Kafka Producer в `application.yml`
- [ ] Создать DTOs для команд: `GenerateDailyReportCommand`, `GeneratePeriodReportCommand`
- [ ] Реализовать методы отправки в топик `reporting-commands`

##### C. REST Proxy для Reporting Service
- [ ] Создать `ReportingServiceClient` для REST вызовов к Reporting Service
- [ ] Проксировать UI запросы к `/api/reports/*` через монолит
- [ ] Добавить Circuit Breaker для устойчивости
- [ ] Конфигурация: `reporting.service.url=http://localhost:8091`

##### D. Dual-write период (переходный)
- [ ] **Опционально:** Публиковать события и в Kafka и оставить старый код
- [ ] Feature flag: `reporting.use-microservice=true/false`
- [ ] Логирование для сравнения результатов
- [ ] Мониторинг расхождений

##### E. Миграция данных
- [ ] **Решение:** Reporting Service имеет свою БД
- [ ] Экспорт существующих отчетов из монолита
- [ ] Импорт в БД Reporting Service
- [ ] Верификация данных

##### F. Удаление старого кода
- [ ] Удалить весь пакет `reporting/` из монолита
- [ ] Удалить зависимости: Apache POI (для Excel), PDFBox, Spring Mail
- [ ] Удалить scheduled jobs для генерации отчетов
- [ ] Очистить конфигурацию email в `application.yml`

##### G. Обновление UI integration
- [ ] Изменить фронтенд для вызова Reporting Service напрямую
- [ ] Или оставить прокси в монолите (рекомендуется для начала)
- [ ] Обновить CORS настройки в Reporting Service

##### H. Тестирование
- [ ] Unit тесты для `ReportingCommandProducer`
- [ ] Integration тесты с EmbeddedKafka
- [ ] E2E тест: запрос отчета → команда в Kafka → отчет сгенерирован
- [ ] Тест экспорта Excel/PDF через прокси

**Критерии готовности:**
- ✅ Монолит публикует команды в Kafka `reporting-commands`
- ✅ Reporting Service обрабатывает команды и генерирует отчеты
- ✅ UI может получать отчеты через монолит-прокси или напрямую
- ✅ Удален весь reporting код из монолита

**Файлы для изменения:**
```
src/main/java/pain_helper_back/
├── reporting/ (УДАЛИТЬ ПОЛНОСТЬЮ)
├── kafka/
│   └── ReportingCommandProducer.java (создать)
├── client/
│   └── ReportingServiceClient.java (создать)
└── config/
    └── KafkaProducerConfig.java (обновить)

pom.xml (удалить Apache POI, PDFBox, Spring Mail)
```

---

### 1.3. Интеграция с Logging Service

**Директория микросервиса:** `C:\backend_projects\microservices\logging-service\`

#### 📋 Задачи:

##### A. Анализ текущей аналитики в монолите
- [ ] Изучить пакет `analytics/` в монолите
- [ ] Определить все domain events
- [ ] Проверить использование MongoDB в монолите
- [ ] Зафиксировать список событий для публикации

##### B. Создание Domain Events DTOs
- [ ] `RecommendationGeneratedEvent`
- [ ] `RecommendationApprovedEvent`
- [ ] `RecommendationRejectedEvent`
- [ ] `EscalationCreatedEvent`
- [ ] `PatientRegisteredEvent`
- [ ] `VasRecordedEvent`
- [ ] `EmrUpdatedEvent`

##### C. Kafka Producer для Analytics Events
- [ ] Создать `AnalyticsEventProducer` для публикации событий
- [ ] Добавить конфигурацию Kafka Producer
- [ ] Топик: `analytics-events`
- [ ] JSON сериализация с correlation ID

##### D. Замена @EventListener на Kafka Producer
- [ ] Найти все `@EventListener` в монолите
- [ ] Заменить сохранение в MongoDB на публикацию в Kafka
- [ ] Удалить `AnalyticsEventListener`
- [ ] Обновить сервисы для публикации событий

##### E. Удаление MongoDB из монолита
- [ ] **Внимание:** Проверить, используется ли MongoDB для чего-то еще
- [ ] Удалить зависимость `spring-boot-starter-data-mongodb` из `pom.xml`
- [ ] Удалить конфигурацию MongoDB из `application.yml`
- [ ] Удалить `AnalyticsMongoConfig`
- [ ] Удалить пакет `analytics/entity/`, `analytics/repository/`

##### F. Удаление Performance Monitoring (опционально)
- [ ] **Решение:** Вынести в Analytics & Monitoring Service позже
- [ ] Пока оставить AOP аспекты для performance monitoring
- [ ] Публиковать метрики в Kafka вместо MongoDB

##### G. Тестирование
- [ ] Unit тесты для `AnalyticsEventProducer`
- [ ] Integration тесты с EmbeddedKafka
- [ ] Verify: события публикуются в правильном формате
- [ ] E2E тест: создание рекомендации → событие в Kafka → сохранено в Logging Service

**Критерии готовности:**
- ✅ Монолит публикует все domain events в Kafka
- ✅ Logging Service получает и сохраняет события
- ✅ MongoDB удален из монолита (если не используется для других целей)
- ✅ Аналитика работает через Logging Service

**Файлы для изменения:**
```
src/main/java/pain_helper_back/
├── analytics/ (УДАЛИТЬ большую часть)
│   ├── event/ (оставить DTOs, удалить @EventListener)
│   └── aspect/ (оставить для performance, изменить на Kafka)
├── kafka/
│   └── AnalyticsEventProducer.java (создать)
├── nurse/service/NurseServiceImpl.java (публиковать события)
├── doctor/service/DoctorServiceImpl.java (публиковать события)
└── anesthesiologist/service/AnesthesiologistServiceImpl.java (публиковать события)

pom.xml (удалить MongoDB если не нужна)
application.yml (удалить MongoDB config)
```

---

### 1.4. Проверка Backup & Restore Service

**Директория микросервиса:** `C:\backend_projects\microservices\backup_restore\`

#### 📋 Задачи:

##### A. Проверка готовности
- [ ] Убедиться, что Backup & Restore Service запускается
- [ ] Проверить endpoints: `/api/backup/create`, `/api/backup/restore`
- [ ] Протестировать бэкап Postgres монолита
- [ ] Протестировать рестор Postgres монолита

##### B. Интеграция (опционально)
- [ ] **Решение:** Backup Service - это ops инструмент, не требует интеграции
- [ ] Использовать вручную или через cron jobs
- [ ] Документировать процедуры бэкапа/рестора

**Критерии готовности:**
- ✅ Backup Service работает
- ✅ Можно делать бэкап БД монолита
- ✅ Можно восстанавливать БД монолита

---

### Итоговый результат ЭТАПА 1:

После завершения всех подэтапов:

**Удалено из монолита:**
- ❌ `common/persons/service/PersonService` (login/password методы)
- ❌ `reporting/` (весь пакет)
- ❌ `analytics/` (большая часть, кроме DTOs)

**Добавлено в монолит:**
- ✅ JWT Validation для Authentication Service
- ✅ Kafka Producer для Reporting Commands
- ✅ Kafka Producer для Analytics Events
- ✅ REST клиенты для Auth и Reporting Services

**Размер монолита:**
- Было: ~18 модулей
- Стало: ~12 модулей (-33%)
- Код: -40% строк кода

**Микросервисы в работе:**
- ✅ Authentication Service (8082)
- ✅ Reporting Service (8091)
- ✅ Logging Service (8081/8083)
- ✅ Backup & Restore Service (8085)

---

## 🆕 ЭТАП 2: СОЗДАНИЕ НОВЫХ МИКРОСЕРВИСОВ

**Длительность:** 25-30 дней (по 5-7 дней на каждый)  
**Приоритет:** Высокий  
**Статус:** Ожидание завершения ЭТАПА 1

---

### 2.1. EMR Integration Service

**Приоритет:** #1 (самый важный для бизнес-логики)

#### Спецификация:

**Порт:** 8086  
**БД:** Postgres (emr_integration_db)  
**Kafka:** Producer (`emr.changes`, `emr.upserted`, `emr.critical.alerts`)

#### Переносимые модули:
- `external_emr_integration_service/` (FHIR клиент, HAPI)
- `emr_recalculation/` (детекция изменений)

#### Задачи:
- [ ] Создать структуру проекта в `C:\backend_projects\microservices\emr-integration-service\`
- [ ] Скопировать код из монолита
- [ ] Создать `pom.xml` с зависимостями (HAPI FHIR, Kafka, Postgres)
- [ ] Реализовать FHIR sync scheduler (cron: каждые 6 часов)
- [ ] Детекция критических изменений EMR
- [ ] Kafka Producer для публикации событий
- [ ] REST API для ручной синхронизации
- [ ] Dockerfile и docker-compose интеграция
- [ ] Тестирование с HAPI FHIR Test Server
- [ ] Удалить код из монолита после успешной интеграции

**Интерфейс с монолитом:**
- Kafka: монолит слушает `emr.changes` и пересчитывает рекомендации
- REST (опционально): `/api/emr/sync/{mrn}` для ручной синхронизации

---

### 2.2. Notification Service

**Приоритет:** #2 (важен для UX)

#### Спецификация:

**Порт:** 8087  
**БД:** Postgres (notification_db) для истории уведомлений  
**Kafka:** Consumer (`notification.requests`)

#### Переносимые модули:
- `websocket/` (WebSocket сервер)
- Email notification функционал

#### Задачи:
- [ ] Создать проект в `C:\backend_projects\microservices\notification-service\`
- [ ] WebSocket endpoints для UI подключений
- [ ] STOMP messaging
- [ ] Email sending (Spring Mail + SMTP)
- [ ] Kafka Consumer для `notification.requests`
- [ ] История уведомлений в БД
- [ ] Управление подписками
- [ ] Dockerfile
- [ ] Тестирование
- [ ] Удалить WebSocket из монолита

**Интерфейс с монолитом:**
- Kafka: монолит публикует `notification.requests`
- WebSocket: фронтенд подключается напрямую к Notification Service (ws://localhost:8087/ws)

---

### 2.3. Analytics & Monitoring Service

**Приоритет:** #3

#### Спецификация:

**Порт:** 8088  
**БД:** MongoDB (analyticsdb)  
**Kafka:** Consumer (`analytics-events`, `performance-metrics`)

#### Переносимые модули:
- `analytics/` (остатки после ЭТАПА 1)
- `performance_SLA_monitoring/`

#### Задачи:
- [ ] Создать проект
- [ ] MongoDB репозитории для events
- [ ] Kafka Consumer
- [ ] REST API для dashboards
- [ ] Aggregation pipelines
- [ ] Performance metrics collection
- [ ] Dockerfile
- [ ] Удалить остатки analytics из монолита

---

### 2.4. Pain Escalation Tracking Service

**Приоритет:** #4

#### Спецификация:

**Порт:** 8089  
**БД:** Postgres (pain_escalation_db)  
**Kafka:** Producer (`dose.administered`, `pain.escalated`)

#### Переносимые модули:
- `pain_escalation_tracking/`

---

### 2.5. External VAS Integration Service

**Приоритет:** #5

#### Спецификация:

**Порт:** 8090  
**БД:** Postgres (vas_integration_db)  
**Kafka:** Producer (`vas.external.received`)

#### Переносимые модули:
- `VAS_external_integration/`

---

## 🧹 ЭТАП 3: ФИНАЛЬНАЯ ОЧИСТКА МОНОЛИТА

**Длительность:** 2-3 дня  
**Приоритет:** Средний  
**Статус:** Ожидание завершения ЭТАПА 2

### Задачи:

#### A. Удаление зависимостей из pom.xml
- [ ] Удалить `hapi-fhir-*` (FHIR клиент)
- [ ] Удалить `spring-boot-starter-websocket`
- [ ] Удалить `spring-boot-starter-mail`
- [ ] Удалить `apache.pdfbox`
- [ ] Удалить `resilience4j` (если не используется)
- [ ] Удалить `javafaker`

#### B. Очистка пакетов
- [ ] Удалить `external_emr_integration_service/`
- [ ] Удалить `emr_recalculation/`
- [ ] Удалить `pain_escalation_tracking/`
- [ ] Удалить `VAS_external_integration/`
- [ ] Удалить `websocket/`
- [ ] Удалить `performance_SLA_monitoring/`

#### C. Очистка конфигурации
- [ ] Удалить FHIR настройки из `application.yml`
- [ ] Удалить SMTP настройки
- [ ] Удалить WebSocket настройки
- [ ] Очистить неиспользуемые профили

#### D. Итоговая структура монолита
```
src/main/java/pain_management_core/
├── PainManagementCoreApplication.java
├── admin/
├── doctor/
├── nurse/
├── anesthesiologist/
├── treatment_protocol/
├── common/
│   └── patients/
├── config/
└── enums/
```

#### E. Оптимизация
- [ ] Обновить README
- [ ] Создать новый Dockerfile для монолита
- [ ] Оптимизировать сборку (build time)
- [ ] Измерить метрики (размер jar, время сборки)

---

## 📊 МЕТРИКИ УСПЕХА

### После завершения всех этапов:

**Монолит:**
- Модули: 6 (было 18) ✅
- Размер кода: -60% ✅
- Время сборки: -50% ✅
- Зависимости: ~10 (было ~20) ✅

**Микросервисы:**
- Количество: 9 сервисов
- Каждый с единой ответственностью ✅
- Event-driven архитектура ✅
- Независимое развертывание ✅

---

## 🎯 ТЕКУЩИЙ ФОКУС

**СЕЙЧАС:** Начинаем ЭТАП 1.1 - Интеграция с Authentication Service

**СЛЕДУЮЩЕЕ:** ЭТАП 1.2 - Интеграция с Reporting Service

**БЛОКЕРЫ:** Нет

---

## 📝 CHANGELOG

### 08.01.2026
- ✅ Создан roadmap
- ✅ Завершен ЭТАП 0 (подготовка)
- 🔄 Начинается ЭТАП 1.1 (Authentication Service)

---

**Документ обновляется по мере прогресса миграции.**
