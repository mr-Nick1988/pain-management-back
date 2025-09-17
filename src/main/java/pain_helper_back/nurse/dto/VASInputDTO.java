package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VASInputDTO {
    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Pain level is required")
    @Min(value = 0, message = "Pain level must be at least 0")
    @Max(value = 10, message = "Pain level must be at most 10")
    private Integer painLevel;
}
