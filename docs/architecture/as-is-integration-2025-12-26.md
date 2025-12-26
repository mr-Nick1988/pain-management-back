# AS-IS: Интеграции монолита с микросервисами и внешними системами (26.12.2025)

Документ фиксирует текущее состояние взаимодействий монолита с внешними микросервисами и системами, чтобы иметь исходную точку для дальнейшего безопасного выделения микросервисов.

## Обзор текущего состояния
- Монолит работает автономно с собственной БД (Postgres) и ведет аналитические события/логи в MongoDB.
- Единственная входящая интеграция по шине сообщений: Kafka-топик `emr.changes` (монолит — consumer).
- Исходящий HTTP-клиент: HAPI FHIR (внешняя EMR система) — используется для синхронизации пациентских данных/наблюдений.
- В репозитории присутствуют документы и docker-compose для микросервисов `authentication-service`, `reporting-service`, `logging-service`, `backup-restore`, но прямой связи монолита с ними по коду не обнаружено (статус: «готово к интеграции», см. ниже Гэп-анализ).

## Инфраструктура (локальная разработка)
См. `docker-compose.dev.yml` и `docs/runbook-local-dev.md`.
- Postgres (монолит): порт 5432 (`postgres-main`).
- Kafka (KRaft): порт 9092 + топики (auto-create и job создания тем).
- Mongo (для logging-профиля): порт 27017 (используется logging-service; монолит может ходить в локальный Mongo по `application-local.yml`).
- Доп. Postgres (analytics/reporting): 5433 (`postgres-analytics`) — для reporting-service.

## Интеграции монолита (по коду)
- Входящие события
  - Kafka consumer: `emr.changes`
    - Группа: `pain-monolith-emr-group`
    - Обработчик: `emr_recalculation.listener.EmrEventListener#handleEmrChanges(...)`
    - Конфиг: `application.yml` (`spring.kafka.*`, trusted packages — `*`).
- Исходящие вызовы
  - HTTP FHIR: HAPI FHIR R4 клиент → внешний FHIR сервер
    - Конфиг: `external_emr_integration_service.FhirConfig` (`fhir.server.url`, таймауты)
    - Клиент: `external_emr_integration_service.client.HapiFhirClient`
  - WebSocket/STOMP: внутренние уведомления UI (не микросервис, внутрипроцессно)
- Хранилища данных
  - Postgres: основная БД домена (JPA/Hibernate)
  - MongoDB: аналитика и техлоги монолита (`analytics.entity.AnalyticsEvent`, `analytics.entity.LogEntry`)

## Микросервисы рядом (есть образы/доки, интеграция в монолите не найдена)
- Authentication Service (порт 8082)
  - Док: `docs/microservices/authentication-service-docs.md`
  - Назначение: выдача/валидация JWT в HttpOnly cookie.
  - Статус интеграции: в монолите сейчас логин реализован локально (`common.persons.service.PersonService`), обращений в `auth-service` не найдено.
- Reporting Service (порт 8091)
  - Док: `docs/microservices/reporting-service-docs.md`
  - Назначение: дневные отчеты, потребление Kafka-команд `reporting-commands`.
  - Статус интеграции: в монолите присутствует собственный модуль `reporting` (контроллеры/экспорты). Публикатора Kafka-команд в монолите не найдено; REST вызовов к сервису — также не найдено.
- Logging Service (порт 8081→8083 внутри контейнера)
  - Док: `docs/microservices/Logging-Service-Documentation.md`
  - Назначение: централизованный прием логов из Kafka (`logging-events`) в Mongo.
  - Статус интеграции: продьюсера `logging-events` в монолите не найдено; монолит пишет аналитику в свою Mongo напрямую.
- Backup & Restore (порт 8085)
  - Док: `docs/microservices/backup-restore-documentation.md`
  - Назначение: бэкап/рестор Postgres и Mongo, история операций в SQL.
  - Статус интеграции: внутрimonolith endpoints отсутствуют; предполагается ручной/внешний вызов API микросервиса.

## Kafka топики (compose)
- Создаются: `analytics-events`, `logging-events`, `reporting-commands`.
- Фактическое использование монолитом сейчас:
  - Consumer: `emr.changes` (в compose не создается отдельной утилитой — ожидается внешний источник).
  - Producer: не обнаружено (ни для `logging-events`, ни для `reporting-commands`).

## Конфигурация монолита (ключевое)
- `src/main/resources/application.yml`
  - `spring.kafka.bootstrap-servers: localhost:9092`
  - Consumer group/id, JsonDeserializer и trusted packages
- `src/main/resources/application-local.yml`
  - Postgres DSN: `${DB_URL:jdbc:postgresql://localhost:5432/pain_management_db}`
  - Mongo URI (аналитика монолита): `${LOGGING_MONGODB_URI:mongodb://localhost:27017/analyticsdb}`

## Внешние системы
- FHIR сервер (по умолчанию public HAPI: `http://hapi.fhir.org/baseR4`)
  - Пакет: `external_emr_integration_service`
  - Бины: `FhirContext`, `IGenericClient`
  - Сервис: `EmrSyncScheduler` (cron 6 ч), `EmrChangeDetectionService`

## Основные потоки (as-is)
- EMR sync (HTTP)
  - `EmrSyncScheduler` → `HapiFhirClient` → обновление `EmrRepository` → `EmrRecalculationService` → WebSocket уведомления
- EMR change (Kafka)
  - Kafka `emr.changes` → `EmrEventListener` → загрузка Patient/EMR → `EmrRecalculationService` → WebSocket
- Аутентификация (локально в монолите)
  - `PersonController`/`PersonService` → `PersonRepository` → публикация `UserLoginEvent` → `AnalyticsEventListener` → Mongo (Analytics)
- Отчетность (локально в монолите)
  - `reporting` пакет: агрегация, REST, экспорт (Excel/PDF)

Диаграммы PlantUML:
- Контекст: `docs/architecture/diagrams/as_is_context.puml`
- Компоненты: `docs/architecture/diagrams/as_is_components.puml`
- Последовательности:
  - `seq_emr_sync.puml`
  - `seq_emr_kafka_change.puml`
  - `seq_user_login.puml`
  - `seq_reporting.puml`

## Доказательная база (ключевые файлы)
- Kafka consumer: `src/main/java/pain_helper_back/emr_recalculation/listener/EmrEventListener.java`
- Kafka consumer настройки: `src/main/resources/application.yml`
- FHIR клиент/конфиг: `src/main/java/pain_helper_back/external_emr_integration_service/FhirConfig.java`, `client/HapiFhirClient.java`
- EMR sync: `src/main/java/pain_helper_back/external_emr_integration_service/service/EmrSyncScheduler.java`
- Аналитика (Mongo):
  - Модель/репозитории: `analytics/entity/*`, `analytics/repository/*`
  - Листенер доменных событий: `analytics/listener/AnalyticsEventListener.java`
  - Сервис выборок: `analytics/service/AnalyticsService.java`
- Локальная аутентификация: `common/persons/service/PersonService.java`
- Docker-compose: `docker-compose.dev.yml`, `docs/runbook-local-dev.md`
- Документация микросервисов: `docs/microservices/*`

## Гэп-анализ (что готово / чего нет)
- Authentication-service: есть отдельный сервис/документация, но монолит пока не валидирует JWT и не проксирует к нему — фактическая интеграция отсутствует.
- Reporting-service: сервис готов (Kafka consumer + REST), но монолит не публикует `reporting-commands` и не вызывает REST `/api/reports` — продолжает собственную отчетность внутри монолита.
- Logging-service: сервис готов (Kafka consumer → Mongo), монолит не шлет события в `logging-events` — аналитику пишет сам в свою Mongo.
- Backup-restore: сервис есть; интеграции/оркестрации из монолита нет (ожидается ручной/ops-вызов).

## Рекомендации по поэтапному выделению микросервисов
1) Аутентификация
   - Внедрить в монолит middleware проверки JWT (через `authentication-service /api/auth/validate`), переключить login/logout на сервис, убрать хранение паролей в монолите.
2) Отчетность
   - Добавить продьюсер Kafka `reporting-commands` из монолита и/или перейти на REST вызовы `/api/reports` для UI.
   - Планом убрать пакет `reporting` из монолита после миграции.
3) Логирование/Аналитика
   - Добавить продьюсер Kafka в монолите (`logging-events`) и перенести сохранение логов/аналитики в logging-service; выровнять схемы Mongo.
4) EMR интеграция
   - Рассмотреть вынос `external_emr_integration_service` + `emr_recalculation` в выделенный сервис (HTTP+Kafka), определив событие «EMR recalculated».
5) Операции/бэкапы
   - Если нужны self-service операции из UI — добавить фасадный REST в монолит, проксирующий в backup-restore.

## Примечания
- В `EmrEventListener` указана `containerFactory = "emrKafkaListenerContainerFactory"`, но объявления бина с таким именем в репозитории не найдено — проверить конфигурацию перед продакшен-использованием.
- `docker-compose.dev.yml` создает топики `analytics-events`, `logging-events`, `reporting-commands` — они сейчас монолитом не используются; оставить как задел.

