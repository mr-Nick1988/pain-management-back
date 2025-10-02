package pain_helper_back.common.patients.entity;

import jakarta.persistence.*;
import lombok.Data;
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
    @Enumerated(EnumType.STRING)   // По умолчанию, если не поставить аннотацию @Enumerated, то JPA сохранит числовой индекс enum-а (ORDINAL).
    private RecommendationStatus status;
    @Column(name = "rejected_reason")
    private String rejectedReason;

    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DrugRecommendation> drugs = new ArrayList<>();

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
    private List<String> comments;      // свободные комментарии

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;
    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.createdBy = "TODO: взять из контекста текущего пользователя";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = "TODO: взять из контекста текущего пользователя";
    }

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;
}
