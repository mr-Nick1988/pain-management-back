package pain_helper_back.backup_restore.services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pain_helper_back.backup_restore.entity.BackupHistory;
import pain_helper_back.backup_restore.enums.BackupStatus;
import pain_helper_back.backup_restore.enums.BackupTrigger;
import pain_helper_back.backup_restore.enums.BackupType;
import pain_helper_back.backup_restore.repository.BackupHistoryRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
 * Сервис для резервного копирования MongoDB.
 *
 * ФУНКЦИОНАЛЬНОСТЬ:
 * - Создание бэкапов MongoDB через mongodump
 * - Восстановление из бэкапов через mongorestore
 * - Управление файлами бэкапов
 * - Автоматическая очистка старых бэкапов
 *
 * ТЕХНОЛОГИЯ:
 * - mongodump: утилита для экспорта MongoDB в BSON
 * - mongorestore: утилита для импорта BSON в MongoDB
 * - Формат: mongo_backup_YYYYMMDD_HHMMSS/ (директория с BSON файлами)
 *
 * ТРЕБОВАНИЯ:
 * - mongodump и mongorestore должны быть установлены в системе
 * - Путь к утилитам настраивается через application.properties
 *
 * СОДЕРЖИМОЕ БЭКАПА:
 * - Аналитические события (AnalyticsEvent)
 * - Технические логи (LogEntry)
 * - Отчеты (DailyReportAggregate, WeeklyReportAggregate, MonthlyReportAggregate)
 * - Метрики производительности (PerformanceMetric)
 * - История бэкапов (BackupHistory)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MongoBackupService {

    private final BackupHistoryRepository backupHistoryRepository;
    @Value("${backup.mongo.directory:./backups/mongodb}")
    private String backupDirectory;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${backup.retention.days:30}")
    private int retentionDays;

    @Value("${backup.mongo.mongodump.path:mongodump}")
    private String mongodumpPath;

    @Value("${backup.mongo.mongorestore.path:mongorestore}")
    private String mongorestorePath;

    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /*
     * Создать бэкап MongoDB.
     *
     * ПРОЦЕСС:
     * 1. Создать запись в истории со статусом IN_PROGRESS
     * 2. Создать директорию для бэкапов
     * 3. Сформировать имя директории с timestamp
     * 4. Выполнить mongodump
     * 5. Рассчитать размер директории
     * 6. Обновить запись в истории
     *
     * КОМАНДА:
     * mongodump --uri="mongodb://..." --out="./backups/mongodb/mongo_backup_20251028_170000"
     *
     * @param trigger Триггер запуска
     * @param initiatedBy Пользователь, инициировавший бэкап
     * @return История бэкапа
     */
    public BackupHistory createBackup(BackupTrigger trigger, String initiatedBy) {
        log.info("Starting MongoDB backup. Trigger: {}, InitiatedBy: {}", trigger, initiatedBy);

        // 1. Создать начальную запись в истории
        BackupHistory history = new BackupHistory();
        history.setBackupType(BackupType.MONGODB.name());
        history.setStatus(BackupStatus.IN_PROGRESS);
        history.setTrigger(trigger.name());
        history.setInitiatedBy(initiatedBy);
        history.setStartTime(LocalDateTime.now());
        history.setExpirationDate(LocalDateTime.now().plusDays(retentionDays));
        // Сохранить начальную запись
        history = backupHistoryRepository.save(history);
        try {
            // 2. Создать директорию для бэкапов
            Path backupDir = Paths.get(backupDirectory);
            Files.createDirectories(backupDir);
            // 3. Сформировать имя директории бэкапа
            String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
            String backupDirName = String.format("mongo_backup_%s", timestamp);
            Path backupPath = backupDir.resolve(backupDirName);
            // 4. Выполнить mongodump
            executeMongoDump(backupPath.toString());
            // 5. Получить размер директории
            long totalSize = calculateDirectorySize(backupPath);
            // 6. Обновить историю - SUCCESS
            history.setStatus(BackupStatus.SUCCESS);
            history.setBackupFilePath(backupPath.toString());
            history.setFileSizeBytes(totalSize);
            history.setEndTime(LocalDateTime.now());
            history.setDurationMs(
                    java.time.Duration.between(history.getStartTime(), history.getEndTime()).toMillis()
            );

            log.info("MongoDB backup completed successfully. Path: {}, Size: {} bytes",
                    backupPath, totalSize);

        } catch (Exception e) {
            // 6. Обновить историю - FAILED
            log.error("MongoDB backup failed: {}", e.getMessage(), e);
            history.setStatus(BackupStatus.FAILED);
            history.setErrorMessage(e.getMessage());
            history.setEndTime(LocalDateTime.now());
        }

        return backupHistoryRepository.save(history);
    }

    /*
     * Восстановить MongoDB из бэкапа.
     *
     * ПРОЦЕСС:
     * 1. Проверить существование директории бэкапа
     * 2. Выполнить mongorestore с флагом --drop
     * 3. Вернуть результат
     *
     * КОМАНДА:
     * mongorestore --uri="mongodb://..." --drop "./backups/mongodb/mongo_backup_20251028_170000"
     *
     * ВАЖНО:
     * - Флаг --drop удаляет существующие коллекции перед восстановлением
     * - Восстановление выполняется онлайн
     * - Данные временно недоступны во время восстановления
     *
     * @param backupPath Путь к директории бэкапа
     * @param initiatedBy Пользователь, инициировавший восстановление
     * @return true если восстановление успешно
     */
    public boolean restoreFromBackup(String backupPath, String initiatedBy) {
        log.warn("Starting MongoDB restore from: {}. InitiatedBy: {}", backupPath, initiatedBy);


        try {
            Path backup = Paths.get(backupPath);
            // Проверить существование директории бэкапа
            if (!Files.exists(backup) || !Files.isDirectory(backup)) {
                log.error("Backup directory not found: {}", backupPath);
                return false;
            }
            // Выполнить mongorestore
            executeMongoRestore(backupPath);
            log.info("MongoDB restore completed successfully from: {}", backupPath);
            return true;
        } catch (Exception e) {
            log.error("MongoDB restore failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /*
     * Выполнить mongodump.
     *
     * КОМАНДА:
     * mongodump --uri="mongodb://..." --out="path/to/backup"
     *
     * ПРОЦЕСС:
     * - Запуск внешнего процесса mongodump
     * - Чтение вывода процесса для логирования
     * - Проверка exit code (0 = успех)
     *
     * @param outputPath Путь к директории для сохранения бэкапа
     * @throws IOException Если ошибка выполнения команды
     * @throws InterruptedException Если процесс прерван
     */
    private void executeMongoDump(String outputPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                mongodumpPath,
                "--uri=" + mongoUri,
                "--out=" + outputPath
        );
        log.debug("Executing mongodump: {}", String.join(" ", processBuilder.command()));

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            // Улучшенное сообщение об ошибке
            String errorMsg = String.format(
                    "Error: %s", e.getMessage()
            );
            log.error(errorMsg);
            throw new IOException(errorMsg, e);
        }
        
        // Читать вывод процесса
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("mongodump: {}", line);
            }
        }
        
        // Читать stderr процесса (mongodump пишет прогресс в stderr)
        try (BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                log.info("mongodump: {}", line); // Прогресс, не ошибка
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("mongodump failed with exit code: " + exitCode);
        }
        log.debug("mongodump completed successfully");
    }

    /*
     * Выполнить mongorestore.
     *
     * КОМАНДА:
     * mongorestore --uri="mongodb://..." --drop "path/to/backup"
     *
     * ФЛАГИ:
     * - --drop: удалить существующие коллекции перед восстановлением
     *
     * @param inputPath Путь к директории с бэкапом
     * @throws IOException Если ошибка выполнения команды
     * @throws InterruptedException Если процесс прерван
     */
    private void executeMongoRestore(String inputPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                mongorestorePath,
                "--uri=" + mongoUri,
                "--drop",  // Удалить существующие коллекции перед восстановлением
                inputPath
        );
        log.debug("Executing mongorestore: {}", String.join(" ", processBuilder.command()));

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            // Улучшенное сообщение об ошибке
            String errorMsg = String.format(
                    "Error: %s", e.getMessage()
            );
            log.error(errorMsg);
            throw new IOException(errorMsg, e);
        }
        
        // Читать вывод процесса
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("mongorestore: {}", line);
            }
        }
        
        // Читать stderr процесса (mongorestore пишет прогресс в stderr)
        try (BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                log.info("mongorestore: {}", line); // Прогресс, не ошибка
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("mongorestore failed with exit code: " + exitCode);
        }
        log.debug("mongorestore completed successfully");
    }

    /*
     * Рассчитать размер директории рекурсивно.
     *
     * ПРОЦЕСС:
     * - Обход всех файлов в директории
     * - Суммирование размеров файлов
     *
     * @param directory Путь к директории
     * @return Размер в байтах
     * @throws IOException Если ошибка чтения файлов
     */
    private long calculateDirectorySize(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
    }

    /*
     * Удалить старые бэкапы согласно политике хранения.
     *
     * ПОЛИТИКА:
     * - Хранение: 30 дней
     * - Удаление: директория + запись в истории
     *
     * @return Количество удаленных директорий
     */
    public int cleanupOldBackups() {
        log.info("Starting cleanup of old MongoDB backups (retention: {} days)", retentionDays);

        LocalDateTime expirationThreshold = LocalDateTime.now();
        List<BackupHistory> expiredBackups = backupHistoryRepository.findByExpirationDateBefore(expirationThreshold);
        int deletedCount = 0;

        for (BackupHistory backup : expiredBackups) {
            if (BackupType.MONGODB.name().equals(backup.getBackupType()) &&
                    backup.getBackupFilePath() != null) {
                try {
                    Path backupPath = Paths.get(backup.getBackupFilePath());
                    if (Files.exists(backupPath)) {
                        deleteDirectory(backupPath);
                        log.info("Deleted expired backup: {}", backup.getBackupFilePath());
                        deletedCount++;
                    }
                    // Удалить запись из истории
                    backupHistoryRepository.delete(backup);
                } catch (IOException e) {
                    log.error("Failed to delete backup directory: {}", backup.getBackupFilePath(), e);
                }
            }
        }
        log.info("Cleanup completed. Deleted {} old MongoDB backups", deletedCount);
        return deletedCount;
    }

    /*
     * Рекурсивно удалить директорию.
     *
     * ПРОЦЕСС:
     * - Обход всех файлов и поддиректорий
     * - Удаление в обратном порядке (файлы перед директориями)
     *
     * @param directory Путь к директории
     * @throws IOException Если ошибка удаления
     */
    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted((a, b) -> -a.compareTo(b))  // Обратный порядок
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("Failed to delete: {}", path, e);
                    }
                });
    }
}
