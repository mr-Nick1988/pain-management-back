package pain_helper_back.doctor.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PatientResponseDTO {
    private Long id;
    private String mrn;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String insurancePolicyNumber;
    private String phoneNumber;
    private String email;
    private String address;
    private String additionalInfo;
    private Long createdBy;
    private Long updatedBy;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}