package pain_helper_back.enums;

/**
 * typeы источников yesнных EMR (Electronic Medical Records).
 * Используется for отслеживания происхождения медицинских yesнных patient.
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
     * data from реальной внешней painницы (будущая функциональность).
     */
    EXTERNAL_HOSPITAL,

    /**
     * data введены вручную медицинским персоналом.
     */
    MANUAL_ENTRY;

    /*
     * Проверяет, является ли источник автоматическим (не ручной ввод).
     *
     * why: for автоматических источников нужна дополнительная валиyesция,
     * так how data могут быть неполными or некорректными.
     *
     * @return true if источник автоматический (FHIR, MOCK, EXTERNAL_HOSPITAL)
     */
    public boolean isAutomated() {
        return this != MANUAL_ENTRY;
    }

    /*
     * Проверяет, требуется ли дополнительная проверка yesнных.
     *
     * why: data from внешних painниц и моковые data требуют
     * more тщательной проверки before использованием в PMA.
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
     * @return Description источника yesнных
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
