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

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
 * Сервис для резервного копирования H2 базы данных.
 *
 * ФУНКЦИОНАЛЬНОСТЬ:
 * - Создание бэкапов H2 через SQL команду BACKUP TO
 * - Восстановление из бэкапов
 * - Управление файлами бэкапов
 * - Автоматическая очистка старых бэкапов (политика 30 дней)
 *
 * ТЕХНОЛОГИЯ:
 * - H2 встроенная команда BACKUP TO создает ZIP архив базы
 * - Бэкап включает все таблицы, индексы, данные
 * - Формат: h2_backup_YYYYMMDD_HHMMSS.zip
 *
 * ВАЖНО:
 * - Восстановление H2 требует остановки приложения
 * - Бэкап выполняется онлайн без блокировки БД
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class H2BackupService {

    private final DataSource dataSource;
    private final BackupHistoryRepository backupHistoryRepository;
    @Value("${backup.h2.directory:./backups/h2}")
    private String backupDirectory;
    @Value("${backup.retention.days:30}")
    private int retentionDays;
    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /*
     * Создать бэкап H2 базы данных.
     *
     * ПРОЦЕСС:
     * 1. Создать запись в истории со статусом IN_PROGRESS
     * 2. Создать директорию для бэкапов (если не существует)
     * 3. Сформировать имя файла с timestamp
     * 4. Выполнить SQL команду BACKUP TO
     * 5. Получить размер созданного файла
     * 6. Обновить запись в истории (SUCCESS или FAILED)
     *
     * @param trigger Триггер запуска (SCHEDULED, MANUAL, PRE_OPERATION)
     * @param initiatedBy Пользователь, инициировавший бэкап
     * @return История бэкапа с результатом операции
     */
    public BackupHistory createBackup(BackupTrigger trigger, String initiatedBy) {
        log.info("Starting H2 database backup. Trigger: {}, InitiatedBy: {}", trigger, initiatedBy);

        BackupHistory history = new BackupHistory();
        history.setBackupType(BackupType.H2_DATABASE.name());
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
            // 3. Сформировать имя файла бэкапа
            String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
            String backupFileName = String.format("h2_backup_%s.zip", timestamp);
            Path backupFilePath = backupDir.resolve(backupFileName);
            // 4. Выполнить бэкап через SQL команду
            executeH2Backup(backupFilePath.toString());
            // 5. Получить размер файла
            long fileSize = Files.size(backupFilePath);
            // 6. Обновить историю - SUCCESS
            history.setStatus(BackupStatus.SUCCESS);
            history.setBackupFilePath(backupFilePath.toString());
            history.setFileSizeBytes(fileSize);
            history.setEndTime(LocalDateTime.now());
            history.setDurationMs(
                    java.time.Duration.between(history.getStartTime(), history.getEndTime()).toMillis()
            );
            log.info("H2 backup completed successfully. File: {}, Size: {} bytes",
                    backupFilePath, fileSize);
        } catch (Exception e) {
            // 6. Обновить историю - FAILED
            log.error("H2 backup failed: {}", e.getMessage(), e);
            history.setStatus(BackupStatus.FAILED);
            history.setErrorMessage(e.getMessage());
            history.setEndTime(LocalDateTime.now());
        }
        return backupHistoryRepository.save(history);
    }

    /*
     * Восстановить H2 базу данных из бэкапа.
     *
     * ВАЖНО:
     * - H2 не поддерживает онлайн восстановление
     * - Требуется остановка приложения
     * - Ручной процесс:
     *   1. Остановить приложение
     *   2. Распаковать ZIP архив
     *   3. Заменить файлы БД в ./data/
     *   4. Перезапустить приложение
     *
     * ЭТОТ МЕТОД:
     * - Проверяет существование файла бэкапа
     * - Логирует инструкции для ручного восстановления
     * - Возвращает true если файл существует
     *
     * @param backupFilePath Путь к файлу бэкапа
     * @param initiatedBy Пользователь, инициировавший восстановление
     * @return true если файл бэкапа существует
     */
    public boolean restoreFromBackup(String backupFilePath, String initiatedBy) {
        log.warn("Starting H2 database restore from: {}. InitiatedBy: {}", backupFilePath, initiatedBy);

        try {
            Path backupPath = Paths.get(backupFilePath);
            // Проверить существование файла бэкапа
            if (!Files.exists(backupPath)) {
                log.error("Backup file not found: {}", backupFilePath);
                return false;
            }
            // ВНИМАНИЕ: Восстановление H2 требует остановки приложения
            log.warn("=================================================================");
            log.warn("H2 RESTORE REQUIRES MANUAL STEPS - APPLICATION RESTART NEEDED");
            log.warn("=================================================================");
            log.warn("Please perform the following steps:");
            log.warn("1. STOP the application");
            log.warn("2. EXTRACT backup file: {}", backupFilePath);
            log.warn("3. REPLACE database files in ./data/ directory");
            log.warn("4. RESTART the application");
            log.warn("=================================================================");
            return true;
        } catch (Exception e) {
            log.error("H2 restore validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /*
     * Выполнить SQL команду бэкапа H2.
     *
     * SQL: BACKUP TO 'path/to/backup.zip'
     *
     * ОСОБЕННОСТИ:
     * - Создает ZIP архив с полной копией БД
     * - Выполняется онлайн без блокировки
     * - Включает все таблицы, индексы, последовательности
     *
     * @param backupFilePath Путь к файлу бэкапа
     * @throws SQLException Если ошибка выполнения SQL
     */
    private void executeH2Backup(String backupFilePath) throws SQLException {
        String sql = String.format("BACKUP TO '%s'", backupFilePath);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            log.debug("Executing H2 backup SQL: {}", sql);
            stmt.execute(sql);
            log.debug("H2 backup SQL executed successfully");
        }
    }

    /*
     * Удалить старые бэкапы согласно политике хранения.
     *
     * ПОЛИТИКА:
     * - Хранение: 30 дней (настраивается через backup.retention.days)
     * - Проверка: по полю expirationDate
     * - Удаление: файл + запись в истории
     *
     * ВЫЗОВ:
     * - Автоматически через BackupScheduler (ежедневно в 04:00)
     * - Вручную через DELETE /api/backup/cleanup
     *
     * @return Количество удаленных файлов
     */
    public int cleanupOldBackups() {
        log.info("Starting cleanup of old H2 backups (retention: {} days)", retentionDays);

        LocalDateTime expirationThreshold = LocalDateTime.now();
        List<BackupHistory> expiredBackups = backupHistoryRepository.findByExpirationDateBefore(expirationThreshold);
        int deletedCount = 0;

        for (BackupHistory backup : expiredBackups) {
            if (BackupType.H2_DATABASE.name().equals(backup.getBackupType()) &&
                    backup.getBackupFilePath() != null) {

                try {
                    Path backupPath = Paths.get(backup.getBackupFilePath());
                    if (Files.exists(backupPath)) {
                        Files.delete(backupPath);
                        log.info("Deleted expired backup: {}", backup.getBackupFilePath());
                        deletedCount++;
                    }
                    // Удалить запись из истории
                    backupHistoryRepository.delete(backup);
                } catch (IOException e) {
                    log.error("Failed to delete backup file: {}", backup.getBackupFilePath(), e);
                }
            }
        }
        log.info("Cleanup completed. Deleted {} old H2 backups", deletedCount);
        return deletedCount;
    }
}

