package pain_helper_back.VAS_external_integration.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequestDTO;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordResponseDTO;
import pain_helper_back.VAS_external_integration.dto.VasMonitorStatsDTO;
import pain_helper_back.VAS_external_integration.parser.VasFormatParser;
import pain_helper_back.VAS_external_integration.service.ApiKeyService;
import pain_helper_back.VAS_external_integration.service.ExternalVasIntegrationService;
import pain_helper_back.VAS_external_integration.service.VasParserFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/external/vas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ExternalVasIntegrationController {

    private final ApiKeyService apiKeyService;
    private final VasParserFactory parserFactory;
    private final ExternalVasIntegrationService integrationService;

    @PostMapping("/record")
    public ResponseEntity<?> recordVas(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestBody String rawData,
            HttpServletRequest request) {

        log.info("POST /api/external/vas/record - apiKey: {}, contentType: {}",
                maskApiKey(apiKey), contentType);

        try {
            // Валидация API ключа
            if (!apiKeyService.validateApiKey(apiKey, getClientIp(request))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid API key or IP not whitelisted"));
            }

            // Парсинг данных
            ExternalVasRecordRequestDTO vas = parserFactory.parse(contentType, rawData);

            // Обработка VAS
            Long vasId = integrationService.processExternalVasRecord(vas);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "status", "success",
                            "vasId", vasId,
                            "patientMrn", vas.getPatientMrn(),
                            "vasLevel", vas.getVasLevel(),
                            "format", vas.getFormat().toString()
                    ));

        } catch (VasFormatParser.ParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Parse error", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> batchImport(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody String csvData,
            HttpServletRequest request) {

        try {
            if (!apiKeyService.validateApiKey(apiKey, getClientIp(request))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid API key"));
            }

            Map<String, Object> result = integrationService.processBatchVasRecords(csvData);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "module", "External VAS Integration",
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    /**
     * Получить список VAS записей с фильтрами (для External VAS Monitor).
     * 
     * ИСПОЛЬЗУЕТСЯ В:
     * - External VAS Monitor (Frontend)
     * - Real-time мониторинг VAS данных с внешних устройств
     * 
     * ФИЛЬТРЫ:
     * - deviceId: фильтр по ID устройства (MONITOR-001, TABLET-WARD-A)
     * - location: фильтр по локации (Ward A, ICU-1)
     * - timeRange: временной диапазон (1h, 6h, 24h, 7d)
     * - vasLevelMin/Max: диапазон уровня боли (0-10)
     * 
     * @return Список VAS записей с данными пациентов
     */
    @GetMapping("/records")
    public ResponseEntity<List<ExternalVasRecordResponseDTO>> getRecords(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) Integer vasLevelMin,
            @RequestParam(required = false) Integer vasLevelMax) {

        log.info("GET /api/external/vas/records - deviceId={}, location={}, timeRange={}, vasRange={}-{}",
                deviceId, location, timeRange, vasLevelMin, vasLevelMax);

        List<ExternalVasRecordResponseDTO> records = integrationService.getVasRecords(
                deviceId, location, timeRange, vasLevelMin, vasLevelMax);

        log.info("Returning {} VAS records", records.size());
        return ResponseEntity.ok(records);
    }

    /**
     * Получить статистику по VAS записям за сегодня.
     * 
     * ИСПОЛЬЗУЕТСЯ В:
     * - External VAS Monitor Dashboard (Frontend)
     * - Отображение карточек статистики
     * 
     * МЕТРИКИ:
     * - totalRecordsToday: общее количество записей за сегодня
     * - averageVas: средний уровень боли
     * - highPainAlerts: количество записей с VAS >= 7
     * - activeDevices: количество активных устройств (DISTINCT)
     * 
     * @return Статистика VAS записей
     */
    @GetMapping("/stats")
    public ResponseEntity<VasMonitorStatsDTO> getStats() {
        log.info("GET /api/external/vas/stats");

        VasMonitorStatsDTO stats = integrationService.getVasStatistics();

        log.info("VAS Statistics: total={}, avg={}, highPain={}, devices={}",
                stats.getTotalRecordsToday(), stats.getAverageVas(),
                stats.getHighPainAlerts(), stats.getActiveDevices());

        return ResponseEntity.ok(stats);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String maskApiKey(String apiKey) {
        return apiKey != null && apiKey.length() >= 8 ? apiKey.substring(0, 8) + "****" : "****";
    }
}