package pain_helper_back.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity

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
