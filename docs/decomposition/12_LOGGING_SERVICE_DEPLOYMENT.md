# 12 - Logging Service - Развертывание

**Предыдущий:** [11_LOGGING_MONOLITH_MIGRATION.md](11_LOGGING_MONOLITH_MIGRATION.md)  
**Следующий:** [13_ANALYTICS_SERVICE_OVERVIEW.md](13_ANALYTICS_SERVICE_OVERVIEW.md)

---

## 🚀 Сборка и развертывание

### Шаг 1: Сборка проекта

```bash
cd logging-service

# Сборка
mvn clean package -DskipTests

# Или с тестами
mvn clean package
```

Результат: `target/logging-service-1.0.0.jar`

---

### Шаг 2: Сборка Docker образа

```bash
# Сборка образа
docker build -t logging-service:1.0.0 .

# Проверка образа
docker images | grep logging-service
```

---

### Шаг 3: Добавить в docker-compose.yml

Добавьте в главный `docker-compose.yml`:

```yaml
logging-service:
  build: ./services/logging-service
  container_name: logging-service
  ports:
    - "8081:8081"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
    - SPRING_DATA_MONGODB_URI=mongodb://admin:admin_password@mongodb-logging:27017/logging_db?authSource=admin
    - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    - MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans
  depends_on:
    eureka-server:
      condition: service_healthy
    kafka:
      condition: service_healthy
    mongodb-logging:
      condition: service_healthy
  networks:
    - microservices-network
  healthcheck:
    test: ["CMD", "wget", "--spider", "-q", "http://localhost:8081/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 5
    start_period: 60s
  restart: unless-stopped
```

---

### Шаг 4: Запуск

```bash
# Запустить только Logging Service
docker-compose up -d logging-service

# Посмотреть логи
docker-compose logs -f logging-service

# Проверить статус
docker-compose ps logging-service
```

---

## 🔍 Проверка работоспособности

### Health Check

```bash
# Health endpoint
curl http://localhost:8081/actuator/health

# Ожидаемый ответ
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "mongo": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### Проверка регистрации в Eureka

```bash
# Открыть Eureka Dashboard
http://localhost:8761

# Или через API
curl http://localhost:8761/eureka/apps/LOGGING-SERVICE
```

Вы должны увидеть `LOGGING-SERVICE` в списке зарегистрированных сервисов.

---

### Проверка Kafka Consumer

```bash
# Проверить consumer group
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group logging-service-group

# Ожидаемый вывод
GROUP                  TOPIC            PARTITION  CURRENT-OFFSET  LAG
logging-service-group  logging-events   0          100             0
logging-service-group  logging-events   1          95              0
logging-service-group  logging-events   2          105             0
```

LAG должен быть близок к 0.

---

### Проверка MongoDB

```bash
# Подключиться к MongoDB
docker exec -it mongodb-logging mongosh -u admin -p admin_password

# Проверить базу данных
use logging_db

# Посмотреть коллекции
show collections

# Посчитать документы
db.log_entries.countDocuments()

# Посмотреть последние логи
db.log_entries.find().sort({timestamp: -1}).limit(5).pretty()
```

---

## 🧪 Тестирование end-to-end

### Шаг 1: Отправить тестовое событие из монолита

```bash
# Выполнить запрос к монолиту
curl -X POST http://localhost:8080/api/doctor/patients \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "emrNumber": "EMR-12345"
  }'
```

### Шаг 2: Проверить Kafka

```bash
# Посмотреть сообщения в топике
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic logging-events \
  --from-beginning \
  --max-messages 1
```

### Шаг 3: Проверить MongoDB

```bash
# Подождать 1-2 секунды для обработки
sleep 2

# Проверить, что лог сохранен
docker exec mongodb-logging mongosh -u admin -p admin_password --eval "
  db.getSiblingDB('logging_db').log_entries.find({
    operation: 'createPatient'
  }).sort({timestamp: -1}).limit(1).pretty()
"
```

### Шаг 4: Проверить через API

```bash
# Получить логи через REST API
curl "http://localhost:8081/api/logs/service/monolith?from=2025-10-31T00:00:00&to=2025-10-31T23:59:59"
```

---

## 📊 Мониторинг

### Prometheus метрики

```bash
# Получить метрики
curl http://localhost:8081/actuator/prometheus

# Важные метрики
kafka_consumer_records_consumed_total
kafka_consumer_records_lag
mongodb_driver_commands_succeeded_total
http_server_requests_seconds_count
```

### Grafana Dashboard

1. Откройте Grafana: http://localhost:3000
2. Login: admin/admin
3. Add Data Source → Prometheus (http://prometheus:9090)
4. Import Dashboard:
   - Spring Boot 2.1 Statistics (ID: 10280)
   - Kafka Consumer Lag (ID: 12460)

---

### Алерты

Настройте алерты в Prometheus (`prometheus.yml`):

```yaml
groups:
  - name: logging-service
    interval: 30s
    rules:
      - alert: HighConsumerLag
        expr: kafka_consumer_lag{service="logging-service"} > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High Kafka consumer lag"
          description: "Consumer lag is {{ $value }} messages"
      
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5..",service="logging-service"}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate in Logging Service"
          description: "Error rate is {{ $value }} req/s"
      
      - alert: ServiceDown
        expr: up{job="logging-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Logging Service is down"
```

---

## 🔄 Canary Deployment

### Шаг 1: Запустить с 10% трафика

В монолите установите:

```properties
logging.kafka.enabled=true
logging.kafka.canary-percentage=10
```

### Шаг 2: Мониторинг (24 часа)

Следите за метриками:
- Consumer lag
- Error rate
- Latency
- CPU/Memory usage

### Шаг 3: Увеличение до 50%

Если все хорошо:

```properties
logging.kafka.canary-percentage=50
```

### Шаг 4: Полное переключение (100%)

После успешного тестирования:

```properties
logging.kafka.canary-percentage=100
```

---

## 🔧 Масштабирование

### Горизонтальное масштабирование

```bash
# Запустить 3 инстанса
docker-compose up -d --scale logging-service=3

# Проверить
docker-compose ps | grep logging-service
```

Kafka автоматически распределит партиции между инстансами.

### Вертикальное масштабирование

В `docker-compose.yml`:

```yaml
logging-service:
  deploy:
    resources:
      limits:
        cpus: '2.0'
        memory: 2G
      reservations:
        cpus: '1.0'
        memory: 1G
```

---

## 🔙 Rollback Plan

### Если что-то пошло не так

#### Шаг 1: Отключить Kafka в монолите

```properties
logging.kafka.enabled=false
```

Или через environment variable:

```bash
export LOGGING_KAFKA_ENABLED=false
```

#### Шаг 2: Остановить Logging Service

```bash
docker-compose stop logging-service
```

#### Шаг 3: Восстановить данные (если нужно)

```bash
# Восстановить MongoDB из бэкапа
docker exec mongodb-logging mongorestore \
  --uri="mongodb://admin:admin_password@localhost:27017" \
  --archive=/backups/logging_db_backup.archive
```

#### Шаг 4: Анализ проблемы

```bash
# Логи сервиса
docker-compose logs logging-service > logging-service-error.log

# Метрики
curl http://localhost:8081/actuator/metrics > metrics.json

# Kafka consumer lag
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group logging-service-group > consumer-lag.txt
```

---

## ✅ Чеклист развертывания

- [ ] Проект собран успешно
- [ ] Docker образ создан
- [ ] Сервис добавлен в docker-compose.yml
- [ ] Сервис запущен
- [ ] Health check проходит
- [ ] Зарегистрирован в Eureka
- [ ] Kafka consumer работает
- [ ] MongoDB подключена
- [ ] End-to-end тест пройден
- [ ] Метрики доступны в Prometheus
- [ ] Grafana dashboard настроен
- [ ] Алерты настроены
- [ ] Canary deployment выполнен
- [ ] Документация обновлена

---

## 📈 Ожидаемые результаты

После успешного развертывания:

| Метрика | Значение |
|---------|----------|
| **Latency монолита (p95)** | ↓ 28% |
| **CPU монолита** | ↓ 33% |
| **Consumer lag** | < 100 messages |
| **Error rate** | < 0.1% |
| **Throughput** | 5000+ msg/s |

---

**Следующий шаг:** [13_ANALYTICS_SERVICE_OVERVIEW.md](13_ANALYTICS_SERVICE_OVERVIEW.md)
