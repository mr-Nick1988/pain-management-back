package pain_helper_back.enums;

/**
 * Типы источников данных EMR (Electronic Medical Records).
 * Используется для отслеживания происхождения медицинских данных пациента.
 */
public enum EmrSourceType {
    /**
     * Данные получены из FHIR-совместимой системы (HAPI FHIR test server).
     */
    FHIR_SERVER,

    /**
     * Моковые данные, сгенерированные внутри системы для тестирования.
     */
    MOCK_GENERATOR,

    /**
     * Данные из реальной внешней больницы (будущая функциональность).
     */
    EXTERNAL_HOSPITAL,

    /**
     * Данные введены вручную медицинским персоналом.
     */
    MANUAL_ENTRY;

    /*
     * Проверяет, является ли источник автоматическим (не ручной ввод).
     *
     * ЗАЧЕМ: Для автоматических источников нужна дополнительная валидация,
     * так как данные могут быть неполными или некорректными.
     *
     * @return true если источник автоматический (FHIR, MOCK, EXTERNAL_HOSPITAL)
     */
    public boolean isAutomated() {
        return this != MANUAL_ENTRY;
    }

    /*
     * Проверяет, требуется ли дополнительная проверка данных.
     *
     * ЗАЧЕМ: Данные из внешних больниц и моковые данные требуют
     * более тщательной проверки перед использованием в PMA.
     *
     * @return true если требуется дополнительная проверка
     */
    public boolean requiresAdditionalValidation() {
        return this == EXTERNAL_HOSPITAL || this == MOCK_GENERATOR;
    }

    /*
     * Возвращает человекочитаемое описание источника.
     *
     * ПРИМЕНЕНИЕ: Для отображения в UI, логах, отчетах.
     *
     * @return описание источника данных
     */
    public String getDisplayName() {
        return switch (this) {
            case FHIR_SERVER -> "FHIR Server";
            case MOCK_GENERATOR -> "Mock Data Generator";
            case EXTERNAL_HOSPITAL -> "External Hospital System";
            case MANUAL_ENTRY -> "Manual Entry by Staff";
        };
    }
}
