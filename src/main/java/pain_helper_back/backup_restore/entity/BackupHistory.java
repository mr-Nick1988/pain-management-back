package pain_helper_back.backup_restore.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import pain_helper_back.backup_restore.enums.BackupStatus;

import java.time.LocalDateTime;


/*
 * История резервных копий системы.
 *
 * НАЗНАЧЕНИЕ:
 * - Хранение метаданных всех бэкапов (H2, MongoDB, полные)
 * - Отслеживание статуса операций бэкапа/восстановления
 * - Управление жизненным циклом бэкапов (политика хранения 30 дней)
 *
 * ХРАНИЛИЩЕ: MongoDB (коллекция "backup_history")
 *
 * ИНДЕКСЫ:
 * - backupType - для быстрого поиска по типу бэкапа
 * - status - для фильтрации успешных/неудачных
 * - startTime - для сортировки по времени
 * - expirationDate - для автоматической очистки старых бэкапов
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "backup_history")
public class BackupHistory {

    @Id
    private String id;
    /**
     * Тип бэкапа: H2_DATABASE, MONGODB, FULL_SYSTEM
     */
    @Indexed
    private String backupType;
    /**
     * Статус операции: IN_PROGRESS, SUCCESS, FAILED
     */
    @Indexed
    private BackupStatus status;

    /**
     * Путь к файлу/директории бэкапа в файловой системе
     */
    private String backupFilePath;

    /**
     * Размер бэкапа в байтах
     */
    private Long fileSizeBytes;

    /**
     * Время начала операции бэкапа
     */
    @Indexed
    private LocalDateTime startTime;

    /**
     * Время завершения операции бэкапа
     */
    private LocalDateTime endTime;

    /**
     * Длительность операции в миллисекундах
     */
    private Long durationMs;

    /**
     * Триггер запуска: SCHEDULED (автоматический), MANUAL (ручной), PRE_OPERATION (перед критической операцией)
     */
    private String trigger;

    /**
     * Пользователь, инициировавший бэкап (для ручных операций)
     */
    private String initiatedBy;

    /**
     * Сообщение об ошибке (если status = FAILED)
     */
    private String errorMessage;

    /**
     * Дополнительные метаданные (JSON строка)
     */
    private String metadata;

    /**
     * Дата истечения срока хранения (для автоматической очистки)
     * По умолчанию: startTime + 30 дней
     */
    @Indexed
    private LocalDateTime expirationDate;
}
