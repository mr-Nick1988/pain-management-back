package pain_helper_back.backup_restore.dto;


import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * DTO для запроса восстановления из бэкапа.
 *
 * ИСПОЛЬЗОВАНИЕ: POST /api/backup/restore
 *
 * ВАЖНО:
 * - Восстановление H2 требует перезапуска приложения
 * - Восстановление MongoDB выполняется онлайн с флагом --drop
 * - Требуется подтверждение (confirmed = true)
 *
 * ВАЛИДАЦИЯ:
 * - backupId: обязательное поле, ID бэкапа из истории
 * - initiatedBy: обязательное поле, ID администратора
 * - confirmed: должно быть true для выполнения операции
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RestoreRequestDTO {
    /**
     * ID бэкапа для восстановления (из BackupHistory)
     */
    @NotBlank(message = "Backup ID is required")
    private String backupId;
    /**
     * Пользователь, инициирующий восстановление
     */
    @NotBlank(message = "Initiated by is required")
    @Size(max = 50, message = "Initiated by must not exceed 50 characters")
    private String initiatedBy;
    /*
     * Подтверждение восстановления (требуется true)
     *
     * ЗАЩИТА: Предотвращает случайное восстановление
     */
    @AssertTrue(message = "Restore operation must be confirmed (confirmed = true)")
    private boolean confirmed;
}
