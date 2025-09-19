package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pain_helper_back.nurse.PatientsGenders;

import java.time.LocalDate;
import java.util.List;

@Data
public class PatientDto {
    @NotBlank(message = "Name is required")
    private String firstName;
    @NotBlank(message = "Name is required")
    private String lastName;
    @NotBlank(message = "Person ID is required")
    private String personId;
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;
    @NotNull(message = "Gender is required")
    private PatientsGenders gender;
    @NotNull(message = "Height is required")
    private Double height;
    @NotNull(message = "Weight is required")
    private Double weight;


    private List<EmrDto> emr;

    private List<VasDto> vas;

    private List<RecommendationDto> recommendations;


}
