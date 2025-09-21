package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientDTO {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private Double weight;

    @NotNull(message = "Age is required")
    @Positive(message = "Age must be positive")
    @Max(value = 150, message = "Age must be realistic")
    private Integer age;

    @Size(max = 50, message = "GFR must not exceed 50 characters")
    private String gfr;

    @Size(max = 50, message = "Child Pugh Score must not exceed 50 characters")
    private String childPughScore;
}
