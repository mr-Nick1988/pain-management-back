# Analytics-Reporting Microservice Setup Guide

## Quick Start

### 1. Start Infrastructure (Kafka, PostgreSQL, MongoDB)
```bash
docker-compose -f docker-compose-analytics.yml up -d
```

This starts:
- **Kafka** (port 9092) - Event streaming
- **Zookeeper** (port 2181) - Kafka coordination
- **PostgreSQL** (port 5433) - Aggregates storage
- **MongoDB** (port 27017) - Raw events storage

### 2. Verify Infrastructure
```bash
# Check all containers are running
docker ps

# Expected output:
# analytics_kafka
# analytics_zookeeper
# analytics_reporting_pg
# analytics_mongodb
```

### 3. Create Kafka Topic (if not auto-created)
```bash
docker exec -it analytics_kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic analytics-events \
  --partitions 3 \
  --replication-factor 1
```

### 4. Configure Monolith Environment Variables

Create/update `.env` file in monolith root:
```bash
# Database
DB_PASSWORD=your_postgres_password
JWT_SECRET=your_jwt_secret_key_min_256_bits

# MongoDB (Performance & Backup)
MONGODB_PERFORMANCE_URI=mongodb://localhost:27017/performance_db
MONGODB_BACKUP_URI=mongodb://localhost:27017/backup_db

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_ANALYTICS_EVENTS=analytics-events

# Analytics-Reporting Microservice
ANALYTICS_REPORTING_BASE_URL=http://localhost:8091
```

### 5. Start Analytics-Reporting Microservice

**Option A: Using Maven**
```bash
cd /path/to/analytics-reporting-service
mvn clean install
mvn spring-boot:run
```

**Option B: Using JAR**
```bash
cd /path/to/analytics-reporting-service
mvn clean package
java -jar target/analytics-reporting-service-0.0.1-SNAPSHOT.jar
```

**Option C: Using Docker** (if you have Dockerfile)
```bash
cd /path/to/analytics-reporting-service
docker build -t analytics-reporting:latest .
docker run -p 8091:8091 \
  --network analytics-network \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e MONGODB_ANALYTICS_URI=mongodb://mongodb-analytics:27017/analytics_db \
  -e PG_JDBC_URL=jdbc:postgresql://postgres-analytics:5432/analytics_reporting \
  -e PG_USER=analytics \
  -e PG_PASSWORD=analytics \
  analytics-reporting:latest
```

### 6. Start Monolith
```bash
cd /path/to/pain_managment_back
mvn spring-boot:run
```

### 7. Verify Integration

**Check Monolith Health:**
```bash
curl http://localhost:8080/actuator/health
```

**Check Microservice Health:**
```bash
curl http://localhost:8091/actuator/health
```

**Test Event Publishing:**
```bash
# Trigger any business action in monolith (e.g., login)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123"}'

# Check microservice logs for event consumption
docker logs analytics_kafka | grep analytics-events
```

**Query Raw Events:**
```bash
curl "http://localhost:8091/api/analytics/events?start=2025-11-11T00:00:00&end=2025-11-11T23:59:59"
```

**Trigger Manual Aggregation:**
```bash
curl -X POST "http://localhost:8091/api/reporting/aggregate/daily?date=2025-11-11"
```

**Get Daily Aggregate:**
```bash
curl "http://localhost:8091/api/reporting/daily/2025-11-11"
```

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    MONOLITH (Port 8080)                     │
│                                                             │
│  Business Logic → AnalyticsEventListener → AnalyticsPublisher│
└───────────────────────────────┬─────────────────────────────┘
                                │
                                ↓ Publishes Events
                    ┌───────────────────────┐
                    │  Kafka: analytics-events │
                    └───────────┬───────────┘
                                │
                                ↓ Consumes Events
┌─────────────────────────────────────────────────────────────┐
│         ANALYTICS-REPORTING MICROSERVICE (Port 8091)        │
│                                                             │
│  AnalyticsEventConsumer → MongoDB (raw events)             │
│                              ↓                              │
│                    AggregationService                       │
│                              ↓                              │
│                    PostgreSQL (aggregates)                  │
│                              ↓                              │
│                    REST API (queries)                       │
└─────────────────────────────────────────────────────────────┘
```

## Microservice Configuration

### application.properties (Analytics-Reporting)
```properties
server.port=8091
spring.application.name=analytics-reporting-service

# MongoDB (raw events)
spring.data.mongodb.uri=${MONGODB_ANALYTICS_URI}
spring.data.mongodb.auto-index-creation=true

# PostgreSQL (aggregates)
spring.datasource.url=${PG_JDBC_URL:jdbc:postgresql://localhost:5433/analytics_reporting}
spring.datasource.username=${PG_USER:analytics}
spring.datasource.password=${PG_PASSWORD:analytics}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Kafka Consumer
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=analytics-reporting-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false

# Kafka Topics
kafka.topics.analytics-events=${KAFKA_TOPIC_ANALYTICS_EVENTS:analytics-events}
```

## Scheduled Aggregations

The microservice runs automatic aggregations:

| Schedule | Frequency | Description |
|----------|-----------|-------------|
| Daily | 00:30 every day | Aggregates yesterday's data |
| Weekly | 01:00 every Monday | Aggregates previous week (Mon-Sun) |
| Monthly | 02:00 on 1st of month | Aggregates previous month |

## API Endpoints

### Monolith (Port 8080)
All business endpoints remain unchanged. Analytics/reporting endpoints are **disabled**.

### Microservice (Port 8091)

#### Analytics Endpoints
```bash
# Manual event ingestion (fallback)
POST /api/analytics/events
Body: AnalyticsEvent JSON

# Query raw events by time range
GET /api/analytics/events?start={ISO_DATETIME}&end={ISO_DATETIME}
```

#### Reporting Endpoints
```bash
# Trigger daily aggregation
POST /api/reporting/aggregate/daily?date={YYYY-MM-DD}

# Get daily aggregate
GET /api/reporting/daily/{YYYY-MM-DD}

# Trigger weekly aggregation
POST /api/reporting/aggregate/weekly?weekStart={YYYY-MM-DD}&weekEnd={YYYY-MM-DD}

# Trigger monthly aggregation
POST /api/reporting/aggregate/monthly?year={YYYY}&month={MM}
```

## Monitoring & Debugging

### View Kafka Messages
```bash
# Console consumer
docker exec -it analytics_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic analytics-events \
  --from-beginning
```

### Check MongoDB Events
```bash
docker exec -it analytics_mongodb mongosh

use analytics_db
db.analytics_events.find().limit(10).pretty()
db.analytics_events.countDocuments()
```

### Check PostgreSQL Aggregates
```bash
docker exec -it analytics_reporting_pg psql -U analytics -d analytics_reporting

\dt
SELECT * FROM daily_report_aggregate ORDER BY report_date DESC LIMIT 5;
SELECT * FROM weekly_report_aggregate ORDER BY week_start DESC LIMIT 5;
SELECT * FROM monthly_report_aggregate ORDER BY year DESC, month DESC LIMIT 5;
```

### Microservice Logs
```bash
# If running with Maven
tail -f analytics-reporting-service/logs/application.log

# If running with Docker
docker logs -f analytics-reporting-service
```

## Troubleshooting

### Issue: Events not reaching microservice

**Check 1: Kafka is running**
```bash
docker ps | grep kafka
```

**Check 2: Topic exists**
```bash
docker exec -it analytics_kafka kafka-topics --list --bootstrap-server localhost:9092
```

**Check 3: Monolith is publishing**
```bash
# Check monolith logs for:
# "Analytics event published: {eventType}"
```

**Check 4: Microservice is consuming**
```bash
# Check microservice logs for:
# "Ingested event from Kafka topic analytics-events: {eventType}"
```

### Issue: Aggregation not working

**Check 1: Raw events exist in MongoDB**
```bash
docker exec -it analytics_mongodb mongosh
use analytics_db
db.analytics_events.countDocuments()
```

**Check 2: Trigger manual aggregation**
```bash
curl -X POST "http://localhost:8091/api/reporting/aggregate/daily?date=2025-11-11"
```

**Check 3: Verify PostgreSQL connection**
```bash
docker exec -it analytics_reporting_pg psql -U analytics -d analytics_reporting -c "SELECT 1;"
```

### Issue: Port conflicts

If ports are already in use:

**Kafka (9092):**
```yaml
# In docker-compose-analytics.yml, change:
ports:
  - "9093:9092"  # Use 9093 instead
# Update KAFKA_BOOTSTRAP_SERVERS in both services
```

**PostgreSQL (5433):**
```yaml
# In docker-compose-analytics.yml, change:
ports:
  - "5434:5432"  # Use 5434 instead
# Update PG_JDBC_URL accordingly
```

**MongoDB (27017):**
```yaml
# In docker-compose-analytics.yml, change:
ports:
  - "27018:27017"  # Use 27018 instead
# Update MONGODB_ANALYTICS_URI accordingly
```

## Performance Tuning

### Kafka Producer (Monolith)
Already optimized in `KafkaProducerConfig.java`:
- Idempotent producer enabled
- Compression: snappy
- Batch size: 16KB
- Linger: 10ms

### Kafka Consumer (Microservice)
Adjust in `application.properties`:
```properties
spring.kafka.consumer.max-poll-records=500
spring.kafka.consumer.fetch-min-size=1024
spring.kafka.consumer.fetch-max-wait=500
```

### MongoDB Indexes
Automatically created on:
- `timestamp`
- `eventType`
- `userId`
- `diagnosisCodes`

### PostgreSQL Indexes
Automatically created on:
- `report_date` (daily_report_aggregate)
- `week_start`, `week_end` (weekly_report_aggregate)
- `year`, `month` (monthly_report_aggregate)

## Backup & Recovery

### Backup MongoDB Events
```bash
docker exec analytics_mongodb mongodump \
  --db=analytics_db \
  --out=/backup/$(date +%Y%m%d)
```

### Backup PostgreSQL Aggregates
```bash
docker exec analytics_reporting_pg pg_dump \
  -U analytics analytics_reporting > backup_$(date +%Y%m%d).sql
```

### Restore from Kafka (Replay Events)
```bash
# Reset consumer group to beginning
docker exec -it analytics_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analytics-reporting-group \
  --reset-offsets --to-earliest \
  --topic analytics-events \
  --execute
```

## Scaling

### Horizontal Scaling (Multiple Microservice Instances)
```bash
# Run multiple instances on different ports
java -jar analytics-reporting.jar --server.port=8091
java -jar analytics-reporting.jar --server.port=8092
java -jar analytics-reporting.jar --server.port=8093

# Kafka will automatically distribute partitions
```

### Kafka Partitioning
```bash
# Increase partitions for better parallelism
docker exec -it analytics_kafka kafka-topics --alter \
  --bootstrap-server localhost:9092 \
  --topic analytics-events \
  --partitions 6
```

## Security Considerations

### Production Checklist
- [ ] Enable Kafka authentication (SASL/SSL)
- [ ] Secure MongoDB with authentication
- [ ] Use PostgreSQL with strong passwords
- [ ] Enable SSL/TLS for all connections
- [ ] Restrict network access (firewall rules)
- [ ] Use secrets management (Vault, AWS Secrets Manager)
- [ ] Enable Spring Security on microservice endpoints
- [ ] Implement API authentication (JWT, OAuth2)

## Next Steps

1. ✅ Infrastructure running
2. ✅ Microservice deployed
3. ✅ Monolith publishing events
4. ⬜ Set up monitoring (Prometheus, Grafana)
5. ⬜ Configure alerting (PagerDuty, Slack)
6. ⬜ Implement backup automation
7. ⬜ Load testing
8. ⬜ Production deployment

## Support

For issues or questions:
1. Check logs (monolith, microservice, Kafka)
2. Review `ANALYTICS_MICROSERVICE_INTEGRATION.md`
3. Verify environment variables
4. Test with manual API calls
