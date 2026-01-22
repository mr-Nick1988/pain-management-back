# Reporting Service (SQL‑only)

A Spring Boot microservice responsible for generating, storing, and serving daily reporting KPIs from SQL sources. The service consumes Kafka commands, runs a nightly scheduler, persists daily aggregates to Postgres, and exposes REST APIs for data access, exporting (Excel/PDF), and email delivery.

This service is fully SQL‑only. All legacy MongoDB ingestion, weekly/monthly artifacts, and legacy controllers were removed.

---

## Key Features

- Daily KPI aggregation from SQL tables (no MongoDB).
- Kafka command consumer for on‑demand generation:
  - GENERATE_DAILY, GENERATE_YESTERDAY, GENERATE_PERIOD (+ regenerate flag)
- Nightly scheduler (00:30) to generate yesterday's report.
- REST APIs for:
  - Fetching daily reports (by date, period, recent), summary KPIs
  - Exporting daily report as Excel/PDF
  - Emailing daily report and period summary
- Postgres persistence in table `daily_report_aggregate`.

---

## Architecture

- Aggregation: `SQLAggregationService`
  - Queries application SQL tables: `patient`, `vas`, `recommendation`, `pain_escalation`, `login_event`.
  - Builds `DailyReportAggregate` and persists via JPA repository.
- Kafka: `ReportingCommandConsumer`
  - Subscribes to `reporting-commands` topic
  - Triggers `SQLAggregationService.generateReportForDate(..)`
- Scheduler: `DailyReportScheduler`
  - Cron `0 30 0 * * *` → generate report for `LocalDate.now().minusDays(1)`
- REST Controllers:
  - `ReportStatisticsController` — daily/period/recent/summary endpoints and manual generation
  - `ExcelExportController` / `PdfExportController` — export daily report
  - `EmailReportController` — email daily report and period summary
- Storage: `daily_report_aggregate` (JPA entity `DailyReportAggregate`)

---

## Data Model: DailyReportAggregate

Required fields the service produces and stores:

- reportDate
- totalPatientsRegistered, totalVasRecords, averageVasLevel, criticalVasCount
- totalRecommendations, approvedRecommendations, rejectedRecommendations, approvalRate
- totalEscalations
- totalLogins, successfulLogins, failedLogins, uniqueActiveUsers
- topDrugsJson
- createdAt, createdBy

Important: `totalLogins = successfulLogins + failedLogins`.

---

## REST API

Base path: `/api/reports`

- Get daily reports by period
  - `GET /daily?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
- Get recent reports
  - `GET /recent?limit=30`
- Get daily report by date
  - `GET /daily/{date}`
- Get period summary KPIs
  - `GET /summary?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
- Generate daily report for date
  - `POST /daily/{date}/generate?regenerate=false`

Exports and Email:

- Excel: `GET /daily/{date}/export/excel`
  - Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  - `Content-Disposition: attachment; filename=daily_report_{date}.xlsx`
- PDF: `GET /daily/{date}/export/pdf`
  - Content-Type: `application/pdf`
  - `Content-Disposition: attachment; filename=daily_report_{date}.pdf`
- Email daily report:
  - `POST /daily/{date}/email?email=...&attachPdf=true&attachExcel=true`
- Email period summary:
  - `POST /email/summary?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&email=...`

### cURL Examples

Fetch period:
```bash
curl "http://localhost:8091/api/reports/daily?startDate=2025-11-01&endDate=2025-11-10"
```
Generate for date:
```bash
curl -X POST "http://localhost:8091/api/reports/daily/2025-11-18/generate?regenerate=true"
```
Download PDF:
```bash
curl -OJ "http://localhost:8091/api/reports/daily/2025-11-18/export/pdf"
```

---

## Kafka

Topic: `reporting-commands` (configurable via `kafka.topics.reporting-commands`).

Message schema (`ReportingCommand`):
```json
{
  "action": "GENERATE_DAILY | GENERATE_YESTERDAY | GENERATE_PERIOD",
  "date": "YYYY-MM-DD",
  "regenerate": true,
  "startDate": "YYYY-MM-DD",
  "endDate": "YYYY-MM-DD"
}
```

Examples:
- Generate by date
```json
{ "action": "GENERATE_DAILY", "date": "2025-11-18", "regenerate": true }
```
- Generate yesterday
```json
{ "action": "GENERATE_YESTERDAY", "regenerate": false }
```
- Generate for period (inclusive)
```json
{ "action": "GENERATE_PERIOD", "startDate": "2025-11-01", "endDate": "2025-11-10", "regenerate": true }
```

---

## Configuration

Primary properties (env‑driven):

- Server
  - `server.port` → default: 8091
- Datasource (Postgres)
  - Preferred: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
  - Alternatively via `PG_JDBC_URL`, `PG_USER`, `PG_PASSWORD` (as used in `application-local.yml`)
- JPA
  - `spring.jpa.hibernate.ddl-auto=update`, `show-sql=false`
- Kafka
  - `KAFKA_BOOTSTRAP_SERVERS`
  - `KAFKA_GROUP_ID` (default: `reporting-service-group`)
  - `KAFKA_TOPIC_REPORTING_COMMANDS` (default: `reporting-commands`)
- Mail (optional)
  - `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`
  - TLS/STARTTLS flags as needed
- Actuator
  - Health/metrics/info exposed; see `application-local.yml`

Sample `application-local.yml` keys:
```yaml
spring:
  datasource:
    url: ${PG_JDBC_URL:jdbc:postgresql://localhost:5433/analytics_reporting}
    username: ${PG_USER:analytics}
    password: ${PG_PASSWORD:analytics}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${KAFKA_GROUP_ID:reporting-service-group}

kafka:
  topics:
    reporting-commands: ${KAFKA_TOPIC_REPORTING_COMMANDS:reporting-commands}
```

---

## Build & Run

Requirements: Java 21, Maven, Postgres, Kafka.

- Build:
```bash
mvn -DskipTests package
```
- Run (local Spring Boot):
```bash
java -jar target/analytics_reporting_service-0.0.1-SNAPSHOT.jar
```
- Docker Compose (example variables for service container):
```
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_TOPIC_REPORTING_COMMANDS=reporting-commands
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-analytics:5432/analytics_reporting
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
SPRING_PROFILES_ACTIVE=local
```

Nightly scheduler and Kafka listener should appear in logs on startup.

---

## Monolith Integration

- Monolith publishes `ReportingCommand` messages into `reporting-commands`.
- REST endpoints under `/api/reports` are consumed by Monolith/frontend for UI and exports.
- Monolith’s own nightly scheduler is disabled (generation is delegated to this microservice).
- Storage: this service writes to `daily_report_aggregate` and serves it.

---

## Frontend Notes

- Use only `/api/reports` endpoints (daily, recent, summary, generate, exports, email).
- File downloads must handle `Content-Disposition` to set file names.
- Centralize base URLs (avoid hardcoded URLs). If a gateway/monolith is used, ensure cookies are sent (e.g., `credentials: "include"`).
- Remove or hide any legacy UI relying on `/api/analytics/events` (Mongo).

---

## Breaking Changes & Migration

- Removed Mongo ingestion and all Mongo entities/repositories/controllers/listeners.
- Removed weekly/monthly entities/repositories/controllers — derive analytics from daily as needed.
- `AggregationService` (legacy, Mongo→PG) removed; replaced by `SQLAggregationService`.
- Endpoints standardized under `/api/reports` (no `/api/reporting`).

---

## Health & Troubleshooting

- Health: `GET /actuator/health` (exposed by actuator)
- Verify:
  - DB connectivity (datasource URL/creds)
  - Kafka connectivity (bootstrap servers, topic exists)
  - Mail settings for email flows
- Common issues:
  - No reports generated → check scheduler time zone, Kafka messages, DB permissions.
  - Empty exports/emails → ensure a report exists for requested `date`.

---

## Tech Stack

- Java 21, Spring Boot (Web, Data JPA, Validation, Actuator, Mail, Kafka)
- Postgres (runtime), Jackson
- Exports: Apache POI (Excel), PDFBox (PDF)
- Lombok (optional)

---

## License
Internal project documentation.
