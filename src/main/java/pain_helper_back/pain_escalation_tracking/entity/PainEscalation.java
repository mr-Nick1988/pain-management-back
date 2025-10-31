package pain_helper_back.pain_escalation_tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.EscalationPriority;

import java.time.LocalDateTime;

@Entity
@Table(name = "pain_escalations")
@Getter
@Setter
@RequiredArgsConstructor
public class PainEscalation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    private Recommendation lastRecommendation;   // nullable

    private Integer previousVas;
    private Integer currentVas;
    private Integer vasChange;

    @Enumerated(EnumType.STRING)
    private EscalationPriority priority;         // LOW, MEDIUM, HIGH, CRITICAL


    private LocalDateTime createdAt;
}