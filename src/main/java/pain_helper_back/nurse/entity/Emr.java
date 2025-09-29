package pain_helper_back.nurse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "emr")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Emr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emr_id")
    private Long id;
    @Column(name = "height")
    private Double height;
    @Column(name = "weight")
    private Double weight;
    @Column(name = "glomerular_filtration_rate")
    private String gfr;
    @Column(name = "child_pugh_score")
    private String childPughScore;
    @Column(name = "platelets_count")
    private Double plt;
    @Column(name = "white_blood_cells")
    private Double wbc;
    @Column(name = "oxygen_saturation")
    private Double sat;
    @Column(name = "sodium_level")
    private Double sodium;

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
