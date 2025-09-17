package pain_helper_back.doctor.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatientResponseDTO {
    private Long id;
    private String emrNumber;
    private String firstName;
    private String lastName;
    private String additionalInfo;
    private Long createdBy;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
