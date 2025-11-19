# Сквозная цепочка бизнес-аналитики (Frontend → Monolith → Kafka → Analytics-Reporting → Mongo)

Документ фиксирует текущую архитектуру, найденные проблемы и конкретные исправления для того, чтобы бизнес-события из монолита попадали в базу MongoDB (analytics_db), были доступны фронтенду, и агрегировались в PostgreSQL.

## Архитектура и адреса
- Frontend (локально)
- Monolith (Spring Boot): http://localhost:8080
  - Публикация аналитики в Kafka через `AnalyticsPublisher`
  - Kafka bootstrap: `localhost:9092`
- Kafka (Docker/KRaft):
  - Внутри сети docker: `kafka:29092`
  - С хоста: `localhost:9092`
  - Топик аналитики: `analytics-events`
- Analytics-Reporting Service (микросервис): http://localhost:8091
  - Kafka consumer `@KafkaListener` читает `analytics-events`
  - Пишет в MongoDB: `mongodb://mongodb-analytics:27017/analytics_db`
- MongoDB:
  - analytics_db: `localhost:27017` (контейнер `dev_mongodb_analytics`)
  - logging_db: `localhost:27018` (контейнер `dev_mongodb_logging`)

## Ключевые файлы и настройки
- Monolith
  - Kafka Producer: `src/main/java/pain_helper_back/config/KafkaProducerConfig.java`
  - Паблишер аналитики: `src/main/java/pain_helper_back/analytics/publisher/AnalyticsPublisher.java`
  - Kafka bootstrap servers: `src/main/resources/application.properties` → `spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`
  - Кастомный Mongo (перфоманс): `src/main/java/pain_helper_back/mongo_config/PerformanceMongoConfig.java`
- Analytics-Reporting Service
  - Bootstrap: `microservices/analytics_reporting_service/src/main/java/pain/managment/analytics_reporting_service/AnalyticsReportingServiceApplication.java` (`@EnableKafka`)
  - Kafka Consumer Config: `microservices/analytics_reporting_service/src/main/java/pain/managment/analytics_reporting_service/config/KafkaConsumerConfig.java`
  - Consumer (`@KafkaListener`): `microservices/analytics_reporting_service/src/main/java/pain/managment/analytics_reporting_service/kafka/AnalyticsEventConsumer.java`
  - Модель Mongo: `microservices/.../entity/mongo/AnalyticsEvent.java` (@Document collection="analytics_events")
- Docker Compose
  - `docker-compose.dev.yml`
    - Kafka: профиль по умолчанию, топики создаются job`ой `kafka-create-topics`
    - analytics-reporting-service: профиль `analytics`, порт `8091:8091`
    - logging-service: профиль `logging`

## Найденные проблемы (root causes)
1) Analytics consumer не стартовал из-за конфликта настроек десериализатора
- Сообщение: `IllegalStateException: JsonDeserializer must be configured with property setters, or via configuration properties; not both`
- Причина: одновременно задавались свойства `JsonDeserializer` через `props.put(...)` и через инстанс `new JsonDeserializer<>(...).addTrustedPackages("*")`.
- Исправление: удалить `JsonDeserializer.*` из props и оставить конфигурацию только через инстанс десериализатора.
  - Файл: `microservices/.../config/KafkaConsumerConfig.java`
  - Статус: ИСПРАВЛЕНО.

2) Kafka недоступна для монолита в момент отправки
- Логи монолита: `Connection refused localhost:9092`, `Bootstrap broker ... disconnected`.
- Причина: стек Docker не был запущен/здоров (Kafka broker down) → события не были отправлены.
- Исправление: запускать Kafka до монолита. При восстановлении — повторно вызвать бизнес-операции, чтобы события ушли в топик.
  - Статус: КОНТРОЛИРУЕТСЯ инфраструктурой (compose).

3) Отсутствует подтверждение публикации аналитики при логине
- В монолите есть метод `AnalyticsPublisher.publishUserLogin(...)`, но прямых вызовов из потока аутентификации не найдено.
- Следствие: логины (USER_LOGIN_SUCCESS/FAILED) не отправляются в Kafka → в Mongo ничего не появляется при простом логине.
- Исправление: добавить вызов `publishUserLogin` в успешный login-flow.
  - Если логин реализован в отдельном `authentication-service`, публиковать оттуда в `analytics-events`.
  - Статус: ТРЕБУЕТСЯ ДОРАБОТКА КОДА.

4) Эндпоинт, который читает события для фронтенда
- Фронт вызывает монолит: `GET /api/analytics/events/recent?limit=50`.
- В репозитории монолита соответствующего контроллера не найдено; запрос приводит к /error и 403 в логах (это нормальная реакция при ошибке маршрутизации/обработке).
- В `analytics-reporting-service` REST-контроллеров под `/api/analytics/...` в репозитории также не обнаружено (см. поиск). Ранее в переписке приводился пример контроллера — вероятно, он не добавлен в текущую кодовую базу микросервиса.
- Следствие: фронтенд не может получить список событий.
- Варианты исправления:
  - A) Добавить в `analytics-reporting-service` REST-контроллеры:
    - `POST /api/analytics/events` — для ingest (фоллбек)
    - `GET /api/analytics/events` (by period), `GET /api/analytics/events/recent?limit=...` — для чтения.
  - B) Либо проксировать через монолит: добавить контроллер в монолите, который дергает REST микросервиса (`analytics.reporting.base-url=http://localhost:8091`).
  - C) Либо использовать API Gateway/Ingress.
  - Статус: ТРЕБУЕТСЯ ДОРАБОТКА КОДА.

5) Несоответствие портов и CORS
- Для логгинг-сервиса ранее не совпадали внутренний порт и маппинг (исправлено: `SERVER_PORT: 8083` при маппинге `8081:8083`).
- Для аналитик-сервиса: `8091:8091` корректно. Убедиться, что фронтенд обращается к нужному origin и CORS разрешен (если идёт прямая интеграция).
- Статус: ПРОВЕРИТЬ НАСТРОЙКИ ФРОНТА/CORS.

6) Сбои монолита из-за кастомной Mongo-конфигурации (перфоманс)
- Ошибка при старте: `ClientSessionException: state should be: open` при создании индексов.
- Причина: отсутствовали дефолтные URI в properties для `PerformanceMongoConfig`.
- Исправление: добавлены дефолты
  - `app.mongodb.performance.uri=${MONGODB_PERFORMANCE_URI:mongodb://localhost:27017}`
  - `app.mongodb.backup.uri=${MONGODB_BACKUP_URI:mongodb://localhost:27017}`
  - Статус: ИСПРАВЛЕНО.

7) В Compass открывается не та база
- Логи: фронтенд показывает события, но в Compass «не видно» — часто из-за подключения к `27017` (analytics_db) вместо `27018` (logging_db) или наоборот.
- Исправление: 
  - Аналитика: `mongodb://localhost:27017/analytics_db` → коллекция `analytics_events`
  - Логи: `mongodb://localhost:27018/logging_db` → коллекция `log_entries`
  - Статус: ПРОВЕРИТЬ ПОДКЛЮЧЕНИЕ.

## Конкретные внесённые изменения
- `docker-compose.dev.yml`
  - analytics-reporting-service: добавлены
    - `SPRING_KAFKA_CONSUMER_GROUP_ID=analytics-reporting-group`
    - `KAFKA_TOPICS_ANALYTICS_EVENTS=analytics-events`
    - `SERVER_PORT=8091`
  - Удалены конфликтующие env для JsonDeserializer (`SPRING_KAFKA_CONSUMER_*_SPRING_JSON_*`).
  - logging-service: добавлены `SPRING_KAFKA_CONSUMER_GROUP_ID`, `SERVER_PORT=8083` под маппинг `8081:8083`.
- Monolith `application.properties`
  - Добавлены дефолтные URI для кастомных Mongo:
    - `app.mongodb.performance.uri=${MONGODB_PERFORMANCE_URI:mongodb://localhost:27017}`
    - `app.mongodb.backup.uri=${MONGODB_BACKUP_URI:mongodb://localhost:27017}`
- Analytics-Reporting `KafkaConsumerConfig`
  - Удалена двойная конфигурация `JsonDeserializer` через props.
  - Оставлена конфигурация только через инстанс десериализатора.

## Что нужно сделать дальше (план фиксов)
1) Публикация событий логина
- Внедрить вызов `AnalyticsPublisher.publishUserLogin(personId, role, success, ipAddress)` в успешный login-flow:
  - либо в монолите, если логин там;
  - либо в `authentication-service`, если логин там (с Kafka bootstrap `kafka:29092` или `localhost:9092` в зависимости от окружения).

2) REST API для чтения событий фронтом
- Реализовать контроллер в `analytics-reporting-service`:
  - `GET /api/analytics/events/recent?limit=...` — N последних событий (из `analytics_events`)
  - `GET /api/analytics/events?start&end` — выборка по периоду
- Либо добавить прокси-контроллер в монолите, который бьёт в `http://localhost:8091`.
- Обновить фронтенд URL на реальный backend (8091 или монолитный прокси). Настроить CORS.

3) Проверка сквозного пути
- Действие в монолите (или аутентификации) → сообщение в `analytics-events` → consumer сохраняет в Mongo → фронт читает `GET /api/analytics/events/...`.
- Инструменты:
  - Kafdrop: http://localhost:9000 → `analytics-events`
  - Mongo: `analytics_db.analytics_events`

4) Доступ (AUTHZ) для ADMIN
- Убедиться, что путь чтения событий открыт для роли ADMIN.
- Если в монолите прокси-контроллер — добавить правила в `SecurityConfig` (разрешить `GET /api/analytics/**` для ADMIN).
- Проверить, чтобы `/error` не блокировался, либо чтобы не происходил форвард на /error из-за отсутствующих контроллеров.

## Частые причины «ничего не видно»
- Kafka не поднята/не здорова → продюсер в монолите не шлёт (см. логи).
- Endpoint фронта смотрит на монолит, а контроллера там нет → редирект на `/error` и 403.
- В микросервисе нет контроллера для чтения событий → нечего отдавать фронту.
- Логин не публикует событие → при логине ничего не улетает в `analytics-events`.
- Compass подключён к другой базе/порту.

## Контрольный список
- [ ] В login-flow добавить вызов `publishUserLogin(...)` (или реализовать в authentication-service).
- [ ] Добавить REST контроллеры чтения событий в analytics-reporting-service (или прокси в монолит).
- [ ] Проверить, что фронтенд обращается к 8091 (или к монолиту-прокси) и CORS разрешён.
- [ ] Убедиться, что Kafka и топик `analytics-events` живы; сообщения появляются в Kafdrop.
- [ ] Проверить, что документы появляются в `analytics_db.analytics_events`.
- [ ] Настроить правила доступа для ADMIN на чтение `/api/analytics/**`.

## Приложение: Текущие значения и порты
- Kafka: `localhost:9092` (host), `kafka:29092` (docker)
- analytics-reporting-service: `8091` (host)
- Mongo Analytics: `localhost:27017/analytics_db`, коллекция `analytics_events`
- Mongo Logging: `localhost:27018/logging_db`, коллекция `log_entries`
- Топик: `analytics-events` (создаётся job`ой compose)

