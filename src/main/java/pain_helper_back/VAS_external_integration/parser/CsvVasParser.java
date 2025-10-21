package pain_helper_back.VAS_external_integration.parser;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
 * Парсер для CSV формата VAS данных (batch импорт).
 *
 * ПОДДЕРЖИВАЕМЫЙ ФОРМАТ:
 * PatientMRN,VASLevel,DeviceID,Location,Timestamp,Notes
 * EMR-12345678,7,MONITOR-001,Ward A Bed 12,2025-10-20T15:30:00,Sharp pain
 * EMR-87654321,5,MONITOR-002,Ward B Bed 5,2025-10-20T15:31:00,Dull ache
 *
 * ОСОБЕННОСТИ:
 * - Первая строка - заголовки (обязательно)
 * - Разделитель: запятая (,)
 * - Поддержка кавычек для значений с запятыми
 * - Возвращает ПЕРВУЮ запись (для single record endpoint)
 * - Для batch импорта используйте parseMultiple()
 */
@Component
@Slf4j
public class CsvVasParser implements VasFormatParser {

    private static final String DELIMITER = ",";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public boolean canParse(String contentType, String rawData) {
        if (contentType != null && contentType.toLowerCase().contains("csv")) {
            return true;
        }

        // Автоопределение CSV по структуре (заголовки)
        String firstLine = rawData.split("\n")[0].toLowerCase();
        return firstLine.contains("patientmrn") && firstLine.contains("vaslevel");
    }

    @Override
    public ExternalVasRecordRequest parse(String rawData) throws ParseException {
        log.debug("Parsing CSV VAS data (single record)");

        List<ExternalVasRecordRequest> records = parseMultiple(rawData);

        if (records.isEmpty()) {
            throw new ParseException("No valid VAS records found in CSV");
        }

        // Возвращаем первую запись
        return records.get(0);
    }

    /*
     * Парсит несколько записей из CSV (для batch импорта)
     *
     * @param rawData CSV данные
     * @return Список VAS записей
     */
    public List<ExternalVasRecordRequest> parseMultiple(String rawData) throws ParseException {
        log.debug("Parsing CSV VAS data (multiple records)");

        List<ExternalVasRecordRequest> records = new ArrayList<>();

        try {
            String[] lines = rawData.split("\n");

            if (lines.length < 2) {
                throw new ParseException("CSV must have at least 2 lines (header + data)");
            }

            // Парсинг заголовков
            String[] headers = parseCsvLine(lines[0]);
            int patientMrnIndex = findColumnIndex(headers, "PatientMRN");
            int vasLevelIndex = findColumnIndex(headers, "VASLevel");
            int deviceIdIndex = findColumnIndex(headers, "DeviceID");
            int locationIndex = findColumnIndex(headers, "Location");
            int timestampIndex = findColumnIndex(headers, "Timestamp");
            int notesIndex = findColumnIndex(headers, "Notes");

            // Парсинг данных
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                try {
                    String[] values = parseCsvLine(line);

                    ExternalVasRecordRequest request = ExternalVasRecordRequest.builder()
                            .patientMrn(getValue(values, patientMrnIndex))
                            .vasLevel(getIntValue(values, vasLevelIndex))
                            .deviceId(getValue(values, deviceIdIndex))
                            .location(getValue(values, locationIndex))
                            .timestamp(getDateTimeValue(values, timestampIndex))
                            .notes(getValue(values, notesIndex))
                            .source("CSV_IMPORT")
                            .format(ExternalVasRecordRequest.DataFormat.CSV)
                            .build();

                    // Установка timestamp если отсутствует
                    if (request.getTimestamp() == null) {
                        request.setTimestamp(LocalDateTime.now());
                    }

                    // Валидация
                    validateRequest(request);

                    records.add(request);

                } catch (Exception e) {
                    log.warn("Failed to parse CSV line {}: {}", i + 1, e.getMessage());
                    // Продолжаем парсинг остальных строк
                }
            }

            log.info("Successfully parsed {} VAS records from CSV", records.size());
            return records;

        } catch (Exception e) {
            log.error("Failed to parse CSV VAS data: {}", e.getMessage());
            throw new ParseException("Invalid CSV format: " + e.getMessage(), e);
        }
    }

    @Override
    public int getPriority() {
        return 4; // Низкий приоритет (batch формат)
    }

    /**
     * Парсит CSV строку с учетом кавычек
     */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    /**
     * Находит индекс колонки по имени (case-insensitive)
     */
    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1; // Колонка не найдена
    }

    private String getValue(String[] values, int index) {
        if (index < 0 || index >= values.length) return null;
        String value = values[index].trim();
        return value.isEmpty() ? null : value;
    }

    private Integer getIntValue(String[] values, int index) throws ParseException {
        String value = getValue(values, index);
        if (value == null) return null;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid integer value: " + value);
        }
    }

    private LocalDateTime getDateTimeValue(String[] values, int index) {
        String value = getValue(values, index);
        if (value == null) return null;

        try {
            return LocalDateTime.parse(value, ISO_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}, using current time", value);
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