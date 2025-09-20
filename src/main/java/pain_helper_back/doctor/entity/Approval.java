package pain_helper_back.doctor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity(name = "doctor_approval")
@Table(name = "doctor_approval")
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
