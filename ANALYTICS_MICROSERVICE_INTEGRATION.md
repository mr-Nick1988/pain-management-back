# Analytics & Reporting Microservice Integration

## Overview

The monolith has been successfully refactored to work with the separate **Analytics-Reporting Microservice** (port 8091).

## Architecture Changes

### Before (Monolith)
- ✗ Events published to Kafka AND stored locally in MongoDB
- ✗ Local aggregation service (`DataAggregationService`)
- ✗ Local reporting endpoints (`/api/reports`, `/api/analytics`)
- ✗ PostgreSQL aggregates stored in monolith database

### After (Microservice Architecture)
- ✓ Events published ONLY to Kafka topic `analytics-events`
- ✓ Microservice consumes events from Kafka
- ✓ Microservice stores raw events in MongoDB (`analytics_db`)
- ✓ Microservice performs aggregations and stores in PostgreSQL (`analytics_reporting` database)
- ✓ Microservice exposes reporting endpoints

## What Was Changed in Monolith

### 1. **AnalyticsEventListener** (Modified)
- **Before:** Saved events to local MongoDB via `AnalyticsEventRepository`
- **After:** Publishes events to Kafka via `AnalyticsPublisher`
- **Location:** `pain_helper_back.analytics.listener.AnalyticsEventListener`

### 2. **AnalyticsMongoConfig** (Disabled)
- **Status:** Commented out `@Configuration` and `@EnableMongoRepositories`
- **Reason:** Analytics MongoDB is now managed by the microservice
- **Location:** `pain_helper_back.mongo_config.AnalyticsMongoConfig`

### 3. **DataAggregationService** (Deprecated)
- **Status:** Marked as `@Deprecated`, disabled by default via `@ConditionalOnProperty`
- **Reason:** All aggregation logic moved to microservice
- **Location:** `pain_helper_back.reporting.service.DataAggregationService`

### 4. **AnalyticsController** (Disabled)
- **Status:** Commented out `@RestController`, marked as `@Deprecated`
- **Reason:** Analytics queries now handled by microservice
- **Location:** `pain_helper_back.analytics.controller.AnalyticsController`

### 5. **ReportStatisticsController** (Disabled)
- **Status:** Commented out `@RestController`, marked as `@Deprecated`
- **Reason:** Reporting endpoints now handled by microservice
- **Location:** `pain_helper_back.reporting.controller.ReportStatisticsController`

### 6. **application.properties** (Updated)
```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3

# Analytics/Reporting Integration
kafka.topic.analytics-events=${KAFKA_TOPIC_ANALYTICS_EVENTS:analytics-events}
analytics.reporting.base-url=${ANALYTICS_REPORTING_BASE_URL:http://localhost:8091}
```

## Event Flow

```
Monolith Business Logic
    ↓
Spring ApplicationEvent published
    ↓
AnalyticsEventListener receives event
    ↓
Builds AnalyticsEvent object
    ↓
AnalyticsPublisher.publish(event)
    ↓
Kafka Topic: analytics-events
    ↓
Analytics-Reporting Microservice consumes
    ↓
Stores in MongoDB (analytics_db)
    ↓
Aggregates daily/weekly/monthly
    ↓
Stores aggregates in PostgreSQL
```

## Microservice Endpoints

### Analytics Endpoints (Port 8091)
- `POST /api/analytics/events` - Manual event ingestion (fallback)
- `GET /api/analytics/events?start={datetime}&end={datetime}` - Query raw events

### Reporting Endpoints (Port 8091)
- `POST /api/reporting/aggregate/daily?date={date}` - Trigger daily aggregation
- `GET /api/reporting/daily/{date}` - Get daily aggregate
- `POST /api/reporting/aggregate/weekly?weekStart={date}&weekEnd={date}` - Trigger weekly aggregation
- `POST /api/reporting/aggregate/monthly?year={year}&month={month}` - Trigger monthly aggregation

## Environment Variables

### Monolith (.env)
```bash
# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_ANALYTICS_EVENTS=analytics-events

# Analytics-Reporting Microservice URL
ANALYTICS_REPORTING_BASE_URL=http://localhost:8091

# MongoDB (Performance & Backup only - analytics moved to microservice)
MONGODB_PERFORMANCE_URI=mongodb://localhost:27017/performance_db
MONGODB_BACKUP_URI=mongodb://localhost:27017/backup_db
```

### Analytics-Reporting Microservice (.env)
```bash
# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_ANALYTICS_EVENTS=analytics-events

# MongoDB (for raw analytics events)
MONGODB_ANALYTICS_URI=mongodb://localhost:27017/analytics_db

# PostgreSQL (for aggregates)
PG_JDBC_URL=jdbc:postgresql://localhost:5433/analytics_reporting
PG_USER=analytics
PG_PASSWORD=analytics
```

## Running the System

### 1. Start Infrastructure
```bash
# Start Kafka (via Docker or local installation)
docker-compose up -d kafka zookeeper

# Start PostgreSQL for microservice
docker-compose -f analytics-reporting-docker-compose.yml up -d postgres

# Start MongoDB
docker-compose up -d mongodb
```

### 2. Start Microservice
```bash
cd analytics-reporting-service
mvn spring-boot:run
# Runs on port 8091
```

### 3. Start Monolith
```bash
cd pain_managment_back
mvn spring-boot:run
# Runs on port 8080
```

## Event Types Published

The monolith publishes the following event types to Kafka:

| Event Type | Triggered By | Contains |
|------------|-------------|----------|
| `PERSON_CREATED` | Admin creates user | personId, role, firstName, lastName |
| `PERSON_DELETED` | Admin deletes user | personId, role, reason |
| `PERSON_UPDATED` | Admin updates user | personId, changedFields |
| `USER_LOGIN_SUCCESS` | Successful login | personId, role, ipAddress |
| `USER_LOGIN_FAILED` | Failed login | personId, ipAddress |
| `PATIENT_REGISTERED` | Patient created | patientId, patientMrn, age, gender |
| `EMR_CREATED` | EMR record created | patientMrn, gfr, weight, diagnosisCodes |
| `VAS_RECORDED` | Pain level recorded | patientMrn, vasLevel, isCritical, vasSource |
| `RECOMMENDATION_CREATED` | Recommendation created | recommendationId, drugName, dosage, vasLevel |
| `RECOMMENDATION_APPROVED` | Doctor approves | recommendationId, patientMrn, processingTimeMs |
| `RECOMMENDATION_REJECTED` | Doctor rejects | recommendationId, rejectionReason |
| `DOSE_ADMINISTERED` | Nurse administers dose | patientMrn, drugName, dosage |
| `ESCALATION_CREATED` | Pain escalation | escalationId, priority, vasLevel |
| `ESCALATION_RESOLVED` | Escalation resolved | escalationId, resolutionTimeMs |

## Data Retention

- **MongoDB (Raw Events):** Cleaned up after 30 days by microservice
- **PostgreSQL (Aggregates):** Retained indefinitely for historical reporting

## Monitoring

### Health Checks
- Monolith: `http://localhost:8080/actuator/health`
- Microservice: `http://localhost:8091/actuator/health`

### Kafka Consumer Status
Check microservice logs for Kafka consumption:
```bash
# Should see: "Ingested event from Kafka topic analytics-events: {eventType}"
tail -f analytics-reporting-service/logs/application.log
```

## Troubleshooting

### Events Not Reaching Microservice
1. Check Kafka is running: `docker ps | grep kafka`
2. Check topic exists: `kafka-topics.sh --list --bootstrap-server localhost:9092`
3. Check microservice consumer logs
4. Verify `kafka.topic.analytics-events` matches in both services

### Fallback to REST
If Kafka fails, `AnalyticsPublisher` automatically falls back to REST:
```
POST http://localhost:8091/api/analytics/events
```

### Missing Aggregates
Trigger manual aggregation:
```bash
curl -X POST "http://localhost:8091/api/reporting/aggregate/daily?date=2025-11-11"
```

## Migration Notes

### Existing Data
- Old analytics events in monolith MongoDB are NOT migrated
- Start fresh with microservice or manually export/import if needed

### Deprecated Components (Can Be Removed)
- `pain_helper_back.analytics.repository.AnalyticsEventRepository`
- `pain_helper_back.analytics.service.AnalyticsService`
- `pain_helper_back.reporting.entity.*` (DailyReportAggregate, etc.)
- `pain_helper_back.reporting.repository.*`
- `pain_helper_back.reporting.service.*`
- `pain_helper_back.mongo_config.AnalyticsMongoConfig`

**Note:** These are kept for reference but disabled. Remove when confident in microservice stability.

## Benefits of This Architecture

1. **Separation of Concerns:** Analytics/reporting isolated from business logic
2. **Scalability:** Microservice can scale independently
3. **Performance:** Monolith freed from aggregation overhead
4. **Reliability:** Kafka provides durability and replay capability
5. **Flexibility:** Easy to add new analytics consumers without touching monolith
