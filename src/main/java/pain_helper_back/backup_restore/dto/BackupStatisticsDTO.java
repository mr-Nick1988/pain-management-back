package pain_helper_back.backup_restore.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
 * DTO для статистики бэкапов.
 *
 * ИСПОЛЬЗОВАНИЕ: GET /api/backup/statistics
 *
 * СОДЕРЖИМОЕ:
 * - Общая статистика (количество, размеры, длительность)
 * - Разбивка по типам бэкапов
 * - Последние бэкапы
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupStatisticsDTO {
    /**
     * Общее количество бэкапов в истории
     */
    private Long totalBackups;

    /**
     * Количество успешных бэкапов
     */
    private Long successfulBackups;

    /**
     * Количество неудачных бэкапов
     */
    private Long failedBackups;

    /**
     * Общий размер всех бэкапов в байтах
     */
    private Long totalSizeBytes;

    /**
     * Общий размер в мегабайтах (человекочитаемый)
     */
    private String totalSizeMB;

    /**
     * Общий размер в гигабайтах (человекочитаемый)
     */
    private String totalSizeGB;

    /**
     * Средняя длительность бэкапа в миллисекундах
     */
    private Double averageBackupDurationMs;

    /**
     * Последние 10 бэкапов
     */
    private List<BackupResponseDTO> recentBackups;

    /**
     * Количество бэкапов H2
     */
    private Long h2BackupsCount;

    /**
     * Количество бэкапов MongoDB
     */
    private Long mongoBackupsCount;

    /**
     * Количество полных системных бэкапов
     */
    private Long fullSystemBackupsCount;
}
