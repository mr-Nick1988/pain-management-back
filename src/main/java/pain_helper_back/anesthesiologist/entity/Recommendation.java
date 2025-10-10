package pain_helper_back.anesthesiologist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity(name = "anesthesiologist_recommendation")

@Data
public class Recommendation {
    @Id
    private Long id;
    private Long patientId;
    private String drugName;
    private String dosage;
    @Column(name = "dosage_interval")

    private String interval;
    private String status; // pending, approved, rejected
    private LocalDateTime createdAt;
}
