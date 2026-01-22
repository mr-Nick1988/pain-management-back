# 🔧 Troubleshooting Guide - Pain Management Platform

**Version:** 3.0  
**Last Updated:** January 12, 2026  
**Scope:** Infrastructure, Microservices, Common Issues

---

## 📋 Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Infrastructure Issues](#infrastructure-issues)
3. [Microservices Issues](#microservices-issues)
4. [Network and Connectivity](#network-and-connectivity)
5. [Database Problems](#database-problems)
6. [Kafka Issues](#kafka-issues)
7. [Performance Problems](#performance-problems)
8. [Authentication Failures](#authentication-failures)
9. [Container Management](#container-management)
10. [Build and Deployment](#build-and-deployment)
11. [Common Error Messages](#common-error-messages)
12. [Emergency Procedures](#emergency-procedures)

---

## Quick Diagnostics

### System Health Check Script

```bash
#!/bin/bash
# quick-diagnostics.sh

echo "============================================"
echo "   PAIN MANAGEMENT PLATFORM - DIAGNOSTICS"
echo "============================================"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

check_port() {
  local name=$1
  local port=$2
  
  if nc -z localhost $port 2>/dev/null; then
    echo -e "${GREEN}✅${NC} $name (port $port): UP"
    return 0
  else
    echo -e "${RED}❌${NC} $name (port $port): DOWN"
    return 1
  fi
}

check_docker_container() {
  local name=$1
  
  if docker ps --format '{{.Names}}' | grep -q "^${name}$"; then
    local status=$(docker inspect --format='{{.State.Health.Status}}' $name 2>/dev/null)
    if [ "$status" == "healthy" ] || [ "$status" == "" ]; then
      echo -e "${GREEN}✅${NC} Container $name: RUNNING"
      return 0
    else
      echo -e "${YELLOW}⚠️${NC} Container $name: UNHEALTHY ($status)"
      return 1
    fi
  else
    echo -e "${RED}❌${NC} Container $name: NOT RUNNING"
    return 1
  fi
}

echo -e "\n=== Docker Containers ==="
check_docker_container "dev_kafka"
check_docker_container "dev_postgres"
check_docker_container "dev_postgres_analytics"
check_docker_container "dev_auth"
check_docker_container "dev_emr"
check_docker_container "dev_notification"
check_docker_container "dev_pain_escalation"
check_docker_container "dev_external_vas"
check_docker_container "dev_reporting"
check_docker_container "dev_backup"

echo -e "\n=== Network Ports ==="
check_port "Monolith" 8080
check_port "Auth Service" 8082
check_port "Backup Service" 8085
check_port "EMR Service" 8086
check_port "Notification Service" 8087
check_port "Pain Escalation" 8088
check_port "External VAS" 8089
check_port "Reporting Service" 8091
check_port "Kafka" 9092
check_port "Kafdrop" 9000
check_port "Prometheus" 9090
check_port "Grafana" 3000
check_port "PostgreSQL" 5432
check_port "PostgreSQL Analytics" 5433

echo -e "\n=== Disk Space ==="
df -h | grep -E 'Filesystem|/dev/sd|C:'

echo -e "\n=== Memory Usage ==="
free -h 2>/dev/null || systeminfo | findstr /C:"Available Physical Memory"

echo -e "\n=== Docker Stats ==="
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

echo -e "\n============================================"
echo "   DIAGNOSTICS COMPLETE"
echo "============================================"
```

Run diagnostics:
```bash
chmod +x scripts/quick-diagnostics.sh
./scripts/quick-diagnostics.sh
```

---

## Infrastructure Issues

### Issue 1: Kafka Won't Start

**Symptoms:**
```
ERROR Exiting Kafka due to fatal exception (kafka.Kafka$)
org.apache.kafka.common.KafkaException: Failed to load metadata
```

**Possible Causes:**
1. Previous data corruption
2. Cluster ID mismatch
3. Port already in use
4. Insufficient disk space

**Solution 1: Clean Restart**
```bash
# Stop all services
docker-compose down

# Remove Kafka data
docker volume rm painmgmt-kafka-data 2>/dev/null || true

# Remove all volumes (⚠️ DATA LOSS)
docker-compose down -v

# Restart
docker-compose up -d kafka
```

**Solution 2: Check Port Conflict**
```bash
# Windows: Find process using port 9092
netstat -ano | findstr :9092

# Kill the process
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :9092
kill -9 <PID>
```

**Solution 3: Increase Timeout**
```yaml
kafka:
  healthcheck:
    start_period: 60s  # Increase from 30s
    interval: 15s
    timeout: 10s
    retries: 15
```

**Verification:**
```bash
# Check Kafka logs
docker-compose logs kafka | tail -50

# Test broker
docker exec dev_kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# List topics
docker exec dev_kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

### Issue 2: PostgreSQL Connection Refused

**Symptoms:**
```
Connection to localhost:5432 refused
PSQLException: The connection attempt failed
```

**Solution 1: Verify PostgreSQL is Running**
```bash
# Check container status
docker ps | grep postgres

# Check health
docker exec dev_postgres pg_isready -U postgres

# View logs
docker-compose logs postgres | tail -30
```

**Solution 2: Check Port Binding**
```bash
# Windows
netstat -ano | findstr :5432

# Linux/Mac
netstat -tuln | grep 5432

# If port is in use, change in docker-compose.yml:
ports:
  - "5433:5432"  # Use different host port
```

**Solution 3: Recreate Container**
```bash
# Stop and remove
docker-compose stop postgres
docker-compose rm -f postgres

# Start fresh
docker-compose up -d postgres

# Wait for initialization
sleep 10
docker exec dev_postgres pg_isready -U postgres
```

**Solution 4: Fix Permissions**
```bash
# Remove volume and recreate
docker-compose down
docker volume rm painmgmt-postgres-data
docker-compose up -d postgres
```

**Verification:**
```bash
# Connect via psql
docker exec -it dev_postgres psql -U postgres -d pain_management_db

# Query
SELECT version();
\dt

# From host
psql -h localhost -p 5432 -U postgres -d pain_management_db
```

---

### Issue 3: Docker Out of Memory

**Symptoms:**
```
docker: Error response from daemon: OCI runtime create failed
Cannot start container: insufficient memory
```

**Solution 1: Increase Docker Memory**
```bash
# Docker Desktop Settings:
# Resources → Advanced → Memory: 8GB minimum

# Or edit Docker Desktop configuration
```

**Solution 2: Stop Unnecessary Containers**
```bash
# Stop all
docker-compose --profile all down

# Start only what you need
docker-compose --profile core up -d
```

**Solution 3: Clear Docker Cache**
```bash
# Remove unused containers
docker container prune -f

# Remove unused images
docker image prune -a -f

# Remove unused volumes (⚠️ DATA LOSS)
docker volume prune -f

# Remove build cache
docker builder prune -a -f
```

**Solution 4: Optimize Service Memory**
```yaml
# Add to docker-compose.yml
services:
  auth-service:
    environment:
      JAVA_OPTS: "-Xmx512m -Xms256m"
    deploy:
      resources:
        limits:
          memory: 1G
```

**Verification:**
```bash
# Check Docker stats
docker stats --no-stream

# Check system memory
free -h  # Linux
systeminfo  # Windows
```

---

## Microservices Issues

### Issue 1: Authentication Service - JWT Token Invalid

**Symptoms:**
```
401 Unauthorized
Invalid JWT token
JWT signature does not match
```

**Root Cause:**
JWT_SECRET mismatch between monolith and auth-service

**Solution:**
```bash
# 1. Check .env file
cat .env | grep JWT_SECRET

# 2. Ensure JWT_SECRET is identical in:
#    - Monolith application.yml
#    - docker-compose.yml (auth-service environment)
#    - .env file

# 3. Update docker-compose.yml
JWT_SECRET: ${JWT_SECRET:-pain-management-secret-key-change-in-production-min-256-bits-required-for-hs256-algorithm-security}

# 4. Restart auth service
docker-compose restart auth-service

# 5. Get new token
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123"}'
```

**Verification:**
```bash
# Decode JWT token (without verification)
TOKEN="your-jwt-token"
echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '.'

# Check token expiration
# Should show exp (expiration timestamp)
```

---

### Issue 2: EMR Service - FHIR Server Timeout

**Symptoms:**
```
SocketTimeoutException: Read timed out
Connect to hapi.fhir.org:80 timed out
```

**Solution 1: Increase Timeout**
```yaml
emr-service:
  environment:
    FHIR_CONNECTION_TIMEOUT: 30000  # 30 seconds
    FHIR_SOCKET_TIMEOUT: 30000
```

**Solution 2: Use Alternative FHIR Server**
```yaml
emr-service:
  environment:
    FHIR_SERVER_URL: http://test.fhir.org/r4  # Alternative server
```

**Solution 3: Disable External Sync**
```yaml
emr-service:
  environment:
    EMR_SYNC_ENABLED: false  # Manual sync only
```

**Verification:**
```bash
# Test FHIR server connectivity
curl -f http://hapi.fhir.org/baseR4/metadata

# Check EMR service logs
docker-compose logs emr-service | grep -i fhir
```

---

### Issue 3: Notification Service - Email Not Sending

**Symptoms:**
```
Mail server connection failed
Could not connect to SMTP host
```

**Solution 1: Use MailHog for Testing**
```bash
# Start MailHog
docker run -d -p 1025:1025 -p 8025:8025 --name mailhog mailhog/mailhog

# Update docker-compose.yml
notification-service:
  environment:
    MAIL_HOST: host.docker.internal
    MAIL_PORT: 1025

# Access web UI
http://localhost:8025
```

**Solution 2: Configure Real SMTP**
```yaml
notification-service:
  environment:
    MAIL_HOST: smtp.gmail.com
    MAIL_PORT: 587
    MAIL_USERNAME: your-email@gmail.com
    MAIL_PASSWORD: your-app-password
    MAIL_PROPERTIES_SMTP_AUTH: true
    MAIL_PROPERTIES_SMTP_STARTTLS_ENABLE: true
```

**Solution 3: Check Firewall**
```bash
# Test SMTP connectivity
telnet smtp.gmail.com 587

# Windows firewall
netsh advfirewall firewall add rule name="SMTP" dir=out action=allow protocol=TCP localport=587
```

**Verification:**
```bash
# Send test email
curl -X POST http://localhost:8087/api/notifications/email \
  -H "Content-Type: application/json" \
  -d '{
    "to": "test@example.com",
    "subject": "Test",
    "body": "Test email",
    "priority": "NORMAL"
  }'

# Check logs
docker-compose logs notification-service | grep -i mail
```

---

### Issue 4: Pain Escalation Service - Not Detecting Escalations

**Symptoms:**
- VAS recorded but no escalation created
- No events in `pain.escalated` topic

**Solution 1: Check Escalation Threshold**
```yaml
pain-escalation-service:
  environment:
    PAIN_ESCALATION_THRESHOLD: 2  # Lower threshold
    PAIN_CRITICAL_VAS: 7
    PAIN_ANALYSIS_WINDOW: 24
```

**Solution 2: Verify Kafka Consumer**
```bash
# Check consumer group
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group pain-escalation-consumer-group

# Check lag
# Should show lag = 0 (all messages consumed)
```

**Solution 3: Check Database**
```bash
# Verify VAS records exist
docker exec dev_postgres psql -U postgres -d pain_escalation_db -c \
  "SELECT * FROM vas_records WHERE patient_mrn = 'MRN-001' ORDER BY recorded_at DESC LIMIT 5;"

# Check escalations
docker exec dev_postgres psql -U postgres -d pain_escalation_db -c \
  "SELECT * FROM escalations ORDER BY created_at DESC LIMIT 5;"
```

**Solution 4: Manual Trigger**
```bash
# Record VAS via API
curl -X POST http://localhost:8080/api/nurse/patients/MRN-001/vas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "painLevel": 3,
    "location": "Ward A"
  }'

# Wait 2 seconds

curl -X POST http://localhost:8080/api/nurse/patients/MRN-001/vas \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "painLevel": 8,
    "location": "Ward A"
  }'

# Check escalations
curl http://localhost:8088/api/pain-escalation/escalations \
  -H "Authorization: Bearer $TOKEN"
```

**Verification:**
```bash
# View service logs
docker-compose logs -f pain-escalation-service

# Check Kafka topic
timeout 5 docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic pain.escalated \
  --from-beginning
```

---

### Issue 5: External VAS Service - API Key Authentication Fails

**Symptoms:**
```
403 Forbidden
Invalid API key
API key not found
```

**Solution 1: Create API Key**
```bash
# Create API key via API
curl -X POST "http://localhost:8089/api/admin/api-keys?createdBy=admin" \
  -H "Content-Type: application/json" \
  -d '{
    "systemName": "Test Device",
    "description": "Testing",
    "ipWhitelist": "*",
    "rateLimitPerMinute": 100
  }'

# Save the returned apiKey
```

**Solution 2: Check IP Whitelist**
```bash
# Update API key to allow all IPs
curl -X PUT "http://localhost:8089/api/admin/api-keys/{keyId}" \
  -H "Content-Type: application/json" \
  -d '{
    "ipWhitelist": "*"
  }'
```

**Solution 3: Verify API Key Format**
```bash
# API key should be in header X-API-Key
curl -X POST http://localhost:8089/api/external/vas/record \
  -H "X-API-Key: YOUR_ACTUAL_API_KEY_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "patientMrn": "MRN-001",
    "vasLevel": 5,
    "deviceId": "DEV-001"
  }'
```

**Verification:**
```bash
# List API keys
curl http://localhost:8089/api/admin/api-keys

# Check service logs
docker-compose logs external-vas-service | grep -i "api key"
```

---

## Network and Connectivity

### Issue 1: Microservices Can't Communicate

**Symptoms:**
```
Connection refused
UnknownHostException: kafka
Could not resolve host
```

**Solution 1: Check Network**
```bash
# Inspect network
docker network inspect painmgmt-dev-network

# Verify all containers are on same network
docker network inspect painmgmt-dev-network | grep -A 3 "Containers"
```

**Solution 2: Use Correct Hostnames**

From Docker containers, use container names:
- `kafka:29092` (NOT localhost:9092)
- `postgres:5432` (NOT localhost:5432)
- `dev_auth:8082` (NOT localhost:8082)

From host machine, use localhost:
- `localhost:9092`
- `localhost:5432`
- `localhost:8082`

**Solution 3: Recreate Network**
```bash
# Stop all services
docker-compose down

# Remove network
docker network rm painmgmt-dev-network

# Restart
docker-compose up -d
```

**Verification:**
```bash
# Test connectivity from container
docker exec dev_auth ping -c 3 kafka
docker exec dev_auth nc -zv postgres 5432

# Check DNS resolution
docker exec dev_auth nslookup kafka
```

---

### Issue 2: Port Already in Use

**Symptoms:**
```
Error starting userland proxy: listen tcp 0.0.0.0:9092: bind: address already in use
```

**Solution 1: Find and Kill Process**
```bash
# Windows
netstat -ano | findstr :9092
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :9092
kill -9 <PID>
```

**Solution 2: Change Port Mapping**
```yaml
# docker-compose.yml
kafka:
  ports:
    - "9093:9092"  # Use different host port
```

**Solution 3: Stop Conflicting Services**
```bash
# If another docker-compose is running
docker ps -a
docker stop $(docker ps -aq)

# Or specific container
docker stop <container_name>
```

---

### Issue 3: Monolith Can't Reach Kafka

**Symptoms:**
```
Failed to send message to analytics-events
Connection to node -1 (localhost/127.0.0.1:9092) could not be established
```

**Solution:**
```yaml
# application-local.yml (monolith)
spring:
  kafka:
    bootstrap-servers: localhost:9092  # Use localhost, NOT kafka:29092
```

**Verification:**
```bash
# Test from host
echo "test" | kafka-console-producer --bootstrap-server localhost:9092 --topic test-topic

# Check monolith logs
# Should see "Successfully sent message to analytics-events"
```

---

## Database Problems

### Issue 1: Database Schema Not Created

**Symptoms:**
```
Table "patients" does not exist
PSQLException: ERROR: relation "patients" does not exist
```

**Solution 1: Check Hibernate Auto-DDL**
```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # or create, create-drop
```

**Solution 2: Run Liquibase/Flyway Manually**
```bash
# If using Liquibase
mvn liquibase:update

# If using Flyway
mvn flyway:migrate
```

**Solution 3: Create Schema Manually**
```bash
# Connect to database
docker exec -it dev_postgres psql -U postgres -d pain_management_db

# Check tables
\dt

# If empty, run schema.sql
\i /path/to/schema.sql
```

**Verification:**
```bash
# List tables
docker exec dev_postgres psql -U postgres -d pain_management_db -c "\dt"

# Check specific table
docker exec dev_postgres psql -U postgres -d pain_management_db -c "\d patients"
```

---

### Issue 2: Foreign Key Constraint Violations

**Symptoms:**
```
PSQLException: ERROR: insert or update on table violates foreign key constraint
```

**Solution 1: Check Data Order**
```sql
-- Insert parent record first
INSERT INTO patients (mrn, first_name, last_name) VALUES ('MRN-001', 'John', 'Doe');

-- Then insert child record
INSERT INTO vas_records (patient_mrn, pain_level) VALUES ('MRN-001', 5);
```

**Solution 2: Disable Constraints Temporarily**
```sql
-- Disable
ALTER TABLE vas_records DISABLE TRIGGER ALL;

-- Insert data
INSERT INTO ...;

-- Re-enable
ALTER TABLE vas_records ENABLE TRIGGER ALL;
```

**Solution 3: Drop and Recreate FK**
```sql
-- Drop constraint
ALTER TABLE vas_records DROP CONSTRAINT fk_patient_mrn;

-- Add constraint with ON DELETE CASCADE
ALTER TABLE vas_records
ADD CONSTRAINT fk_patient_mrn
FOREIGN KEY (patient_mrn) REFERENCES patients(mrn)
ON DELETE CASCADE;
```

---

### Issue 3: PostgreSQL Logs Growing Too Large

**Symptoms:**
- Disk space running out
- Slow database performance

**Solution 1: Truncate Logs**
```bash
# Connect to container
docker exec -it dev_postgres bash

# Find and truncate logs
find /var/lib/postgresql/data/log -name "*.log" -exec truncate -s 0 {} \;
```

**Solution 2: Configure Log Rotation**
```yaml
postgres:
  command:
    - "postgres"
    - "-c"
    - "logging_collector=on"
    - "-c"
    - "log_rotation_age=1d"
    - "-c"
    - "log_rotation_size=100MB"
```

**Solution 3: Reduce Logging**
```sql
ALTER SYSTEM SET log_statement = 'none';
ALTER SYSTEM SET log_min_duration_statement = 1000;  -- Only log slow queries
SELECT pg_reload_conf();
```

---

## Kafka Issues

### Issue 1: Consumer Lag Increasing

**Symptoms:**
- Messages piling up in topics
- Delayed event processing

**Solution 1: Check Consumer Status**
```bash
# View consumer groups
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list

# Describe specific group
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group notification-service-group
```

**Solution 2: Increase Consumers**
```yaml
# Scale consumer service (if using kubernetes/swarm)
# Or optimize consumer configuration

notification-service:
  environment:
    KAFKA_CONSUMER_CONCURRENCY: 5  # Process 5 messages concurrently
```

**Solution 3: Reset Consumer Offset**
```bash
# Reset to earliest
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --reset-offsets \
  --to-earliest \
  --all-topics \
  --execute

# Reset to latest (skip old messages)
docker exec dev_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --reset-offsets \
  --to-latest \
  --all-topics \
  --execute
```

---

### Issue 2: Topic Auto-Creation Disabled

**Symptoms:**
```
Unknown topic or partition
Topic does not exist
```

**Solution 1: Enable Auto-Creation**
```yaml
kafka:
  environment:
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
```

**Solution 2: Create Topics Manually**
```bash
# Create topic
docker exec dev_kafka kafka-topics \
  --create \
  --topic analytics-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# Verify
docker exec dev_kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092
```

---

### Issue 3: Messages Not Appearing in Kafdrop

**Symptoms:**
- Kafdrop shows topic but no messages
- Messages are being produced

**Solution 1: Check Kafdrop Connection**
```yaml
kafdrop:
  environment:
    KAFKA_BROKERCONNECT: kafka:29092  # Use internal network
```

**Solution 2: Refresh Kafdrop**
- Access http://localhost:9000
- Click on topic name
- Click "View Messages"
- Set "Offset" to "Beginning"

**Solution 3: Verify Messages in Kafka**
```bash
# Consume from beginning
docker exec dev_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic analytics-events \
  --from-beginning \
  --max-messages 10
```

---

## Performance Problems

### Issue 1: Slow API Response Times

**Symptoms:**
- API calls taking >5 seconds
- Timeout errors

**Solution 1: Check Database Query Performance**
```sql
-- Enable query logging
ALTER SYSTEM SET log_min_duration_statement = 100;  -- Log queries >100ms
SELECT pg_reload_conf();

-- View slow queries
SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;
```

**Solution 2: Add Database Indexes**
```sql
-- Check missing indexes
CREATE INDEX idx_patients_mrn ON patients(mrn);
CREATE INDEX idx_vas_records_patient_recorded ON vas_records(patient_mrn, recorded_at);
CREATE INDEX idx_escalations_status ON escalations(status);
```

**Solution 3: Increase JVM Memory**
```yaml
monolith:
  environment:
    JAVA_OPTS: "-Xmx4096m -Xms1024m -XX:+UseG1GC"

auth-service:
  environment:
    JAVA_OPTS: "-Xmx1024m -Xms512m"
```

**Solution 4: Enable Caching**
```java
@EnableCaching
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("patients", "protocols");
    }
}
```

---

### Issue 2: High Memory Usage

**Symptoms:**
- Out of memory errors
- System slowdown

**Solution 1: Monitor Memory**
```bash
# Docker stats
docker stats --no-stream

# JVM heap dump
docker exec dev_auth jmap -heap 1

# GC logs
docker exec dev_auth jstat -gc 1 1000
```

**Solution 2: Tune JVM**
```yaml
auth-service:
  environment:
    JAVA_OPTS: >
      -Xmx1024m
      -Xms512m
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:+UseStringDeduplication
```

**Solution 3: Set Container Limits**
```yaml
auth-service:
  deploy:
    resources:
      limits:
        memory: 2G
      reservations:
        memory: 512M
```

---

## Authentication Failures

### Issue 1: User Login Failed

**Symptoms:**
```
401 Unauthorized
Invalid credentials
User not found
```

**Solution 1: Verify User Exists**
```bash
# Check users in database
docker exec dev_postgres psql -U postgres -d auth_db -c \
  "SELECT person_id, login, active FROM persons;"
```

**Solution 2: Reset Password**
```bash
# Generate bcrypt hash
# Use online tool or script

# Update password
docker exec dev_postgres psql -U postgres -d auth_db -c \
  "UPDATE persons SET password_hash = '\$2a\$10\$...' WHERE login = 'admin';"
```

**Solution 3: Check Authentication Service**
```bash
# Verify service is running
curl http://localhost:8082/actuator/health

# Check logs
docker-compose logs auth-service | grep -i login
```

---

### Issue 2: CORS Errors

**Symptoms:**
```
Access-Control-Allow-Origin header is missing
CORS policy blocked
```

**Solution:**
```yaml
# docker-compose.yml
auth-service:
  environment:
    CORS_ALLOWED_ORIGINS: "http://localhost:5173,http://localhost:3000"

# Or in application.yml
spring:
  web:
    cors:
      allowed-origins:
        - "http://localhost:5173"
        - "http://localhost:3000"
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
      allowed-headers: "*"
```

---

## Container Management

### Issue 1: Container Keeps Restarting

**Symptoms:**
```
docker ps shows container restarting
Status: Restarting (1) X minutes ago
```

**Solution 1: Check Logs**
```bash
# View logs
docker logs dev_auth

# Follow logs
docker logs -f dev_auth

# Last 100 lines
docker logs --tail=100 dev_auth
```

**Solution 2: Disable Restart Policy**
```yaml
auth-service:
  restart: "no"  # Temporarily disable auto-restart
```

**Solution 3: Fix Application Error**
Common issues:
- Missing environment variables
- Database not ready
- Port conflict
- Configuration error

---

### Issue 2: Cannot Remove Container

**Symptoms:**
```
Error response from daemon: container is running
```

**Solution:**
```bash
# Force stop and remove
docker stop dev_auth
docker rm -f dev_auth

# Or using compose
docker-compose stop auth-service
docker-compose rm -f auth-service
```

---

## Build and Deployment

### Issue 1: Maven Build Fails

**Symptoms:**
```
[ERROR] Failed to execute goal
Compilation failure
```

**Solution 1: Clean Build**
```bash
# Clean and rebuild
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Force update dependencies
mvn clean install -U
```

**Solution 2: Check Java Version**
```bash
# Must be Java 21
java -version

# Set JAVA_HOME
export JAVA_HOME=/path/to/jdk-21
```

**Solution 3: Clear Maven Cache**
```bash
# Windows
rmdir /s /q %USERPROFILE%\.m2\repository

# Linux/Mac
rm -rf ~/.m2/repository

# Rebuild
mvn clean install
```

---

### Issue 2: Docker Build Fails

**Symptoms:**
```
ERROR: failed to solve: process "/bin/sh -c mvn clean package"
```

**Solution 1: Check Dockerfile**
```dockerfile
# Multi-stage build
FROM maven:3.9-openjdk-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Solution 2: Build with No Cache**
```bash
docker-compose build --no-cache auth-service
```

**Solution 3: Increase Docker Memory**
Docker Desktop → Settings → Resources → Memory: 8GB

---

## Common Error Messages

### Error 1: `UnknownHostException: kafka`

**Meaning:** Container can't resolve Kafka hostname  
**Fix:** Ensure container is on painmgmt-dev-network

```bash
docker network connect painmgmt-dev-network <container>
```

---

### Error 2: `BindException: Address already in use`

**Meaning:** Port is occupied  
**Fix:** Kill process or change port

```bash
# Windows
netstat -ano | findstr :<PORT>
taskkill /PID <PID> /F
```

---

### Error 3: `HikariPool-1 - Connection is not available`

**Meaning:** Database connection pool exhausted  
**Fix:** Increase pool size

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

---

### Error 4: `PSQLException: FATAL: password authentication failed`

**Meaning:** Wrong database password  
**Fix:** Check .env and docker-compose.yml

```bash
cat .env | grep POSTGRES_PASSWORD
docker-compose config | grep POSTGRES_PASSWORD
```

---

## Emergency Procedures

### Complete System Reset

```bash
#!/bin/bash
# emergency-reset.sh

echo "⚠️  EMERGENCY SYSTEM RESET"
echo "This will delete ALL data!"
read -p "Continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
  echo "Aborted"
  exit 1
fi

echo "Stopping all containers..."
docker-compose --profile all down

echo "Removing volumes..."
docker volume rm painmgmt-postgres-data
docker volume rm painmgmt-analytics-data
docker volume rm painmgmt-prometheus-data
docker volume rm painmgmt-grafana-data

echo "Removing network..."
docker network rm painmgmt-dev-network

echo "Pruning Docker..."
docker system prune -af --volumes

echo "Rebuilding..."
docker-compose --profile all build --no-cache

echo "Starting system..."
docker-compose --profile all up -d

echo "✅ System reset complete"
echo "Wait 2-3 minutes for services to initialize"
```

---

### Backup Before Troubleshooting

```bash
#!/bin/bash
# backup-before-fix.sh

BACKUP_DIR="backups/emergency-$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

echo "Creating emergency backup..."

# Backup databases
docker exec dev_postgres pg_dumpall -U postgres > $BACKUP_DIR/postgres-all.sql

# Backup volumes
docker run --rm \
  -v painmgmt-postgres-data:/data \
  -v $(pwd)/$BACKUP_DIR:/backup \
  alpine tar czf /backup/postgres-data.tar.gz /data

echo "✅ Backup saved to $BACKUP_DIR"
```

---

## Getting Help

### Collect Debug Information

```bash
#!/bin/bash
# collect-debug-info.sh

DEBUG_DIR="debug-$(date +%Y%m%d_%H%M%S)"
mkdir -p $DEBUG_DIR

# System info
uname -a > $DEBUG_DIR/system-info.txt
docker --version >> $DEBUG_DIR/system-info.txt
docker-compose --version >> $DEBUG_DIR/system-info.txt

# Container status
docker ps -a > $DEBUG_DIR/containers.txt

# Logs
docker-compose logs > $DEBUG_DIR/all-logs.txt

# Network
docker network inspect painmgmt-dev-network > $DEBUG_DIR/network.json

# Volumes
docker volume ls > $DEBUG_DIR/volumes.txt

# Compose config
docker-compose config > $DEBUG_DIR/compose-resolved.yml

echo "✅ Debug info collected in $DEBUG_DIR"
echo "Share this directory when reporting issues"
```

---

**Last Updated:** January 12, 2026  
**Version:** 3.0  
**For additional help, see:** [DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md)
