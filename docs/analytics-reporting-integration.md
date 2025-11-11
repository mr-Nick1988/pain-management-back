# Analytics & Reporting Integration (Monolith ↔ Microservice)

This document describes how the monolith integrates with the Analytics & Reporting microservice using the Strangler Fig pattern.

## What moves out of the monolith
- Ingestion of analytics events (writes to Mongo) → moved to microservice.
- Aggregations (daily/weekly/monthly) Mongo → Postgres → moved to microservice.
- Monolith now only publishes business events and (optionally) proxies reporting reads to the microservice.

## Changes applied in the monolith
- DataAggregationService annotated with `@ConditionalOnProperty(name = "feature.local-aggregation.enabled", havingValue = "true")` so schedulers are disabled by default.
- New `AnalyticsPublisher` publishes events to Kafka topic `analytics-events`; if Kafka is down it falls back to REST `POST /api/analytics/events` on the microservice.
- `application.properties` updated:
  - `feature.local-aggregation.enabled=false`
  - `kafka.topic.analytics-events=analytics-events`
  - `analytics.reporting.base-url=http://localhost:8091`
  - Mongo URIs are environment-driven: `app.mongodb.analytics.uri=${MONGODB_ANALYTICS_URI}` etc.

## Remaining step (automated refactor)
Replace direct `AnalyticsEventRepository.save(...)` calls in `AnalyticsEventListener` with `analyticsPublisher.publish(...)` and swap the imported type. A scripted replace is proposed in the runbook section.

## Microservice contracts
- Kafka topic: `analytics-events` (JSON or Spring-Kafka JSON serialized `AnalyticsEvent`).
- REST fallback (microservice): `POST /api/analytics/events` accepts `AnalyticsEvent`.
- Reporting API (microservice):
  - `POST /api/reporting/aggregate/daily?date=YYYY-MM-DD`
  - `GET /api/reporting/daily/{date}`
  - `POST /api/reporting/aggregate/weekly?weekStart=YYYY-MM-DD&weekEnd=YYYY-MM-DD`
  - `POST /api/reporting/aggregate/monthly?year=YYYY&month=MM`

## Configuration
- Monolith
  - Kafka: `spring.kafka.bootstrap-servers=localhost:9092`
  - Flags: `feature.local-aggregation.enabled=false`
  - Topic: `kafka.topic.analytics-events=analytics-events`
  - Microservice URL: `analytics.reporting.base-url=http://localhost:8091`
- Microservice
  - Mongo (analytics_db) via `ANALYTICS_MONGODB_URI` (Atlas user with readWrite on analytics_db only)
  - Postgres dedicated: e.g., `jdbc:postgresql://reporting-pg:5432/analytics_reporting`
  - Kafka: `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`

## Strangler Fig rollout plan
1. Publish events from monolith to Kafka using `AnalyticsPublisher` (stop direct Mongo writes).
2. Disable monolith schedulers (done via feature flag).
3. Switch UI/backend reads of reporting data to microservice endpoints (optional temporary proxy in monolith if needed).
4. After verification, delete old analytics/reporting code from monolith.

## Testing
1. Start Kafka and the microservice. Ensure microservice can reach Atlas and Postgres.
2. Start monolith with updated properties.
3. Trigger business flows (login, create recommendations, VAS). The monolith should publish events.
4. In microservice, check raw events appear in `analytics_db.analytics_events`.
5. Call `POST /api/reporting/aggregate/daily?date=<yesterday>` then `GET /api/reporting/daily/<yesterday>` to verify aggregates in Postgres.

## Security
- Use dedicated Atlas users with `readWrite` on specific databases (analytics_db/performance_db/backup_db/logging_db).
- Keep all secrets in environment variables (.env is ignored by VCS).

## Runbook: scripted refactor for AnalyticsEventListener
Use the provided PowerShell one-liner (see task comment) to:
- Replace import `AnalyticsEventRepository` → `AnalyticsPublisher`.
- Replace field `private final AnalyticsEventRepository analyticsEventRepository;` → `private final AnalyticsPublisher analyticsPublisher;`.
- Replace `analyticsEventRepository.save(<event>);` → `analyticsPublisher.publish(<event>);`.

After the change, build the monolith with `mvn -DskipTests package` and verify startup.
