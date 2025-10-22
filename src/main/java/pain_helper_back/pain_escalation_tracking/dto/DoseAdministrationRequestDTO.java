package pain_helper_back.pain_escalation_tracking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * DTO для регистрации введённой дозы
 * Используется медсестрой/врачом при ручном вводе информации о введении препарата
 */
@Getter
public class DoseAdministrationRequestDTO {

    /** Название препарата */
    @NotBlank(message = "Drug name is required")
    @Size(max = 200, message = "Drug name must not exceed 200 characters")
    private String drugName;

    /** Дозировка препарата (например, 10 mg IV) */
    @NotBlank(message = "Dosage is required")
    @Size(max = 100, message = "Dosage must not exceed 100 characters")
    private String dosage;

    /** Путь введения (IV, IM и т.д.) */
    @Size(max = 100, message = "Route must not exceed 100 characters")
    private String route;

    /** Кто вводил препарат (ID медсестры/врача) */
    @NotBlank(message = "Administered by is required")
    @Size(max = 50, message = "Administered by must not exceed 50 characters")
    private String administeredBy;

    /** ID рекомендации, если доза вводится в рамках конкретной рекомендации */
    private Long recommendationId;

    /** Уровень боли до введения (0-10) */
    @Min(value = 0, message = "VAS before must be between 0 and 10")
    @Max(value = 10, message = "VAS before must be between 0 and 10")
    private Integer vasBefore;

    /** Уровень боли после введения (0-10) */
    @Min(value = 0, message = "VAS after must be between 0 and 10")
    @Max(value = 10, message = "VAS after must be between 0 and 10")
    private Integer vasAfter;

    /** Дополнительные примечания (не более 1000 символов) */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}