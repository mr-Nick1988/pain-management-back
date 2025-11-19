# Полная расшифровка docker-compose.dev.yml (Kafka KRaft)

Ниже построчная расшифровка ключевых частей `docker-compose.dev.yml`, чтобы понимать каждую настройку и взаимосвязь сервисов.

## Заголовок

```yaml
name: painmgmt-dev
```
- `name`: имя проекта Compose. В Docker Desktop все контейнеры будут сгруппированы под этим именем. Ключ `version` в новых версиях Compose не обязателен и нами не используется.

## Сервис: kafka (KRaft)

```yaml
kafka:
  image: confluentinc/cp-kafka:7.6.1
  container_name: dev_kafka
  ports:
    - "9092:9092"   # host access (PLAINTEXT_HOST)
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,PLAINTEXT_HOST://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,CONTROLLER:PLAINTEXT
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
    CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
  healthcheck:
    test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
    interval: 10s
    timeout: 5s
    retries: 10
  networks:
    - dev-net
```

- `ports: 9092:9092` — внешний порт для приложений, работающих на хосте (IDE, curl и т.п.).
- `KAFKA_NODE_ID` — ID ноды.
- `KAFKA_PROCESS_ROLES` — роли процесса: брокер и контроллер находятся на одном экземпляре (одиночный кластер для локалки).
- `KAFKA_LISTENERS` — три слушателя:
  - `PLAINTEXT://0.0.0.0:29092` — для доступа из других контейнеров в docker-сети (внутренний порт).
  - `PLAINTEXT_HOST://0.0.0.0:9092` — для доступа с хоста (ваша ОС).
  - `CONTROLLER://0.0.0.0:9093` — канал для контроллера KRaft.
- `KAFKA_ADVERTISED_LISTENERS` — как Kafka объявляет свои адреса клиентам:
  - `PLAINTEXT://kafka:29092` — адрес внутри docker-сети (имя сервиса `kafka`).
  - `PLAINTEXT_HOST://localhost:9092` — адрес для хоста.
- `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP` — сопоставление имён слушателей протоколам.
- `KAFKA_CONTROLLER_LISTENER_NAMES` — listener для контроллера.
- `KAFKA_CONTROLLER_QUORUM_VOTERS` — кворум контроллеров (одна нода: `1@kafka:9093`).
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` и настройки логов — тюнинг для одиночного кластера.
- `healthcheck` — проверка готовности брокера.

## Сервис: kafka-create-topics

```yaml
kafka-create-topics:
  image: confluentinc/cp-kafka:7.6.1
  depends_on:
    kafka:
      condition: service_healthy
  entrypoint: ["bash", "-lc", "\
    kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 --topic analytics-events --replication-factor 1 --partitions 1 && \
    kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 --topic logging-events --replication-factor 1 --partitions 1 && \
    kafka-topics --create --if-not-exists --bootstrap-server kafka:29092 --topic reporting-commands --replication-factor 1 --partitions 1 && \
    echo 'Kafka topics ensured' "]
  networks:
    - dev-net
```

- Создаёт топики `analytics-events` и `logging-events` при старте.
- `--bootstrap-server kafka:29092` — подключение к внутреннему слушателю Kafka.

## Postgres: postgres-main и postgres-analytics

```yaml
postgres-main:
  image: postgres:16
  container_name: dev_postgres_main
  environment:
    POSTGRES_DB: pain_management_db
    POSTGRES_USER: ${PG_AUTH_USER:-postgres}
    POSTGRES_PASSWORD: ${PG_AUTH_PASSWORD:-postgres}
  ports:
    - "5432:5432"
  volumes:
    - pg-main-data:/var/lib/postgresql/data
    - ./docker/postgres-main/init.sql:/docker-entrypoint-initdb.d/10-init.sql:ro
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
    interval: 10s
    timeout: 5s
    retries: 10
  networks:
    - dev-net
```

- Основной Postgres (`5432`) для монолита и Auth-сервиса.
- `init.sql` — создаёт БД `auth_db` при первом старте контейнера.
- Если не нужен — удалите строку монтирования скрипта и сам файл.

```yaml
postgres-analytics:
  image: postgres:16
  container_name: dev_postgres_analytics
  environment:
    POSTGRES_DB: analytics_reporting
    POSTGRES_USER: ${PG_USER:-analytics}
    POSTGRES_PASSWORD: ${PG_PASSWORD:-analytics}
  ports:
    - "5433:5432"
  volumes:
    - pg-analytics-data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
    interval: 10s
    timeout: 5s
    retries: 10
  networks:
    - dev-net
```

- Отдельная БД для сервиса Analytics-Reporting.

## MongoDB: mongodb-analytics и mongodb-logging

```yaml
mongodb-analytics:
  image: mongo:7.0
  container_name: dev_mongodb_analytics
  environment:
    MONGO_INITDB_DATABASE: analytics_db
  ports:
    - "27017:27017"
  volumes:
    - mongo-analytics-data:/data/db
  healthcheck:
    test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
    interval: 10s
    timeout: 5s
    retries: 10
  networks:
    - dev-net
```

- Mongo для аналитики.

```yaml
mongodb-logging:
  image: mongo:7.0
  container_name: dev_mongodb_logging
  environment:
    MONGO_INITDB_DATABASE: logging_db
  ports:
    - "27018:27017"
  volumes:
    - mongo-logging-data:/data/db
  healthcheck:
    test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
    interval: 10s
    timeout: 5s
    retries: 10
  networks:
    - dev-net
```

- Mongo для логгинга.

## Профильные сервисы (опционально)

### analytics-reporting-service

```yaml
analytics-reporting-service:
  image: analytics-reporting-service:dev
  build:
    context: C:/backend_projects/microservices/analytics_reporting_service
  container_name: dev_analytics_reporting
  profiles: ["analytics"]
  environment:
    SPRING_PROFILES_ACTIVE: local
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    KAFKA_TOPIC_ANALYTICS_EVENTS: ${KAFKA_TOPIC_ANALYTICS_EVENTS:-analytics-events}
    KAFKA_TOPIC_REPORTING_COMMANDS: ${KAFKA_TOPIC_REPORTING_COMMANDS:-reporting-commands}
    PG_JDBC_URL: jdbc:postgresql://postgres-analytics:5432/analytics_reporting
    PG_USER: ${PG_USER:-analytics}
    PG_PASSWORD: ${PG_PASSWORD:-analytics}
    MONGODB_ANALYTICS_URI: mongodb://mongodb-analytics:27017/analytics_db
  depends_on:
    kafka:
      condition: service_healthy
    postgres-analytics:
      condition: service_started
    mongodb-analytics:
      condition: service_started
  ports:
    - "8091:8091"
  networks:
    - dev-net
```

- В контейнере сервис читает Kafka по адресу `kafka:29092` и слушает топик команд `reporting-commands` (Monolith -> Reporting).
- Подключения к Postgres и Mongo — по именам сервисов в сети compose.
- Порт 8091 проброшен наружу для доступа с хоста.

### authentication-service

```yaml
authentication-service:
  image: authentication-service:dev
  build:
    context: C:/backend_projects/microservices/authentication-service
  container_name: dev_authentication
  profiles: ["auth"]
  environment:
    SPRING_PROFILES_ACTIVE: local
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-main:5432/auth_db
    SPRING_DATASOURCE_USERNAME: ${PG_AUTH_USER:-postgres}
    SPRING_DATASOURCE_PASSWORD: ${PG_AUTH_PASSWORD:-postgres}
    JWT_SECRET: ${JWT_SECRET}
    CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:5173}
  depends_on:
    postgres-main:
      condition: service_started
  ports:
    - "8082:8082"
  networks:
    - dev-net
```

- Использует `postgres-main`.
- JWT секрет берётся из переменной окружения (можно задать через Docker Desktop или PowerShell при запуске).

### logging-service

```yaml
logging-service:
  image: logging-service:dev
  build:
    context: C:/backend_projects/microservices/logging-service
  container_name: dev_logging
  profiles: ["logging"]
  environment:
    SPRING_PROFILES_ACTIVE: local
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    SPRING_DATA_MONGODB_URI: mongodb://mongodb-logging:27017/logging_db
    KAFKA_TOPIC_LOGGING_EVENTS: ${KAFKA_TOPIC_LOGGING_EVENTS:-logging-events}
  depends_on:
    kafka:
      condition: service_healthy
    mongodb-logging:
      condition: service_started
  networks:
    - dev-net
```

- Kafka и Mongo — по именам сервисов Docker-сети.

## Инструменты (опционально)

```yaml
kafdrop:
  image: obsidiandynamics/kafdrop:latest
  container_name: dev_kafdrop
  profiles: ["tools"]
  environment:
    KAFKA_BROKERCONNECT: kafka:29092
    JVM_OPTS: -Xms32M -Xmx64M
  depends_on:
    kafka:
      condition: service_healthy
  ports:
    - "9000:9000"
  networks:
    - dev-net
```

- Веб-интерфейс для просмотра топиков/партиций/сообщений Kafka. Не нужен — удалите блок.

## Сети и тома

```yaml
networks:
  dev-net:
    driver: bridge

volumes:
  pg-main-data:
  pg-analytics-data:
  mongo-analytics-data:
  mongo-logging-data:
```

- `bridge`-сеть для общения контейнеров по именам сервисов.
- Томы для персистентности данных БД и Mongo.
