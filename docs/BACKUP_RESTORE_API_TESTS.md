# 🧪 BACKUP & RESTORE MODULE - API TESTING GUIDE

## 📋 Postman Collection

### Environment Variables
```json
{
  "baseUrl": "http://localhost:8080",
  "adminUser": "admin123"
}
```

---

## 🔧 TEST SCENARIOS

### 1. CREATE H2 BACKUP (Manual)

**Request:**
```http
POST {{baseUrl}}/api/backup/create
Content-Type: application/json

{
  "backupType": "H2_DATABASE",
  "initiatedBy": "{{adminUser}}",
  "metadata": "{\"reason\": \"manual_test\", \"version\": \"1.0\"}"
}
```

**Expected Response (200 OK):**
```json
{
  "id": "67890abcdef1234567890abc",
  "backupType": "H2_DATABASE",
  "status": "SUCCESS",
  "backupFilePath": "./backups/h2/h2_backup_20251029_143022.zip",
  "fileSizeBytes": 15728640,
  "fileSizeMB": "15.00 MB",
  "startTime": "2025-10-29T14:30:22",
  "endTime": "2025-10-29T14:30:35",
  "durationMs": 13000,
  "trigger": "MANUAL",
  "initiatedBy": "admin123",
  "errorMessage": null,
  "expirationDate": "2025-11-28T14:30:22"
}
```

**Validation:**
- ✅ Status = SUCCESS
- ✅ File exists at backupFilePath
- ✅ File size > 0
- ✅ Duration < 30 seconds (typical)
- ✅ Trigger = MANUAL
- ✅ ExpirationDate = startTime + 30 days

---

### 2. CREATE MONGODB BACKUP (Manual)

**Request:**
```http
POST {{baseUrl}}/api/backup/create
Content-Type: application/json

{
  "backupType": "MONGODB",
  "initiatedBy": "{{adminUser}}"
}
```

**Expected Response (200 OK):**
```json
{
  "id": "67890abcdef1234567890def",
  "backupType": "MONGODB",
  "status": "SUCCESS",
  "backupFilePath": "./backups/mongodb/mongo_backup_20251029_143100",
  "fileSizeBytes": 5242880,
  "fileSizeMB": "5.00 MB",
  "startTime": "2025-10-29T14:31:00",
  "endTime": "2025-10-29T14:31:08",
  "durationMs": 8000,
  "trigger": "MANUAL",
  "initiatedBy": "admin123",
  "errorMessage": null,
  "expirationDate": "2025-11-28T14:31:00"
}
```

**Validation:**
- ✅ Directory exists at backupFilePath
- ✅ Contains BSON files (*.bson, *.metadata.json)
- ✅ Collections backed up: backup_history, analytics_events, performance_metrics, etc.

---

### 3. CREATE FULL SYSTEM BACKUP

**Request:**
```http
POST {{baseUrl}}/api/backup/create
Content-Type: application/json

{
  "backupType": "FULL_SYSTEM",
  "initiatedBy": "{{adminUser}}",
  "metadata": "{\"reason\": \"before_system_upgrade\"}"
}
```

**Expected Response (200 OK):**
```json
{
  "id": "67890abcdef1234567890xyz",
  "backupType": "FULL_SYSTEM",
  "status": "SUCCESS",
  "backupFilePath": null,
  "fileSizeBytes": 20971520,
  "fileSizeMB": "20.00 MB",
  "startTime": "2025-10-29T14:35:00",
  "endTime": "2025-10-29T14:35:25",
  "durationMs": 25000,
  "trigger": "MANUAL",
  "initiatedBy": "admin123",
  "errorMessage": null,
  "expirationDate": "2025-11-28T14:35:00"
}
```

**Validation:**
- ✅ Creates 3 records in backup_history: H2, MONGODB, FULL_SYSTEM
- ✅ FULL_SYSTEM.metadata contains child backup IDs
- ✅ Total size = H2 size + MongoDB size
- ✅ Duration = sum of both backups

**Check metadata:**
```json
{
  "h2_backup_id": "67890abcdef1234567890abc",
  "mongo_backup_id": "67890abcdef1234567890def",
  "h2_status": "SUCCESS",
  "mongo_status": "SUCCESS"
}
```

---

### 4. GET BACKUP HISTORY

**Request:**
```http
GET {{baseUrl}}/api/backup/history
```

**Expected Response (200 OK):**
```json
[
  {
    "id": "67890abcdef1234567890xyz",
    "backupType": "FULL_SYSTEM",
    "status": "SUCCESS",
    ...
  },
  {
    "id": "67890abcdef1234567890def",
    "backupType": "MONGODB",
    "status": "SUCCESS",
    ...
  },
  ...
]
```

**Validation:**
- ✅ Sorted by startTime DESC (newest first)
- ✅ Max 50 records returned
- ✅ All required fields present

---

### 5. GET BACKUP STATISTICS

**Request:**
```http
GET {{baseUrl}}/api/backup/statistics
```

**Expected Response (200 OK):**
```json
{
  "totalBackups": 15,
  "successfulBackups": 14,
  "failedBackups": 1,
  "totalSizeBytes": 314572800,
  "totalSizeMB": "300.00 MB",
  "totalSizeGB": "0.29 GB",
  "averageBackupDurationMs": 12500.5,
  "recentBackups": [
    { "id": "...", "backupType": "H2_DATABASE", ... },
    ...
  ],
  "h2BackupsCount": 10,
  "mongoBackupsCount": 4,
  "fullSystemBackupsCount": 1
}
```

**Validation:**
- ✅ totalBackups = successfulBackups + failedBackups
- ✅ recentBackups contains max 10 items
- ✅ Counts sum correctly

---

### 6. RESTORE FROM BACKUP (MongoDB)

**Request:**
```http
POST {{baseUrl}}/api/backup/restore
Content-Type: application/json

{
  "backupId": "67890abcdef1234567890def",
  "initiatedBy": "{{adminUser}}",
  "confirmed": true
}
```

**Expected Response (200 OK):**
```json
"Restore initiated successfully. Check logs for details."
```

**Validation:**
- ✅ MongoDB collections restored
- ✅ Data matches backup timestamp
- ✅ Check logs for mongorestore output

**Error Cases:**
```http
# Backup not found
{
  "backupId": "invalid_id",
  "initiatedBy": "admin123",
  "confirmed": true
}
# Response: 500 "Backup not found with ID: invalid_id"

# Not confirmed
{
  "backupId": "67890abcdef1234567890def",
  "initiatedBy": "admin123",
  "confirmed": false
}
# Response: 400 "Restore operation must be confirmed (confirmed = true)"

# Failed backup
{
  "backupId": "failed_backup_id",
  "initiatedBy": "admin123",
  "confirmed": true
}
# Response: 500 "Cannot restore from failed backup"
```

---

### 7. RESTORE FROM BACKUP (H2)

**Request:**
```http
POST {{baseUrl}}/api/backup/restore
Content-Type: application/json

{
  "backupId": "67890abcdef1234567890abc",
  "initiatedBy": "{{adminUser}}",
  "confirmed": true
}
```

**Expected Response (200 OK):**
```json
"Restore initiated successfully. Check logs for details."
```

**Check Logs:**
```
WARN  - H2 RESTORE REQUIRES MANUAL STEPS - APPLICATION RESTART NEEDED
WARN  - Please perform the following steps:
WARN  - 1. STOP the application
WARN  - 2. EXTRACT backup file: ./backups/h2/h2_backup_20251029_143022.zip
WARN  - 3. REPLACE database files in ./data/ directory
WARN  - 4. RESTART the application
```

**Manual Steps:**
1. Stop Spring Boot app
2. Extract ZIP to temp folder
3. Copy `pain_management_db.mv.db` to `./data/`
4. Restart app

---

### 8. CLEANUP OLD BACKUPS

**Request:**
```http
DELETE {{baseUrl}}/api/backup/cleanup
```

**Expected Response (200 OK):**
```json
"Deleted 3 old backups"
```

**Validation:**
- ✅ Backups older than 30 days removed
- ✅ Files/directories deleted from disk
- ✅ Records removed from MongoDB

---

## ❌ ERROR SCENARIOS

### 1. Invalid Backup Type

**Request:**
```http
POST {{baseUrl}}/api/backup/create
Content-Type: application/json

{
  "backupType": "INVALID_TYPE",
  "initiatedBy": "admin123"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "timestamp": "2025-10-29T14:40:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Backup type must be H2_DATABASE, MONGODB, or FULL_SYSTEM",
  "path": "/api/backup/create"
}
```

---

### 2. Missing Required Fields

**Request:**
```http
POST {{baseUrl}}/api/backup/create
Content-Type: application/json

{
  "backupType": "H2_DATABASE"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "timestamp": "2025-10-29T14:41:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Initiated by is required",
  "path": "/api/backup/create"
}
```

---

### 3. Backup File Not Found (Restore)

**Request:**
```http
POST {{baseUrl}}/api/backup/restore
Content-Type: application/json

{
  "backupId": "backup_with_missing_file",
  "initiatedBy": "admin123",
  "confirmed": true
}
```

**Expected Response (200 OK but failed):**
```json
"Restore failed. Check logs for details."
```

**Logs:**
```
ERROR - Backup file not found: ./backups/h2/missing_file.zip
ERROR - Restore failed from backup: backup_with_missing_file
```

---

## 🔄 AUTOMATED BACKUP TESTING

### Test Scheduled Backups

**Temporarily change cron for testing:**
```properties
# In application.properties (for testing only)
# Run every minute instead of daily
```

**Modify BackupScheduler.java:**
```java
@Scheduled(cron = "0 * * * * ?")  // Every minute
public void scheduledH2Backup() {
    log.info("Starting scheduled H2 backup");
    h2BackupService.createBackup(BackupTrigger.SCHEDULED, "SYSTEM");
}
```

**Validation:**
1. Wait 1 minute
2. Check `GET /api/backup/history`
3. Verify new backup with trigger = "SCHEDULED"
4. Verify initiatedBy = "SYSTEM"

**Restore original cron after testing!**

---

## 📊 PERFORMANCE BENCHMARKS

### Expected Timings

| Operation | Expected Duration | Notes |
|-----------|------------------|-------|
| H2 Backup (15 MB) | 10-20 seconds | Depends on disk I/O |
| MongoDB Backup (5 MB) | 5-15 seconds | Depends on mongodump speed |
| Full System Backup | 20-40 seconds | Sum of both |
| MongoDB Restore | 10-20 seconds | Depends on mongorestore speed |
| Cleanup (10 files) | 1-3 seconds | File deletion |

### Load Testing

**Create 10 backups in parallel:**
```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/backup/create \
    -H "Content-Type: application/json" \
    -d '{"backupType":"H2_DATABASE","initiatedBy":"load_test_'$i'"}' &
done
wait
```

**Validation:**
- ✅ All 10 backups created successfully
- ✅ No file conflicts (unique timestamps)
- ✅ All records in MongoDB

---

## ✅ ACCEPTANCE CRITERIA

### Functional Requirements
- [x] Manual backup creation works for all types
- [x] Scheduled backups run automatically
- [x] MongoDB restore works online
- [x] H2 restore provides manual instructions
- [x] Cleanup removes old backups
- [x] Statistics calculated correctly
- [x] History sorted by date DESC

### Non-Functional Requirements
- [x] Backup duration < 30 seconds (typical)
- [x] API response time < 500ms (except create/restore)
- [x] Validation errors return 400
- [x] Server errors return 500
- [x] All operations logged

### Security Requirements
- [x] Only ADMIN role can access endpoints
- [x] Restore requires confirmation
- [x] All operations audit logged with initiatedBy

---

## 🐛 TROUBLESHOOTING

### Issue: mongodump not found

**Error:**
```
IOException: mongodump failed with exit code: 127
```

**Solution:**
```bash
# Install MongoDB Database Tools
# Windows: Download from mongodb.com
# Linux: sudo apt install mongodb-database-tools
# Mac: brew install mongodb-database-tools

# Verify installation
mongodump --version
```

### Issue: Permission denied on backup directory

**Error:**
```
IOException: Permission denied: ./backups/h2
```

**Solution:**
```bash
# Create directories with correct permissions
mkdir -p ./backups/h2
mkdir -p ./backups/mongodb
chmod 755 ./backups
```

### Issue: MongoDB connection refused

**Error:**
```
mongodump: Failed to connect to localhost:27017
```

**Solution:**
- Check MongoDB is running: `systemctl status mongod`
- Verify URI in application.properties
- Test connection: `mongosh`

---

## 📝 TEST REPORT TEMPLATE

```markdown
# Backup & Restore Module - Test Report

**Date:** 2025-10-29
**Tester:** [Your Name]
**Environment:** Development

## Test Results

| Test Case | Status | Duration | Notes |
|-----------|--------|----------|-------|
| Create H2 Backup | ✅ PASS | 12s | File size: 15 MB |
| Create MongoDB Backup | ✅ PASS | 8s | File size: 5 MB |
| Create Full System Backup | ✅ PASS | 22s | Total: 20 MB |
| Get History | ✅ PASS | 150ms | 15 records |
| Get Statistics | ✅ PASS | 200ms | All counts correct |
| Restore MongoDB | ✅ PASS | 15s | Data verified |
| Restore H2 | ✅ PASS | N/A | Manual steps logged |
| Cleanup Old Backups | ✅ PASS | 2s | 3 files deleted |
| Invalid Backup Type | ✅ PASS | 50ms | 400 error |
| Missing Required Field | ✅ PASS | 50ms | 400 error |
| Scheduled Backup | ✅ PASS | 13s | Trigger=SCHEDULED |

## Issues Found
- None

## Recommendations
- Add progress indicator for long-running backups
- Implement email notifications for failed backups
- Add backup compression options
```
