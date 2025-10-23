package pain_helper_back.VAS_external_integration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.VAS_external_integration.entity.ApiKey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
 * Repository для работы с API ключами
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

    /**
     * Найти активный ключ
     */
    Optional<ApiKey> findByApiKeyAndActiveTrue(String apiKey);

    /**
     * Проверить существование активного ключа
     */
    boolean existsByApiKeyAndActiveTrue(String apiKey);

    /**
     * Найти все ключи системы
     */
    List<ApiKey> findBySystemName(String systemName);

    /**
     * Найти все активные ключи
     */
    List<ApiKey> findByActiveTrue();

    /**
     * Найти истекшие ключи
     */
    List<ApiKey> findByExpiresAtBefore(LocalDateTime dateTime);
}