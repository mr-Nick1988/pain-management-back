# 🚀 Pain Management Platform - Complete DevOps Guide

**Version:** 3.2  
**Last Updated:** January 23, 2026  
**Architecture:** Microservices with API Gateway

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Prerequisites](#prerequisites)
4. [Quick Start (5 Minutes)](#quick-start-5-minutes)
5. [Docker Compose Profiles Explained](#docker-compose-profiles-explained)
6. [Infrastructure Services](#infrastructure-services)
7. [Microservices](#microservices)
8. [Complete Startup Scenarios](#complete-startup-scenarios)
9. [Container Management](#container-management)
10. [Environment Variables](#environment-variables)
11. [Networking](#networking)
12. [Volumes and Data Persistence](#volumes-and-data-persistence)
13. [Health Checks and Monitoring](#health-checks-and-monitoring)
14. [Logs Management](#logs-management)
15. [Development Workflows](#development-workflows)
16. [CI/CD Integration](#cicd-integration)
17. [Performance Tuning](#performance-tuning)
18. [Security Best Practices](#security-best-practices)

---

## Overview

### System Components

The Pain Management Platform consists of:

1. **API Gateway** (port 8000) - Single entry point for all requests
2. **Monolith Application** (port 8080) - Core business logic
3. **7 Microservices** (ports 8082-8091) - Specialized functions
4. **Infrastructure**:
   - Apache Kafka (port 9092) - Event streaming
   - PostgreSQL x2 (ports 5432, 5433) - Databases
   - Prometheus (port 9090) - Metrics collection
   - Grafana (port 3000) - Visualization
   - Kafdrop (port 9000) - Kafka UI

### Key Technologies

- **Java:** 21
- **Spring Boot:** 3.2.x
- **Docker:** 20.10+
- **Docker Compose:** 2.0+
- **Kafka:** 7.6.1 (KRaft mode)
- **PostgreSQL:** 16 (Alpine)
- **Maven:** 3.9+

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND (port 5173)                     │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (port 8000)                       │
│  - Request Routing       - JWT Validation                        │
│  - Circuit Breaker       - CORS Management                       │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MONOLITH (port 8080)                          │
│  - Patient Management    - Treatment Protocols                   │
│  - VAS Recording         - Doctor/Nurse Workflows                │
└──────────────┬───────────────────────────────┬──────────────────┘
               │                               │
               ▼                               ▼
┌──────────────────────────┐      ┌──────────────────────────────┐
│   KAFKA (port 9092)      │      │  PostgreSQL (port 5432)      │
│   Event Streaming        │      │  Main Database               │
└────┬─────────────────────┘      └──────────────────────────────┘
     │
     │  Events: pain.escalated, vas.recorded, emr.changes, etc.
     │
     ├─────────────────────────────────────────────────────────────┐
     │                                                             │
     ▼                                                             ▼
┌─────────────────────────┐                         ┌─────────────────────────┐
│ Authentication Service  │                         │ EMR Integration Service │
│ (port 8082)             │                         │ (port 8086)             │
│ - JWT validation        │                         │ - FHIR R4 integration   │
│ - User management       │                         │ - External EMR sync     │
└─────────────────────────┘                         └─────────────────────────┘
     │                                                             │
     ▼                                                             ▼
┌─────────────────────────┐                         ┌─────────────────────────┐
│ Notification Service    │                         │ Pain Escalation Service │
│ (port 8087)             │                         │ (port 8088)             │
│ - Email notifications   │                         │ - Automatic tracking    │
│ - WebSocket push        │                         │ - Alert generation      │
└─────────────────────────┘                         └─────────────────────────┘
     │                                                             │
     ▼                                                             ▼
┌─────────────────────────┐                         ┌─────────────────────────┐
│ External VAS Service    │                         │ Reporting Service       │
│ (port 8089)             │                         │ (port 8091)             │
│ - VAS device integration│                         │ - Analytics & reports   │
│ - JSON/XML/CSV support  │                         │ - Business metrics      │
└─────────────────────────┘                         └─────────────────────────┘
     │                                                             │
     ▼                                                             ▼
┌─────────────────────────┐                         ┌─────────────────────────┐
│ Backup/Restore Service  │                         │ PostgreSQL Analytics    │
│ (port 8085)             │                         │ (port 5433)             │
│ - Database backups      │                         │ - Reporting database    │
│ - Restore operations    │                         │ - FDW to main DB        │
└─────────────────────────┘                         └─────────────────────────┘
```

---

## Prerequisites

### Required Software

1. **Java Development Kit 21**
   ```bash
   java -version
   # Expected: openjdk version "21.x.x"
   ```

2. **Maven 3.9+**
   ```bash
   mvn -version
   # Expected: Apache Maven 3.9.x
   ```

3. **Docker Desktop for Windows**
   - Version: 20.10+
   - WSL 2 backend enabled (recommended)
   - Minimum resources:
     - RAM: 8GB (4GB for Docker)
     - Disk: 20GB free space

4. **Git**
   ```bash
   git --version
   # Expected: git version 2.30+
   ```

### System Requirements

- **OS:** Windows 10/11, Linux, macOS
- **CPU:** 4+ cores recommended
- **RAM:** 16GB minimum (8GB for containers, 8GB for OS/IDE)
- **Disk:** 50GB free space (for images, volumes, builds)

### Network Ports

Ensure the following ports are available:

| Port  | Service                    | Required |
|-------|----------------------------|----------|
| 3000  | Grafana                    | Optional |
| 5432  | PostgreSQL (Main)          | ✅       |
| 5433  | PostgreSQL (Analytics)     | Optional |
| 8000  | API Gateway                | ✅       |
| 8080  | Monolith                   | ✅       |
| 8082  | Authentication Service     | ✅       |
| 8085  | Backup/Restore Service     | Optional |
| 8086  | EMR Integration Service    | ✅       |
| 8087  | Notification Service       | ✅       |
| 8088  | Pain Escalation Service    | ✅       |
| 8089  | External VAS Service       | Optional |
| 8091  | Reporting Service          | ✅       |
| 9000  | Kafdrop (Kafka UI)         | Optional |
| 9090  | Prometheus                 | Optional |
| 9092  | Kafka                      | ✅       |

---

## Quick Start (5 Minutes)

### Step 1: Clone and Navigate
```bash
cd C:\backend_projects\pain_managment_back
```

### Step 2: Create Environment File
```bash
# Copy example to .env
cp .env.example .env

# Edit .env if needed (optional for local dev)
```

### Step 3: Start Infrastructure
```bash
# Start Kafka + PostgreSQL
docker-compose up -d

# Wait for services to be ready (30-60 seconds)
docker-compose logs -f
```

### Step 4: Start Monolith
```bash
# Build and run
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Step 5: Verify
```bash
# Check monolith health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

**🎉 System is ready!**

---

## Docker Compose Profiles Explained

Docker Compose profiles allow selective service startup. This is crucial for:
- **Resource optimization** (don't run what you don't need)
- **Development focus** (work on specific services)
- **CI/CD pipelines** (test specific components)

### Available Profiles

| Profile      | Services Included                                      | Use Case                          |
|--------------|-------------------------------------------------------|-----------------------------------|
| `(none)`     | kafka, postgres                                       | Infrastructure only               |
| `all`        | All services                                          | Full system testing               |
| `core`       | auth, emr, notification, escalation, reporting        | Essential microservices           |
| `auth`       | authentication-service                                | Auth development/testing          |
| `emr`        | emr-integration-service                               | EMR development/testing           |
| `notification` | notification-service                                | Notification development          |
| `escalation` | pain-escalation-service                               | Pain tracking development         |
| `vas`        | external-vas-service                                  | VAS integration development       |
| `reporting`  | reporting-service, postgres-analytics                 | Reporting development             |
| `ops`        | backup-restore                                        | Operations/backup development     |
| `tools`      | kafdrop                                               | Development tools                 |
| `monitoring` | prometheus, grafana                                   | Observability                     |

### Profile Usage Examples

#### 1. Infrastructure Only (Default)
```bash
docker-compose up -d
```
**Starts:** Kafka, PostgreSQL  
**Use for:** Running monolith only

#### 2. Full System
```bash
docker-compose --profile all up -d
```
**Starts:** Everything  
**Use for:** Integration testing, demos

#### 3. Core Services
```bash
docker-compose --profile core up -d
```
**Starts:** Infrastructure + auth + emr + notification + escalation + reporting  
**Use for:** Standard development

#### 4. Specific Service Development
```bash
# Work on Authentication Service
docker-compose --profile auth up -d

# Work on EMR Integration
docker-compose --profile emr up -d

# Multiple profiles
docker-compose --profile auth --profile emr up -d
```

#### 5. With Development Tools
```bash
docker-compose --profile core --profile tools up -d
```
**Adds:** Kafdrop (Kafka UI) on http://localhost:9000

#### 6. With Monitoring
```bash
docker-compose --profile all --profile monitoring up -d
```
**Adds:** Prometheus (9090) + Grafana (3000)

---

## Infrastructure Services

### 1. Apache Kafka (KRaft Mode)

**Purpose:** Event streaming platform for asynchronous communication  
**Port:** 9092 (localhost), 29092 (Docker network)  
**Image:** confluentinc/cp-kafka:7.6.1

#### Start Kafka
```bash
docker-compose up -d kafka
```

#### Verify Kafka
```bash
# Check if running
docker ps | grep kafka

# Test broker connection
docker exec dev_kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# List topics
docker exec dev_kafka kafka-topics --list --bootstrap-server localhost:9092
```

#### Create Topics Manually (Optional)
```bash
# Kafka auto-creates topics, but you can create them explicitly:

docker exec dev_kafka kafka-topics \
  --create \
  --topic analytics-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec dev_kafka kafka-topics \
  --create \
  --topic pain.escalated \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

#### Kafka Topics Used

| Topic                    | Producer                  | Consumer                  | Purpose                           |
|--------------------------|---------------------------|---------------------------|-----------------------------------|
| `analytics-events`       | Monolith                  | Reporting Service         | Business analytics events         |
| `reporting-commands`     | Monolith                  | Reporting Service         | Report generation commands        |
| `emr.changes`            | EMR Integration Service   | Monolith, Notification    | EMR data changes                  |
| `emr.created`            | EMR Integration Service   | Monolith                  | New EMR records                   |
| `pain.escalated`         | Pain Escalation Service   | Notification Service      | Pain escalation alerts            |
| `dose.administered`      | Monolith                  | Pain Escalation Service   | Medication administration         |
| `vas.recorded`           | Monolith                  | Pain Escalation Service   | VAS recordings (internal)         |
| `vas.external.recorded`  | External VAS Service      | Pain Escalation Service   | VAS from external devices         |

#### View Messages in Topics
```bash
# Consume from beginning
docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic pain.escalated \
  --from-beginning

# Consume latest messages
docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic analytics-events
```

#### Kafka Configuration Details

- **No Zookeeper:** Uses KRaft mode (Kafka Raft consensus protocol)
- **Auto-create topics:** Enabled (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`)
- **Replication factor:** 1 (dev environment)
- **Listeners:**
  - `PLAINTEXT://kafka:29092` - Docker network
  - `PLAINTEXT_HOST://localhost:9092` - Host machine

---

### 2. PostgreSQL (Main Database)

**Purpose:** Primary relational database  
**Port:** 5432  
**Image:** postgres:16-alpine  
**Database:** pain_management_db

#### Start PostgreSQL
```bash
docker-compose up -d postgres
```

#### Verify PostgreSQL
```bash
# Check if running
docker exec dev_postgres pg_isready -U postgres

# Connect to database
docker exec -it dev_postgres psql -U postgres -d pain_management_db

# List tables
\dt

# Exit
\q
```

#### Database Connection from Host
```
Host:     localhost
Port:     5432
Database: pain_management_db
Username: postgres
Password: postgres (or from .env)
```

#### Common Database Operations
```bash
# View all databases
docker exec dev_postgres psql -U postgres -c "\l"

# View table structure
docker exec dev_postgres psql -U postgres -d pain_management_db -c "\d patients"

# Query data
docker exec dev_postgres psql -U postgres -d pain_management_db -c "SELECT * FROM patients LIMIT 10;"

# Backup database
docker exec dev_postgres pg_dump -U postgres pain_management_db > backup_$(date +%Y%m%d).sql

# Restore database
cat backup_20260112.sql | docker exec -i dev_postgres psql -U postgres -d pain_management_db
```

---

### 3. PostgreSQL Analytics (Reporting Database)

**Purpose:** Separate database for reporting/analytics  
**Port:** 5433  
**Image:** postgres:16-alpine  
**Database:** analytics_reporting  
**Profile:** `reporting`, `all`, `core`

#### Start Analytics Database
```bash
docker-compose --profile reporting up -d postgres-analytics
```

#### Connection Details
```
Host:     localhost
Port:     5433
Database: analytics_reporting
Username: analytics (or from .env)
Password: analytics (or from .env)
```

#### Foreign Data Wrapper (FDW)

The analytics database uses FDW to access main database:

```sql
-- Connect to analytics database
docker exec -it dev_postgres_analytics psql -U analytics -d analytics_reporting

-- FDW should be configured to access auth_db on host
-- Check foreign tables
SELECT * FROM information_schema.foreign_tables;
```

---

### 4. Kafdrop (Kafka UI)

**Purpose:** Web UI for Kafka monitoring  
**Port:** 9000  
**Profile:** `tools`, `all`  
**URL:** http://localhost:9000

#### Start Kafdrop
```bash
docker-compose --profile tools up -d kafdrop
```

#### Features
- Browse topics and partitions
- View messages (with search)
- Monitor consumer groups and lag
- View broker and cluster info

---

### 5. Prometheus (Metrics Collection)

**Purpose:** Metrics collection and time-series database  
**Port:** 9090  
**Profile:** `monitoring`, `all`  
**URL:** http://localhost:9090

#### Start Prometheus
```bash
docker-compose --profile monitoring up -d prometheus
```

#### Configuration
- Config file: `monitoring/prometheus.yml`
- Scrapes `/actuator/prometheus` from all services every 15s

#### Useful Queries
- CPU usage: `process_cpu_usage`
- Memory: `jvm_memory_used_bytes`
- HTTP requests: `http_server_requests_seconds_count`

---

### 6. Grafana (Visualization)

**Purpose:** Metrics visualization dashboards  
**Port:** 3000  
**Profile:** `monitoring`, `all`  
**URL:** http://localhost:3000  
**Credentials:** admin/admin (change on first login)

#### Start Grafana
```bash
docker-compose --profile monitoring up -d grafana
```

#### Setup
1. Open http://localhost:3000
2. Login with admin/admin
3. Add Prometheus data source: http://prometheus:9090
4. Import dashboards or create custom ones

---

## Microservices

### 1. Authentication Service (Port 8082)

**Purpose:** JWT authentication and user management  
**Profile:** `auth`, `core`, `all`  
**Database:** auth_db (on host PostgreSQL at 5432)

#### Start
```bash
docker-compose --profile auth up -d auth-service
```

#### Verify
```bash
curl http://localhost:8082/actuator/health
```

#### Key Endpoints
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/validate` - Token validation
- `GET /actuator/health` - Health check

#### Environment Variables
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/auth_db
JWT_SECRET: (must match monolith!)
CORS_ALLOWED_ORIGINS: http://localhost:5173
```

#### Testing
```bash
# Login
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123"}'
```

---

### 2. EMR Integration Service (Port 8086)

**Purpose:** External EMR integration via FHIR R4  
**Profile:** `emr`, `core`, `all`  
**Database:** emr_integration_db (on main PostgreSQL)

#### Start
```bash
docker-compose --profile emr up -d emr-service
```

#### Verify
```bash
curl http://localhost:8086/actuator/health
```

#### Key Features
- FHIR R4 patient sync
- External EMR webhook integration
- Kafka events: `emr.changes`, `emr.created`

#### Configuration
```yaml
FHIR_SERVER_URL: http://hapi.fhir.org/baseR4
EMR_SYNC_ENABLED: false (manual sync only in dev)
```

#### Testing
```bash
# Sync patient from FHIR
curl -X POST http://localhost:8086/api/emr/sync/patient/example \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3. Notification Service (Port 8087)

**Purpose:** Email, WebSocket, and push notifications  
**Profile:** `notification`, `core`, `all`  
**Database:** notification_db (on main PostgreSQL)

#### Start
```bash
docker-compose --profile notification up -d notification-service
```

#### Verify
```bash
curl http://localhost:8087/actuator/health
```

#### Features
- Email notifications (SMTP)
- WebSocket real-time push
- Kafka consumers: `pain.escalated`, `dose.administered`

#### Configuration
```yaml
MAIL_HOST: localhost (use MailHog for dev)
MAIL_PORT: 1025
```

#### Testing with MailHog (Optional)
```bash
# Start MailHog for email testing
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog

# WebUI: http://localhost:8025
```

---

### 4. Pain Escalation Service (Port 8088)

**Purpose:** Automatic pain escalation tracking and alerting  
**Profile:** `escalation`, `core`, `all`  
**Database:** pain_escalation_db (on main PostgreSQL)

#### Start
```bash
docker-compose --profile escalation up -d pain-escalation-service
```

#### Verify
```bash
curl http://localhost:8088/actuator/health
```

#### Key Features
- Automatic VAS trend analysis
- Escalation threshold detection
- Critical pain alerts
- Kafka producers: `pain.escalated`

#### Configuration
```yaml
PAIN_ESCALATION_THRESHOLD: 2  # VAS increase to trigger
PAIN_CRITICAL_VAS: 7           # Critical pain level
PAIN_ANALYSIS_WINDOW: 24       # Hours
```

#### Testing
```bash
# Get all escalations
curl http://localhost:8088/api/pain-escalation/escalations \
  -H "Authorization: Bearer $TOKEN"

# Get escalations by status
curl http://localhost:8088/api/pain-escalation/escalations/status/ACTIVE \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5. External VAS Integration Service (Port 8089)

**Purpose:** Integration with external VAS devices (JSON/XML/CSV)  
**Profile:** `vas`, `all`  
**Database:** external_vas_db (on main PostgreSQL)

#### Start
```bash
docker-compose --profile vas up -d external-vas-service
```

#### Verify
```bash
curl http://localhost:8089/api/external/vas/health
```

#### Features
- API key authentication
- Multi-format support (JSON, XML, CSV)
- Rate limiting per API key
- Batch import capabilities

#### Testing
```bash
# Create API key
curl -X POST http://localhost:8089/api/admin/api-keys?createdBy=admin \
  -H "Content-Type: application/json" \
  -d '{
    "systemName":"Test Device",
    "description":"Testing",
    "ipWhitelist":"*",
    "rateLimitPerMinute":100
  }'

# Record VAS (save apiKey from previous response)
curl -X POST http://localhost:8089/api/external/vas/record \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn":"MRN-001",
    "vasLevel":7,
    "deviceId":"DEV-001",
    "location":"Ward A"
  }'
```

---

### 6. Reporting Service (Port 8091)

**Purpose:** Analytics and report generation  
**Profile:** `reporting`, `core`, `all`  
**Database:** analytics_reporting (separate PostgreSQL on port 5433)

#### Start
```bash
docker-compose --profile reporting up -d reporting-service
```

#### Verify
```bash
curl http://localhost:8091/actuator/health
```

#### Features
- Business analytics reports
- FDW to access main database
- Kafka consumer: `reporting-commands`

---

### 7. Backup & Restore Service (Port 8085)

**Purpose:** Database backup and restore operations  
**Profile:** `ops`, `all`  
**Database:** backup_service (on main PostgreSQL)

#### Start
```bash
docker-compose --profile ops up -d backup-restore
```

#### Features
- PostgreSQL backup/restore
- Scheduled backups (configurable)
- Backup history tracking

#### Configuration
```yaml
BACKUP_BASE_DIR: /app/backups
BACKUP_RETENTION_DAYS: 30
BACKUP_SCHEDULER_ENABLED: false (manual in dev)
```

#### Testing
```bash
# Create backup
curl -X POST http://localhost:8085/api/backup/create \
  -H "Authorization: Bearer $TOKEN"

# List backups
curl http://localhost:8085/api/backup/list \
  -H "Authorization: Bearer $TOKEN"
```

---

## Complete Startup Scenarios

### Scenario 1: Minimal Setup (Monolith Only)

**Use Case:** Quick development, minimal resources  
**Services:** Kafka, PostgreSQL, Monolith

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Wait for readiness (30-60 seconds)
docker-compose ps

# 3. Start monolith
cd C:\backend_projects\pain_managment_back
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. Verify
curl http://localhost:8080/actuator/health
```

**What works:**
- ✅ Patient management
- ✅ VAS recording (internal)
- ✅ Treatment protocols
- ✅ Basic workflows
- ❌ JWT authentication
- ❌ Notifications
- ❌ Pain escalation

---

### Scenario 2: Standard Development Setup

**Use Case:** Normal development with essential services  
**Services:** Infrastructure + core microservices

```bash
# 1. Start infrastructure + core services
docker-compose --profile core up -d

# 2. Wait for all services (2-3 minutes)
docker-compose logs -f

# 3. Check all services are up
docker-compose ps

# 4. Start monolith
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 5. Verify all services
./scripts/verify-all.sh  # See script below
```

**What works:**
- ✅ Everything essential
- ✅ JWT authentication
- ✅ Notifications
- ✅ Pain escalation
- ✅ Reporting

---

### Scenario 3: Full System with Monitoring

**Use Case:** Integration testing, demos, performance analysis  
**Services:** Everything including monitoring tools

```bash
# 1. Start everything
docker-compose --profile all --profile monitoring --profile tools up -d

# 2. Wait for startup (3-5 minutes)
docker-compose logs -f

# 3. Start monolith
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. Access UIs
# - Kafdrop: http://localhost:9000
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3000
```

---

### Scenario 4: Specific Microservice Development

**Use Case:** Developing/debugging a specific microservice

#### Example: Working on EMR Integration Service

```bash
# 1. Start infrastructure + dependencies
docker-compose --profile core up -d

# 2. Stop the service you want to develop locally
docker-compose stop dev_emr

# 3. Run the microservice from IDE or Maven
cd C:\backend_projects\microservices\emr-integration-service
mvn spring-boot:run

# Now you can:
# - Set breakpoints in IDE
# - See console logs directly
# - Hot reload code changes
```

#### Example: Working on Authentication Service

```bash
# 1. Start only infrastructure
docker-compose up -d

# 2. Run auth service locally
cd C:\backend_projects\microservices\authentication-service
mvn spring-boot:run

# 3. Run monolith
cd C:\backend_projects\pain_managment_back
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

### Scenario 5: CI/CD Pipeline Simulation

**Use Case:** Testing the full pipeline locally

```bash
# 1. Clean everything
docker-compose --profile all down -v

# 2. Build all images from scratch
docker-compose --profile all build --no-cache

# 3. Start services
docker-compose --profile all up -d

# 4. Run integration tests
mvn verify -P integration-tests

# 5. Check logs for errors
docker-compose logs | grep -i error
```

---

## Container Management

### Starting Containers

#### Start with Default Profile (Infrastructure Only)
```bash
docker-compose up
```

#### Start in Detached Mode (Background)
```bash
docker-compose up -d
```

#### Start with Specific Profile
```bash
docker-compose --profile core up -d
```

#### Start Multiple Profiles
```bash
docker-compose --profile core --profile tools up -d
```

#### Start Specific Service
```bash
docker-compose up -d kafka
docker-compose up -d postgres
docker-compose up -d auth-service
```

#### Start with Build
```bash
# Build images before starting
docker-compose --profile all up -d --build

# Force rebuild (no cache)
docker-compose --profile all build --no-cache
docker-compose --profile all up -d
```

---

### Stopping Containers

#### Stop All Running Containers
```bash
docker-compose stop
```

#### Stop Specific Service
```bash
docker-compose stop auth-service
docker-compose stop kafka
```

#### Stop with Profile
```bash
docker-compose --profile all stop
```

#### Graceful Shutdown (Wait for Containers)
```bash
docker-compose down
```

#### Force Stop (Immediate)
```bash
docker-compose kill
```

---

### Removing Containers

#### Stop and Remove Containers
```bash
docker-compose down
```

#### Remove Containers and Networks
```bash
docker-compose down --remove-orphans
```

#### Remove Containers, Networks, and Volumes (⚠️ DATA LOSS!)
```bash
docker-compose down -v
```

#### Remove Containers, Networks, Volumes, and Images
```bash
docker-compose down -v --rmi all
```

#### Remove Specific Service
```bash
docker-compose rm -f auth-service
```

---

### Restarting Containers

#### Restart All
```bash
docker-compose restart
```

#### Restart Specific Service
```bash
docker-compose restart kafka
docker-compose restart postgres
```

#### Restart with New Environment Variables
```bash
# Edit .env file first
docker-compose up -d --force-recreate
```

---

### Viewing Container Status

#### List Running Containers
```bash
docker-compose ps
```

#### List All Containers (Including Stopped)
```bash
docker-compose ps -a
```

#### View Container Details
```bash
docker inspect dev_kafka
docker inspect dev_postgres
```

#### View Container Resource Usage
```bash
docker stats

# Specific container
docker stats dev_kafka
```

---

### Executing Commands in Containers

#### Interactive Shell
```bash
# Postgres
docker exec -it dev_postgres bash

# Kafka
docker exec -it dev_kafka bash
```

#### One-off Command
```bash
# Postgres query
docker exec dev_postgres psql -U postgres -d pain_management_db -c "SELECT COUNT(*) FROM patients;"

# Kafka topics
docker exec dev_kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

## Environment Variables

### Environment File (.env)

Create `.env` in project root:

```bash
# Copy from example
cp .env.example .env
```

### Critical Variables

#### Database Credentials
```ini
# Main PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Analytics PostgreSQL
POSTGRES_ANALYTICS_USER=analytics
POSTGRES_ANALYTICS_PASSWORD=analytics
```

#### Security
```ini
# JWT Secret (MUST be same across monolith and auth-service!)
JWT_SECRET=pain-management-secret-key-change-in-production-min-256-bits-required-for-hs256-algorithm-security

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

#### Email (for Notification Service)
```ini
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
```

#### Backup Configuration
```ini
BACKUP_BASE_DIR=/app/backups
BACKUP_RETENTION_DAYS=30
BACKUP_SCHEDULER_ENABLED=false
```

### Overriding Variables

#### Command Line Override
```bash
POSTGRES_PASSWORD=newpass docker-compose up -d
```

#### Multiple Variables
```bash
POSTGRES_PASSWORD=newpass KAFKA_HEAP_OPTS="-Xmx2G" docker-compose up -d
```

---

## Networking

### Network Overview

All services run in a custom bridge network: `painmgmt-dev-network`

```bash
# Inspect network
docker network inspect painmgmt-dev-network

# View connected containers
docker network inspect painmgmt-dev-network | grep -i name
```

### Service Discovery

Services communicate using container names:

- `kafka:29092` - Kafka broker (from within Docker network)
- `postgres:5432` - Main database
- `postgres-analytics:5432` - Analytics database
- `dev_auth:8082` - Auth service
- `dev_emr:8086` - EMR service
- etc.

### Host Access

From monolith running on host:

- `localhost:9092` - Kafka
- `localhost:5432` - PostgreSQL
- `localhost:8082` - Auth service
- etc.

### Container to Host

From containers to host machine:

- `host.docker.internal:5432` - Host PostgreSQL
- `host.docker.internal:8080` - Monolith on host

---

## Volumes and Data Persistence

### Named Volumes

| Volume                      | Purpose                          | Mount Point                     |
|-----------------------------|----------------------------------|---------------------------------|
| `painmgmt-postgres-data`    | Main PostgreSQL data             | `/var/lib/postgresql/data`      |
| `painmgmt-analytics-data`   | Analytics PostgreSQL data        | `/var/lib/postgresql/data`      |
| `painmgmt-prometheus-data`  | Prometheus metrics storage       | `/prometheus`                   |
| `painmgmt-grafana-data`     | Grafana dashboards & config      | `/var/lib/grafana`              |

### Bind Mounts

| Host Path           | Container Path                          | Purpose                   |
|---------------------|-----------------------------------------|---------------------------|
| `./backups`         | `/app/backups`                          | Backup files storage      |
| `./monitoring`      | `/etc/prometheus`                       | Prometheus config         |

### Volume Operations

#### List Volumes
```bash
docker volume ls
```

#### Inspect Volume
```bash
docker volume inspect painmgmt-postgres-data
```

#### Backup Volume
```bash
# Backup Postgres data
docker run --rm \
  -v painmgmt-postgres-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/postgres-backup-$(date +%Y%m%d).tar.gz /data
```

#### Restore Volume
```bash
# Stop containers first
docker-compose down

# Restore
docker run --rm \
  -v painmgmt-postgres-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/postgres-backup-20260112.tar.gz -C /

# Start containers
docker-compose up -d
```

#### Remove Volume (⚠️ DATA LOSS!)
```bash
docker volume rm painmgmt-postgres-data
```

#### Remove All Unused Volumes
```bash
docker volume prune
```

---

## Health Checks and Monitoring

### Built-in Health Checks

All services have Docker health checks:

```bash
# View health status
docker-compose ps

# Detailed health check
docker inspect dev_postgres | grep -A 10 Health
```

### Spring Boot Actuator

All microservices expose actuator endpoints:

#### Health Check
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8082/actuator/health
```

#### Detailed Health
```bash
curl http://localhost:8080/actuator/health | jq
```

#### Metrics (Prometheus Format)
```bash
curl http://localhost:8080/actuator/prometheus
```

#### Info Endpoint
```bash
curl http://localhost:8080/actuator/info
```

### Automated Health Check Script

Create `scripts/verify-all.sh`:

```bash
#!/bin/bash

echo "=== Checking Infrastructure ==="
curl -s http://localhost:8080/actuator/health | jq '.status'
curl -s http://localhost:9092 > /dev/null && echo "Kafka: UP" || echo "Kafka: DOWN"

echo ""
echo "=== Checking Microservices ==="
services=(8082 8085 8086 8087 8088 8089 8091)
names=("Auth" "Backup" "EMR" "Notification" "Escalation" "VAS" "Reporting")

for i in "${!services[@]}"; do
  port=${services[$i]}
  name=${names[$i]}
  status=$(curl -s http://localhost:$port/actuator/health | jq -r '.status' 2>/dev/null || echo "DOWN")
  echo "$name ($port): $status"
done
```

---

## Logs Management

### View Logs

#### All Services
```bash
docker-compose logs
```

#### Follow Logs (Tail -f)
```bash
docker-compose logs -f
```

#### Specific Service
```bash
docker-compose logs kafka
docker-compose logs auth-service
docker-compose logs -f notification-service
```

#### Last N Lines
```bash
docker-compose logs --tail=100 auth-service
```

#### Since Timestamp
```bash
docker-compose logs --since="2026-01-12T10:00:00" auth-service
```

#### With Timestamps
```bash
docker-compose logs -t auth-service
```

### Search Logs

```bash
# Search for errors
docker-compose logs | grep -i error

# Search for specific service
docker-compose logs | grep dev_auth

# Count errors
docker-compose logs | grep -i error | wc -l
```

### Export Logs

```bash
# Save all logs to file
docker-compose logs > logs_$(date +%Y%m%d_%H%M%S).txt

# Save specific service
docker-compose logs auth-service > auth_logs_$(date +%Y%m%d).txt
```

### Log Rotation

Docker automatically rotates logs. Configure in `/etc/docker/daemon.json`:

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

---

## Development Workflows

### Workflow 1: Hot Reload Development

#### Monolith Hot Reload
```bash
# Use spring-boot-devtools (already in pom.xml)
mvn spring-boot:run

# Changes to Java files auto-recompile
# Changes to resources auto-reload
```

#### Microservice Hot Reload
```bash
# Stop Docker container
docker-compose stop dev_auth

# Run locally with devtools
cd C:\backend_projects\microservices\authentication-service
mvn spring-boot:run

# Make changes, see them reflected immediately
```

---

### Workflow 2: Debug Mode

#### Debug Monolith in IDE
1. Open `PainHelperBackApplication.java`
2. Right-click → Debug
3. Set breakpoints
4. Make requests

#### Debug Microservice Remotely
```bash
# Start with debug port exposed
docker-compose -f docker-compose.debug.yml up -d auth-service

# In IDE, attach remote debugger to localhost:5005
```

---

### Workflow 3: Integration Testing

```bash
# Start test environment
docker-compose --profile all up -d

# Run integration tests
mvn verify -P integration-tests

# View test results
cat target/failsafe-reports/*.xml
```

---

### Workflow 4: Database Migrations

```bash
# Start infrastructure
docker-compose up -d postgres

# Run Liquibase/Flyway migrations
mvn liquibase:update

# Or through monolith startup (auto-migration)
mvn spring-boot:run
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Start infrastructure
      run: |
        docker-compose up -d kafka postgres
        sleep 30
    
    - name: Build monolith
      run: mvn clean install
    
    - name: Run tests
      run: mvn test
    
    - name: Build Docker images
      run: docker-compose --profile all build
    
    - name: Integration tests
      run: |
        docker-compose --profile all up -d
        mvn verify -P integration-tests
    
    - name: Cleanup
      run: docker-compose --profile all down -v
```

---

## Performance Tuning

### Docker Resources

#### Increase Container Memory
Edit `.env`:
```ini
JAVA_OPTS=-Xmx2048m -Xms512m
```

#### Kafka Performance
```yaml
environment:
  KAFKA_HEAP_OPTS: "-Xmx1G -Xms1G"
```

### Database Performance

#### PostgreSQL Tuning
```bash
docker exec dev_postgres psql -U postgres -c "ALTER SYSTEM SET shared_buffers = '256MB';"
docker exec dev_postgres psql -U postgres -c "ALTER SYSTEM SET effective_cache_size = '1GB';"
docker-compose restart postgres
```

### Monolith Performance

```bash
# Run with optimized JVM settings
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xmx4096m -Xms1024m -XX:+UseG1GC"
```

---

## Security Best Practices

### 1. Never Commit Secrets
```bash
# Ensure .env is in .gitignore
echo ".env" >> .gitignore
```

### 2. Use Strong JWT Secret
```ini
# Generate strong secret
JWT_SECRET=$(openssl rand -base64 64)
```

### 3. Limit Network Exposure
```yaml
# Don't expose unnecessary ports
# Remove ports: section for internal services
```

### 4. Use Read-Only Volumes
```yaml
volumes:
  - ./config:/app/config:ro
```

### 5. Run as Non-Root User
```dockerfile
USER spring:spring
```

---

## Verification Script

Create `scripts/verify-all.sh`:

```bash
#!/bin/bash

echo "=== Infrastructure Health Check ==="

# Kafka
if docker exec dev_kafka kafka-broker-api-versions --bootstrap-server localhost:9092 &>/dev/null; then
    echo "✅ Kafka: UP"
else
    echo "❌ Kafka: DOWN"
fi

# PostgreSQL
if docker exec dev_postgres pg_isready -U postgres &>/dev/null; then
    echo "✅ PostgreSQL: UP"
else
    echo "❌ PostgreSQL: DOWN"
fi

echo ""
echo "=== Microservices Health Check ==="

check_service() {
    local name=$1
    local port=$2
    local status=$(curl -s http://localhost:$port/actuator/health | jq -r '.status' 2>/dev/null)
    
    if [ "$status" == "UP" ]; then
        echo "✅ $name ($port): UP"
    else
        echo "❌ $name ($port): DOWN"
    fi
}

check_service "Monolith" 8080
check_service "Auth Service" 8082
check_service "EMR Service" 8086
check_service "Notification Service" 8087
check_service "Pain Escalation Service" 8088
check_service "External VAS Service" 8089
check_service "Reporting Service" 8091
check_service "Backup Service" 8085
```

Make executable:
```bash
chmod +x scripts/verify-all.sh
./scripts/verify-all.sh
```

---

## Quick Reference Commands

### Daily Development
```bash
# Start work
docker-compose --profile core up -d
mvn spring-boot:run

# End work
docker-compose stop
```

### Full Restart
```bash
docker-compose --profile all down
docker-compose --profile all up -d --build
```

### Emergency Stop
```bash
docker-compose kill
docker-compose down -v
```

### Check Everything
```bash
docker-compose ps
docker-compose logs --tail=50
```

---

**Next:** See [DOCKER_COMPOSE_REFERENCE.md](DOCKER_COMPOSE_REFERENCE.md) for detailed Docker Compose documentation  
**See Also:** [TESTING_GUIDE.md](TESTING_GUIDE.md) for comprehensive testing scenarios

---

**Last Updated:** January 12, 2026  
**Version:** 3.0  
**Maintained By:** DevOps Team
