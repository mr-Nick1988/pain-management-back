# 📦 BACKUP & RESTORE MODULE - Полная документация

## 🎯 НАЗНАЧЕНИЕ

Модуль автоматического резервного копирования и восстановления данных системы Pain Management Assistant.

**Основные функции:**
- Автоматическое резервное копирование H2 и MongoDB по расписанию
- Ручное создание бэкапов через REST API
- Восстановление из бэкапов
- Управление жизненным циклом бэкапов (политика хранения 30 дней)
- Статистика и история операций

---

## 🏗️ АРХИТЕКТУРА

### Технологический стек

**ГИБРИДНЫЙ ПОДХОД:**
1. **H2 Database** - встроенная SQL команда `BACKUP TO` (создает ZIP архив)
2. **MongoDB** - внешние утилиты `mongodump` / `mongorestore` (BSON формат)
3. **Spring Scheduler** - автоматизация по cron расписанию
4. **ModelMapper** - маппинг Entity ↔ DTO

**Почему гибридный подход?**
- H2 имеет встроенную команду бэкапа - используем её напрямую через JDBC
- MongoDB требует внешние утилиты - запускаем через ProcessBuilder
- Spring Scheduler - стандартный механизм для периодических задач
- Это **нормальный и правильный подход** для работы с разными БД

### Структура модуля

```
backup_restore/
├── entity/
│   └── BackupHistory.java          # MongoDB entity (история бэкапов)
├── enums/
│   ├── BackupType.java              # H2_DATABASE, MONGODB, FULL_SYSTEM
│   ├── BackupStatus.java            # IN_PROGRESS, SUCCESS, FAILED
│   └── BackupTrigger.java           # SCHEDULED, MANUAL, PRE_OPERATION
├── dto/
│   ├── BackupRequestDTO.java        # Request для создания бэкапа
│   ├── BackupResponseDTO.java       # Response с деталями бэкапа
│   ├── RestoreRequestDTO.java       # Request для восстановления
│   └── BackupStatisticsDTO.java     # Статистика бэкапов
├── repository/
│   └── BackupHistoryRepository.java # MongoDB repository
├── services/
│   ├── H2BackupService.java         # Бэкап H2 через SQL
│   ├── MongoBackupService.java      # Бэкап MongoDB через mongodump
│   └── BackupManagementService.java # Координация + DTO маппинг
├── scheduler/
│   └── BackupScheduler.java         # Автоматические бэкапы
└── controller/
    └── BackupController.java        # REST API
```

---

## 📊 ИСПОЛЬЗОВАНИЕ DTO И MODELMAPPER

### Почему используется ModelMapper?

**BackupManagementService** использует ModelMapper для конвертации `BackupHistory` (Entity) ↔ `BackupResponseDTO`:

```java
// В BackupManagementService
private final ModelMapper modelMapper;

public BackupResponseDTO createBackup(BackupRequestDTO request) {
    BackupHistory result = h2BackupService.createBackup(...);
    return modelMapper.map(result, BackupResponseDTO.class); // Entity → DTO
}
```

**Преимущества:**
- Разделение слоев: Entity (MongoDB) и API (DTO)
- Автоматический маппинг полей
- Гибкость: можно добавлять вычисляемые поля в DTO (fileSizeMB)

**Сервисы H2/Mongo** работают напрямую с Entity, потому что:
- Это внутренние сервисы (не выходят наружу)
- BackupManagementService координирует их и конвертирует в DTO

---

## 🔄 АВТОМАТИЧЕСКИЕ БЭКАПЫ

### Расписание (BackupScheduler)

```java
@Scheduled(cron = "0 0 2 * * ?")  // 02:00 ежедневно
public void scheduledH2Backup()

@Scheduled(cron = "0 0 3 * * ?")  // 03:00 ежедневно
public void scheduledMongoBackup()

@Scheduled(cron = "0 0 4 * * ?")  // 04:00 ежедневно
public void scheduledCleanup()    // Удаление старых бэкапов
```

### Политика хранения

- **Срок хранения:** 30 дней (настраивается через `backup.retention.days`)
- **Автоматическая очистка:** ежедневно в 04:00
- **Поле expirationDate:** `startTime + 30 дней`

---

## 🌐 REST API ENDPOINTS

### 1. Создать бэкап (ручной)

**POST** `/api/backup/create`

**Request Body:**
```json
{
  "backupType": "H2_DATABASE",  // H2_DATABASE | MONGODB | FULL_SYSTEM
  "initiatedBy": "admin123",
  "metadata": "{\"reason\": \"before_update\"}"
}
```

**Response:**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "backupType": "H2_DATABASE",
  "status": "SUCCESS",
  "backupFilePath": "./backups/h2/h2_backup_20251029_120000.zip",
  "fileSizeBytes": 15728640,
  "fileSizeMB": "15.00 MB",
  "startTime": "2025-10-29T12:00:00",
  "endTime": "2025-10-29T12:00:15",
  "durationMs": 15000,
  "trigger": "MANUAL",
  "initiatedBy": "admin123",
  "errorMessage": null,
  "expirationDate": "2025-11-28T12:00:00"
}
```

### 2. Восстановить из бэкапа

**POST** `/api/backup/restore`

**Request Body:**
```json
{
  "backupId": "507f1f77bcf86cd799439011",
  "initiatedBy": "admin123",
  "confirmed": true  // ОБЯЗАТЕЛЬНО true для защиты
}
```

**Response:**
```json
"Restore initiated successfully. Check logs for details."
```

**ВАЖНО для H2:**
- Восстановление H2 требует ручных шагов (см. логи)
- Приложение нужно остановить, распаковать архив, заменить файлы БД

### 3. История бэкапов

**GET** `/api/backup/history`

**Response:**
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "backupType": "H2_DATABASE",
    "status": "SUCCESS",
    ...
  },
  ...
]
```

### 4. Статистика

**GET** `/api/backup/statistics`

**Response:**
```json
{
  "totalBackups": 45,
  "successfulBackups": 43,
  "failedBackups": 2,
  "totalSizeBytes": 671088640,
  "totalSizeMB": "640.00 MB",
  "totalSizeGB": "0.63 GB",
  "averageBackupDurationMs": 12500.5,
  "recentBackups": [...],
  "h2BackupsCount": 30,
  "mongoBackupsCount": 14,
  "fullSystemBackupsCount": 1
}
```

### 5. Очистка старых бэкапов

**DELETE** `/api/backup/cleanup`

**Response:**
```json
"Deleted 5 old backups"
```

---

## ⚙️ КОНФИГУРАЦИЯ

### application.properties

```properties
# Backup Configuration
backup.h2.directory=./backups/h2
backup.mongo.directory=./backups/mongodb
backup.retention.days=30
backup.mongo.mongodump.path=mongodump
backup.mongo.mongorestore.path=mongorestore
```

### Требования

**MongoDB утилиты:**
- Установить MongoDB Database Tools
- Windows: `mongodump.exe` и `mongorestore.exe` в PATH
- Linux/Mac: `sudo apt install mongodb-database-tools`

**Проверка:**
```bash
mongodump --version
mongorestore --version
```

---

## 🧪 ТЕСТИРОВАНИЕ

### 1. Тест создания H2 бэкапа

**Postman/cURL:**
```bash
curl -X POST http://localhost:8080/api/backup/create \
  -H "Content-Type: application/json" \
  -d '{
    "backupType": "H2_DATABASE",
    "initiatedBy": "test_user"
  }'
```

**Проверка:**
- Файл создан: `./backups/h2/h2_backup_YYYYMMDD_HHMMSS.zip`
- Запись в MongoDB: коллекция `backup_history`
- Статус: `SUCCESS`

### 2. Тест создания MongoDB бэкапа

```bash
curl -X POST http://localhost:8080/api/backup/create \
  -H "Content-Type: application/json" \
  -d '{
    "backupType": "MONGODB",
    "initiatedBy": "test_user"
  }'
```

**Проверка:**
- Директория создана: `./backups/mongodb/mongo_backup_YYYYMMDD_HHMMSS/`
- Внутри BSON файлы: `pain_management_db/backup_history.bson`, etc.

### 3. Тест полного системного бэкапа

```bash
curl -X POST http://localhost:8080/api/backup/create \
  -H "Content-Type: application/json" \
  -d '{
    "backupType": "FULL_SYSTEM",
    "initiatedBy": "test_user"
  }'
```

**Проверка:**
- Создано 3 записи в истории: H2, MongoDB, FULL_SYSTEM
- FULL_SYSTEM содержит metadata с ID дочерних бэкапов

### 4. Тест восстановления MongoDB

```bash
# 1. Получить ID бэкапа
curl http://localhost:8080/api/backup/history

# 2. Восстановить
curl -X POST http://localhost:8080/api/backup/restore \
  -H "Content-Type: application/json" \
  -d '{
    "backupId": "507f1f77bcf86cd799439011",
    "initiatedBy": "test_user",
    "confirmed": true
  }'
```

### 5. Тест автоматической очистки

```bash
# Вручную запустить очистку
curl -X DELETE http://localhost:8080/api/backup/cleanup
```

**Проверка:**
- Удалены бэкапы старше 30 дней
- Файлы/директории удалены с диска
- Записи удалены из MongoDB

### 6. Тест статистики

```bash
curl http://localhost:8080/api/backup/statistics
```

---

## 🚀 FRONTEND INTEGRATION GUIDE

### Компоненты для реализации

#### 1. Backup Dashboard (Главная страница)

**Функциональность:**
- Карточки со статистикой (total, success, failed, size)
- График истории бэкапов (по дням)
- Кнопки "Create Backup", "View History"

**API Calls:**
```typescript
// GET /api/backup/statistics
interface BackupStatistics {
  totalBackups: number;
  successfulBackups: number;
  failedBackups: number;
  totalSizeMB: string;
  totalSizeGB: string;
  averageBackupDurationMs: number;
  recentBackups: BackupResponse[];
  h2BackupsCount: number;
  mongoBackupsCount: number;
  fullSystemBackupsCount: number;
}
```

#### 2. Create Backup Modal

**Форма:**
```typescript
interface BackupRequest {
  backupType: 'H2_DATABASE' | 'MONGODB' | 'FULL_SYSTEM';
  initiatedBy: string;  // Из текущего пользователя
  metadata?: string;
}

// POST /api/backup/create
```

**UI:**
- Radio buttons для выбора типа
- Input для metadata (опционально)
- Progress indicator во время создания

#### 3. Backup History Table

**Колонки:**
- Type (badge: H2/MongoDB/Full)
- Status (badge: Success/Failed/In Progress)
- Size (MB)
- Duration (seconds)
- Started At
- Trigger (Scheduled/Manual)
- Initiated By
- Actions (Restore, Delete)

**API:**
```typescript
// GET /api/backup/history
interface BackupResponse {
  id: string;
  backupType: string;
  status: 'IN_PROGRESS' | 'SUCCESS' | 'FAILED';
  backupFilePath: string;
  fileSizeBytes: number;
  fileSizeMB: string;
  startTime: string;  // ISO 8601
  endTime: string;
  durationMs: number;
  trigger: string;
  initiatedBy: string;
  errorMessage?: string;
  expirationDate: string;
}
```

#### 4. Restore Confirmation Modal

**Форма:**
```typescript
interface RestoreRequest {
  backupId: string;
  initiatedBy: string;
  confirmed: boolean;  // Checkbox "I understand..."
}

// POST /api/backup/restore
```

**UI:**
- Warning message (особенно для H2)
- Checkbox подтверждения
- Кнопка "Restore" (disabled пока не confirmed)

#### 5. Cleanup Management

**UI:**
- Кнопка "Cleanup Old Backups"
- Показать retention policy (30 days)
- Confirmation dialog

**API:**
```typescript
// DELETE /api/backup/cleanup
// Response: "Deleted X old backups"
```

### Пример React компонента

```typescript
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const BackupDashboard = () => {
  const [stats, setStats] = useState<BackupStatistics | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchStatistics();
  }, []);

  const fetchStatistics = async () => {
    const response = await axios.get('/api/backup/statistics');
    setStats(response.data);
  };

  const createBackup = async (type: string) => {
    setLoading(true);
    try {
      await axios.post('/api/backup/create', {
        backupType: type,
        initiatedBy: currentUser.id
      });
      fetchStatistics(); // Refresh
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="backup-dashboard">
      <h1>Backup & Restore</h1>
      
      {/* Statistics Cards */}
      <div className="stats-grid">
        <Card title="Total Backups" value={stats?.totalBackups} />
        <Card title="Success Rate" 
              value={`${(stats?.successfulBackups / stats?.totalBackups * 100).toFixed(1)}%`} />
        <Card title="Total Size" value={stats?.totalSizeGB} />
      </div>

      {/* Actions */}
      <div className="actions">
        <Button onClick={() => createBackup('H2_DATABASE')} disabled={loading}>
          Backup H2 Database
        </Button>
        <Button onClick={() => createBackup('MONGODB')} disabled={loading}>
          Backup MongoDB
        </Button>
        <Button onClick={() => createBackup('FULL_SYSTEM')} disabled={loading}>
          Full System Backup
        </Button>
      </div>

      {/* Recent Backups Table */}
      <BackupHistoryTable backups={stats?.recentBackups} />
    </div>
  );
};
```

---

## ✅ CHECKLIST РЕАЛИЗАЦИИ

### Backend (Готово ✓)
- [x] Entity: BackupHistory
- [x] Enums: BackupType, BackupStatus, BackupTrigger
- [x] DTOs: Request/Response с валидацией
- [x] Repository: MongoDB queries
- [x] Services: H2, Mongo, Management
- [x] Scheduler: Автоматические бэкапы
- [x] Controller: REST API
- [x] Configuration: application.properties

### Frontend (Требуется)
- [ ] Backup Dashboard (статистика)
- [ ] Create Backup Modal
- [ ] Backup History Table
- [ ] Restore Confirmation Modal
- [ ] Cleanup Management
- [ ] Error handling & notifications
- [ ] Loading states
- [ ] Responsive design

---

## 🔒 БЕЗОПАСНОСТЬ

**Доступ:**
- Только для роли ADMIN
- Требуется аутентификация

**Валидация:**
- `@Valid` на всех DTO
- `@AssertTrue` для подтверждения restore
- Проверка статуса бэкапа перед восстановлением

**Логирование:**
- Все операции логируются с initiatedBy
- ERROR level для failed операций
- WARN level для restore операций

---

## 📝 ЗАМЕТКИ

1. **H2 Restore** - требует ручных шагов (остановка приложения)
2. **MongoDB Restore** - онлайн с флагом `--drop`
3. **ModelMapper** - используется только в BackupManagementService
4. **Гибридный подход** - нормально и правильно для разных БД
5. **Spring Scheduler** - стандартный механизм для cron задач
