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
                success = restoreFullSystem(backup, request.getInitiatedBy());
                break;

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
     * Восстановить полную систему (H2 + MongoDB).
     *
     * ПРОЦЕСС:
     * 1. Извлечь ID дочерних бэкапов из metadata
     * 2. Восстановить MongoDB
     * 3. Логировать инструкции для H2 (требует перезапуска)
     *
     * ВАЖНО:
     * - MongoDB восстанавливается онлайн
     * - H2 требует ручного восстановления с перезапуском приложения
     *
     * @param fullBackup История полного бэкапа
     * @param initiatedBy Инициатор восстановления
     * @return true если MongoDB восстановлен успешно
     */
    private boolean restoreFullSystem(BackupHistory fullBackup, String initiatedBy) {
        log.warn("Starting full system restore. InitiatedBy: {}", initiatedBy);
        
        try {
            // Парсинг metadata для получения ID дочерних бэкапов
            String metadata = fullBackup.getMetadata();
            if (metadata == null || metadata.isEmpty()) {
                log.error("Full system backup has no metadata. Cannot restore.");
                return false;
            }
            
            // Простой парсинг JSON (без Jackson для минимальных зависимостей)
            String h2BackupId = extractJsonValue(metadata, "h2_backup_id");
            String mongoBackupId = extractJsonValue(metadata, "mongo_backup_id");
            
            log.info("Full system restore: H2 backup ID: {}, MongoDB backup ID: {}", h2BackupId, mongoBackupId);
            
            boolean mongoSuccess = false;
            boolean h2Success = false;
            
            // 1. Восстановить MongoDB (онлайн)
            if (mongoBackupId != null && !mongoBackupId.isEmpty()) {
                BackupHistory mongoBackup = backupHistoryRepository.findById(mongoBackupId).orElse(null);
                if (mongoBackup != null && mongoBackup.getBackupFilePath() != null) {
                    mongoSuccess = mongoBackupService.restoreFromBackup(
                            mongoBackup.getBackupFilePath(),
                            initiatedBy
                    );
                    log.info("MongoDB restore result: {}", mongoSuccess ? "SUCCESS" : "FAILED");
                } else {
                    log.error("MongoDB backup not found or has no file path: {}", mongoBackupId);
                }
            }
            
            // 2. Логировать инструкции для H2 (требует перезапуска)
            if (h2BackupId != null && !h2BackupId.isEmpty()) {
                BackupHistory h2Backup = backupHistoryRepository.findById(h2BackupId).orElse(null);
                if (h2Backup != null && h2Backup.getBackupFilePath() != null) {
                    h2Success = h2BackupService.restoreFromBackup(
                            h2Backup.getBackupFilePath(),
                            initiatedBy
                    );
                    log.info("H2 restore instructions logged: {}", h2Backup.getBackupFilePath());
                } else {
                    log.error("H2 backup not found or has no file path: {}", h2BackupId);
                }
            }
            
            // Считаем успешным, если хотя бы MongoDB восстановлен
            // H2 всегда возвращает true (только логирует инструкции)
            return mongoSuccess;
            
        } catch (Exception e) {
            log.error("Full system restore failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /*
     * Извлечь значение из простого JSON.
     * Пример: {"key":"value"} -> extractJsonValue(json, "key") -> "value"
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) {
            return null;
        }
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return null;
        }
        return json.substring(startIndex, endIndex);
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
