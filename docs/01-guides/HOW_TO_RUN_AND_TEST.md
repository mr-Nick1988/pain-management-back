# 🚀 Pain Management Platform - Запуск и Тестирование

**Дата:** 2026-01-12  
**Версия:** 3.0 (Complete DevOps Documentation)

> ⚠️ **Note:** This is a quick reference guide. For comprehensive documentation, see:
> - **[DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md)** - Complete DevOps guide
> - **[DOCKER_COMPOSE_REFERENCE.md](DOCKER_COMPOSE_REFERENCE.md)** - Docker Compose reference
> - **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Comprehensive testing guide
> - **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Troubleshooting guide

---

## 📋 Содержание

1. [Предварительные требования](#предварительные-требования)
2. [Быстрый старт (5 минут)](#быстрый-старт)
3. [Детальная инструкция запуска](#детальная-инструкция-запуска)
4. [Тестирование API](#тестирование-api)
5. [Мониторинг и отладка](#мониторинг-и-отладка)
6. [Частые проблемы](#частые-проблемы)

---

## Предварительные требования

### Обязательно:

1. **Java 21** (OpenJDK или Oracle JDK)
   ```bash
   java -version
   # Должно показать: java version "21"
   ```

2. **Maven 3.9+**
   ```bash
   mvn -version
   # Должно показать: Apache Maven 3.9.x
   ```

3. **Docker Desktop** (для Windows)
   - Минимум 4GB RAM для Docker
   - Включен WSL 2 (рекомендуется)

4. **Git**
   ```bash
   git --version
   ```

### Опционально:

- **IntelliJ IDEA** / **VS Code** (с расширениями Java и Spring)
- **Postman** / **Insomnia** (для тестирования API)
- **PlantUML plugin** (для просмотра диаграмм)

---

## Быстрый старт

### Шаг 1: Запустить инфраструктуру (Kafka + PostgreSQL)

```bash
cd C:\backend_projects\pain_managment_back

# Запуск только инфраструктуры
docker-compose up

# ИЛИ в фоновом режиме
docker-compose up -d
```

**Что запустится:**
- Apache Kafka (порт 9092)
- PostgreSQL (порт 5432)
- Healthchecks для проверки готовности

**Проверка:**
```bash
# Kafka готов?
docker exec dev_kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# PostgreSQL готов?
docker exec dev_postgres pg_isready -U postgres
```

---

### Шаг 2: Запустить монолит

**Через Maven:**
```bash
cd C:\backend_projects\pain_managment_back
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Через IDE (IntelliJ IDEA):**
1. Открыть проект
2. Найти `PainHelperBackApplication.java`
3. Правый клик → Run
4. В VM options добавить: `-Dspring.profiles.active=local`

**Монолит доступен:** http://localhost:8080

---

### Шаг 3: Запустить микросервисы (опционально)

**Критичные сервисы (рекомендуется):**
```bash
docker-compose --profile core up -d
```

Запустит:
- Authentication Service (8082)
- EMR Integration Service (8086)
- Notification Service (8087)
- Pain Escalation Service (8088)
- Reporting Service (8091)

**ИЛИ все сервисы:**
```bash
docker-compose --profile all up -d
```

**ИЛИ выборочно:**
```bash
# Только Auth
docker-compose --profile auth up -d

# Только EMR Integration
docker-compose --profile emr up -d

# С инструментами (Kafdrop для просмотра Kafka)
docker-compose --profile tools up -d
```

---

### Шаг 4: Проверить что всё запустилось

```bash
# Проверить Docker контейнеры
docker ps

# Проверить логи
docker-compose logs -f

# Проверить логи конкретного сервиса
docker-compose logs -f dev_auth
```

**Ожидаемые контейнеры:**
- `dev_kafka` (running)
- `dev_postgres` (running)
- `dev_auth` (running, если запущен profile)
- `dev_emr` (running, если запущен profile)
- и т.д.

**Порты должны быть доступны:**
```bash
# Windows PowerShell
Test-NetConnection -ComputerName localhost -Port 8080  # Монолит
Test-NetConnection -ComputerName localhost -Port 9092  # Kafka
Test-NetConnection -ComputerName localhost -Port 5432  # PostgreSQL
```

---

## Детальная инструкция запуска

### Вариант 1: Только монолит (минимальная конфигурация)

**Что нужно:**
- Kafka
- PostgreSQL

```bash
# 1. Запустить инфраструктуру
docker-compose up -d

# 2. Дождаться готовности (30-60 секунд)
docker-compose logs -f kafka | grep "started (kafka.server.KafkaServer)"

# 3. Запустить монолит
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. Проверить
curl http://localhost:8080/actuator/health
```

**Что работает:**
- ✅ Управление пациентами
- ✅ VAS запись (внутренняя)
- ✅ EMR запись (внутренняя)
- ✅ Протоколы лечения
- ✅ Роли пользователей
- ❌ Аутентификация JWT (нужен Authentication Service)
- ❌ Уведомления (нужен Notification Service)
- ❌ Эскалация боли (нужен Pain Escalation Service)

---

### Вариант 2: Полная система (рекомендуется)

```bash
# 1. Запустить всё
docker-compose --profile all up -d

# 2. Дождаться готовности всех сервисов (2-3 минуты)
docker-compose ps

# 3. Запустить монолит
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. Открыть Kafdrop (Kafka UI)
# http://localhost:9000
```

**Что работает:**
- ✅ ВСЁ
- ✅ Аутентификация JWT
- ✅ Уведомления (Email + WebSocket)
- ✅ Автоматическая эскалация боли
- ✅ Интеграция с внешними EMR (FHIR)
- ✅ Интеграция с внешними VAS устройствами
- ✅ Генерация отчетов

---

### Вариант 3: Разработка конкретного микросервиса

**Пример: Разработка EMR Integration Service**

```bash
# 1. Запустить инфраструктуру + зависимые сервисы
docker-compose --profile core up -d

# 2. ОСТАНОВИТЬ EMR сервис в Docker (чтобы запустить локально)
docker-compose stop dev_emr

# 3. Запустить EMR сервис из IDE
cd C:\backend_projects\microservices\emr-integration-service
mvn spring-boot:run

# 4. Отладка через IDE
# Поставить breakpoints и debug
```

---

## Тестирование API

### 1. Health Check (проверка работоспособности)

**Монолит:**
```bash
curl http://localhost:8080/actuator/health
```

**Ожидаемый ответ:**
```json
{
  "status": "UP"
}
```

**Микросервисы:**
```bash
# Authentication Service
curl http://localhost:8082/actuator/health

# EMR Integration Service
curl http://localhost:8086/actuator/health

# Notification Service
curl http://localhost:8087/actuator/health

# Pain Escalation Service
curl http://localhost:8088/actuator/health

# External VAS Service
curl http://localhost:8089/api/external/vas/health

# Reporting Service
curl http://localhost:8091/actuator/health
```

---

### 2. Тестирование Authentication Service

**Регистрация пользователя (через монолит Admin API):**
```bash
curl -X POST http://localhost:8080/api/admin/persons \
  -H "Content-Type: application/json" \
  -d '{
    "personId": "doctor_001",
    "firstName": "Иван",
    "lastName": "Иванов",
    "login": "doctor",
    "password": "password123",
    "roles": ["DOCTOR"]
  }'
```

**Логин:**
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "doctor",
    "password": "password123"
  }'
```

**Ожидаемый ответ:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "personId": "doctor_001",
  "roles": ["DOCTOR"]
}
```

**Использование токена:**
```bash
# Сохранить токен
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Использовать в запросах
curl http://localhost:8080/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3. Тестирование Patient Management

**Создать пациента:**
```bash
curl -X POST http://localhost:8080/api/nurse/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "Петр",
    "lastName": "Петров",
    "dateOfBirth": "1980-05-15",
    "gender": "MALE",
    "phoneNumber": "+79991234567",
    "email": "petr@example.com"
  }'
```

**Получить список пациентов:**
```bash
curl http://localhost:8080/api/nurse/patients \
  -H "Authorization: Bearer $TOKEN"
```

---

### 4. Тестирование VAS Recording + Pain Escalation

**Записать VAS (низкий уровень боли):**
```bash
curl -X POST http://localhost:8080/api/nurse/patients/MRN-000001/vas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "painLevel": 3,
    "location": "Ward A, Bed 5",
    "notes": "Пациент жалуется на умеренную боль"
  }'
```

**Записать VAS (высокий уровень → эскалация):**
```bash
curl -X POST http://localhost:8080/api/nurse/patients/MRN-000001/vas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "painLevel": 8,
    "location": "Ward A, Bed 5",
    "notes": "СИЛЬНАЯ БОЛЬ! Требуется вмешательство"
  }'
```

**Проверить эскалации:**
```bash
curl http://localhost:8088/api/pain-escalation/escalations \
  -H "Authorization: Bearer $TOKEN"
```

**Ожидаемый результат:**
- Pain Escalation Service автоматически обнаружит эскалацию (VAS 3→8 = +5)
- Создаст событие `pain.escalated` в Kafka
- Notification Service отправит уведомление

---

### 5. Тестирование External VAS Integration

**Создать API ключ:**
```bash
curl -X POST http://localhost:8089/api/admin/api-keys?createdBy=admin \
  -H "Content-Type: application/json" \
  -d '{
    "systemName": "Test VAS Monitor",
    "description": "Testing API key",
    "ipWhitelist": "*",
    "rateLimitPerMinute": 100
  }'
```

**Ответ (сохранить apiKey):**
```json
{
  "apiKey": "a1b2c3d4e5f6...",
  "systemName": "Test VAS Monitor",
  "active": true,
  ...
}
```

**Отправить VAS данные (JSON):**
```bash
curl -X POST http://localhost:8089/api/external/vas/record \
  -H "X-API-Key: a1b2c3d4e5f6..." \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "MRN-000001",
    "vasLevel": 7,
    "deviceId": "MONITOR-001",
    "location": "ICU-1",
    "painPlace": "Грудная клетка",
    "source": "VAS_MONITOR"
  }'
```

**Batch import (CSV):**
```bash
curl -X POST http://localhost:8089/api/external/vas/batch \
  -H "X-API-Key: a1b2c3d4e5f6..." \
  -H "Content-Type: text/csv" \
  -d 'MRN,VASLevel,DeviceID,Location
MRN-000001,5,MONITOR-001,Ward A
MRN-000002,8,MONITOR-002,ICU-1
MRN-000003,3,MONITOR-003,Ward B'
```

---

### 6. Тестирование EMR Integration (FHIR)

**Sync patient from FHIR server:**
```bash
curl -X POST http://localhost:8086/api/emr/sync/patient/example \
  -H "Authorization: Bearer $TOKEN"
```

**Check EMR changes topic:**
```bash
# Через Kafdrop: http://localhost:9000
# Топик: emr.changes
# Должны появиться сообщения о синхронизации
```

---

## Мониторинг и отладка

### Kafdrop (Kafka UI)

**URL:** http://localhost:9000

**Что можно делать:**
- Просматривать топики
- Читать сообщения
- Проверять consumer groups
- Мониторить lag

**Запуск:**
```bash
docker-compose --profile tools up -d
```

---

### Логи

**Просмотр логов Docker контейнеров:**
```bash
# Все сервисы
docker-compose logs -f

# Конкретный сервис
docker-compose logs -f dev_kafka
docker-compose logs -f dev_postgres
docker-compose logs -f dev_auth

# Последние 100 строк
docker-compose logs --tail=100 dev_emr
```

**Логи монолита:**
```bash
# В консоли где запущен mvn spring-boot:run

# ИЛИ в IDE (вкладка Run/Debug)
```

---

### Database Inspector

**Подключиться к PostgreSQL:**
```bash
# Через psql
docker exec -it dev_postgres psql -U postgres -d pain_management_db

# Посмотреть таблицы
\dt

# Посмотреть пациентов
SELECT * FROM patients LIMIT 10;
```

**Через DBeaver / DataGrip:**
- Host: localhost
- Port: 5432
- Database: pain_management_db
- User: postgres
- Password: postgres

---

### Spring Boot Actuator Endpoints

**Доступно для всех сервисов:**

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics (для Prometheus)
curl http://localhost:8080/actuator/prometheus

# Beans
curl http://localhost:8080/actuator/beans

# Environment variables
curl http://localhost:8080/actuator/env
```

---

## Частые проблемы

### Проблема 1: Kafka не стартует

**Симптомы:**
```
ERROR Exiting Kafka due to fatal exception
```

**Решение:**
```bash
# 1. Остановить всё
docker-compose down

# 2. Удалить volumes (ВНИМАНИЕ: удалит данные!)
docker-compose down -v

# 3. Перезапустить
docker-compose up -d
```

---

### Проблема 2: PostgreSQL connection refused

**Симптомы:**
```
Connection to localhost:5432 refused
```

**Решение:**
```bash
# Проверить что PostgreSQL запущен
docker ps | grep postgres

# Проверить healthcheck
docker inspect dev_postgres | grep Health

# Если не запущен
docker-compose up -d postgres

# Подождать 10-15 секунд для инициализации
```

---

### Проблема 3: Монолит не подключается к Kafka

**Симптомы:**
```
Failed to send message to topic analytics-events
```

**Решение:**
```bash
# 1. Проверить что Kafka готов
docker exec dev_kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# 2. Проверить KAFKA_BOOTSTRAP_SERVERS в application-local.yml
# Должно быть: localhost:9092 (НЕ kafka:29092)

# 3. Проверить firewall
# Windows: Разрешить порт 9092
```

---

### Проблема 4: JWT токен невалиден

**Симптомы:**
```
401 Unauthorized
```

**Решение:**
```bash
# 1. Проверить что JWT_SECRET одинаковый в монолите и Auth Service

# Монолит (application.yml)
jwt:
  secret: pain-management-secret-key...

# Auth Service (docker-compose)
JWT_SECRET: pain-management-secret-key...

# 2. Проверить срок действия токена (default: 24 часа)

# 3. Получить новый токен
curl -X POST http://localhost:8082/api/auth/login ...
```

---

### Проблема 5: Out of memory

**Симптомы:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Решение:**
```bash
# Увеличить память для монолита
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2048m"

# Увеличить память Docker (Docker Desktop Settings)
# Минимум 4GB RAM для контейнеров
```

---

## Команды для быстрого копирования

### Полный перезапуск системы

```bash
# Остановить всё
docker-compose --profile all down

# Очистить volumes (опционально)
docker-compose --profile all down -v

# Собрать образы заново
docker-compose --profile all build

# Запустить всё
docker-compose --profile all up -d

# Дождаться готовности
sleep 60

# Запустить монолит
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

### Проверка всех портов

```bash
# PowerShell
@(8080, 8082, 8085, 8086, 8087, 8088, 8089, 8091, 9092, 5432) | ForEach-Object {
    Write-Host "Port $_`: " -NoNewline
    Test-NetConnection -ComputerName localhost -Port $_ -WarningAction SilentlyContinue | Select-Object -ExpandProperty TcpTestSucceeded
}
```

---

### Создание тестовых данных

```bash
# Скрипт для создания тестовых пациентов
curl -X POST http://localhost:8080/api/nurse/patients -H "Content-Type: application/json" -d '{"firstName":"Patient1","lastName":"Test","dateOfBirth":"1990-01-01","gender":"MALE","phoneNumber":"+79991111111"}'

curl -X POST http://localhost:8080/api/nurse/patients -H "Content-Type: application/json" -d '{"firstName":"Patient2","lastName":"Test","dateOfBirth":"1985-05-15","gender":"FEMALE","phoneNumber":"+79992222222"}'

curl -X POST http://localhost:8080/api/nurse/patients -H "Content-Type: application/json" -d '{"firstName":"Patient3","lastName":"Test","dateOfBirth":"1975-12-20","gender":"MALE","phoneNumber":"+79993333333"}'
```

---

## Дополнительные ресурсы

### Complete Documentation Suite
- **[DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md)** - Complete DevOps operations guide
- **[DOCKER_COMPOSE_REFERENCE.md](DOCKER_COMPOSE_REFERENCE.md)** - Detailed Docker Compose reference
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Comprehensive testing scenarios
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Problem resolution guide

### Architecture & Design
- **Архитектурные диаграммы:** `docs/architecture/diagrams/`
- **Документация микросервисов:** `docs/microservices/`
- **Kafka события:** `docs/api/event-schemas/KAFKA_EVENT_SCHEMAS.md`
- **Прогресс миграции:** `docs/STAGE_2_PROGRESS.md`

---

**Удачного тестирования! 🚀**
