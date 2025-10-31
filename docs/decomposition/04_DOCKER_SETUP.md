# 04 - Docker Setup

**Предыдущий:** [03_INFRASTRUCTURE_OVERVIEW.md](03_INFRASTRUCTURE_OVERVIEW.md)  
**Следующий:** [05_EUREKA_SERVER.md](05_EUREKA_SERVER.md)

---

## 🐳 Структура проекта

```
pain-management-microservices/
├── docker-compose.yml              # Главный файл
├── docker-compose.dev.yml          # Development
├── docker-compose.prod.yml         # Production
├── .env                            # Environment variables
├── infrastructure/
│   ├── eureka-server/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   ├── config-server/
│   ├── api-gateway/
│   ├── mongodb/
│   │   ├── init-logging.js
│   │   └── init-analytics.js
│   ├── postgresql/
│   │   └── init-multiple-databases.sh
│   └── prometheus/
│       └── prometheus.yml
└── services/
    ├── logging-service/
    ├── analytics-service/
    └── ...
```

---

## 📄 docker-compose.yml

Создайте файл `docker-compose.yml` в корне проекта:

```yaml
version: '3.8'

services:
  # ============================================
  # Zookeeper - Kafka dependency
  # ============================================
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - microservices-network
    volumes:
      - zookeeper-data:/var/lib/zookeeper/data
      - zookeeper-logs:/var/lib/zookeeper/log

  # ============================================
  # Apache Kafka
  # ============================================
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    depends_on:
      - zookeeper
    networks:
      - microservices-network
    volumes:
      - kafka-data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5

  # ============================================
  # Redis
  # ============================================
  redis:
    image: redis:7.2-alpine
    container_name: redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes --requirepass redis_password
    networks:
      - microservices-network
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 5

  # ============================================
  # MongoDB - Logging
  # ============================================
  mongodb-logging:
    image: mongo:7.0
    container_name: mongodb-logging
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin_password
      MONGO_INITDB_DATABASE: logging_db
    networks:
      - microservices-network
    volumes:
      - mongodb-logging-data:/data/db
      - ./infrastructure/mongodb/init-logging.js:/docker-entrypoint-initdb.d/init.js:ro
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 30s
      timeout: 10s
      retries: 5

  # ============================================
  # MongoDB - Analytics
  # ============================================
  mongodb-analytics:
    image: mongo:7.0
    container_name: mongodb-analytics
    ports:
      - "27018:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin_password
      MONGO_INITDB_DATABASE: analytics_db
    networks:
      - microservices-network
    volumes:
      - mongodb-analytics-data:/data/db
      - ./infrastructure/mongodb/init-analytics.js:/docker-entrypoint-initdb.d/init.js:ro
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 30s
      timeout: 10s
      retries: 5

  # ============================================
  # PostgreSQL
  # ============================================
  postgresql:
    image: postgres:16-alpine
    container_name: postgresql
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres_password
      POSTGRES_DB: postgres
    networks:
      - microservices-network
    volumes:
      - postgresql-data:/var/lib/postgresql/data
      - ./infrastructure/postgresql/init-multiple-databases.sh:/docker-entrypoint-initdb.d/init.sh:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 30s
      timeout: 10s
      retries: 5

  # ============================================
  # InfluxDB
  # ============================================
  influxdb:
    image: influxdb:2.7-alpine
    container_name: influxdb
    ports:
      - "8086:8086"
    environment:
      DOCKER_INFLUXDB_INIT_MODE: setup
      DOCKER_INFLUXDB_INIT_USERNAME: admin
      DOCKER_INFLUXDB_INIT_PASSWORD: admin_password
      DOCKER_INFLUXDB_INIT_ORG: pain-management
      DOCKER_INFLUXDB_INIT_BUCKET: performance_metrics
      DOCKER_INFLUXDB_INIT_ADMIN_TOKEN: my-super-secret-auth-token
    networks:
      - microservices-network
    volumes:
      - influxdb-data:/var/lib/influxdb2

  # ============================================
  # MinIO
  # ============================================
  minio:
    image: minio/minio:latest
    container_name: minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
    networks:
      - microservices-network
    volumes:
      - minio-data:/data

  # ============================================
  # Kafka UI
  # ============================================
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
    depends_on:
      - kafka
    networks:
      - microservices-network

  # ============================================
  # Prometheus
  # ============================================
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - microservices-network
    volumes:
      - ./infrastructure/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus

  # ============================================
  # Grafana
  # ============================================
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin
    depends_on:
      - prometheus
    networks:
      - microservices-network
    volumes:
      - grafana-data:/var/lib/grafana

  # ============================================
  # Zipkin
  # ============================================
  zipkin:
    image: openzipkin/zipkin:latest
    container_name: zipkin
    ports:
      - "9411:9411"
    networks:
      - microservices-network
    environment:
      STORAGE_TYPE: mem

# ============================================
# Networks
# ============================================
networks:
  microservices-network:
    driver: bridge

# ============================================
# Volumes
# ============================================
volumes:
  zookeeper-data:
  zookeeper-logs:
  kafka-data:
  redis-data:
  mongodb-logging-data:
  mongodb-analytics-data:
  postgresql-data:
  influxdb-data:
  minio-data:
  prometheus-data:
  grafana-data:
```

---

## 🔧 Вспомогательные скрипты

### infrastructure/mongodb/init-logging.js

```javascript
db = db.getSiblingDB('logging_db');

// Create collections
db.createCollection('log_entries');
db.createCollection('audit_trail');

// Create indexes for log_entries
db.log_entries.createIndex({ "timestamp": -1 });
db.log_entries.createIndex({ "serviceName": 1 });
db.log_entries.createIndex({ "userId": 1 });
db.log_entries.createIndex({ "operation": 1 });
db.log_entries.createIndex({ "logLevel": 1 });
db.log_entries.createIndex({ "traceId": 1 });

// Create indexes for audit_trail
db.audit_trail.createIndex({ "timestamp": -1 });
db.audit_trail.createIndex({ "userId": 1 });
db.audit_trail.createIndex({ "action": 1 });
db.audit_trail.createIndex({ "entityType": 1 });
db.audit_trail.createIndex({ "serviceName": 1 });

print('Logging database initialized successfully');
```

### infrastructure/mongodb/init-analytics.js

```javascript
db = db.getSiblingDB('analytics_db');

// Create collections
db.createCollection('events');
db.createCollection('statistics');
db.createCollection('aggregations');

// Create indexes
db.events.createIndex({ "timestamp": -1 });
db.events.createIndex({ "eventType": 1 });
db.events.createIndex({ "userId": 1 });

db.statistics.createIndex({ "date": -1 });
db.statistics.createIndex({ "metricName": 1 });

print('Analytics database initialized successfully');
```

### infrastructure/postgresql/init-multiple-databases.sh

```bash
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE core_db;
    CREATE DATABASE pain_escalation_db;
    CREATE DATABASE reporting_db;
    
    GRANT ALL PRIVILEGES ON DATABASE core_db TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE pain_escalation_db TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE reporting_db TO postgres;
EOSQL

echo "Multiple databases created successfully"
```

---

## 🚀 Команды запуска

### Запуск всей инфраструктуры

```bash
# Запустить все сервисы
docker-compose up -d

# Посмотреть логи
docker-compose logs -f

# Посмотреть статус
docker-compose ps
```

### Запуск по группам

```bash
# Только базы данных
docker-compose up -d zookeeper kafka redis mongodb-logging mongodb-analytics postgresql influxdb minio

# Только мониторинг
docker-compose up -d prometheus grafana zipkin kafka-ui
```

### Остановка

```bash
# Остановить все
docker-compose down

# Остановить и удалить volumes
docker-compose down -v
```

---

## 🔍 Проверка работоспособности

### Скрипт проверки

Создайте файл `check-infrastructure.sh`:

```bash
#!/bin/bash

echo "Checking infrastructure..."

# Kafka
echo -n "Kafka: "
docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1 && echo "✅ OK" || echo "❌ FAIL"

# Redis
echo -n "Redis: "
docker exec redis redis-cli ping > /dev/null 2>&1 && echo "✅ OK" || echo "❌ FAIL"

# MongoDB Logging
echo -n "MongoDB Logging: "
docker exec mongodb-logging mongosh --quiet --eval "db.adminCommand('ping')" > /dev/null 2>&1 && echo "✅ OK" || echo "❌ FAIL"

# MongoDB Analytics
echo -n "MongoDB Analytics: "
docker exec mongodb-analytics mongosh --quiet --eval "db.adminCommand('ping')" > /dev/null 2>&1 && echo "✅ OK" || echo "❌ FAIL"

# PostgreSQL
echo -n "PostgreSQL: "
docker exec postgresql pg_isready -U postgres > /dev/null 2>&1 && echo "✅ OK" || echo "❌ FAIL"

echo "Infrastructure check complete!"
```

Запуск:

```bash
chmod +x check-infrastructure.sh
./check-infrastructure.sh
```

---

## 📊 Мониторинг ресурсов

```bash
# Использование ресурсов всеми контейнерами
docker stats

# Использование дискового пространства
docker system df

# Очистка неиспользуемых ресурсов
docker system prune -a
```

---

**Следующий шаг:** [05_EUREKA_SERVER.md](05_EUREKA_SERVER.md)
