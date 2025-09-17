package pain_helper_back.anesthesiologist.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity

@Data
public class AnesthesiologistRecommendation {
    @Id
    private Long id;
    private Long patientId;
    private String drugName;
    private String dosage;
    private String interval;
    private String status; // pending, approved, rejected
    private LocalDateTime createdAt;
}
