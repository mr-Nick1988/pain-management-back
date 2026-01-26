# 🚀 MICROSERVICES QUICKSTART GUIDE

**Date:** January 24, 2026  
**Status:** ✅ API Gateway + 7 Microservices + Distributed Tracing Running

---

## 📚 ДОКУМЕНТАЦИЯ

**Главный документ:** [BACKEND_ARCHITECTURE_OVERVIEW.md](BACKEND_ARCHITECTURE_OVERVIEW.md) - полная архитектура бэкенда

### Основные документы:
- [DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md) - DevOps руководство
- [DOCKER_COMPOSE_REFERENCE.md](DOCKER_COMPOSE_REFERENCE.md) - Docker Compose справочник
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Руководство по тестированию
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Решение проблем
- [HOW_TO_RUN_AND_TEST.md](HOW_TO_RUN_AND_TEST.md) - Как запустить и тестировать

### Архитектура:
- [architecture/MICROSERVICES_MIGRATION_STRATEGY.md](architecture/MICROSERVICES_MIGRATION_STRATEGY.md) - Стратегия архитектуры
- [api/event-schemas/KAFKA_EVENT_SCHEMAS.md](api/event-schemas/KAFKA_EVENT_SCHEMAS.md) - Kafka события

---

## ✅ ТЕКУЩЕЕ СОСТОЯНИЕ СИСТЕМЫ

### API Gateway (1/1)
```
✅ API Gateway (8000) - Single Entry Point
```

### Инфраструктура (6/6)
```
✅ Kafka (9092) - Healthy
✅ PostgreSQL Main (5432) - Healthy  
✅ PostgreSQL Analytics (5433) - Healthy
✅ Prometheus (9090) - Running
✅ Grafana (3000) - Running
✅ Kafdrop (9000) - Running
```

### Микросервисы (7/7)
```
✅ Authentication Service (8082)
✅ EMR Integration Service (8086)
✅ Notification Service (8087)
✅ Pain Escalation Service (8088)
✅ External VAS Service (8089)
✅ Reporting Service (8091)
✅ Backup & Restore Service (8085)
```

### Монолит
```
⚪ Monolith (8080) - запускается вручную
```

---

## 📁 СТРУКТУРА ПРОЕКТА

```
C:\backend_projects\
├── pain_managment_back/              # Монолит + инфраструктура
│   ├── src/main/java/                # Код монолита
│   ├── docs/                         # Документация
│   ├── docker-compose.dev.yml        # Docker Compose конфигурация
│   ├── init-databases.sql            # Инициализация БД
│   └── .env.development              # Шаблон переменных окружения
│
└── microservices/                    # Все микросервисы
    ├── api-gateway-service/          # Порт 8000 - Single Entry Point
    ├── authentication-service/       # Порт 8082
    ├── emr-integration-service/      # Порт 8086
    ├── notification-service/         # Порт 8087
    ├── pain-escalation-service/      # Порт 8088
    ├── external-vas-service/         # Порт 8089
    ├── reporting-service/            # Порт 8091
    └── backup-restore/               # Порт 8085
```

---

## 🛠️ ТЕХНОЛОГИИ

### Backend:
- Java 21
- Spring Boot 3.5.5
- Maven 3.9+
- PostgreSQL 16
- Apache Kafka 7.6.1

### Infrastructure:
- Docker & Docker Compose
- Prometheus (мониторинг)
- Grafana (визуализация)
- Kafdrop (Kafka UI)

---

## 🚀 БЫСТРЫЙ ЗАПУСК

### 1. Запуск всей системы (все микросервисы)

```bash
cd C:\backend_projects\pain_managment_back

# Запустить всю инфраструктуру + все микросервисы
docker-compose -f docker-compose.dev.yml --profile all up -d

# Проверить статус (через 1-2 минуты)
docker-compose -f docker-compose.dev.yml ps
```

### 2. Запуск только инфраструктуры (без микросервисов)

```bash
cd C:\backend_projects\pain_managment_back

# Только Kafka, PostgreSQL, Prometheus, Grafana, Kafdrop
docker-compose -f docker-compose.dev.yml up -d

# Проверить статус
docker ps
```

### 3. Запуск монолита

```bash
cd C:\backend_projects\pain_managment_back

# Собрать и запустить
mvn clean install -DskipTests
mvn spring-boot:run

# Или через IDE (Run/Debug)
```

### 4. Проверка работоспособности

```bash
# Инфраструктура
curl http://localhost:9092  # Kafka (должен ответить)
curl http://localhost:9090  # Prometheus
curl http://localhost:3000  # Grafana
curl http://localhost:9000  # Kafdrop

# API Gateway
curl http://localhost:8000/actuator/health  # API Gateway

# Микросервисы
curl http://localhost:8082/actuator/health  # Auth
curl http://localhost:8086/actuator/health  # EMR
curl http://localhost:8087/actuator/health  # Notification
curl http://localhost:8088/actuator/health  # Pain Escalation
curl http://localhost:8089/api/external/vas/health  # External VAS
curl http://localhost:8091/actuator/health  # Reporting
curl http://localhost:8085/actuator/health  # Backup

# Монолит (если запущен)
curl http://localhost:8080/actuator/health
```

---

## 📊 БАЗЫ ДАННЫХ

### PostgreSQL Main (localhost:5432)
```sql
pain_management_db     -- Монолит
auth_db                -- Authentication Service
emr_integration_db     -- EMR Service
notification_db        -- Notification Service
pain_escalation_db     -- Pain Escalation Service
external_vas_db        -- External VAS Service
backup_service         -- Backup Service
```

### PostgreSQL Analytics (localhost:5433)
```sql
analytics_reporting    -- Reporting Service
```

**Все базы данных создаются автоматически при первом запуске через `init-databases.sql`.**

---

## 🔧 ПОЛЕЗНЫЕ КОМАНДЫ

### Управление контейнерами

```bash
# Остановить все сервисы
docker-compose -f docker-compose.dev.yml --profile all down

# Перезапустить конкретный сервис
docker-compose -f docker-compose.dev.yml restart auth-service

# Посмотреть логи
docker logs dev_auth --tail 50
docker logs dev_emr --tail 50

# Пересобрать и запустить
docker-compose -f docker-compose.dev.yml --profile all up -d --build
```

### Kafka

```bash
# Список топиков
docker exec -it dev_kafka kafka-topics --list --bootstrap-server localhost:9092

# Просмотр сообщений в топике
docker exec -it dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic analytics-events \
  --from-beginning

# Или через Kafdrop UI: http://localhost:9000
```

### Базы данных

```bash
# Подключиться к PostgreSQL
docker exec -it dev_postgres psql -U postgres -d pain_management_db

# Список баз
docker exec -it dev_postgres psql -U postgres -c "\l"

# Список таблиц
docker exec -it dev_postgres psql -U postgres -d pain_management_db -c "\dt"
```

---

## 🎯 АРХИТЕКТУРА

**Подробная архитектура:** См. [BACKEND_ARCHITECTURE_OVERVIEW.md](BACKEND_ARCHITECTURE_OVERVIEW.md)

### Слои системы:

```
Frontend (React)
       ↓
API Gateway (8000) - Single Entry Point
       ↓
Application Layer (Monolith + 7 Microservices)
       ↓
Event Streaming (Kafka - 8 topics)
       ↓
Data Layer (PostgreSQL Main + Analytics)
       ↓
Observability (Prometheus, Grafana)
```

### Паттерны:
- **API Gateway** - единая точка входа для всех запросов
- **Circuit Breaker** - защита от каскадных сбоев (Resilience4j)
- **Database per Service** - каждый микросервис имеет свою БД
- **Event-Driven Architecture** - асинхронное общение через Kafka
- **CQRS** - разделение команд и запросов
- **Domain-Driven Design** - bounded contexts

---

## 📦 ENVIRONMENT ПЕРЕМЕННЫЕ

**Шаблон:** `.env.development` (скопируй в `.env` если нужно)

### Основные переменные:
```bash
# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# JWT
JWT_SECRET=pain-management-secret-key...
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS,PATCH
CORS_ALLOWED_HEADERS=Authorization,Content-Type,Accept,Origin,X-Requested-With
CORS_ALLOW_CREDENTIALS=true

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# Mail (для dev - можно оставить пустым)
MAIL_HOST=localhost
MAIL_PORT=1025
```

---

## 🔍 МОНИТОРИНГ

### Prometheus (http://localhost:9090)
- Метрики всех микросервисов
- Метрики JVM, HTTP запросов
- Custom business метрики

### Grafana (http://localhost:3000)
- Визуализация метрик
- Дашборды по микросервисам
- Алерты (настраиваются)
- **Login:** admin/admin

### Kafdrop (http://localhost:9000)
- Просмотр Kafka топиков
- Сообщения в реальном времени
- Consumer groups и lag

---

## 🧪 ТЕСТИРОВАНИЕ

**См. подробнее:** [TESTING_GUIDE.md](TESTING_GUIDE.md)

### Unit тесты
```bash
cd C:\backend_projects\microservices\authentication-service
mvn test
```

### Integration тесты
```bash
mvn verify -P integration-tests
```

### Health checks
```bash
# Автоматическая проверка всех сервисов
curl http://localhost:8082/actuator/health
curl http://localhost:8086/actuator/health
# ... и т.д.
```

---

## 🐛 TROUBLESHOOTING

**Полное руководство:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

### Сервис не запускается

```bash
# 1. Проверить логи
docker logs dev_auth --tail 50

# 2. Проверить зависимости (Kafka, PostgreSQL)
docker ps --filter "name=dev_"

# 3. Проверить переменные окружения
docker exec dev_auth env | grep SPRING
```

### База данных недоступна

```bash
# Проверить PostgreSQL
docker exec -it dev_postgres psql -U postgres -c "\l"

# Пересоздать volume (ОСТОРОЖНО: удалит данные)
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml --profile all up -d
```

### Kafka не работает

```bash
# Проверить статус
docker logs dev_kafka --tail 50

# Проверить топики
docker exec -it dev_kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

## 📖 ДАЛЬНЕЙШЕЕ ЧТЕНИЕ

1. **[BACKEND_ARCHITECTURE_OVERVIEW.md](BACKEND_ARCHITECTURE_OVERVIEW.md)** - Полная архитектура
2. **[DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md)** - DevOps руководство
3. **[DOCKER_COMPOSE_REFERENCE.md](DOCKER_COMPOSE_REFERENCE.md)** - Docker Compose детали
4. **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Тестирование
5. **[DATABASE_SETUP_PLAN.md](DATABASE_SETUP_PLAN.md)** - Структура БД

---

**Все микросервисы работают. Система готова к разработке и тестированию.** ✅
