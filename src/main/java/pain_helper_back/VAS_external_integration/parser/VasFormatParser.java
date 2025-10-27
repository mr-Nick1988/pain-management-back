package pain_helper_back.VAS_external_integration.parser;


import pain_helper_back.VAS_external_integration.dto.ExternalVasRecordRequestDTO;

/*
 * Интерфейс для парсеров различных форматов VAS данных.
 *
 * Каждый формат (JSON, XML, HL7, FHIR, CSV) имеет свою реализацию.
 */
public interface VasFormatParser {
    /*
     * Проверяет, может ли парсер обработать данный формат
     *
     * @param contentType Content-Type из HTTP заголовка
     * @param rawData Сырые данные
     * @return true если парсер поддерживает этот формат
     */
    boolean canParse(String contentType, String rawData);
    /*
     * Парсит данные в унифицированный DTO
     *
     * @param rawData Сырые данные в строковом формате
     * @return Унифицированный VAS record
     * @throws ParseException если данные невалидны
     */
    ExternalVasRecordRequestDTO parse(String rawData)throws ParseException;
    /*
     * Возвращает приоритет парсера (чем меньше, тем выше)
     * Используется когда несколько парсеров могут обработать формат
     */
    int getPriority();

    class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
