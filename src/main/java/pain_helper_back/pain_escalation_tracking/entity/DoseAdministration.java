package pain_helper_back.pain_escalation_tracking.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;

import java.time.LocalDateTime;

/**
 * Сущность для отслеживания введенных доз препаратов
 * Используется для проверки интервалов между дозами
 */
@Entity
@Table(name = "dose_administrations")
@Getter
@Setter
@RequiredArgsConstructor
public class DoseAdministration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    @ManyToOne
    @JoinColumn(name = "recommendation_id")
    private Recommendation recommendation;
    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Column(name = "dosage", nullable = false)
    private Double dosage;

    @Column(name = "unit")
    private String unit;//единица измерения дозы например mg

    @Column(name = "route")
    private String route;

    @Column(name = "administered_at", nullable = false)
    private LocalDateTime administeredAt;

    @Column(name = "administered_by", nullable = false)
    private String administeredBy;

    @Column(name = "vas_before")
    private Integer vasBefore;

    @Column(name = "vas_after")
    private Integer vasAfter;

    @Column(name = "next_dose_allowed_at")
    private LocalDateTime nextDoseAllowedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (administeredAt == null) {
            administeredAt = LocalDateTime.now();
        }
    }
}
