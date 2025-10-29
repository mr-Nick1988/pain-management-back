package pain_helper_back.VAS_external_integration.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequestDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * Парсер для HL7 v2 формата VAS данных.
 *
 * ПОДДЕРЖИВАЕМЫЙ ФОРМАТ (ORU^R01 - Observation Result):
 * MSH|^~\&|VAS_MONITOR|WARD_A|PMA|HOSPITAL|20251020153000||ORU^R01|MSG001|P|2.5
 * PID|1||EMR-12345678^^^MRN||DOE^JOHN||19800515|M
 * OBR|1||VAS001|VAS^Visual Analog Scale^LOCAL|||20251020153000
 * OBX|1|NM|VAS^Pain Level^LOCAL||7|points|0-10|N|||F
 *
 * СЕГМЕНТЫ:
 * - MSH: Message Header (метаданные сообщения)
 * - PID: Patient Identification (идентификация пациента)
 * - OBR: Observation Request (запрос наблюдения)
 * - OBX: Observation Result (результат наблюдения - VAS уровень)
 */
@Component
@Slf4j
public class Hl7VasParser implements VasFormatParser {

    private static final String FIELD_SEPARATOR = "|";
    private static final String COMPONENT_SEPARATOR = "^";
    private static final DateTimeFormatter HL7_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public boolean canParse(String contentType, String rawData) {
        if (contentType != null && contentType.toLowerCase().contains("hl7")) {
            return true;
        }

        // Автоопределение HL7 по структуре (начинается с MSH)
        return rawData.trim().startsWith("MSH|");
    }

    @Override
    public ExternalVasRecordRequestDTO parse(String rawData) throws ParseException {
        log.debug("Parsing HL7 v2 VAS data");

        try {
            String[] segments = rawData.split("\r\n|\n|\r");

            String patientMrn = null;
            Integer vasLevel = null;
            LocalDateTime timestamp = null;
            String deviceId = null;

            for (String segment : segments) {
                String segmentType = segment.substring(0, 3);

                switch (segmentType) {
                    case "MSH":
                        // MSH|^~\&|VAS_MONITOR|WARD_A|...
                        String[] mshFields = segment.split("\\" + FIELD_SEPARATOR);
                        if (mshFields.length > 3) {
                            deviceId = mshFields[3]; // Sending Application
                        }
                        if (mshFields.length > 7) {
                            timestamp = parseHl7DateTime(mshFields[7]);
                        }
                        break;

                    case "PID":
                        // PID|1||EMR-12345678^^^MRN||...
                        String[] pidFields = segment.split("\\" + FIELD_SEPARATOR);
                        if (pidFields.length > 3) {
                            // PID-3: Patient Identifier List
                            String patientId = pidFields[3];
                            // Извлекаем MRN (формат: EMR-12345678^^^MRN)
                            patientMrn = patientId.split("\\^")[0];
                        }
                        break;

                    case "OBX":
                        // OBX|1|NM|VAS^Pain Level^LOCAL||7|points|0-10|N|||F
                        String[] obxFields = segment.split("\\" + FIELD_SEPARATOR);
                        if (obxFields.length > 5) {
                            // OBX-5: Observation Value
                            vasLevel = Integer.parseInt(obxFields[5].trim());
                        }
                        break;
                }
            }

            ExternalVasRecordRequestDTO request = ExternalVasRecordRequestDTO.builder()
                    .patientMrn(patientMrn)
                    .vasLevel(vasLevel)
                    .deviceId(deviceId)
                    .timestamp(timestamp != null ? timestamp : LocalDateTime.now())
                    .source("HL7_V2_IMPORT")
                    .format(ExternalVasRecordRequestDTO.DataFormat.HL7_V2)
                    .build();

            // Валидация
            validateRequest(request);

            log.info("Successfully parsed HL7 v2 VAS record: patientMrn={}, vasLevel={}",
                    request.getPatientMrn(), request.getVasLevel());

            return request;

        } catch (Exception e) {
            log.error("Failed to parse HL7 v2 VAS data: {}", e.getMessage());
            throw new ParseException("Invalid HL7 v2 format: " + e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 3; // Низкий приоритет (специфичный формат)
    }

    private LocalDateTime parseHl7DateTime(String hl7DateTime) {
        try {
            return LocalDateTime.parse(hl7DateTime, HL7_DATETIME);
        } catch (Exception e) {
            log.warn("Failed to parse HL7 datetime: {}, using current time", hl7DateTime);
            return LocalDateTime.now();
        }
    }

    private void validateRequest(ExternalVasRecordRequestDTO request) throws ParseException {
        if (request.getPatientMrn() == null || request.getPatientMrn().trim().isEmpty()) {
            throw new ParseException("Patient MRN not found in HL7 message (PID segment)");
        }

        if (request.getVasLevel() == null) {
            throw new ParseException("VAS level not found in HL7 message (OBX segment)");
        }

        if (request.getVasLevel() < 0 || request.getVasLevel() > 10) {
            throw new ParseException("VAS level must be between 0 and 10, got: " + request.getVasLevel());
        }
    }
}