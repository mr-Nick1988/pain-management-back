package pain_helper_back.backup_restore.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pain_helper_back.backup_restore.enums.BackupStatus;

import java.time.LocalDateTime;

/*
 * DTO для ответа с информацией о бэкапе.
 *
 * ИСПОЛЬЗОВАНИЕ:
 * - GET /api/backup/history
 * - GET /api/backup/history/{id}
 * - POST /api/backup/create (в ответе)
 *
 * ДОПОЛНИТЕЛЬНЫЕ ПОЛЯ:
 * - fileSizeMB: человекочитаемый размер в мегабайтах
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupResponseDTO {
    /**
     * ID бэкапа в MongoDB
     */
    private String id;

    /**
     * Тип бэкапа
     */
    private String backupType;

    /**
     * Статус операции
     */
    private BackupStatus status;

    /**
     * Путь к файлу бэкапа
     */
    private String backupFilePath;

    /**
     * Размер в байтах
     */
    private Long fileSizeBytes;

    /**
     * Размер в мегабайтах (человекочитаемый формат)
     */
    private String fileSizeMB;

    /**
     * Время начала
     */
    private LocalDateTime startTime;

    /**
     * Время завершения
     */
    private LocalDateTime endTime;

    /**
     * Длительность в миллисекундах
     */
    private Long durationMs;

    /**
     * Триггер запуска
     */
    private String trigger;

    /**
     * Инициатор
     */
    private String initiatedBy;

    /**
     * Сообщение об ошибке (если есть)
     */
    private String errorMessage;

    /**
     * Дата истечения срока хранения
     */
    private LocalDateTime expirationDate;
}
