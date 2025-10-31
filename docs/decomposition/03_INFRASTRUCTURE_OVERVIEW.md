# 03 - Обзор инфраструктуры

**Предыдущий:** [02_DECOMPOSITION_STRATEGY.md](02_DECOMPOSITION_STRATEGY.md)  
**Следующий:** [04_DOCKER_SETUP.md](04_DOCKER_SETUP.md)

---

## 🏗️ Компоненты инфраструктуры

### Архитектурная диаграмма

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway :8080                     │
│              (Spring Cloud Gateway)                      │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼──────┐ ┌──▼────────┐ ┌─▼──────────┐
│ Eureka :8761 │ │Config:8888│ │Zipkin:9411 │
│   (Service   │ │  (Config  │ │ (Tracing)  │
│  Discovery)  │ │  Server)  │ │            │
└──────────────┘ └───────────┘ └────────────┘

┌─────────────────────────────────────────────────────────┐
│                  Message Broker Layer                    │
├─────────────────────────────────────────────────────────┤
│  Kafka :9092  │  Zookeeper :2181  │  Redis :6379       │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    Database Layer                        │
├─────────────────────────────────────────────────────────┤
│  MongoDB-Logging :27017  │  MongoDB-Analytics :27018   │
│  PostgreSQL :5432        │  InfluxDB :8086             │
│  MinIO :9000/9001                                       │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  Monitoring Layer                        │
├─────────────────────────────────────────────────────────┤
│  Prometheus :9090  │  Grafana :3000  │  Kafka-UI :8090 │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 Список компонентов

### Service Mesh

| Компонент | Порт | Назначение | Приоритет |
|-----------|------|------------|-----------|
| **API Gateway** | 8080 | Единая точка входа | 🔴 Критический |
| **Eureka Server** | 8761 | Service Discovery | 🔴 Критический |
| **Config Server** | 8888 | Централизованная конфигурация | 🔴 Критический |
| **Zipkin** | 9411 | Distributed Tracing | 🟡 Важный |

### Message Broker

| Компонент | Порт | Назначение | Приоритет |
|-----------|------|------------|-----------|
| **Apache Kafka** | 9092 | Event Streaming | 🔴 Критический |
| **Zookeeper** | 2181 | Kafka Coordination | 🔴 Критический |
| **Redis** | 6379 | Distributed Cache | 🟡 Важный |
| **Kafka UI** | 8090 | Kafka Management | 🟢 Опциональный |

### Databases

| Компонент | Порт | Назначение | Приоритет |
|-----------|------|------------|-----------|
| **MongoDB (Logging)** | 27017 | Логи и audit trail | 🔴 Критический |
| **MongoDB (Analytics)** | 27018 | Аналитические данные | 🟡 Важный |
| **PostgreSQL** | 5432 | Основная БД | 🔴 Критический |
| **InfluxDB** | 8086 | Time-series метрики | 🟡 Важный |
| **MinIO** | 9000/9001 | Object Storage (бэкапы) | 🟡 Важный |

### Monitoring

| Компонент | Порт | Назначение | Приоритет |
|-----------|------|------------|-----------|
| **Prometheus** | 9090 | Metrics Collection | 🟡 Важный |
| **Grafana** | 3000 | Visualization | 🟡 Важный |

---

## 🔄 Порядок запуска

### Шаг 1: Базовые сервисы

```bash
docker-compose up -d zookeeper kafka redis
```

**Ожидание:** 30-60 секунд

### Шаг 2: Databases

```bash
docker-compose up -d mongodb-logging mongodb-analytics postgresql influxdb minio
```

**Ожидание:** 30-60 секунд

### Шаг 3: Service Discovery

```bash
docker-compose up -d eureka-server
```

**Ожидание:** 30-60 секунд  
**Проверка:** http://localhost:8761

### Шаг 4: Config Server

```bash
docker-compose up -d config-server
```

**Ожидание:** 30-60 секунд  
**Проверка:** http://localhost:8888/actuator/health

### Шаг 5: API Gateway

```bash
docker-compose up -d api-gateway
```

**Ожидание:** 30-60 секунд  
**Проверка:** http://localhost:8080/actuator/health

### Шаг 6: Monitoring

```bash
docker-compose up -d prometheus grafana zipkin kafka-ui
```

**Проверка:**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Zipkin: http://localhost:9411
- Kafka UI: http://localhost:8090

---

## 🔍 Health Checks

### Проверка всех сервисов

```bash
# Статус всех контейнеров
docker-compose ps

# Логи конкретного сервиса
docker-compose logs -f eureka-server

# Health check всех сервисов
curl http://localhost:8761/actuator/health
curl http://localhost:8888/actuator/health
curl http://localhost:8080/actuator/health
```

### Ожидаемые результаты

```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

---

## 📊 Требования к ресурсам

### Development Environment

| Компонент | CPU | RAM | Disk |
|-----------|-----|-----|------|
| Kafka + Zookeeper | 1 core | 2 GB | 10 GB |
| MongoDB (x2) | 0.5 core | 1 GB | 20 GB |
| PostgreSQL | 0.5 core | 512 MB | 10 GB |
| InfluxDB | 0.5 core | 512 MB | 5 GB |
| MinIO | 0.5 core | 512 MB | 50 GB |
| Eureka | 0.5 core | 512 MB | 1 GB |
| Config Server | 0.5 core | 512 MB | 1 GB |
| API Gateway | 0.5 core | 512 MB | 1 GB |
| Redis | 0.5 core | 256 MB | 1 GB |
| Monitoring | 1 core | 2 GB | 5 GB |
| **ИТОГО** | **6-8 cores** | **8-10 GB** | **104 GB** |

### Production Environment

- CPU: 16-32 cores
- RAM: 32-64 GB
- Disk: 500 GB - 1 TB (SSD)
- Network: 1 Gbps+

---

## 🔐 Безопасность

### Credentials (Development)

```yaml
# MongoDB
Username: admin
Password: admin_password

# PostgreSQL
Username: postgres
Password: postgres_password

# Redis
Password: redis_password

# MinIO
Access Key: minioadmin
Secret Key: minioadmin

# Grafana
Username: admin
Password: admin
```

⚠️ **ВАЖНО:** В production используйте сильные пароли и храните их в secrets!

---

## 🌐 Network Configuration

### Docker Network

```yaml
networks:
  microservices-network:
    driver: bridge
```

### Service Communication

```
Внутри Docker:
  kafka:9092
  mongodb-logging:27017
  eureka-server:8761

Снаружи Docker:
  localhost:9092
  localhost:27017
  localhost:8761
```

---

## 📝 Volumes

### Persistent Data

```yaml
volumes:
  kafka-data:           # Kafka messages
  mongodb-logging-data: # Logs
  mongodb-analytics-data: # Analytics
  postgresql-data:      # Core business data
  influxdb-data:        # Metrics
  minio-data:           # Backups
  redis-data:           # Cache
  prometheus-data:      # Prometheus metrics
  grafana-data:         # Grafana dashboards
```

### Backup Strategy

```bash
# Создание бэкапа всех volumes
docker-compose down
tar -czf volumes-backup.tar.gz /var/lib/docker/volumes/

# Восстановление
tar -xzf volumes-backup.tar.gz -C /
docker-compose up -d
```

---

## ✅ Проверка готовности

### Чеклист

- [ ] Все контейнеры запущены (`docker-compose ps`)
- [ ] Eureka показывает зарегистрированные сервисы
- [ ] Config Server отвечает на запросы
- [ ] API Gateway доступен
- [ ] Kafka topics созданы
- [ ] MongoDB принимает подключения
- [ ] PostgreSQL принимает подключения
- [ ] Prometheus собирает метрики
- [ ] Grafana доступна
- [ ] Zipkin доступен

---

**Следующий шаг:** [04_DOCKER_SETUP.md](04_DOCKER_SETUP.md)
