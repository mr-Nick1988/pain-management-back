package pain_helper_back.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Audit {
    @Id
    private Long id;
    private String action; // e.g., "VAS_INPUT", "RECOMMENDATION_GENERATED", "APPROVAL"
    private Long patientId;
    private Long recommendationId;
    private String details; // JSON or description
    private LocalDateTime timestamp;
    private String userRole; // e.g., "nurse", "doctor"
}
