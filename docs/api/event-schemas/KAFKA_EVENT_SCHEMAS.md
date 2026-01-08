# 📨 KAFKA EVENT SCHEMAS

**Версия:** 1.0  
**Дата:** 08.01.2026

---

## 📋 Обзор

Документ описывает все Kafka топики и схемы событий для микросервисной архитектуры Pain Management System.

**Принципы:**
- Event-driven architecture (асинхронная коммуникация)
- JSON формат для всех событий
- ISO 8601 для временных меток
- Correlation ID для трассировки
- Идемпотентность обработки событий

---

## 🔄 KAFKA ТОПИКИ

### Инфраструктурные топики:
- **`analytics-events`** — бизнес-события для аналитики
- **`logging-events`** — технические логи приложения
- **`reporting-commands`** — команды для генерации отчетов

### Доменные топики:
- **`emr.changes`** — изменения в медицинских картах пациентов
- **`emr.upserted`** — полные снапшоты EMR после обновления
- **`emr.critical.alerts`** — критические изменения EMR
- **`notification.requests`** — запросы на отправку уведомлений
- **`dose.administered`** — события о введении препаратов
- **`pain.escalated`** — события эскалации боли
- **`vas.external.received`** — VAS данные из внешних систем

---

## 📊 СХЕМЫ СОБЫТИЙ

### 1. Analytics Events (`analytics-events`)

#### Базовая структура:
```json
{
  "eventId": "uuid",
  "eventType": "ENUM",
  "timestamp": "ISO-8601",
  "correlationId": "uuid",
  "userId": "string",
  "sessionId": "string",
  "metadata": {}
}
```

#### Event Types:

##### RECOMMENDATION_GENERATED
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "RECOMMENDATION_GENERATED",
  "timestamp": "2025-01-08T16:00:00Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001",
  "userId": "NURSE-123",
  "sessionId": "session-456",
  "metadata": {
    "patientId": "MRN-123",
    "recommendationId": 789,
    "painLevel": 8,
    "primaryDrug": "Morphine",
    "regimenHierarchy": 3,
    "generationDurationMs": 450
  }
}
```

##### RECOMMENDATION_APPROVED
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440002",
  "eventType": "RECOMMENDATION_APPROVED",
  "timestamp": "2025-01-08T16:10:00Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001",
  "userId": "DOCTOR-456",
  "sessionId": "session-789",
  "metadata": {
    "patientId": "MRN-123",
    "recommendationId": 789,
    "approvalComment": "Approved without modifications",
    "timeToApprovalMinutes": 10
  }
}
```

##### RECOMMENDATION_REJECTED
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440003",
  "eventType": "RECOMMENDATION_REJECTED",
  "timestamp": "2025-01-08T16:15:00Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001",
  "userId": "DOCTOR-456",
  "sessionId": "session-789",
  "metadata": {
    "patientId": "MRN-123",
    "recommendationId": 789,
    "rejectionReason": "Patient allergic to morphine",
    "rejectedBy": "DOCTOR-456"
  }
}
```

##### ESCALATION_CREATED
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440004",
  "eventType": "ESCALATION_CREATED",
  "timestamp": "2025-01-08T16:20:00Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001",
  "userId": "DOCTOR-456",
  "sessionId": "session-789",
  "metadata": {
    "patientId": "MRN-123",
    "escalationId": 101,
    "priority": "HIGH",
    "reason": "Patient pain level remains at 9 after treatment",
    "escalatedBy": "DOCTOR-456"
  }
}
```

##### PATIENT_REGISTERED
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440005",
  "eventType": "PATIENT_REGISTERED",
  "timestamp": "2025-01-08T14:00:00Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440005",
  "userId": "NURSE-123",
  "sessionId": "session-111",
  "metadata": {
    "patientId": "MRN-124",
    "firstName": "John",
    "lastName": "Doe",
    "age": 45,
    "registeredBy": "NURSE-123"
  }
}
```

---

### 2. Logging Events (`logging-events`)

#### Структура (используется Logging Service):
```json
{
  "id": "uuid",
  "timestamp": "2025-01-08T16:00:00",
  "className": "pain.management.nurse.service.NurseServiceImpl",
  "methodName": "createRecommendation",
  "methodSignature": "RecommendationDTO createRecommendation(Long, String)",
  "arguments": "{\"patientId\":123,\"createdBy\":\"NURSE-123\"}",
  "durationMs": 456,
  "success": true,
  "errorMessage": null,
  "errorStackTrace": null,
  "userId": "NURSE-123",
  "sessionId": "session-456",
  "logCategory": "BUSINESS",
  "level": "INFO",
  "module": "nurse",
  "traceId": "trace-001",
  "spanId": "span-001"
}
```

**Log Levels:** `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`

**Log Categories:** `BUSINESS`, `TECHNICAL`, `SECURITY`, `PERFORMANCE`, `AUDIT`

**Modules:** `admin`, `doctor`, `nurse`, `anesthesiologist`, `emr-integration`, `notification`, `reporting`, `analytics`

---

### 3. Reporting Commands (`reporting-commands`)

#### Command Types:

##### GENERATE_DAILY
```json
{
  "commandId": "550e8400-e29b-41d4-a716-446655440006",
  "commandType": "GENERATE_DAILY",
  "timestamp": "2025-01-08T00:30:00Z",
  "payload": {
    "reportDate": "2025-01-07",
    "regenerate": false
  }
}
```

##### GENERATE_YESTERDAY
```json
{
  "commandId": "550e8400-e29b-41d4-a716-446655440007",
  "commandType": "GENERATE_YESTERDAY",
  "timestamp": "2025-01-08T10:00:00Z",
  "payload": {
    "regenerate": true
  }
}
```

##### GENERATE_PERIOD
```json
{
  "commandId": "550e8400-e29b-41d4-a716-446655440008",
  "commandType": "GENERATE_PERIOD",
  "timestamp": "2025-01-08T10:00:00Z",
  "payload": {
    "startDate": "2025-01-01",
    "endDate": "2025-01-07",
    "regenerate": false
  }
}
```

---

### 4. EMR Changes (`emr.changes`)

#### Структура:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440009",
  "mrn": "MRN-123",
  "patientId": 123,
  "timestamp": "2025-01-08T16:00:00Z",
  "source": "FHIR_SERVER",
  "changes": [
    {
      "field": "gfr",
      "oldValue": "Normal (>90)",
      "newValue": "Mild (60-89)",
      "severity": "WARNING"
    },
    {
      "field": "plt",
      "oldValue": "250.0",
      "newValue": "80.0",
      "severity": "CRITICAL"
    }
  ],
  "overallSeverity": "CRITICAL",
  "triggersRecalculation": true,
  "metadata": {
    "fhirResourceId": "Patient/123",
    "fhirServerUrl": "http://hapi.fhir.org/baseR4",
    "syncJobId": "job-456"
  }
}
```

**Source Values:** `FHIR_SERVER`, `MANUAL_ENTRY`, `EXTERNAL_HOSPITAL`, `MOCK_GENERATOR`

**Severity Levels:** `INFO`, `WARNING`, `CRITICAL`

---

### 5. EMR Upserted (`emr.upserted`)

#### Полный снапшот EMR после обновления:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440010",
  "mrn": "MRN-123",
  "patientId": 123,
  "timestamp": "2025-01-08T16:00:00Z",
  "emrVersion": 5,
  "emrData": {
    "gfr": "Mild (60-89)",
    "childPughScore": "A",
    "plt": 80.0,
    "wbc": 7.5,
    "sodium": 138.0,
    "sat": 97.0,
    "height": 175.0,
    "weight": 80.0,
    "diagnoses": [
      {
        "code": "M54.5",
        "name": "Low back pain"
      }
    ]
  },
  "source": "FHIR_SERVER",
  "observedAt": "2025-01-08T15:55:00Z"
}
```

---

### 6. Critical EMR Alerts (`emr.critical.alerts`)

#### Алерты о критических изменениях:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440011",
  "mrn": "MRN-123",
  "patientId": 123,
  "timestamp": "2025-01-08T16:00:00Z",
  "alertType": "CRITICAL_VALUE_CHANGE",
  "alerts": [
    {
      "parameter": "plt",
      "currentValue": "80.0",
      "threshold": "100.0",
      "message": "Platelet count below safe threshold",
      "requiresAction": true,
      "recommendedAction": "Review current medication recommendations"
    }
  ],
  "priority": "HIGH"
}
```

**Alert Types:** 
- `CRITICAL_VALUE_CHANGE`
- `CONTRAINDICATION_DETECTED`
- `DRUG_INTERACTION_WARNING`
- `ALLERGY_ALERT`

**Priority Levels:** `LOW`, `MEDIUM`, `HIGH`, `URGENT`

---

### 7. Notification Requests (`notification.requests`)

#### Email Notification:
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440012",
  "type": "EMAIL",
  "timestamp": "2025-01-08T16:00:00Z",
  "priority": "HIGH",
  "recipient": {
    "email": "doctor@hospital.com",
    "userId": "DOCTOR-456",
    "name": "Dr. Smith"
  },
  "subject": "Critical Pain Escalation - MRN-123",
  "body": "Patient MRN-123 (John Doe) requires immediate attention. Pain level escalated to 9 after treatment.",
  "metadata": {
    "patientId": "MRN-123",
    "escalationId": 101,
    "templateId": "escalation-alert",
    "correlationId": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

#### WebSocket Notification:
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440013",
  "type": "WEBSOCKET",
  "timestamp": "2025-01-08T16:00:00Z",
  "priority": "MEDIUM",
  "recipient": {
    "userId": "DOCTOR-456",
    "sessionId": "session-789"
  },
  "event": "RECOMMENDATION_READY",
  "payload": {
    "patientId": "MRN-123",
    "recommendationId": 789,
    "message": "New recommendation generated for patient MRN-123"
  }
}
```

**Notification Types:** `EMAIL`, `WEBSOCKET`, `SMS`, `PUSH`

**Priority Levels:** `LOW`, `MEDIUM`, `HIGH`, `URGENT`

---

### 8. Dose Administered (`dose.administered`)

#### Событие о введении препарата:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440014",
  "patientId": "MRN-123",
  "timestamp": "2025-01-08T16:00:00Z",
  "administeredBy": "NURSE-123",
  "dose": {
    "drugName": "Morphine",
    "dosage": "10mg",
    "route": "IV",
    "administeredAt": "2025-01-08T16:00:00Z"
  },
  "recommendationId": 789,
  "metadata": {
    "location": "Ward A, Room 301",
    "vitalsBeforeDose": {
      "bloodPressure": "120/80",
      "heartRate": 75,
      "respiratoryRate": 16
    }
  }
}
```

---

### 9. Pain Escalated (`pain.escalated`)

#### Событие эскалации боли:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440015",
  "patientId": "MRN-123",
  "timestamp": "2025-01-08T16:20:00Z",
  "escalation": {
    "previousPainLevel": 6,
    "currentPainLevel": 9,
    "timeElapsedMinutes": 120,
    "escalatedBy": "NURSE-123",
    "reason": "Pain increased despite medication"
  },
  "currentTreatment": {
    "drugName": "Morphine",
    "dosage": "10mg",
    "administeredAt": "2025-01-08T14:00:00Z"
  },
  "requiresReview": true
}
```

---

### 10. External VAS Received (`vas.external.received`)

#### VAS данные из внешней системы:
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440016",
  "externalSystemId": "EXTERNAL-HOSPITAL-A",
  "timestamp": "2025-01-08T16:00:00Z",
  "apiKeyId": "key-123",
  "vasData": {
    "patientIdentifier": "EXT-PAT-456",
    "painLevel": 7,
    "location": "Lower back",
    "description": "Chronic pain, worsening",
    "recordedAt": "2025-01-08T15:55:00Z"
  },
  "matchedPatient": {
    "mrn": "MRN-123",
    "matchConfidence": "HIGH"
  },
  "processed": false
}
```

---

## 🔐 SECURITY & VALIDATION

### Обязательные поля для всех событий:
- `eventId` (UUID v4)
- `timestamp` (ISO 8601 с timezone)
- `eventType` или `commandType`

### Валидация:
- **Timestamp format:** `yyyy-MM-dd'T'HH:mm:ss'Z'` или с timezone offset
- **UUID format:** `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`
- **Enum values:** строгая проверка допустимых значений
- **Required fields:** все обязательные поля должны быть present и non-null

### Идемпотентность:
- Consumers должны обрабатывать события идемпотентно
- Использовать `eventId` для дедупликации
- Хранить processed event IDs для предотвращения повторной обработки

---

## 📈 MONITORING & METRICS

### Метрики для отслеживания:
- **Message lag:** задержка обработки сообщений
- **Processing time:** время обработки события
- **Error rate:** процент ошибок обработки
- **Dead letter queue size:** количество failed messages
- **Throughput:** сообщений в секунду

### Алерты:
- Lag > 1000 сообщений
- Error rate > 5%
- Processing time > 5 секунд
- DLQ не пустая

---

## 🔄 CONSUMER GROUPS

### Рекомендуемые consumer groups:

**analytics-events:**
- `analytics-service-group` (Analytics & Monitoring Service)

**logging-events:**
- `logging-service-group` (Logging Service)

**reporting-commands:**
- `reporting-service-group` (Reporting Service)

**emr.changes:**
- `pain-monolith-emr-group` (Monolith - для пересчета рекомендаций)
- `analytics-emr-group` (Analytics Service - для метрик)

**notification.requests:**
- `notification-service-group` (Notification Service)

**dose.administered:**
- `pain-escalation-service-group` (Pain Escalation Tracking Service)
- `analytics-dose-group` (Analytics Service)

**pain.escalated:**
- `notification-escalation-group` (Notification Service - для алертов)
- `analytics-escalation-group` (Analytics Service)

---

## 🛠️ PRODUCER GUIDELINES

### Монолит (Pain Management Core):
**Produces:**
- `analytics-events` (все бизнес-события)
- `reporting-commands` (запросы на отчеты)
- `notification.requests` (email/websocket notifications)

### EMR Integration Service:
**Produces:**
- `emr.changes` (изменения EMR)
- `emr.upserted` (полные снапшоты)
- `emr.critical.alerts` (критические алерты)

### Pain Escalation Tracking Service:
**Produces:**
- `dose.administered` (введенные дозы)
- `pain.escalated` (эскалации боли)

### External VAS Integration Service:
**Produces:**
- `vas.external.received` (внешние VAS данные)

---

## 📊 KAFKA CONFIGURATION

### Рекомендуемые настройки:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    
    # Producer config
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      
    # Consumer config
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: "*"
```

### Partition strategy:
- **По умолчанию:** 3 партиции на топик
- **High volume топики** (`analytics-events`): 6 партиций
- **Ключи партиционирования:** `patientId`, `userId`, `mrn`

---

**Версия документа:** 1.0  
**Последнее обновление:** 08.01.2026
