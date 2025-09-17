package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Weight is required")
    private Double weight;

    @NotNull(message = "Age is required")
    private Integer age;
    private String gfr;
    private String childPughScore;
    private Double plt;
    private Double wbc;
    private Double sat;
    private Double sodium;
}
