# Backup & Restore Microservice — Documentation

A standalone Spring Boot microservice that creates and restores backups for PostgreSQL and MongoDB, with operation history persisted in a relational database (PostgreSQL) via JPA. Supports manual and scheduled backups, storage retention, and a simple REST API for integration.


## Key Features
- **PostgreSQL backups** using `pg_dump` (custom format `.dump`) and restores using `pg_restore -c`.
- **MongoDB backups** using `mongodump` (directory output) and restores using `mongorestore --drop`.
- **Full system backup** that triggers both PostgreSQL and MongoDB backups and records an aggregate history entry.
- **Operation history** stored in SQL table `backup_history` with status, duration, size, paths, initiator, and metadata.
- **Retention & cleanup** of expired backups based on configurable TTL.
- **Optional scheduling** for daily backups and cleanup jobs.
- **Simple REST API** to create backups, restore, list history, statistics, and cleanup.


## Technology Stack
- Java 21, Spring Boot 3.5.x
- Spring Web, Spring Data JPA, Validation (Jakarta)
- PostgreSQL JDBC driver (for history persistence)
- Lombok (compile-time annotations)


## Architecture Overview
- **Controller**: `BackupController` exposes REST endpoints under `/api/backup`.
- **Services**:
  - `PostgresBackupService` — orchestrates `pg_dump`/`pg_restore` and persistence of history entries.
  - `MongoBackupService` — orchestrates `mongodump`/`mongorestore` and persistence of history entries.
  - `BackupManagementService` — high-level orchestration, statistics, full system flows, and cleanup.
- **Storage**: backup files/directories are written to configured locations on disk.
- **History**: SQL entity `BackupHistory` with `BackupHistoryRepository` (JPA) for queries and retention cleanup.
- **Scheduling**: `BackupScheduler` provides optional cron jobs (disabled by default via property).


## Data Model (History)
Entity: `backup_history`
- `id` (PK)
- `backup_type` (`POSTGRES` | `MONGODB` | `FULL_SYSTEM`)
- `status` (`IN_PROGRESS` | `SUCCESS` | `FAILED`)
- `backup_file_path` (file path for Postgres `.dump` or directory path for Mongo backup)
- `file_size_bytes`
- `start_time`, `end_time`, `duration_ms`
- `trigger` (`SCHEDULED` | `MANUAL` | `PRE_OPERATION`)
- `initiated_by`
- `metadata` (free text/JSON)
- `expiration_date`

Indexes exist on `backup_type`, `status`, `start_time`, and `expiration_date`.


## Prerequisites
- JDK 21
- Maven 3.9+
- A PostgreSQL instance for persisting operation history (separate from the application data you back up, although you may point it to the same server if desired).
- PostgreSQL CLI tools installed: `pg_dump`, `pg_restore` (on PATH or configure absolute paths).
- MongoDB database tools installed: `mongodump`, `mongorestore` (on PATH or configure absolute paths).
- Appropriate DB credentials with privileges to dump and to restore (restore operations are destructive and require object drop permissions).


## Build & Run
- Build:
  ```bash
  mvn -DskipTests package
  ```
- Run (from sources):
  ```bash
  mvn spring-boot:run
  ```
- Run (from packaged JAR):
  ```bash
  java -jar target/backup_restore-0.0.1-SNAPSHOT.jar
  ```

Set environment variables as needed (see Configuration). To use the values shown in `application-local.yml`, run with profile `local`:
```bash
# Linux/macOS
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run

# Windows (PowerShell)
$Env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```


## Configuration
Configuration keys and defaults (as seen in `src/main/resources/application-local.yml`). All of these can be overridden by environment variables.

- **Server**
  - `server.port`: `8085` (only in the `local` profile file; otherwise Spring Boot default is `8080`).

- **Spring Datasource (SQL history store)**
  - `SPRING_DATASOURCE_URL` default: `jdbc:postgresql://localhost:5432/backup_service`
  - `SPRING_DATASOURCE_USERNAME` default: `postgres`
  - `SPRING_DATASOURCE_PASSWORD` default: `postgres`
  - JPA `ddl-auto: update`

- **Backup retention and base directories**
  - `BACKUP_RETENTION_DAYS` → `backup.retention.days` default: `30`
  - `BACKUP_BASE_DIR` → `backup.base-dir` default: `/app/backups`

- **Postgres backup** (`backup.postgres.*`)
  - `BACKUP_PG_DIR` → `backup.postgres.directory` default: `${backup.base-dir}/postgres`
  - `PG_HOST` → `backup.postgres.host` default: `localhost`
  - `PG_PORT` → `backup.postgres.port` default: `5432`
  - `PG_DB` → `backup.postgres.database` default: `pain_management_db`
  - `PG_USER` → `backup.postgres.username` default: `postgres`
  - `PG_PASSWORD` → `backup.postgres.password` default: `postgres`
  - `PG_DUMP_PATH` → `backup.postgres.pg_dump_path` default: `pg_dump`
  - `PG_RESTORE_PATH` → `backup.postgres.pg_restore_path` default: `pg_restore`

- **Mongo backup** (`backup.mongo.*`)
  - `BACKUP_MONGO_DIR` → `backup.mongo.directory` default: `${backup.base-dir}/mongodb`
  - `MONGODB_BACKUP_URI` → `backup.mongo.backup_uri` default: `mongodb://localhost:27017/painmanagement`
  - `MONGO_DUMP_PATH` → `backup.mongo.mongodump_path` default: `mongodump`
  - `MONGO_RESTORE_PATH` → `backup.mongo.mongorestore_path` default: `mongorestore`

- **Scheduling**
  - `BACKUP_SCHEDULER_ENABLED` → `backup.scheduler.enabled` default: `false`

Notes:
- For PostgreSQL CLI calls, the service sets `PGPASSWORD` in the child process environment—do not log or expose this.
- Ensure the configured directories exist or are creatable by the service process.


## Storage Layout & Naming
- **PostgreSQL**: files are created under `backup.postgres.directory` named like `postgres_backup_yyyyMMdd_HHmmss.dump`.
- **MongoDB**: directories are created under `backup.mongo.directory` named like `mongo_backup_yyyyMMdd_HHmmss` (containing subdirectories/files produced by `mongodump`).
- **Full system**: creates individual Postgres and Mongo entries; the aggregate `FULL_SYSTEM` record stores combined metadata referencing both backup IDs.
- **Retention**: each backup history entry has an `expirationDate` (default now + `backup.retention.days`). Cleanup deletes expired backups and their history entries.


## Scheduling (optional)
Enable with `backup.scheduler.enabled=true`.
- `02:00` daily — PostgreSQL backup
- `03:00` daily — MongoDB backup
- `04:00` daily — Cleanup expired backups

Cron time zone is the server’s default unless otherwise configured.


## API Reference
Base path: `/api/backup`
CORS: `*` (open; consider restricting in production).

### 1) Create Backup
- Method: `POST /api/backup/create`
- Request body:
  ```json
  {
    "backupType": "POSTGRES|MONGODB|FULL_SYSTEM",
    "initiatedBy": "john.doe",
    "metadata": "optional free text or JSON"
  }
  ```
- Response: `BackupResponseDTO`
  ```json
  {
    "id": 123,
    "backupType": "POSTGRES",
    "status": "SUCCESS",
    "backupFilePath": "/app/backups/postgres/postgres_backup_20250101_020000.dump",
    "fileSizeBytes": 10485760,
    "startTime": "2025-01-01T02:00:00",
    "endTime": "2025-01-01T02:02:31",
    "durationMs": 151000,
    "trigger": "MANUAL",
    "initiatedBy": "john.doe",
    "metadata": "{...}",
    "expirationDate": "2025-01-31T02:00:00",
    "errorMessage": null
  }
  ```

Example cURL:
```bash
curl -X POST http://localhost:8085/api/backup/create \
  -H "Content-Type: application/json" \
  -d '{"backupType":"POSTGRES","initiatedBy":"admin"}'
```

### 2) Restore From Backup
- Method: `POST /api/backup/restore`
- Request body:
  ```json
  {
    "backupId": "123",
    "initiatedBy": "admin"
  }
  ```
- Response: `200 OK` with message string:
  - `"Restore completed successfully."` on success
  - `"Restore failed. Check service logs."` on failure
- Behavior:
  - Only backups with `status=SUCCESS` are eligible.
  - Postgres uses `pg_restore -c` (drops and recreates objects).
  - Mongo uses `mongorestore --drop` (drops existing collections before restore).

Example cURL:
```bash
curl -X POST http://localhost:8085/api/backup/restore \
  -H "Content-Type: application/json" \
  -d '{"backupId":"123","initiatedBy":"admin"}'
```

### 3) List History
- Method: `GET /api/backup/history?limit=50`
- Response: `List<BackupResponseDTO>` ordered by most recent first.

### 4) Get History by ID
- Method: `GET /api/backup/history/{id}`
- Response: `BackupResponseDTO`.

### 5) Statistics
- Method: `GET /api/backup/statistics`
- Response: `BackupStatisticsDTO` with totals, sizes (MB/GB strings), averages, per-type counts, and last 10 records.

### 6) Cleanup Expired Backups
- Method: `DELETE /api/backup/cleanup`
- Response: textual message, e.g. `"Deleted 3 expired backups"`.


## Enums & DTOs
- `BackupType`: `POSTGRES`, `MONGODB`, `FULL_SYSTEM`
- `BackupStatus`: `IN_PROGRESS`, `SUCCESS`, `FAILED`
- `BackupTrigger`: `SCHEDULED`, `MANUAL`, `PRE_OPERATION`

DTOs (requests/responses) and validation:
- `BackupRequestDTO`
  - `backupType` (required, pattern: `POSTGRES|MONGODB|FULL_SYSTEM`)
  - `initiatedBy` (required, max 50)
  - `metadata` (optional, max 1000)
- `RestoreRequestDTO`
  - `backupId` (required; numeric string)
  - `initiatedBy` (required, max 50)
- `BackupResponseDTO` and `BackupStatisticsDTO` — see API examples.


## Operational Guidance
- **Backups**
  - Ensure CLI tools are installed and accessible or provide absolute paths via configuration.
  - Ensure the configured directories are writable.
  - For `FULL_SYSTEM`, two independent entries are created (Postgres + Mongo); the aggregate FULL_SYSTEM entry stores metadata linking to these by ID.
- **Restores (Destructive)**
  - Postgres restore runs `pg_restore -c` which drops and recreates objects in the target database.
  - Mongo restore runs `mongorestore --drop` which drops existing data before restoring.
  - Validate target hosts/URIs before restoring to avoid overwriting the wrong environment.
- **Retention & Cleanup**
  - Expired backup files/directories are removed and corresponding history entries deleted.
  - Default retention is 30 days (`backup.retention.days`).
  - FULL_SYSTEM entries currently use a fixed `+30 days` in code for expiration.
- **Scheduling**
  - Disabled by default. Enable with `backup.scheduler.enabled=true`.
  - Cron times (02:00 PG, 03:00 Mongo, 04:00 cleanup) can be customized in code if needed.


## Error Handling & Responses
- `create` and `history` endpoints respond with 200 and DTOs on success.
- `restore` returns `200 OK` with a text message indicating success/failure; some invalid states (e.g., nonexistent ID, non-success backup) throw runtime exceptions which typically map to `500` responses.
- For detailed root causes, refer to service logs; process outputs from CLI tools are piped to logs.


## Security Considerations
- Restrict API exposure and CORS in production (controller currently sets `@CrossOrigin(origins = "*")`).
- Store DB credentials securely and do not log them; `PGPASSWORD` is injected into the child process environment for CLI calls.
- Restores are destructive; limit who can call restore endpoints and consider adding authentication/authorization.
- Ensure filesystem permissions prevent unauthorized access to backup files/directories.


## Troubleshooting
- "Command not found" or exit code errors
  - Ensure `pg_dump`, `pg_restore`, `mongodump`, `mongorestore` are installed and on PATH, or set `PG_DUMP_PATH`, `PG_RESTORE_PATH`, `MONGO_DUMP_PATH`, `MONGO_RESTORE_PATH`.
- Permission denied writing backups
  - Verify process user can create and write to `backup.base-dir` and subdirectories.
- Restore fails
  - Check service logs for the full CLI output; confirm credentials and privileges allow dropping and recreating objects.
- History not persisting
  - Verify `SPRING_DATASOURCE_*` settings point to a reachable PostgreSQL instance; confirm the schema is created (JPA `ddl-auto: update`).


## Limitations & Notes
- The `FULL_SYSTEM` entry stores metadata as a simple JSON-like text and parses it via string search (not a full JSON parser).
- `restore` endpoint returns status in message text rather than structured JSON; consider extending for richer error semantics.
- Default logging category in `application-local.yml` uses a package prefix (`com.painmgmt.backuprestore`) that may differ from the actual code package; adjust if needed for log level filtering.


## Example End-to-End Flow
1. Create a PostgreSQL backup:
   ```bash
   curl -X POST http://localhost:8085/api/backup/create \
     -H "Content-Type: application/json" \
     -d '{"backupType":"POSTGRES","initiatedBy":"ops"}'
   ```
2. Confirm it appears in history:
   ```bash
   curl "http://localhost:8085/api/backup/history?limit=10"
   ```
3. Restore from that backup ID (destructive):
   ```bash
   curl -X POST http://localhost:8085/api/backup/restore \
     -H "Content-Type: application/json" \
     -d '{"backupId":"<ID>","initiatedBy":"ops"}'
   ```


## Versioning
- Artifact: `pain.managment:backup_restore:0.0.1-SNAPSHOT`


## License
- Internal project (add license details if needed).
