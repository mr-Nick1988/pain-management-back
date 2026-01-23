# 🔍 Service Discovery Implementation Guide - HashiCorp Consul

**Status:** ✅ COMPLETE  
**Implementation Date:** January 23, 2026  
**Version:** 1.0.0

---

## 📋 Overview

Реализация Service Discovery с использованием **HashiCorp Consul** для динамической регистрации и обнаружения микросервисов в системе Pain Management Platform.

### Зачем Service Discovery?

**Проблемы без Service Discovery:**
- ❌ Hardcoded URLs в конфигурациях
- ❌ Невозможность горизонтального масштабирования
- ❌ Ручное управление адресами сервисов
- ❌ Нет автоматического failover

**С Service Discovery:**
- ✅ Динамическая регистрация сервисов
- ✅ Автоматическое обнаружение инстансов
- ✅ Load balancing между репликами
- ✅ Health-based routing
- ✅ Горизонтальное масштабирование

---

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                  CONSUL SERVER                           │
│              (port 8500 UI, 8600 DNS)                   │
│                                                           │
│  Service Registry:                                       │
│  ┌──────────────────┬──────────────┬─────────┬────────┐ │
│  │ Service Name     │ Instance     │ Status  │ Tags   │ │
│  ├──────────────────┼──────────────┼─────────┼────────┤ │
│  │ api-gateway      │ 172.18.0.10  │ HEALTHY │ v1.0.0 │ │
│  │ auth-service     │ 172.18.0.11  │ HEALTHY │ v1.0.0 │ │
│  │ emr-service      │ 172.18.0.12  │ HEALTHY │ v1.0.0 │ │
│  │ notification     │ 172.18.0.13  │ HEALTHY │ v1.0.0 │ │
│  │ pain-escalation  │ 172.18.0.14  │ HEALTHY │ v1.0.0 │ │
│  │ external-vas     │ 172.18.0.15  │ HEALTHY │ v1.0.0 │ │
│  │ reporting        │ 172.18.0.16  │ HEALTHY │ v1.0.0 │ │
│  │ backup-service   │ 172.18.0.17  │ HEALTHY │ v1.0.0 │ │
│  └──────────────────┴──────────────┴─────────┴────────┘ │
└────────────┬────────────────────────────────────────────┘
             │
             │ 1. Register on startup
             │ 2. Health checks every 10s
             │ 3. Query for service locations
             │
┌────────────▼────────────────────────────────────────────┐
│                   API GATEWAY                            │
│  - Queries Consul for service instances                 │
│  - Load balances: lb://authentication-service           │
│  - Routes only to HEALTHY instances                     │
└────────────┬────────────────────────────────────────────┘
             │
             │ HTTP Requests
             │
┌────────────▼────────────────────────────────────────────┐
│              MICROSERVICES (8 services)                  │
│  Each service:                                           │
│  - Auto-registers with Consul on startup                │
│  - Sends health checks to /actuator/health              │
│  - Updates metadata (tags, version)                     │
│  - Auto-deregisters on shutdown                         │
└──────────────────────────────────────────────────────────┘
```

---

## 🛠️ Implementation Details

### 1. Consul Server Setup

**Docker Compose:**
```yaml
consul:
  image: hashicorp/consul:1.17
  container_name: dev_consul
  hostname: consul
  ports:
    - "8500:8500"   # HTTP API + Web UI
    - "8600:8600/udp"  # DNS interface
  command: agent -server -bootstrap -ui -client=0.0.0.0 -bind=0.0.0.0
  environment:
    CONSUL_BIND_INTERFACE: eth0
  volumes:
    - consul-data:/consul/data
  healthcheck:
    test: ["CMD", "consul", "members"]
    interval: 10s
    timeout: 3s
    retries: 3
    start_period: 10s
```

**Features:**
- Single-node setup (dev environment)
- Web UI доступен: http://localhost:8500/ui
- DNS interface для service discovery через DNS
- Persistent storage в volume

---

### 2. Service Registration

#### Spring Cloud Consul Dependencies

**pom.xml (все микросервисы):**
```xml
<properties>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
</properties>

<dependencies>
    <!-- Consul Service Discovery -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-consul-discovery</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### Configuration

**application-consul.yml (template для всех сервисов):**
```yaml
spring:
  application:
    name: {service-name}  # authentication-service, emr-service, etc.
  cloud:
    consul:
      host: ${CONSUL_HOST:consul}
      port: ${CONSUL_PORT:8500}
      discovery:
        enabled: true
        register: true
        prefer-ip-address: true
        instance-id: ${spring.application.name}:${random.value}
        service-name: ${spring.application.name}
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-critical-timeout: 30s
        tags:
          - "version=1.0.0"
          - "profile=${spring.profiles.active:default}"
          - "secure=false"
```

**Key Configuration Parameters:**

| Parameter | Value | Description |
|-----------|-------|-------------|
| `enabled` | `true` | Включает Consul discovery |
| `register` | `true` | Auto-registration при старте |
| `prefer-ip-address` | `true` | Использовать IP вместо hostname |
| `instance-id` | `{name}:{random}` | Уникальный ID для каждого инстанса |
| `health-check-path` | `/actuator/health` | Путь для health check |
| `health-check-interval` | `10s` | Частота проверок |
| `health-check-critical-timeout` | `30s` | Время до auto-deregister |

---

### 3. Registered Services

| Service Name | Port | Health Check | Tags |
|--------------|------|--------------|------|
| `api-gateway` | 8000 | `/actuator/health` | gateway=true |
| `authentication-service` | 8082 | `/actuator/health` | v1.0.0 |
| `emr-service` | 8086 | `/actuator/health` | v1.0.0 |
| `notification-service` | 8087 | `/actuator/health` | v1.0.0 |
| `pain-escalation-service` | 8088 | `/actuator/health` | v1.0.0 |
| `external-vas-service` | 8089 | `/api/external/vas/health` | v1.0.0 |
| `reporting-service` | 8091 | `/actuator/health` | v1.0.0 |
| `backup-service` | 8085 | `/actuator/health` | v1.0.0 |

**Total:** 8 services registered

---

### 4. API Gateway Integration

#### Dependencies

**pom.xml (API Gateway specific):**
```xml
<!-- Consul Service Discovery -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>

<!-- Load Balancer -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

#### Dynamic Routing Configuration

**application-consul.yml (API Gateway):**
```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: authentication-service
          uri: lb://authentication-service  # Load balanced!
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: authService
                fallbackUri: forward:/fallback/auth
```

**Load Balancing:**
- `lb://` prefix означает load-balanced routing
- Spring Cloud LoadBalancer выбирает healthy инстанс
- Round-robin по умолчанию
- Автоматический failover при падении инстанса

---

## 🚀 Usage Guide

### Starting the System

```bash
# 1. Start infrastructure (Consul, Kafka, PostgreSQL)
cd C:/backend_projects/pain_managment_back
docker-compose -f docker-compose.dev.yml up -d

# 2. Start all services with consul profile
docker-compose -f docker-compose.dev.yml --profile all up -d

# 3. Verify services in Consul UI
# Open: http://localhost:8500/ui
# Check "Services" tab - should see 8 services registered
```

### Consul UI

**Access:** http://localhost:8500/ui

**Features:**
- View all registered services
- Check health status (green = healthy)
- View service instances and metadata
- Monitor health check history
- Query Key-Value store (future use)

---

## 🔍 Service Discovery in Action

### Example: Gateway → Authentication Service

**Before (Hardcoded):**
```yaml
routes:
  - id: auth
    uri: http://localhost:8082  # ❌ Fixed URL
```

**After (Service Discovery):**
```yaml
routes:
  - id: auth
    uri: lb://authentication-service  # ✅ Dynamic discovery
```

**How It Works:**

1. **Frontend → Gateway:**
   ```
   GET http://localhost:8000/api/auth/login
   ```

2. **Gateway → Consul:**
   ```
   Query: "Give me healthy instances of authentication-service"
   Response: [
     { "address": "172.18.0.11", "port": 8082, "status": "passing" }
   ]
   ```

3. **Gateway → Auth Service:**
   ```
   GET http://172.18.0.11:8082/login
   ```

4. **Gateway → Frontend:**
   ```
   200 OK { "token": "..." }
   ```

---

## 📊 Health Checks

### Consul Health Check Configuration

**Type:** HTTP  
**Interval:** 10 seconds  
**Timeout:** 5 seconds  
**Critical Timeout:** 30 seconds (auto-deregister)

**States:**
- ✅ **passing** - Service healthy (HTTP 200 from /actuator/health)
- ⚠️ **warning** - Service degraded
- ❌ **critical** - Service unhealthy (HTTP error, timeout)

### Spring Boot Actuator Health

**Endpoint:** `/actuator/health`

**Response (healthy):**
```json
{
  "status": "UP",
  "components": {
    "consul": { "status": "UP" },
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## 🔧 Scaling Services

### Running Multiple Instances

```bash
# Scale authentication-service to 2 instances
docker-compose -f docker-compose.dev.yml --profile auth up -d --scale auth-service=2

# Consul will register both:
# - authentication-service:random-id-1 (172.18.0.11:8082)
# - authentication-service:random-id-2 (172.18.0.12:8082)

# Gateway will load balance between them (Round Robin)
```

### Load Balancing Strategies

**Spring Cloud LoadBalancer (default):**
- Round Robin (равномерное распределение)
- Random (случайный выбор)
- Weighted (с учетом весов)

**Configuration:**
```yaml
spring:
  cloud:
    loadbalancer:
      ribbon:
        enabled: false  # Use Spring Cloud LoadBalancer
```

---

## 🛡️ Resilience Integration

### Circuit Breaker + Service Discovery

Gateway routes комбинируют оба паттерна:

```yaml
routes:
  - id: auth-service
    uri: lb://authentication-service  # Service Discovery
    filters:
      - name: CircuitBreaker           # Resilience
        args:
          name: authService
          fallbackUri: forward:/fallback/auth
```

**Benefits:**
1. **Service Discovery:** Находит healthy инстансы
2. **Circuit Breaker:** Защищает от cascade failures
3. **Load Balancing:** Распределяет нагрузку
4. **Retry:** Автоматические повторы (из Resilience4j)

---

## 📈 Monitoring

### Consul Metrics

**HTTP API:**
```bash
# All services
curl http://localhost:8500/v1/catalog/services

# Specific service
curl http://localhost:8500/v1/health/service/authentication-service

# Only healthy instances
curl http://localhost:8500/v1/health/service/authentication-service?passing=true
```

**Response:**
```json
[
  {
    "Service": {
      "ID": "authentication-service:12345",
      "Service": "authentication-service",
      "Address": "172.18.0.11",
      "Port": 8082,
      "Tags": ["version=1.0.0", "profile=consul"]
    },
    "Checks": [
      {
        "Status": "passing",
        "Output": "{\"status\":\"UP\"}"
      }
    ]
  }
]
```

### Prometheus Metrics

Spring Cloud Consul exposes metrics:

```
# SERVICE_DISCOVERY_IMPLEMENTATION.md continuation
consul_health_check_duration_seconds
consul_service_instances_total
consul_catalog_services_total
```

**Grafana Dashboard:**
- Service registration/deregistration events
- Health check success/failure rates
- Service instance count per service

---

## 🔐 Security Considerations

### Current Setup (Development)

- ⚠️ **No ACLs** - Consul без authentication
- ⚠️ **HTTP only** - не HTTPS
- ⚠️ **Single node** - no HA

### Production Recommendations

```bash
# 1. Enable ACLs
consul acl bootstrap

# 2. TLS/SSL
consul tls cert create -server

# 3. Multi-node cluster (3-5 servers)
consul agent -server -bootstrap-expect=3

# 4. Service mesh (Consul Connect)
consul connect enable
```

---

## 🐛 Troubleshooting

### Service Not Registered

**Check 1: Consul connectivity**
```bash
docker exec -it {service-container} curl http://consul:8500/v1/status/leader
```

**Check 2: Application logs**
```bash
docker logs {service-container} | grep -i consul
# Should see: "Registering service with consul"
```

**Check 3: Health endpoint**
```bash
curl http://localhost:808X/actuator/health
# Should return 200 OK with status: UP
```

### Service Marked as Critical

**Причины:**
1. Health check endpoint не отвечает
2. Сервис возвращает HTTP 500
3. Health check timeout (> 5s response time)

**Fix:**
```bash
# Check service health directly
curl http://localhost:808X/actuator/health

# Check Consul logs
docker logs dev_consul
```

### Gateway Can't Find Service

**Symptom:** `503 Service Unavailable`

**Check:**
```bash
# 1. Service registered?
curl http://localhost:8500/v1/catalog/service/authentication-service

# 2. Service healthy?
curl http://localhost:8500/v1/health/service/authentication-service?passing=true

# 3. Gateway logs
docker logs dev_api_gateway | grep -i "lb://authentication-service"
```

---

## 📚 Configuration Reference

### Environment Variables (Docker Compose)

| Variable | Default | Description |
|----------|---------|-------------|
| `CONSUL_HOST` | `consul` | Consul server hostname |
| `CONSUL_PORT` | `8500` | Consul HTTP API port |
| `SPRING_PROFILES_ACTIVE` | `consul` | Activate consul profile |

### Application Properties

**Complete example:**
```yaml
spring:
  application:
    name: my-service
  cloud:
    consul:
      # Connection
      host: consul
      port: 8500
      
      # Discovery
      discovery:
        enabled: true
        register: true
        deregister: true
        prefer-ip-address: true
        instance-id: ${spring.application.name}:${random.value}
        service-name: ${spring.application.name}
        
        # Health checks
        health-check-path: /actuator/health
        health-check-interval: 10s
        health-check-timeout: 5s
        health-check-critical-timeout: 30s
        
        # Metadata
        tags:
          - version=1.0.0
          - profile=${spring.profiles.active}
        metadata:
          version: 1.0.0
          region: us-east-1
```

---

## 🎯 Benefits Achieved

### 1. Dynamic Service Discovery
- ✅ Сервисы регистрируются автоматически
- ✅ Нет hardcoded URLs
- ✅ API Gateway использует service names

### 2. Health-Based Routing
- ✅ Только healthy инстансы получают трафик
- ✅ Автоматическое исключение failed instances
- ✅ Health checks каждые 10s

### 3. Load Balancing
- ✅ Round-robin между инстансами
- ✅ Готовность к горизонтальному масштабированию
- ✅ Автоматический failover

### 4. Operational Visibility
- ✅ Consul UI для мониторинга
- ✅ HTTP API для automation
- ✅ Prometheus metrics

---

## 🚀 Future Enhancements

### Phase 4: Advanced Features

1. **Multi-Datacenter Setup**
   ```yaml
   consul:
     datacenter: dc1
     wan-join: dc2.consul.example.com
   ```

2. **Service Mesh (Consul Connect)**
   - mTLS между сервисами
   - Service-to-service authorization
   - Traffic splitting для A/B testing

3. **Configuration Management**
   ```yaml
   spring:
     cloud:
       consul:
         config:
           enabled: true
           prefix: config
           defaultContext: application
   ```

4. **Advanced Load Balancing**
   - Weighted routing
   - Locality-aware routing
   - Custom health checks

---

## 📖 References

- [HashiCorp Consul Documentation](https://www.consul.io/docs)
- [Spring Cloud Consul](https://spring.io/projects/spring-cloud-consul)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Service Discovery Patterns](https://microservices.io/patterns/service-registry.html)

---

## ✅ Implementation Checklist

- [x] Consul Server в docker-compose
- [x] Spring Cloud Consul в 8 сервисах
- [x] application-consul.yml для всех сервисов
- [x] API Gateway integration с load balancing
- [x] Health checks настроены
- [x] Docker Compose environment variables
- [ ] Production ACLs и TLS (future)
- [ ] Multi-node Consul cluster (future)
- [ ] Grafana dashboards для Consul metrics (future)

---

**Status:** ✅ Phase 3 Complete  
**Next Phase:** Distributed Tracing (Jaeger) или Advanced Observability

**Last Updated:** January 23, 2026
