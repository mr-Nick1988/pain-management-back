# 🚀 PHASE 8: CENTRALIZED LOGGING - QUICK START

**Date:** January 24, 2026  
**Status:** ✅ IMPLEMENTATION COMPLETE

---

## ✅ ЧТО СДЕЛАНО

### 1. ELK Stack в docker-compose.dev.yml
- ✅ Elasticsearch 8.11.3 (port 9200) - хранилище логов
- ✅ Logstash 8.11.3 (port 5000) - обработка логов
- ✅ Kibana 8.11.3 (port 5601) - визуализация
- ✅ Health checks для всех компонентов
- ✅ Volumes для persistence

### 2. Logstash Pipeline Configuration
- ✅ TCP input (port 5000) для JSON логов
- ✅ Beats input (port 5044) для Filebeat
- ✅ Grok фильтры для извлечения traceId, spanId
- ✅ Output в Elasticsearch с daily indices

### 3. JSON Logging во всех 8 сервисах
- ✅ logstash-logback-encoder dependency
- ✅ logback-spring.xml для каждого сервиса
- ✅ Async logging для performance
- ✅ TraceID и SpanID в логах (Jaeger integration)

### 4. Docker Compose Updates
- ✅ LOGSTASH_HOST и LOGSTASH_PORT env vars
- ✅ Все 8 сервисов настроены

---

## 🚀 КАК ЗАПУСТИТЬ

### 1. Запуск ELK Stack

```bash
cd C:\backend_projects\pain_managment_back

# Только ELK Stack
docker-compose --profile logging up -d

# Или всё сразу (инфраструктура + микросервисы + ELK)
docker-compose --profile all up -d
```

### 2. Ожидание запуска

ELK Stack требует ~90 секунд для полного запуска:
- Elasticsearch: ~60 секунд
- Logstash: ~30 секунд (после Elasticsearch)
- Kibana: ~60 секунд (после Elasticsearch)

```bash
# Проверка статуса
docker-compose ps

# Проверка health
docker-compose ps | grep -E "elasticsearch|logstash|kibana"
```

### 3. Проверка доступности

```bash
# Elasticsearch
curl http://localhost:9200/_cluster/health

# Logstash
curl http://localhost:9600

# Kibana
curl http://localhost:5601/api/status
```

---

## 🔍 ДОСТУП К UI

### Kibana (Log Visualization)
- **URL:** http://localhost:5601
- **Authentication:** Disabled (dev mode)
- **First Time:** Configure index pattern

### Elasticsearch (Direct API)
- **URL:** http://localhost:9200
- **Indices:** http://localhost:9200/_cat/indices?v

### Logstash (Monitoring API)
- **URL:** http://localhost:9600
- **Stats:** http://localhost:9600/_node/stats

---

## 📊 НАСТРОЙКА KIBANA (FIRST TIME)

### 1. Открыть Kibana
```
http://localhost:5601
```

### 2. Создать Index Pattern
1. Menu → Stack Management → Index Patterns
2. Click "Create index pattern"
3. Index pattern name: `painmgmt-logs-*`
4. Time field: `@timestamp`
5. Click "Create index pattern"

### 3. Просмотр Логов
1. Menu → Discover
2. Select index pattern: `painmgmt-logs-*`
3. Set time range (Last 15 minutes)
4. View logs!

---

## 🔎 ПОИСК ЛОГОВ

### По сервису
```
service:"api-gateway"
service:"authentication"
service:"emr-service"
```

### По TraceID (correlation с Jaeger)
```
trace_id:"64a7f2e8b9c1d3f5"
```

### По уровню логирования
```
level:"ERROR"
level:"WARN"
```

### По сообщению
```
message:"Circuit Breaker"
message:"exception"
```

### Комбинированный поиск
```
service:"api-gateway" AND level:"ERROR" AND trace_id:*
```

---

## 📈 ELASTICSEARCH INDICES

Логи хранятся в daily indices по сервисам:
```
painmgmt-logs-api-gateway-2026.01.24
painmgmt-logs-authentication-2026.01.24
painmgmt-logs-emr-service-2026.01.24
painmgmt-logs-notification-2026.01.24
painmgmt-logs-pain-escalation-2026.01.24
painmgmt-logs-external-vas-2026.01.24
painmgmt-logs-reporting-2026.01.24
painmgmt-logs-backup-2026.01.24
```

### Просмотр indices
```bash
curl http://localhost:9200/_cat/indices?v
```

### Удаление старых indices (cleanup)
```bash
# Удалить indices старше 7 дней
curl -X DELETE "http://localhost:9200/painmgmt-logs-*-2026.01.17"
```

---

## 🔗 ИНТЕГРАЦИЯ С JAEGER

Каждый лог содержит `trace_id` и `span_id` из Jaeger!

### Workflow:
1. Найти ошибку в Kibana → получить `trace_id`
2. Открыть Jaeger UI: http://localhost:16686
3. Поиск по TraceID в Jaeger
4. Видеть full call chain!

**Или наоборот:**
1. Найти медленный trace в Jaeger → получить `trace_id`
2. Поиск в Kibana: `trace_id:"..."`
3. Видеть detailed logs!

---

## ⚙️ НАСТРОЙКИ

### Memory Settings (docker-compose.dev.yml)

```yaml
elasticsearch:
  environment:
    ES_JAVA_OPTS: "-Xms512m -Xmx512m"  # Увеличить для prod
    
logstash:
  environment:
    LS_JAVA_OPTS: "-Xms256m -Xmx256m"  # Увеличить для prod
```

### Log Retention

По умолчанию: хранится всё.

Для production добавить Curator или ILM (Index Lifecycle Management):
```
- Delete indices older than 30 days
- Archive to S3 after 7 days
```

---

## 🧪 ТЕСТИРОВАНИЕ

### 1. Запустить сервисы
```bash
docker-compose --profile all up -d
```

### 2. Сгенерировать логи
```bash
# Запросы к API Gateway
curl http://localhost:8000/actuator/health

# Ошибка (для тестирования ERROR logs)
curl http://localhost:8000/nonexistent
```

### 3. Проверить в Kibana
```
http://localhost:5601/app/discover
```

### 4. Найти trace в Jaeger
```
http://localhost:16686
```

---

## 🐛 TROUBLESHOOTING

### Logstash не получает логи
```bash
# Проверить connectivity
docker exec dev_logstash curl -f http://elasticsearch:9200

# Проверить pipeline
docker logs dev_logstash --tail 50
```

### Kibana не открывается
```bash
# Проверить Elasticsearch
curl http://localhost:9200/_cluster/health

# Логи Kibana
docker logs dev_kibana --tail 50
```

### Indices не создаются
```bash
# Проверить Logstash pipeline
docker exec dev_logstash cat /usr/share/logstash/pipeline/logstash.conf

# Restart Logstash
docker-compose restart logstash
```

---

## 📊 NEXT STEPS

1. ✅ Создать custom Kibana dashboards:
   - Service Health Dashboard
   - Error Rate Dashboard
   - Response Time Dashboard

2. ✅ Setup alerts:
   - Email on ERROR spikes
   - Slack notifications
   - PagerDuty integration

3. ✅ Production settings:
   - Enable Elasticsearch security (xpack.security.enabled: true)
   - Setup Elasticsearch cluster (3+ nodes)
   - Configure ILM for log retention
   - Add Filebeat for container logs

---

## ✅ PHASE 8 COMPLETE!

**Roadmap Progress:** 7/12 patterns (58%)

**Next Phase:** Phase 4 - Saga Pattern OR Phase 12 - Security Hardening

**Last Updated:** January 24, 2026, 18:35 UTC+2
