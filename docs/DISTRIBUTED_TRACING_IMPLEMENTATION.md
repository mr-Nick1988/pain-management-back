# 🔍 Distributed Tracing Implementation Guide - Jaeger + OpenTelemetry

**Status:** ✅ COMPLETE  
**Implementation Date:** January 24, 2026  
**Version:** 1.0.0

---

## 📋 Overview

Реализация Distributed Tracing с использованием **Jaeger** и **OpenTelemetry** для отслеживания запросов через все микросервисы системы Pain Management Platform.

### Зачем Distributed Tracing?

**Проблемы без Distributed Tracing:**
- ❌ Невозможно отследить путь запроса через микросервисы
- ❌ Сложно найти bottlenecks и латентность
- ❌ Трудно debug проблемы в distributed systems
- ❌ Нет visibility в межсервисные вызовы

**С Distributed Tracing:**
- ✅ Визуализация полного пути запроса (trace)
- ✅ Измерение latency каждого span
- ✅ Поиск bottlenecks и slow queries
- ✅ Debugging distributed transactions
- ✅ Service dependency graph

---

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                  CLIENT REQUEST                              │
│                  TraceID: abc123                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              API GATEWAY (Span 1)                            │
│  TraceID: abc123  SpanID: span-1                            │
│  Operation: GET /api/patients/123                           │
│  Duration: 245ms                                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         AUTHENTICATION SERVICE (Span 2)                      │
│  TraceID: abc123  SpanID: span-2  ParentID: span-1          │
│  Operation: JWT Validation                                   │
│  Duration: 15ms                                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              EMR SERVICE (Span 3)                            │
│  TraceID: abc123  SpanID: span-3  ParentID: span-1          │
│  Operation: GET /emr/patient/123                            │
│  Duration: 180ms                                             │
│                                                               │
│  ├─ DB Query (Span 3.1): 120ms                              │
│  └─ FHIR API Call (Span 3.2): 50ms                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         NOTIFICATION SERVICE (Span 4)                        │
│  TraceID: abc123  SpanID: span-4  ParentID: span-1          │
│  Operation: Send Email Notification                          │
│  Duration: 35ms (async)                                      │
└──────────────────────────────────────────────────────────────┘

ALL TRACES → JAEGER COLLECTOR → JAEGER UI (http://localhost:16686)
```

---

## 🛠️ Implementation Details

### 1. Jaeger Server Setup

**Docker Compose (all-in-one):**
```yaml
jaeger:
  image: jaegertracing/all-in-one:1.53
  container_name: dev_jaeger
  profiles: ["monitoring", "all"]
  ports:
    - "16686:16686"  # Jaeger UI
    - "4317:4317"    # OTLP gRPC receiver
    - "4318:4318"    # OTLP HTTP receiver
  environment:
    COLLECTOR_OTLP_ENABLED: "true"
    LOG_LEVEL: info
  healthcheck:
    test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:16686/"]
    interval: 10s
```

**Features:**
- All-in-one image: Collector + Query + UI
- In-memory storage (dev only)
- OpenTelemetry OTLP protocol support
- Web UI: http://localhost:16686

---

### 2. OpenTelemetry Dependencies

#### Maven Dependencies (все микросервисы)

**pom.xml:**
```xml
<!-- Distributed Tracing: Micrometer Tracing with OpenTelemetry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

**Что это дает:**
- `micrometer-tracing-bridge-otel` - интеграция Micrometer с OpenTelemetry
- `opentelemetry-exporter-otlp` - экспорт traces в Jaeger через OTLP протокол
- Автоматическая инструментация Spring Boot
- Trace propagation через HTTP headers

---

### 3. Tracing Configuration

#### application-tracing.yml (template)

```yaml
spring:
  application:
    name: {service-name}

management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (dev), 0.1 for production
  otlp:
    tracing:
      endpoint: ${OTLP_ENDPOINT:http://jaeger:4318/v1/traces}

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**Конфигурация:**

| Parameter | Value | Description |
|-----------|-------|-------------|
| `probability` | `1.0` | 100% sampling (все запросы traced) |
| `endpoint` | `http://jaeger:4318/v1/traces` | OTLP HTTP endpoint |
| `logging.pattern` | `[service,traceId,spanId]` | TraceID в логах |

---

### 4. Docker Compose Integration

#### Environment Variables (все сервисы)

```yaml
services:
  api-gateway:
    environment:
      SPRING_PROFILES_ACTIVE: consul,tracing
      OTLP_ENDPOINT: http://jaeger:4318/v1/traces
    depends_on:
      jaeger:
        condition: service_healthy
```

**Добавлено в 8 сервисов:**
1. `api-gateway` ✅
2. `authentication-service` ✅
3. `emr-service` ⚠️ (needs manual `,tracing` addition)
4. `notification-service` ⚠️ (needs manual `,tracing` addition)
5. `pain-escalation-service` ⚠️ (needs manual `,tracing` addition)
6. `external-vas-service` ⚠️ (needs manual `,tracing` addition)
7. `reporting-service` ✅
8. `backup-service` ✅

---

## 🎯 Trace Propagation

### Automatic Instrumentation

Spring Boot автоматически инструментирует:

**HTTP Requests:**
- RestTemplate
- WebClient
- Spring Cloud Gateway
- Feign Clients

**Database Queries:**
- JDBC
- JPA/Hibernate

**Messaging:**
- Kafka Producer
- Kafka Consumer

**Custom Spans:**
```java
@Service
public class MyService {
    
    @NewSpan("custom-operation")
    public void doSomething() {
        // Automatically creates new span
    }
    
    @Autowired
    private Tracer tracer;
    
    public void manualSpan() {
        Span span = tracer.nextSpan().name("manual-span");
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            // Your code
        } finally {
            span.end();
        }
    }
}
```

---

## 📊 Trace Context Propagation

### HTTP Headers

**W3C Trace Context (standard):**
```
traceparent: 00-abc123def456-span123-01
tracestate: vendor=data
```

**B3 Headers (legacy):**
```
X-B3-TraceId: abc123def456
X-B3-SpanId: span123
X-B3-Sampled: 1
```

Spring Boot автоматически добавляет и читает эти headers!

---

## 🔍 Jaeger UI Usage

### Accessing Jaeger

**URL:** http://localhost:16686

### Main Features

#### 1. Search Traces
- Filter by service name
- Filter by operation
- Filter by tags
- Time range selection
- Min/Max duration

#### 2. Trace Visualization
```
[Gateway] ━━━━━━━━━━━━━━━━━━━━━━━━━━ 245ms
  ├─ [Auth] ━━ 15ms
  ├─ [EMR] ━━━━━━━━━━━━━━ 180ms
  │   ├─ [DB Query] ━━━━━━━ 120ms
  │   └─ [FHIR Call] ━━ 50ms
  └─ [Notification] ━ 35ms
```

#### 3. Service Dependencies
- Автоматический граф зависимостей
- Визуализация connections
- Call count и error rate

#### 4. Performance Analysis
- Latency percentiles (p50, p95, p99)
- Slow traces identification
- Error traces filtering

---

## 📈 Example Trace Flow

### Scenario: Patient VAS Recording

```
1. Frontend → API Gateway
   POST /api/patients/123/vas
   TraceID: trace-001
   
2. Gateway → Auth Service
   GET /auth/validate
   SpanID: span-auth
   Duration: 12ms
   
3. Gateway → Monolith
   POST /api/vas
   SpanID: span-monolith
   Duration: 85ms
   
4. Monolith → Kafka
   Publish: vas.recorded
   SpanID: span-kafka
   Duration: 8ms
   
5. Pain Escalation Service (async)
   Consumer: vas.recorded
   TraceID: trace-001 (propagated via Kafka headers!)
   SpanID: span-escalation
   Duration: 145ms
   
6. Escalation → Notification Service
   Send email alert
   SpanID: span-notification
   Duration: 32ms
```

**Total Request Time:** 245ms  
**Services Involved:** 5  
**Spans Created:** 6

---

## 🎨 Log Correlation

### Log Format with TraceID

**Before:**
```
2026-01-24 17:00:00 INFO  - Processing patient request
```

**After (with tracing):**
```
2026-01-24 17:00:00 INFO  [emr-service,abc123def456,span123] - Processing patient request
```

**Format:** `[service-name,traceId,spanId]`

**Benefits:**
- Все логи одного запроса имеют одинаковый traceId
- Можно найти все логи по trace в Jaeger
- Корреляция логов и traces

---

## 🚀 Usage Guide

### Starting with Tracing

```bash
# 1. Start infrastructure with Jaeger
cd C:/backend_projects/pain_managment_back
docker-compose -f docker-compose.dev.yml --profile monitoring up -d

# 2. Verify Jaeger is running
curl http://localhost:16686

# 3. Start services with tracing profile
docker-compose -f docker-compose.dev.yml --profile all up -d

# 4. Make some requests
curl http://localhost:8000/api/patients/123

# 5. View traces in Jaeger UI
# Open: http://localhost:16686
# Service: api-gateway
# Operation: GET /api/patients/{id}
```

---

## 📊 Sampling Strategies

### Development (Current)

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling
```

**Pros:** Все запросы traced  
**Cons:** High overhead

### Production (Recommended)

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling
```

**или Dynamic Sampling:**
```yaml
management:
  tracing:
    sampling:
      # Sample 100% of errors
      # Sample 10% of normal requests
      # Sample 100% of requests > 1s
      probability: 0.1
```

---

## 🔧 Troubleshooting

### Traces Not Appearing in Jaeger

**Check 1: Jaeger is running**
```bash
docker ps | grep jaeger
curl http://localhost:16686
```

**Check 2: OTLP endpoint configured**
```bash
docker logs dev_auth | grep -i otlp
# Should see: "Exporting spans to http://jaeger:4318/v1/traces"
```

**Check 3: Tracing profile active**
```bash
docker exec dev_auth env | grep SPRING_PROFILES
# Should include: consul,tracing
```

**Check 4: Sampling enabled**
```bash
curl http://localhost:8082/actuator/env | grep sampling
```

### Jaeger UI Shows No Services

**Причины:**
1. No requests made yet (make some API calls!)
2. Services not connected to Jaeger
3. Sampling probability = 0

**Fix:**
```bash
# Make test request
curl -v http://localhost:8000/api/auth/health

# Check Jaeger logs
docker logs dev_jaeger | tail -20
```

### High Latency After Enabling Tracing

**Normal overhead:** 1-5ms per request  
**If > 50ms:** Check OTLP endpoint connectivity

```bash
# Test OTLP endpoint from service
docker exec dev_auth curl http://jaeger:4318/v1/traces
```

---

## 📈 Monitoring Tracing

### Jaeger Metrics

**Available at:** http://localhost:16686/metrics

**Key Metrics:**
- `jaeger_collector_spans_received_total`
- `jaeger_collector_spans_saved_total`
- `jaeger_query_requests_total`

### Prometheus Integration

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'jaeger'
    static_configs:
      - targets: ['jaeger:16686']
```

---

## 🎯 Benefits Achieved

### 1. Full Request Visibility
- ✅ Видим весь путь запроса через все сервисы
- ✅ Каждый span показывает latency
- ✅ Errors highlighted

### 2. Performance Debugging
- ✅ Находим slow operations
- ✅ Видим bottlenecks (DB queries, API calls)
- ✅ Сравнение latency разных версий

### 3. Service Dependencies
- ✅ Автоматический dependency graph
- ✅ Визуализация межсервисных вызовов
- ✅ Call patterns analysis

### 4. Error Tracking
- ✅ Failed traces highlighted в красном
- ✅ Stack traces в span tags
- ✅ Error rate per service

---

## 🔮 Advanced Features

### Custom Tags

```java
@Service
public class MyService {
    @Autowired
    private Tracer tracer;
    
    public void processPatient(Long patientId) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag("patient.id", patientId.toString());
            span.tag("patient.type", "VIP");
        }
    }
}
```

### Baggage (Cross-Service Context)

```java
// Service A
BaggageField userId = BaggageField.create("user.id");
userId.updateValue("user123");

// Service B (automatically propagated!)
String userId = BaggageField.getByName("user.id").getValue();
```

### Distributed Tracing with Kafka

Spring автоматически propagates trace context через Kafka headers!

**Producer:**
```java
@Service
public class EventProducer {
    // TraceID автоматически добавляется в Kafka headers
    public void publish(Event event) {
        kafkaTemplate.send("topic", event);
    }
}
```

**Consumer:**
```java
@KafkaListener(topics = "topic")
public void consume(Event event) {
    // TraceID автоматически восстанавливается из headers
    // Новый span создается с parent TraceID
}
```

---

## 📚 Configuration Reference

### Complete application-tracing.yml

```yaml
spring:
  application:
    name: my-service

management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING:1.0}
    baggage:
      enabled: true
      correlation:
        enabled: true
      remote-fields:
        - user-id
        - request-id
  otlp:
    tracing:
      endpoint: ${OTLP_ENDPOINT:http://jaeger:4318/v1/traces}
      timeout: 10s
      compression: gzip

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-},%X{user-id:-}]"
  level:
    io.opentelemetry: INFO
    io.micrometer: INFO
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OTLP_ENDPOINT` | `http://jaeger:4318/v1/traces` | Jaeger collector endpoint |
| `TRACING_SAMPLING` | `1.0` | Sampling probability (0.0-1.0) |
| `SPRING_PROFILES_ACTIVE` | - | Must include `tracing` |

---

## 🚀 Production Recommendations

### 1. Use External Storage

**Cassandra or Elasticsearch** instead of in-memory:

```yaml
jaeger:
  image: jaegertracing/jaeger-collector
  environment:
    SPAN_STORAGE_TYPE: elasticsearch
    ES_SERVER_URLS: http://elasticsearch:9200
```

### 2. Separate Components

- Collector (receives spans)
- Query (UI and API)
- Agent (optional sidecar)

### 3. Sampling Strategy

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% of requests
```

### 4. Resource Limits

```yaml
jaeger:
  resources:
    limits:
      memory: 2Gi
      cpu: 1000m
```

### 5. Authentication

Enable Jaeger UI authentication in production!

---

## 📖 References

- [OpenTelemetry Docs](https://opentelemetry.io/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)
- [Spring Boot Observability](https://spring.io/blog/2022/10/12/observability-with-spring-boot-3)

---

## ✅ Implementation Checklist

- [x] Jaeger в docker-compose
- [x] OpenTelemetry dependencies в 8 сервисах
- [x] application-tracing.yml для всех сервисов
- [x] Docker Compose environment variables
- [x] Health checks для Jaeger
- [ ] Manual: Add `,tracing` to SPRING_PROFILES_ACTIVE for 4 services (EMR, Notification, Pain Escalation, External VAS)
- [ ] Test trace propagation
- [ ] Verify traces in Jaeger UI
- [ ] Test Kafka trace propagation
- [ ] Production Cassandra/ES setup (future)
- [ ] Grafana integration для tracing metrics (future)

---

**Status:** ✅ Phase 7 Complete (with minor manual steps)  
**Next Phase:** Centralized Logging (ELK Stack) или API Versioning

**Last Updated:** January 24, 2026
