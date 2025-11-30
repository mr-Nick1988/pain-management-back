# LulaGermis — Logging Service: Отчетная документация

Ниже — готовая отчетная документация для хранения на Monolith. Содержит назначение, архитектуру, интерфейсы, конфигурацию, эксплуатационные аспекты и особенности реализации.

## Назначение
- **Цель**: централизованный прием и хранение событий логирования из микросервисов системы.
- **Источник событий**: Kafka-топик с JSON-сообщениями формата `LogEventDTO`.
- **Хранилище**: MongoDB коллекция `log_entries`.
- **Доступ к данным**: REST API для выборок и агрегатов по логам, а также ручная очистка старых записей.

## Архитектура и стек
- **Язык/платформа**: Java 21, Spring Boot 3.5.7.
- **Компоненты**:
  - **Kafka consumer** (`@KafkaListener`) с ручным подтверждением (`AckMode.MANUAL`).
  - **MongoDB** (`spring-boot-starter-data-mongodb`), индексы на ключевых полях.
  - **REST API** (Spring Web) для выдачи логов и простой статистики.
  - **Actuator** для health/info/metrics.
  - **CORS** для `/api/**` на локальные фронтенд-порты.
- **Системные зависимости**:
  - Доступный кластер Kafka.
  - Доступная MongoDB.
- **Пакеты кода**: `config`, `consumer`, `controller`, `dto`, `entity`, `repository`, `service`.

## Модель данных
- Коллекция: `log_entries`.
- Схема `LogEntry` (Mongo document):
  - `id` (String, `@Id`)
  - `timestamp` (LocalDateTime, `@Indexed`)
  - `className`, `methodName`, `methodSignature`
  - `arguments` (String)
  - `durationMs` (Long), `success` (Boolean)
  - `errorMessage`, `errorStackTrace`
  - `userId`, `sessionId`
  - `logCategory` (String, `@Indexed`)
  - `logLevel` (String, `@Indexed`)
  - `module` (String, `@Indexed`)
  - `traceId`, `spanId`
- Индексы: `timestamp`, `logCategory`, `logLevel`, `module` — для ускорения выборок.

## Входящие интерфейсы

### Kafka consumer
- **Топик**: `${KAFKA_TOPIC_LOGGING_EVENTS:logging-events}`.
- **Группа**: `${spring.kafka.consumer.group-id:logging-service-group}`.
- **Подтверждение**: ручное (`AckMode.MANUAL`), `enable-auto-commit=false` → семантика «at-least-once».
- **Конкурентность**: 3 consumer-потока.
- **Десериализация**: `JsonDeserializer` с `VALUE_DEFAULT_TYPE = pain.managment.logging.dto.LogEventDTO`.
- Обратите внимание на написание пакета: `pain.managment` (без буквы "e"). Это важно для десериализации и настройки уровней логирования.

Пример события (`LogEventDTO`):
```json
{
  "id": "2f1d1c86-5b3a-4a78-a509-2f1af5b9a111",
  "timestamp": "2025-01-13T10:15:30",
  "className": "com.example.Service",
  "methodName": "doWork",
  "methodSignature": "String doWork(int, String)",
  "arguments": "{\"arg0\":42,\"arg1\":\"abc\"}",
  "durationMs": 123,
  "success": true,
  "errorMessage": null,
  "errorStackTrace": null,
  "userId": "u-123",
  "sessionId": "s-456",
  "logCategory": "BUSINESS",
  "level": "INFO",
  "module": "nurse",
  "traceId": "trace-1",
  "spanId": "span-1"
}
```

### HTTP API (REST)
Базовый префикс: `/api/logs`

- `GET /api/logs/module/{module}?startDate=ISO&endDate=ISO`
  - Выборка по модулю и интервалу времени.
- `GET /api/logs/level/{level}?startDate=ISO&endDate=ISO` (оба параметра опциональны)
  - Если даты не переданы — вернет все записи с уровнем `level`.
- `GET /api/logs?startDate=ISO&endDate=ISO` (опционально)
  - Все логи за интервал или все логи.
- `GET /api/logs/user/{userId}?startDate=ISO&endDate=ISO`
  - По пользователю и интервалу.
- `GET /api/logs/stats/{level}`
  - Количество записей указанного уровня.
- `DELETE /api/logs/clean?daysToKeep=7`
  - Удаление логов старше `now - daysToKeep`.

Примеры:
```bash
# Все INFO-логи
curl "http://localhost:8080/api/logs/level/INFO"

# WARN-логи за интервал
timestamp_start="2025-01-10T00:00:00"; timestamp_end="2025-01-11T00:00:00"
curl "http://localhost:8080/api/logs/level/WARN?startDate=$timestamp_start&endDate=$timestamp_end"

# По модулю 'nurse' за интервал
curl "http://localhost:8080/api/logs/module/nurse?startDate=$timestamp_start&endDate=$timestamp_end"

# Очистить старше 30 дней
curl -X DELETE "http://localhost:8080/api/logs/clean?daysToKeep=30"
```

Дата-время — формат ISO (`DateTimeFormat.ISO.DATE_TIME`), пример: `2025-01-10T12:30:00`.

## Конфигурация и переменные окружения

- Spring Boot:
  - `spring.application.name=logging-service`
- Профили/локальные настройки (`src/main/resources/application-local.yml`):
  - `spring.data.mongodb.uri`: `${SPRING_DATA_MONGODB_URI:mongodb://localhost:27018/logging_db}`
  - `spring.data.mongodb.auto-index-creation: true`
  - `spring.kafka.bootstrap-servers`: `${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`
  - `spring.kafka.consumer.group-id`: `logging-service-group`
  - `spring.kafka.consumer.auto-offset-reset`: `earliest`
  - `spring.kafka.consumer.enable-auto-commit`: `false`
  - `kafka.topic.logging-events`: `${KAFKA_TOPIC_LOGGING_EVENTS:logging-events}`
  - Actuator:
    - `management.endpoints.web.exposure.include: health,info,metrics`
    - `management.endpoint.health.show-details: always`
  - Логирование:
    - `pain.management.logging: DEBUG` (см. ниже про рассинхронизацию namespace).

Важно:
- Для применения `application-local.yml` активируйте профиль: `SPRING_PROFILES_ACTIVE=local`.
- В коде package — `pain.managment.logging` (без «e»), а в конфиге логгера — `pain.management.logging`. Это может привести к тому, что уровень логгирования пакета не применится к нужному пространству имен.

## Сборка и запуск

- Maven:
  - Сборка: `mvn -B -DskipTests package`
  - Артефакт: `target/*.jar`
- Docker (multi-stage):
  - Build stage: `maven:3.9-eclipse-temurin-21`, кэш зависимостей через `dependency:go-offline`.
  - Runtime: `eclipse-temurin:21-jre`, копирует `app.jar`, переменная `JAVA_OPTS` поддерживается.
  - `EXPOSE 8083` в Dockerfile.
  - Entrypoint: `exec java $JAVA_OPTS -jar /app/app.jar`.

Порты:
- В приложении `server.port` не задан — дефолт Spring Boot `8080`.
- Dockerfile объявляет `EXPOSE 8083`. Рекомендуется привести к единообразию:
  - Задать `server.port=8083`, или
  - Изменить `EXPOSE` на `8080`.

## Наблюдаемость
- **Actuator**: `/actuator` (экспонируются `health`, `info`, `metrics`).
- **Логирование**:
  - Конфиг в `application-local.yml` (см. рассинхронизацию namespace пакета).
- **Трейсинг**:
  - В лог-событиях предусмотрены `traceId` и `spanId` (складываются в Mongo). Интеграция с системой трассировки происходит на стороне источника событий.

## Безопасность и CORS
- Явных настроек Spring Security нет — API публично доступен.
- **CORS** (`WebConfig`):
  - Путь: `/api/**`
  - Разрешенные origin: `http://localhost:3000`, `http://localhost:5173`, `http://localhost:5174`
  - Методы: `GET, POST, PUT, DELETE, OPTIONS`
  - Заголовки: `*`
  - `allowCredentials: true`

При необходимости ограничить доступ — добавить конфигурацию Spring Security (JWT/OAuth2, сервисные ключи и пр.).

## Надежность и производительность
- **Kafka consumer**:
  - Ручной `ack` → «at-least-once».
  - `max.poll.records=100`, `concurrency=3`.
  - При исключении сообщение не подтверждается — повторная обработка.
  - DLQ/RetryTopic не настроены — возможна повторная доставка «ядовитых» сообщений без изоляции.
- **Идемпотентность**:
  - `id` события, если не задан, генерируется. При повторной доставке с тем же `id` запись будет перезаписана; при разном `id` — возможны дубли.
- **Очистка**:
  - `DELETE /api/logs/clean` удаляет записи старше `now - daysToKeep`.
  - Планировщика (cron) нет — только ручной вызов или внешняя оркестрация.
- **Транзакции**:
  - Методы помечены `@Transactional`. Для транзакций в Mongo потребуется replica set и `MongoTransactionManager`; в противном случае гарантий транзакционности нет.

## Заметные особенности и потенциальные улучшения
- **Несоответствие namespace для логгера**: `pain.management.logging` vs `pain.managment.logging`.
- **Порт**: `EXPOSE 8083` vs дефолт `8080`. Привести к единообразию.
- **Безопасность**: добавить аутентификацию/авторизацию при необходимости.
- **Надежность обработки**: добавить DLQ/RetryTopic и алерты на «ядовитые» сообщения.
- **Retention**: внедрить плановую очистку (Spring Scheduler/Quartz/K8s CronJob).
