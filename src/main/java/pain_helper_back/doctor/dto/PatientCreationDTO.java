package pain_helper_back.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientCreationDTO {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;
    
    @NotBlank(message = "EMR number is required")
    @Size(max = 20, message = "EMR number must not exceed 20 characters")
    private String emrNumber;

    @Size(max = 1000, message = "Additional info must not exceed 1000 characters")
    private String additionalInfo;
}
