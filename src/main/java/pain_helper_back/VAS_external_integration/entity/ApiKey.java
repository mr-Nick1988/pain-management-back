package pain_helper_back.VAS_external_integration.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * Entity для хранения API ключей внешних систем.
 *
 * НАЗНАЧЕНИЕ:
 * - Аутентификация внешних систем (мониторы, планшеты, EMR)
 * - Контроль доступа по IP whitelist
 * - Rate limiting (ограничение запросов)
 * - Аудит использования API
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @Column(name = "api_key", length = 64, nullable = false, unique = true)
    private String apiKey;
    /*
     * Название внешней системы
     * Примеры: "VAS Monitor Ward A", "Tablet Nurse Station", "Hospital EMR"
     */
    @Column(name = "system_name", length = 255, nullable = false)
    private String systemName;
    /*
     * Описание системы (опционально)
     */
    @Column(name = "description", length = 500)
    private String description;
    /*
     * Активен ли ключ
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    /*
     * Дата создания ключа
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    /*
     * Дата истечения ключа (null = бессрочный)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    /*
     * IP whitelist (разделенные запятыми)
     * Примеры: "192.168.1.100,192.168.1.101" или "*" для любого IP
     */
    @Column(name = "ip_whitelist", length = 500)
    private String ipWhitelist;
    /*
     * Rate limit (запросов в минуту)
     * null = без ограничений
     */
    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute;
    /*
     * Последнее использование ключа
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    /*
     * Количество использований
     */
    @Column(name = "usage_count")
    private Long usageCount = 0L;
    /*
     * Кто создал ключ (для аудита)
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (usageCount == null) {
            usageCount = 0L;
        }
    }
    /*
     * Проверяет, истек ли срок действия ключа
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    /*
     * Проверяет, валиден ли ключ (активен и не истек)
     */
    public boolean isValid() {
        return active && !isExpired();
    }
}

