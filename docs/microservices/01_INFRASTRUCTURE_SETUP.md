# 🏗️ Infrastructure Setup - Настройка инфраструктуры

**Phase:** 0 - Подготовка  
**Длительность:** 2 недели

---

## 📋 Компоненты инфраструктуры

### Основные сервисы

| Компонент | Порт | Назначение |
|-----------|------|------------|
| **Eureka Server** | 8761 | Service Discovery |
| **Config Server** | 8888 | Centralized Configuration |
| **API Gateway** | 8080 | Единая точка входа |
| **Kafka** | 9092 | Message Broker |
| **Zookeeper** | 2181 | Kafka coordination |
| **Redis** | 6379 | Distributed Cache |
| **MongoDB (Logging)** | 27017 | Логи |
| **MongoDB (Analytics)** | 27018 | Аналитика |
| **PostgreSQL** | 5432 | Основная БД |
| **InfluxDB** | 8086 | Time-series метрики |
| **MinIO** | 9000/9001 | Object Storage |
| **Prometheus** | 9090 | Metrics Collection |
| **Grafana** | 3000 | Visualization |
| **Zipkin** | 9411 | Distributed Tracing |

---

## 🐳 Docker Compose Setup

### Структура проекта

```
pain-management-microservices/
├── docker-compose.yml
├── infrastructure/
│   ├── eureka-server/
│   ├── config-server/
│   ├── api-gateway/
│   ├── prometheus/
│   ├── grafana/
│   └── scripts/
└── services/
    ├── logging-service/
    ├── analytics-service/
    └── ...
```

### docker-compose.yml (см. отдельный файл)

Полный файл: `docker-compose-infrastructure.yml`

---

## 🔍 Eureka Server

### pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
</dependencies>
```

### EurekaServerApplication.java

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

### application.yml

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

---

## ⚙️ Config Server

### pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-config-server</artifactId>
    </dependency>
</dependencies>
```

### ConfigServerApplication.java

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

---

## 🚪 API Gateway

### pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>
```

### Routing Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: logging-service
          uri: lb://logging-service
          predicates:
            - Path=/api/logs/**
        - id: analytics-service
          uri: lb://analytics-service
          predicates:
            - Path=/api/analytics/**
```

---

## 📊 Monitoring Stack

### Prometheus Configuration

```yaml
scrape_configs:
  - job_name: 'spring-actuator'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    eureka_sd_configs:
      - server: http://eureka-server:8761/eureka
```

### Grafana Dashboards

- Spring Boot 2.1 Statistics (ID: 10280)
- JVM Micrometer (ID: 4701)
- Kafka Overview (ID: 7589)

---

## 🚀 Запуск инфраструктуры

### Шаг 1: Запуск базовых сервисов

```bash
docker-compose up -d zookeeper kafka redis mongodb-logging mongodb-analytics postgresql
```

### Шаг 2: Запуск Service Discovery

```bash
docker-compose up -d eureka-server
```

### Шаг 3: Запуск Config Server

```bash
docker-compose up -d config-server
```

### Шаг 4: Запуск API Gateway

```bash
docker-compose up -d api-gateway
```

### Шаг 5: Запуск Monitoring

```bash
docker-compose up -d prometheus grafana zipkin
```

### Проверка статуса

```bash
docker-compose ps
```

---

## ✅ Проверка работоспособности

| Сервис | URL | Ожидаемый результат |
|--------|-----|---------------------|
| Eureka | http://localhost:8761 | Dashboard с зарегистрированными сервисами |
| Config Server | http://localhost:8888/actuator/health | {"status":"UP"} |
| API Gateway | http://localhost:8080/actuator/health | {"status":"UP"} |
| Kafka UI | http://localhost:8090 | Kafka topics |
| Grafana | http://localhost:3000 | Login page (admin/admin) |
| Prometheus | http://localhost:9090 | Targets page |
| Zipkin | http://localhost:9411 | Tracing UI |

---

**Следующий шаг:** [02_LOGGING_SERVICE_MIGRATION.md](./02_LOGGING_SERVICE_MIGRATION.md)
