# 🧪 Complete Testing Guide - Pain Management Platform

**Version:** 3.0  
**Last Updated:** January 12, 2026  
**Scope:** Monolith + All Microservices

---

## 📋 Table of Contents

1. [Testing Strategy Overview](#testing-strategy-overview)
2. [Test Environment Setup](#test-environment-setup)
3. [Infrastructure Testing](#infrastructure-testing)
4. [Microservices Testing](#microservices-testing)
5. [Integration Testing](#integration-testing)
6. [End-to-End Testing](#end-to-end-testing)
7. [Performance Testing](#performance-testing)
8. [API Testing](#api-testing)
9. [Kafka Event Testing](#kafka-event-testing)
10. [Database Testing](#database-testing)
11. [Automated Test Suites](#automated-test-suites)
12. [Test Data Management](#test-data-management)
13. [Continuous Testing](#continuous-testing)

---

## Testing Strategy Overview

### Test Pyramid

```
         /\
        /  \  E2E Tests (10%)
       /    \
      /------\
     / Integ. \ Integration Tests (30%)
    /  Tests  \
   /------------\
  /   Unit Tests \ Unit Tests (60%)
 /________________\
```

### Testing Levels

| Level | Scope | Tools | Frequency |
|-------|-------|-------|-----------|
| Unit | Individual methods/classes | JUnit, Mockito | Every commit |
| Integration | Service + Database/Kafka | Spring Boot Test | Daily |
| Contract | API contracts | Spring Cloud Contract | On API changes |
| E2E | Full user workflows | REST Assured, Postman | Before release |
| Performance | Load, stress, scalability | JMeter, Gatling | Weekly |
| Security | Vulnerabilities, auth | OWASP ZAP | Weekly |

---

## Test Environment Setup

### Prerequisites

```bash
# Required tools
java -version          # Java 21
mvn -version          # Maven 3.9+
docker --version      # Docker 20.10+
curl --version        # cURL (for API testing)
jq --version          # jq (JSON processing)
```

### Environment Preparation

#### 1. Start Test Infrastructure

```bash
# Navigate to project
cd C:\backend_projects\pain_managment_back

# Start infrastructure only
docker-compose up -d kafka postgres

# Verify infrastructure
docker-compose ps
docker exec dev_kafka kafka-broker-api-versions --bootstrap-server localhost:9092
docker exec dev_postgres pg_isready -U postgres
```

#### 2. Create Test Database

```bash
# Create test databases
docker exec dev_postgres psql -U postgres -c "CREATE DATABASE pain_management_test;"
docker exec dev_postgres psql -U postgres -c "CREATE DATABASE auth_db_test;"
docker exec dev_postgres psql -U postgres -c "CREATE DATABASE emr_integration_test;"
docker exec dev_postgres psql -U postgres -c "CREATE DATABASE notification_test;"
docker exec dev_postgres psql -U postgres -c "CREATE DATABASE pain_escalation_test;"
docker exec dev_postgres psql -U postgres -c "CREATE DATABASE external_vas_test;"
docker exec dev_postgres psql -U postgres -c "CREATE DATABASE analytics_reporting_test;"
```

#### 3. Configure Test Properties

Create `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pain_management_test
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreate schema for each test
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: test-consumer-group
      auto-offset-reset: earliest

# Disable actuator for tests
management:
  endpoints:
    enabled-by-default: false

# Test-specific settings
test:
  cleanup: true
  mock-external-services: true
```

---

## Infrastructure Testing

### Kafka Testing

#### Test 1: Broker Availability

```bash
#!/bin/bash
# test-kafka.sh

echo "=== Testing Kafka Broker ==="

# Check broker is running
if docker exec dev_kafka kafka-broker-api-versions --bootstrap-server localhost:9092 &>/dev/null; then
    echo "✅ Kafka broker is UP"
else
    echo "❌ Kafka broker is DOWN"
    exit 1
fi

# List topics
echo -e "\n=== Available Topics ==="
docker exec dev_kafka kafka-topics --list --bootstrap-server localhost:9092

# Check cluster ID
echo -e "\n=== Cluster Info ==="
docker exec dev_kafka kafka-cluster --cluster-id MkU3OEVBNTcwNTJENDM2Qk --bootstrap-server localhost:9092 describe

echo -e "\n✅ Kafka is healthy"
```

#### Test 2: Topic Operations

```bash
#!/bin/bash
# test-kafka-topics.sh

TOPIC_NAME="test-topic-$(date +%s)"

echo "Creating test topic: $TOPIC_NAME"
docker exec dev_kafka kafka-topics \
  --create \
  --topic $TOPIC_NAME \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

echo "Producing test message..."
echo "test message" | docker exec -i dev_kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic $TOPIC_NAME

echo "Consuming test message..."
timeout 5 docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic $TOPIC_NAME \
  --from-beginning \
  --max-messages 1

echo "Deleting test topic..."
docker exec dev_kafka kafka-topics \
  --delete \
  --topic $TOPIC_NAME \
  --bootstrap-server localhost:9092

echo "✅ Kafka topic operations successful"
```

#### Test 3: Consumer Group Testing

```bash
#!/bin/bash
# test-kafka-consumer-groups.sh

GROUP_NAME="test-group-$(date +%s)"
TOPIC="analytics-events"

echo "Creating consumer in group: $GROUP_NAME"
timeout 10 docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic $TOPIC \
  --group $GROUP_NAME \
  --from-beginning &

sleep 3

echo "Checking consumer group..."
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group $GROUP_NAME

echo "✅ Consumer group test successful"
```

---

### PostgreSQL Testing

#### Test 1: Database Connectivity

```bash
#!/bin/bash
# test-postgres.sh

echo "=== Testing PostgreSQL ==="

# Check if running
if docker exec dev_postgres pg_isready -U postgres &>/dev/null; then
    echo "✅ PostgreSQL is UP"
else
    echo "❌ PostgreSQL is DOWN"
    exit 1
fi

# Check version
echo -e "\n=== PostgreSQL Version ==="
docker exec dev_postgres psql -U postgres -c "SELECT version();"

# List databases
echo -e "\n=== Available Databases ==="
docker exec dev_postgres psql -U postgres -c "\l"

# Test connection to main database
echo -e "\n=== Testing Connection to pain_management_db ==="
docker exec dev_postgres psql -U postgres -d pain_management_db -c "SELECT 1;"

echo -e "\n✅ PostgreSQL is healthy"
```

#### Test 2: Performance Test

```bash
#!/bin/bash
# test-postgres-performance.sh

echo "=== PostgreSQL Performance Test ==="

# Create test table
docker exec dev_postgres psql -U postgres -d pain_management_db <<EOF
CREATE TABLE IF NOT EXISTS test_performance (
  id SERIAL PRIMARY KEY,
  data TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);
EOF

# Insert 10,000 rows
echo "Inserting 10,000 rows..."
time docker exec dev_postgres psql -U postgres -d pain_management_db <<EOF
INSERT INTO test_performance (data)
SELECT 'test data ' || generate_series
FROM generate_series(1, 10000);
EOF

# Query performance
echo -e "\nQuery performance test..."
time docker exec dev_postgres psql -U postgres -d pain_management_db -c \
  "SELECT COUNT(*) FROM test_performance WHERE data LIKE 'test%';"

# Cleanup
docker exec dev_postgres psql -U postgres -d pain_management_db -c \
  "DROP TABLE test_performance;"

echo "✅ Performance test completed"
```

---

## Microservices Testing

### Authentication Service Tests

#### Unit Test Example

Create `AuthenticationServiceTest.java`:

```java
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthenticationServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should authenticate user with valid credentials")
    void testValidLogin() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.personId").value("admin"));
    }

    @Test
    @DisplayName("Should reject invalid credentials")
    void testInvalidLogin() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrong");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate JWT token")
    void testTokenValidation() throws Exception {
        String token = generateTestToken("admin");
        
        mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }
}
```

#### Integration Test

```bash
#!/bin/bash
# test-auth-service.sh

echo "=== Testing Authentication Service ==="

# Start service
docker-compose --profile auth up -d auth-service

# Wait for service to be ready
echo "Waiting for service..."
for i in {1..30}; do
    if curl -f http://localhost:8082/actuator/health &>/dev/null; then
        echo "✅ Service is ready"
        break
    fi
    sleep 2
done

# Test 1: Health check
echo -e "\n=== Test 1: Health Check ==="
curl -f http://localhost:8082/actuator/health | jq '.'

# Test 2: Login (should fail - no user)
echo -e "\n=== Test 2: Login (Invalid) ==="
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123"}' \
  -w "\nHTTP Status: %{http_code}\n"

# Test 3: Register user (via monolith admin API)
echo -e "\n=== Test 3: Register User ==="
# This requires monolith to be running

# Test 4: Validate token format
echo -e "\n=== Test 4: Token Validation ==="
INVALID_TOKEN="invalid.jwt.token"
curl -X POST http://localhost:8082/api/auth/validate \
  -H "Authorization: Bearer $INVALID_TOKEN" \
  -w "\nHTTP Status: %{http_code}\n"

echo -e "\n✅ Authentication Service tests completed"
```

---

### EMR Integration Service Tests

```bash
#!/bin/bash
# test-emr-service.sh

echo "=== Testing EMR Integration Service ==="

# Start service
docker-compose --profile emr up -d emr-service

# Wait for ready
for i in {1..30}; do
    if curl -f http://localhost:8086/actuator/health &>/dev/null; then
        break
    fi
    sleep 2
done

# Test 1: Health check
echo -e "\n=== Test 1: Health Check ==="
curl -f http://localhost:8086/actuator/health | jq '.'

# Test 2: FHIR connection test
echo -e "\n=== Test 2: FHIR Server Connectivity ==="
curl -X GET http://localhost:8086/api/emr/fhir/health \
  -w "\nHTTP Status: %{http_code}\n"

# Test 3: Sync patient (requires auth token)
echo -e "\n=== Test 3: Patient Sync ==="
# TOKEN="your-jwt-token"
# curl -X POST http://localhost:8086/api/emr/sync/patient/example \
#   -H "Authorization: Bearer $TOKEN" \
#   -w "\nHTTP Status: %{http_code}\n"

echo -e "\n✅ EMR Integration Service tests completed"
```

---

### Notification Service Tests

```bash
#!/bin/bash
# test-notification-service.sh

echo "=== Testing Notification Service ==="

# Start service
docker-compose --profile notification up -d notification-service

# Wait for ready
for i in {1..30}; do
    if curl -f http://localhost:8087/actuator/health &>/dev/null; then
        break
    fi
    sleep 2
done

# Test 1: Health check
echo -e "\n=== Test 1: Health Check ==="
curl -f http://localhost:8087/actuator/health | jq '.'

# Test 2: Send test notification
echo -e "\n=== Test 2: Send Email Notification ==="
curl -X POST http://localhost:8087/api/notifications/email \
  -H "Content-Type: application/json" \
  -d '{
    "to": "test@example.com",
    "subject": "Test Notification",
    "body": "This is a test",
    "priority": "NORMAL"
  }' \
  -w "\nHTTP Status: %{http_code}\n"

# Test 3: Check notification history
echo -e "\n=== Test 3: Notification History ==="
curl -X GET http://localhost:8087/api/notifications/history?limit=10 \
  -w "\nHTTP Status: %{http_code}\n" | jq '.'

echo -e "\n✅ Notification Service tests completed"
```

---

### Pain Escalation Service Tests

```bash
#!/bin/bash
# test-pain-escalation-service.sh

echo "=== Testing Pain Escalation Service ==="

# Start service
docker-compose --profile escalation up -d pain-escalation-service

# Wait for ready
for i in {1..30}; do
    if curl -f http://localhost:8088/actuator/health &>/dev/null; then
        break
    fi
    sleep 2
done

# Test 1: Health check
echo -e "\n=== Test 1: Health Check ==="
curl -f http://localhost:8088/actuator/health | jq '.'

# Test 2: Get all escalations
echo -e "\n=== Test 2: Get All Escalations ==="
curl -X GET http://localhost:8088/api/pain-escalation/escalations \
  -w "\nHTTP Status: %{http_code}\n" | jq '.'

# Test 3: Get escalations by status
echo -e "\n=== Test 3: Get Active Escalations ==="
curl -X GET http://localhost:8088/api/pain-escalation/escalations/status/ACTIVE \
  -w "\nHTTP Status: %{http_code}\n" | jq '.'

# Test 4: Get escalations by priority
echo -e "\n=== Test 4: Get Critical Escalations ==="
curl -X GET http://localhost:8088/api/pain-escalation/escalations/priority/CRITICAL \
  -w "\nHTTP Status: %{http_code}\n" | jq '.'

echo -e "\n✅ Pain Escalation Service tests completed"
```

---

### External VAS Service Tests

```bash
#!/bin/bash
# test-external-vas-service.sh

echo "=== Testing External VAS Service ==="

# Start service
docker-compose --profile vas up -d external-vas-service

# Wait for ready
for i in {1..30}; do
    if curl -f http://localhost:8089/api/external/vas/health &>/dev/null; then
        break
    fi
    sleep 2
done

# Test 1: Health check
echo -e "\n=== Test 1: Health Check ==="
curl -f http://localhost:8089/api/external/vas/health | jq '.'

# Test 2: Create API key
echo -e "\n=== Test 2: Create API Key ==="
API_KEY_RESPONSE=$(curl -X POST "http://localhost:8089/api/admin/api-keys?createdBy=test" \
  -H "Content-Type: application/json" \
  -d '{
    "systemName": "Test System",
    "description": "Testing API key",
    "ipWhitelist": "*",
    "rateLimitPerMinute": 100
  }' -s)

echo "$API_KEY_RESPONSE" | jq '.'
API_KEY=$(echo "$API_KEY_RESPONSE" | jq -r '.apiKey')

# Test 3: Record VAS with API key
echo -e "\n=== Test 3: Record VAS ==="
curl -X POST http://localhost:8089/api/external/vas/record \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "TEST-001",
    "vasLevel": 5,
    "deviceId": "DEV-TEST-001",
    "location": "Test Ward",
    "painPlace": "Test Location",
    "source": "VAS_MONITOR"
  }' \
  -w "\nHTTP Status: %{http_code}\n" | jq '.'

# Test 4: Batch import CSV
echo -e "\n=== Test 4: Batch Import CSV ==="
curl -X POST http://localhost:8089/api/external/vas/batch \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: text/csv" \
  -d 'MRN,VASLevel,DeviceID,Location
TEST-001,3,DEV-001,Ward A
TEST-002,7,DEV-002,ICU' \
  -w "\nHTTP Status: %{http_code}\n" | jq '.'

# Test 5: Invalid API key
echo -e "\n=== Test 5: Invalid API Key ==="
curl -X POST http://localhost:8089/api/external/vas/record \
  -H "X-API-Key: invalid-key" \
  -H "Content-Type: application/json" \
  -d '{"patientMrn":"TEST-001","vasLevel":5}' \
  -w "\nHTTP Status: %{http_code}\n"

echo -e "\n✅ External VAS Service tests completed"
```

---

### Reporting Service Tests

```bash
#!/bin/bash
# test-reporting-service.sh

echo "=== Testing Reporting Service ==="

# Start service (requires analytics database)
docker-compose --profile reporting up -d reporting-service

# Wait for ready
for i in {1..30}; do
    if curl -f http://localhost:8091/actuator/health &>/dev/null; then
        break
    fi
    sleep 2
done

# Test 1: Health check
echo -e "\n=== Test 1: Health Check ==="
curl -f http://localhost:8091/actuator/health | jq '.'

# Test 2: Get available reports
echo -e "\n=== Test 2: Available Reports ==="
curl -X GET http://localhost:8091/api/reports/available \
  -w "\nHTTP Status: %{http_code}\n" | jq '.'

# Test 3: Generate sample report
echo -e "\n=== Test 3: Generate Report ==="
curl -X POST http://localhost:8091/api/reports/generate \
  -H "Content-Type: application/json" \
  -d '{
    "reportType": "PAIN_SUMMARY",
    "dateFrom": "2026-01-01",
    "dateTo": "2026-01-12"
  }' \
  -w "\nHTTP Status: %{http_code}\n" | jq '.'

echo -e "\n✅ Reporting Service tests completed"
```

---

## Integration Testing

### Full System Integration Test

```bash
#!/bin/bash
# integration-test-full.sh

echo "============================================"
echo "   FULL SYSTEM INTEGRATION TEST"
echo "============================================"

# Start all services
echo -e "\n=== Starting All Services ==="
docker-compose --profile all up -d

# Wait for all services
echo -e "\n=== Waiting for Services (60s) ==="
sleep 60

# Start monolith
echo -e "\n=== Starting Monolith ==="
cd C:\backend_projects\pain_managment_back
mvn spring-boot:run -Dspring-boot.run.profiles=local &
MONOLITH_PID=$!

sleep 30

# Test workflow
echo -e "\n=== Test Workflow: Patient -> VAS -> Escalation -> Notification ==="

# Step 1: Register user (Admin)
echo -e "\n--- Step 1: Register Admin User ---"
curl -X POST http://localhost:8080/api/admin/persons \
  -H "Content-Type: application/json" \
  -d '{
    "personId": "test_admin",
    "firstName": "Test",
    "lastName": "Admin",
    "login": "testadmin",
    "password": "test123",
    "roles": ["ADMIN", "DOCTOR", "NURSE"]
  }' | jq '.'

# Step 2: Login
echo -e "\n--- Step 2: Login ---"
TOKEN=$(curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"testadmin","password":"test123"}' -s | jq -r '.token')

echo "Token: $TOKEN"

# Step 3: Create patient
echo -e "\n--- Step 3: Create Patient ---"
PATIENT_MRN=$(curl -X POST http://localhost:8080/api/nurse/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "phoneNumber": "+1234567890"
  }' -s | jq -r '.mrn')

echo "Patient MRN: $PATIENT_MRN"

# Step 4: Record low VAS
echo -e "\n--- Step 4: Record Low VAS (3) ---"
curl -X POST http://localhost:8080/api/nurse/patients/$PATIENT_MRN/vas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "painLevel": 3,
    "location": "Test Ward",
    "notes": "Low pain"
  }' | jq '.'

sleep 2

# Step 5: Record high VAS (trigger escalation)
echo -e "\n--- Step 5: Record High VAS (8) - Should Trigger Escalation ---"
curl -X POST http://localhost:8080/api/nurse/patients/$PATIENT_MRN/vas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "painLevel": 8,
    "location": "Test Ward",
    "notes": "HIGH PAIN - ESCALATION TEST"
  }' | jq '.'

sleep 5

# Step 6: Check for escalations
echo -e "\n--- Step 6: Check Escalations ---"
curl -X GET http://localhost:8088/api/pain-escalation/escalations \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | select(.patientMrn=="'$PATIENT_MRN'")'

# Step 7: Check Kafka topic for events
echo -e "\n--- Step 7: Check Kafka Events ---"
timeout 5 docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic pain.escalated \
  --from-beginning \
  --max-messages 5

# Cleanup
echo -e "\n=== Cleanup ==="
kill $MONOLITH_PID
docker-compose --profile all down

echo -e "\n✅ Integration test completed"
```

---

## End-to-End Testing

### E2E Test Suite with Postman/Newman

Create `postman-collection.json`:

```json
{
  "info": {
    "name": "Pain Management E2E Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Register User",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/admin/persons",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"personId\": \"e2e_user\",\n  \"firstName\": \"E2E\",\n  \"lastName\": \"Test\",\n  \"login\": \"e2euser\",\n  \"password\": \"e2e123\",\n  \"roles\": [\"DOCTOR\", \"NURSE\"]\n}"
        }
      },
      "test": [
        "pm.test('Status is 201', () => pm.response.to.have.status(201));"
      ]
    },
    {
      "name": "2. Login",
      "request": {
        "method": "POST",
        "url": "{{auth_url}}/api/auth/login",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"login\": \"e2euser\",\n  \"password\": \"e2e123\"\n}"
        }
      },
      "test": [
        "pm.test('Has token', () => {",
        "  pm.response.to.have.jsonBody('token');",
        "  pm.environment.set('token', pm.response.json().token);",
        "});"
      ]
    },
    {
      "name": "3. Create Patient",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/api/nurse/patients",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"firstName\": \"Test\",\n  \"lastName\": \"Patient\",\n  \"dateOfBirth\": \"1990-01-01\",\n  \"gender\": \"MALE\"\n}"
        }
      },
      "test": [
        "pm.test('Patient created', () => {",
        "  pm.response.to.have.status(201);",
        "  pm.environment.set('patient_mrn', pm.response.json().mrn);",
        "});"
      ]
    }
  ]
}
```

Run with Newman:

```bash
# Install newman
npm install -g newman

# Run collection
newman run postman-collection.json \
  --environment postman-environment.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

---

## Performance Testing

### JMeter Test Plan

Create `jmeter-test-plan.jmx` for load testing:

```bash
# Run JMeter test
jmeter -n -t jmeter-test-plan.jmx \
  -l results.jtl \
  -e -o reports/

# Test scenarios:
# 1. 100 concurrent users
# 2. 1000 requests per minute
# 3. Duration: 10 minutes
# 4. Endpoints: login, create patient, record VAS
```

### Simple Load Test Script

```bash
#!/bin/bash
# load-test-simple.sh

CONCURRENT=50
REQUESTS=1000
URL="http://localhost:8080/actuator/health"

echo "=== Simple Load Test ==="
echo "Concurrent: $CONCURRENT"
echo "Requests: $REQUESTS"
echo "URL: $URL"

# Using Apache Bench
ab -n $REQUESTS -c $CONCURRENT $URL

# Or using wrk
# wrk -t12 -c400 -d30s $URL
```

---

## API Testing

### Comprehensive API Test Suite

```bash
#!/bin/bash
# api-test-suite.sh

BASE_URL="http://localhost:8080"
AUTH_URL="http://localhost:8082"

# Color output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

passed=0
failed=0

test_api() {
  local name=$1
  local method=$2
  local url=$3
  local expected_status=$4
  local data=$5
  local headers=$6
  
  echo -n "Testing: $name ... "
  
  status=$(curl -s -o /dev/null -w "%{http_code}" \
    -X $method "$url" \
    -H "Content-Type: application/json" \
    $headers \
    $data)
  
  if [ "$status" -eq "$expected_status" ]; then
    echo -e "${GREEN}PASS${NC} (Status: $status)"
    ((passed++))
  else
    echo -e "${RED}FAIL${NC} (Expected: $expected_status, Got: $status)"
    ((failed++))
  fi
}

echo "=== API Test Suite ==="

# Test infrastructure endpoints
test_api "Monolith Health" "GET" "$BASE_URL/actuator/health" 200
test_api "Auth Service Health" "GET" "$AUTH_URL/actuator/health" 200

# Test authentication
test_api "Login Invalid Credentials" "POST" "$AUTH_URL/api/auth/login" 401 \
  '-d {"login":"invalid","password":"wrong"}'

# Test patient API
# (requires authentication)

echo -e "\n=== Test Results ==="
echo -e "Passed: ${GREEN}$passed${NC}"
echo -e "Failed: ${RED}$failed${NC}"

if [ $failed -eq 0 ]; then
  echo -e "\n${GREEN}✅ All tests passed${NC}"
  exit 0
else
  echo -e "\n${RED}❌ Some tests failed${NC}"
  exit 1
fi
```

---

## Kafka Event Testing

### Event Publishing Test

```bash
#!/bin/bash
# test-kafka-events.sh

echo "=== Kafka Event Testing ==="

# Test 1: Publish test event
echo -e "\n--- Test 1: Publish Event ---"
cat <<EOF | docker exec -i dev_kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic analytics-events \
  --property "key.separator=:" \
  --property "parse.key=true"
test-key:{"eventType":"TEST","timestamp":"$(date -Iseconds)","data":"test"}
EOF

# Test 2: Consume event
echo -e "\n--- Test 2: Consume Event ---"
timeout 5 docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic analytics-events \
  --from-beginning \
  --max-messages 1

# Test 3: Check consumer lag
echo -e "\n--- Test 3: Consumer Lag ---"
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --all-groups

echo -e "\n✅ Kafka event tests completed"
```

---

## Database Testing

### Database Schema Validation

```bash
#!/bin/bash
# test-database-schema.sh

echo "=== Database Schema Validation ==="

# Check required tables exist
TABLES=(
  "patients"
  "vas_records"
  "drug_recommendations"
  "escalations"
  "protocols"
  "persons"
)

for table in "${TABLES[@]}"; do
  echo -n "Checking table $table ... "
  
  if docker exec dev_postgres psql -U postgres -d pain_management_db \
    -c "SELECT 1 FROM $table LIMIT 1" &>/dev/null; then
    echo "✅ EXISTS"
  else
    echo "❌ MISSING"
  fi
done

# Check indexes
echo -e "\n--- Database Indexes ---"
docker exec dev_postgres psql -U postgres -d pain_management_db -c \
  "SELECT tablename, indexname FROM pg_indexes WHERE schemaname = 'public';"

echo -e "\n✅ Schema validation completed"
```

---

## Automated Test Suites

### Master Test Runner

```bash
#!/bin/bash
# run-all-tests.sh

echo "============================================"
echo "   PAIN MANAGEMENT PLATFORM - TEST SUITE"
echo "============================================"

START_TIME=$(date +%s)
FAILED_TESTS=()

run_test() {
  local test_name=$1
  local test_script=$2
  
  echo -e "\n=========================================="
  echo "Running: $test_name"
  echo "=========================================="
  
  if bash $test_script; then
    echo "✅ $test_name PASSED"
  else
    echo "❌ $test_name FAILED"
    FAILED_TESTS+=("$test_name")
  fi
}

# Infrastructure tests
run_test "Kafka Tests" "./scripts/tests/test-kafka.sh"
run_test "PostgreSQL Tests" "./scripts/tests/test-postgres.sh"

# Microservice tests
run_test "Auth Service Tests" "./scripts/tests/test-auth-service.sh"
run_test "EMR Service Tests" "./scripts/tests/test-emr-service.sh"
run_test "Notification Service Tests" "./scripts/tests/test-notification-service.sh"
run_test "Pain Escalation Service Tests" "./scripts/tests/test-pain-escalation-service.sh"
run_test "External VAS Service Tests" "./scripts/tests/test-external-vas-service.sh"
run_test "Reporting Service Tests" "./scripts/tests/test-reporting-service.sh"

# Integration tests
run_test "Integration Tests" "./scripts/tests/integration-test-full.sh"

# API tests
run_test "API Test Suite" "./scripts/tests/api-test-suite.sh"

# Calculate duration
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "\n=========================================="
echo "   TEST SUMMARY"
echo "=========================================="
echo "Duration: ${DURATION}s"

if [ ${#FAILED_TESTS[@]} -eq 0 ]; then
  echo -e "\n✅ ALL TESTS PASSED"
  exit 0
else
  echo -e "\n❌ FAILED TESTS:"
  for test in "${FAILED_TESTS[@]}"; do
    echo "  - $test"
  done
  exit 1
fi
```

---

## Test Data Management

### Test Data Generator

```sql
-- test-data-generator.sql

-- Insert test users
INSERT INTO persons (person_id, first_name, last_name, login, password_hash, active, created_at)
VALUES 
  ('TEST_DOCTOR_001', 'Test', 'Doctor', 'testdoctor', '$2a$10$...', true, NOW()),
  ('TEST_NURSE_001', 'Test', 'Nurse', 'testnurse', '$2a$10$...', true, NOW()),
  ('TEST_ADMIN_001', 'Test', 'Admin', 'testadmin', '$2a$10$...', true, NOW());

-- Insert test patients
INSERT INTO patients (mrn, first_name, last_name, date_of_birth, gender, phone_number, active, created_at)
VALUES
  ('TEST-MRN-001', 'John', 'Doe', '1990-01-01', 'MALE', '+1111111111', true, NOW()),
  ('TEST-MRN-002', 'Jane', 'Smith', '1985-05-15', 'FEMALE', '+2222222222', true, NOW()),
  ('TEST-MRN-003', 'Bob', 'Johnson', '1975-12-20', 'MALE', '+3333333333', true, NOW());

-- Insert test VAS records
INSERT INTO vas_records (patient_mrn, pain_level, location, recorded_by, recorded_at)
VALUES
  ('TEST-MRN-001', 3, 'Ward A', 'TEST_NURSE_001', NOW() - INTERVAL '2 hours'),
  ('TEST-MRN-001', 5, 'Ward A', 'TEST_NURSE_001', NOW() - INTERVAL '1 hour'),
  ('TEST-MRN-001', 8, 'Ward A', 'TEST_NURSE_001', NOW());
```

### Test Data Cleanup

```bash
#!/bin/bash
# cleanup-test-data.sh

echo "=== Cleaning Up Test Data ==="

docker exec dev_postgres psql -U postgres -d pain_management_db <<EOF
-- Delete test records
DELETE FROM vas_records WHERE patient_mrn LIKE 'TEST-%';
DELETE FROM drug_recommendations WHERE patient_mrn LIKE 'TEST-%';
DELETE FROM patients WHERE mrn LIKE 'TEST-%';
DELETE FROM persons WHERE person_id LIKE 'TEST_%';

-- Reset sequences if needed
-- ALTER SEQUENCE patients_id_seq RESTART WITH 1;

SELECT 'Test data cleaned' AS result;
EOF

echo "✅ Test data cleanup completed"
```

---

## Continuous Testing

### GitHub Actions Workflow

Create `.github/workflows/test.yml`:

```yaml
name: Automated Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Start infrastructure
      run: |
        docker-compose up -d kafka postgres
        sleep 30
    
    - name: Run unit tests
      run: mvn test
    
    - name: Build Docker images
      run: docker-compose --profile all build
    
    - name: Start microservices
      run: |
        docker-compose --profile all up -d
        sleep 60
    
    - name: Run integration tests
      run: mvn verify -P integration-tests
    
    - name: Run API tests
      run: bash scripts/tests/api-test-suite.sh
    
    - name: Cleanup
      if: always()
      run: docker-compose --profile all down -v
    
    - name: Upload test results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-results
        path: target/surefire-reports/
```

---

## Test Checklist

### Before Release

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] All microservices health checks pass
- [ ] Kafka message flow validated
- [ ] Database migrations applied successfully
- [ ] API endpoints return expected responses
- [ ] Authentication/authorization working
- [ ] Performance benchmarks met
- [ ] Load testing completed
- [ ] Security scan passed
- [ ] Test data cleaned up

---

**Next:** See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues and solutions  
**See Also:** [DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md) for overall DevOps practices

---

**Last Updated:** January 12, 2026  
**Version:** 3.0
