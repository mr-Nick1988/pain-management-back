package pain_helper_back.enums;

/**
 * typeы источников данных EMR (Electronic Medical Records).
 * Используется for отслеживания происхождения медицинских данных patient.
 */
public enum EmrSourceType {
    /**
     * data получены from FHIR-совместимой системы (HAPI FHIR test server).
     */
    FHIR_SERVER,

    /**
     * Моковые data, сгенерированные внутри системы for тестирования.
     */
    MOCK_GENERATOR,

    /**
     * data from реальной внешней больницы (будущая функциональность).
     */
    EXTERNAL_HOSPITAL,

    /**
     * data введены вручную медицинским персоналом.
     */
    MANUAL_ENTRY;

    /*
     * Проверяет, является ли источник автоматическим (не ручной ввод).
     *
     * ЗАЧЕМ: for автоматических источников нужна дополнительная валидация,
     * так как data могут быть неполными or некорректными.
     *
     * @return true if источник автоматический (FHIR, MOCK, EXTERNAL_HOSPITAL)
     */
    public boolean isAutomated() {
        return this != MANUAL_ENTRY;
    }

    /*
     * Проверяет, требуется ли дополнительная проверка данных.
     *
     * ЗАЧЕМ: data from внешних больниц и моковые data требуют
     * более тщательной проверки before использованием в PMA.
     *
     * @return true if требуется дополнительная проверка
     */
    public boolean requiresAdditionalValidation() {
        return this == EXTERNAL_HOSPITAL || this == MOCK_GENERATOR;
    }

    /*
     * Returns человекочитаемое Description источника.
     *
     * ПРИМЕНЕНИЕ: for отображения в UI, логах, отчетах.
     *
     * @return Description источника данных
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
