package pain_helper_back.common.patients.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DiagnosisDTO {
    @Pattern(
            regexp = "^[A-Z]?\\d{2,3}(?:\\.[A-Z0-9]{1,4})?$",
            message = "Diagnosis code must match ICD-9 or ICD-10 format (e.g. 571.2, V45.1103, 585.4W1)"
    )
    @NotNull(message = "Diagnosis code is required")
    private String IcdCode;
    @NotNull(message = "Diagnosis description is required")
    @Size(min = 3, max = 200, message = "Diagnosis description must be between 3 and 200 characters")
    private String description;
}