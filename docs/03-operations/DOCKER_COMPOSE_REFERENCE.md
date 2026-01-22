# 🐋 Docker Compose - Complete Reference Guide

**Version:** 3.0  
**Last Updated:** January 12, 2026  
**File:** `docker-compose.dev.yml`

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [File Structure](#file-structure)
3. [Profiles Deep Dive](#profiles-deep-dive)
4. [Service Configurations](#service-configurations)
5. [Command Reference](#command-reference)
6. [Advanced Usage](#advanced-usage)
7. [Configuration Validation](#configuration-validation)
8. [Performance Optimization](#performance-optimization)
9. [Troubleshooting](#troubleshooting)

---

## Overview

### Compose File Version

```yaml
name: painmgmt-dev
```

The compose project is named `painmgmt-dev`, which prefixes all container names and resources.

### Architecture Components

| Component Type | Count | Purpose |
|----------------|-------|---------|
| Infrastructure | 2 | Kafka, PostgreSQL (main) |
| Additional DBs | 1 | PostgreSQL (analytics) |
| Microservices | 7 | Business logic services |
| Operational | 1 | Backup/Restore |
| Tools | 1 | Kafdrop (Kafka UI) |
| Monitoring | 2 | Prometheus, Grafana |
| **Total** | **14** | **All services** |

---

## File Structure

### Services Section

```yaml
services:
  kafka:           # Event streaming platform
  postgres:        # Main database
  postgres-analytics: # Reporting database
  auth-service:    # JWT authentication
  emr-service:     # EMR integration
  notification-service: # Notifications
  pain-escalation-service: # Pain tracking
  external-vas-service: # VAS devices
  reporting-service: # Analytics
  backup-restore:  # Backup operations
  kafdrop:         # Kafka UI
  prometheus:      # Metrics collection
  grafana:         # Visualization
```

### Networks Section

```yaml
networks:
  dev-net:
    driver: bridge
    name: painmgmt-dev-network
```

Single bridge network for all services.

### Volumes Section

```yaml
volumes:
  postgres-data:           # Main PostgreSQL data
  postgres-analytics-data: # Analytics PostgreSQL data
  prometheus-data:         # Prometheus metrics
  grafana-data:           # Grafana dashboards
```

Named volumes for data persistence.

---

## Profiles Deep Dive

### Profile System

Docker Compose profiles allow selective service startup. Services without a profile start by default.

### Default Services (No Profile Required)

```yaml
kafka:        # Always available
postgres:     # Always available
```

These start with:
```bash
docker-compose up -d
```

### Profile Mapping

| Profile | Services | Use Case |
|---------|----------|----------|
| `all` | All services | Full system |
| `core` | auth, emr, notification, escalation, reporting | Essential services |
| `auth` | auth-service | Authentication development |
| `emr` | emr-service | EMR development |
| `notification` | notification-service | Notification development |
| `escalation` | pain-escalation-service | Pain tracking development |
| `vas` | external-vas-service | VAS integration development |
| `reporting` | reporting-service, postgres-analytics | Reporting development |
| `ops` | backup-restore | Operations/backup |
| `tools` | kafdrop | Development tools |
| `monitoring` | prometheus, grafana | Observability |

### Profile Combinations

#### Multiple Profiles
```bash
# Core services + tools
docker-compose --profile core --profile tools up -d

# Core + monitoring
docker-compose --profile core --profile monitoring up -d

# Everything
docker-compose --profile all --profile monitoring --profile tools up -d
```

#### Custom Profile Workflows

**Backend Developer:**
```bash
docker-compose --profile core --profile tools up -d
```
Gets: Infrastructure + essential services + Kafdrop

**DevOps Engineer:**
```bash
docker-compose --profile all --profile monitoring up -d
```
Gets: Everything + Prometheus + Grafana

**QA Tester:**
```bash
docker-compose --profile all up -d
```
Gets: All microservices for integration testing

---

## Service Configurations

### 1. Kafka (KRaft Mode)

```yaml
kafka:
  image: confluentinc/cp-kafka:7.6.1
  container_name: dev_kafka
  hostname: kafka
  ports:
    - "9092:9092"
  environment:
    # KRaft configuration (no Zookeeper)
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    
    # Listeners
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,PLAINTEXT_HOST://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,CONTROLLER:PLAINTEXT
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    
    # Performance (dev environment)
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    
    KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
  healthcheck:
    test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
    interval: 10s
    timeout: 5s
    retries: 10
    start_period: 30s
  networks:
    - dev-net
```

**Key Points:**
- **KRaft Mode:** No Zookeeper dependency
- **Dual Listeners:** Docker network (29092) + Host (9092)
- **Auto-create Topics:** Enabled for dev convenience
- **Health Check:** Validates broker availability
- **Replication Factor:** 1 (single broker for dev)

**Accessing Kafka:**

From Docker containers:
```bash
kafka:29092
```

From host machine:
```bash
localhost:9092
```

**Common Operations:**

```bash
# List topics
docker exec dev_kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic
docker exec dev_kafka kafka-topics \
  --create \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# Describe topic
docker exec dev_kafka kafka-topics \
  --describe \
  --topic analytics-events \
  --bootstrap-server localhost:9092

# Consume messages
docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic pain.escalated \
  --from-beginning

# Produce message
echo "test message" | docker exec -i dev_kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic analytics-events

# Consumer groups
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list

# Consumer lag
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group my-consumer-group
```

---

### 2. PostgreSQL (Main Database)

```yaml
postgres:
  image: postgres:16-alpine
  container_name: dev_postgres
  hostname: postgres
  ports:
    - "5432:5432"
  environment:
    POSTGRES_DB: pain_management_db
    POSTGRES_USER: ${POSTGRES_USER:-postgres}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    POSTGRES_INITDB_ARGS: "--encoding=UTF8 --lc-collate=C --lc-ctype=C"
  volumes:
    - postgres-data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres -d pain_management_db"]
    interval: 10s
    timeout: 5s
    retries: 5
  networks:
    - dev-net
```

**Key Points:**
- **Alpine Image:** ~80MB vs ~150MB standard
- **Environment Variables:** Configurable via .env
- **Persistent Storage:** Named volume `postgres-data`
- **Health Check:** Validates database availability
- **UTF8 Encoding:** Standard character set

**Accessing PostgreSQL:**

```bash
# psql shell
docker exec -it dev_postgres psql -U postgres -d pain_management_db

# One-off query
docker exec dev_postgres psql -U postgres -d pain_management_db -c "SELECT version();"

# From host (using psql installed locally)
psql -h localhost -p 5432 -U postgres -d pain_management_db
```

**Database Operations:**

```bash
# List databases
docker exec dev_postgres psql -U postgres -c "\l"

# List tables
docker exec dev_postgres psql -U postgres -d pain_management_db -c "\dt"

# Describe table
docker exec dev_postgres psql -U postgres -d pain_management_db -c "\d patients"

# Query data
docker exec dev_postgres psql -U postgres -d pain_management_db -c "SELECT * FROM patients LIMIT 5;"

# Backup database
docker exec dev_postgres pg_dump -U postgres pain_management_db > backup.sql

# Restore database
cat backup.sql | docker exec -i dev_postgres psql -U postgres -d pain_management_db

# Create new database
docker exec dev_postgres psql -U postgres -c "CREATE DATABASE test_db;"

# Drop database
docker exec dev_postgres psql -U postgres -c "DROP DATABASE test_db;"
```

---

### 3. PostgreSQL Analytics (Reporting Database)

```yaml
postgres-analytics:
  image: postgres:16-alpine
  container_name: dev_postgres_analytics
  hostname: postgres-analytics
  profiles: ["reporting", "all", "core"]
  ports:
    - "5433:5432"
  environment:
    POSTGRES_DB: analytics_reporting
    POSTGRES_USER: ${POSTGRES_ANALYTICS_USER:-analytics}
    POSTGRES_PASSWORD: ${POSTGRES_ANALYTICS_PASSWORD:-analytics}
  volumes:
    - postgres-analytics-data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U analytics -d analytics_reporting"]
    interval: 10s
    timeout: 5s
    retries: 5
  networks:
    - dev-net
```

**Key Points:**
- **Separate Port:** 5433 (external), 5432 (internal)
- **Profile-based:** Only starts with specific profiles
- **Dedicated Volume:** Isolated from main database
- **FDW Ready:** Can connect to main database

**Foreign Data Wrapper Setup:**

```sql
-- Connect to analytics database
docker exec -it dev_postgres_analytics psql -U analytics -d analytics_reporting

-- Install postgres_fdw extension
CREATE EXTENSION IF NOT EXISTS postgres_fdw;

-- Create foreign server (to host postgres)
CREATE SERVER auth_db_server
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS (host 'host.docker.internal', port '5432', dbname 'auth_db');

-- Create user mapping
CREATE USER MAPPING FOR analytics
SERVER auth_db_server
OPTIONS (user 'postgres', password 'newpassword');

-- Create foreign table
CREATE FOREIGN TABLE auth_login_events (
  id BIGSERIAL,
  person_id VARCHAR(50),
  login_time TIMESTAMP,
  success BOOLEAN
)
SERVER auth_db_server
OPTIONS (schema_name 'public', table_name 'auth_login_events');

-- Query foreign data
SELECT * FROM auth_login_events LIMIT 10;
```

---

### 4. Authentication Service

```yaml
auth-service:
  build:
    context: C:/backend_projects/microservices/authentication-service
    dockerfile: Dockerfile
  image: pain-mgmt/auth-service:dev
  container_name: dev_auth
  profiles: ["auth", "all", "core"]
  ports:
    - "8082:8082"
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/auth_db
    SPRING_DATASOURCE_USERNAME: ${AUTH_DB_USER:-postgres}
    SPRING_DATASOURCE_PASSWORD: ${AUTH_DB_PASSWORD:-newpassword}
    JWT_SECRET: ${JWT_SECRET:-pain-management-secret-key...}
    CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:5173}
  depends_on:
    - postgres
  networks:
    - dev-net
```

**Key Points:**
- **Custom Build:** Built from local source
- **Host Database:** Connects to PostgreSQL on host machine
- **JWT Secret:** Must match monolith!
- **CORS:** Configurable for frontend
- **Dependency:** Waits for postgres

**Rebuild Service:**

```bash
# Rebuild image
docker-compose build auth-service

# Rebuild without cache
docker-compose build --no-cache auth-service

# Rebuild and restart
docker-compose up -d --build auth-service
```

---

### 5. EMR Integration Service

```yaml
emr-service:
  build:
    context: C:/backend_projects/microservices/emr-integration-service
    dockerfile: Dockerfile
  image: pain-mgmt/emr-service:dev
  container_name: dev_emr
  profiles: ["emr", "all", "core"]
  ports:
    - "8086:8086"
  environment:
    DB_URL: jdbc:postgresql://postgres:5432/emr_integration_db
    DB_USER: ${POSTGRES_USER:-postgres}
    DB_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    FHIR_SERVER_URL: ${FHIR_SERVER_URL:-http://hapi.fhir.org/baseR4}
    FHIR_CONNECTION_TIMEOUT: 10000
    FHIR_SOCKET_TIMEOUT: 10000
    EMR_SYNC_ENABLED: ${EMR_SYNC_ENABLED:-false}
  depends_on:
    kafka:
      condition: service_healthy
    postgres:
      condition: service_healthy
  networks:
    - dev-net
```

**Key Points:**
- **Internal Database:** Uses postgres container
- **FHIR Integration:** Connects to external FHIR server
- **Kafka Producer:** Publishes emr.changes, emr.created
- **Conditional Dependencies:** Waits for healthy services
- **Sync Control:** EMR_SYNC_ENABLED for scheduled sync

**Environment Variables Explained:**

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | postgres:5432/emr_integration_db | Database connection |
| `KAFKA_BOOTSTRAP_SERVERS` | kafka:29092 | Kafka broker (Docker network) |
| `FHIR_SERVER_URL` | http://hapi.fhir.org/baseR4 | External FHIR server |
| `FHIR_CONNECTION_TIMEOUT` | 10000 | Connection timeout (ms) |
| `FHIR_SOCKET_TIMEOUT` | 10000 | Socket timeout (ms) |
| `EMR_SYNC_ENABLED` | false | Auto-sync scheduler |

---

### 6. Notification Service

```yaml
notification-service:
  build:
    context: C:/backend_projects/microservices/notification-service
    dockerfile: Dockerfile
  image: pain-mgmt/notification-service:dev
  container_name: dev_notification
  profiles: ["notification", "all", "core"]
  ports:
    - "8087:8087"
  environment:
    DB_URL: jdbc:postgresql://postgres:5432/notification_db
    DB_USER: ${POSTGRES_USER:-postgres}
    DB_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    MAIL_HOST: ${MAIL_HOST:-localhost}
    MAIL_PORT: ${MAIL_PORT:-1025}
    MAIL_USERNAME: ${MAIL_USERNAME:-}
    MAIL_PASSWORD: ${MAIL_PASSWORD:-}
  depends_on:
    kafka:
      condition: service_healthy
    postgres:
      condition: service_healthy
  networks:
    - dev-net
```

**Email Configuration:**

For local development, use MailHog:

```bash
# Start MailHog
docker run -d -p 1025:1025 -p 8025:8025 --name mailhog mailhog/mailhog

# Update .env
MAIL_HOST=localhost
MAIL_PORT=1025

# Access web UI
http://localhost:8025
```

For production SMTP:

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

---

### 7. Pain Escalation Service

```yaml
pain-escalation-service:
  build:
    context: C:/backend_projects/microservices/pain-escalation-service
    dockerfile: Dockerfile
  image: pain-mgmt/pain-escalation:dev
  container_name: dev_pain_escalation
  profiles: ["escalation", "all", "core"]
  ports:
    - "8088:8088"
  environment:
    DB_URL: jdbc:postgresql://postgres:5432/pain_escalation_db
    DB_USER: ${POSTGRES_USER:-postgres}
    DB_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    PAIN_ESCALATION_THRESHOLD: 2
    PAIN_ANALYSIS_WINDOW: 24
    PAIN_CRITICAL_VAS: 7
  depends_on:
    kafka:
      condition: service_healthy
    postgres:
      condition: service_healthy
  networks:
    - dev-net
```

**Business Logic Configuration:**

| Variable | Default | Description |
|----------|---------|-------------|
| `PAIN_ESCALATION_THRESHOLD` | 2 | VAS increase to trigger escalation |
| `PAIN_ANALYSIS_WINDOW` | 24 | Analysis window (hours) |
| `PAIN_CRITICAL_VAS` | 7 | Critical pain threshold |

**Example:**
- Patient VAS: 3 → 5 → 8 (within 24h)
- Increase: 5 points (> threshold of 2)
- Last VAS: 8 (> critical of 7)
- **Result:** Escalation triggered + Critical alert

---

### 8. External VAS Service

```yaml
external-vas-service:
  build:
    context: C:/backend_projects/microservices/external-vas-integration-service
    dockerfile: Dockerfile
  image: pain-mgmt/external-vas:dev
  container_name: dev_external_vas
  profiles: ["vas", "all"]
  ports:
    - "8089:8089"
  environment:
    DB_URL: jdbc:postgresql://postgres:5432/external_vas_db
    DB_USER: ${POSTGRES_USER:-postgres}
    DB_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    VAS_AUTO_RECOMMENDATION: 4
    VAS_HIGH_PAIN: 7
    API_KEY_LENGTH: 64
    API_KEY_DEFAULT_RATE_LIMIT: 100
  depends_on:
    kafka:
      condition: service_healthy
    postgres:
      condition: service_healthy
  networks:
    - dev-net
```

**API Key Configuration:**

| Variable | Default | Purpose |
|----------|---------|---------|
| `API_KEY_LENGTH` | 64 | Generated key length |
| `API_KEY_DEFAULT_RATE_LIMIT` | 100 | Requests per minute |
| `VAS_AUTO_RECOMMENDATION` | 4 | VAS level for auto-recommendation |
| `VAS_HIGH_PAIN` | 7 | High pain threshold |

---

### 9. Reporting Service

```yaml
reporting-service:
  build:
    context: C:/backend_projects/microservices/reporting_service
    dockerfile: Dockerfile
  image: pain-mgmt/reporting:dev
  container_name: dev_reporting
  profiles: ["reporting", "all", "core"]
  ports:
    - "8091:8091"
  environment:
    PG_JDBC_URL: jdbc:postgresql://postgres-analytics:5432/analytics_reporting
    PG_USER: ${POSTGRES_ANALYTICS_USER:-analytics}
    PG_PASSWORD: ${POSTGRES_ANALYTICS_PASSWORD:-analytics}
    KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    KAFKA_TOPIC_REPORTING_COMMANDS: reporting-commands
  depends_on:
    kafka:
      condition: service_healthy
    postgres-analytics:
      condition: service_healthy
  networks:
    - dev-net
```

**Key Points:**
- Uses separate analytics database
- Kafka consumer for reporting commands
- Dependent on postgres-analytics

---

### 10. Backup & Restore Service

```yaml
backup-restore:
  build:
    context: C:/backend_projects/microservices/backup_restore
    dockerfile: Dockerfile
  image: pain-mgmt/backup-restore:dev
  container_name: dev_backup
  profiles: ["ops", "all"]
  ports:
    - "8085:8085"
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/backup_service
    SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-postgres}
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    PG_HOST: postgres
    PG_PORT: 5432
    PG_DB: pain_management_db
    PG_USER: ${POSTGRES_USER:-postgres}
    PG_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
    BACKUP_BASE_DIR: /app/backups
    BACKUP_RETENTION_DAYS: 30
    BACKUP_SCHEDULER_ENABLED: ${BACKUP_SCHEDULER_ENABLED:-false}
  volumes:
    - ./backups:/app/backups
  depends_on:
    - postgres
  networks:
    - dev-net
```

**Backup Configuration:**

| Variable | Default | Purpose |
|----------|---------|---------|
| `BACKUP_BASE_DIR` | /app/backups | Backup storage location |
| `BACKUP_RETENTION_DAYS` | 30 | Days to keep backups |
| `BACKUP_SCHEDULER_ENABLED` | false | Auto-backup scheduler |

**Volume Mount:**
- Host: `./backups` (relative to compose file)
- Container: `/app/backups`
- Backups accessible on host filesystem

---

### 11. Kafdrop (Kafka UI)

```yaml
kafdrop:
  image: obsidiandynamics/kafdrop:latest
  container_name: dev_kafdrop
  profiles: ["tools", "all"]
  ports:
    - "9000:9000"
  environment:
    KAFKA_BROKERCONNECT: kafka:29092
    JVM_OPTS: "-Xms32M -Xmx64M"
  depends_on:
    kafka:
      condition: service_healthy
  networks:
    - dev-net
```

**Usage:**
1. Start: `docker-compose --profile tools up -d`
2. Access: http://localhost:9000
3. Browse topics, messages, consumer groups

**Features:**
- Topic browser with message viewing
- Consumer group monitoring
- Broker and partition information
- Message search and filtering

---

### 12. Prometheus (Metrics Collection)

```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: dev_prometheus
  profiles: ["monitoring", "all"]
  ports:
    - "9090:9090"
  volumes:
    - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus-data:/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--web.console.libraries=/usr/share/prometheus/console_libraries'
    - '--web.console.templates=/usr/share/prometheus/consoles'
    - '--web.enable-lifecycle'
  networks:
    - dev-net
```

**Configuration File:** `monitoring/prometheus.yml`

**Key Features:**
- Scrapes `/actuator/prometheus` from all services
- 15-second scrape interval
- Time-series database storage
- Web UI for queries

**Access:** http://localhost:9090

**Useful Queries:**
```promql
# CPU usage
process_cpu_usage{application="pain-management-monolith"}

# Memory
jvm_memory_used_bytes{application="auth-service"}

# HTTP requests
http_server_requests_seconds_count{uri="/api/auth/login"}

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

---

### 13. Grafana (Visualization)

```yaml
grafana:
  image: grafana/grafana:latest
  container_name: dev_grafana
  profiles: ["monitoring", "all"]
  ports:
    - "3000:3000"
  environment:
    GF_SECURITY_ADMIN_USER: ${GRAFANA_ADMIN_USER:-admin}
    GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD:-admin}
    GF_ANALYTICS_REPORTING_ENABLED: "false"
    GF_ANALYTICS_CHECK_FOR_UPDATES: "false"
    GF_LOG_LEVEL: info
  volumes:
    - grafana-data:/var/lib/grafana
    - ./monitoring/grafana/provisioning:/etc/grafana/provisioning
  depends_on:
    - prometheus
  networks:
    - dev-net
```

**Setup:**
1. Access: http://localhost:3000
2. Login: admin/admin (change on first login)
3. Add Prometheus datasource: http://prometheus:9090
4. Import dashboards

**Auto-provisioning:**
Place datasource/dashboard configs in `monitoring/grafana/provisioning/`

---

## Command Reference

### Basic Commands

#### Start Services
```bash
# Default (infrastructure only)
docker-compose up

# Detached mode
docker-compose up -d

# With profile
docker-compose --profile core up -d

# Multiple profiles
docker-compose --profile core --profile tools up -d

# Specific service
docker-compose up -d kafka

# With build
docker-compose up -d --build

# Force recreate
docker-compose up -d --force-recreate
```

#### Stop Services
```bash
# Stop all
docker-compose stop

# Stop specific service
docker-compose stop kafka

# Stop with timeout
docker-compose stop -t 30

# Kill (force stop)
docker-compose kill
```

#### Remove Services
```bash
# Stop and remove
docker-compose down

# Remove with volumes (⚠️ DATA LOSS)
docker-compose down -v

# Remove with images
docker-compose down --rmi all

# Remove orphans
docker-compose down --remove-orphans
```

#### Restart Services
```bash
# Restart all
docker-compose restart

# Restart specific
docker-compose restart kafka

# Restart with timeout
docker-compose restart -t 30 kafka
```

### Build Commands

```bash
# Build all services
docker-compose build

# Build specific service
docker-compose build auth-service

# Build without cache
docker-compose build --no-cache

# Build with profile
docker-compose --profile all build

# Parallel build
docker-compose build --parallel

# Pull latest base images before build
docker-compose build --pull
```

### View Commands

```bash
# List containers
docker-compose ps

# List all (including stopped)
docker-compose ps -a

# View logs
docker-compose logs

# Follow logs
docker-compose logs -f

# Service-specific logs
docker-compose logs auth-service

# Last N lines
docker-compose logs --tail=100 kafka

# Since timestamp
docker-compose logs --since="2026-01-12T10:00:00"

# With timestamps
docker-compose logs -t
```

### Execute Commands

```bash
# Interactive shell
docker-compose exec kafka bash
docker-compose exec postgres bash

# One-off command
docker-compose exec postgres psql -U postgres

# As specific user
docker-compose exec -u postgres postgres bash

# Run new container
docker-compose run --rm kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Configuration Commands

```bash
# Validate compose file
docker-compose config

# View resolved configuration
docker-compose config --services

# View environment variables
docker-compose config --env

# Convert to JSON
docker-compose config --format json
```

### Advanced Commands

```bash
# Scale service (not applicable with container_name)
docker-compose up -d --scale worker=3

# Pull latest images
docker-compose pull

# Pull specific service
docker-compose pull kafka

# Push images to registry
docker-compose push

# View events
docker-compose events

# View top processes
docker-compose top

# Pause services
docker-compose pause

# Unpause services
docker-compose unpause

# Create services without starting
docker-compose create
```

---

## Advanced Usage

### Custom Environment Files

```bash
# Use custom env file
docker-compose --env-file .env.production up -d

# Multiple env files (later overrides earlier)
docker-compose --env-file .env --env-file .env.local up -d
```

### Override Compose Files

```bash
# Use override file
docker-compose -f docker-compose.dev.yml -f docker-compose.override.yml up -d

# Production config
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Example `docker-compose.override.yml`:
```yaml
services:
  kafka:
    environment:
      KAFKA_HEAP_OPTS: "-Xmx2G"
  
  auth-service:
    environment:
      SPRING_PROFILES_ACTIVE: debug
    ports:
      - "5005:5005"  # Debug port
```

### Project Name Override

```bash
# Custom project name
docker-compose -p my-project up -d

# Results in containers: my-project_kafka_1, my-project_postgres_1, etc.
```

### Selective Service Start

```bash
# Start infrastructure + specific services
docker-compose up -d kafka postgres auth-service emr-service
```

### Health Check Waiting

```bash
# Wait for healthy services
docker-compose up -d --wait

# With timeout
docker-compose up -d --wait --wait-timeout 60
```

---

## Configuration Validation

### Validate Syntax

```bash
docker-compose config
```

If valid, outputs resolved configuration. If invalid, shows errors.

### Check Service Dependencies

```bash
docker-compose config --services
```

Lists all services.

### Verify Environment Variables

```bash
docker-compose config | grep -A 5 "environment:"
```

Shows resolved environment variables.

### Test Service Startup

```bash
# Start without detaching to see errors
docker-compose up kafka

# Check exit codes
echo $?  # 0 = success, non-zero = failure
```

---

## Performance Optimization

### Resource Limits

Add to service configuration:

```yaml
auth-service:
  deploy:
    resources:
      limits:
        cpus: '0.5'
        memory: 512M
      reservations:
        memory: 256M
```

### Java Memory Tuning

```yaml
auth-service:
  environment:
    JAVA_OPTS: "-Xmx1024m -Xms256m -XX:+UseG1GC"
```

### Kafka Performance

```yaml
kafka:
  environment:
    KAFKA_HEAP_OPTS: "-Xmx2G -Xms2G"
    KAFKA_NUM_NETWORK_THREADS: 8
    KAFKA_NUM_IO_THREADS: 8
```

### PostgreSQL Tuning

```yaml
postgres:
  command:
    - "postgres"
    - "-c"
    - "shared_buffers=256MB"
    - "-c"
    - "effective_cache_size=1GB"
    - "-c"
    - "max_connections=200"
```

---

## Troubleshooting

### Service Won't Start

```bash
# View detailed logs
docker-compose logs service-name

# Check service status
docker-compose ps

# Inspect container
docker inspect dev_service_name

# Check health
docker inspect dev_service_name | grep -A 10 Health
```

### Port Already in Use

```bash
# Find what's using port
netstat -ano | findstr :9092

# Kill process (Windows)
taskkill /PID <PID> /F

# Change port in compose file
ports:
  - "9093:9092"  # Use different host port
```

### Volume Permission Issues

```bash
# Fix permissions
docker-compose run --rm --user root service-name chown -R spring:spring /app/data
```

### Network Issues

```bash
# Recreate network
docker-compose down
docker network rm painmgmt-dev-network
docker-compose up -d

# Check network
docker network inspect painmgmt-dev-network
```

### Build Failures

```bash
# Clean build
docker-compose build --no-cache --pull

# Check Dockerfile
docker-compose config | grep -A 20 "auth-service:"

# Manual build
cd C:/backend_projects/microservices/authentication-service
docker build -t pain-mgmt/auth-service:dev .
```

### Health Check Failures

```bash
# Check health check command
docker-compose config | grep -A 5 "healthcheck:"

# Test health check manually
docker exec dev_kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Increase timeout
healthcheck:
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 60s
```

---

## Best Practices

### 1. Use .env for Secrets
Never commit secrets to git:
```bash
echo ".env" >> .gitignore
cp .env.example .env
# Edit .env with real values
```

### 2. Pin Image Versions
```yaml
# Bad
image: postgres:latest

# Good
image: postgres:16-alpine
```

### 3. Use Health Checks
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### 4. Proper Dependencies
```yaml
depends_on:
  kafka:
    condition: service_healthy  # Wait for healthy
  postgres:
    condition: service_healthy
```

### 5. Named Volumes for Persistence
```yaml
volumes:
  - postgres-data:/var/lib/postgresql/data  # Named volume
  - ./backups:/app/backups  # Bind mount for access
```

### 6. Resource Limits for Production
```yaml
deploy:
  resources:
    limits:
      memory: 1G
    reservations:
      memory: 512M
```

### 7. Logging Configuration
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

---

## Quick Reference Card

| Task | Command |
|------|---------|
| Start infrastructure | `docker-compose up -d` |
| Start everything | `docker-compose --profile all up -d` |
| Start core services | `docker-compose --profile core up -d` |
| Start with tools | `docker-compose --profile core --profile tools up -d` |
| Stop all | `docker-compose stop` |
| Remove all | `docker-compose down` |
| Remove with data | `docker-compose down -v` |
| View logs | `docker-compose logs -f` |
| View status | `docker-compose ps` |
| Rebuild service | `docker-compose build --no-cache service-name` |
| Restart service | `docker-compose restart service-name` |
| Execute command | `docker-compose exec service-name bash` |
| Validate config | `docker-compose config` |

---

**Next:** See [TESTING_GUIDE.md](TESTING_GUIDE.md) for comprehensive testing scenarios  
**See Also:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for detailed problem resolution

---

**Last Updated:** January 12, 2026  
**Version:** 3.0
