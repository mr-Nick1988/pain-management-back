package pain_helper_back.VAS_external_integration.parser;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequestDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * Парсер для FHIR R4 формата VAS данных.
 *
 * ПОДДЕРЖИВАЕМЫЙ ФОРМАТ (FHIR Observation):
 * {
 *   "resourceType": "Observation",
 *   "status": "final",
 *   "code": {
 *     "coding": [{
 *       "system": "http://loinc.org",
 *       "code": "38208-5",
 *       "display": "Pain severity"
 *     }]
 *   },
 *   "subject": {
 *     "reference": "Patient/EMR-12345678"
 *   },
 *   "valueInteger": 7,
 *   "effectiveDateTime": "2025-10-20T15:30:00Z",
 *   "device": {
 *     "display": "VAS Monitor Ward A"
 *   }
 * }
 *
 * LOINC КОДЫ ДЛЯ БОЛИ:
 * - 38208-5: Pain severity
 * - 72514-3: Pain severity - 0-10 verbal numeric rating
 * - 38221-8: Pain severity Wong-Baker FACES Scale
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FhirVasParser implements VasFormatParser {
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter FHIR_DATETIME = DateTimeFormatter.ISO_DATE_TIME;

    // LOINC коды для боли
    private static final String[] PAIN_LOINC_CODES = {
            "38208-5",  // Pain severity
            "72514-3",  // Pain severity - 0-10 verbal numeric rating
            "38221-8"   // Pain severity Wong-Baker FACES Scale
    };

    @Override
    public boolean canParse(String contentType, String rawData) {
        if (contentType != null && contentType.toLowerCase().contains("fhir")) {
            return true;
        }

        // Автоопределение FHIR по структуре
        try {
            JsonNode root = objectMapper.readTree(rawData);
            return root.has("resourceType") &&
                    "Observation".equals(root.get("resourceType").asText());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ExternalVasRecordRequestDTO parse(String rawData) throws ParseException {
        log.debug("Parsing FHIR VAS data");

        try {
            JsonNode root = objectMapper.readTree(rawData);

            // Валидация resourceType
            if (!"Observation".equals(root.path("resourceType").asText())) {
                throw new ParseException("Expected resourceType 'Observation', got: " +
                        root.path("resourceType").asText());
            }

            // Проверка, что это VAS observation (по LOINC коду)
            if (!isPainObservation(root)) {
                throw new ParseException("Not a pain/VAS observation (LOINC code not found)");
            }

            // Извлечение данных
            String patientMrn = extractPatientMrn(root);
            Integer vasLevel = extractVasLevel(root);
            LocalDateTime timestamp = extractTimestamp(root);
            String deviceId = extractDeviceId(root);

            ExternalVasRecordRequestDTO request = ExternalVasRecordRequestDTO.builder()
                    .patientMrn(patientMrn)
                    .vasLevel(vasLevel)
                    .deviceId(deviceId)
                    .timestamp(timestamp != null ? timestamp : LocalDateTime.now())
                    .source("FHIR_R4_IMPORT")
                    .format(ExternalVasRecordRequestDTO.DataFormat.FHIR)
                    .build();

            // Валидация
            validateRequest(request);

            log.info("Successfully parsed FHIR VAS record: patientMrn={}, vasLevel={}",
                    request.getPatientMrn(), request.getVasLevel());

            return request;

        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse FHIR VAS data: {}", e.getMessage());
            throw new ParseException("Invalid FHIR format: " + e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 1; // Высокий приоритет (FHIR - современный стандарт)
    }

    /**
     * Проверяет, является ли observation записью о боли
     */
    private boolean isPainObservation(JsonNode root) {
        JsonNode coding = root.path("code").path("coding");
        if (!coding.isArray()) return false;

        for (JsonNode code : coding) {
            String loincCode = code.path("code").asText();
            for (String painCode : PAIN_LOINC_CODES) {
                if (painCode.equals(loincCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Извлекает MRN пациента из subject.reference
     * Формат: "Patient/EMR-12345678" → "EMR-12345678"
     */
    private String extractPatientMrn(JsonNode root) throws ParseException {
        String reference = root.path("subject").path("reference").asText();
        if (reference.isEmpty()) {
            throw new ParseException("Patient reference not found in FHIR Observation");
        }

        // Извлекаем ID из "Patient/EMR-12345678"
        if (reference.contains("/")) {
            return reference.substring(reference.lastIndexOf("/") + 1);
        }
        return reference;
    }

    /**
     * Извлекает уровень боли из valueInteger или valueQuantity
     */
    private Integer extractVasLevel(JsonNode root) throws ParseException {
        // Вариант 1: valueInteger
        if (root.has("valueInteger")) {
            return root.get("valueInteger").asInt();
        }

        // Вариант 2: valueQuantity.value
        if (root.has("valueQuantity")) {
            JsonNode quantity = root.get("valueQuantity");
            if (quantity.has("value")) {
                return quantity.get("value").asInt();
            }
        }

        throw new ParseException("VAS level not found (expected valueInteger or valueQuantity)");
    }

    /**
     * Извлекает временную метку из effectiveDateTime
     */
    private LocalDateTime extractTimestamp(JsonNode root) {
        String dateTimeStr = root.path("effectiveDateTime").asText();
        if (dateTimeStr.isEmpty()) {
            return null;
        }

        try {
            // FHIR использует ISO 8601 с timezone
            // Убираем timezone для LocalDateTime
            if (dateTimeStr.contains("Z")) {
                dateTimeStr = dateTimeStr.replace("Z", "");
            } else if (dateTimeStr.contains("+")) {
                dateTimeStr = dateTimeStr.substring(0, dateTimeStr.indexOf("+"));
            }
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            log.warn("Failed to parse FHIR timestamp: {}, using current time", dateTimeStr);
            return null;
        }
    }

    /*
     * Извлекает ID устройства из device.display
     */
    private String extractDeviceId(JsonNode root) {
        return root.path("device").path("display").asText(null);
    }

    private void validateRequest(ExternalVasRecordRequestDTO request) throws ParseException {
        if (request.getPatientMrn() == null || request.getPatientMrn().trim().isEmpty()) {
            throw new ParseException("Patient MRN not found in FHIR Observation");
        }

        if (request.getVasLevel() == null) {
            throw new ParseException("VAS level not found in FHIR Observation");
        }

        if (request.getVasLevel() < 0 || request.getVasLevel() > 10) {
            throw new ParseException("VAS level must be between 0 and 10, got: " + request.getVasLevel());
        }
    }
}
