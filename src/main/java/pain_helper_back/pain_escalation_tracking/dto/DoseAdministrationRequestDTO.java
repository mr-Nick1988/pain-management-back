package pain_helper_back.pain_escalation_tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * DTO для регистрации введенной дозы препарата.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoseAdministrationRequestDTO {

    @NotBlank(message = "Drug name is required")
    @Size(max = 200, message = "Drug name must not exceed 200 characters")
    private String drugName;

    @NotNull(message = "Dosage is required")
    @Positive(message = "Dosage must be positive")
    private Double dosage;

    @NotBlank(message = "Unit is required")
    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    @NotBlank(message = "Route is required")
    @Size(max = 50, message = "Route must not exceed 50 characters")
    private String route;

    @NotBlank(message = "Administered by is required")
    @Size(max = 100, message = "Administered by must not exceed 100 characters")
    private String administeredBy;

    private Integer vasBefore;  // Уровень боли до
    private Integer vasAfter;   // Уровень боли после
    private Long recommendationId;  // ID рекомендации

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

}