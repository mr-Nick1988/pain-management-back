# Локальная оркестрация микросервисов через Docker Compose

Цель: простой и логичный запуск/остановка инфраструктуры и микросервисов для разработки с минимальным потреблением ресурсов.

## Профили и состав

- infra
  - kafka (Confluent Kafka KRaft)
  - kafka-create-topics (одноразовая утилита для создания топиков)
- reporting
  - postgres-analytics (Postgres для отчётности)
  - reporting-service (микросервис отчётности)
- auth
  - authentication-service
- logging
  - mongo (MongoDB для логов)
  - logging-service
- ops
  - backup-restore (микросервис бэкапа/рестора)
- tools
  - kafdrop (UI для Kafka)

По умолчанию (без профилей) стартует только postgres-main — БД монолита для локальной разработки. Остальные сервисы стартуют только при указании соответствующего профиля.

## Быстрый старт (частые сценарии)

Предварительно создайте .env по образцу .env.example (см. корень проекта).

- Только Postgres монолита (минимальный старт)
```bash
# Запустит postgres-main
docker compose -f docker-compose.dev.yml up -d
```

- Инфраструктура Kafka + инструменты
```bash
# Kafka и утилита создания топиков
docker compose -f docker-compose.dev.yml --profile infra up -d kafka kafka-create-topics
# UI для Kafka (опционально)
docker compose -f docker-compose.dev.yml --profile tools up -d kafdrop
```

- Reporting стек (Kafka + Postgres analytics + сервис)
```bash
# ВАЖНО: нужен Kafka (infra) и reporting
docker compose -f docker-compose.dev.yml --profile infra --profile reporting up -d postgres-analytics reporting-service kafka-create-topics
```

- Authentication сервис
```bash
# Сервис использует Postgres на хосте (host.docker.internal:5432/auth_db)
docker compose -f docker-compose.dev.yml --profile auth up -d authentication-service
```

- Logging стек (Kafka + Mongo + сервис)
```bash
# Нужны профили infra и logging
docker compose -f docker-compose.dev.yml --profile infra --profile logging up -d mongo logging-service kafka-create-topics
```

- Backup & Restore
```bash
# Микросервис бэкапов (по умолчанию выключен, профиль ops)
docker compose -f docker-compose.dev.yml --profile ops up -d backup-restore
```

## Остановка и очистка

- Мягкая остановка (контейнеры остаются)
```bash
docker compose -f docker-compose.dev.yml stop
```

- Полная остановка и удаление контейнеров (данные в volumes сохраняются)
```bash
docker compose -f docker-compose.dev.yml down
```

- Удаление и volumes (ОСТОРОЖНО: удалит данные Postgres/Mongo)
```bash
docker compose -f docker-compose.dev.yml down -v
```

- Диагностика
```bash
# Список контейнеров в этом compose
docker compose -f docker-compose.dev.yml ps
# Логи сервиса
docker compose -f docker-compose.dev.yml logs -f reporting-service
```

- Очистка неиспользуемых образов и кэшей (для экономии места)
```bash
# Внимательно: удалит dangling ресурсы
docker system prune
# Более агрессивно (включая неиспользуемые образы)
# docker system prune -a
```

## Монолит: профили и конфигурация

- application.yml — базовая конфигурация (у вас только имя приложения)
- application-local.yml — профиль local для разработки:
  - server.port: 8080
  - Postgres: DB_URL/DB_USER/DB_PASSWORD (дефолт localhost:5432/pain_management_db)
  - JPA: ddl-auto=update
  - Mongo: LOGGING_MONGODB_URI (дефолт mongodb://localhost:27017/analyticsdb)

Запуск монолита локально (из IDE или CLI):
```bash
# Maven
mvn spring-boot:run -Dspring-boot.run.profiles=local
# или JAR
java -jar target/app.jar --spring.profiles.active=local
```

## Порты по умолчанию

- Postgres main: 5432 (localhost)
- Kafka: 9092 (localhost)
- Kafdrop: 9000
- Postgres analytics: 5433 (localhost)
- Authentication-service: 8082
- Reporting-service: 8091
- Mongo: 27017
- Logging-service: 8081 (проброшен на контейнерный 8083)
- Backup-restore: 8085

## Расшифровка ключевых переменных окружения

- Postgres main (монолит)
  - PG_AUTH_USER, PG_AUTH_PASSWORD — учётка postgres
- Reporting-service / postgres-analytics
  - PG_USER, PG_PASSWORD
- Authentication-service
  - SPRING_DATASOURCE_URL/USERNAME/PASSWORD — DSN к Postgres (по умолчанию хостовая БД auth_db)
  - JWT_SECRET — длинный случайный секрет для подписи JWT
  - CORS_ALLOWED_ORIGINS — фронтенд origin
- Logging-service
  - LOGGING_MONGODB_URI — строка подключения к MongoDB
  - KAFKA_TOPIC_LOGGING_EVENTS — топик логов (дефолт logging-events)
- Backup-restore
  - BACKUP_DS_URL/USER/PASS — JPA БД истории (Postgres)
  - PG_HOST/PORT/DB/USER/PASSWORD — целевой Postgres для бэкапов
  - MONGODB_BACKUP_URI — целевой Mongo для бэкапов
  - BACKUP_BASE_DIR, BACKUP_RETENTION_DAYS, BACKUP_SCHEDULER_ENABLED

## Облегчение образов и ресурсоёмкости

- Postgres: использованы alpine-образы (меньше размер)
- Kafka: оставлен Confluent (тяжёлый). Альтернатива — Redpanda или Bitnami Kafka (легче), можно заменить позже
- Java-сервисы: рекомендуется multi-stage Dockerfile с runtime `eclipse-temurin:21-jre-alpine` или Distroless. Также возможна сборка с Jib для меньших слоёв
- Добавьте `.dockerignore` для каждого микросервиса (исключить target/, .git/, docs/, *.md и т.п.)

## Как добавить новый микросервис

1) В docker-compose.dev.yml:
- Добавьте сервис с нужным профилем (например, `profiles: ["orders"]`)
- Опишите зависимости `depends_on`
- Используйте переменные из .env

2) Создайте документацию `docs/microservices/<service>-docs.md` с портами, зависимостями и примером запуска.

3) Проверьте, что локальный профиль Spring (`application-local.yml`) обслуживает переменные из окружения.

## Примечания к запуску профилей

- Если сервис зависит от Kafka, используйте одновременно профили сервиса и `infra`.
- Пример: для reporting — `--profile infra --profile reporting`.
- Для logging — `--profile infra --profile logging`.
