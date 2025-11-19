# Полная расшифровка конфигурации Монолита (application.properties)

Файл: `src/main/resources/application.properties`

Данный документ построчно объясняет ключевые свойства монолита и их назначение. Значения в фигурных скобках `${VAR:default}` означают: если переменная окружения `VAR` не задана, используется `default`.

---

## 1. Базовые настройки приложения

```properties
spring.application.name=pain_helper_back
server.port=8080
```
- `spring.application.name` — логическое имя приложения, попадает в логи/актуаторы.
- `server.port` — HTTP‑порт монолита (по умолчанию 8080).

---

## 2. База данных (PostgreSQL)

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/pain_management_db}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver
```
- `spring.datasource.url` — JDBC URL основной БД. По умолчанию — Postgres из compose на хосте `localhost:5432`, БД `pain_management_db`.
- `spring.datasource.username` — имя пользователя Postgres (по умолчанию `postgres`).
- `spring.datasource.password` — пароль к Postgres (по умолчанию `postgres`).
- `driver-class-name` — JDBC‑драйвер Postgres.

```properties
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
```
- JPA/Hibernate настройка: диалект PostgreSQL, авто‑миграции схемы `update` (под dev), форматирование SQL.

```properties
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.maximum-pool-size=5
```
- Пул соединений HikariCP: таймаут 20с, максимум 5 соединений (под локальную разработку).

```properties
spring.liquibase.enabled=false
```
- Liquibase отключен (схема управляется Hibernate `ddl-auto=update`).

---

## 3. JWT (секрет должен совпадать с сервисом аутентификации)

```properties
jwt.secret=${JWT_SECRET}
```
- Секрет для подписи JWT. В dev можно прокинуть через переменную окружения или задать дефолт в compose для Auth. Монолиту важно использовать тот же секрет, что и Auth-сервис.

---

## 4. MongoDB (отключён автоконфиг монго в монолите)

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
spring.data.mongodb.repositories.enabled=false
spring.data.mongodb.auto-index-creation=true

app.mongodb.performance.uri=${MONGODB_PERFORMANCE_URI}
app.mongodb.backup.uri=${MONGODB_BACKUP_URI}
app.mongodb.performance.database=performance_db
app.mongodb.backup.database=backup_db
```
- Автоконфигурация Spring Data Mongo отключена (аналитика вынесена в микросервис), но оставлены кастомные URI для задач производительности/бэкапов, если вы их используете отдельно.

---

## 5. Kafka (интеграция с сервисом аналитики)

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
```
- `bootstrap-servers` — адресы брокеров Kafka. По умолчанию локальный хост‑листенер `localhost:9092` (KRaft). В контейнерах микросервисов используется `kafka:29092` — монолиту это не нужно.
- Сериализаторы ключа/значения, политика подтверждения `acks=all`, количество ретраев 3 — безопасные дефолты для отправки аналитических событий.

```properties
kafka.topic.analytics-events=${KAFKA_TOPIC_ANALYTICS_EVENTS:analytics-events}
```
- Имя топика, куда монолит публикует аналитические события. Топик создаётся контейнером `kafka-create-topics`.

---

## 6. REST‑интеграция с Analytics‑Reporting (fallback)

```properties
analytics.reporting.base-url=${ANALYTICS_REPORTING_BASE_URL:http://localhost:8091}
```
- Бэкап‑вариант (REST) на случай, если Kafka недоступна: базовый URL сервиса аналитики.

---

## 7. Асинхронность и SLA

```properties
spring.task.execution.pool.core-size=5
performance.sla.enabled=true
performance.sla.async-recording=true
```
- Пул задач Spring для асинхронных операций (ядро — 5 потоков).
- Мониторинг SLA включён, запись асинхронная.

---

## 8. Бэкапы

```properties
backup.postgres.directory=./backups/postgres
backup.mongo.directory=./backups/mongodb
backup.retention.days=30
backup.mongo.mongodump.path=mongodump
backup.mongo.mongorestore.path=mongorestore
```
- Локальные директории для бэкапов и пути к утилитам `mongodump`/`mongorestore` при их использовании.

---

## 9. FHIR (внешний EMR)

```properties
fhir.server.url=http://hapi.fhir.org/baseR4
fhir.connection.timeout=30000
fhir.socket.timeout=30000
```
- Внешний FHIR‑сервер (R4) для интеграции с EMR, таймауты соединения/сокета — 30с.

---

## 10. Почта (Spring Mail)

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```
- SMTP‑настройки для отправки писем (уведомления/алерты). Для прод/теста замените на ваш SMTP (или Mailtrap).

---

## 11. Пороговые значения эскалации боли (опционально)

```properties
#pain.escalation.min-vas-increase=2
#pain.escalation.min-dose-interval-hours=4
#pain.escalation.critical-vas-level=8
#pain.escalation.high-vas-level=6
#pain.escalation.trend-analysis-period-hours=24
#pain.escalation.max-escalations-per-period=3
```
- Закомментировано. Можно включить и настроить пороги под нужды исследования.

---

## 12. Логи

```properties
logging.level.pain_helper_back=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.pain_helper_back.security.JwtAuthenticationFilter=DEBUG
```
- Уровни логирования для пакетов приложения и безопасности. В dev удобно держать `DEBUG`, в prod — снижать до `INFO/WARN`.

---

## 13. Практические рекомендации

- Для локальной разработки достаточно дефолтов в этом файле: Postgres `localhost:5432`, Kafka `localhost:9092` (из нашего compose KRaft), Analytics REST `http://localhost:8091`.
- Если необходимо переопределить свойства на машине (без контейнера) — задавайте переменные окружения в ОС: `setx KAFKA_BOOTSTRAP_SERVERS localhost:9092` (Windows) и перезапускайте IDE/терминал.
- В Docker Compose микросервисы получают адреса через `environment`, монолит обычно запускается на хосте с дефолтами `localhost` — специально разделено, чтобы «localhost» внутри контейнера не ломал подключения.
