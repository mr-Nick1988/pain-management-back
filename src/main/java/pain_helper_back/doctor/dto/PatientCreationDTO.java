package pain_helper_back.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientCreationDTO {

    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "EMR number is required")
    private String emrNumber;

    private String additionalInfo;
}
