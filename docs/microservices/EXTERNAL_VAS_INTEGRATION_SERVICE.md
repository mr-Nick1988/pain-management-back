# External VAS Integration Service Documentation

**Version:** 1.0.0  
**Port:** 8089  
**Status:** ✅ Implemented  
**Location:** `C:\backend_projects\microservices\external-vas-integration-service\`

---

## Overview

Microservice for integrating external VAS (Visual Analog Scale) devices and systems with the Pain Management Platform.

### Key Features

- ✅ API key management and authentication
- ✅ Multi-format data parsing (JSON, XML, CSV)
- ✅ Secure API key generation with IP whitelisting
- ✅ Rate limiting and usage tracking
- ✅ Batch VAS imports
- ✅ Kafka event publishing
- ✅ Auto-recommendation triggering

---

## Architecture

### Tech Stack

- **Java 21**
- **Spring Boot 3.5.5**
- **PostgreSQL** (API keys, VAS records)
- **Apache Kafka** (event streaming)
- **Liquibase** (migrations)
- **Jackson** (JSON/XML parsing)

### Database Schema

**Tables:**
1. `api_keys` - Secure API keys with IP whitelist and rate limits
2. `external_vas_records` - Complete history of external VAS submissions

---

## API Key Management

### Security Features

- **64-character random API keys**
- **IP whitelisting** per key (comma-separated or `*`)
- **Rate limiting** (requests per minute)
- **Expiration management**
- **Usage tracking** and audit trail

### Create API Key

```http
POST /api/admin/api-keys?createdBy=admin
Content-Type: application/json

{
  "systemName": "VAS Monitor Ward A",
  "description": "Ward A pain monitoring devices",
  "expiresAt": "2027-01-09T00:00:00",
  "ipWhitelist": "192.168.1.100,192.168.1.101",
  "rateLimitPerMinute": 100
}
```

**Returns:** Complete API key details including generated key

### Manage API Keys

```http
GET    /api/admin/api-keys              # All keys
GET    /api/admin/api-keys/active        # Active only
PUT    /api/admin/api-keys/{key}/deactivate
DELETE /api/admin/api-keys/{key}
```

---

## VAS Data Integration

### Supported Formats

1. **JSON** (`application/json`)
2. **XML** (`application/xml`)
3. **CSV** (`text/csv`) - batch imports

### Record VAS (JSON)

```http
POST /api/external/vas/record
Headers:
  X-API-Key: <your-api-key>
  Content-Type: application/json

Body:
{
  "patientMrn": "MRN-12345",
  "vasLevel": 8,
  "deviceId": "MONITOR-001",
  "location": "Ward A, Bed 12",
  "painPlace": "Lower back",
  "timestamp": "2026-01-09T14:00:00",
  "notes": "Patient complaining of severe pain",
  "source": "VAS_MONITOR"
}

Response:
{
  "status": "success",
  "vasId": 123,
  "patientMrn": "MRN-12345",
  "vasLevel": 8,
  "format": "JSON"
}
```

### Record VAS (XML)

```http
POST /api/external/vas/record
Headers:
  X-API-Key: <your-api-key>
  Content-Type: application/xml

Body:
<externalVasRequest>
  <patientMrn>MRN-12345</patientMrn>
  <vasLevel>8</vasLevel>
  <deviceId>MONITOR-001</deviceId>
  <location>Ward A</location>
</externalVasRequest>
```

### Batch Import (CSV)

```http
POST /api/external/vas/batch
Headers:
  X-API-Key: <your-api-key>
  Content-Type: text/csv

Body (CSV):
MRN,VASLevel,DeviceID,Location,PainPlace,Timestamp,Notes
MRN-12345,8,MONITOR-001,Ward A,Lower back,2026-01-09T14:00:00,Severe pain
MRN-67890,5,MONITOR-002,ICU-1,Chest,2026-01-09T14:05:00,Moderate pain

Response:
{
  "status": "success",
  "total": 2,
  "success": 2,
  "failed": 0,
  "createdVasIds": [123, 124],
  "errors": []
}
```

### Query VAS Records

```http
GET /api/external/vas/records?deviceId=MONITOR-001&timeRange=24h&vasLevelMin=7
```

**Filters:**
- `deviceId` - Filter by device
- `location` - Filter by location
- `timeRange` - `1h`, `6h`, `24h`, `7d`
- `vasLevelMin` / `vasLevelMax` - VAS range

### Statistics

```http
GET /api/external/vas/stats

Response:
{
  "totalRecordsToday": 250,
  "averageVas": 5.8,
  "highPainAlerts": 45,
  "activeDevices": 12
}
```

---

## Kafka Integration

### Produces

**Topic:** `vas.external.recorded`

**Event Schema:**
```json
{
  "eventId": "uuid",
  "vasRecordId": 123,
  "mrn": "MRN-12345",
  "patientId": 456,
  "vasLevel": 8,
  "deviceId": "MONITOR-001",
  "location": "Ward A",
  "painPlace": "Lower back",
  "source": "VAS_MONITOR",
  "format": "JSON",
  "recordedAt": "2026-01-09T14:00:00",
  "timestamp": "2026-01-09T14:00:05",
  "autoRecommendationTriggered": true
}
```

**Consumers:**
- Pain Escalation Service (triggers VAS recording)
- Business Analytics Service (device tracking)

---

## Auto-Recommendation Logic

When VAS level ≥ 4 (configurable):
- Kafka event flag `autoRecommendationTriggered` set to `true`
- Downstream services can trigger recommendation workflow
- Configurable threshold via `VAS_AUTO_RECOMMENDATION` env var

---

## Configuration

```yaml
vas:
  integration:
    auto-recommendation-threshold: 4  # VAS to trigger recommendation
    high-pain-threshold: 7            # VAS considered high pain
    batch-size-limit: 1000            # Max records per batch

api:
  key:
    length: 64                        # API key length
    default-rate-limit: 100           # Default requests/min
```

---

## Integration Examples

### From Monolith

**When receiving VAS from external device:**

```java
@Autowired
private RestTemplate restTemplate;

public void forwardExternalVas(ExternalVasDTO vasData) {
    String url = externalVasServiceUrl + "/record";
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-API-Key", monolithApiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    HttpEntity<ExternalVasDTO> request = new HttpEntity<>(vasData, headers);
    
    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
    
    log.info("VAS forwarded to external service: {}", response.getBody());
}
```

### JavaScript Client (VAS Device)

```javascript
const apiKey = 'your-api-key-here';

async function sendVasReading(mrn, vasLevel, deviceId) {
  const response = await fetch('http://localhost:8089/api/external/vas/record', {
    method: 'POST',
    headers: {
      'X-API-Key': apiKey,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      patientMrn: mrn,
      vasLevel: vasLevel,
      deviceId: deviceId,
      location: 'Ward A',
      timestamp: new Date().toISOString(),
      source: 'VAS_DEVICE'
    })
  });
  
  return response.json();
}
```

### Python Client (EMR System)

```python
import requests

api_key = 'your-api-key-here'
headers = {'X-API-Key': api_key, 'Content-Type': 'application/json'}

def sync_vas_from_emr(mrn, vas_level):
    data = {
        'patientMrn': mrn,
        'vasLevel': vas_level,
        'deviceId': 'EMR_SYSTEM',
        'source': 'EMR_IMPORT'
    }
    
    response = requests.post(
        'http://localhost:8089/api/external/vas/record',
        headers=headers,
        json=data
    )
    
    return response.json()
```

---

## Running the Service

### Prerequisites

```bash
# PostgreSQL
createdb external_vas_db

# Kafka
docker-compose up -d kafka
```

### Local Development

```bash
cd C:\backend_projects\microservices\external-vas-integration-service
mvn spring-boot:run
```

### Environment Variables

```bash
DB_URL=jdbc:postgresql://localhost:5432/external_vas_db
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
VAS_AUTO_RECOMMENDATION=4
VAS_HIGH_PAIN=7
API_KEY_LENGTH=64
SERVER_PORT=8089
```

---

## Security Best Practices

### API Key Management

1. **Never commit API keys** to version control
2. **Rotate keys periodically** (set expiration dates)
3. **Use IP whitelisting** in production
4. **Monitor usage patterns** for suspicious activity

### Network Security

1. **Always use HTTPS** in production
2. **Configure TLS/SSL** certificates
3. **Enable HSTS** headers
4. **Use reverse proxy** (nginx) for rate limiting

### Authentication

1. **Validate API key** on every request
2. **Check IP whitelist** if configured
3. **Enforce rate limits** per key
4. **Log all authentication failures**

---

## Monitoring

### Key Metrics

- API requests per second per key
- Failed authentication attempts
- Average VAS level from devices
- Batch import success rate
- High pain alerts (VAS ≥7)
- Active devices count

### Health Check

```http
GET /api/external/vas/health
```

---

## Troubleshooting

### Invalid API Key (401)

**Causes:**
- API key is incorrect
- Key is deactivated
- Key has expired
- IP not whitelisted

**Solutions:**
1. Verify key: `GET /api/admin/api-keys`
2. Check expiration date
3. Add IP to whitelist
4. Generate new key if needed

### Parse Error (400)

**Causes:**
- Content-Type header mismatch
- Invalid JSON/XML/CSV format
- VAS level out of range (0-10)

**Solutions:**
1. Verify Content-Type header matches data
2. Validate JSON/XML structure
3. Check CSV format (MRN,VASLevel minimum)
4. Ensure VAS is integer 0-10

---

## Files Created

**Total:** 28 files, 2,126 lines

**Key Components:**
- `ExternalVasIntegrationServiceApplication.java` - Main app
- `ExternalVasIntegrationService.java` - Core logic
- `ApiKeyService.java` - Key management
- `VasParserFactory.java` - Multi-format parsing

**Parsers:**
- `JsonVasParser.java` - JSON support
- `XmlVasParser.java` - XML support
- `CsvVasParser.java` - CSV batch support

**Controllers:**
- `ExternalVasController.java` - VAS recording API
- `ApiKeyController.java` - Key management API

**Entities:**
- `ApiKey.java` - API key with security features
- `ExternalVasRecord.java` - VAS record history

**Kafka:**
- `VasExternalRecordedEvent.java` - Event DTO
- `VasExternalEventProducer.java` - Publisher

---

## Future Enhancements

- [ ] HL7 v2 parser for hospital systems
- [ ] FHIR R4 parser for modern healthcare
- [ ] Real-time device status monitoring
- [ ] WebSocket for live alerts
- [ ] OAuth2 authentication support
- [ ] Advanced rate limiting with Redis

---

**Created:** 2026-01-09  
**Committed:** ✅  
**Ready for Integration:** ✅
