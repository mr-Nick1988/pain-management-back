# 🏗️ Backend Architecture Overview - Pain Management Platform

**Last Updated:** January 24, 2026  
**Status:** ✅ All Services Running + API Gateway + Distributed Tracing  
**Version:** 3.3

---

## 🎯 Backend Architecture Overview

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND LAYER                            │
│              React SPA (localhost:5173)                      │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/REST
┌──────────────────────▼──────────────────────────────────────┐
│                  API GATEWAY LAYER                           │
│            API Gateway (port 8000)                           │
│  - JWT Validation  - Circuit Breaker  - CORS                │
│  - Request Routing - Rate Limiting    - Logging             │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              APPLICATION LAYER                               │
│  ┌────────────────┐  ┌─────────────────────────────────┐   │
│  │   Monolith     │  │      Microservices              │   │
│  │   (port 8080)  │  │  - Auth (8082)                  │   │
│  │                │  │  - EMR (8086)                   │   │
│  │  Core Business │  │  - Notification (8087)          │   │
│  │  Logic         │  │  - Pain Escalation (8088)       │   │
│  │                │  │  - External VAS (8089)          │   │
│  │                │  │  - Reporting (8091)             │   │
│  │                │  │  - Backup (8085)                │   │
│  └────────┬───────┘  └──────────┬──────────────────────┘   │
└───────────┼─────────────────────┼──────────────────────────┘
            │                     │
            │   ┌─────────────────▼─────────────────┐
            │   │   EVENT STREAMING LAYER           │
            │   │   Apache Kafka (port 9092)        │
            │   │   - 8 Topics for async events     │
            │   └─────────────────┬─────────────────┘
            │                     │
┌───────────▼─────────────────────▼─────────────────────────┐
│                    DATA LAYER                              │
│  ┌─────────────────────┐    ┌──────────────────────┐     │
│  │ PostgreSQL Main     │    │ PostgreSQL Analytics │     │
│  │ (port 5432)         │    │ (port 5433)          │     │
│  │                     │    │                      │     │
│  │ - pain_management_db│    │ - analytics_reporting│     │
│  │ - auth_db           │    │                      │     │
│  │ - emr_integration_db│    └──────────────────────┘     │
│  │ - notification_db   │                                  │
│  │ - pain_escalation_db│                                  │
│  │ - external_vas_db   │                                  │
│  │ - backup_service    │                                  │
│  └─────────────────────┘                                  │
└────────────────────────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────────────┐
│              OBSERVABILITY LAYER                             │
│  Prometheus (9090) → Grafana (3000)                         │
│  Kafdrop (9000) - Kafka UI                                  │
└──────────────────────────────────────────────────────────────┘
```

---

## 📊 Компоненты Системы

### 1. Monolith (Port 8080)

**Роль:** Core Business Logic

**Модули:**
- `admin/` - управление пользователями
- `doctor/` - работа с пациентами, рекомендации
- `nurse/` - работа с VAS, EMR
- `anesthesiologist/` - эскалации, протоколы лечения
- `treatment_protocol/` - алгоритм генерации рекомендаций (75k ICD кодов)
- `common/` - общие entity (Patient, VAS, EMR)

**База данных:** `pain_management_db` (PostgreSQL 5432)

**Технологии:**
- Spring Boot 3.5.5
- Java 22
- Liquibase (миграции)
- Spring Data JPA

---

### 2. API Gateway (Port 8000)

**Роль:** Single Entry Point для всех клиентских запросов

**Функции:**
- Request routing ко всем микросервисам
- JWT валидация и user context propagation
- ✅ Circuit Breaker для resilience (Resilience4j)
- Централизованный CORS
- Request/Response logging
- Rate limiting (готов к использованию)

**Технологии:**
- Spring Cloud Gateway (reactive)
- Resilience4j (Circuit Breaker)
- Java 21

**Документация:** [api-gateway-service.md](../05-microservices/api-gateway-service.md)

---

### 3. Microservices (7 сервисов)

#### 🔐 Authentication Service (Port 8082)
- **Назначение:** JWT аутентификация, управление пользователями
- **База:** `auth_db` (host.docker.internal:5432)
- **Kafka:** Публикует события аутентификации
- **Endpoint:** `/actuator/health`

#### 📋 EMR Integration Service (Port 8086)
- **Назначение:** Интеграция с FHIR серверами (HAPI FHIR)
- **База:** `emr_integration_db` (Docker PostgreSQL)
- **Kafka:** Публикует `emr.changes`, `emr.created`
- **Технологии:** FHIR R4, REST API

#### 📧 Notification Service (Port 8087)
- **Назначение:** Email, WebSocket, Push уведомления
- **База:** `notification_db` (Docker PostgreSQL)
- **Kafka:** Подписан на `pain.escalated`, `dose.administered`, `vas.recorded`
- **Технологии:** JavaMailSender, WebSocket

#### ⚠️ Pain Escalation Service (Port 8088)
- **Назначение:** Отслеживание эскалаций боли, дозировок
- **База:** `pain_escalation_db` (Docker PostgreSQL)
- **Kafka:** Публикует `pain.escalated`, `dose.administered`
- **Алгоритмы:** VAS increase threshold, critical pain level

#### 📊 External VAS Service (Port 8089)
- **Назначение:** Интеграция с внешними VAS устройствами
- **База:** `external_vas_db` (Docker PostgreSQL)
- **Kafka:** Публикует `vas.external.recorded`
- **Форматы:** JSON, XML, CSV

#### 📈 Reporting Service (Port 8091)
- **Назначение:** Генерация отчетов (Excel, PDF), email
- **База:** `analytics_reporting` (PostgreSQL Analytics 5433)
- **Kafka:** Подписан на `reporting-commands`
- **Технологии:** Apache POI, iText

#### 💾 Backup & Restore Service (Port 8085)
- **Назначение:** Резервное копирование БД
- **База:** `backup_service` (Docker PostgreSQL)
- **Технологии:** pg_dump, pg_restore
- **Scheduler:** Автоматические бэкапы (опционально)

---

### 3. Event Streaming - Apache Kafka (Port 9092)

**Kafka Topics:**
```
analytics-events          → Аналитические события
emr.changes              → Изменения в EMR
emr.created              → Новые EMR записи
pain.escalated           → Эскалация боли
dose.administered        → Введение дозы
vas.recorded             → Запись VAS
vas.external.recorded    → Внешние VAS данные
reporting-commands       → Команды для отчетов
```

**Режим:** KRaft (без Zookeeper)

---

### 4. Data Layer

#### PostgreSQL Main (Port 5432)
```
pain_management_db     ← Monolith
auth_db                ← Authentication (host.docker.internal)
emr_integration_db     ← EMR Service
notification_db        ← Notification Service
pain_escalation_db     ← Pain Escalation Service
external_vas_db        ← External VAS Service
backup_service         ← Backup Service
```

#### PostgreSQL Analytics (Port 5433)
```
analytics_reporting    ← Reporting Service
```

**Isolation Strategy:** Database per Service pattern

---

### 5. Service Discovery - HashiCorp Consul

- **Consul Server** (8500, 8600): Service Registry
- **Auto-registration**: All microservices register on startup
- **Health Checks**: HTTP checks every 10s
- **Load Balancing**: Round-robin via Spring Cloud LoadBalancer
- **Dynamic Routing**: API Gateway uses `lb://service-name`
- **Web UI**: http://localhost:8500/ui

**Registered Services:** API Gateway, Authentication, EMR, Notification, Pain Escalation, External VAS, Reporting, Backup

---

### 6. Distributed Tracing - Jaeger + OpenTelemetry

- **Jaeger Server** (16686, 4317, 4318): Trace collection and visualization
- **Protocol**: OpenTelemetry OTLP HTTP
- **Instrumentation**: Automatic via Micrometer Tracing Bridge
- **Sampling**: 100% (development), configurable for production
- **Trace Propagation**: Automatic across HTTP, JDBC, Kafka
- **Log Correlation**: TraceID in logs `[service,traceId,spanId]`
- **Web UI**: http://localhost:16686

**Features:**
- End-to-end request tracking across all 8 services
- Performance bottleneck identification
- Service dependency visualization
- Error correlation and debugging

---

### 7. Observability (Monitoring)

- **Prometheus** (9090): Metrics collection from all services
- **Grafana** (3000): Metrics visualization
- **Jaeger UI** (16686): Distributed tracing visualization
- **Consul UI** (8500): Service registry monitoring
- **Kafdrop** (9000): Kafka UI for topics and events
- **Spring Boot Actuator**: Health checks, metrics endpoints

---

## 🔄 Patterns & Principles

### Architectural Patterns

1. **Microservices Architecture**
   - Database per Service
   - API Gateway Pattern
   - API First
   - Decentralized Data Management

2. **Resilience Patterns** ✅
   - Circuit Breaker (Resilience4j)
   - Retry with Exponential Backoff
   - Fallback Methods
   - Timeout Management

3. **Service Discovery** ✅
   - Dynamic Service Registration (Consul)
   - Health-Based Routing
   - Client-Side Load Balancing
   - Service Mesh Ready

4. **Distributed Tracing** ✅
   - End-to-End Request Tracking (Jaeger)
   - OpenTelemetry Instrumentation
   - Trace Propagation (HTTP, Kafka, JDBC)
   - Performance Analysis

5. **Event-Driven Architecture**
   - Asynchronous Communication
   - Event Sourcing
   - CQRS (Command Query Responsibility Segregation)

6. **Domain-Driven Design**
   - Bounded Contexts
   - Aggregates
   - Domain Events

### Communication Patterns

1. **Synchronous:**
   - REST API (HTTP/JSON)
   - Direct service-to-service calls

2. **Asynchronous:**
   - Kafka events
   - Pub/Sub model
   - Event-driven workflows

---

## 🚀 Deployment Architecture

### Development Environment (Docker Compose)

**Профили:**
```bash
# Только инфраструктура
docker-compose -f docker-compose.dev.yml up -d

# Все сервисы
docker-compose -f docker-compose.dev.yml --profile all up -d

# Критичные сервисы
docker-compose -f docker-compose.dev.yml --profile core up -d

# С мониторингом
docker-compose -f docker-compose.dev.yml --profile all --profile monitoring up -d
```

### Container Network

**Network:** `painmgmt-dev-network` (bridge)

**Service Discovery:**
- ✅ **HashiCorp Consul** для динамической регистрации
- Сервисы регистрируются автоматически при старте
- Health checks каждые 10s
- API Gateway использует load-balanced routing: `lb://service-name`
- DNS resolution by Docker (fallback)

---

## 📈 Data Flow Examples

### Example 1: VAS Recording Flow

```
1. Nurse записывает VAS через Monolith API
2. Monolith сохраняет в pain_management_db
3. Monolith публикует событие в Kafka: vas.recorded
4. Pain Escalation Service получает событие
5. Pain Escalation анализирует эскалацию
6. Если есть эскалация → публикует pain.escalated
7. Notification Service получает pain.escalated
8. Notification отправляет email анестезиологу
```

### Example 2: EMR Sync Flow

```
1. EMR Service запрашивает FHIR Server
2. Получает данные пациента в FHIR R4
3. Преобразует в internal format
4. Сохраняет в emr_integration_db
5. Публикует событие emr.created в Kafka
6. Monolith подписан на emr.created
7. Monolith обновляет свою копию EMR
```

### Example 3: Reporting Flow

```
1. Doctor запрашивает отчет через Monolith
2. Monolith публикует reporting-commands в Kafka
3. Reporting Service получает команду
4. Reporting Service читает данные из analytics_reporting
5. Генерирует Excel/PDF
6. Отправляет email с отчетом
```

---

## 🔐 Security Architecture

### Authentication & Authorization

1. **Frontend → Backend:**
   - JWT token в Authorization header
   - Token выдается Authentication Service
   - Monolith валидирует token через Auth Service

2. **Service-to-Service:**
   - Internal network isolation
   - No authentication needed (trusted zone)
   - Future: mTLS or API Gateway

### Data Security

- **Secrets:** Environment variables (.env file)
- **Database:** PostgreSQL user/password authentication
- **CORS:** Configured per service
- **No hardcoded credentials** in code

---

## 📦 Technology Stack

### Backend
- **Language:** Java 22
- **Framework:** Spring Boot 3.5.5
- **Build Tool:** Maven 3.9

### Data
- **Primary DB:** PostgreSQL 16 (Alpine)
- **Message Broker:** Apache Kafka 7.6.1 (Confluent)
- **Migrations:** Liquibase, Hibernate DDL

### Infrastructure
- **Containerization:** Docker, Docker Compose
- **Monitoring:** Prometheus, Grafana
- **Observability:** Spring Boot Actuator, Micrometer

### External Integrations
- **FHIR:** HAPI FHIR R4
- **Email:** JavaMailSender (SMTP)
- **Charts:** Apache POI, iText

---

## 📚 Related Documentation

### Architecture
- [MICROSERVICES_MIGRATION_STRATEGY.md](architecture/MICROSERVICES_MIGRATION_STRATEGY.md) - Стратегия миграции
- [MIGRATION_ROADMAP.md](architecture/MIGRATION_ROADMAP.md) - Дорожная карта
- [microservices-architecture-2026.puml](architecture/diagrams/microservices-architecture-2026.puml) - PlantUML диаграмма

### Operations
- [DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md) - DevOps руководство
- [DOCKER_COMPOSE_REFERENCE.md](DOCKER_COMPOSE_REFERENCE.md) - Docker Compose справка
- [DATABASE_SETUP_PLAN.md](DATABASE_SETUP_PLAN.md) - План настройки БД

### Development
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Руководство по тестированию
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Решение проблем
- [HOW_TO_RUN_AND_TEST.md](HOW_TO_RUN_AND_TEST.md) - Быстрый старт

---

## ✅ Current Status (January 23, 2026)

### Infrastructure
- ✅ Consul Service Registry (8500, 8600) - Running
- ✅ Jaeger Distributed Tracing (16686, 4317, 4318) - Running
- ✅ Kafka Running (healthy)
- ✅ PostgreSQL Main Running (healthy)
- ✅ PostgreSQL Analytics Running (healthy)
- ✅ Prometheus Running
- ✅ Grafana Running
- ✅ Kafdrop Running

### API Gateway
- ✅ API Gateway Service (8000) - Running

### Microservices
- ✅ Authentication Service (8082) - Running
- ✅ EMR Integration Service (8086) - Running
- ✅ Notification Service (8087) - Running
- ✅ Pain Escalation Service (8088) - Running
- ✅ External VAS Service (8089) - Running
- ✅ Reporting Service (8091) - Running
- ✅ Backup & Restore Service (8085) - Running

**Total:** 8/8 Services Running (100%)

---

## 🎯 Next Steps

1. ✅ **API Gateway** - Complete (Spring Cloud Gateway)
2. ✅ **Circuit Breaker** - Complete (Resilience4j in all services)
3. ✅ **Service Discovery** - Complete (HashiCorp Consul)
4. ✅ **Distributed Tracing** - Complete (Jaeger + OpenTelemetry)
5. **Centralized Logging** - ELK Stack (Phase 8)
6. **Service Mesh** - Istio or Linkerd (Phase 9)
7. **CI/CD Pipeline** - GitHub Actions
8. **Production Deployment** - Kubernetes

---

**For detailed technical information, refer to specific service documentation in `docs/microservices/` folder.**
