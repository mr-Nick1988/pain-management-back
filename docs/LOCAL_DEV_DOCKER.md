# Local Docker Dev Setup (Unified Compose)

This repo now contains a unified `docker-compose.dev.yml` to run all local infrastructure and (optionally) microservices in Kafka KRaft mode (no ZooKeeper).

- File: `docker-compose.dev.yml`
- Env vars: copy `.env.example` to `.env` and adjust secrets
- Profiles: `reporting`, `auth`, `logging`, `tools`

## What this stack runs (KRaft)

- Kafka (KRaft single-node) with dual listeners:
  - Internal for containers: `kafka:29092`
  - Host access for apps on your machine: `localhost:9092`
- PostgreSQL:
  - `postgres-main` for Monolith + Authentication (port 5432)
  - `postgres-analytics` for Analytics Reporting (port 5433)
- MongoDB:
  - `mongodb-analytics` (port 27017)
  - `mongodb-logging` (port 27018)
- Optional tools:
  - Kafdrop (Kafka UI) on http://localhost:9000 (enable with `--profile tools`)

## Start infrastructure only

```bash
docker compose -f docker-compose.dev.yml up -d
```
(Starts everything including DBs and Mongo; microservices are profile-gated and won't start unless you enable them.)

## Start optional microservices

- Reporting Service:
```bash
docker compose -f docker-compose.dev.yml --profile reporting up -d reporting-service
```
- Authentication:
```bash
docker compose -f docker-compose.dev.yml --profile auth up -d authentication-service
```
- Logging Service:
```bash
docker compose -f docker-compose.dev.yml --profile logging up -d logging-service
```
- Tools (Kafdrop):
```bash
docker compose -f docker-compose.dev.yml --profile tools up -d kafdrop
```

## Monolith configuration

Monolith runs on host (port 8080) and connects to Docker infra via host ports.

Key properties in `src/main/resources/application.properties`:

- `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/pain_management_db}`
- `spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`
- `kafka.topic.analytics-events=${KAFKA_TOPIC_ANALYTICS_EVENTS:analytics-events}`

Adjust `.env` in repo root to override defaults:

```env
DB_URL=jdbc:postgresql://localhost:5432/pain_management_db
DB_USER=postgres
DB_PASSWORD=postgres
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_ANALYTICS_EVENTS=analytics-events
JWT_SECRET=change-me
ANALYTICS_REPORTING_BASE_URL=http://localhost:8091
```


Each microservice should have an `application-local.yml` (or use env placeholders) so it works both:
- Inside Docker: use service names (`kafka:29092`, `postgres-main`, `mongodb-analytics`)
- On host: default to `localhost` ports

Copy-paste these exact configs into your microservices as `application-local.yml` and use profile `local`:

Authentication Service:
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
```

Reporting Service:
```yaml
server:
  port: 8091

spring:
  application:
    name: reporting-service

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
      group-id: ${SPRING_KAFKA_CONSUMER_GROUP_ID:reporting-service-group}
      auto-offset-reset: earliest
      enable-auto-commit: false

kafka:
  topics:
    reporting-commands: ${KAFKA_TOPIC_REPORTING_COMMANDS:reporting-commands}
```

Logging Service:
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

## Why Docker Desktop showed multiple Kafka/Compose projects

Likely you previously started Kafka from different compose files (e.g., one from a microservice repo and one from here). Docker Desktop groups containers by Compose "project" name (defaults to directory). With this unified compose (`name: painmgmt-dev`) you can:
- Stop old stacks: remove/stop older compose projects in Docker Desktop or with CLI from the directories they were started

## Adding a new microservice

1. Build a local image (or set `image:` to a registry image) in its repo.
2. Add a service block under `docker-compose.dev.yml` with a new `profiles: ["<name>"]`.
3. Use env placeholders in the microservice config similar to provided examples.
4. Start it with `--profile <name>`.

## Topics

`kafka-create-topics` job ensures `analytics-events`, `logging-events` and `reporting-commands` exist (in KRaft via `kafka:29092`). Add new topics by editing that container entrypoint.

## KRaft (no ZooKeeper)

Stack uses KRaft (controller quorum) and removes ZooKeeper entirely. Kafka advertises two listeners: `PLAINTEXT_HOST://localhost:9092` for host apps and `PLAINTEXT://kafka:29092` for containers.
