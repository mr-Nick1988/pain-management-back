package pain_helper_back.nurse.dto;

import lombok.Data;
import pain_helper_back.enums.PatientsGenders;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PatientUpdateDTO {

    private String firstName;
    private String lastName;
    private PatientsGenders gender;
    private String insurancePolicyNumber;
    private String phoneNumber;
    private String email;
    private String address;
    private String additionalInfo;
    private LocalDateTime updatedAt;
    private String updatedBy;



}
