package pain_helper_back.VAS_external_integration.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequestDTO;
import pain_helper_back.VAS_external_integration.parser.VasFormatParser;

import java.util.Comparator;
import java.util.List;

/*
 * Фабрика для автоматического выбора парсера по формату данных.
 *
 * ЛОГИКА:
 * 1. Проверяет Content-Type заголовок
 * 2. Автоматически определяет формат по структуре данных
 * 3. Выбирает парсер с наивысшим приоритетом
 * 4. Парсит данные в унифицированный DTO
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VasParserFactory {

    private final List<VasFormatParser> parsers;

    /*
     * Автоматический парсинг VAS данных
     *
     * @param contentType Content-Type из HTTP заголовка
     * @param rawData Сырые данные
     * @return Распарсенный VAS record
     * @throws VasFormatParser.ParseException если формат не поддерживается
     */
    public ExternalVasRecordRequestDTO parse(String contentType, String rawData)
            throws VasFormatParser.ParseException {

        log.debug("Parsing VAS data, contentType: {}, dataLength: {}", contentType, rawData.length());
        // Находим все подходящие парсеры
        List<VasFormatParser> suitableParsers = parsers.stream()
                .filter(parser -> parser.canParse(contentType, rawData))
                .sorted(Comparator.comparingInt(VasFormatParser::getPriority))
                .toList();
        if (suitableParsers.isEmpty()) {
            log.error("No suitable parser found for contentType: {}", contentType);
            throw new VasFormatParser.ParseException(
                    "Unsupported data format. Supported formats: JSON, XML, HL7 v2, FHIR, CSV");
        }

        // Используем парсер с наивысшим приоритетом
        VasFormatParser selectedParser = suitableParsers.get(0);
        log.info("Selected parser: {}", selectedParser.getClass().getSimpleName());

        // Парсим данные
        ExternalVasRecordRequestDTO result = selectedParser.parse(rawData);

        log.info("Successfully parsed VAS data using {}: patientMrn={}, vasLevel={}, format={}",
                selectedParser.getClass().getSimpleName(),
                result.getPatientMrn(),
                result.getVasLevel(),
                result.getFormat());

        return result;
    }
    /*
     * Получить список поддерживаемых форматов
     */
    public List<String> getSupportedFormats() {
        return parsers.stream()
                .map(parser -> parser.getClass().getSimpleName().replace("VasParser", ""))
                .toList();
    }
}
