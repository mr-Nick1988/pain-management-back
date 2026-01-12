package pain_helper_back.admin.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
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
