package pain_helper_back.common.patients.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vas")

public class Vas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vas_id")
    private Long id;
    @Column(name = "pain_place")
    private String painPlace;
    @Column(name = "pain_level")
    private Integer painLevel; // 0-10 scale

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by",updatable = false)
    private String createdBy;
    @Column(name="updated_by")
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
