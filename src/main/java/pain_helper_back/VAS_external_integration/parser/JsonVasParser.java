package pain_helper_back.VAS_external_integration.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequest;

import java.time.LocalDateTime;

/*
 * Парсер для JSON формата VAS данных.
 *
 * ПОДДЕРЖИВАЕМЫЕ ФОРМАТЫ:
 * 1. Стандартный JSON:
 *    {"patientMrn": "EMR-123", "vasLevel": 7, "deviceId": "MONITOR-001"}
 *
 * 2. Nested JSON:
 *    {"patient": {"mrn": "EMR-123"}, "observation": {"painLevel": 7}}
 *
 * 3. Camel/Snake case:
 *    {"patient_mrn": "EMR-123", "vas_level": 7}
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JsonVasParser implements VasFormatParser {

    private final ObjectMapper objectMapper;

    @Override
    public boolean canParse(String contentType, String rawData) {
        if (contentType != null && contentType.toLowerCase().contains("json")) {
            return true;
        }

        // Автоопределение JSON по структуре
        String trimmed = rawData.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    @Override
    public ExternalVasRecordRequest parse(String rawData) throws ParseException {
        log.debug("Parsing JSON VAS data: {}", rawData);

        try {
            // Прямой парсинг в DTO
            ExternalVasRecordRequest request = objectMapper.readValue(rawData, ExternalVasRecordRequest.class);

            // Установка формата
            request.setFormat(ExternalVasRecordRequest.DataFormat.JSON);

            // Установка timestamp если отсутствует
            if (request.getTimestamp() == null) {
                request.setTimestamp(LocalDateTime.now());
            }

            // Валидация обязательных полей
            validateRequest(request);

            log.info("Successfully parsed JSON VAS record: patientMrn={}, vasLevel={}",
                    request.getPatientMrn(), request.getVasLevel());

            return request;

        } catch (Exception e) {
            log.error("Failed to parse JSON VAS data: {}", e.getMessage());
            throw new ParseException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 1; // Высокий приоритет (JSON - стандарт)
    }

    private void validateRequest(ExternalVasRecordRequest request) throws ParseException {
        if (request.getPatientMrn() == null || request.getPatientMrn().trim().isEmpty()) {
            throw new ParseException("patientMrn is required");
        }

        if (request.getVasLevel() == null) {
            throw new ParseException("vasLevel is required");
        }

        if (request.getVasLevel() < 0 || request.getVasLevel() > 10) {
            throw new ParseException("vasLevel must be between 0 and 10, got: " + request.getVasLevel());
        }
    }
}