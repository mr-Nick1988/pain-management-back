# Pain Escalation Tracking Service Documentation

**Version:** 1.0.0  
**Port:** 8088  
**Status:** ✅ Implemented  
**Location:** `C:\backend_projects\microservices\pain-escalation-service\`

---

## Overview

Microservice for tracking pain escalations, VAS (Visual Analog Scale) trends, and dose administrations with automatic escalation detection.

### Key Features

- ✅ VAS record tracking and trend analysis
- ✅ **Automatic pain escalation detection** (VAS increase ≥2)
- ✅ Dose administration logging
- ✅ Kafka event publishing for pain events
- ✅ Priority calculation: LOW, MEDIUM, HIGH, CRITICAL
- ✅ Escalation workflow: OPEN → IN_PROGRESS → RESOLVED

---

## Architecture

### Tech Stack

- **Java 21**
- **Spring Boot 3.5.5**
- **PostgreSQL** (VAS records, escalations, doses)
- **Apache Kafka** (event streaming)
- **Liquibase** (migrations)

### Database Schema

**Tables:**
1. `vas_records` - Complete VAS history for all patients
2. `pain_escalations` - Escalation events with priority and status
3. `dose_administrations` - Pain medication administration log

---

## Auto-Escalation Logic

### Triggers

When VAS is recorded:
1. Compare with previous VAS
2. **If increase ≥2** → Create escalation automatically
3. Calculate priority based on current VAS and change magnitude
4. Publish to Kafka
5. Notification Service alerts anesthesiologist

### Priority Calculation

- **CRITICAL:** Current VAS ≥7 OR VAS change ≥4
- **HIGH:** Current VAS ≥6 OR VAS change ≥3
- **MEDIUM:** VAS change ≥2
- **LOW:** VAS change <2

### Configuration

```yaml
pain:
  escalation:
    threshold: 2           # VAS increase to trigger escalation
    analysis-window-hours: 24  # Hours for trend analysis
    critical-vas-level: 7      # VAS considered critical
```

---

## Kafka Integration

### Produces

**Topics:**
1. `pain.escalated` - Pain escalation detected
2. `dose.administered` - Medication administered
3. `vas.recorded` - VAS level recorded

**Event Schemas:**

### PainEscalatedEvent
```json
{
  "eventId": "uuid",
  "mrn": "MRN-123",
  "patientId": 456,
  "patientName": "John Doe",
  "previousVas": 5,
  "currentVas": 8,
  "vasChange": 3,
  "priority": "HIGH",
  "escalationId": 789,
  "timestamp": "2026-01-09T14:00:00",
  "triggeredBy": "nurse123"
}
```

**Consumers:** Notification Service, Business Analytics Service

### DoseAdministeredEvent
```json
{
  "eventId": "uuid",
  "mrn": "MRN-123",
  "patientId": 456,
  "drugName": "Morphine",
  "dosage": 10.0,
  "unit": "mg",
  "route": "IV",
  "administeredBy": "nurse123",
  "administeredAt": "2026-01-09T14:05:00",
  "timestamp": "2026-01-09T14:05:00"
}
```

---

## REST API

### Pain Trend Analysis

```http
GET /api/pain-escalation/patients/{mrn}/trend
```

**Response:**
```json
{
  "mrn": "MRN-123",
  "patientId": 456,
  "currentVas": 8,
  "previousVas": 5,
  "trend": "INCREASING",
  "vasHistory": [...],
  "escalationCount": 1,
  "analyzedAt": "2026-01-09T16:00:00"
}
```

**Trend Values:** `INCREASING`, `DECREASING`, `STABLE`, `INSUFFICIENT_DATA`

### Record VAS

```http
POST /api/pain-escalation/patients/{mrn}/vas?patientId=123
Content-Type: application/json

{
  "vasLevel": 8,
  "location": "Lower back",
  "recordedBy": "nurse123"
}
```

**Side Effect:** Auto-escalation if VAS increases by ≥2

### Record Dose Administration

```http
POST /api/pain-escalation/patients/{mrn}/dose?patientId=123
Content-Type: application/json

{
  "drugName": "Morphine",
  "dosage": 10.0,
  "unit": "mg",
  "route": "IV",
  "administeredBy": "nurse123",
  "notes": "Patient complaining of severe pain"
}
```

### Escalation Management

```http
GET    /api/pain-escalation/escalations                           # All open
GET    /api/pain-escalation/patients/{mrn}/escalations            # By patient
PUT    /api/pain-escalation/escalations/{id}/resolve              # Resolve
PUT    /api/pain-escalation/escalations/{id}/status               # Update status
```

---

## Workflow Example

### Scenario: Nurse Records Increasing Pain

**Step 1:** Patient reports pain increase
```
Previous VAS: 5 (recorded 2 hours ago)
Current VAS: 8 (just now)
```

**Step 2:** Nurse records new VAS
```http
POST /api/pain-escalation/patients/MRN-123/vas?patientId=456
{"vasLevel": 8, "location": "Lower back", "recordedBy": "nurse123"}
```

**Step 3:** Service detects escalation
```
VAS change: +3 (exceeds threshold of 2)
Priority: HIGH (currentVas=8, change=3)
Creates PainEscalation with status=OPEN
```

**Step 4:** Kafka event published
```
Topic: pain.escalated
Event: PainEscalatedEvent with priority=HIGH
```

**Step 5:** Notification Service acts
```
Sends notification to anesthesiologist
Email + WebSocket alert
```

**Step 6:** Anesthesiologist responds
```
Reviews patient
Adjusts treatment
Resolves escalation
```

```http
PUT /api/pain-escalation/escalations/789/resolve?resolvedBy=doctor123&notes=Increased morphine dose, pain controlled
```

---

## Integration with Monolith

### From Nurse Module

**After recording VAS:**
```java
@Autowired
private RestTemplate restTemplate;

public void recordPatientVas(String mrn, Long patientId, int vasLevel) {
    VasRecordRequest request = VasRecordRequest.builder()
        .vasLevel(vasLevel)
        .location("Lower back")
        .recordedBy(SecurityUtils.getCurrentUserId())
        .build();
    
    String url = painEscalationServiceUrl + "/patients/" + mrn + "/vas?patientId=" + patientId;
    restTemplate.postForEntity(url, request, VasRecord.class);
}
```

**After administering dose:**
```java
public void recordDoseAdministration(String mrn, Long patientId, String drug, double dosage) {
    DoseAdministrationRequest request = DoseAdministrationRequest.builder()
        .drugName(drug)
        .dosage(dosage)
        .unit("mg")
        .route("IV")
        .administeredBy(SecurityUtils.getCurrentUserId())
        .build();
    
    String url = painEscalationServiceUrl + "/patients/" + mrn + "/dose?patientId=" + patientId;
    restTemplate.postForEntity(url, request, DoseAdministration.class);
}
```

### From Doctor Module

**View pain trend:**
```java
public PainTrendAnalysisDTO getPatientPainTrend(String mrn) {
    String url = painEscalationServiceUrl + "/patients/" + mrn + "/trend";
    return restTemplate.getForObject(url, PainTrendAnalysisDTO.class);
}
```

### From Anesthesiologist Module

**Get urgent escalations:**
```java
public List<PainEscalation> getUrgentEscalations() {
    String url = painEscalationServiceUrl + "/escalations";
    return restTemplate.getForObject(url, List.class);
}
```

---

## Running the Service

### Prerequisites

```bash
# PostgreSQL
createdb pain_escalation_db

# Kafka
docker-compose up -d kafka
```

### Local Development

```bash
cd C:\backend_projects\microservices\pain-escalation-service
mvn spring-boot:run
```

### Environment Variables

```bash
DB_URL=jdbc:postgresql://localhost:5432/pain_escalation_db
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
PAIN_ESCALATION_THRESHOLD=2
PAIN_ANALYSIS_WINDOW=24
PAIN_CRITICAL_VAS=7
SERVER_PORT=8088
```

---

## Testing

### Manual Test Flow

```bash
# 1. Record first VAS (baseline)
curl -X POST http://localhost:8088/api/pain-escalation/patients/MRN-123/vas?patientId=1 \
  -H "Content-Type: application/json" \
  -d '{"vasLevel":5,"location":"Back","recordedBy":"nurse1"}'

# 2. Record second VAS (escalation - increase by 3)
curl -X POST http://localhost:8088/api/pain-escalation/patients/MRN-123/vas?patientId=1 \
  -H "Content-Type: application/json" \
  -d '{"vasLevel":8,"location":"Back","recordedBy":"nurse1"}'

# 3. Check for escalations (should see HIGH priority)
curl http://localhost:8088/api/pain-escalation/escalations

# 4. Record dose
curl -X POST http://localhost:8088/api/pain-escalation/patients/MRN-123/dose?patientId=1 \
  -H "Content-Type: application/json" \
  -d '{"drugName":"Morphine","dosage":10,"unit":"mg","route":"IV","administeredBy":"nurse1"}'

# 5. Get pain trend
curl http://localhost:8088/api/pain-escalation/patients/MRN-123/trend
```

---

## Monitoring

### Key Metrics

- VAS records per hour
- Escalations created per day
- Average time to resolve escalations
- Critical vs high priority ratio

### Health Check

```http
GET /api/pain-escalation/health
```

---

## Files Created

**Total:** 26 files, 1,862 lines

**Key Components:**
- `PainEscalationServiceApplication.java` - Main application
- `PainEscalationServiceImpl.java` - Core logic with auto-escalation
- `EscalationEventProducer.java` - Kafka publisher
- `PainEscalationController.java` - REST API

**Entities:**
- `PainEscalation.java` - Escalation records
- `VasRecord.java` - VAS history
- `DoseAdministration.java` - Medication log

**Kafka Events:**
- `PainEscalatedEvent.java`
- `DoseAdministeredEvent.java`
- `VasRecordedEvent.java`

---

## Future Enhancements

- [ ] Predictive pain escalation using ML
- [ ] Integration with treatment protocols
- [ ] Pain medication effectiveness tracking
- [ ] Patient-reported outcomes (PRO)
- [ ] Real-time dashboard

---

**Created:** 2026-01-09  
**Committed:** ✅  
**Ready for Integration:** ✅
