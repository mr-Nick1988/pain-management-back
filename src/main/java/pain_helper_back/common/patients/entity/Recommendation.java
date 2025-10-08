package pain_helper_back.common.patients.entity;

import jakarta.persistence.*;
import lombok.Data;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "recommendation")
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;
    @Column(name = "regimen_hierarchy")
    private int regimenHierarchy;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    // По умолчанию, если не поставить аннотацию @Enumerated, то JPA сохранит числовой индекс enum-а (ORDINAL).
    private RecommendationStatus status;
    @Column(name = "rejected_reason")
    private String rejectedReason;

    // ========== ПРЕПАРАТЫ ========== //
    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DrugRecommendation> drugs = new ArrayList<>();

    // ========== ПРОТИВОПОКАЗАНИЯ И КОММЕНТАРИИ ========== //
    @ElementCollection //Эта аннотация используется для хранения простых коллекций (recommendation_id,element)
    @CollectionTable(name = "recommendation_avoid_sensitivity", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "element")
    private List<String> avoidIfSensitivity = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "recommendation_contraindications", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "element")
    private List<String> contraindications = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "recommendation_comments", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "element")
    private List<String> comments = new ArrayList<>();      // свободные комментарии

    // ========== WORKFLOW: DOCTOR LEVEL ========== //
    @Column(name = "doctor_id", length = 50)
    private String doctorId;
    @Column(name = "doctor_action_at")
    private LocalDateTime doctorActionAt;
    @Column(name = "doctor_comment", length = 1000)
    private String doctorComment;

    // ========== WORKFLOW: ESCALATION ========== //
    @OneToOne(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Escalation escalation;

    // ========== WORKFLOW: ANESTHESIOLOGIST LEVEL ========== //
    @Column(name = "anesthesiologist_id", length = 50)
    private String anesthesiologistId;
    @Column(name = "anesthesiologist_action_at")
    private LocalDateTime anesthesiologistActionAt;
    @Column(name = "anesthesiologist_comment", length = 1000)
    private String anesthesiologistComment;

    // ========== WORKFLOW: FINAL APPROVAL ========== //
    @Column(name = "final_approved_by", length = 50)
    private String finalApprovedBy;
    @Column(name = "final_approval_at")
    private LocalDateTime finalApprovalAt;

    // ========== AUDIT FIELDS ========== //
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "created_by", updatable = false,length = 50)
    private String createdBy;
    @Column(name = "updated_by",length = 50)
    private String updatedBy;

    // ========== PATIENT RELATIONSHIP ========== //
    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    // ========== LIFECYCLE CALLBACKS ========== //
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.createdBy == null) {
            this.createdBy = "system";
        }
        if (this.status == null) {
            this.status = RecommendationStatus.PENDING;
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