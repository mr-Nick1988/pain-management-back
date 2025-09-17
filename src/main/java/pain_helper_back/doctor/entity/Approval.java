package pain_helper_back.doctor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity

@Data
public class Approval {
    @Id

    private Long id;
    private Long recommendationId;
    private String approvedBy; // Role: doctor, nurse, etc.
    private Boolean approved;
    private String reason; // If rejected
    private LocalDateTime approvedAt;
}
