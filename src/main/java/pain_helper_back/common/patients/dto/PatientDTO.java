package pain_helper_back.common.patients.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pain_helper_back.enums.PatientsGenders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PatientDTO {
    private String mrn;
    @NotBlank(message = "Name is required")
    private String firstName;
    @NotBlank(message = "Name is required")
    private String lastName;
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;
    @NotNull(message = "Gender is required")
    private PatientsGenders gender;
    @NotBlank(message = "Insurance Policy Number is required")
    private String insurancePolicyNumber;
    @NotBlank(message = "Phone Number is required")
    private String phoneNumber;
    @Email(message = "Email is required")
    private String email;
    @NotBlank(message = "Address is required")
    private String address;
    private String additionalInfo;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private String createdBy;


    private List<EmrDTO> emr;

    private List<VasDTO> vas;

    private List<RecommendationDTO> recommendations;


}