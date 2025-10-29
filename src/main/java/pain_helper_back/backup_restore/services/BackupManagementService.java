package pain_helper_back.backup_restore.services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.backup_restore.dto.BackupRequestDTO;
import pain_helper_back.backup_restore.dto.BackupResponseDTO;
import pain_helper_back.backup_restore.dto.BackupStatisticsDTO;
import pain_helper_back.backup_restore.dto.RestoreRequestDTO;
import pain_helper_back.backup_restore.entity.BackupHistory;
import pain_helper_back.backup_restore.enums.BackupStatus;
import pain_helper_back.backup_restore.enums.BackupTrigger;
import pain_helper_back.backup_restore.enums.BackupType;
import pain_helper_back.backup_restore.repository.BackupHistoryRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/*
 * Общий сервис управления резервными копиями.
 *
 * ФУНКЦИОНАЛЬНОСТЬ:
 * - Координация бэкапов H2 и MongoDB
 * - Полный системный бэкап (H2 + MongoDB)
 * - Восстановление из бэкапов
 * - Статистика и история бэкапов
 * - Конвертация Entity ↔ DTO
 *
 * ИСПОЛЬЗОВАНИЕ:
 * - BackupController (REST API)
 * - BackupScheduler (автоматические бэкапы)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BackupManagementService {
    private final H2BackupService h2BackupService;
    private final MongoBackupService mongoBackupService;
    private final BackupHistoryRepository backupHistoryRepository;
    private final ModelMapper modelMapper;

    /*
     * Создать бэкап по запросу.
     *
     * ПРОЦЕСС:
     * - H2_DATABASE: вызов H2BackupService
     * - MONGODB: вызов MongoBackupService
     * - FULL_SYSTEM: последовательный вызов обоих сервисов
     *
     * @param request DTO с типом бэкапа и инициатором
     * @return DTO с результатом операции
     */
    public BackupResponseDTO createBackup(BackupRequestDTO request) {
        log.info("Creating backup: type={}, initiatedBy={}",
                request.getBackupType(), request.getInitiatedBy());

        BackupType backupType = BackupType.valueOf(request.getBackupType());
        BackupHistory result;

        switch (backupType) {
            case H2_DATABASE:
                result = h2BackupService.createBackup(
                        BackupTrigger.MANUAL,
                        request.getInitiatedBy()
                );
                break;

            case MONGODB:
                result = mongoBackupService.createBackup(
                        BackupTrigger.MANUAL,
                        request.getInitiatedBy()
                );
                break;

            case FULL_SYSTEM:
                result = createFullSystemBackup(request.getInitiatedBy());
                break;

            default:
                throw new IllegalArgumentException("Unknown backup type: " + backupType);
        }
        return modelMapper.map(result, BackupResponseDTO.class);
    }

    /*
     * Создать полный системный бэкап (H2 + MongoDB).
     *
     * ПРОЦЕСС:
     * 1. Создать бэкап H2
     * 2. Создать бэкап MongoDB
     * 3. Создать сводную запись FULL_SYSTEM
     *
     * РЕЗУЛЬТАТ:
     * - Если оба успешны: статус SUCCESS
     * - Если хотя бы один неудачен: статус FAILED
     *
     * @param initiatedBy Инициатор бэкапа
     * @return История полного бэкапа
     */
    private BackupHistory createFullSystemBackup(String initiatedBy) {
        log.info("Creating full system backup (H2 + MongoDB). InitiatedBy: {}", initiatedBy);

        LocalDateTime startTime = LocalDateTime.now();
        // 1. Бэкап H2
        BackupHistory h2Backup = h2BackupService.createBackup(BackupTrigger.MANUAL, initiatedBy);
        // 2. Бэкап MongoDB
        BackupHistory mongoBackup = mongoBackupService.createBackup(BackupTrigger.MANUAL, initiatedBy);

        // 3. Создать сводную запись
        BackupHistory fullBackup = new BackupHistory();
        fullBackup.setBackupType(BackupType.FULL_SYSTEM.name());
        fullBackup.setTrigger(BackupTrigger.MANUAL.name());
        fullBackup.setInitiatedBy(initiatedBy);
        fullBackup.setStartTime(startTime);
        fullBackup.setEndTime(LocalDateTime.now());

        // Определить общий статус
        boolean bothSuccess = h2Backup.getStatus() == BackupStatus.SUCCESS &&
                mongoBackup.getStatus() == BackupStatus.SUCCESS;
        fullBackup.setStatus(bothSuccess ? BackupStatus.SUCCESS : BackupStatus.FAILED);
        // Суммировать размеры
        long totalSize = 0L;
        if (h2Backup.getFileSizeBytes() != null) {
            totalSize += h2Backup.getFileSizeBytes();
        }
        if (mongoBackup.getFileSizeBytes() != null) {
            totalSize += mongoBackup.getFileSizeBytes();
        }
        fullBackup.setFileSizeBytes(totalSize);
        // Рассчитать длительность
        fullBackup.setDurationMs(
                Duration.between(startTime, fullBackup.getEndTime()).toMillis()
        );
        // Сохранить метаданные
        String metadata = String.format(
                "{\"h2_backup_id\":\"%s\",\"mongo_backup_id\":\"%s\",\"h2_status\":\"%s\",\"mongo_status\":\"%s\"}",
                h2Backup.getId(),
                mongoBackup.getId(),
                h2Backup.getStatus(),
                mongoBackup.getStatus()
        );
        fullBackup.setMetadata(metadata);
        // Установить срок хранения
        fullBackup.setExpirationDate(LocalDateTime.now().plusDays(30));
        log.info("Full system backup completed. Status: {}, TotalSize: {} bytes",
                fullBackup.getStatus(), totalSize);

        return backupHistoryRepository.save(fullBackup);
    }

    /*
     * Восстановить из бэкапа.
     *
     * ПРОЦЕСС:
     * 1. Найти бэкап по ID
     * 2. Определить тип бэкапа
     * 3. Вызвать соответствующий сервис восстановления
     *
     * @param request DTO с ID бэкапа и подтверждением
     * @return Сообщение о результате
     */
    public String restoreFromBackup(RestoreRequestDTO request) {
        log.warn("Restore requested: backupId={}, initiatedBy={}",
                request.getBackupId(), request.getInitiatedBy());
        // Найти бэкап
        BackupHistory backup = backupHistoryRepository.findById(request.getBackupId())
                .orElseThrow(() -> new RuntimeException("Backup not found with ID: " + request.getBackupId()));
        // Проверить статус бэкапа
        if (backup.getStatus() != BackupStatus.SUCCESS) {
            throw new RuntimeException("Cannot restore from failed backup");
        }
        // Выполнить восстановление
        BackupType backupType = BackupType.valueOf(backup.getBackupType());
        boolean success;

        switch (backupType) {
            case H2_DATABASE:
                success = h2BackupService.restoreFromBackup(
                        backup.getBackupFilePath(),
                        request.getInitiatedBy()
                );
                break;

            case MONGODB:
                success = mongoBackupService.restoreFromBackup(
                        backup.getBackupFilePath(),
                        request.getInitiatedBy()
                );
                break;

            case FULL_SYSTEM:
                throw new RuntimeException("Full system restore not implemented. Please restore H2 and MongoDB separately.");

            default:
                throw new IllegalArgumentException("Unknown backup type: " + backupType);
        }
        if (success) {
            log.info("Restore completed successfully from backup: {}", request.getBackupId());
            return "Restore initiated successfully. Check logs for details.";
        } else {
            log.error("Restore failed from backup: {}", request.getBackupId());
            return "Restore failed. Check logs for details.";
        }
    }

    /*
     * Получить историю бэкапов.
     *
     * @return Список последних 50 бэкапов
     */
    @Transactional(readOnly = true)
    public List<BackupResponseDTO> getBackupHistory() {
        log.info("Fetching backup history");

        List<BackupHistory> history = backupHistoryRepository
                .findByOrderByStartTimeDesc(PageRequest.of(0, 50));

        return history.stream()
                .map(h -> modelMapper.map(h, BackupResponseDTO.class))
                .toList();
    }

    /*
     * Получить детали конкретного бэкапа.
     *
     * @param backupId ID бэкапа
     * @return DTO с деталями бэкапа
     */
    @Transactional(readOnly = true)
    public BackupResponseDTO getBackupById(String backupId) {
        log.info("Fetching backup details: {}", backupId);

        BackupHistory backup = backupHistoryRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Backup not found with ID: " + backupId));

        return modelMapper.map(backup, BackupResponseDTO.class);
    }

    /*
     * Получить статистику бэкапов.
     *
     * СТАТИСТИКА:
     * - Общее количество бэкапов
     * - Успешные/неудачные
     * - Общий размер
     * - Средняя длительность
     * - Разбивка по типам
     * - Последние 10 бэкапов
     *
     * @return DTO со статистикой
     */
    @Transactional(readOnly = true)
    public BackupStatisticsDTO getStatistics() {
        log.info("Calculating backup statistics");

        List<BackupHistory> allBackups = backupHistoryRepository.findAll();

        // Общее количество
        long totalBackups = allBackups.size();

        // Успешные/неудачные
        long successfulBackups = allBackups.stream()
                .filter(b -> b.getStatus() == BackupStatus.SUCCESS)
                .count();

        long failedBackups = allBackups.stream()
                .filter(b -> b.getStatus() == BackupStatus.FAILED)
                .count();

        // Общий размер
        long totalSizeBytes = allBackups.stream()
                .filter(b -> b.getFileSizeBytes() != null)
                .mapToLong(BackupHistory::getFileSizeBytes)
                .sum();

        String totalSizeMB = String.format("%.2f MB", totalSizeBytes / 1024.0 / 1024.0);
        String totalSizeGB = String.format("%.2f GB", totalSizeBytes / 1024.0 / 1024.0 / 1024.0);

        // Средняя длительность
        double averageDuration = allBackups.stream()
                .filter(b -> b.getDurationMs() != null)
                .mapToLong(BackupHistory::getDurationMs)
                .average()
                .orElse(0.0);

        // Разбивка по типам
        long h2Count = allBackups.stream()
                .filter(b -> BackupType.H2_DATABASE.name().equals(b.getBackupType()))
                .count();

        long mongoCount = allBackups.stream()
                .filter(b -> BackupType.MONGODB.name().equals(b.getBackupType()))
                .count();

        long fullSystemCount = allBackups.stream()
                .filter(b -> BackupType.FULL_SYSTEM.name().equals(b.getBackupType()))
                .count();

        // Последние 10 бэкапов
        List<BackupResponseDTO> recentBackups = backupHistoryRepository
                .findByOrderByStartTimeDesc(PageRequest.of(0, 10))
                .stream()
                .map(r -> modelMapper.map(r, BackupResponseDTO.class))
                .toList();

        return new BackupStatisticsDTO(
                totalBackups,
                successfulBackups,
                failedBackups,
                totalSizeBytes,
                totalSizeMB,
                totalSizeGB,
                averageDuration,
                recentBackups,
                h2Count,
                mongoCount,
                fullSystemCount
        );
    }

    /*
     * Очистить старые бэкапы.
     *
     * ПРОЦЕСС:
     * - Вызов H2BackupService.cleanupOldBackups()
     * - Вызов MongoBackupService.cleanupOldBackups()
     *
     * @return Общее количество удаленных бэкапов
     */
    public int cleanupOldBackups() {
        log.info("Starting cleanup of old backups");

        int h2Deleted = h2BackupService.cleanupOldBackups();
        int mongoDeleted = mongoBackupService.cleanupOldBackups();
        int totalDeleted = h2Deleted + mongoDeleted;

        log.info("Cleanup completed. Total deleted: {} (H2: {}, MongoDB: {})",
                totalDeleted, h2Deleted, mongoDeleted);

        return totalDeleted;
    }
}
