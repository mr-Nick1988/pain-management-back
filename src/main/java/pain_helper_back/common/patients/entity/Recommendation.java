package pain_helper_back.common.patients.entity;

import jakarta.persistence.*;
import lombok.Data;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.pain_escalation_tracking.entity.DoseAdministration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a medical recommendation record.
 * Note: this entity currently contains both core medical data (drugs, contraindications)
 * and workflow-related fields (doctor/anesthesiologist actions).
 * TODO (v2): separate workflow actions into a new entity RecommendationWorkflow
 * to simplify auditing and reporting.
 */

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
    @Enumerated(EnumType.STRING)   // По умолчанию, если не поставить аннотацию @Enumerated, то JPA сохранит числовой индекс enum-а (ORDINAL).
    private RecommendationStatus status;
    @Column(name = "rejected_reason")
    private String rejectedReason;


    // ========== ПРЕПАРАТЫ ========== //
    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DrugRecommendation> drugs = new ArrayList<>();

    // ========== ПРОТИВОПОКАЗАНИЯ И КОММЕНТАРИИ ========== //
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_contraindications", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "element",length = 2000,columnDefinition = "TEXT")
    private List<String> contraindications = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_comments", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "element")
    private List<String> comments = new ArrayList<>();      // свободные комментарии

    // ========== NON-PERSISTENT (transient) FIELDS ========== //
    @Column(name = "generation_failed")
    private Boolean generationFailed; // не сохраняется в БД, используется только на уровне логики
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "recommendation_rejection_reasons",
            joinColumns = @JoinColumn(name = "recommendation_id")
    )
    @Column(name = "reason", length = 2000)
    private List<String> rejectionReasonsSummary = new ArrayList<>(); // копим системные причины отказа

    // ========== WORKFLOW: DOCTOR LEVEL ========== //
    @Column(name = "doctor_id", length = 50)
    private String doctorId;
    @Column(name = "doctor_action_at")
    private LocalDateTime doctorActionAt;
    @Column(name = "doctor_comment", length = 1000)
    private String doctorComment;



    // ========== WORKFLOW: ANESTHESIOLOGIST LEVEL ========== //
    @Column(name = "anesthesiologist_id", length = 50)
    private String anesthesiologistId;
    @Column(name = "anesthesiologist_action_at")
    private LocalDateTime anesthesiologistActionAt;
    @Column(name = "anesthesiologist_comment", length = 1000)
    private String anesthesiologistComment;

    @Column(name = "replaced_at")
    private LocalDateTime replacedAt; // Отследить жизн. цикл после reject старой рек. заменяется на новую
    @Column(name = "replacement_id")
    private Long replacementId; // ID новой рекомендации, которая заменяет старую

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

    // ========== Doses checking ========== //
    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DoseAdministration> doseAdministrations = new ArrayList<>();

    // ========== EMR RECALCULATION FIELDS ========== // TODO - перенести в analytics
    @Column(name = "review_reason", length = 2000)
    private String reviewReason;  // Причина необходимости пересмотра
    
    @Column(name = "review_requested_at")
    private LocalDateTime reviewRequestedAt;  // Когда запрошен пересмотр
    
    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;  // Кто пересмотрел
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;  // Когда пересмотрено
    
    @Column(name = "description", length = 5000, columnDefinition = "TEXT")
    private String description;  // Описание рекомендации
    
    @Column(name = "justification", length = 5000, columnDefinition = "TEXT")
    private String justification;  // Обоснование рекомендации

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