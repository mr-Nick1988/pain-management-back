package pain_helper_back.anesthesiologist.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "escalation")
public class Escalation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escalation_id")
    private Long id;

    // ========== СВЯЗЬ С РЕКОМЕНДАЦИЕЙ ========== //
    @OneToOne
    @JoinColumn(name = "recommendation_id", nullable = false, unique = true)
    private Recommendation recommendation;

    // ========== ИНФОРМАЦИЯ ОБ ЭСКАЛАЦИИ ========== //
    @Column(name = "escalated_by", nullable = false, length = 50)
    private String escalatedBy;  // Doctor ID who escalated

    @Column(name = "escalated_at", nullable = false)
    private LocalDateTime escalatedAt;

    @Column(name = "escalation_reason", length = 1000)
    private String escalationReason;  // Why doctor rejected

    // ========== ПРИОРИТЕТ И СТАТУС ========== //
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private EscalationPriority priority;  // HIGH, MEDIUM, LOW

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EscalationStatus status;  // PENDING, IN_PROGRESS, RESOLVED, CANCELLED

    // ========== ОПИСАНИЕ И РАЗРЕШЕНИЕ ========== //
    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "resolution", length = 2000)
    private String resolution;

    // ========== ИНФОРМАЦИЯ О РАЗРЕШЕНИИ ========== //
    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;  // Anesthesiologist ID

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ========== AUDIT FIELDS ========== //
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // ========== LIFECYCLE CALLBACKS ========== //
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.createdBy == null) {
            this.createdBy = "system";
        }
        if (this.status == null) {
            this.status = EscalationStatus.PENDING;
        }
        if (this.escalatedAt == null) {
            this.escalatedAt = LocalDateTime.now();
        }
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.updatedBy == null) {
            this.updatedBy = "system";
        }
    }
}