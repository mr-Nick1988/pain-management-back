# 🚪 API Gateway Service

**Port:** 8000  
**Status:** ✅ Operational  
**Version:** 1.0.0  
**Created:** January 23, 2026

---

## 📋 Overview

API Gateway is the **single entry point** for all client requests to the Pain Management Platform. It routes requests to appropriate microservices and provides cross-cutting concerns.

---

## 🎯 Responsibilities

- **Request Routing** - Route requests to 7 microservices + monolith
- **Service Discovery** - Dynamic routing via Consul registry ✅
- **Load Balancing** - Round-robin between service instances ✅
- **JWT Validation** - Centralized authentication for protected routes
- **Circuit Breaking** - Resilience against service failures
- **CORS Management** - Single CORS configuration point
- **Request Logging** - Unified logging with trace IDs
- **Rate Limiting** - Protect services from overload (ready)
- **Fallback Responses** - Graceful degradation when services fail

---

## 🏗️ Technology Stack

- **Framework:** Spring Cloud Gateway (reactive)
- **Language:** Java 21
- **Build:** Maven 3.9
- **Service Discovery:** Spring Cloud Consul ✅
- **Load Balancer:** Spring Cloud LoadBalancer ✅
- **Circuit Breaker:** Resilience4j
- **Metrics:** Micrometer + Prometheus
- **Container:** Docker (multi-stage build)

---

## 🔍 Service Discovery Integration

**Consul Registration:**
- Gateway registers itself as `api-gateway` service
- Queries Consul for downstream services
- Uses `lb://service-name` for load-balanced routing
- Health checks every 10s to `/actuator/health`

**Dynamic Routing Example:**
```yaml
routes:
  - id: authentication-service
    uri: lb://authentication-service  # Load balanced!
    predicates:
      - Path=/api/auth/**
```

**Benefits:**
- No hardcoded service URLs
- Automatic service discovery
- Load balancing between multiple instances
- Health-based routing (only to healthy services)

**Consul UI:** http://localhost:8500/ui

---

## 📡 Routes

### Public Routes (No JWT Required)

| Method | Path | Target | Description |
|--------|------|--------|-------------|
| POST | `/api/auth/login` | auth-service:8082 | User login |
| POST | `/api/auth/register` | auth-service:8082 | Registration |
| POST | `/api/auth/refresh` | auth-service:8082 | Token refresh |

### Protected Routes (JWT Required)

| Path Pattern | Target Service | Port |
|-------------|---------------|------|
| `/api/patients/**` | monolith | 8080 |
| `/api/vas/**` | monolith | 8080 |
| `/api/recommendations/**` | monolith | 8080 |
| `/api/admin/**` | monolith | 8080 |
| `/api/doctor/**` | monolith | 8080 |
| `/api/nurse/**` | monolith | 8080 |
| `/api/anesthesiologist/**` | monolith | 8080 |
| `/api/auth/**` | auth-service | 8082 |
| `/api/emr/**` | emr-service | 8086 |
| `/api/fhir/**` | emr-service | 8086 |
| `/api/notifications/**` | notification-service | 8087 |
| `/api/escalations/**` | pain-escalation-service | 8088 |
| `/api/pain-tracking/**` | pain-escalation-service | 8088 |
| `/api/external-vas/**` | external-vas-service | 8089 |
| `/api/devices/**` | external-vas-service | 8089 |
| `/api/reports/**` | reporting-service | 8091 |
| `/api/analytics/**` | reporting-service | 8091 |
| `/api/backup/**` | backup-service | 8085 |
| `/api/restore/**` | backup-service | 8085 |

---

## 🔐 Security

### JWT Validation

All protected routes require JWT token:
```
Authorization: Bearer <token>
```

**Process:**
1. Extract token from Authorization header
2. Validate signature using `JWT_SECRET`
3. Check expiration
4. Extract claims (userId, username, role)
5. Propagate to downstream services via headers:
   - `X-User-Id`
   - `X-Username`
   - `X-User-Role`

### CORS

Configured allowed origins:
```
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

---

## 🛡️ Resilience

### Circuit Breaker

Each service has dedicated circuit breaker:

**Configuration:**
- Sliding window: 10 requests
- Failure threshold: 50%
- Open state duration: 10 seconds
- Half-open calls: 3

**States:**
- `CLOSED` - Normal operation
- `OPEN` - Service failing, return fallback
- `HALF_OPEN` - Testing recovery

### Retry Policy

- Max retries: 3
- Backoff: 50ms → 500ms (exponential)
- Retry on: `503 SERVICE_UNAVAILABLE`, `502 BAD_GATEWAY`

### Timeouts

- Connect timeout: 5 seconds
- Response timeout: 30 seconds

### Fallback Responses

Returns graceful error when service unavailable:
```json
{
  "error": "Service Unavailable",
  "message": "X service is temporarily unavailable",
  "status": 503,
  "timestamp": "2026-01-23T19:00:00",
  "suggestion": "Please try again in a few moments"
}
```

---

## 📊 Monitoring

### Health Check
```bash
GET http://localhost:8000/actuator/health
```

### Metrics
```bash
GET http://localhost:8000/actuator/prometheus
```

**Available metrics:**
- Request count by route
- Response times (p50, p95, p99)
- Circuit breaker events
- Error rates

### Gateway Routes
```bash
GET http://localhost:8000/actuator/gateway/routes
```

### Circuit Breaker State
```bash
GET http://localhost:8000/actuator/circuitbreakers
```

---

## 🐋 Docker

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | Yes | - | JWT signing secret (min 256 bits) |
| `CORS_ALLOWED_ORIGINS` | Yes | - | Comma-separated origins |
| `REDIS_HOST` | No | redis | Redis for rate limiting |
| `REDIS_PORT` | No | 6379 | Redis port |

### Docker Compose

```yaml
api-gateway:
  image: pain-mgmt/api-gateway:dev
  container_name: dev_api_gateway
  profiles: ["gateway", "all", "core"]
  ports:
    - "8000:8000"
  environment:
    - JWT_SECRET=${JWT_SECRET}
    - CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
```

### Start

```bash
# With all services
docker-compose --profile all up -d

# Only gateway + infrastructure
docker-compose --profile gateway up -d
```

---

## 🧪 Testing

### Public Endpoint
```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"pass"}'
```

### Protected Endpoint
```bash
curl http://localhost:8000/api/patients \
  -H "Authorization: Bearer <token>"
```

### Circuit Breaker Test
```bash
# Stop service
docker stop dev_emr

# Request should return fallback
curl http://localhost:8000/api/emr/patients
```

---

## 📁 Source Code

**Location:** `C:/backend_projects/microservices/api-gateway-service/`

**Structure:**
```
api-gateway-service/
├── src/main/java/com/painmanagement/gateway/
│   ├── ApiGatewayApplication.java
│   ├── config/
│   │   ├── GatewayConfig.java          # Routes
│   │   └── CorsConfig.java             # CORS
│   ├── filter/
│   │   ├── JwtAuthenticationFilter.java # JWT validation
│   │   └── RequestLoggingFilter.java    # Logging
│   └── controller/
│       └── FallbackController.java      # Circuit breaker fallbacks
├── src/main/resources/
│   └── application.yml                  # Configuration
├── Dockerfile
└── pom.xml
```

---

## 🔧 Configuration

**application.yml:**
```yaml
server:
  port: 8000

spring:
  application:
    name: api-gateway-service
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000
        response-timeout: 30s
      default-filters:
        - name: Retry
          args:
            retries: 3
            backoff:
              firstBackoff: 50ms
              maxBackoff: 500ms

resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

---

## 🎯 Benefits

### Before API Gateway (Distributed Monolith)
- Frontend calls 8 different services directly
- CORS configured in each service
- No centralized authentication
- No circuit breakers
- No unified logging

### After API Gateway (True Microservices)
- ✅ Single entry point (localhost:8000)
- ✅ Centralized JWT validation
- ✅ Circuit breakers for resilience
- ✅ Unified CORS
- ✅ Request tracing
- ✅ Rate limiting ready
- ✅ Loose coupling between frontend and services

---

## 📈 Performance

- **Routing overhead:** ~5-10ms
- **JWT validation:** ~2-3ms
- **Total overhead:** ~7-13ms per request

**Trade-off:** Minimal latency for significant architectural benefits.

---

## 🚀 Future Enhancements

- [ ] Rate limiting with Redis
- [ ] API versioning (v1, v2 routes)
- [ ] Service Discovery integration (Consul)
- [ ] OAuth2/OIDC support
- [ ] API key management
- [ ] Request/Response transformation
- [ ] GraphQL gateway
- [ ] WebSocket routing

---

## 📚 Related Documentation

- [API Gateway Integration Guide](../API_GATEWAY_INTEGRATION_GUIDE.md)
- [Microservices Patterns Roadmap](../MICROSERVICES_PATTERNS_ROADMAP.md)
- [Backend Architecture Overview](../02-architecture/BACKEND_ARCHITECTURE_OVERVIEW.md)

---

**Status:** ✅ Operational - Phase 1 Complete
