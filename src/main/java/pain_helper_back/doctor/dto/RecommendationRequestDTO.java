package pain_helper_back.doctor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecommendationRequestDTO {
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    
    @NotNull(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Justification is required")
    @Size(max = 1000, message = "Justification must not exceed 1000 characters")
    private String justification;
}
