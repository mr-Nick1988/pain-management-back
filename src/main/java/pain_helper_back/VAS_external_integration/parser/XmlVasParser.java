package pain_helper_back.VAS_external_integration.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * Парсер для XML формата VAS данных.
 *
 * ПОДДЕРЖИВАЕМЫЙ ФОРМАТ:
 * <?xml version="1.0" encoding="UTF-8"?>
 * <VASRecord>
 *   <PatientMRN>EMR-12345678</PatientMRN>
 *   <VASLevel>7</VASLevel>
 *   <DeviceID>MONITOR-001</DeviceID>
 *   <Location>Ward A, Bed 12</Location>
 *   <Timestamp>2025-10-20T15:30:00</Timestamp>
 *   <Notes>Patient reports sharp pain</Notes>
 * </VASRecord>
 */
@Component
@Slf4j
public class XmlVasParser implements VasFormatParser {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public boolean canParse(String contentType, String rawData) {
        if (contentType != null && contentType.toLowerCase().contains("xml")) {
            return true;
        }

        // Автоопределение XML по структуре
        String trimmed = rawData.trim();
        return trimmed.startsWith("<?xml") || trimmed.startsWith("<VASRecord");
    }

    @Override
    public ExternalVasRecordRequest parse(String rawData) throws ParseException {
        log.debug("Parsing XML VAS data");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(rawData.getBytes()));

            Element root = doc.getDocumentElement();

            ExternalVasRecordRequest request = ExternalVasRecordRequest.builder()
                    .patientMrn(getElementText(root, "PatientMRN"))
                    .vasLevel(getElementInt(root, "VASLevel"))
                    .deviceId(getElementText(root, "DeviceID"))
                    .location(getElementText(root, "Location"))
                    .timestamp(getElementDateTime(root, "Timestamp"))
                    .notes(getElementText(root, "Notes"))
                    .source("XML_IMPORT")
                    .format(ExternalVasRecordRequest.DataFormat.XML)
                    .build();

            // Установка timestamp если отсутствует
            if (request.getTimestamp() == null) {
                request.setTimestamp(LocalDateTime.now());
            }

            // Валидация
            validateRequest(request);

            log.info("Successfully parsed XML VAS record: patientMrn={}, vasLevel={}",
                    request.getPatientMrn(), request.getVasLevel());

            return request;

        } catch (Exception e) {
            log.error("Failed to parse XML VAS data: {}", e.getMessage());
            throw new ParseException("Invalid XML format: " + e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 2; // Средний приоритет
    }

    private String getElementText(Element parent, String tagName) {
        try {
            return parent.getElementsByTagName(tagName).item(0).getTextContent();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getElementInt(Element parent, String tagName) throws ParseException {
        String text = getElementText(parent, tagName);
        if (text == null) return null;

        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid integer value for " + tagName + ": " + text);
        }
    }

    private LocalDateTime getElementDateTime(Element parent, String tagName) {
        String text = getElementText(parent, tagName);
        if (text == null) return null;

        try {
            return LocalDateTime.parse(text.trim(), ISO_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}, using current time", text);
            return null;
        }
    }

    private void validateRequest(ExternalVasRecordRequest request) throws ParseException {
        if (request.getPatientMrn() == null || request.getPatientMrn().trim().isEmpty()) {
            throw new ParseException("PatientMRN is required");
        }

        if (request.getVasLevel() == null) {
            throw new ParseException("VASLevel is required");
        }

        if (request.getVasLevel() < 0 || request.getVasLevel() > 10) {
            throw new ParseException("VASLevel must be between 0 and 10, got: " + request.getVasLevel());
        }
    }
}