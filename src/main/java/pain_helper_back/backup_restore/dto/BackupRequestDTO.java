package pain_helper_back.backup_restore.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * DTO для запроса создания бэкапа.
 *
 * ИСПОЛЬЗОВАНИЕ: POST /api/backup/create
 *
 * ВАЛИДАЦИЯ:
 * - backupType: обязательное поле, только допустимые значения
 * - initiatedBy: обязательное поле, ID пользователя
 * - metadata: опциональное поле для дополнительной информации
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BackupRequestDTO {

    /**
     * Тип бэкапа: H2_DATABASE, MONGODB, FULL_SYSTEM
     */
    @NotBlank(message = "Backup type is required")
    @Pattern(regexp = "H2_DATABASE|MONGODB|FULL_SYSTEM",
            message = "Backup type must be H2_DATABASE, MONGODB, or FULL_SYSTEM")
    private String backupType;
    /**
     * Пользователь, инициирующий бэкап (обязательно для ручных бэкапов)
     */
    @NotBlank(message = "Initiated by is required")
    @Size(max = 50, message = "Initiated by must not exceed 50 characters")
    private String initiatedBy;
    /**
     * Дополнительные метаданные (опционально)
     * Например: {"reason": "before_system_update", "version": "2.0"}
     */
    @Size(max = 1000, message = "Metadata must not exceed 1000 characters")
    private String metadata;
}
