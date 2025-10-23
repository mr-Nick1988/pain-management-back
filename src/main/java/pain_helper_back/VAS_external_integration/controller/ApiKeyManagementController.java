package pain_helper_back.VAS_external_integration.controller;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.VAS_external_integration.entity.ApiKey;
import pain_helper_back.VAS_external_integration.service.ApiKeyService;

import java.util.List;
import java.util.Map;

/*
 * REST API для управления API ключами (только для админов).
 *
 * ЭНДПОИНТЫ:
 * - POST /api/admin/api-keys/generate - создать новый ключ
 * - GET /api/admin/api-keys - получить все ключи
 * - DELETE /api/admin/api-keys/{apiKey} - деактивировать ключ
 * - PUT /api/admin/api-keys/{apiKey}/whitelist - обновить IP whitelist
 * - PUT /api/admin/api-keys/{apiKey}/rate-limit - обновить rate limit
 */
@RestController
@RequestMapping("/api/admin/api-keys")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
// TODO: Добавить @PreAuthorize("hasRole('ADMIN')") после внедрения Spring Security
public class ApiKeyManagementController {

    private final ApiKeyService apiKeyService;

    /**
     * Создать новый API ключ
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateApiKey(
            @RequestParam String systemName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer expiresInDays,
            @RequestParam(defaultValue = "admin") String createdBy) {

        log.info("POST /api/admin/api-keys/generate - systemName: {}", systemName);

        try {
            ApiKey apiKey = apiKeyService.generateApiKey(systemName, description, createdBy, expiresInDays);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "API key generated successfully",
                    "apiKey", apiKey.getApiKey(),
                    "systemName", apiKey.getSystemName(),
                    "expiresAt", apiKey.getExpiresAt() != null ? apiKey.getExpiresAt().toString() : "never",
                    "ipWhitelist", apiKey.getIpWhitelist(),
                    "rateLimitPerMinute", apiKey.getRateLimitPerMinute()
            ));

        } catch (Exception e) {
            log.error("Failed to generate API key: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to generate API key", "message", e.getMessage()));
        }
    }

    /**
     * Получить все активные ключи
     */
    @GetMapping
    public ResponseEntity<?> getAllKeys() {
        log.info("GET /api/admin/api-keys");

        try {
            List<ApiKey> keys = apiKeyService.getAllActiveKeys();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "total", keys.size(),
                    "keys", keys
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Деактивировать API ключ
     */
    @DeleteMapping("/{apiKey}")
    public ResponseEntity<?> deactivateKey(@PathVariable String apiKey) {
        log.info("DELETE /api/admin/api-keys/{}", apiKey);

        try {
            apiKeyService.deactivateKey(apiKey);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "API key deactivated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Обновить IP whitelist
     */
    @PutMapping("/{apiKey}/whitelist")
    public ResponseEntity<?> updateWhitelist(
            @PathVariable String apiKey,
            @RequestParam String ipWhitelist) {

        log.info("PUT /api/admin/api-keys/{}/whitelist", apiKey);

        try {
            apiKeyService.updateIpWhitelist(apiKey, ipWhitelist);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "IP whitelist updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Обновить rate limit
     */
    @PutMapping("/{apiKey}/rate-limit")
    public ResponseEntity<?> updateRateLimit(
            @PathVariable String apiKey,
            @RequestParam Integer rateLimitPerMinute) {

        log.info("PUT /api/admin/api-keys/{}/rate-limit", apiKey);

        try {
            apiKeyService.updateRateLimit(apiKey, rateLimitPerMinute);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Rate limit updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}