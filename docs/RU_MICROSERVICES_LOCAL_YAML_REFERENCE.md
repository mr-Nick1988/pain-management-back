# Микросервисы: полная расшифровка application-local.yml (Kafka KRaft)

Этот документ — ПОДРОБНАЯ инструкция для вставки и понимания `application-local.yml` в микросервисах Authentication, Analytics-Reporting и Logging. Здесь мы объясняем каждую важную строку и связь с переменными окружения, которые подставляет Docker Compose при запуске внутри контейнеров.

Важно: эти YAML-файлы не используются монолитом. Их нужно вставить в соответствующие микросервисы.

---

## Общий принцип

- В YAML используем синтаксис `${VAR:default}`.
  - Если переменная окружения `VAR` задана (например, через Docker Compose), берётся её значение.
  - Если не задана — берётся `default` (обычно `localhost`, чтобы удобно запускать из IDE на хосте).
- В Docker контейнерах микросервисы НЕ должны стучаться на `localhost` — вместо этого используются имена сервисов Docker-сети (например, `kafka:29092`, `postgres-main`, `postgres-analytics`, `mongodb-analytics`, `mongodb-logging`). Compose прописывает их через переменные окружения.
- Профиль `local` включает эти настройки. В compose он задаётся `SPRING_PROFILES_ACTIVE=local`.

---

## 1) Authentication Service — `application-local.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: authentication-service

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/auth_db}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

jwt:
  secret: ${JWT_SECRET:change-me}
  expiration: 900000
  refresh-expiration: 604800000

cookie:
  secure: false
  max-age:
    access-token: 900
    refresh-token: 604800

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
  allowed-headers: "*"
  allow-credentials: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.painmanagement.auth: DEBUG
    org.springframework.security: INFO
```

Разбор ключевых строк:
- `server.port: 8082` — порт сервиса.
- `spring.datasource.url` — JDBC URL. Снаружи (IDE) по умолчанию `localhost:5432/auth_db`. В контейнере пересекается на `jdbc:postgresql://postgres-main:5432/auth_db` (через Compose env `SPRING_DATASOURCE_URL`).
- `jwt.secret` — секрет JWT. В контейнере передаётся через Compose как `JWT_SECRET`.
- `cors.allowed-origins` — разрешённый фронтенд (по умолчанию `http://localhost:5173`).

Связь с Compose (`authentication-service`):
- `SPRING_PROFILES_ACTIVE=local` — включает этот YAML.
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-main:5432/auth_db` — чтобы не использовать `localhost` внутри контейнера.
- `JWT_SECRET` — задаётся окружением (по умолчанию прописан `change-me-dev` в compose).

---

## 2) Analytics-Reporting Service — `application-local.yml`

```yaml
server:
  port: 8091

spring:
  application:
    name: analytics-reporting-service

  data:
    mongodb:
      uri: ${MONGODB_ANALYTICS_URI:mongodb://localhost:27017/analytics_db}
      auto-index-creation: true

  datasource:
    url: ${PG_JDBC_URL:jdbc:postgresql://localhost:5433/analytics_reporting}
    username: ${PG_USER:analytics}
    password: ${PG_PASSWORD:analytics}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: analytics-reporting-group
      auto-offset-reset: earliest
      enable-auto-commit: false

kafka:
  topics:
    analytics-events: ${KAFKA_TOPIC_ANALYTICS_EVENTS:analytics-events}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

Разбор ключевых строк:
- `spring.data.mongodb.uri` — Mongo для аналитики. В контейнере будет `mongodb://mongodb-analytics:27017/analytics_db` (через `MONGODB_ANALYTICS_URI`).
- `spring.datasource.url` — Postgres аналитики. В контейнере `jdbc:postgresql://postgres-analytics:5432/analytics_reporting` (через `PG_JDBC_URL`).
- `spring.kafka.bootstrap-servers` — Kafka. В контейнере `kafka:29092` (через `KAFKA_BOOTSTRAP_SERVERS`). На хосте — дефолт `localhost:9092`.
- `kafka.topics.analytics-events` — имя топика для аналитики.

Связь с Compose (`analytics-reporting-service`):
- `SPRING_PROFILES_ACTIVE=local`
- `KAFKA_BOOTSTRAP_SERVERS=kafka:29092`
- `PG_JDBC_URL=jdbc:postgresql://postgres-analytics:5432/analytics_reporting`
- `MONGODB_ANALYTICS_URI=mongodb://mongodb-analytics:27017/analytics_db`

---

## 3) Logging Service — `application-local.yml`

```yaml
spring:
  application:
    name: logging-service

  data:
    mongodb:
      uri: ${SPRING_DATA_MONGODB_URI:mongodb://localhost:27018/logging_db}
      auto-index-creation: true

  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: logging-service-group
      auto-offset-reset: earliest
      enable-auto-commit: false

kafka:
  topic:
    logging-events: ${KAFKA_TOPIC_LOGGING_EVENTS:logging-events}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    pain.management.logging: DEBUG
    org.springframework.kafka: INFO
    org.springframework.security: DEBUG
    org.mongodb: INFO
```

Разбор ключевых строк:
- `spring.data.mongodb.uri` — Mongo для логов. В контейнере: `mongodb://mongodb-logging:27017/logging_db` (через `SPRING_DATA_MONGODB_URI`).
- `spring.kafka.bootstrap-servers` — Kafka. В контейнере: `kafka:29092` (через `SPRING_KAFKA_BOOTSTRAP_SERVERS`). На хосте — дефолт `localhost:9092`.
- `kafka.topic.logging-events` — топик логирования.

Связь с Compose (`logging-service`):
- `SPRING_PROFILES_ACTIVE=local`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092`
- `SPRING_DATA_MONGODB_URI=mongodb://mongodb-logging:27017/logging_db`

---

## Практическое резюме

- При запуске с хоста (IDE): дефолты в YAML (`localhost:...`) работают без специальных переменных.
- При запуске в контейнерах (через единый compose): переменные окружения переопределяют дефолты на адреса сервисов docker-сети (имена сервисов).
- Профиль `local` обязателен для активации этих YAML.
