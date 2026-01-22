# 🗄️ Database Setup Plan - Pain Management Platform

**Date:** January 12, 2026  
**Status:** In Progress  
**Current Stage:** Monolith Running, Microservices Need Database Configuration

---

## 📊 Current Database Architecture

### Existing PostgreSQL Clusters

#### **Cluster 1: Main Application** (`localhost:5432`)
```
├── pain_management_db     ← Монолит (работает)
├── auth_db                ← Authentication Service (нужна настройка)
└── postgres               ← Системная база
```

#### **Cluster 2: Analytics** (`localhost:5433`)
```
└── analytics_reporting    ← Reporting Service (работает)
```

---

## ✅ Current System Status

### Infrastructure
- ✅ Kafka (9092) - Running, Healthy
- ✅ PostgreSQL Main (5432) - Running, Healthy (Docker)
- ✅ PostgreSQL Analytics (5433) - Running, Healthy (Docker)
- ✅ Prometheus (9090) - Running
- ✅ Grafana (3000) - Running
- ✅ Kafdrop (9000) - Running

### Monolith
- ✅ **Port:** 8080
- ✅ **Database:** `pain_management_db` (localhost:5432)
- ✅ **Status:** Running Successfully
- ⚠️ **Warnings:** 
  - Generated security password (development mode)
  - spring.jpa.open-in-view enabled (performance warning)

### Microservices Status

| Service | Port | Status | Database Needed | Current Issue |
|---------|------|--------|-----------------|---------------|
| **Authentication** | 8082 | ❌ Crashed | `auth_db` | Missing config: `cors.allowed-methods` |
| **EMR Integration** | 8086 | 🔄 Restarting | `emr_integration_db` | Database does not exist |
| **Notification** | 8087 | 🔄 Restarting | `notification_db` | Database does not exist |
| **Pain Escalation** | 8088 | 🔄 Restarting | `pain_escalation_db` | Database does not exist |
| **External VAS** | 8089 | ✅ Running | Own DB | OK (uses shared DB?) |
| **Reporting** | 8091 | ✅ Running | `analytics_reporting` | OK |
| **Backup & Restore** | 8085 | ❌ Crashed | Config issue | Missing datasource config |

---

## 🎯 Database Architecture Strategy

### Option 1: Microservices Architecture (Recommended) ✅

**Concept:** Each microservice has its own database (Database per Service pattern)

**Advantages:**
- ✅ True microservices isolation
- ✅ Independent scaling
- ✅ Independent deployment
- ✅ Technology flexibility
- ✅ Easier to manage transactions within service

**Structure:**
```
PostgreSQL Cluster (localhost:5432)
├── pain_management_db          ← Monolith
├── auth_db                      ← Authentication Service (exists)
├── emr_integration_db           ← EMR Integration Service (need to create)
├── notification_db              ← Notification Service (need to create)
├── pain_escalation_db           ← Pain Escalation Service (need to create)
└── external_vas_db              ← External VAS Service (need to create)

PostgreSQL Cluster (localhost:5433)
└── analytics_reporting          ← Reporting Service (exists)
```

### Option 2: Shared Database (Not Recommended) ❌

All microservices connect to `pain_management_db` - **defeats microservices purpose**

---

## 📋 Step-by-Step Setup Plan

### Phase 1: Create Missing Databases ✅ RECOMMENDED

**Step 1:** Create databases in PostgreSQL cluster (localhost:5432)

```sql
-- Connect to PostgreSQL
psql -h localhost -p 5432 -U postgres

-- Create databases
CREATE DATABASE emr_integration_db;
CREATE DATABASE notification_db;
CREATE DATABASE pain_escalation_db;
CREATE DATABASE external_vas_db;

-- Grant privileges (if needed)
GRANT ALL PRIVILEGES ON DATABASE emr_integration_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pain_escalation_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE external_vas_db TO postgres;

-- Verify
\l
```

**Step 2:** Configure Docker Compose to use **local PostgreSQL** (not containers)

Current microservices are configured to connect to Docker PostgreSQL containers.  
Need to change to connect to **host PostgreSQL** (`host.docker.internal:5432`).

**Step 3:** Fix Authentication Service configuration
- Add missing `cors.allowed-methods` environment variable

**Step 4:** Fix Backup & Restore Service configuration
- Configure datasource URL

---

### Phase 2: Connection Configuration

#### Current Problem:
Microservices in Docker containers try to connect to `postgres:5432` (Docker network).  
Your actual databases are on **host machine** (`localhost:5432`).

#### Solution:
Update microservice connection strings to use `host.docker.internal:5432`

**Example for EMR Integration Service:**
```yaml
emr-service:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/emr_integration_db
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
```

---

## 🔧 Detailed Configuration for Each Service

### 1. Authentication Service (Port 8082)

**Database:** `auth_db` (already exists on localhost:5432)

**Current Issues:**
1. Missing environment variable: `cors.allowed-methods`
2. Database connection might point to Docker container instead of host

**Fix:**
```yaml
# docker-compose.dev.yml
auth-service:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/auth_db
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
    CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:5173}
    CORS_ALLOWED_METHODS: GET,POST,PUT,DELETE,OPTIONS  # ← ADD THIS
    JWT_SECRET: ${JWT_SECRET}
```

---

### 2. EMR Integration Service (Port 8086)

**Database:** `emr_integration_db` (needs to be created)

**Steps:**
1. Create database:
   ```sql
   CREATE DATABASE emr_integration_db;
   ```

2. Update docker-compose.yml:
   ```yaml
   emr-service:
     environment:
       SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/emr_integration_db
       SPRING_DATASOURCE_USERNAME: postgres
       SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
   ```

---

### 3. Notification Service (Port 8087)

**Database:** `notification_db` (needs to be created)

**Steps:**
1. Create database:
   ```sql
   CREATE DATABASE notification_db;
   ```

2. Update docker-compose.yml:
   ```yaml
   notification-service:
     environment:
       SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/notification_db
       SPRING_DATASOURCE_USERNAME: postgres
       SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
   ```

---

### 4. Pain Escalation Service (Port 8088)

**Database:** `pain_escalation_db` (needs to be created)

**Steps:**
1. Create database:
   ```sql
   CREATE DATABASE pain_escalation_db;
   ```

2. Update docker-compose.yml:
   ```yaml
   pain-escalation-service:
     environment:
       SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/pain_escalation_db
       SPRING_DATASOURCE_USERNAME: postgres
       SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
   ```

---

### 5. External VAS Service (Port 8089)

**Database:** `external_vas_db` (needs to be created)

**Status:** Currently running (might be using shared DB or in-memory)

**Steps:**
1. Create database:
   ```sql
   CREATE DATABASE external_vas_db;
   ```

2. Update docker-compose.yml:
   ```yaml
   external-vas-service:
     environment:
       SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/external_vas_db
       SPRING_DATASOURCE_USERNAME: postgres
       SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
   ```

---

### 6. Reporting Service (Port 8091)

**Database:** `analytics_reporting` (exists on localhost:5433) ✅

**Status:** Running successfully

**Current Config:**
```yaml
reporting-service:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5433/analytics_reporting
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
```

---

### 7. Backup & Restore Service (Port 8085)

**Database:** Needs main database access for backup

**Current Issue:** Missing datasource configuration

**Fix:**
```yaml
backup-restore-service:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/pain_management_db
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
```

---

## 📝 Complete Action Plan

### ✅ Phase 1: Database Creation (5 minutes)

```bash
# Connect to local PostgreSQL
psql -h localhost -p 5432 -U postgres

# Create all missing databases
CREATE DATABASE emr_integration_db;
CREATE DATABASE notification_db;
CREATE DATABASE pain_escalation_db;
CREATE DATABASE external_vas_db;

# Verify
\l

# Exit
\q
```

### ✅ Phase 2: Stop Current Containers (1 minute)

```bash
cd C:\backend_projects\pain_managment_back

# Stop all running containers
docker-compose -f docker-compose.dev.yml --profile all down

# Verify stopped
docker ps
```

### ✅ Phase 3: Update docker-compose.dev.yml (10 minutes)

Update all microservices to:
1. Use `host.docker.internal:5432` instead of `postgres:5432`
2. Add missing environment variables
3. Point to correct databases

### ✅ Phase 4: Restart Services (5 minutes)

```bash
# Start infrastructure first
docker-compose -f docker-compose.dev.yml up -d kafka postgres postgres-analytics

# Wait 30 seconds

# Start all microservices
docker-compose -f docker-compose.dev.yml --profile all up -d

# Check status
docker-compose -f docker-compose.dev.yml ps
```

### ✅ Phase 5: Verify Health (5 minutes)

```bash
# Check each service
curl http://localhost:8082/actuator/health  # Auth
curl http://localhost:8086/actuator/health  # EMR
curl http://localhost:8087/actuator/health  # Notification
curl http://localhost:8088/actuator/health  # Pain Escalation
curl http://localhost:8089/api/external/vas/health  # External VAS
curl http://localhost:8091/actuator/health  # Reporting
```

---

## 🚀 Quick Start Commands

### Create All Databases at Once

```sql
-- create-microservices-databases.sql
-- Run: psql -h localhost -p 5432 -U postgres -f create-microservices-databases.sql

-- Check if databases exist, create if not
SELECT 'CREATE DATABASE emr_integration_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'emr_integration_db')\gexec

SELECT 'CREATE DATABASE notification_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec

SELECT 'CREATE DATABASE pain_escalation_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pain_escalation_db')\gexec

SELECT 'CREATE DATABASE external_vas_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'external_vas_db')\gexec

-- List all databases
\l
```

---

## 📊 Expected Final Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    MONOLITH (Port 8080)                          │
│                  pain_management_db (5432)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
            ┌───────▼────────┐   ┌──────▼──────┐
            │  Kafka (9092)  │   │ PostgreSQL  │
            │   Event Bus    │   │  (5432)     │
            └───────┬────────┘   └──────┬──────┘
                    │                   │
       ┌────────────┼───────────────────┼────────────┐
       │            │                   │            │
       ▼            ▼                   ▼            ▼
┌──────────┐ ┌──────────┐       ┌──────────┐ ┌──────────┐
│   Auth   │ │   EMR    │       │  Notify  │ │   Pain   │
│  (8082)  │ │  (8086)  │       │  (8087)  │ │  (8088)  │
│          │ │          │       │          │ │          │
│ auth_db  │ │emr_int.. │       │notif_db  │ │pain_es.. │
│  (5432)  │ │  (5432)  │       │  (5432)  │ │  (5432)  │
└──────────┘ └──────────┘       └──────────┘ └──────────┘

       ▼            ▼                   ▼
┌──────────┐ ┌──────────┐       ┌──────────────────┐
│   VAS    │ │ Backup   │       │   PostgreSQL     │
│  (8089)  │ │  (8085)  │       │   Analytics      │
│          │ │          │       │    (5433)        │
│ext_vas_db│ │pain_mgm..│       └────────┬─────────┘
│  (5432)  │ │  (5432)  │                │
└──────────┘ └──────────┘                ▼
                              ┌──────────────────┐
                              │   Reporting      │
                              │    (8091)        │
                              │                  │
                              │analytics_report..│
                              │     (5433)       │
                              └──────────────────┘
```

---

## 🔍 Database Connection Summary

| Service | Database | Host | Port | Schema Auto-Create |
|---------|----------|------|------|-------------------|
| Monolith | `pain_management_db` | localhost | 5432 | Liquibase |
| Authentication | `auth_db` | localhost | 5432 | Liquibase |
| EMR Integration | `emr_integration_db` | localhost | 5432 | Liquibase |
| Notification | `notification_db` | localhost | 5432 | Liquibase |
| Pain Escalation | `pain_escalation_db` | localhost | 5432 | Liquibase |
| External VAS | `external_vas_db` | localhost | 5432 | JPA/Hibernate |
| Reporting | `analytics_reporting` | localhost | 5433 | JPA/Hibernate |
| Backup & Restore | `pain_management_db` | localhost | 5432 | Read-only |

**Note:** All services use Liquibase or Hibernate to auto-create schema, so you only need to create empty databases.

---

## ⚠️ Important Notes

1. **Docker Container PostgreSQL vs Host PostgreSQL:**
   - Docker containers have their own PostgreSQL instances
   - Your local DBIaver connects to **host PostgreSQL** (localhost)
   - Microservices in containers need `host.docker.internal` to reach host PostgreSQL

2. **Password Management:**
   - Ensure `.env` file has correct `POSTGRES_PASSWORD`
   - Same password should work for all databases (same PostgreSQL cluster)

3. **Schema Migration:**
   - Liquibase/Hibernate will create tables automatically
   - You only need to create empty databases

4. **Backup Strategy:**
   - Each database can be backed up independently
   - Backup service can access all databases for centralized backup

---

## 📁 Next Steps Files to Create

1. `scripts/create-databases.sql` - SQL script to create all databases
2. `scripts/verify-databases.sh` - Script to verify all connections
3. Update `docker-compose.dev.yml` - Fix all connection strings
4. Update `.env.example` - Document all required variables

---

**Last Updated:** January 22, 2026  
**Status:** ✅ COMPLETED - All Databases Created, All Services Running  
**Implementation Date:** January 22, 2026

---

## ✅ FINAL STATUS - January 22, 2026

### All Databases Successfully Created

**PostgreSQL Main (localhost:5432):**
```
✅ pain_management_db     ← Monolith
✅ auth_db                ← Authentication Service  
✅ emr_integration_db     ← EMR Integration Service
✅ notification_db        ← Notification Service
✅ pain_escalation_db     ← Pain Escalation Service
✅ external_vas_db        ← External VAS Service
✅ backup_service         ← Backup & Restore Service
```

**PostgreSQL Analytics (localhost:5433):**
```
✅ analytics_reporting    ← Reporting Service
```

### All Microservices Running

- ✅ Authentication Service (8082)
- ✅ EMR Integration Service (8086)
- ✅ Notification Service (8087)
- ✅ Pain Escalation Service (8088)
- ✅ External VAS Service (8089)
- ✅ Reporting Service (8091)
- ✅ Backup & Restore Service (8085)

**Success Rate:** 7/7 (100%)

All databases were created using Docker PostgreSQL initialization and manual commands. The Database per Service pattern is fully implemented and operational.

---

**See Also:**
- [BACKEND_ARCHITECTURE_OVERVIEW.md](BACKEND_ARCHITECTURE_OVERVIEW.md) - Complete architecture overview
- [MICROSERVICES_STARTUP_FIXES_JAN2026.md](MICROSERVICES_STARTUP_FIXES_JAN2026.md) - Detailed fixes applied
- [DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md)
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- [HOW_TO_RUN_AND_TEST.md](HOW_TO_RUN_AND_TEST.md)
