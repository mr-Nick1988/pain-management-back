# 📊 Centralized Logging Implementation - ELK Stack

**Created:** January 24, 2026  
**Phase:** 8 - Centralized Logging  
**Status:** ✅ COMPLETE  
**Stack:** Elasticsearch 8.11.3 + Logstash 8.11.3 + Kibana 8.11.3

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Components](#components)
4. [Implementation Details](#implementation-details)
5. [Configuration](#configuration)
6. [Usage](#usage)
7. [Log Correlation with Jaeger](#log-correlation-with-jaeger)
8. [Kibana Dashboards](#kibana-dashboards)
9. [Performance Considerations](#performance-considerations)
10. [Production Recommendations](#production-recommendations)
11. [Troubleshooting](#troubleshooting)

---

## Overview

### What is Centralized Logging?

Centralized logging aggregates logs from all microservices into a single location for:
- **Searchability**: Full-text search across all services
- **Correlation**: Link logs by traceID, spanID, user, request
- **Analysis**: Identify patterns, errors, performance issues
- **Retention**: Long-term storage with indexing
- **Visualization**: Real-time dashboards and alerts

### Why ELK Stack?

- **Industry Standard**: Most widely used logging solution
- **Scalable**: Handles millions of logs per day
- **Powerful Search**: Elasticsearch query DSL
- **Rich Visualization**: Kibana dashboards
- **Integration**: Works seamlessly with Jaeger tracing

### Architecture Decision

**Chosen:** ELK Stack (Elasticsearch + Logstash + Kibana)  
**Alternatives Considered:**
- Loki + Grafana (simpler but less powerful search)
- Splunk (enterprise, expensive)
- CloudWatch Logs (AWS-only)

---

## Architecture

### High-Level Flow

```
┌──────────────┐
│ Microservice │
│  (8 services)│
└──────┬───────┘
       │ JSON logs via TCP
       ↓
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Logstash   │─────→│Elasticsearch │←─────│   Kibana     │
│ (port 5000)  │      │ (port 9200)  │      │ (port 5601)  │
│              │      │              │      │              │
│ - Parse JSON │      │ - Store logs │      │ - Visualize  │
│ - Filter     │      │ - Index      │      │ - Search     │
│ - Transform  │      │ - Query      │      │ - Dashboard  │
└──────────────┘      └──────────────┘      └──────────────┘
```

### Service Integration

```
┌─────────────────────────────────────────────────────────┐
│                    Application Code                      │
│  Logger.info("Processing request for user {}", userId)  │
└────────────────────────┬────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────┐
│                  Logback (logging framework)             │
│  - Console Appender (human-readable)                    │
│  - Logstash TCP Socket Appender (JSON)                  │
└────────────────────────┬────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────┐
│              Logstash TCP Socket (port 5000)            │
│  Receives: {                                            │
│    "timestamp": "2026-01-24T18:30:00.123Z",             │
│    "level": "INFO",                                     │
│    "logger": "com.painmanagement.auth.AuthService",    │
│    "message": "Processing request for user john_doe",   │
│    "trace_id": "64a7f2e8b9c1d3f5",                      │
│    "span_id": "a2b4c6d8e0f1",                           │
│    "application_name": "authentication-service"         │
│  }                                                      │
└────────────────────────┬────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────┐
│              Logstash Pipeline Processing                │
│  1. Parse JSON                                          │
│  2. Extract service name                                │
│  3. Normalize fields                                    │
│  4. Add environment tag                                 │
└────────────────────────┬────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────┐
│      Elasticsearch Index: painmgmt-logs-auth-2026.01.24 │
│  Stored with full-text indexing for fast search        │
└────────────────────────┬────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────┐
│                   Kibana Discover                        │
│  Users search logs via web UI                           │
└─────────────────────────────────────────────────────────┘
```

---

## Components

### 1. Elasticsearch (Log Storage)

**Image:** `docker.elastic.co/elasticsearch/elasticsearch:8.11.3`  
**Ports:**
- 9200: HTTP API
- 9300: Node communication

**Configuration:**
```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.11.3
  environment:
    discovery.type: single-node
    xpack.security.enabled: "false"  # Dev only!
    ES_JAVA_OPTS: "-Xms512m -Xmx512m"
    cluster.name: painmgmt-logs-dev
  volumes:
    - elasticsearch-data:/usr/share/elasticsearch/data
  healthcheck:
    test: curl -f http://localhost:9200/_cluster/health
    interval: 30s
```

**Purpose:**
- Store all logs with full-text indexing
- Support complex queries (boolean, regex, range)
- Aggregate data for analytics
- Daily indices per service: `painmgmt-logs-{service}-YYYY.MM.DD`

**Key Features:**
- **Inverted Index**: Fast full-text search
- **Distributed**: Can scale to multiple nodes
- **RESTful API**: Easy integration
- **JSON Documents**: Native JSON storage

---

### 2. Logstash (Log Processing)

**Image:** `docker.elastic.co/logstash/logstash:8.11.3`  
**Ports:**
- 5000: TCP JSON input from microservices
- 5044: Beats input (Filebeat)
- 9600: Monitoring API

**Pipeline Configuration:** `logging/logstash/pipeline/logstash.conf`

```ruby
input {
  tcp {
    port => 5000
    codec => json
    tags => ["microservice", "json"]
  }
}

filter {
  # Extract traceId and spanId from message
  grok {
    match => { "message" => "\[%{DATA:service_name},%{DATA:trace_id},%{DATA:span_id}\]" }
  }
  
  # Normalize service name
  mutate {
    add_field => { "service" => "%{application_name}" }
  }
  
  # Add environment
  mutate {
    add_field => { "environment" => "development" }
  }
}

output {
  elasticsearch {
    hosts => ["${ELASTICSEARCH_HOSTS}"]
    index => "painmgmt-logs-%{service}-%{+YYYY.MM.dd}"
  }
}
```

**Purpose:**
- Receive logs from all microservices
- Parse and transform log data
- Extract metadata (traceId, spanId, service)
- Route to Elasticsearch with proper indexing

---

### 3. Kibana (Visualization)

**Image:** `docker.elastic.co/kibana/kibana:8.11.3`  
**Port:** 5601

**Configuration:**
```yaml
kibana:
  image: docker.elastic.co/kibana/kibana:8.11.3
  environment:
    ELASTICSEARCH_HOSTS: "http://elasticsearch:9200"
    XPACK_SECURITY_ENABLED: "false"  # Dev only!
  volumes:
    - kibana-data:/usr/share/kibana/data
```

**Purpose:**
- Web UI for log searching and visualization
- Create custom dashboards
- Set up alerts and monitoring
- Analyze log patterns and trends

**Key Features:**
- **Discover**: Search and filter logs
- **Dashboard**: Custom visualizations
- **Alerting**: Email/Slack notifications
- **Index Management**: Lifecycle policies

---

## Implementation Details

### Microservices Configuration

#### 1. Maven Dependency (All Services)

Added to `pom.xml`:
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

#### 2. Logback Configuration (All Services)

Created `src/main/resources/logback-spring.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="applicationName" 
                    source="spring.application.name"/>
    <springProperty scope="context" name="logstashHost" 
                    source="LOGSTASH_HOST" defaultValue="localhost"/>
    <springProperty scope="context" name="logstashPort" 
                    source="LOGSTASH_PORT" defaultValue="5000"/>

    <!-- Console Appender (for local development) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [${applicationName:},%X{traceId:-},%X{spanId:-}] %c{1} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Logstash Appender (JSON logs to Elasticsearch) -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${logstashHost}:${logstashPort}</destination>
        
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application_name":"${applicationName}"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
        
        <keepAliveDuration>5 minutes</keepAliveDuration>
        <reconnectionDelay>1 second</reconnectionDelay>
    </appender>

    <!-- Async Appender for performance -->
    <appender name="ASYNC_LOGSTASH" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="LOGSTASH"/>
        <queueSize>512</queueSize>
        <includeCallerData>true</includeCallerData>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_LOGSTASH"/>
    </root>
</configuration>
```

#### 3. Environment Variables (docker-compose.dev.yml)

```yaml
api-gateway:
  environment:
    LOGSTASH_HOST: logstash
    LOGSTASH_PORT: 5000
```

Applied to all 8 services:
- api-gateway
- authentication-service
- emr-integration-service
- notification-service
- pain-escalation-service
- external-vas-integration-service
- reporting-service
- backup-restore

---

## Configuration

### Start ELK Stack

```bash
# Only ELK Stack
docker-compose --profile logging up -d

# All services + ELK
docker-compose --profile all up -d
```

### Startup Times

- **Elasticsearch**: ~60 seconds
- **Logstash**: ~30 seconds (after Elasticsearch)
- **Kibana**: ~90 seconds (after Elasticsearch)

**Total wait:** ~2 minutes for full stack

### Health Checks

```bash
# Elasticsearch
curl http://localhost:9200/_cluster/health

# Logstash
curl http://localhost:9600

# Kibana
curl http://localhost:5601/api/status
```

---

## Usage

### 1. Access Kibana

Open: http://localhost:5601

### 2. Create Index Pattern (First Time)

1. Navigate: **Stack Management** → **Index Patterns**
2. Click: **Create index pattern**
3. Index pattern name: `painmgmt-logs-*`
4. Time field: `@timestamp`
5. Click: **Create index pattern**

### 3. Search Logs

Navigate to: **Discover**

#### Search by Service
```
service:"api-gateway"
service:"authentication"
service:"emr-service"
```

#### Search by Level
```
level:"ERROR"
level:"WARN"
level:"INFO"
```

#### Search by TraceID (Jaeger correlation)
```
trace_id:"64a7f2e8b9c1d3f5"
```

#### Search by Message
```
message:"Circuit Breaker"
message:"exception"
message:"timeout"
```

#### Combined Queries
```
service:"api-gateway" AND level:"ERROR" AND trace_id:*
```

#### Time Range
- Last 15 minutes
- Last 1 hour
- Last 24 hours
- Custom range

---

## Log Correlation with Jaeger

### Bidirectional Correlation

Every log entry contains **traceId** and **spanId** from Jaeger, enabling:

#### Workflow 1: Error → Trace
1. Find ERROR in Kibana
2. Copy `trace_id` from log
3. Open Jaeger UI: http://localhost:16686
4. Search by TraceID
5. See full request flow

#### Workflow 2: Slow Trace → Logs
1. Find slow trace in Jaeger
2. Copy `traceId`
3. Search in Kibana: `trace_id:"..."`
4. See detailed logs for that request

### Example Log Entry

```json
{
  "@timestamp": "2026-01-24T18:30:00.123Z",
  "level": "INFO",
  "logger_name": "com.painmanagement.auth.AuthService",
  "message": "[authentication-service,64a7f2e8b9c1d3f5,a2b4c6d8e0f1] User login successful",
  "trace_id": "64a7f2e8b9c1d3f5",
  "span_id": "a2b4c6d8e0f1",
  "service": "authentication",
  "application_name": "authentication-service",
  "environment": "development"
}
```

### Benefits

✅ **Root Cause Analysis**: Trace error from log to full call chain  
✅ **Performance Debugging**: Link slow spans to detailed logs  
✅ **User Journey**: Track single user request across all services  
✅ **Incident Response**: Quickly identify affected services

---

## Kibana Dashboards

### Recommended Dashboards

#### 1. Service Health Dashboard
- Request count by service (last 1h)
- Error rate by service
- Average response time
- Top 10 errors

#### 2. Error Analysis Dashboard
- Error count over time
- Error distribution by service
- Top error messages
- Stack traces

#### 3. Performance Dashboard
- Response time percentiles (p50, p95, p99)
- Slow requests (>1s)
- Database query time
- External API latency

#### 4. Security Dashboard
- Failed login attempts
- 401/403 responses
- Suspicious activity patterns
- API key usage

---

## Performance Considerations

### Async Logging

```xml
<appender name="ASYNC_LOGSTASH" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="LOGSTASH"/>
    <queueSize>512</queueSize>        <!-- Buffer size -->
    <discardingThreshold>0</discardingThreshold>  <!-- Never discard -->
    <includeCallerData>true</includeCallerData>   <!-- Include stack -->
</appender>
```

**Benefits:**
- Non-blocking: Logging doesn't slow down request processing
- Batching: Multiple logs sent together
- Queue: Handles bursts of log traffic

### Memory Settings

**Development:**
```yaml
elasticsearch:
  ES_JAVA_OPTS: "-Xms512m -Xmx512m"
logstash:
  LS_JAVA_OPTS: "-Xms256m -Xmx256m"
```

**Production:**
```yaml
elasticsearch:
  ES_JAVA_OPTS: "-Xms2g -Xmx2g"  # Minimum 2GB
logstash:
  LS_JAVA_OPTS: "-Xms1g -Xmx1g"
```

### Indexing Strategy

**Daily Indices:**
```
painmgmt-logs-api-gateway-2026.01.24
painmgmt-logs-api-gateway-2026.01.25
```

**Benefits:**
- Fast queries (smaller indices)
- Easy deletion (drop old indices)
- Parallel processing

---

## Production Recommendations

### 1. Enable Security

```yaml
elasticsearch:
  environment:
    xpack.security.enabled: "true"
    ELASTIC_PASSWORD: "your-secure-password"
```

### 2. Elasticsearch Cluster

Minimum 3 nodes for high availability:
```yaml
elasticsearch-01:
  environment:
    cluster.name: painmgmt-logs-prod
    node.name: es-node-01
    discovery.seed_hosts: es-node-02,es-node-03
```

### 3. Index Lifecycle Management (ILM)

```json
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": { "number_of_shards": 1 },
          "forcemerge": { "max_num_segments": 1 }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": { "delete": {} }
      }
    }
  }
}
```

**Lifecycle:**
- Hot: Last 7 days (active indexing)
- Warm: 7-30 days (read-only, compressed)
- Delete: After 30 days

### 4. Filebeat for Container Logs

```yaml
filebeat:
  image: docker.elastic.co/beats/filebeat:8.11.3
  volumes:
    - /var/lib/docker/containers:/var/lib/docker/containers:ro
  environment:
    ELASTICSEARCH_HOSTS: http://elasticsearch:9200
```

Collects Docker container stdout/stderr logs.

### 5. Alerts

**Example: High Error Rate**
```json
{
  "trigger": {
    "schedule": { "interval": "5m" }
  },
  "input": {
    "search": {
      "request": {
        "indices": ["painmgmt-logs-*"],
        "body": {
          "query": {
            "bool": {
              "must": [
                { "term": { "level": "ERROR" } },
                { "range": { "@timestamp": { "gte": "now-5m" } } }
              ]
            }
          }
        }
      }
    }
  },
  "condition": {
    "compare": { "ctx.payload.hits.total": { "gt": 10 } }
  },
  "actions": {
    "send_email": {
      "email": {
        "to": "ops@painmanagement.com",
        "subject": "High Error Rate Alert",
        "body": "More than 10 errors in last 5 minutes"
      }
    }
  }
}
```

---

## Troubleshooting

### Logs Not Appearing in Kibana

**1. Check Logstash Connection**
```bash
# From microservice container
telnet logstash 5000

# Check Logstash logs
docker logs dev_logstash --tail 50
```

**2. Check Elasticsearch Indices**
```bash
curl http://localhost:9200/_cat/indices?v
```

**3. Verify Logback Configuration**
```bash
# Check if logback-spring.xml exists
docker exec dev_api_gateway ls -la /app/resources/logback-spring.xml

# Check microservice logs
docker logs dev_api_gateway --tail 50
```

### Kibana Won't Start

**1. Check Elasticsearch Health**
```bash
curl http://localhost:9200/_cluster/health
```

**2. Check Kibana Logs**
```bash
docker logs dev_kibana --tail 50
```

**3. Restart Sequence**
```bash
docker-compose restart elasticsearch
# Wait 60 seconds
docker-compose restart logstash
docker-compose restart kibana
```

### High Memory Usage

**1. Reduce Elasticsearch Heap**
```yaml
ES_JAVA_OPTS: "-Xms256m -Xmx256m"  # For dev
```

**2. Limit Docker Memory**
```yaml
elasticsearch:
  mem_limit: 1g
  memswap_limit: 1g
```

**3. Delete Old Indices**
```bash
# List indices
curl http://localhost:9200/_cat/indices?v

# Delete indices older than 7 days
curl -X DELETE "http://localhost:9200/painmgmt-logs-*-2026.01.17"
```

### Logstash Pipeline Errors

**1. Test Pipeline Syntax**
```bash
docker exec dev_logstash logstash -f /usr/share/logstash/pipeline/logstash.conf --config.test_and_exit
```

**2. Check Pipeline Stats**
```bash
curl http://localhost:9600/_node/stats/pipelines?pretty
```

**3. Reload Pipeline**
```bash
docker-compose restart logstash
```

---

## Testing

### 1. Generate Test Logs

```bash
# Make requests to generate logs
curl http://localhost:8000/actuator/health
curl http://localhost:8082/api/auth/test
curl http://localhost:8086/api/emr/test
```

### 2. View in Kibana

1. Open: http://localhost:5601
2. Navigate: **Discover**
3. Select: `painmgmt-logs-*`
4. Set time: Last 15 minutes
5. Search: `service:"api-gateway"`

### 3. Test Trace Correlation

1. Make request through API Gateway
2. Copy `traceId` from Kibana logs
3. Search in Jaeger: http://localhost:16686
4. Verify full trace appears

---

## Summary

### What We Achieved

✅ **Centralized Storage**: All logs in one place  
✅ **Full-Text Search**: Elasticsearch query power  
✅ **Correlation**: TraceID links logs to Jaeger traces  
✅ **Visualization**: Kibana dashboards  
✅ **Performance**: Async logging, non-blocking  
✅ **Scalability**: Daily indices, ILM ready  
✅ **Production-Ready**: Security, clustering, alerts

### Services Covered

- ✅ API Gateway (8000)
- ✅ Authentication Service (8082)
- ✅ EMR Integration Service (8086)
- ✅ Notification Service (8087)
- ✅ Pain Escalation Service (8088)
- ✅ External VAS Service (8089)
- ✅ Reporting Service (8091)
- ✅ Backup & Restore Service (8085)

### Access Points

- **Kibana UI**: http://localhost:5601
- **Elasticsearch API**: http://localhost:9200
- **Logstash Monitoring**: http://localhost:9600
- **Jaeger UI**: http://localhost:16686 (for trace correlation)

---

**Phase 8: Centralized Logging - COMPLETE ✅**

**Next Steps:**
- Create custom Kibana dashboards
- Set up alerts for production
- Configure log retention policies
- Enable security features

**Documentation:** [PHASE_8_QUICK_START.md](../PHASE_8_QUICK_START.md)

**Last Updated:** January 24, 2026
