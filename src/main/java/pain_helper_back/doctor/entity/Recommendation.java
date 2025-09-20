package pain_helper_back.doctor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity(name = "doctor_recommendation")

@Data
public class Recommendation {
    @Id
    private Long id;
    private Long patientId;
    private String drugName;
    private String dosage;
    private String interval;
    private String status; // pending, approved, rejected
    private LocalDateTime createdAt;
}
