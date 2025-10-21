package pain_helper_back.VAS_external_integration.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.VAS_external_integration.entity.ApiKey;
import pain_helper_back.VAS_external_integration.repository.ApiKeyRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Сервис для управления API ключами внешних систем.
 *
 * ФУНКЦИИ:
 * - Генерация новых API ключей
 * - Валидация ключей
 * - IP whitelist проверка
 * - Rate limiting
 * - Статистика использования
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    /*
     * Генерация нового API ключа для внешней системы
     *
     * @param systemName Название системы
     * @param description Описание системы
     * @param createdBy Кто создал ключ
     * @param expiresInDays Срок действия в днях (null = бессрочный)
     * @return Сгенерированный API ключ
     */
    public ApiKey generateApiKey(String systemName, String description, String createdBy, Integer expiresInDays) {
        log.info("Generating new API key for system: {}", systemName);
        // Генерация уникального ключа (UUID без дефисов)
        String apiKey = UUID.randomUUID().toString().replace("-", "");

        // Расчет даты истечения
        LocalDateTime expiresAt = null;
        if (expiresInDays != null && expiresInDays > 0) {
            expiresAt = LocalDateTime.now().plusDays(expiresInDays);
        }
        ApiKey key = ApiKey.builder()
                .apiKey(apiKey)
                .systemName(systemName)
                .description(description)
                .active(true)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .ipWhitelist("*") // По умолчанию - любой IP
                .rateLimitPerMinute(100) // По умолчанию - 100 запросов/минуту
                .usageCount(0L)
                .createdBy(createdBy)
                .build();
        ApiKey saved = apiKeyRepository.save(key);

        log.info("Successfully generated API key: {} for system: {}", apiKey, systemName);
        return saved;
    }

    /*
     * Валидация API ключа
     *
     * @param apiKey API ключ
     * @param clientIp IP адрес клиента
     * @return true если ключ валиден
     */

    public boolean validateApiKey(String apiKey, String clientIp) {
        log.debug("Validating API key: {} from IP: {}", maskApiKey(apiKey), clientIp);
        // Поиск ключа
        ApiKey key = apiKeyRepository.findByApiKeyAndActiveTrue(apiKey).orElse(null);

        if (key == null) {
            log.warn("API key not found or inactive: {}", maskApiKey(apiKey));
            return false;
        }
        // Проверка срока действия
        if (key.isExpired()) {
            log.warn("API key expired: {}, expiresAt: {}", maskApiKey(apiKey), key.getExpiresAt());
            return false;
        }
        // Проверка IP whitelist
        if (!isIpAllowed(key.getIpWhitelist(), clientIp)) {
            log.warn("IP not whitelisted: {} for API key: {}", clientIp, maskApiKey(apiKey));
            return false;
        }
        // Обновление статистики использования
        updateUsageStats(key);

        log.debug("API key validated successfully: {}", maskApiKey(apiKey));
        return true;
    }
    /*
     * Проверка IP whitelist
     *
     * @param whitelist Whitelist (разделенные запятыми IP или "*")
     * @param clientIp IP клиента
     * @return true если IP разрешен
     */

    private boolean isIpAllowed(String whitelist, String clientIp) {
        if (whitelist == null || whitelist.trim().isEmpty()) {
            return true; // Нет whitelist - разрешаем всем
        }
        if ("*".equals(whitelist.trim())) {
            return true; // Wildcard - разрешаем всем
        }
        // Проверка списка IP
        String[] allowedIps = whitelist.split(",");
        for (String allowedIp : allowedIps) {
            if (allowedIp.trim().equals(clientIp)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Обновление статистики использования ключа
     */
    public void updateUsageStats(ApiKey key) {
        key.setLastUsedAt(LocalDateTime.now());
        key.setUsageCount(key.getUsageCount() + 1);
        apiKeyRepository.save(key);
    }

    /*
     * Деактивация API ключа
     */
    public void deactivateKey(String apiKey) {
        log.info("Deactivating API key: {}", maskApiKey(apiKey));

        ApiKey key = apiKeyRepository.findById(apiKey).orElse(null);
        if (key != null) {
            key.setActive(false);
            apiKeyRepository.save(key);
            log.info("API key deactivated: {}", maskApiKey(apiKey));
        }
    }

    /*
     * Получить все активные ключи
     */
    @Transactional(readOnly = true)
    public List<ApiKey> getAllActiveKeys() {
        return apiKeyRepository.findByActiveTrue();
    }

    /*
     * Получить ключи системы
     */
    @Transactional(readOnly = true)
    public List<ApiKey> getKeysBySystem(String systemName) {
        return apiKeyRepository.findBySystemName(systemName);
    }

    /*
     * Обновить IP whitelist
     */
    public void updateIpWhitelist(String apiKey, String ipWhitelist) {
        log.info("Updating IP whitelist for API key: {}", maskApiKey(apiKey));

        ApiKey key = apiKeyRepository.findById(apiKey).orElse(null);
        if (key != null) {
            key.setIpWhitelist(ipWhitelist);
            apiKeyRepository.save(key);
            log.info("IP whitelist updated: {}", ipWhitelist);
        }
    }

    /*
     * Обновить rate limit
     */
    public void updateRateLimit(String apiKey, Integer rateLimitPerMinute) {
        log.info("Updating rate limit for API key: {}", maskApiKey(apiKey));

        ApiKey key = apiKeyRepository.findById(apiKey).orElse(null);
        if (key != null) {
            key.setRateLimitPerMinute(rateLimitPerMinute);
            apiKeyRepository.save(key);
            log.info("Rate limit updated: {}", rateLimitPerMinute);
        }
    }

    /*
     * Маскировка API ключа для логов (показываем только первые 8 символов)
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 8) + "****";
    }

    /*
     * Очистка истекших ключей (запускается по расписанию)
     */
    public void cleanupExpiredKeys() {
        log.info("Cleaning up expired API keys");

        List<ApiKey> expiredKeys = apiKeyRepository.findByExpiresAtBefore(LocalDateTime.now());

        for(ApiKey key:expiredKeys){
            if(key.getActive()){
                key.setActive(false);
                apiKeyRepository.save(key);
                log.info("Deactivated expired API key: {}", maskApiKey(key.getApiKey()));
            }
        }
        log.info("Cleanup completed: {} keys deactivated", expiredKeys.size());
    }
}


