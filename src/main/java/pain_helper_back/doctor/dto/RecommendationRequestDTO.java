package pain_helper_back.doctor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecommendationRequestDTO {
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    @NotNull(message = "Description is required")
    private String description;
    @NotNull(message = "Justification is required")
    private String justification;
}
