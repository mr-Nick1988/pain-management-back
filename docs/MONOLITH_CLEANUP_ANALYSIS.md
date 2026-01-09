# Анализ оставшегося кода монолита

**Дата:** 2026-01-09  
**Статус:** После удаления VAS_external_integration и pain_escalation_tracking

---

## 📊 Что осталось в монолите

### 1. **emr_recalculation** (5 файлов)
**Статус:** ❌ **УДАЛИТЬ** - дублирует функционал EMR Integration Service

**Файлы:**
- `EmrChangeAlertDTO.java`
- `EmrEventListener.java` - Kafka consumer для emr.changes
- `EmrChangeDetectionService.java`
- `EmrRecalculation Service.java`
- `WebSocketNotificationService.java`

**Причина удаления:**
- Этот функционал уже реализован в **EMR Integration Service** (микросервис)
- Дублирование логики обработки EMR событий
- WebSocket уведомления теперь в **Notification Service**

**Действие:** Удалить весь пакет `emr_recalculation`

---

### 2. **external_emr_integration_service** (21 файл)
**Статус:** ❌ **УДАЛИТЬ** - это уже вынесено в микросервис

**Файлы:**
- Controllers: `EmrIntegrationController.java`
- Services: `EmrIntegrationService`, `EmrIntegrationServiceImpl`, `EmrSyncScheduler`
- FHIR клиент: `HapiFhirClient.java`, `FhirConfig.java`
- DTO: FhirPatientDTO, FhirObservationDTO, EmrSyncResultDTO и др.
- Entities: `EmrMapping.java`
- Уведомления: `EmailNotificationService`, `WebSocketNotificationService`

**Причина удаления:**
- Полностью дублирует **EMR Integration Service** (порт 8086)
- FHIR интеграция перенесена в микросервис
- Уведомления теперь в **Notification Service**

**Действие:** Удалить весь пакет `external_emr_integration_service`

---

### 3. **performance_SLA_monitoring** (10 файлов)
**Статус:** ❌ **УДАЛИТЬ** - заменить на Prometheus/Grafana

**Файлы:**
- Aspect: `PerformanceMonitoringAspect.java` (AOP для мониторинга методов)
- Service: `PerformanceMonitoringService`, `PerformanceMonitoringServiceImpl`
- Controller: `PerformanceController.java`
- Entity: `PerformanceMetric.java`
- Repository: `PerformanceMetricRepository.java`
- Config: `PerformanceSlaConfig.java`
- DTO: PerformanceMetricDTO, PerformanceStatisticDTO, SlaViolationDTO

**Причина удаления:**
- Кастомный мониторинг производительности - не индустриальный стандарт
- Заменяется на **Prometheus + Grafana + Spring Boot Actuator + Micrometer**
- MongoDB для метрик - избыточно (Prometheus использует TimeSeries DB)

**Действие:** 
1. Удалить весь пакет `performance_SLA_monitoring`
2. Добавить Spring Boot Actuator + Micrometer в монолит
3. Настроить Prometheus/Grafana в STAGE 4

---

### 4. **validation** (1 файл)
**Статус:** ✅ **ОСТАВИТЬ** - утилита для валидации

**Файлы:**
- `ValidationGroups.java` - группы валидации для Bean Validation

**Причина сохранения:**
- Общая утилита для всех модулей монолита
- Используется в DTO для группировки валидаций
- Не является бизнес-логикой, которую нужно выносить

**Действие:** Оставить как есть

---

### 5. **websocket** (3 файла)
**Статус:** ⚠️ **ЧАСТИЧНО УДАЛИТЬ** - оставить только тестовый контроллер

**Файлы:**
- `WebSocketTestController.java` - тестовый контроллер для WebSocket
- `UnifiedNotificationDTO.java` - DTO для уведомлений
- `UnifiedNotificationService.java` - сервис для отправки уведомлений

**Причина частичного удаления:**
- WebSocket уведомления теперь в **Notification Service** (порт 8087)
- `UnifiedNotificationService` дублирует функционал микросервиса

**Действие:**
1. Удалить `UnifiedNotificationDTO.java`
2. Удалить `UnifiedNotificationService.java`
3. **ОСТАВИТЬ** `WebSocketTestController.java` - для тестирования подключения к Notification Service

---

## 🎯 Итоговые действия

### Удалить (39 файлов):
1. ❌ `emr_recalculation/` - 5 файлов
2. ❌ `external_emr_integration_service/` - 21 файл
3. ❌ `performance_SLA_monitoring/` - 10 файлов
4. ❌ `websocket/UnifiedNotificationDTO.java` - 1 файл
5. ❌ `websocket/UnifiedNotificationService.java` - 1 файл
6. ❌ MongoDB конфигурация для performance metrics

### Оставить:
1. ✅ `validation/ValidationGroups.java`
2. ✅ `websocket/WebSocketTestController.java`

### Добавить (STAGE 4):
1. ➕ Spring Boot Actuator
2. ➕ Micrometer Prometheus registry
3. ➕ Prometheus/Grafana Docker Compose services

---

## 📦 Что остается в монолите после очистки

**Core Business Logic:**
- `admin/` - управление пользователями и ролями
- `doctor/` - функционал врача (пациенты, рекомендации)
- `nurse/` - функционал медсестры (VAS, EMR, пациенты)
- `anesthesiologist/` - управление протоколами лечения и эскалациями
- `treatment_protocol/` - протоколы лечения боли

**Common/Shared:**
- `common/` - общие сущности (Patient, EMR, VAS, Recommendation)
- `enums/` - перечисления
- `config/` - конфигурации (Security, CORS, ModelMapper и др.)
- `kafka/` - Kafka producer для analytics events
- `validation/` - группы валидации

**Интеграции:**
- `client/` - клиенты для микросервисов (Auth, Reporting)
- `internal/` - внутренние утилиты

---

## 🔄 Взаимодействие с микросервисами

**Монолит теперь использует:**

1. **Authentication Service** (порт 8082)
   - Проверка JWT токенов
   - Управление аутентификацией

2. **Reporting Service** (порт 8091)
   - Генерация отчетов
   - Аналитика

3. **EMR Integration Service** (порт 8086)
   - Синхронизация с внешними EMR системами (FHIR)
   - Обработка EMR событий

4. **Notification Service** (порт 8087)
   - Email уведомления
   - WebSocket real-time уведомления
   - Push notifications

5. **Pain Escalation Service** (порт 8088)
   - Автоматическое отслеживание эскалации боли
   - Анализ трендов VAS
   - Управление дозами

6. **External VAS Integration Service** (порт 8089)
   - Интеграция с внешними VAS устройствами
   - API key management
   - Multi-format parsing (JSON/XML/CSV)

**Kafka Topics (монолит публикует):**
- `analytics-events` - бизнес-события для аналитики
- `emr.created` - создание EMR записей
- `vas.recorded` - запись VAS
- `patient.registered` - регистрация пациентов
- `recommendation.created` - создание рекомендаций

**Kafka Topics (монолит подписан):**
- `emr.changes` - изменения в EMR (от EMR Integration Service)

---

## 📈 Метрики очистки

**До очистки (после STAGE 3):**
- Удалено: 34 файла (VAS + Pain Escalation)
- Осталось проблемных: 39 файлов (дубликаты функционала)

**После полной очистки:**
- Дополнительно удалим: 39 файлов
- **Итого очистка:** 73 файла (~4,500+ строк кода)
- **Монолит станет легче на ~45%!**

---

## 🎯 Следующие шаги

1. ✅ Анализ завершен
2. ⏳ Удалить 39 файлов
3. ⏳ Переделать application.yml (с комментариями)
4. ⏳ Переделать application-local.yml (с комментариями)
5. ⏳ Переделать docker-compose.dev.yml (упростить, добавить комментарии)
6. ⏳ Создать новые архитектурные диаграммы (PlantUML)
7. ⏳ Добавить Prometheus/Grafana (STAGE 4)
8. ⏳ Создать документацию запуска и тестирования

---

**Prepared by:** Cascade AI  
**Review Required:** Yes - перед удалением кода
