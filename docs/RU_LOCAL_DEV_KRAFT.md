# Полный гайд по локальному запуску (Kafka KRaft, без ZooKeeper)

Этот документ описывает, как запустить локально монолит и микросервисы с использованием единого Docker Compose и Kafka в режиме KRaft (без ZooKeeper). В документе есть пошаговая инструкция, архитектурные схемы, полная расшифровка конфигураций и точные файлы для вставки в микросервисы.

Сопутствующий подробный разбор каждого сервиса и каждой переменной в compose находится в файле: `docs/RU_DOCKER_COMPOSE_KRAFT_REFERENCE.md`.

---

## 1. Цели и принципы

- Один единый compose-файл в монолите: `docker-compose.dev.yml`.
- Kafka в режиме KRaft (без ZooKeeper).
- Два слушателя Kafka:
  - Для приложений на вашем компьютере (хост): `localhost:9092`.
  - Для контейнеров внутри Docker-сети: `kafka:29092`.
- Микросервисы поднимаются по профилям (`analytics`, `auth`, `logging`).
- Конфиги микросервисов формата `application-local.yml`, где значения читаются из переменных окружения (которые подставляет Docker Compose), а при запуске «с хоста» используются дефолты `localhost`.

---

## 2. Архитектура (высокоуровнево)

```mermaid
flowchart LR
    subgraph Host (Windows)
      IDE[IDE/Monolith 8080]
      KafUI[Kafdrop UI 9000]
    end

    subgraph Docker Compose: painmgmt-dev
      KFK[Kafka KRaft\n PLAINTEXT_HOST:9092\n PLAINTEXT:29092]
      PG_MAIN[(Postgres Main 5432)]
      PG_AN[(Postgres Analytics 5433)]
      MNG_AN[(Mongo Analytics 27017)]
      MNG_LOG[(Mongo Logging 27018)]

      subgraph Profiles
        AUTH[authentication-service 8082]
        ARS[analytics-reporting-service 8091]
        LOGS[logging-service]
      end
    end

    IDE -- bootstrap-servers=localhost:9092 --> KFK
    AUTH -- bootstrap-servers=kafka:29092 --> KFK
    ARS -- bootstrap-servers=kafka:29092 --> KFK
    LOGS -- bootstrap-servers=kafka:29092 --> KFK

    IDE --> PG_MAIN
    AUTH --> PG_MAIN
    ARS --> PG_AN
    ARS --> MNG_AN
    LOGS --> MNG_LOG

    KafUI --> KFK
```

---

## 3. Что делать со старыми docker-compose в микросервисах

1) В каталогах, откуда вы их поднимали, выполните остановку:
```
# если Docker Compose v2
docker compose down --remove-orphans

# если Docker Compose v1
docker-compose down --remove-orphans
```
2) В Docker Desktop удалите/остановите старые проекты.
3) Далее используем ТОЛЬКО `docker-compose.dev.yml` из монолита.

---

## 4. Быстрый старт: инфраструктура + микросервисы

Из корня репозитория монолита:

- Поднять инфраструктуру (Kafka KRaft, Postgres, Mongo, топики):
```
docker compose -f docker-compose.dev.yml up -d
```
- Поднять нужные микросервисы по профилям:
```
# Analytics Reporting (8091)
docker compose -f docker-compose.dev.yml --profile analytics up -d analytics-reporting-service

# Authentication (8082)
docker compose -f docker-compose.dev.yml --profile auth up -d authentication-service

# Logging Service
docker compose -f docker-compose.dev.yml --profile logging up -d logging-service

# Kafka UI (опционально) — http://localhost:9000
docker compose -f docker-compose.dev.yml --profile tools up -d kafdrop
```
- Монолит запускается из IDE/консоли на хосте (порт 8080) и смотрит на `localhost:9092` (Kafka) и `localhost:5432` (Postgres).

- Примечание по сборке микросервисов: в compose уже настроены пути `build:` — образы соберутся автоматически при первом запуске профилей. Используются пути:
  - Authentication: `C:/backend_projects/microservices/authentication-service`
  - Analytics-Reporting: `C:/backend_projects/microservices/analytics_reporting_service`
  - Logging: `C:/backend_projects/microservices/logging-service`
  При необходимости принудительно пересобрать образ: добавьте флаг `--build`, например:
  ```
  docker compose -f docker-compose.dev.yml --profile analytics up -d --build analytics-reporting-service
  ```
  Требуется наличие `Dockerfile` в указанных каталогах. Если его нет — дайте знать, добавлю корректный `Dockerfile`.

---

## 5. Конфигурация монолита (важные параметры)

Файл: `src/main/resources/application.properties`

- `spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`
  - По умолчанию — `localhost:9092` (хост-листенер Kafka).
- `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/pain_management_db}`
  - По умолчанию — локальный Postgres из compose (`postgres-main:5432`, проброшен на хост `localhost:5432`).
- `kafka.topic.analytics-events=${KAFKA_TOPIC_ANALYTICS_EVENTS:analytics-events}`
  - Топик создаётся автоматически контейнером `kafka-create-topics`.

Остальные свойства — по назначению (JPA, Mail, FHIR и т.д.). Монолит читает переменные окружения из ОС (если заданы), иначе берёт дефолт после двоеточия.

---

## 6. Готовые конфиги для микросервисов (вставить в их репозитории)

Сохраните в каждом микросервисе как `src/main/resources/application-local.yml` и запускайте сервис с профилем `local`.

### 6.1 Authentication Service (`application-local.yml`)
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

- В Docker Compose для auth уже проставлено `SPRING_PROFILES_ACTIVE=local` и JDBC на `postgres-main:5432/auth_db`.
- При запуске с хоста (без контейнера) берутся дефолты `localhost`.

### 6.2 Analytics-Reporting Service (`application-local.yml`)
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

- В Docker Compose для analytics сервис подключается к `kafka:29092`, Postgres `postgres-analytics:5432`, Mongo `mongodb-analytics:27017`.
- При запуске с хоста используются дефолты `localhost`.

### 6.3 Logging Service (`application-local.yml`)
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

- В Docker Compose для logging подключение к `kafka:29092` и `mongodb-logging:27017` уже пробрасывается через переменные окружения.

---

### 6.4 Шаблоны Dockerfile для микросервисов (скопировать в репозитории сервисов)

Шаблоны лежат в монолите:
- `docs/microservices-dockerfiles/Dockerfile.authentication-service`
- `docs/microservices-dockerfiles/Dockerfile.analytics-reporting-service`
- `docs/microservices-dockerfiles/Dockerfile.logging-service`

Скопируйте их в корни соответствующих микросервисов и переименуйте в `Dockerfile`:

```
# Authentication
# из корня монолита
copy .\docs\microservices-dockerfiles\Dockerfile.authentication-service C:\backend_projects\microservices\authentication-service\Dockerfile

# Analytics-Reporting
copy .\docs\microservices-dockerfiles\Dockerfile.analytics-reporting-service C:\backend_projects\microservices\analytics_reporting_service\Dockerfile

# Logging
copy .\docs\microservices-dockerfiles\Dockerfile.logging-service C:\backend_projects\microservices\logging-service\Dockerfile
```

После копирования можно сразу собирать/запускать профили с `--build`:
```
docker compose -f docker-compose.dev.yml --profile auth up -d --build authentication-service
docker compose -f docker-compose.dev.yml --profile analytics up -d --build analytics-reporting-service
docker compose -f docker-compose.dev.yml --profile logging up -d --build logging-service
```

Примечания:
- В Dockerfile используется Maven (Java 17). Если сервис на Gradle — сообщите, дам Gradle-вариант Dockerfile.
- В Logging Service при необходимости поправьте `EXPOSE` под фактический порт (если есть HTTP-эндпоинт). Для чисто потребителя Kafka EXPOSE не критичен.

---

## 7. Почему нужны переменные окружения в Docker и дефолты в YAML

- В контейнере сервис НЕ должен подключаться к `localhost`, потому что «localhost» внутри контейнера — это сам контейнер. Поэтому для контейнеров мы даём адреса сервисов Docker-сети (например, `kafka:29092`, `postgres-main`, `postgres-analytics`).
- При запуске на хосте из IDE удобно использовать `localhost`. Поэтому в `application-local.yml` указываются дефолты `localhost`, а Docker Compose переопределяет их переменными окружения при запуске внутри контейнеров.

---

## 8. Добавление нового микросервиса

1) В его репозитории создайте `src/main/resources/application-local.yml` по тому же принципу: дефолты — `localhost`, все ключевые параметры — через `${VAR:default}`.
2) В `docker-compose.dev.yml` добавьте блок сервиса с `profiles: ["<имя>"]` и окружением, указывающим адреса Docker-сервисов (`kafka:29092`, `postgres-...`, `mongodb-...`).
3) Запускайте: `docker compose -f docker-compose.dev.yml --profile <имя> up -d <service-name>`.

---

## 9. Частые проблемы и решения (FAQ)

- «Приложение в контейнере не видит Kafka по localhost:9092» — внутри контейнера используйте `kafka:29092`. В compose это уже задано переменными окружения.
- «Монолит не видит Postgres» — проверьте, что `postgres-main` слушает `localhost:5432`, а в `application.properties` URL по умолчанию — `jdbc:postgresql://localhost:5432/pain_management_db`.
- «Топиков нет» — контейнер `kafka-create-topics` создаёт `analytics-events` и `logging-events`. Повторите запуск compose или создайте топики командой вручную из контейнера Kafka.
- «Нужен UI для Kafka?» — профилем `tools` поднимите Kafdrop на http://localhost:9000. Если не нужен — удалите секцию `kafdrop` из compose.

- Kafka контейнер упал сразу после старта, в логах: `Running in KRaft mode... CLUSTER_ID is required`
  - Причина: для KRaft требуется `CLUSTER_ID` и каталог логов.
  - Исправление уже внесено в `docker-compose.dev.yml`: добавлены `CLUSTER_ID`, `KAFKA_LOG_DIRS` и `KAFKA_INTER_BROKER_LISTENER_NAME`. Также удалён устаревший ключ `version` (предупреждение про него можно игнорировать, сейчас его нет).
  - Действия:
    1) Обновите контейнер: 
       ```
       docker compose -f docker-compose.dev.yml up -d --force-recreate kafka
       ```
    2) Проверьте логи:
       ```
       docker logs dev_kafka --tail=200
       ```
    3) После успешного старта Kafka автоматически запустится `kafka-create-topics` (если не запускался ранее), либо запустите его:
       ```
       docker compose -f docker-compose.dev.yml up -d kafka-create-topics
       ```

---

## 9.1. Как собираются Docker-образы микросервисов

В `docker-compose.dev.yml` для микросервисов уже настроены `build:` контексты по вашим путям. Compose автоматически соберёт образы при первом запуске соответствующих профилей.

Если хотите собирать вручную (альтернатива):
```
# Примерные команды (укажите реальные пути)
docker build -t authentication-service:dev C:\path\to\authentication-service
docker build -t analytics-reporting-service:dev C:\path\to\analytics-reporting-service
docker build -t logging-service:dev C:\path\to\logging-service
```

Принудительная пересборка через compose:
```
docker compose -f docker-compose.dev.yml build authentication-service
docker compose -f docker-compose.dev.yml up -d --build authentication-service
```

Важно: в каталогах микросервисов должен быть `Dockerfile` (Maven/Gradle сборка выполняется внутри Dockerfile).

---

## 9.2. Логи и диагностика микросервисов

Посмотреть логи конкретного сервиса (следить в реальном времени):
```
docker compose -f docker-compose.dev.yml logs -f authentication-service
docker compose -f docker-compose.dev.yml logs -f analytics-reporting-service
docker compose -f docker-compose.dev.yml logs -f logging-service
```

Показать состояние всех контейнеров стека:
```
docker compose -f docker-compose.dev.yml ps
```

Перезапуск сервиса:
```
docker compose -f docker-compose.dev.yml restart authentication-service
```

Остановить и удалить контейнер сервиса (без удаления образа):
```
docker compose -f docker-compose.dev.yml stop authentication-service
docker compose -f docker-compose.dev.yml rm -f authentication-service
```

---

## 10. Что можно удалить, если хотите минимальный набор

- `docker/postgres-main/init.sql` — если не хотите автосоздание БД `auth_db`.
- `docs/microservices-config-examples/` — если не хотите держать заготовки YAML в монолите (в этом документе уже есть финальные версии для вставки).
- Секцию `kafdrop` из `docker-compose.dev.yml` — если UI не нужен.

---

## 11. Где посмотреть детальную расшифровку всего compose

См. файл `docs/RU_DOCKER_COMPOSE_KRAFT_REFERENCE.md` — там помодульно и построчно разобраны `kafka`, `kafka-create-topics`, `postgres-main`, `postgres-analytics`, `mongodb-analytics`, `mongodb-logging`, а также блоки микросервисов и все переменные окружения.

---

## 12. Завершение работы и очистка (профессионально)

Остановить только микросервисы:
```
docker compose -f docker-compose.dev.yml stop authentication-service analytics-reporting-service logging-service
```

Удалить контейнеры микросервисов (образы и данные БД не трогать):
```
docker compose -f docker-compose.dev.yml rm -f authentication-service analytics-reporting-service logging-service
```

Остановить всю инфраструктуру, сохранить данные Postgres/Mongo (тома остаются):
```
docker compose -f docker-compose.dev.yml down --remove-orphans
```

Полный сброс (удалить контейнеры, сети и тома — потеряете данные БД/Mongo):
```
docker compose -f docker-compose.dev.yml down -v --remove-orphans
```

Удалить только образы микросервисов (если надо пересобрать «с нуля»):
```
docker image rm authentication-service:dev analytics-reporting-service:dev logging-service:dev
```

Очистить кеш сборщика Docker (безопасно):
```
docker builder prune -f
```
